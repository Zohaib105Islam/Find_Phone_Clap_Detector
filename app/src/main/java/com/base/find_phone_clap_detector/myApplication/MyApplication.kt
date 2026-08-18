package com.base.find_phone_clap_detector.myApplication

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.base.find_phone_clap_detector.BuildConfig
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.managers.AdsManager
import com.base.find_phone_clap_detector.managers.AnalyticsManager
import com.base.find_phone_clap_detector.managers.BillingManagerV5
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.ui.activities.PremiumFreeTrailActivity
import com.base.find_phone_clap_detector.ui.activities.PremiumScreenActivity
import com.base.find_phone_clap_detector.ui.activities.SettingsActivity
import com.base.find_phone_clap_detector.ui.activities.SplashActivity
import com.base.find_phone_clap_detector.ui.activities.StopSoundActivity
import com.base.find_phone_clap_detector.utils.AppStateTracker
import com.base.find_phone_clap_detector.utils.RemoteConfigAds
import dagger.hilt.android.HiltAndroidApp
import org.json.JSONObject
import java.util.Date
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class MyApplication: Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {

    @Inject
    lateinit var preferenceManager: PreferenceManager
    @Inject
    lateinit var adsManager: AdsManager
    @Inject
    lateinit var billingManagerV5: BillingManagerV5

    private lateinit var appOpenAdManager: AppOpenAdManager

    private lateinit var currentActivity: Activity

    private val LOG_TAG = "MyApplication"

    private var isLoadingShown = false

    var byCreateAudioService= false
    var byCreateAudioServiceActivated= false

    private var alertDialogAds: AlertDialog? = null

    override fun onCreate() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isDeadSystemException(throwable)) {
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            } else {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        Timber.plant(Timber.DebugTree())
        mInstance = this
        mContext = applicationContext
        preferenceManager.put(PreferenceManager.Key.isDarkTheme, false)
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()
        appContext = applicationContext

        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

    }

    private fun isDeadSystemException(throwable: Throwable?): Boolean {
        var t = throwable
        while (t != null) {
            if (t is android.os.DeadSystemException) {
                return true
            }
            t = t.cause
        }
        return false
    }


   /* override fun attachBaseContext(newBase: Context?) {
        val localeContext = newBase?.let {
            val lang = Language()
            lang.setLanguage(it)
            it
        }
        super.attachBaseContext(localeContext)
    }*/

    //open ad
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        if (preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_ADS_FREE, false)) {
            return
        }
        if(isOutForRating){
           isOutForRating = false
            return
        }

        when (currentActivity) {
            is SettingsActivity -> {}
            is AdActivity -> {}
            is SplashActivity -> {}
            is PremiumScreenActivity -> {}
            is PremiumFreeTrailActivity -> {}
            is StopSoundActivity -> {}
            else -> {
                currentActivity.let {
                    if (!isLoadingShown) {
                        if (RemoteConfigAds.shouldShowAd(
                                RemoteConfigAds.OPEN_AD_APP,
                                RemoteConfigAds.OPEN_ADS_OBJ
                            )
                        ) {
                            Log.d("RemoteConfig","Open ad is enabled")
                            AnalyticsManager.logEvent("FA_open_ad_resume_requested")
                         //   loadOpenAdManually(it)
                        }else {
                            Log.d("RemoteConfig","Open ad is disabled")
                            AnalyticsManager.logEvent("FA_open_ad_resume_disabled_remote")
                        }

                    }
                }
            }
        }
    }


    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity
            AppStateTracker.isAppKilled = false
        }
    }

    override fun onActivityResumed(activity: Activity) {
        AppStateTracker.isInForeground = true
    }

    override fun onActivityPaused(activity: Activity) {
        AppStateTracker.isInForeground = false
    }

    override fun onActivityStopped(activity: Activity) {
        try {
            com.base.find_phone_clap_detector.utils.QueuedWorkFix.clearQueuedWork()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    /**
     * Shows an app open ad.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */
    fun showAdIfAvailable(
        adId: String,
        activity: Activity,
        onShowAdCompleteListener: MyApplication.OnShowAdCompleteListener
    ) {
        appOpenAdManager.showAdIfAvailable(activity, adId, onShowAdCompleteListener)
    }

    /**
     * since we are showing open ad in only splash screen
     * and not in onResume
     * so we have to load manually
     * */
    fun showAdLoadingDialog(context: Activity?) {
        isLoadingShown = true
        val dialogBuilder = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.layout_ad_loading, null)
        dialogBuilder.setView(dialogView)
        alertDialogAds = dialogBuilder.create()
        alertDialogAds?.setCancelable(false)
        if (!(context?.isFinishing == true || context?.isDestroyed == true)) {
            alertDialogAds?.show()
        }
        if (alertDialogAds?.window != null) {
            alertDialogAds?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    fun hideDialogAds() {
        if (alertDialogAds != null && alertDialogAds?.isShowing == true) {
            try {
                isLoadingShown = false
                alertDialogAds?.dismiss()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }
    }

    fun loadOpenAdManually(activity: Activity, adId: String= getString(R.string.ADMOD_OPEN_AD), isShowDialog: Boolean = true) {
        Log.d(LOG_TAG, "loadOpenAdManually: ")
        if (isShowDialog) showAdLoadingDialog(activity)
        appOpenAdManager?.loadAd(activity, adId) { isLoaded ->
            if (isLoaded) {
                currentActivity?.let {
                    showAdIfAvailable(
                        adId,
                        it,
                        object : OnShowAdCompleteListener {
                            override fun onShowAdComplete() {
                                hideDialogAds()
                            }
                        })
                }
            } else hideDialogAds()
        }
    }

    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    private inner class AppOpenAdManager {
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd: Boolean = false
        private var loadTime: Long = 0
        private var lastLoadRequestTime: Long = 0

        fun loadAd(context: Context, adId: String, isLoaded: (Boolean) -> Unit = {}) {
            if (preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_ADS_FREE, false)) {
                isLoaded.invoke(false)
                Log.d(LOG_TAG, "onAdLoaded. Premium")
                AnalyticsManager.logEvent("FA_open_ad_skip_premium")
                return
            }
            if (isAdAvailable) {
                isLoaded.invoke(true)
                Log.d(LOG_TAG, "onAdLoaded. already loaded")
                AnalyticsManager.logEvent("FA_open_ad_cached_hit")
                return
            }
            if (isLoadingAd) {
                isLoaded.invoke(false)
                Log.d(LOG_TAG, "onAdLoaded. already loading")
                AnalyticsManager.logEvent("FA_open_ad_skip_already_loading")
                return
            }
            
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastLoadRequestTime < 15000) {
                isLoaded.invoke(false)
                Log.d(LOG_TAG, "onAdLoaded. Debounced (15s)")
                AnalyticsManager.logEvent("FA_open_ad_debounced")
                return
            }
            lastLoadRequestTime = currentTime

            isLoadingAd = true
            AnalyticsManager.logEvent("FA_open_ad_load_started")
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context,
                adId,
                request,
                object : AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = (Date()).time
                        isLoaded.invoke(true)
                        Log.d(LOG_TAG, "onAdLoaded.")
                        AnalyticsManager.logEvent("FA_open_ad_loaded")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingAd = false
                        isLoaded.invoke(false)
                        Log.d(LOG_TAG, "onAdFailedToLoad: " + loadAdError.message)
                        AnalyticsManager.logEvent("FA_open_ad_failed_to_load")
                    }
                })
        }

        fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference = (Date()).time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return (dateDifference < (numMilliSecondsPerHour * numHours))
        }

        val isAdAvailable: Boolean
            get() = appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)

        fun showAdIfAvailable(
            activity: Activity,
            adId: String,
            onShowAdCompleteListener: MyApplication.OnShowAdCompleteListener = object :
                OnShowAdCompleteListener {
                override fun onShowAdComplete() {}
            }
        ) {
            if (isShowingAd) {
                Log.d(LOG_TAG, "The app open ad is already showing.")
                AnalyticsManager.logEvent("FA_open_ad_skip_already_showing")
                return
            }

            if (!isAdAvailable) {
                Log.d(LOG_TAG, "The app open ad is not ready yet.")
                AnalyticsManager.logEvent("FA_open_ad_not_available")
                onShowAdCompleteListener.onShowAdComplete()
                return
            }

            if (!AppStateTracker.isInForeground) {
                Log.d(LOG_TAG, "App is not in foreground. Keeping ad cached for later.")
                AnalyticsManager.logEvent("FA_open_ad_skip_background")
                onShowAdCompleteListener.onShowAdComplete()
                return
            }

            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false
                    AnalyticsManager.logEvent("FA_open_ad_dismissed")
                    onShowAdCompleteListener.onShowAdComplete()
//                    loadAd(activity, adId)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    AnalyticsManager.logEvent("FA_open_ad_failed_to_show")
                    onShowAdCompleteListener.onShowAdComplete()
//                    loadAd(activity, adId)
                    hideDialogAds()
                }

                override fun onAdShowedFullScreenContent() {
                    AnalyticsManager.logEvent("FA_open_ad_impression")
                }
            }

            isShowingAd = true
            appOpenAd?.show(activity)
        }
    }

    companion object{
        private lateinit var mContext: Context
        lateinit var mInstance: MyApplication
        lateinit var appContext: Context
        var isGeneral: Boolean? = false
        var isScanSuccessful = false
        var isFirstPremium = true
        var isFromMain = true
        var isOutForRating = false
        var isFromStopClap =  false
        var isSoundActive=  false
        var shouldRestartDetectorFromStopSound = false

        fun isAppInitialized(): Boolean {
            return ::mInstance.isInitialized
        }
    }
}
