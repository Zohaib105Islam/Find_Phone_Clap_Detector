package com.base.find_phone_clap_detector.ui

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import com.base.find_phone_clap_detector.myApplication.MyApplication
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.managers.PreferenceManager

class ClapDetectorActivity : AppCompatActivity() {

    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private val detectorThreshold = 29000 // Adjust this threshold as needed
    private var isClapDetected = false

    private lateinit var audioRecorder: AudioRecord

    private var clapCounter = 0
    private var lastClapTime = System.currentTimeMillis()
    private var uri = Uri.parse(MyApplication.mInstance.preferenceManager.getString(PreferenceManager.Key.appliedSoundUri , "null"))
    private lateinit var mediaPlayer: MediaPlayer
    private var checkLoop = MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.checkLoop , false)
    private var isPlaying = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clap_detector)
        val clapStatusTextView = findViewById<TextView>(R.id.clap_text)
        val soundImageView = findViewById<ImageView>(R.id.soundImageView)

        // Check and request microphone permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
        }

        if (uri.toString() == "null") {
            mediaPlayer = MediaPlayer.create(this@ClapDetectorActivity, R.raw.doorbell_sound)
        } else {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@ClapDetectorActivity, uri)
                prepare()
            }
        }

        mediaPlayer.setOnCompletionListener {
            if (checkLoop) {
                mediaPlayer.start()
            } else {
                stopAudio()
            }
        }

        try {
            // Determine minimum buffer size for safety
            val minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                channelConfig,
                audioFormat
            )

            if (minBufferSize <= 0) {
                // Invalid parameters, cannot initialize
                Log.e("ClapDetector", "Invalid audio parameters: minBufferSize=$minBufferSize")
                Toast.makeText(this, "Cannot initialize microphone", Toast.LENGTH_SHORT).show()
            } else {
                // Initialize AudioRecord
                audioRecorder = AudioRecord(
                    audioSource,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufferSize
                )

                if (audioRecorder.state == AudioRecord.STATE_INITIALIZED) {
                    // Start recording safely
                    audioRecorder.startRecording()
                    Log.d("ClapDetector", "AudioRecord started successfully")
                } else {
                    Log.e("ClapDetector", "AudioRecord failed to initialize")
                    Toast.makeText(this, "Microphone initialization failed", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            // Catch any unexpected exception
            Log.e("ClapDetector", "Error starting AudioRecord: ${e.localizedMessage}")
            Toast.makeText(this, "Unable to start microphone", Toast.LENGTH_SHORT).show()
        }

        val handler = Handler()

        val clapDetectionRunnable = object : Runnable {
            override fun run() {
                val buffer = ShortArray(bufferSize)
                audioRecorder.read(buffer, 0, bufferSize)

                if (detectClap(buffer)) {
                    if (!isClapDetected) {
                        clapCounter++
                        clapStatusTextView.text = "Clap Detected $clapCounter"
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClapTime <= 600) { // Check within 1.2 seconds
                            clapCounter++
                            if (clapCounter >= 2) {
                                isClapDetected = true
                                clapStatusTextView.text = "Clap Detected $clapCounter"
                                soundImageView.visibility = View.VISIBLE
                                playAudio()
                                // Perform actions when two claps are detected
                            }
                        } else {
                            clapCounter = 1 // Start counting from 1 if more than 1.2 seconds elapsed
                        }
                        lastClapTime = currentTime
                    }
                } else {
                    isClapDetected = false
                    clapCounter = 0
                    clapStatusTextView.text = "Clap Not Detected"
                }

                handler.post(this)
            }
        }

        handler.post(clapDetectionRunnable)

        soundImageView.setOnClickListener {
            soundImageView.visibility = View.GONE
            stopAudio()
        }
    }

    private fun detectClap(audioData: ShortArray): Boolean {
        for (sample in audioData) {
            if (Math.abs(sample.toInt()) > detectorThreshold) {
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder.release()
    }

    private fun stopAudio() {
        if (isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.prepareAsync()
            isPlaying = false
        }
    }

    private fun playAudio() {
        if (!isPlaying) {
            mediaPlayer.start()
            isPlaying = true
        }
    }
}