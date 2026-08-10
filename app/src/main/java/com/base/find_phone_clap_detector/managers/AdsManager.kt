package com.base.find_phone_clap_detector.managers

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VideoController.VideoLifecycleCallbacks
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.myApplication.MyApplication.Companion.mInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("LogNotTimber")
@Singleton
class AdsManager @Inject constructor(
    context: Context?,
    private val preferenceManager: PreferenceManager
) {
    enum class NativeAdType {
        REGULAR_TYPE,
        MEDIUM_TYPE,
        SMALL_TYPE,

        NOMEDIA_MEDIUM,
        MEDIA_SMALL_NEW
    }

    private var admobInterstitialAd: InterstitialAd? = null
    private val TAG: String = AdsManager::class.java.getName()

    private var mRewardedAd: RewardedAd? = null
    private var alertDialogAds: AlertDialog? = null

    fun initSDK(context: Context, isdKinit: () -> Unit) {
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(
                mutableListOf<String?>(
                    "F58A121CA6B48BE487E841B65137F635",
                    "F489CD7B6E9F529144AEEBD5D32D3417",
                    "47BD8D71811EF0D2F25B46868A922D56",
                    "32F9DA89E49FBDF09C1F69F89FF071E0"
                )
            )
                .build()
        )

        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(
                context.applicationContext,
                OnInitializationCompleteListener { initializationStatus: InitializationStatus? ->
                    Log.d(TAG, "AdsManager: initializes")
                    if (initializationStatus != null) {
                        val statusMap = initializationStatus.adapterStatusMap
                        for (adapterClass in statusMap.keys) {
                            val status = statusMap[adapterClass]
                            Log.d(
                                "MyApp",
                                String.format(
                                    "Adapter name: %s, Description: %s, Latency: %d",
                                    adapterClass,
                                    status?.description,
                                    status?.latency ?: 0
                                )
                            )
                        }
                    }
                    Handler(Looper.getMainLooper()).post {
                        isdKinit.invoke()
                    }
                }
            )
        }
    }

    private fun prepareAdRequest(): AdRequest {
        val adRequest: AdRequest
        adRequest = AdRequest.Builder().build()
        return adRequest
    }

    fun loadInterstitialAd(
        activity: Activity,
        adId: String = activity.getString(R.string.ADMOB_INTERSTITIAL_V2),
        funcToInvoke: Runnable
    ) {

        if (preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_ADS_FREE, false)){
            funcToInvoke.run()
            return
        }

        if (!isNetWorkAvailable(activity)) {
            funcToInvoke.run()
            return
        }

        if (activity.isFinishing || activity.isDestroyed) return

        showAdLoadingDialog(activity)
        if (admobInterstitialAd != null) {
            showInterstitialAd(activity, funcToInvoke)
            return
        }
        // Create a timer for a 10-second timeout
        val handler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            hideDialogAds()
            funcToInvoke.run()
        }
        handler.postDelayed(timeoutRunnable, 10000) // Delay for 10 seconds


        val interstitialAdLoadCallback: InterstitialAdLoadCallback =
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    super.onAdLoaded(interstitialAd)
                    Log.d(TAG, "onLoad: admob interstitial")
                    handler.removeCallbacks(timeoutRunnable) // Remove timer if ad loads
                    admobInterstitialAd = interstitialAd
                    showInterstitialAd(activity, funcToInvoke)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    Log.d(TAG, "onAdFailedToLoad: admob interstitial. Loading facebook ad")
                    handler.removeCallbacks(timeoutRunnable) // Remove timer on failure
                    hideDialogAds()
                    funcToInvoke.run()
                }
            }

        InterstitialAd.load(
            activity,
            adId,
            prepareAdRequest(),
            interstitialAdLoadCallback
        )
    }

    fun showInterstitialAd(activity: Activity, funcAfterAdHidden: Runnable) {
        if (admobInterstitialAd != null) {
            admobInterstitialAd!!.setFullScreenContentCallback(object :
                FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    MyApplication.isOutForRating = false
                    Log.d(TAG, "onAdDismissedFullScreenContent: ")
                    admobInterstitialAd = null
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            funcAfterAdHidden.run()
                        }
                    }, 200)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    hideDialogAds()
                    super.onAdFailedToShowFullScreenContent(adError)
                    Log.d(TAG, "onAdFailedToShowFullScreenContent: ")
                    MyApplication.isOutForRating = false
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        funcAfterAdHidden.run()
                    }
                }

                override fun onAdShowedFullScreenContent() {
                    hideDialogAds()
                    super.onAdShowedFullScreenContent()
                    MyApplication.isOutForRating = true
                }
            })
            admobInterstitialAd!!.show(activity)
        } else {
            hideDialogAds()
            funcAfterAdHidden.run()
        }
    }

    fun showAdLoadingDialog(context: Activity?) {
        if (context == null || context.isFinishing || context.isDestroyed) {
            Log.d(TAG, "showAdLoadingDialog: activity is not in valid state")
            return
        }
        val dialogBuilder = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.layout_ad_loading, null)
        dialogBuilder.setView(dialogView)
        alertDialogAds = dialogBuilder.create()
        alertDialogAds!!.setCancelable(false)
        try {
            alertDialogAds!!.show()
            alertDialogAds!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        } catch (e: Exception) {
            Log.d(TAG, "showAdLoadingDialog: failed to show dialog", e)
            alertDialogAds = null
        }
    }

    private fun hideDialogAds() {
        if (alertDialogAds != null) {
            if (alertDialogAds!!.isShowing()) {
                try {
                    alertDialogAds!!.dismiss()
                } catch (e: Exception) {
                    Log.d("AdsManager", "Failed to dismiss dialog", e)
                } finally {
                    alertDialogAds = null
                }
            }
        } else {
            Log.d("AdsManager", "Dialog is not showing or already null.")
        }
    }

    private fun getAdSize(activity: Activity): AdSize {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        val display = activity.windowManager.getDefaultDisplay()
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }


    fun loadNativeAd(
        activity: Activity,
        frameLayout: FrameLayout?,
        nativeAdViewType: AdsManager.NativeAdType,
        nativeAdId: String,
        shimmer: ShimmerFrameLayout
    ) {
        if (!isNetWorkAvailable(activity)) {
            frameLayout?.visibility = View.GONE
            shimmer.stopShimmer()
            shimmer.hideShimmer()
            AnalyticsManager.logEvent("FA_native_skip_no_network")
            return
        }
        
        if (preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_ADS_FREE, false)) {
            frameLayout?.visibility = View.GONE
            shimmer.stopShimmer()
            shimmer.hideShimmer()
            AnalyticsManager.logEvent("FA_native_skip_premium")
            return
        }
        frameLayout?.visibility = View.VISIBLE
        // Clear any existing stacked NativeAdViews before loading, but leave the Shimmer intact
        frameLayout?.let {
            for (i in it.childCount - 1 downTo 0) {
                val child = it.getChildAt(i)
                if (child is NativeAdView) {
                    it.removeViewAt(i)
                }
            }
        }
        shimmer.startShimmer()
        shimmer.visibility = View.VISIBLE
        AnalyticsManager.logEvent("FA_native_load_started")

        val builder = AdLoader.Builder(activity, nativeAdId)
        builder.forNativeAd(NativeAd.OnNativeAdLoadedListener { nativeAd: NativeAd? ->
            if (activity.isDestroyed || activity.isFinishing) {
                Log.d(TAG, "Activity is finishing or destroyed, discarding native ad.")
                AnalyticsManager.logEvent("FA_native_discarded_activity_dead")
                nativeAd?.destroy()
                return@OnNativeAdLoadedListener
            }

            var nativeAdView: NativeAdView? = null
            when (nativeAdViewType) {
                AdsManager.NativeAdType.REGULAR_TYPE -> nativeAdView =
                    LayoutInflater.from(activity)
                        .inflate(R.layout.admob_native_regular_layout, null) as NativeAdView?

                AdsManager.NativeAdType.MEDIUM_TYPE -> nativeAdView =
                    LayoutInflater.from(activity)
                        .inflate(R.layout.admob_native_medium_layout, null) as NativeAdView?

                AdsManager.NativeAdType.SMALL_TYPE -> nativeAdView =
                    LayoutInflater.from(activity)
                        .inflate(R.layout.admob_native_small_layout, null) as NativeAdView?

                AdsManager.NativeAdType.NOMEDIA_MEDIUM -> nativeAdView =
                    LayoutInflater.from(activity)
                        .inflate(
                            R.layout.admob_native_media_small_layout,
                            null
                        ) as NativeAdView?

                AdsManager.NativeAdType.MEDIA_SMALL_NEW -> nativeAdView =
                    LayoutInflater.from(activity)
                        .inflate(
                            R.layout.admob_native_media_small_layout_new,
                            null
                        ) as NativeAdView?

                else -> {}
            }

            if (nativeAdView != null) {
                nativeAd?.let {
                    populateUnifiedNativeAdView(
                        it,
                        nativeAdView,
                        nativeAdViewType
                    )
                }
            }
            if (frameLayout == null) {
                Log.d(TAG, "FRAME_LAYOUT_NULL: ")
            } else {
                // Remove any old native ad views before adding the new one
                for (i in frameLayout.childCount - 1 downTo 0) {
                    val child = frameLayout.getChildAt(i)
                    if (child is NativeAdView) {
                        frameLayout.removeViewAt(i)
                    }
                }
                frameLayout.addView(nativeAdView)
                Log.d(TAG, "onNativeAdLoaded: ")
            }
        })

        val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
        val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
        builder.withNativeAdOptions(adOptions)

        val adLoader = builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.d(TAG, "onAdFailedToLoad: " + loadAdError.message)
                AnalyticsManager.logEvent("FA_native_failed_to_load")
                frameLayout?.visibility = View.GONE
                shimmer.stopShimmer()
                shimmer.hideShimmer()
            }

            override fun onAdClicked() {
                super.onAdClicked()
                Log.d(TAG, "onClickNativeAds: Okay")
                AnalyticsManager.logEvent("FA_native_clicked")
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                shimmer.stopShimmer()
                shimmer.hideShimmer()
                AnalyticsManager.logEvent("FA_native_loaded")
            }
            
            override fun onAdImpression() {
                super.onAdImpression()
                AnalyticsManager.logEvent("FA_native_impression")
            }
        }).build()

        adLoader.loadAd(prepareAdRequest())
    }

    private fun populateUnifiedNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView,
        nativeAdType: AdsManager.NativeAdType?
    ) {
        // Set the media view.
        if (nativeAdType == AdsManager.NativeAdType.REGULAR_TYPE || nativeAdType == AdsManager.NativeAdType.MEDIUM_TYPE || nativeAdType == AdsManager.NativeAdType.NOMEDIA_MEDIUM || nativeAdType == AdsManager.NativeAdType.MEDIA_SMALL_NEW) {
            adView.mediaView = adView.findViewById<MediaView?>(R.id.ad_media)
        }

        // Set other ad assets.
        adView.headlineView = adView.findViewById<View?>(R.id.ad_headline)
        adView.bodyView = adView.findViewById<View?>(R.id.ad_body)
        adView.callToActionView = adView.findViewById<View?>(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById<View?>(R.id.ad_app_icon)
        adView.starRatingView = adView.findViewById<View?>(R.id.ad_stars)
        adView.storeView = adView.findViewById<View?>(R.id.ad_store)

        // The headline and mediaContent are guaranteed to be in every UnifiedNativeAd.
        (adView.headlineView as TextView).text = nativeAd.headline
        if (nativeAdType == AdsManager.NativeAdType.REGULAR_TYPE || nativeAdType == AdsManager.NativeAdType.MEDIUM_TYPE || nativeAdType == AdsManager.NativeAdType.NOMEDIA_MEDIUM || nativeAdType == AdsManager.NativeAdType.MEDIA_SMALL_NEW) {
            adView.mediaView?.mediaContent = nativeAd.mediaContent
        }

        // Check for other ad assets.
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            Log.d(TAG, "populateUnifiedNativeAdView: " + nativeAd.body)
            if (nativeAdType == AdsManager.NativeAdType.NOMEDIA_MEDIUM) {
                adView.bodyView?.visibility = View.GONE
            } else {
                adView.bodyView?.visibility = View.VISIBLE
                (adView.bodyView as TextView).text = nativeAd.body
            }
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as Button).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(nativeAd.icon?.drawable)
            adView.iconView?.visibility = View.VISIBLE
        }

        if (nativeAd.store == null) {
            (adView.storeView as TextView).text = "Google Play"
        } else {
            adView.storeView?.visibility = View.VISIBLE
            (adView.storeView as TextView).text = nativeAd.store
        }

        // Complete the ad view setup.
        adView.setNativeAd(nativeAd)
        val vc = nativeAd.mediaContent?.videoController

        if (vc?.hasVideoContent() == true) {
            vc.videoLifecycleCallbacks = object : VideoLifecycleCallbacks() {
                override fun onVideoEnd() {
                    super.onVideoEnd()
                }
            }
        }
    }


    // ******** BANNER ADS **********//
    fun showBanner(
        context: Context,
        size: AdSize,
        adFrame: FrameLayout,
        adId: String,
        shimmer: ShimmerFrameLayout
    ) {
//        if (!preferenceManager.getBoolean(PreferenceManager.Key.REMOTE_CONFIG_IS_AD_ENABLE, true)) {
//            adFrame.setVisibility(View.GONE);
//            return;
//        }
        if (!isNetWorkAvailable(context)) {
            adFrame.visibility = View.GONE
            shimmer.stopShimmer()
            shimmer.hideShimmer()
            AnalyticsManager.logEvent("FA_banner_skip_no_network")
            return
        }
        if (preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_ADS_FREE, false)) {
            adFrame.visibility = View.GONE
            AnalyticsManager.logEvent("FA_banner_skip_premium")
            return
        }
        Log.d("BANNER_AD", "BANNER_AD_FUN")

        // Clear any previously stacked AdViews to prevent duplicate ad requests, but leave shimmer intact
        for (i in adFrame.childCount - 1 downTo 0) {
            val child = adFrame.getChildAt(i)
            if (child is AdView) {
                adFrame.removeViewAt(i)
            }
        }

        shimmer.startShimmer()
        shimmer.visibility = View.VISIBLE
        AnalyticsManager.logEvent("FA_banner_load_started")

        val adView = AdView(context)
        adView.setAdSize(size)
        adView.adUnitId = adId
        adFrame.addView(adView)
        adView.loadAd(prepareAdRequest())
        
        // Auto-destroy the AdView when the Activity is destroyed to prevent leaks and false impressions
        if (context is LifecycleOwner) {
            context.lifecycle.addObserver(object : androidx.lifecycle.LifecycleEventObserver {
                override fun onStateChanged(
                    source: LifecycleOwner,
                    event: Lifecycle.Event
                ) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        Log.d("BANNER_AD", "Activity destroyed, destroying AdView")
                        adView.destroy()
                        source.lifecycle.removeObserver(this)
                    }
                }
            })
        }

        adView.adListener = object : AdListener() {
            override fun onAdClicked() {
                super.onAdClicked()
                AnalyticsManager.logEvent("FA_banner_clicked")
            }

            override fun onAdClosed() {
                super.onAdClosed()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                Log.d("BANNER_AD", "Failed to load: ${loadAdError.message}")
                adFrame.visibility = View.GONE
                shimmer.stopShimmer()
                shimmer.hideShimmer()
                AnalyticsManager.logEvent("FA_banner_failed_to_load")
            }

            override fun onAdImpression() {
                super.onAdImpression()
                AnalyticsManager.logEvent("FA_banner_impression")
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                if (context is Activity && (context.isDestroyed || context.isFinishing)) {
                    Log.d("BANNER_AD", "Activity destroyed before banner loaded, discarding.")
                    AnalyticsManager.logEvent("FA_banner_discarded_activity_dead")
                    adView.destroy()
                    return
                }
                
                Log.d("BANNER_AD", "Banner loaded")
                shimmer.stopShimmer()
                shimmer.hideShimmer()
                AnalyticsManager.logEvent("FA_banner_loaded")
            }

            override fun onAdOpened() {
                super.onAdOpened()
            }
        }
    }

    interface ISDKinit {
        fun onInitialized()
    }

    fun showAdaptiveBanner(
        activity: Activity,
        adViewContainer: FrameLayout,
        shimmer: ShimmerFrameLayout
    ) {
        if (mInstance.preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_ADS_FREE, false)) {
            adViewContainer.setVisibility(View.GONE)
            shimmer.setVisibility(View.GONE)
            return
        }
        if (isNetWorkAvailable(activity.getApplicationContext())) {
            shimmer.startShimmer()
            val adView: AdView?
            adView = AdView(activity)
            adView.setAdUnitId(activity.getString(R.string.ADMOB_BANNER_V2))
            adViewContainer.removeAllViews()
            adViewContainer.addView(adView)
            adView.setAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    shimmer.stopShimmer()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    adViewContainer.setVisibility(View.GONE)
                    shimmer.setVisibility(View.GONE)
                }
            })
            adView.setAdSize(getAdSize(activity))
            val extras = Bundle()
            extras.putString("collapsible", "bottom")
            val adRequest =
                AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
            adView.loadAd(adRequest)
        } else {
            adViewContainer.setVisibility(View.GONE)
            shimmer.setVisibility(View.GONE)
        }
    }

    // **********Reward Video *********//
    fun loadRewardVideoAd(activity: Activity, iRewardVideo: IRewardVideo) {
        RewardedAd.load(
            activity,
            activity.getString(R.string.ADMOB_REWARD_VIDEO),
            prepareAdRequest(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error.
                    Log.d(TAG, loadAdError.toString() + " RewardVideoAd")
                    mRewardedAd = null
                    iRewardVideo.onFailedToLoad()
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    Log.d(TAG, "RewardVideoAd Ad was loaded.")
                    iRewardVideo.onRewardVideoLoad()
                }
            })
    }

    fun showRewardVideoAd(activity: Activity, iRewardVideo: IRewardVideo) {
        mRewardedAd!!.setFullScreenContentCallback(object : FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                Log.d(TAG, " RewardVideoAd Ad dismissed fullscreen content.")
                mRewardedAd = null
                iRewardVideo.onRewardedSuccess()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                // Called when ad fails to show.
                Log.e(TAG, " RewardVideoAd Ad failed to show fullscreen content.")
                mRewardedAd = null
                iRewardVideo.onFailedToShow()
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "RewardVideoAd Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "RewardVideoAd Ad showed fullscreen content.")
            }
        })

        if (mRewardedAd != null) {
            mRewardedAd!!.show(activity, object : OnUserEarnedRewardListener {
                override fun onUserEarnedReward(rewardItem: RewardItem) {
                    // Handle the reward.
                    Log.d(TAG, "The user earned the reward.")
                    val rewardAmount = rewardItem.getAmount()
                    val rewardType = rewardItem.getType()
                }
            })
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
        }
    }

    interface IRewardVideo {
        fun onFailedToLoad()

        fun onRewardVideoLoad()

        fun onFailedToShow()

        fun onRewardedSuccess()
    }

    companion object {
        fun isNetWorkAvailable(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetWorkInfo = connectivityManager.getActiveNetworkInfo()
            return activeNetWorkInfo != null && activeNetWorkInfo.isConnected()
        }
    }
}
