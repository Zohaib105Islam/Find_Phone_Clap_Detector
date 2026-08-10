package com.base.find_phone_clap_detector.utils

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.content.pm.PackageManager
import com.base.find_phone_clap_detector.databinding.SettingDialogueBinding
import com.base.find_phone_clap_detector.R

class StoragePermissionUtil(fragment: Fragment) {

    private val fragmentRef: Fragment = fragment
    private val context: Context = fragment.requireContext()

    private var onGranted: (() -> Unit)? = null

    // Launcher for single permission (we will launch dynamically)
    private val permissionLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onGranted?.invoke()
        } else {
            if (!fragment.shouldShowRequestPermissionRationale(currentPermission)) {
                showSettingsDialog()
            }
        }
    }

    private var currentPermission: String = ""

    /**
     * Call this function with a callback to request storage/audio permissions.
     * Will handle Android 13+ Media permissions automatically.
     */
    fun checkAndRequestPermissions(onPermissionGranted: () -> Unit) {
        onGranted = onPermissionGranted

        val permissions = mutableListOf<String>()

        // Android 13+ separate permissions for audio/media
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        requestNextPermission(permissions)
    }

    // Recursive function to request one permission at a time
    private fun requestNextPermission(permissions: MutableList<String>) {
        if (permissions.isEmpty()) {
            onGranted?.invoke()
            return
        }

        currentPermission = permissions.removeAt(0)

        // Already granted?
        if (ContextCompat.checkSelfPermission(context, currentPermission) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNextPermission(permissions)
        } else {
            // Request this permission
            permissionLauncher.launch(currentPermission)
        }
    }

    private fun showSettingsDialog() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = SettingDialogueBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setCancelable(false)

        val width = (context.resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        binding.tvPermissionDesc.text =
            fragmentRef.getString(R.string.storage_and_audio_recording_permission_is_required_for_find_phone_using_clap_please_grant_the_permission_in_the_app_settings)

        binding.closeBtn.setOnClickListener { dialog.dismiss() }
        binding.yes.setOnClickListener {
            dialog.dismiss()
            openAppSettings()
        }

        try {
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}