package com.base.find_phone_clap_detector.utils

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.base.find_phone_clap_detector.databinding.SettingDialogueBinding
import com.base.find_phone_clap_detector.R

class NotificationPermissionUtil(private val caller: ActivityOrFragment) {

    private var onGranted: (() -> Unit)? = null

    // Launcher works for both Activity and Fragment
    private val permissionLauncher = caller.getActivityResultCaller().registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onGranted?.invoke()
        } else {
            // If user selected "Don't ask again"
            if (!caller.shouldShowRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                showSettingsDialog(caller.getContext())
            }
        }
    }

    fun checkAndRequest(onPermissionGranted: () -> Unit) {
        onGranted = onPermissionGranted

        val context = caller.getContext()

        // Below Android 13 → auto-granted
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onPermissionGranted.invoke()
            return
        }

        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED -> onPermissionGranted.invoke()
            else -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showSettingsDialog(context: Context) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = SettingDialogueBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setCancelable(false)

        val width = (context.resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        binding.tvPermissionDesc.text =
            context.getString(R.string.post_notification_permission_is_required_for_find_phone_using_clap_please_grant_the_permission_in_the_app_settings)

        binding.closeBtn.setOnClickListener { dialog.dismiss() }
        binding.yes.setOnClickListener {
            dialog.dismiss()
            openAppSettings(context)
        }

        try { dialog.show() } catch (e: Exception) { e.printStackTrace() }
    }

    private fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
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