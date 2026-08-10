package com.base.find_phone_clap_detector.service.helpers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.base.find_phone_clap_detector.managers.PreferenceManager.Key
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.service.DetectorLog
import java.util.Locale
class PasscodeDetector(
    private val context: Context,
    private val packageName: String,
    private val onPasscodeMatched: () -> Unit
) {
    private companion object {
        private const val TAG = "PasscodeDetector"
        private const val SPEECH_RESTART_DELAY_MS = 600L
        private const val RECOGNIZER_BUSY_RESTART_DELAY_MS = 1000L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var isListening = false

    fun start() {
        DetectorLog.d(TAG, "start: Initializing speech recognizer")
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            DetectorLog.e(TAG, "Speech recognition not available")
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                putExtra("android.speech.extra.DICTATION_MODE", true)
                putExtra("android.speech.extra.PROMPT", "")
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    DetectorLog.d(TAG, "Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    isListening = true
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    restartListeningWithDelay()
                }

                override fun onError(error: Int) {
                    isListening = false
                    DetectorLog.d(TAG, "Speech error: $error")
                    if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        stopAndRestartRecognizer()
                    } else {
                        restartListeningWithDelay()
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val passcode = MyApplication.mInstance.preferenceManager.getString(
                        Key.PASSCODE_STRING,
                        "hello app"
                    )

                    matches?.forEach { result ->
                        DetectorLog.d(TAG, "Heard: $result")
                        if (result.contains(passcode, ignoreCase = true)) {
                            DetectorLog.d(TAG, "Passcode matched!")
                            onPasscodeMatched()
                            return
                        }
                    }

                    restartListeningWithDelay()
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        startListeningSafe()
    }

    fun destroy() {
        mainHandler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        speechRecognizer = null
        recognizerIntent = null
        isListening = false
    }

    private fun startListeningSafe() {
        try {
            if (!isListening) {
                speechRecognizer?.startListening(recognizerIntent)
                isListening = true
            } else {
                DetectorLog.d(TAG, "Already listening, skip starting again")
            }
        } catch (e: Exception) {
            DetectorLog.e(TAG, "startListeningSafe: ${e.message}", e)
        }
    }

    private fun stopAndRestartRecognizer() {
        try {
            speechRecognizer?.cancel()
            isListening = false
            mainHandler.postDelayed(
                { startListeningSafe() },
                RECOGNIZER_BUSY_RESTART_DELAY_MS
            )
        } catch (e: Exception) {
            DetectorLog.e(TAG, "stopAndRestartRecognizer: ${e.message}", e)
        }
    }

    private fun restartListeningWithDelay() {
        try {
            mainHandler.postDelayed(
                { startListeningSafe() },
                SPEECH_RESTART_DELAY_MS
            )
        } catch (e: Exception) {
            DetectorLog.e(TAG, "restartListeningWithDelay: ${e.message}", e)
        }
    }
}
