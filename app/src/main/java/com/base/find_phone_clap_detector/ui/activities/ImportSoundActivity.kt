package com.base.find_phone_clap_detector.ui.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import com.base.find_phone_clap_detector.myApplication.MyApplication
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdSize
import com.horse.identification.extensions.gone
import com.horse.identification.extensions.visible
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivityImportSoundBinding
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.ui.adapters.AudiosAdapter
import com.base.find_phone_clap_detector.ui.dataClasses.AudioModel
import com.base.find_phone_clap_detector.ui.dataClasses.SoundsDataClass
import com.base.find_phone_clap_detector.ui.interfaces.CallBackAudioSelection
import com.base.find_phone_clap_detector.ui.viewModels.AudioSelectionViewModel
import com.base.find_phone_clap_detector.utils.DetectorWorkerStarter
import com.base.find_phone_clap_detector.utils.KEY
import com.base.find_phone_clap_detector.utils.Language
import com.base.find_phone_clap_detector.utils.LocaleHelper
import com.base.find_phone_clap_detector.utils.TinyDB
import com.base.find_phone_clap_detector.utils.singleToast
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class ImportSoundActivity : AppCompatActivity(), CallBackAudioSelection {

    private lateinit var binding: ActivityImportSoundBinding
    private   var audioAdapter: AudiosAdapter?=null
    private lateinit var viewModel: AudioSelectionViewModel
    private var mediaPlayer: MediaPlayer? = null
    private val tinyDB: TinyDB = TinyDB(MyApplication.appContext)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply current app locale before view inflation
        LocaleHelper.setAppLocale(this)
        super.onCreate(savedInstanceState)
        binding = ActivityImportSoundBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.main.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initAds()
        initViews()
    }

    private fun initAds() {
        MyApplication.mInstance.adsManager.showBanner(
            this@ImportSoundActivity,
            AdSize.LARGE_BANNER,
            binding.adFrame,
            this.getString(R.string.ADMOB_BANNER_V2),
            binding.shimmerLayout
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initViews() {
        if (!MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.isDarkTheme, false)) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        viewModel = ViewModelProvider(this)[AudioSelectionViewModel::class.java]
        binding.audioRecyclerView.layoutManager = LinearLayoutManager(this)

        val intentList = intent.getParcelableArrayListExtra<AudioModel>("audioList")

        if (!intentList.isNullOrEmpty()) {
            Log.d("ImportAudio", "Received ${intentList.size} items from Splash")
            viewModel.setAudioListIfNotLoaded(intentList)
            setAudioListToRecycler(intentList)
        } else {
            // Fallback: ViewModel will fetch if not set
            viewModel.fetchAudioFiles()
            binding.llLoading.visible()
            viewModel.audioModel.observe(this, Observer {
                Log.d("ImportAudio", "Received ${it.size} items from Splash")
                binding.llLoading.gone()
                viewModel.setAudioListIfNotLoaded(it)
                setAudioListToRecycler(it)
            })
        }

        binding.backIcon.setOnClickListener {
            finish()
        }
    }

    private fun setAudioListToRecycler(list: List<AudioModel>) {
        audioAdapter = AudiosAdapter(this@ImportSoundActivity, list, this)
        binding.audioRecyclerView.adapter = audioAdapter
    }

    override fun getItem(
        position: Int,
        holder: AudiosAdapter.MyViewHolder,
        albumPhoto: AudioModel?
    ) {
        albumPhoto?.let { audioModel ->
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.dialogue_player)

            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            dialog.setCancelable(false)
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            startOrPauseMediaPlayer(audioModel , dialog)

            try {
                dialog.show()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startOrPauseMediaPlayer(audio: AudioModel, dialog: Dialog) {
        val favoriteAudioList = ArrayList<SoundsDataClass>()

        try {
            if (mediaPlayer == null) {
                Log.d("MediaPlayer", "Trying URI: ${audio.audioUri}")
                mediaPlayer = MediaPlayer()
                mediaPlayer?.setDataSource(this@ImportSoundActivity, audio.audioUri)
                mediaPlayer?.prepare()
            }

            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            } else {
                mediaPlayer?.start()
            }

        } catch (e: IOException) {
            e.printStackTrace()
            singleToast("Cannot play this audio file.")
            dialog.dismiss()
            return
        }

        // Set audio title in dialog
        dialog.findViewById<TextView>(R.id.nameSound).text = audio.title

        // Set up play/pause buttons in the dialog
        val pauseBtn = dialog.findViewById<ImageView>(R.id.pauseBtn)
        val playBtn = dialog.findViewById<ImageView>(R.id.playBtn)
        pauseBtn.setOnClickListener {
            mediaPlayer?.pause()
            playBtn.visibility = View.VISIBLE
            pauseBtn.visibility = View.GONE
        }
        playBtn.setOnClickListener {
            mediaPlayer?.start()
            playBtn.visibility = View.GONE
            pauseBtn.visibility = View.VISIBLE
        }

        // Set up stop button and close button in the dialog
        dialog.findViewById<ImageView>(R.id.stopBtn).setOnClickListener {
            dialog.dismiss()
            mediaPlayer?.release()
            mediaPlayer = null
        }
        dialog.findViewById<ImageView>(R.id.cross).setOnClickListener {
            dialog.dismiss()
            mediaPlayer?.release()
            mediaPlayer = null
        }

        dialog.findViewById<TextView>(R.id.saveBtn).setOnClickListener {
//            val titleResId = resources.getIdentifier(audio.title, "string", packageName)
            val model = SoundsDataClass(audio.title, R.drawable.img_rv, true, audio.audioUri.toString())
            val favoriteItems = tinyDB.getListObject(KEY.sounds, SoundsDataClass::class.java)

            if (favoriteItems.any { it.audioUri == model.audioUri }) {
                singleToast("Already in Sounds...")
            } else {
                favoriteItems.add(model)
                tinyDB.putListObject(KEY.sounds, favoriteItems)
                singleToast("${audio.title} added to Sounds")
                mediaPlayer?.release()
                mediaPlayer = null
                dialog.dismiss()
            }
        }

        dialog.findViewById<TextView>(R.id.applyBtn).setOnClickListener {
//            val titleResId = resources.getIdentifier(audio.title, "string", packageName)
            val model = SoundsDataClass(audio.title, R.drawable.img_rv, true, audio.audioUri.toString())
            val favoriteItems = tinyDB.getListObject(KEY.sounds, SoundsDataClass::class.java)

            if (favoriteItems.any { it.audioUri == model.audioUri }) {
                singleToast("Already in Sounds...")
            } else {
                favoriteItems.add(model)
                tinyDB.putListObject(KEY.sounds, favoriteItems)
                singleToast("${audio.title} added to Sounds")
            }

            MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.appliedSoundUri , audio.audioUri.toString())
            singleToast("Sound Applied")
            mediaPlayer?.release()
            mediaPlayer = null
            restartService()
            dialog.dismiss()
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this@ImportSoundActivity, CreateSoundActivity::class.java))
                finish()
            }, 200)
        }

        // Set up SeekBar in the dialog
        val seekBar = dialog.findViewById<SeekBar>(R.id.seekBar)
        val playerDuration = dialog.findViewById<TextView>(R.id.playerDuration)
        seekBar.max = mediaPlayer?.duration ?: 0

        val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
                playerDuration.text = convertFormat(mediaPlayer?.currentPosition ?: 0)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        }
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener)

        // Update SeekBar progress and duration text
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                seekBar.progress = mediaPlayer?.currentPosition ?: 0
                playerDuration.text = convertFormat(mediaPlayer?.currentPosition ?: 0)
                handler.postDelayed(this, 1000) // Update every second
            }
        }, 0)
    }

    @SuppressLint("DefaultLocale")
    private fun convertFormat(duration: Int): String {
        return String.format("%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(duration.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration.toLong())))
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun restartService() {
        val isDetectorActive = MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.isDetectorActive, false)
        if (isDetectorActive) {
            DetectorWorkerStarter.requestStop(
                this@ImportSoundActivity,
                "ImportSoundActivity.restartService"
            )
            DetectorWorkerStarter.requestStart(
                this@ImportSoundActivity,
                "ImportSoundActivity.restartService"
            )
        } else {
            Log.d(TAG, "restartService: Nothing Happens")
        }
    }

    private var toast: Toast? = null

    fun showSingleToast( message: String) {
        toast?.cancel()  // Cancel any previous Toast
        toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast?.show()
    }
}
