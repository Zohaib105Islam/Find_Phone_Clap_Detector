package com.base.find_phone_clap_detector.managers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.*
import com.google.common.collect.ImmutableList
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.ui.activities.MainActivity
import com.base.find_phone_clap_detector.utils.Constants
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManagerV5 @Inject constructor(val context: Context) {

    private var retryCount = 0
    private var billingClient: BillingClient? = null
    private var productsAvailable: List<ProductDetails> = ArrayList()

    private fun inItBillingManagerV5() {
        Timber.tag(TAG).d("BillingManagerV5: init")

        billingClient = BillingClient.newBuilder(context)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts() // required for one-time products
                    .build()
            )
            .setListener { billingResult: BillingResult, purchases: List<Purchase>? ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        verifySubPurchase(purchase)
                    }
                }
            }
            .enableAutoServiceReconnection()
            .build()

        establishConnection()
    }

    fun establishConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    showProducts()
                    subscriptionPurchaseDetails(Constants.ITEM_SKU_PRO_USER_SUB)

                    val params = QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()

                    billingClient?.queryPurchasesAsync(params) { result, historyList ->
                        Timber.tag(TAG)
                            .d("Purchase history: $result, size=${historyList.size}")
                        // handlePurchaseHistory(result, historyList)
                    }
                    checkPurchasedSubs()
                }
            }

            override fun onBillingServiceDisconnected() {
                retryCount++
                if (retryCount < 3) {
                    Handler(Looper.getMainLooper())
                        .postDelayed({ establishConnection() }, 10000)
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    fun showProducts() {
        Timber.tag(TAG).d("showProducts: ")
        val productList = ImmutableList.of(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("remove_ads")
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("get_premium")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { result, queryProductDetailsResult ->
            Timber.tag(TAG)
                .d("showProducts: queryProductDetailsAsync size=${queryProductDetailsResult.productDetailsList.size}")
            productsAvailable = queryProductDetailsResult.productDetailsList
        }
    }

    private fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        if (billingClient?.isReady != true || activity.isFinishing || activity.isDestroyed) {
            Timber.tag(TAG).w("launchPurchaseFlow aborted: client/activity not ready")
            return
        }
        val productDetailsParamsList = ImmutableList.of(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        val result = billingClient?.launchBillingFlow(activity, billingFlowParams)
        if (result == null || result.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.tag(TAG).w(
                "launchPurchaseFlow failed: code=${result?.responseCode} msg=${result?.debugMessage}"
            )
        }
    }

    private fun verifySubPurchase(purchases: Purchase) {
        Timber.tag(TAG).d("verifySubPurchase: ")
        val acknowledgePurchaseParams = AcknowledgePurchaseParams
            .newBuilder()
            .setPurchaseToken(purchases.purchaseToken)
            .build()

        when (purchases.products[0]) {
            Constants.ITEM_SKU_REMOVE_ADS_ONLY -> makeAppAdsFree()
            Constants.ITEM_SKU_GET_PREMIUM,
            Constants.ITEM_SKU_PRO_USER_SUB -> makeAppPremium()
            "item_1", "item_2" -> thanksToast()
            "item_3", "item_4" -> {
                makeAppAdsFree()
                thanksToast()
            }
        }

        Toast.makeText(
            MyApplication.mInstance,
            MyApplication.mInstance.getString(android.R.string.ok), // adjust if using custom string
            Toast.LENGTH_LONG
        ).show()

        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            Timber.tag(TAG)
                .d("verifySubPurchase: acknowledgePurchase result=${billingResult.responseCode}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Timber.tag(TAG).d("verifySubPurchase acknowledged successfully")
            }
            Timber.tag(TAG).d("Purchase Token: ${purchases.purchaseToken}")
            Timber.tag(TAG).d("Purchase Time: ${purchases.purchaseTime}")
            Timber.tag(TAG).d("Purchase OrderID: ${purchases.orderId}")
        }
    }

    private fun thanksToast() {
        Toast.makeText(MyApplication.mInstance, "Thanks for your support", Toast.LENGTH_SHORT).show()
    }

    fun oneTimePurchase(activity: Activity, itemSkuId: String) {
        val productList = ImmutableList.of(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(itemSkuId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { _, queryProductDetailsResult ->
            if (queryProductDetailsResult.productDetailsList.isNotEmpty()) {
                launchPurchaseFlow(activity, queryProductDetailsResult.productDetailsList[0])
            }
        }
    }

    fun subscription(activity: Activity) {
        if (billingClient?.isReady != true || activity.isFinishing || activity.isDestroyed) {
            Timber.tag(TAG).w("subscription aborted: client/activity not ready")
            return
        }
        val featureResult = billingClient?.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        if (featureResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.tag(TAG).w(
                "subscription unsupported: code=${featureResult?.responseCode} msg=${featureResult?.debugMessage}"
            )
            return
        }
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(Constants.ITEM_SKU_PRO_USER_SUB)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingClient?.isReady != true || activity.isFinishing || activity.isDestroyed) {
                Timber.tag(TAG).w("subscription launch aborted after query: client/activity not ready")
                return@queryProductDetailsAsync
            }
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Timber.tag(TAG).w(
                    "subscription query failed: code=${billingResult.responseCode} msg=${billingResult.debugMessage}"
                )
                return@queryProductDetailsAsync
            }

            val productDetails = queryProductDetailsResult.productDetailsList.firstOrNull()
            if (productDetails != null) {
                val selectedOffer = findLaunchableOffer(productDetails)
                val offerToken = selectedOffer?.offerToken
                if (!offerToken.isNullOrBlank()) {
                    val productDetailsParamsList = listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()
                    val result = billingClient?.launchBillingFlow(activity, billingFlowParams)
                    if (result == null || result.responseCode != BillingClient.BillingResponseCode.OK) {
                        Timber.tag(TAG).w(
                            "subscription launch failed: code=${result?.responseCode} msg=${result?.debugMessage}"
                        )
                    }
                } else {
                    Timber.tag(TAG).w(
                        "subscription: no launchable offer for product=${productDetails.productId}"
                    )
                }
            } else {
                Timber.tag(TAG).w("subscription: no product details for sku=${Constants.ITEM_SKU_PRO_USER_SUB}")
            }
        }
    }

    private fun findLaunchableOffer(productDetails: ProductDetails): ProductDetails.SubscriptionOfferDetails? {
        return productDetails.subscriptionOfferDetails?.firstOrNull { offer ->
            !offer.offerToken.isNullOrBlank() &&
                offer.pricingPhases.pricingPhaseList.isNotEmpty()
        }
    }

    private fun checkPurchasedSubs() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult1: BillingResult, list: List<Purchase> ->
            if (billingResult1.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(Companion.TAG, "onBillingSetupFinished: " + list.size + " size")
                if (list.isNotEmpty()) {
                    makeAppPremium()
                    // set 1 to activate premium feature
                    for ((i, purchase: Purchase) in list.withIndex()) {
                        //Here you can manage each product, if you have multiple subscription
                        Log.d(
                            "testOffer",
                            purchase.originalJson
                        ) // Get to see the order information
                        Log.d("testOffer", " index$i")
                    }
                } else {
//                    makeAppPremium()
                    disableAppPremium()
                }
            }
        }
    }

    fun disableAppPremium() {
        MyApplication.mInstance.preferenceManager.put(
            PreferenceManager.Key.IS_APP_ADS_FREE,
            false
        )
        MyApplication.mInstance.preferenceManager.put(
            PreferenceManager.Key.IS_APP_PREMIUM,
            false
        )

    }


    private fun handlePurchaseHistory(p0: BillingResult, p1: MutableList<PurchaseHistoryRecord>?) {
        Timber.tag(TAG).d("onPurchaseHistoryResponse: $p0 and record list $p1")
        if (p1?.isNotEmpty() == true) {
            p1.forEach {
                when (it.products[0]) {
                    Constants.ITEM_SKU_REMOVE_ADS_ONLY -> makeAppAdsFree()
                    Constants.ITEM_SKU_GET_PREMIUM -> makeAppPremium()
                }
            }
        }
    }

    fun makeAppPremium() {
        MyApplication.mInstance.preferenceManager.put(
            PreferenceManager.Key.IS_APP_ADS_FREE, true
        )
        MyApplication.mInstance.preferenceManager.put(
            PreferenceManager.Key.IS_APP_PREMIUM, true
        )
        Toast.makeText(
            MyApplication.mInstance,
            "Subscription activated, Enjoy! Please Restart the App",
            Toast.LENGTH_LONG
        ).show()
        restartApp()
    }

    fun restartApp() {
        val intent = Intent(MyApplication.mInstance, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        MyApplication.mInstance.startActivity(intent)
    }

    private fun makeAppAdsFree() {
        MyApplication.mInstance.preferenceManager.put(
            PreferenceManager.Key.IS_APP_ADS_FREE, true
        )
        Toast.makeText(
            MyApplication.mInstance,
            "Subscription activated, Enjoy! Please Restart the App",
            Toast.LENGTH_LONG
        ).show()
        restartApp()
    }

    fun subscriptionPurchaseDetails(itemSkuId: String) {
        try {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(itemSkuId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            billingClient?.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Timber.tag(TAG).w(
                        "subscriptionPurchaseDetails query failed: code=${billingResult.responseCode} msg=${billingResult.debugMessage}"
                    )
                    return@queryProductDetailsAsync
                }

                val productDetails = queryProductDetailsResult.productDetailsList.firstOrNull()
                val pricingPhases = findLaunchableOffer(productDetails ?: return@queryProductDetailsAsync)
                    ?.pricingPhases
                    ?.pricingPhaseList
                    .orEmpty()

                if (pricingPhases.isNotEmpty()) {
                    Constants.oneTimeProductPremiumPrice = pricingPhases.getOrNull(0)?.formattedPrice.orEmpty()

                    Constants.afterDiscount = pricingPhases.getOrNull(1)?.formattedPrice.orEmpty()
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e("Error in oneTimePurchaseDetails: ${e.localizedMessage}")
        }
    }

    companion object {
        var oneTimeProductPremiumPrice = ""
        private const val TAG = "BillingManagerV5"
    }

    init {
        inItBillingManagerV5()
    }
}
