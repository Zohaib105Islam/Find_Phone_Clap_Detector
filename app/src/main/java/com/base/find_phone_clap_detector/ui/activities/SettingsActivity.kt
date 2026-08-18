package com.base.find_phone_clap_detector.ui.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import com.base.find_phone_clap_detector.myApplication.MyApplication
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.RatingBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivitySettingsBinding
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.utils.DetectorWorkerStarter
import com.base.find_phone_clap_detector.utils.Language
import com.base.find_phone_clap_detector.utils.LocaleHelper
import com.base.find_phone_clap_detector.utils.disableMultipleClicking
import com.base.find_phone_clap_detector.utils.gone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    // toggles pref
    private var isSettingFlashActive = MyApplication.mInstance.preferenceManager.getBoolean(
        PreferenceManager.Key.isFlashActive,
        true
    )
    private var isSettingVibrationActive = MyApplication.mInstance.preferenceManager.getBoolean(
        PreferenceManager.Key.isVibrationActive,
        true
    )

    // flash modes prefs
    private var isFlashDefault = MyApplication.mInstance.preferenceManager.getBoolean(
        PreferenceManager.Key.isFlashDefault,
        true
    )
    private var isFlashDisco = MyApplication.mInstance.preferenceManager.getBoolean(
        PreferenceManager.Key.isFlashDisco,
        false
    )
    private var isFlashSos = MyApplication.mInstance.preferenceManager.getBoolean(
        PreferenceManager.Key.isFlashSos,
        false
    )

    // vibration modes prefs
    private var isVibrationDefault = MyApplication.mInstance.preferenceManager.getBoolean(
        PreferenceManager.Key.isVibrationDefault,
        true
    )
    private var isVibrationStrong = MyApplication.mInstance.preferenceManager.getBoolean(
        PreferenceManager.Key.isVibrationStrong,
        false
    )
    private var isVibrationHeart = MyApplication.mInstance.preferenceManager.getBoolean(
        PreferenceManager.Key.isVibrationHeart,
        false
    )

    // flash management
    private var isFlashlightOn = false
    private var isDiscoModeOn = false
    private lateinit var cameraManager: CameraManager
    var cameraId: String = ""
    private lateinit var discoThread: Thread
    private var flashlightJob: Job? = null

    // vibration management
    private lateinit var vibrator: Vibrator
    private var vibrationJob: Job? = null
    private var languageText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply current app locale before view inflation
        LocaleHelper.setAppLocale(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.main.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator // Initialize vibrator
        initLanguageName()
        initViews()
        initToggleButtons()
        initGeneralViews()
    }

    private fun initLanguageName() {

        // Load selected language from your real PreferenceManager
        val selectedLanguage = MyApplication.mInstance.preferenceManager
            .getString(PreferenceManager.Key.APP_LANGUAGE, "English") ?: "English"

        languageText = selectedLanguage
        binding.textLangauge.text = languageText

        // Hide Pro if user is premium
        if (MyApplication.mInstance.preferenceManager
                .getBoolean(PreferenceManager.Key.IS_APP_PREMIUM, false)
        ) {
            binding.csPro.gone()
        }
    }

    private fun initGeneralViews() {
        binding.feedback.setOnClickListener {
            MyApplication.isGeneral = true
            MyApplication.isOutForRating = true
            val supportEmail = "iobitsofficial@gmail.com" // Replace with your support email address
            val subject = "Feedback"
            val feedback = "Find Phone Via Clap"
            this.showEmailChooser(supportEmail, subject, feedback)
        }

        binding.rateUs.setOnClickListener {
            disableMultipleClicking(it, 1000)
            MyApplication.isGeneral = true
            MyApplication.isOutForRating = true
            showRatingDialogue()
        }

        binding.privacyPolicy.setOnClickListener {
            disableMultipleClicking(it, 1000)
            MyApplication.isGeneral = true
            MyApplication.isOutForRating = true
            try {
                val url =
                    "https://iobitsofficial.blogspot.com/2023/12/privacy-policy-for-find-phone-by-clap.html"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
            }
        }

        binding.shareBtn.setOnClickListener {
            MyApplication.isOutForRating = true
            disableMultipleClicking(it, 1000)
            MyApplication.isGeneral = true
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "👏 Clap and Locate Your Lost Phone Instantly with 'Find Phone Via Clap' app! " +
                            "\n\"🔊 Clap Detection for Effortless Finding 🔍\" " +
                            "\n\"📡 Quick, Accurate, and User-Friendly 📱\" " +
                            "\n\"🛡️ Never Lose Your Phone Again! 🛡️\" " +
                            "\n\"🎉 Experience the Ultimate Phone Finding Solution! 🎉\" " +
                            "\n\n📲 Tap here to download now: https://play.google.com/store/apps/details?id=com.phonefinder.findmyphone.clapflash"
                )

            }
            val shareIntent = Intent.createChooser(sendIntent, "Share via")
            startActivity(shareIntent)
        }

        binding.customerSupport.setOnClickListener {
            MyApplication.isOutForRating = true
            disableMultipleClicking(it, 1000)
            MyApplication.isGeneral = true
            val premiumCheck = MyApplication.mInstance?.preferenceManager?.getBoolean(
                PreferenceManager.Key.IS_APP_PREMIUM,
                false
            )
            if (premiumCheck == true) {
                val supportEmail =
                    "iobitsofficial@gmail.com" // Replace with your support email address
                val subject = "Support"
                val feedback = "Find Phone Via Clap"
                this.showEmailChooser(supportEmail, subject, feedback)
            } else {
                startActivity(Intent(this@SettingsActivity, PremiumScreenActivity::class.java))
            }
        }
    }

    private fun initToggleButtons() {
        binding.flashToggleButton.isChecked = isSettingFlashActive
        if (isSettingFlashActive) {
            binding.flashToggleButton.thumbTintList =
                ColorStateList.valueOf(resources.getColor(R.color.primary))
            binding.flashToggleButton.trackTintList =
                ColorStateList.valueOf(resources.getColor(R.color.settings_switch_track_active))
            binding.flashMenuLayout.visibility = View.VISIBLE
        } else {
            binding.flashToggleButton.thumbTintList =
                ColorStateList.valueOf(resources.getColor(R.color.settings_switch_thumb_inactive))
            binding.flashToggleButton.trackTintList =
                ColorStateList.valueOf(resources.getColor(R.color.settings_switch_track_inactive))
            binding.flashMenuLayout.visibility = View.GONE
        }
        binding.flashToggleButton.apply {
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Switch is ON, change thumb and track color for active state
                    thumbTintList = ColorStateList.valueOf(resources.getColor(R.color.primary))
                    trackTintList = ColorStateList.valueOf(resources.getColor(R.color.settings_switch_track_active))
                    MyApplication.mInstance.preferenceManager.put(
                        PreferenceManager.Key.isFlashActive,
                        true
                    )
                    restartService()
                    binding.flashMenuLayout.visibility = View.VISIBLE
                } else {
                    // Switch is OFF, change thumb and track color for inactive state
                    thumbTintList = ColorStateList.valueOf(resources.getColor(R.color.settings_switch_thumb_inactive))
                    trackTintList = ColorStateList.valueOf(resources.getColor(R.color.settings_switch_track_inactive))
                    MyApplication.mInstance.preferenceManager.put(
                        PreferenceManager.Key.isFlashActive,
                        false
                    )
                    restartService()
                    binding.flashMenuLayout.visibility = View.GONE
                }
            }
        }

        binding.vibrateToggleButton.isChecked = isSettingVibrationActive
        if (isSettingVibrationActive) {
            binding.vibrateToggleButton.thumbTintList =
                ColorStateList.valueOf(resources.getColor(R.color.primary))
            binding.vibrateToggleButton.trackTintList =
                ColorStateList.valueOf(resources.getColor(R.color.settings_switch_track_active))
            binding.vibrationMenuLayout.visibility = View.VISIBLE
        } else {
            binding.vibrateToggleButton.thumbTintList =
                ColorStateList.valueOf(resources.getColor(R.color.settings_switch_thumb_inactive))
            binding.vibrateToggleButton.trackTintList =
                ColorStateList.valueOf(resources.getColor(R.color.settings_switch_track_inactive))
            binding.vibrationMenuLayout.visibility = View.GONE
        }
        binding.vibrateToggleButton.apply {
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Switch is ON, change thumb and track color for active state
                    thumbTintList = ColorStateList.valueOf(resources.getColor(R.color.primary))
                    trackTintList = ColorStateList.valueOf(resources.getColor(R.color.settings_switch_track_active))
                    MyApplication.mInstance.preferenceManager.put(
                        PreferenceManager.Key.isVibrationActive,
                        true
                    )
                    restartService()
                    binding.vibrationMenuLayout.visibility = View.VISIBLE
                } else {
                    // Switch is OFF, change thumb and track color for inactive state
                    thumbTintList = ColorStateList.valueOf(resources.getColor(R.color.settings_switch_thumb_inactive))
                    trackTintList = ColorStateList.valueOf(resources.getColor(R.color.settings_switch_track_inactive))
                    MyApplication.mInstance.preferenceManager.put(
                        PreferenceManager.Key.isVibrationActive,
                        false
                    )
                    restartService()
                    binding.vibrationMenuLayout.visibility = View.GONE
                }
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initViews() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding.backIcon.setOnClickListener {
            finish()
        }

        if (isFlashDefault) {
            binding.radioDefFlash.isChecked = true
            binding.radioDiscoFlash.isChecked = false
            binding.radioSosFlash.isChecked = false
        } else if (isFlashDisco) {
            binding.radioDefFlash.isChecked = false
            binding.radioDiscoFlash.isChecked = true
            binding.radioSosFlash.isChecked = false
        } else if (isFlashSos) {
            binding.radioDefFlash.isChecked = false
            binding.radioDiscoFlash.isChecked = false
            binding.radioSosFlash.isChecked = true
        }

        if (isVibrationDefault) {
            binding.radioDefVibrate.isChecked = true
            binding.radioStrongVibrate.isChecked = false
            binding.radioHeartVibrate.isChecked = false
        } else if (isVibrationStrong) {
            binding.radioDefVibrate.isChecked = false
            binding.radioStrongVibrate.isChecked = true
            binding.radioHeartVibrate.isChecked = false
        } else if (isVibrationHeart) {
            binding.radioDefVibrate.isChecked = false
            binding.radioStrongVibrate.isChecked = false
            binding.radioHeartVibrate.isChecked = true
        }

        binding.radioDefFlash.setOnClickListener {
            if (!binding.radioDefFlash.isChecked) return@setOnClickListener
            stopFlashMode()
            startDefFlashMode()
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isFlashDefault,
                true
            )
            MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.isFlashDisco, false)
            MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.isFlashSos, false)
        }

        binding.radioDiscoFlash.setOnClickListener {
            if (!binding.radioDiscoFlash.isChecked) return@setOnClickListener
            stopFlashMode()
            startDiscoFlashMode()
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isFlashDefault,
                false
            )
            MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.isFlashDisco, true)
            MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.isFlashSos, false)
        }

        binding.radioSosFlash.setOnClickListener {
            if (!binding.radioSosFlash.isChecked) return@setOnClickListener
            stopFlashMode()
            startSosFlashMode()
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isFlashDefault,
                false
            )
            MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.isFlashDisco, false)
            MyApplication.mInstance.preferenceManager.put(PreferenceManager.Key.isFlashSos, true)
        }

        binding.radioDefVibrate.setOnClickListener {
            stopVibration()
            startDefaultVibration()
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isVibrationDefault,
                true
            )
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isVibrationStrong,
                false
            )
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isVibrationHeart,
                false
            )
        }

        binding.radioStrongVibrate.setOnClickListener {
            stopVibration()
            startStrongVibration()
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isVibrationDefault,
                false
            )
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isVibrationStrong,
                true
            )
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isVibrationHeart,
                false
            )
        }

        binding.radioHeartVibrate.setOnClickListener {
            stopVibration()
            startHeartbeatVibration()
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isVibrationDefault,
                false
            )
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isVibrationStrong,
                false
            )
            MyApplication.mInstance.preferenceManager.put(
                PreferenceManager.Key.isVibrationHeart,
                true
            )
        }

        binding.subscriptionSettings.setOnClickListener {
            disableMultipleClicking(it, 1000)
            startActivity(Intent(this@SettingsActivity, PremiumScreenActivity::class.java))
        }

        binding.selectLanguage.setOnClickListener {
            disableMultipleClicking(it, 1000)
            startActivity(Intent(this@SettingsActivity, LanguageActivity::class.java))
        }

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        try {
            cameraId = getBackCameraId() ?: ""
            if (!isValidCameraId(cameraId)) {
                cameraId = cameraManager.cameraIdList.firstOrNull { it.toIntOrNull() != null } ?: ""
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getBackCameraId(): String? {
        return try {
            for (id in cameraManager.cameraIdList) {
                // Guard against bogus/non-numeric IDs some devices report
                if (id.isBlank() || id.toIntOrNull() == null) continue
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return id
                }
            }
            null
        } catch (e: Exception) {
            Log.e("Settings", "Error finding back camera: ${e.message}")
            null
        }
    }

    private fun isValidCameraId(id: String): Boolean {
        if (id.isBlank() || id.toIntOrNull() == null) return false
        return try {
            id in cameraManager.cameraIdList
        } catch (e: CameraAccessException) {
            false
        }
    }

    // Functions for different flashlight modes
    private fun startDefFlashMode() {
        isDiscoModeOn = true
        binding.radioDefFlash.isChecked = true
        binding.radioDiscoFlash.isChecked = false
        binding.radioSosFlash.isChecked = false
        discoThread = Thread {
            while (isDiscoModeOn) {
                toggleFlashlight()
                try {
                    Thread.sleep(800) // Adjust this value for the desired speed
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        discoThread.start()
        flashlightJob?.cancel()
        flashlightJob = CoroutineScope(Dispatchers.Main).launch {
            delay(3000)
            stopFlashMode()
        }
    }

    private fun startDiscoFlashMode() {
        isDiscoModeOn = true
        binding.radioDefFlash.isChecked = false
        binding.radioDiscoFlash.isChecked = true
        binding.radioSosFlash.isChecked = false
        discoThread = Thread {
            while (isDiscoModeOn) {
                toggleFlashlight()
                try {
                    Thread.sleep(100) // Adjust this value for the desired speed
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        discoThread.start()
        flashlightJob = CoroutineScope(Dispatchers.Main).launch {
            delay(3000)
            stopFlashMode()
        }
    }

    private fun startSosFlashMode() {
        isDiscoModeOn = true
        binding.radioDefFlash.isChecked = false
        binding.radioDiscoFlash.isChecked = false
        binding.radioSosFlash.isChecked = true
        discoThread = Thread {
            while (isDiscoModeOn) {
                toggleFlashlight()
                try {
                    Thread.sleep(400) // Adjust this value for the desired speed
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        discoThread.start()
        flashlightJob = CoroutineScope(Dispatchers.Main).launch {
            delay(3000)
            stopFlashMode()
        }
    }

    private fun stopFlashMode() {
        isDiscoModeOn = false
        isFlashlightOn = false

        if (::discoThread.isInitialized && discoThread.isAlive) {
            try {
                discoThread.interrupt()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        turnOffFlashlight()
    }

    private fun toggleFlashlight() {
        if (isFlashlightOn) {
            turnOffFlashlight()
        } else {
            turnOnFlashlight()
        }
        isFlashlightOn = !isFlashlightOn
    }

    private fun turnOnFlashlight() {
        try {
            if (!isValidCameraId(cameraId)) return
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                cameraManager.setTorchMode(cameraId, true)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            // Catches NumberFormatException too, since it's a subclass
            Log.e(TAG, "Invalid camera id for torch: $cameraId", e)
        }
    }

    private fun turnOffFlashlight() {
        try {
            val flashCameraId = getFlashSupportedCameraId()
            if (flashCameraId != null && isValidCameraId(flashCameraId)) {
                cameraManager.setTorchMode(flashCameraId, false)
            } else {
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "No flash available on this device",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    private fun getFlashSupportedCameraId(): String? {
        try {
            for (id in cameraManager.cameraIdList) {
                if (id.isBlank() || id.toIntOrNull() == null) continue
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return id
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return null
    }
    // Functions for different Vibration modes
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startDefaultVibration() {
        binding.radioDefVibrate.isChecked = true
        binding.radioStrongVibrate.isChecked = false
        binding.radioHeartVibrate.isChecked = false
        vibrationJob?.cancel()
        vibrationJob = CoroutineScope(Dispatchers.Main).launch {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
            delay(3000)
            stopVibration()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startStrongVibration() {
        binding.radioDefVibrate.isChecked = false
        binding.radioStrongVibrate.isChecked = true
        binding.radioHeartVibrate.isChecked = false
        vibrationJob?.cancel()
        val pattern =
            longArrayOf(0, 500, 500, 500, 500, 500) // Pattern for heartbeat-like vibration
        vibrationJob = CoroutineScope(Dispatchers.Main).launch {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            delay(3000)
            stopVibration()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startHeartbeatVibration() {
        binding.radioDefVibrate.isChecked = false
        binding.radioStrongVibrate.isChecked = false
        binding.radioHeartVibrate.isChecked = true
        vibrationJob?.cancel()
        val pattern =
            longArrayOf(0, 500, 200, 500, 200, 500) // Pattern for heartbeat-like vibration
        vibrationJob = CoroutineScope(Dispatchers.Main).launch {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            delay(3000)
            stopVibration()
        }
    }

    private fun stopVibration() {
        vibrator.cancel()
        vibrationJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFlashMode()
        stopVibration()
    }

    private fun showRatingDialogue() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.rating_dialog)
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.setCancelable(false)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        dialog.findViewById<View>(R.id.cancel).setOnClickListener {
            dialog.dismiss()
//            App.mInstance.preferenceManager.put(PreferencesManager.Key.SHOW_RATING_DIALOG,true)
        }
        dialog.findViewById<View>(R.id.submit).setOnClickListener {
            Toast.makeText(
                this,
                getString(R.string.thanks_message),
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
        }
        val simpleRatingBar: RatingBar =
            dialog.findViewById(R.id.ratingBar) // initiate a rating bar
        simpleRatingBar.onRatingBarChangeListener =
            RatingBar.OnRatingBarChangeListener { _, rating, _ ->
                // Called when the user swipes the RatingBar
                if (rating >= 4) {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.phonefinder.findmyphone.clapflash")
                    )
                    MyApplication.mInstance.preferenceManager.put(
                        PreferenceManager.Key.SHOW_RATING_DIALOG,
                        true
                    )
                    startActivity(intent)
                    dialog.dismiss()
                }
            }
        try {
            dialog.show()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun Context.showEmailChooser(supportEmail: String, subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }

        try {
            val chooser = Intent.createChooser(intent, "Send Email")
            if (chooser.resolveActivity(packageManager) != null) {
                startActivity(chooser)
            } else {
                Toast.makeText(this, "No email client found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No email client found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restartService() {
        val isDetectorActive = MyApplication.mInstance.preferenceManager.getBoolean(
            PreferenceManager.Key.isDetectorActive,
            false
        )
        if (isDetectorActive) {
            DetectorWorkerStarter.requestStop(
                this@SettingsActivity,
                "SettingsActivity.restartService"
            )
            DetectorWorkerStarter.requestStart(
                this@SettingsActivity,
                "SettingsActivity.restartService"
            )
        } else {
            Log.d(TAG, "restartService: Nothing Happens")
        }
    }
}
