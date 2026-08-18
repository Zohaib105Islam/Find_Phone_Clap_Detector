package com.base.find_phone_clap_detector.ui.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivitySoundPreviewBinding
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.ui.dataClasses.SoundsDataClass
import com.base.find_phone_clap_detector.utils.AudioPermissionUtil
import com.base.find_phone_clap_detector.utils.Constants
import com.base.find_phone_clap_detector.utils.DetectorWorkerStarter
import com.base.find_phone_clap_detector.utils.KEY
import com.base.find_phone_clap_detector.utils.LocaleHelper
import com.base.find_phone_clap_detector.utils.NotificationPermissionUtil
import com.base.find_phone_clap_detector.utils.SeekArc
import com.base.find_phone_clap_detector.utils.SoundsPreviewCarousel
import com.base.find_phone_clap_detector.utils.TinyDB
import com.base.find_phone_clap_detector.utils.disableMultipleClicking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SoundPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySoundPreviewBinding

    private var soundsArrayList = arrayListOf<SoundsDataClass>()
    private var selectedSoundPosition = 0
    private var selectedSoundUri: String? = null

    private lateinit var audioPermissionUtil: AudioPermissionUtil
    private lateinit var notificationPermissionUtil: NotificationPermissionUtil

    private lateinit var tinyDB: TinyDB

    private lateinit var prefs: SharedPreferences

    private var isFromCreateSound = false
    private lateinit var job: Job
    var handler = Handler(Looper.getMainLooper())

    private var mediaPlayer: MediaPlayer? = null

    private val TAG = "PreviewAct"


    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.setAppLocale(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySoundPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.main.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        prefs = getSharedPreferences("feature_attempts_prefs", Context.MODE_PRIVATE)

        audioPermissionUtil = AudioPermissionUtil(
            AudioPermissionUtil.FromActivity(this)
        )
        notificationPermissionUtil = NotificationPermissionUtil(
            NotificationPermissionUtil.FromActivity(this)
        )

        tinyDB = TinyDB(this)
        job = Job()

        apiArgs()
        initViews()
        initComposeList()
    }

    private fun apiArgs() {
        val img = intent.getIntExtra("img", 0)
        selectedSoundPosition = intent.getIntExtra("position", 0)
        val title = intent.getStringExtra("title")
        isFromCreateSound = intent.getBooleanExtra("isFromCreateSound", false)
        val uriString = intent.getStringExtra("uri")
        if (uriString.isNullOrEmpty()) {
            Toast.makeText(this, "Sound URI missing!", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Missing URI from Intent, finishing activity safely.")
            finish()
        } else {
            selectedSoundUri = uriString
            playNewSelectedSound(selectedSoundUri)
        }

        binding.currentSoundName.text = title ?: getString(R.string.preview_sound)
        binding.circleImageView.setImageResource(
            if (img != 0) img else R.drawable.ic_record_audio
        )

    }

    private fun initComposeList() {
        soundsArrayList.clear()

        if (isFromCreateSound) {
            soundsArrayList = getFavoriteItems()
        } else {
            lifecycleScope.launch {
                soundsArrayList.add(
                    SoundsDataClass(
                        getString(R.string.cat_meow),
                        R.drawable.ic_cat_meo,
                        false,
                        Uri.parse("android.resource://" + packageName + "/" + R.raw.cat_sound)
                            .toString()
                    )
                )
                soundsArrayList.add(
                    SoundsDataClass(
                        getString(R.string.dog_barking),
                        R.drawable.ic_dog_bark,
                        false,
                        Uri.parse("android.resource://" + packageName + "/" + R.raw.dog_sound)
                            .toString()
                    )
                )
                soundsArrayList.add(
                    SoundsDataClass(
                        getString(R.string.say_he),
                        R.drawable.ic_say_hey,
                        false,
                        Uri.parse("android.resource://" + packageName + "/" + R.raw.hey_sound)
                            .toString()
                    )
                )
                soundsArrayList.add(
                    SoundsDataClass(
                        getString(R.string.whistlee),
                        R.drawable.ic_whistle2,
                        false,
                        Uri.parse("android.resource://" + packageName + "/" + R.raw.whistle_sound)
                            .toString()
                    )
                )
                soundsArrayList.add(
                    SoundsDataClass(
                        getString(R.string.door_bell),
                        R.drawable.ic_door_bell,
                        false,
                        Uri.parse("android.resource://" + packageName + "/" + R.raw.doorbell_sound)
                            .toString()
                    )
                )

                soundsArrayList.add(
                    SoundsDataClass(
                        getString(R.string.car_horn),
                        R.drawable.ic_car_horn,
                        false,
                        Uri.parse("android.resource://" + packageName + "/" + R.raw.car_horn_sound)
                            .toString()
                    )
                )
                soundsArrayList.add(
                    SoundsDataClass(
                        getString(R.string.hello),
                        R.drawable.ic_robot,
                        false,
                        Uri.parse("android.resource://" + packageName + "/" + R.raw.hello_sound)
                            .toString()
                    )
                )
                soundsArrayList.add(
                    SoundsDataClass(
                        getString(R.string.party_horn),
                        R.drawable.ic_party_horn,
                        false,
                        Uri.parse("android.resource://" + packageName + "/" + R.raw.party_sound)
                            .toString()
                    )
                )
                soundsArrayList.add(
                    SoundsDataClass(
                        getString(R.string.police_horn),
                        R.drawable.ic_police_whistle,
                        false,
                        Uri.parse("android.resource://" + packageName + "/" + R.raw.police_sound)
                            .toString()
                    )
                )

            }
        }

        binding.composeCarousel.setContent {
            SoundsPreviewCarousel(
                sounds = soundsArrayList,
                initialSelectedIndex = selectedSoundPosition,
                onItemSelected = { soundItem ->

                    // Save new selected URI
                    selectedSoundUri = soundItem.audioUri
                    binding.currentSoundName.text = soundItem.title
                    binding.circleImageView.setImageResource(
                        if (soundItem.img != 0) soundItem.img else R.drawable.ic_record_audio
                    )

                    // Stop previous audio
                    stopCurrentMedia()

                    // Play new selected audio
                    playNewSelectedSound(soundItem.audioUri)
                }
            )
        }
    }

    private fun initViews() {

        binding.playBtn.setOnClickListener {
            mediaPlayer?.let {
                it.start()
                binding.playBtn.visibility = View.GONE
                binding.pauseBtn.visibility = View.VISIBLE
                playWaveSeek()
            }
        }

        binding.pauseBtn.setOnClickListener {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    binding.playBtn.visibility = View.VISIBLE
                    binding.pauseBtn.visibility = View.GONE
                }
            }
        }

        setupTimerAndLoop() // default selection handled here

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding.backIcon.setOnClickListener {
            mediaPlayer?.release()
            mediaPlayer = null
            finish()
        }

        binding.applySoundBtn.setOnClickListener {
            disableMultipleClicking(it)

            audioPermissionUtil.checkAndRequest {
                notificationPermissionUtil.checkAndRequest {
                    //  Permission granted → Go Next
                    if (Constants.SERVICE_TYPE == Constants.POCKET_MODE) {
                        prefs.edit().putInt("pocket_mode_attempts", 0).apply()
                    }
                    if (Constants.SERVICE_TYPE == Constants.DONT_TOUCH) {
                        prefs.edit().putInt("dont_touch_attempts", 0).apply()
                    }
                    handleApplySoundLogic()
                }
            }
        }

        // --- Toggle buttons --- //
        val disableUsingPhone = MyApplication.mInstance.preferenceManager
            .getBoolean(PreferenceManager.Key.disableWhenUsingPhone, false)

        binding.btnDisableToggle.isChecked = disableUsingPhone
        setToggleColor(binding.btnDisableToggle)
        binding.btnDisableToggle.setOnCheckedChangeListener { _, isChecked ->
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.disableWhenUsingPhone,
                isChecked
            )
            setToggleColor(binding.btnDisableToggle)
        }

        val scheduleDeactivate = MyApplication.mInstance.preferenceManager
            .getBoolean(PreferenceManager.Key.scheduleDeactivate, false)

        binding.btnDeactivateToggle.isChecked = scheduleDeactivate
        setToggleColor(binding.btnDeactivateToggle)
        binding.btnDeactivateToggle.setOnCheckedChangeListener { _, isChecked ->
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.scheduleDeactivate,
                isChecked
            )
            setToggleColor(binding.btnDeactivateToggle)
        }

        // --- Volume SeekArc --- //
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val runnable = object : Runnable {
            override fun run() {
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val progress = (currentVolume.toFloat() / maxVolume.toFloat() * 100).toInt()
                binding.seekArc.progress = progress

                // --- Fix: display volume percent --- //
                binding.tvVolumePercent.text = "$progress%"

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)

        binding.seekArc.setOnSeekArcChangeListener(object : SeekArc.OnSeekArcChangeListener {
            override fun onProgressChanged(seekArc: SeekArc?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val volume = progress.toFloat() / seekArc?.max!! * maxVolume
                    setVolume(volume, maxVolume, audioManager)
                    binding.tvVolumePercent.text = "$progress%"
                }
            }

            override fun onStartTrackingTouch(seekArc: SeekArc?) {}
            override fun onStopTrackingTouch(seekArc: SeekArc?) {}
        })

        val audioFile = getFileFromRawResource(R.raw.cat_meow)
        if (audioFile == null) {
            Log.e(TAG, "Failed to load audio file for waveform.")
        }
    }

    private fun setToggleColor(toggle: androidx.appcompat.widget.SwitchCompat) {

        val selectedBlue = ContextCompat.getColor(this, R.color.primary)
        val unselectedBlue = ContextCompat.getColor(this, R.color.sound_preview_track_off)
        val white = ContextCompat.getColor(this, R.color.white)

        toggle.trackTintList = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                selectedBlue,
                unselectedBlue
            )
        )

        toggle.thumbTintList = android.content.res.ColorStateList.valueOf(white)
    }

    private fun setupTimerAndLoop() {

        val savedLoop = MyApplication.mInstance.preferenceManager
            .getBoolean(PreferenceManager.Key.checkLoop, true)

        val savedTime = MyApplication.mInstance.preferenceManager
            .getInt(PreferenceManager.Key.soundPlayTime, 45000)

        if (savedLoop) {
            selectLoop()
        } else {
            selectTimer(savedTime)
        }

        setupTimerClickListeners()
    }

    private fun setupTimerClickListeners() {

        binding.cardFiveSecond.setOnClickListener { selectTimer(5000) }
        binding.cardTenSecond.setOnClickListener { selectTimer(10000) }
        binding.cardThirtySecond.setOnClickListener { selectTimer(30000) }
        binding.cardFortyfiveSecond.setOnClickListener { selectTimer(45000) }
        binding.cvLoopToggle.setOnClickListener { selectLoop() }
    }

    private fun selectTimer(time: Int) {
        MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.checkLoop, false)
        MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.soundPlayTime, time)

        resetAllTimerUI()

        when (time) {
            5000 -> highlightCard(binding.cardFiveSecond)
            10000 -> highlightCard(binding.cardTenSecond)
            30000 -> highlightCard(binding.cardThirtySecond)
            45000 -> highlightCard(binding.cardFortyfiveSecond)
        }
    }

    private fun selectLoop() {
        MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.checkLoop, true)
        MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.soundPlayTime, -1)

        resetAllTimerUI()
        highlightCard(binding.cvLoopToggle)
    }

    private fun resetAllTimerUI() {

        val cards = listOf(
            binding.cardFiveSecond,
            binding.cardTenSecond,
            binding.cardThirtySecond,
            binding.cardFortyfiveSecond,
            binding.cvLoopToggle
        )

        cards.forEach { card ->

            card.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.sound_preview_chip_inactive)
            )
            card.cardElevation = 0f
            card.scaleX = 1f
            card.scaleY = 1f

            when (card.id) {

                R.id.cvLoopToggle -> {
                    val img = card.getChildAt(0) as ImageView
                    img.setColorFilter(
                        ContextCompat.getColor(this, R.color.sound_preview_text),
                        android.graphics.PorterDuff.Mode.SRC_IN
                    )
                }

                else -> {
                    val tv = card.getChildAt(0) as TextView
                    tv.setTextColor(
                        ContextCompat.getColor(this, R.color.sound_preview_text)
                    )
                }
            }
        }
    }

    private fun highlightCard(card: androidx.cardview.widget.CardView) {

        card.setCardBackgroundColor(
            ContextCompat.getColor(this, R.color.primary)
        )
        card.cardElevation = 5f * resources.displayMetrics.density
        card.scaleX = 1.04f
        card.scaleY = 1.04f

        when (card.id) {

            R.id.cvLoopToggle -> {
                val img = card.getChildAt(0) as ImageView
                img.setColorFilter(
                    ContextCompat.getColor(this, R.color.white),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }

            else -> {
                val tv = card.getChildAt(0) as TextView
                tv.setTextColor(
                    ContextCompat.getColor(this, R.color.white)
                )
            }
        }
    }

    private fun handleApplySoundLogic() {
        applySound()
    }

    private fun applySound() {
        // Save preferences based on toggle state
        val isLoop = MyApplication.mInstance.preferenceManager
            .getBoolean(PreferenceManager.Key.checkLoop, true)

        if (isLoop) {
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.soundPlayTime,
                -1
            )
        } else {
            val selectedTime = MyApplication.mInstance.preferenceManager
                .getInt(PreferenceManager.Key.soundPlayTime, 45000)

            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.soundPlayTime,
                selectedTime
            )
        }

        // Proceed with activation and ad flow
        MyApplication.isFromMain = false

        if (MyApplication.mInstance.byCreateAudioService) {
            Log.d("MainActivityTest", "Sound Preview byCreate Audio")
            Constants.SERVICE_TYPE = Constants.BY_CLAP

            MyApplication.mInstance.byCreateAudioServiceActivated = true
            MyApplication.mInstance.byCreateAudioService = false

            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.appliedSoundUri,
                selectedSoundUri
            )
            startActivity(Intent(this, SuccessfullyActivatedActivity::class.java))
            Toast.makeText(this, "Service Activated", Toast.LENGTH_SHORT).show()

            if (::job.isInitialized) job.cancel()
            restartService()
            finish()
        } else {
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.appliedSoundUri,
                selectedSoundUri
            )
            startActivity(Intent(this, SuccessfullyActivatedActivity::class.java))
            Toast.makeText(this, "Service Activated", Toast.LENGTH_SHORT).show()
            if (::job.isInitialized) job.cancel()
            restartService()
            finish()
        }

        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun setCardBackgroundColorAndClickListeners(timerSound: Int) {
        val cardList = listOf(
            binding.cardFiveSecond,
            binding.cardTenSecond,
            binding.cardThirtySecond,
            binding.cardFortyfiveSecond,
            binding.cvLoopToggle
        )

        val clickActions = listOf(
            {
                MyApplication.mInstance.preferenceManager.put(
                    PreferenceManager.Key.soundPlayTime,
                    5000
                )
            },
            {
                MyApplication.mInstance.preferenceManager.put(
                    PreferenceManager.Key.soundPlayTime,
                    10000
                )
            },
            {
                MyApplication.mInstance.preferenceManager.put(
                    PreferenceManager.Key.soundPlayTime,
                    30000
                )
            },
            {
                MyApplication.mInstance.preferenceManager.put(
                    PreferenceManager.Key.soundPlayTime,
                    45000
                )
            },
            {
                MyApplication.mInstance.preferenceManager.put(
                    PreferenceManager.Key.soundPlayTime,
                    60000
                )
            }
        )

        // Update UI based on current selection
        cardList.forEachIndexed { index, cardView ->
            val color = when (timerSound) {
                5000 -> if (index == 0) R.color.purple_dark else R.color.gray_light
                10000 -> if (index == 1) R.color.purple_dark else R.color.gray_light
                30000 -> if (index == 2) R.color.purple_dark else R.color.gray_light
                45000 -> if (index == 3) R.color.purple_dark else R.color.gray_light
                60000 -> if (index == 4) R.color.purple_dark else R.color.gray_light
                else -> R.color.gray_light
            }

            cardView.setCardBackgroundColor(ContextCompat.getColor(this, color))

            // Timer selection logic
            cardView.setOnClickListener {
                clickActions[index].invoke()
                setCardBackgroundColorAndClickListeners(
                    MyApplication.mInstance.preferenceManager.getInt(
                        PreferenceManager.Key.soundPlayTime, 45000
                    )
                )
            }
        }
    }

    private fun getFavoriteItems(): ArrayList<SoundsDataClass> {
        val items = tinyDB.getListObject(KEY.sounds, SoundsDataClass::class.java)
        logFavoriteItems("getFavoriteItems", items)
        return items
    }

    private fun logFavoriteItems(source: String, items: List<SoundsDataClass> = emptyList()) {
        Log.d(TAG, "[$source] favoriteItems count=${items.size}")
        items.forEachIndexed { index, item ->
            val resourceInfo = describeDrawableResource(item.img)
            Log.d(
                TAG,
                "[$source] item#$index title='${item.title}' imgId=${item.img} resource=$resourceInfo premium=${item.isPremium} audioUri='${item.audioUri}'"
            )
        }
    }

    private fun describeDrawableResource(resId: Int): String {
        if (resId == 0) return "resId=0"

        return try {
            val typedValue = TypedValue()
            resources.getValue(resId, typedValue, true)
            val path = typedValue.string?.toString() ?: "unknown"
            val entryName = resources.getResourceEntryName(resId)
            val typeName = resources.getResourceTypeName(resId)
            "type=$typeName entry=$entryName path=$path"
        } catch (e: Exception) {
            "unresolved(${e.javaClass.simpleName}: ${e.message})"
        }
    }

    private fun stopCurrentMedia() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
    }

    private fun playNewSelectedSound(uriString: String?) {

        if (uriString.isNullOrEmpty()) return

        lifecycleScope.launch {
            try {
                stopCurrentMedia()

                val uri = Uri.parse(uriString)
                val player = MediaPlayer()

                withContext(Dispatchers.IO) {
                    player.setDataSource(this@SoundPreviewActivity, uri)
                    player.isLooping = true   // 🔥 THIS MAKES IT REPEAT FOREVER
                    player.prepare()
                }

                mediaPlayer = player
                if (mediaPlayer?.isPlaying != true) {
                    mediaPlayer?.start()
                }

                binding.playBtn.visibility = View.GONE
                binding.pauseBtn.visibility = View.VISIBLE

                playWaveSeek()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playWaveSeek() {

        // Update seek bar progress on audio progress
        job = lifecycleScope.launch {
            while (mediaPlayer?.isPlaying == true) {
                runOnUiThread {
                    try {
                        val currentDuration = mediaPlayer?.currentPosition
                        //      binding.waveSeekbar.progress = currentDuration!!.toFloat()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                try {
                    delay(300)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun restartService() {
        val isDetectorActive = MyApplication.mInstance.preferenceManager.getBoolean(
            PreferenceManager.Key.isDetectorActive,
            false
        )
        if (isDetectorActive) {
            DetectorWorkerStarter.requestStop(
                this@SoundPreviewActivity,
                "SoundPreviewActivity.restartService.stop"
            )
            DetectorWorkerStarter.requestStart(
                this@SoundPreviewActivity,
                "SoundPreviewActivity.restartService.active"
            )
        } else {
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isDetectorActive,
                true
            )
            DetectorWorkerStarter.requestStart(
                this@SoundPreviewActivity,
                "SoundPreviewActivity.restartService.inactive"
            )
        }
    }


    // Function to set volume to AudioManager
    private fun setVolume(volume: Float, maxVolume: Int, audioManager: AudioManager) {
        val calcVolume =
            if (volume > maxVolume) maxVolume else if (volume < 0) 0 else volume.toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, calcVolume, 0)
    }

    override fun onDestroy() {
        binding.root.clearFocus()
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onPostResume() {
        super.onPostResume()
        binding.root.post {
            binding.root.clearFocus()
            binding.main.clearFocus()
            window.decorView.clearFocus()
        }
    }

    override fun onPause() {
        binding.root.clearFocus()
        super.onPause()
        stopCurrentMedia()
    }

    override fun onBackPressed() {
        binding.root.clearFocus()
        super.onBackPressed()
        // Check if job is initialized before accessing it
        if (::job.isInitialized) {
            job.cancel()
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun getFileFromRawResource(@RawRes rawResId: Int): File? {
        return try {
            val inputStream = resources.openRawResource(rawResId)
            val tempFile = File.createTempFile("sample_audio", ".mp3", cacheDir)
            val outputStream = FileOutputStream(tempFile)

            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            tempFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

}
