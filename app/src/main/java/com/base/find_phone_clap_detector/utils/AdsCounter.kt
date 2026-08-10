package com.base.find_phone_clap_detector.utils

import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication

object AdsCounter {
    var premiumCounter = 0
    var proCounter = 4
    var settingsCounter = 0
    var knowAboutCounter = 1
    var historyCounter = 1

    var rattingCounter = 0

//    val isAppPremium = MyApplication.mInstance.preferenceManager
//        .getBoolean(PreferenceManager.Key.IS_APP_PREMIUM)

    fun isAppPremium(): Boolean {
        return MyApplication.mInstance.preferenceManager
            .getBoolean(PreferenceManager.Key.IS_APP_PREMIUM)
    }

    fun showSettingsAd(): Boolean {
      //  premiumCounter++
        settingsCounter++
        return settingsCounter % 2 == 0
    }

    fun isShowRatting(): Boolean {
        rattingCounter++
        return rattingCounter % 7 == 0
    }

    fun showPremiumScreen(): Boolean {
        proCounter++
        return proCounter != 0 && proCounter % 5 == 0
    }


    fun showKnowAboutAd(): Boolean {
        knowAboutCounter++
        return knowAboutCounter % 3 == 0
    }
    fun showHistoryAd():Boolean{
        historyCounter++
        return historyCounter % 2 == 0
    }
}