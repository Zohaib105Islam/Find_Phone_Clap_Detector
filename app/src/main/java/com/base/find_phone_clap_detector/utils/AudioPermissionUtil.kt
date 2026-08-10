package com.base.find_phone_clap_detector.utils

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.base.find_phone_clap_detector.databinding.SettingDialogueBinding
import com.base.find_phone_clap_detector.R
import androidx.core.net.toUri

class AudioPermissionUtil(private val caller: ActivityOrFragment) {

    private var onGranted: (() -> Unit)? = null

    // Launcher works for both Activity and Fragment
    private val permissionLauncher = caller.getActivityResultCaller().registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onGranted?.invoke()
        } else {
            if (!caller.shouldShowRationale(Manifest.permission.RECORD_AUDIO)) {
                showSettingsDialog(caller.getContext())
            }
        }
    }

    fun checkAndRequest(onPermissionGranted: () -> Unit) {
        onGranted = onPermissionGranted

        val context = caller.getContext()

        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED -> onPermissionGranted.invoke()
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun showSettingsDialog(context: Context) {
        val dialog = Dialog(context)
        val binding = SettingDialogueBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (context.resources.displayMetrics.widthPixels * 0.90).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        binding.closeBtn.setOnClickListener { dialog.dismiss() }
        binding.yes.setOnClickListener {
            dialog.dismiss()
            openAppSettings(context)
        }

        try { dialog.show() } catch (e: Exception) { e.printStackTrace() }
    }

    private fun openAppSettings(context: Context) {
        val packageUri = "package:${context.packageName}".toUri()
        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val manageAppsIntent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pm = context.packageManager
        when {
            appDetailsIntent.resolveActivity(pm) != null -> context.startActivity(appDetailsIntent)
            manageAppsIntent.resolveActivity(pm) != null -> context.startActivity(manageAppsIntent)
            settingsIntent.resolveActivity(pm) != null -> context.startActivity(settingsIntent)
        }
    }

    // Helper interface to unify Activity/Fragment
    interface ActivityOrFragment {
        fun getContext(): Context
        fun getActivityResultCaller(): ActivityResultCaller
        fun shouldShowRationale(permission: String): Boolean
    }

    // Implementations for Activity or Fragment
    class FromActivity(private val activity: ComponentActivity) : ActivityOrFragment {
        override fun getContext(): Context = activity
        override fun getActivityResultCaller(): ActivityResultCaller = activity
        override fun shouldShowRationale(permission: String): Boolean =
            activity.shouldShowRequestPermissionRationale(permission)
    }

    class FromFragment(private val fragment: Fragment) : ActivityOrFragment {
        override fun getContext(): Context = fragment.requireContext()
        override fun getActivityResultCaller(): ActivityResultCaller = fragment
        override fun shouldShowRationale(permission: String): Boolean =
            fragment.shouldShowRequestPermissionRationale(permission)
    }
}
