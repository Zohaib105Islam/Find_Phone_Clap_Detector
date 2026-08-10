package com.base.find_phone_clap_detector.managers

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.myApplication.MyApplication

object AdEventManager{
    private var loadingDialog: AlertDialog? = null
     private val handler = Handler(Looper.getMainLooper())
    fun handleAdEvent(context: Activity, adEvent: AdState, func:()->Unit) {
        when(adEvent){
            AdState.InitAdCall -> {
                if(isInternetAvailable()){
                    showLoadingDialog(context)
                    scheduleDelayedCallback(func)
                }else{
                    func?.invoke()
                }

            }
            AdState.Loaded -> {
                cancelDelayedCallback()
//                func?.invoke()
            }
            AdState.Displayed -> {

                dismissLoadingDialog()

            }
            AdState.Hidden -> {
                func?.invoke()

            }
            AdState.Clicked -> {


            }
            AdState.LoadFailed -> {
                if(isInternetAvailable()){
                    dismissLoadingDialog()
                    func?.invoke()
                }
            }
            AdState.DisplayFailed -> {
                if(isInternetAvailable()){
                    dismissLoadingDialog()
                    func?.invoke()
                }

            }
        }

        }


    private fun showLoadingDialog(context: Context) {
        try{

            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.layout_ad_loading, null)

            // Set layout parameters to cover the entire screen
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            loadingDialog = AlertDialog.Builder(context,R.style.Base_Theme_FindMyPhoneViaClap)
                .setView(view)
                .setCancelable(false)
                .create()
            //    loadingDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            loadingDialog?.show()
        }catch (e:Exception){
            e.localizedMessage
        }
    }

    private fun dismissLoadingDialog() {
        try {
            loadingDialog?.dismiss()
        } catch (e:Exception){
            e.localizedMessage
        }
    }

    private fun scheduleDelayedCallback(function:()->Unit) {
        handler.postDelayed({
            dismissLoadingDialog()
            function?.invoke()
        }, 7000) // Delayed invocation after 6 seconds (6000 milliseconds)
    }

    private fun cancelDelayedCallback() {
        handler.removeCallbacksAndMessages(null)
    }
      fun isInternetAvailable(): Boolean {
        var result = false
        val connectivityManager =
            MyApplication.mInstance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        connectivityManager?.let {
            it.getNetworkCapabilities(connectivityManager.activeNetwork)?.apply {
                result = when {
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    else -> false
                }
            }
        }
        return result
    }
}