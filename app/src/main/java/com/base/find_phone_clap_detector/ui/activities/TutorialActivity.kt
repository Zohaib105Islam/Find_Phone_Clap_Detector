package com.base.find_phone_clap_detector.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivityTutorialBinding
import com.base.find_phone_clap_detector.managers.AdsManager
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.utils.Language
import com.base.find_phone_clap_detector.utils.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TutorialActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityTutorialBinding.inflate(layoutInflater)
    }
    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val detectorThreshold = 20000
    private var isClapDetected = false
    private var audioRecorder: AudioRecord?= null
    private var clapCounter = 0
    private var lastClapTime = System.currentTimeMillis()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var clapJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply current app locale before view inflation
        LocaleHelper.setAppLocale(this)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.main.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        initViews()
        startDetectClap()
        initAds()
    }

    private fun startDetectClap() {
        try {
            bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (bufferSize <= 0) return

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            audioRecorder = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize)
            if (audioRecorder?.state != AudioRecord.STATE_INITIALIZED) return

            audioRecorder?.startRecording()

            clapJob = coroutineScope.launch(Dispatchers.IO) {
                while (isActive) {
                    val buffer = ShortArray(bufferSize)
                    val readResult = audioRecorder?.read(buffer, 0, buffer.size) ?: 0

                    if (readResult > 0) {
                        // Calculate the decibels of the audio
                        val amplitude = calculateAmplitude(buffer)
                        val decibels = 20 * Math.log10(amplitude.toDouble())

                        // Log decibels
                        Log.d("TAG", "Current Decibels: $decibels dB")

                        withContext(Dispatchers.Main) {
                            if (decibels >= 50) {
                                binding.textNotice.visibility = View.VISIBLE
                            } else {
                                binding.textNotice.visibility = View.GONE
                            }
                        }

                        // Log clap detection
                        if (detectClap(buffer)) {
                            Log.d("TAG", "Clap detected!")
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastClapTime <= 1000) {
                                clapCounter++
                                Log.d("TAG", "Before Double check!")
                                if (clapCounter >= 2 && !isClapDetected) {
                                    stopClapDetection()
                                    isClapDetected = true
                                    gotoMainActivity()
                                }
                            } else {
                                clapCounter = 1
                            }
                            lastClapTime = currentTime
                        } else {
                            isClapDetected = false
                            clapCounter = 0
                        }
                    }
                    delay(100)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun gotoMainActivity() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                binding.textNotice.visibility = View.GONE
                binding.animationViewRippleOn.visibility = View.GONE
                binding.lottieAnimationView2.visibility = View.GONE
                binding.lottieAnimationView3.visibility = View.VISIBLE
            }
            delay(3000)
            MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.IS_APP_FIRST_TIME, false)
            val premiumCheck = MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_PREMIUM, false)
            val intent2: Intent = if (premiumCheck) {
                Intent(this@TutorialActivity, MainActivity::class.java)
            } else {
                Intent(this@TutorialActivity, PremiumScreenActivity::class.java)
            }
            withContext(Dispatchers.Main) {
                val intentArray = arrayOf(Intent(this@TutorialActivity, MainActivity::class.java), intent2)
                startActivities(intentArray)
                finish()
            }
        }
    }

    private fun calculateAmplitude(buffer: ShortArray): Int {
        var sum = 0.0
        for (sample in buffer) {
            sum += Math.abs(sample.toDouble())
        }
        return (sum / buffer.size).toInt()
    }

    private fun detectClap(audioData: ShortArray): Boolean {
        for (sample in audioData) {
            if (Math.abs(sample.toInt()) > detectorThreshold) {
                return true
            }
        }
        return false
    }

    private fun initAds() {
        // loadNativeAd
        MyApplication.mInstance.adsManager.loadNativeAd(
            this,
            binding.adFrame,
            AdsManager.NativeAdType.NOMEDIA_MEDIUM,
            this.getString(R.string.ADMOB_NATIVE_WITHOUT_MEDIA_V2),
            binding.shimmerLayout
        )
    }

    private fun initViews() {
        binding.apply {
skipBtn.setOnClickListener {
    gotoMainActivity()
}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopClapDetection()
    }

    private fun stopClapDetection() {
        try {
            audioRecorder?.stop()
            audioRecorder?.release()
        }catch (e:Exception){
            e.printStackTrace()
        }

    }
}
