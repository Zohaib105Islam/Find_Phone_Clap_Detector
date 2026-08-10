package com.base.find_phone_clap_detector.managers

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.myApplication.MyApplication
import androidx.core.graphics.drawable.toDrawable
import com.base.find_phone_clap_detector.utils.RemoteConfigAds

object RewardAdManager {

    private var showRewardedInterstitialAd = true
    private var rewardAdCountDownTimer: CountDownTimer? = null

    fun showRewardedAd(context: Context, activity: Activity, onAdSuccess: () -> Unit) {
        val rewardLoadingDialog = createLoadingDialog(context)
        rewardAdCountDownTimer = object : CountDownTimer(7000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                rewardLoadingDialog?.findViewById<TextView>(R.id.textView4)?.text =
                    "Loading ad in ${millisUntilFinished / 1000} seconds ..."
            }

            override fun onFinish() {
                showRewardedInterstitialAd = false
                if (rewardLoadingDialog != null) {
                    dismissLoadingDialog(rewardLoadingDialog)
                }
                dismissLoadingDialog(rewardLoadingDialog!!)
                showToast(context, activity.getString(R.string.reward_ad_not))
            }
        }

        rewardLoadingDialog?.show()
        if (rewardLoadingDialog?.window != null) {
            rewardLoadingDialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
        rewardAdCountDownTimer?.start()
        rewardLoadingDialog?.let { loadRewardedAd(context, activity, it, onAdSuccess) }
    }

    private fun createLoadingDialog(context: Context): AlertDialog? {
        val builder = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.layout_ad_loading, null)
        builder.setView(dialogView)
        return builder.setCancelable(false).create()
    }

    private fun loadRewardedAd(
        context: Context,
        activity: Activity,
        rewardLoadingDialog: AlertDialog,
        onAdSuccess: () -> Unit
    ) {
        showRewardedInterstitialAd = true
        MyApplication.mInstance.adsManager.loadRewardVideoAd(
            activity,
            object : AdsManager.IRewardVideo {
                override fun onFailedToLoad() {
                    dismissLoadingDialog(rewardLoadingDialog)
                    cancelTimer()

                    if (!RemoteConfigAds.shouldShowAd(RemoteConfigAds.REWARD_AD)) {
                        showToast(context, activity.getString(R.string.reward_ad_not))
                    }else {
                        try {
                            Log.d("ADD","Before inter ad loading")
                            MyApplication.mInstance.adsManager.loadInterstitialAd(activity, activity.getString(R.string.ADMOB_REWARD_INTER)) {
                                onAdSuccess()
                                Log.d("ADD","After inter ad loading")
                            }
                        } catch (e: Exception) {
                            e.localizedMessage
                        }
                    }
                }

                override fun onRewardVideoLoad() {
                    if (showRewardedInterstitialAd) {
                        MyApplication.mInstance.adsManager.showRewardVideoAd(activity, this)
                        cancelTimer() // Cancel the timer as the ad is loading
                    }
                    dismissLoadingDialog(rewardLoadingDialog)
                }

                override fun onFailedToShow() {
                    dismissLoadingDialog(rewardLoadingDialog)
                    cancelTimer()
                    showToast(context, activity.getString(R.string.reward_ad_not))
                }

                override fun onRewardedSuccess() {
                    dismissLoadingDialog(rewardLoadingDialog)
                    cancelTimer()
                    onAdSuccess()
                }
            })
    }

    private fun cancelTimer() {
        rewardAdCountDownTimer?.cancel()
        rewardAdCountDownTimer = null
    }

    private fun dismissLoadingDialog(dialog: AlertDialog) {

        try {
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
