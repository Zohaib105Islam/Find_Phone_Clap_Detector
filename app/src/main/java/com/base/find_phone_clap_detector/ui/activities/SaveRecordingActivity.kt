package com.base.find_phone_clap_detector.ui.activities

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivitySaveRecordingBinding
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.ui.dataClasses.SoundsDataClass
import com.base.find_phone_clap_detector.utils.DetectorWorkerStarter
import com.base.find_phone_clap_detector.utils.KEY
import com.base.find_phone_clap_detector.utils.Language
import com.base.find_phone_clap_detector.utils.LocaleHelper
import com.base.find_phone_clap_detector.utils.TinyDB
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SaveRecordingActivity : AppCompatActivity() {

    private var filepath = ""
    private lateinit var binding: ActivitySaveRecordingBinding
    private var mediaPlayer: MediaPlayer? = null

    private var savedAudioUri: String? = null
    private val tinyDB: TinyDB = TinyDB(MyApplication.appContext)
    private var uriString = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply current app locale before view inflation
        LocaleHelper.setAppLocale(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySaveRecordingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.main.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        intiViews()
        startOrPauseMediaPlayer(filepath)
    }

    private fun intiViews() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.backColor)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        filepath = intent.getStringExtra("filepath").toString()
        Log.d(TAG, "GETTED_FILE_PATH: $filepath")

        binding.linearLayout3.setOnClickListener {
            binding.nameText.requestFocus()

            // Show keyboard manually
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.nameText, InputMethodManager.SHOW_IMPLICIT)
        }

        binding.saveApplyBtn.setOnClickListener {
            MyApplication.isFromMain = false
            if (MyApplication.mInstance.byCreateAudioService) {
                MyApplication.mInstance.byCreateAudioServiceActivated = true
                MyApplication.mInstance.byCreateAudioService = false
            }
            soundUriRecording()
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.appliedSoundUri,
                savedAudioUri
            )
            restartService()
        }

        binding.saveSoundBtn.setOnClickListener {
            soundUriRecording()
        }

        binding.backIcon.setOnClickListener {
            finish()
        }
    }

    private fun soundUriRecording() {
        // save sound to music directory
        val currentDateTime =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${binding.nameText.text.toString()}_${currentDateTime}.mp3"

        savedAudioUri = saveAudioToMusicDirectory(filepath, fileName)
        uriString = savedAudioUri.toString()
//        val titleResId = resources.getIdentifier(fileName, "string", packageName)
        val model = SoundsDataClass(fileName, R.drawable.img_rv, true, savedAudioUri.toString())
        val favoriteItems = tinyDB.getListObject(KEY.sounds, SoundsDataClass::class.java)

        if (favoriteItems.any { it.audioUri == model.audioUri }) {
            Toast.makeText(this, "Already in Sounds...", Toast.LENGTH_SHORT).show()
        } else {
            favoriteItems.add(model)
            tinyDB.putListObject(KEY.sounds, favoriteItems)
            Toast.makeText(this, "${binding.nameText.text.toString()} Applied", Toast.LENGTH_SHORT)
                .show()
            startActivity(Intent(this@SaveRecordingActivity, CreateSoundActivity::class.java))
            finish()
        }
    }

    private fun startOrPauseMediaPlayer(filepath: String) {
        // Initialize MediaPlayer if null
        mediaPlayer = MediaPlayer()
        try {
            Log.d(TAG, "MediaPlayer: PREPARED & START")
            mediaPlayer?.setDataSource(filepath)
            Log.d(TAG, "MediaPlayer: $filepath")
            mediaPlayer?.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Play or pause MediaPlayer
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            binding.playBtn.visibility = View.VISIBLE
            binding.pauseBtn.visibility = View.GONE
        } else {
            mediaPlayer?.start()
            binding.playBtn.visibility = View.GONE
            binding.pauseBtn.visibility = View.VISIBLE
        }

        // Set up play/pause buttons in the dialog
        binding.pauseBtn.setOnClickListener {
            mediaPlayer?.pause()
            binding.playBtn.visibility = View.VISIBLE
            binding.pauseBtn.visibility = View.GONE
        }
        binding.playBtn.setOnClickListener {
            mediaPlayer?.start()
            binding.playBtn.visibility = View.GONE
            binding.pauseBtn.visibility = View.VISIBLE
        }

        // Set up stop button and close button in the dialog
        binding.stopBtn.setOnClickListener {
            mediaPlayer?.release()
            mediaPlayer = null
        }

        // Set up SeekBar in the dialog
        binding.seekBar.max = mediaPlayer?.duration ?: 0

        val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    Log.d(TAG, "onProgressChanged: media player progress $progress")
                }
                binding.playerDuration.text = convertFormat(mediaPlayer?.currentPosition ?: 0)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        }
        binding.seekBar.setOnSeekBarChangeListener(seekBarChangeListener)

        // Update SeekBar progress and duration text
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    binding.seekBar.progress = mediaPlayer?.currentPosition ?: 0
                    binding.playerDuration.text = convertFormat(mediaPlayer?.currentPosition ?: 0)

                    if (mediaPlayer?.isPlaying == true) {
                        binding.playBtn.visibility = View.GONE
                        binding.pauseBtn.visibility = View.VISIBLE
                    } else {
                        binding.playBtn.visibility = View.VISIBLE
                        binding.pauseBtn.visibility = View.GONE
                    }
                    handler.postDelayed(this, 1000) // Update every second
                } catch (e: Exception) {
                    Log.d(TAG, "run: ERROR ${e.printStackTrace()}")
                }
            }
        }, 0)
    }

    @SuppressLint("DefaultLocale")
    private fun convertFormat(duration: Int): String {
        return String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(duration.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration.toLong()))
        )
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

    private fun saveAudioToMusicDirectory(filepath: String, fileName: String): String? {
        try {
            val externalFile = File(filepath)
            val inputStream = FileInputStream(externalFile)

            val musicDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val customMusicDirectory = File(musicDirectory, "FindPhoneClapMusic")

            if (!customMusicDirectory.exists()) {
                customMusicDirectory.mkdirs()
            }

            val outputFile = File(customMusicDirectory, fileName)

            val outputStream = FileOutputStream(outputFile)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            return outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun restartService() {
        val isDetectorActive = MyApplication.mInstance.preferenceManager.getBoolean(
            PreferenceManager.Key.isDetectorActive,
            false
        )
        if (isDetectorActive) {
            DetectorWorkerStarter.requestStop(
                this@SaveRecordingActivity,
                "SaveRecordingActivity.restartService"
            )
            DetectorWorkerStarter.requestStart(
                this@SaveRecordingActivity,
                "SaveRecordingActivity.restartService"
            )
        } else {
            Log.d(TAG, "restartService: Nothing Happens")
        }
    }
}
