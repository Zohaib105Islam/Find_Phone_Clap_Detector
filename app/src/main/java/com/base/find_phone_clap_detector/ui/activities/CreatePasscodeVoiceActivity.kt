package com.base.find_phone_clap_detector.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.horse.identification.extensions.showPasscodeSavedDialog
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivityCreatePasscodeVoiceBinding
import com.base.find_phone_clap_detector.managers.PreferenceManager.Key
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.utils.PermissionsUtil
import com.base.find_phone_clap_detector.utils.PermissionsUtil.showCustomDialog
import com.base.find_phone_clap_detector.utils.disableMultipleClicking
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class CreatePasscodeVoiceActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val binding by lazy {
        ActivityCreatePasscodeVoiceBinding.inflate(layoutInflater)
    }

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var tts: TextToSpeech

    private var isListening = false
    private var isStoppedManually = false
    private var speechTimeoutJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        tts = TextToSpeech(this, this)

        initSpeechRecognizer()
        initListeners()

    }

    private fun initListeners() {

        binding.backBtn.setOnClickListener {
            disableMultipleClicking(it, 1000)
            onBackPressed()
        }

        binding.recordIcon.setOnClickListener {
            disableMultipleClicking(it, 1000)
            checkPermissionStartListening()
        }

        binding.listenBtn.setOnClickListener {
            disableMultipleClicking(it, 1000)
            speakOut(binding.recordedText.text.toString())
        }

        binding.saveBtn.setOnClickListener {
            disableMultipleClicking(it, 1000)
            MyApplication.mInstance.preferenceManager.put(Key.PASSCODE_STRING, binding.recordedText.text.toString())
            showPasscodeSavedDialog{
                finish()
            }
        }

    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val spokenText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0) ?: ""
                val trimmedText = spokenText.take(20)
                binding.recordedText.text = trimmedText
                stopUI()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0) ?: ""
                val limited = partial.take(20)
                binding.recordedText.text = limited
                if (limited.length >= 20 && !isStoppedManually) {
                    speechRecognizer.stopListening()
                    stopUI()
                }
            }

            override fun onBeginningOfSpeech() {
                binding.recordedText.text = "Listening..."
            }

            override fun onError(error: Int) {
                stopUI(true)
                binding.recordedText.text = "Error: $error"
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun checkPermissionStartListening(){
        requestMicPermissions {
            if (!isListening) {
                startListening()
            }
        }
    }

    private fun startListening() {

        binding.buttonsLay.visibility = View.GONE
        binding.recordIcon.visibility = View.GONE
        binding.recordingAnimation.visibility = View.VISIBLE
        binding.instructionText.text = getString(R.string.tap_to_complete)

        isListening = true
        isStoppedManually = false
        speechRecognizer.startListening(recognizerIntent)

        binding.recordedText.text = "Listening..."
        speechTimeoutJob = lifecycleScope.launch {
            delay(5000) // max wait
            if (!isStoppedManually) {
                speechRecognizer.stopListening()
                stopUI()
            }
        }
    }

    private fun stopUI(isError: Boolean = false) {

        if (isError) binding.buttonsLay.visibility = View.GONE
        else binding.buttonsLay.visibility = View.VISIBLE
        binding.recordIcon.visibility = View.VISIBLE
        binding.recordingAnimation.visibility = View.GONE
        binding.instructionText.text = getString(R.string.tap_to_record)
        binding.textView22.text = getString(R.string.record_your_voice_passcode)

        isListening = false
        isStoppedManually = true
        speechTimeoutJob?.cancel()
        speechTimeoutJob = null
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
        speechTimeoutJob?.cancel()
        speechRecognizer.destroy()
    }
    /**
     * TEXT TO SPEECH
     */

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "TTS language not supported", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speakOut(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    /**
     * Permissions
     */

    private fun requestMicPermissions(onPermissionGiven: ()-> Unit) {
            if (!PermissionsUtil.allPermissionsGrantedMic(this)) {
               val permissionsArray = PermissionsUtil.REQUIRED_PERMISSIONS_MIC
                permissionLauncherCam.launch(permissionsArray)
            }else{
                onPermissionGiven.invoke()
            }
    }

    private val permissionLauncherCam = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val grantedPermissions = permissions.filter { it.value }.map { it.key }
        val deniedPermissions = permissions.filterNot { it.value }.map { it.key }

        if (grantedPermissions.isNotEmpty()) {
            if (grantedPermissions.size == permissions.size) {
                checkPermissionStartListening()
            }

        }
        if (deniedPermissions.isNotEmpty()) {
            if (deniedPermissions.all {
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        it
                    )
                }
            ) {
                showCustomDialog(this)
            } else {
                checkPermissionStartListening()
            }
        }
    }
}