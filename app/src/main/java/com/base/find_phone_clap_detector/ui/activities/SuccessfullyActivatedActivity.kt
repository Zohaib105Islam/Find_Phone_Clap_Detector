package com.base.find_phone_clap_detector.ui.activities

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.horse.identification.extensions.disableMultipleClicking
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivitySuccessfullyActivatedBinding
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.utils.DetectorWorkerStarter

class SuccessfullyActivatedActivity : AppCompatActivity() {

    private val binding: ActivitySuccessfullyActivatedBinding by lazy {
        ActivitySuccessfullyActivatedBinding.inflate(layoutInflater)
    }

    private val pref by lazy {
        MyApplication.mInstance.preferenceManager
    }

    private var isVibrationActive =
        pref.getBoolean(PreferenceManager.Key.isVibrationActive, true)

    private var isFlashActive =
        pref.getBoolean(PreferenceManager.Key.isFlashActive, true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        initClicks()
        initToggleListeners()
    }

    // ================= DEFAULT STATE FROM PREF =================
    private fun initViews() {
        setToggleColor(binding.toggleVibration)
        setToggleColor(binding.toggleFlash)

        updateVibrationUI(isVibrationActive)
        updateFlashUI(isFlashActive)
    }

    // ================= TOGGLE CLICK LISTENERS =================
    private fun initToggleListeners() {

        binding.toggleVibration.setOnCheckedChangeListener { _, isChecked ->
            isVibrationActive = isChecked
            // Save in preferences
            pref.put(
                PreferenceManager.Key.isVibrationActive,
                isChecked
            )

            updateVibrationUI(isChecked)
            restartService()
        }

        binding.toggleFlash.setOnCheckedChangeListener { _, isChecked ->
            isFlashActive = isChecked
            // Save in preferences
            pref.put(
                PreferenceManager.Key.isFlashActive,
                isChecked
            )
            updateFlashUI(isChecked)
            restartService()
        }
    }

    // ================= VIBRATION UI =================
    private fun updateVibrationUI(isActive: Boolean) {

        if (isActive) {
            binding.vibrateCard.setBackgroundResource(
                R.drawable.bg_vibration_flash_selected
            )
            binding.ivVibration.setColorFilter(
                ContextCompat.getColor(this, R.color.purple_dark),
                PorterDuff.Mode.SRC_IN
            )
            binding.toggleVibration.isChecked = true

        } else {
            binding.vibrateCard.setBackgroundResource(
                R.drawable.bg_vibration_flash_unselected
            )
            binding.ivVibration.setColorFilter(
                ContextCompat.getColor(this, R.color.gray_icon),
                PorterDuff.Mode.SRC_IN
            )
            binding.toggleVibration.isChecked = false
        }
    }

    // ================= FLASH UI =================
    private fun updateFlashUI(isActive: Boolean) {

        if (isActive) {

            binding.flashCard.setBackgroundResource(
                R.drawable.bg_vibration_flash_selected
            )

            binding.ivFlash.setColorFilter(
                ContextCompat.getColor(this, R.color.purple_dark),
                PorterDuff.Mode.SRC_IN
            )

            binding.toggleFlash.isChecked = true

        } else {

            binding.flashCard.setBackgroundResource(
                R.drawable.bg_vibration_flash_unselected
            )

            binding.ivFlash.setColorFilter(
                ContextCompat.getColor(this, R.color.gray_icon),
                PorterDuff.Mode.SRC_IN
            )

            binding.toggleFlash.isChecked = false
        }
    }

    // ================= SWITCH COLOR =================
    private fun setToggleColor(toggle: androidx.appcompat.widget.SwitchCompat) {

        val purple = ContextCompat.getColor(this, R.color.purple_dark)
        val gray = ContextCompat.getColor(this, R.color.gray_light)
        val white = ContextCompat.getColor(this, R.color.white)

        toggle.trackTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                purple,
                gray
            )
        )

        toggle.thumbTintList = ColorStateList.valueOf(white)
    }

    // ================= BACK CLICKS =================
    private fun initClicks() {
        binding.apply {

            backIcon.setOnClickListener {
                disableMultipleClicking(it)
                finish()
            }

            goBack.setOnClickListener {
                disableMultipleClicking(it)
                finish()
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
                this@SuccessfullyActivatedActivity,
                "SuccessfullyActivatedActivity.restartService"
            )
            DetectorWorkerStarter.requestStart(
                this@SuccessfullyActivatedActivity,
                "SuccessfullyActivatedActivity.restartService"
            )
        } else {
            Log.d(TAG, "restartService: Nothing Happens")
        }
    }
}
