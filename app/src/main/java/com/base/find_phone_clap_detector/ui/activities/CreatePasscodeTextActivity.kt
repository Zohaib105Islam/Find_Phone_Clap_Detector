package com.base.find_phone_clap_detector.ui.activities

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.horse.identification.extensions.showPasscodeSavedDialog
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivityCreatePasscodeTextBinding
import com.base.find_phone_clap_detector.managers.PreferenceManager.Key
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.utils.disableMultipleClicking
import java.util.Locale

class CreatePasscodeTextActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val binding by lazy {
        ActivityCreatePasscodeTextBinding.inflate(layoutInflater)
    }
    private lateinit var tts: TextToSpeech

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
        initListeners()
    }

    private fun initListeners() {

        binding.backBtn.setOnClickListener {
            disableMultipleClicking(it, 1000)
            onBackPressed()
        }

        binding.recordedText .addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.buttonsLay.visibility = if (!s.isNullOrBlank()) View.VISIBLE else View.GONE
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

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
}