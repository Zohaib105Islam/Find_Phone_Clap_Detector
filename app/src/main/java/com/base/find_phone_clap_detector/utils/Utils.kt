package com.base.find_phone_clap_detector.utils

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.card.MaterialCardView
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.managers.RewardAdManager

object Utils {
    // Reward Ad Dilogs
    fun watchAdOrBuyPremium(
        activity: Activity,
        onCloseClick: () -> Unit = {},
        onBuyPremium: () -> Unit,
        afterAdSuccess: () -> Unit
    ) {
        val dialogBuilder = AlertDialog.Builder(activity)
        val dialogView =
            activity.layoutInflater.inflate(R.layout.buy_pro_or_watch_ad_dialog, null)
        val buyPremium = dialogView?.findViewById<MaterialCardView>(R.id.buyPremium)
        val watchAd = dialogView?.findViewById<MaterialCardView>(R.id.watchAdAndContinue)
        val closebtn = dialogView?.findViewById<TextView>(R.id.closeBtn)
        dialogBuilder.setView(dialogView)
        val alertDialog = dialogBuilder.create()
        alertDialog.show()
        alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        closebtn?.setOnClickListener {
            it.disableMultipleClicking(1000)
            alertDialog.dismiss()
            onCloseClick.invoke()
        }
        buyPremium?.setOnClickListener {
            it.disableMultipleClicking(1000)
            alertDialog.dismiss()
            onBuyPremium.invoke()
        }
        watchAd?.setOnClickListener {
            it.disableMultipleClicking(1000)
            alertDialog.dismiss()
            RewardAdManager.showRewardedAd(activity, activity, onAdSuccess = afterAdSuccess)
        }
    }
}