package com.base.find_phone_clap_detector.utils

import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import org.json.JSONObject
import timber.log.Timber

object RemoteConfigAds {

    //Ad Objects
    const val MAIN_OBJECT = "ads"
    const val INTER_ADS_OBJ = "inter"
    const val OPEN_ADS_OBJ = "open_ads"

    // Inter
    const val SERVICE_START = "service_start"
    const val SERVICE_STOP = "service_stop"
    const val PREMIUM_CLOSE = "premium_close"
    const val REWARD_AD = "reward"

    // Open Ads
    const val OPEN_AD_APP = "app_open_ad"
    const val OPEN_AD_SPLASH = "splash_open_ad"

    fun shouldShowAd(adName: String, adType: String = INTER_ADS_OBJ): Boolean {
        val json = MyApplication.mInstance.preferenceManager.getString(PreferenceManager.Key.ADS_CONFIG_JSON, null) ?: return true

        return try {
            JSONObject(json)
                .getJSONObject(MAIN_OBJECT)
                .getJSONObject(adType)
                .optBoolean(adName, true)
        } catch (e: Exception) {
            Timber.e(e)
            true
        }
    }

}