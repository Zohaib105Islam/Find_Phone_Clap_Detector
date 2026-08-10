package com.base.find_phone_clap_detector.ui.interfaces

import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd

interface RewardInterLoaded {
    fun onRewardedInterstitialLoaded(ad: RewardedInterstitialAd? = null)
}