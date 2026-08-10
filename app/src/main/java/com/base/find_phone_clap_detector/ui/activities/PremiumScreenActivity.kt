package com.base.find_phone_clap_detector.ui.activities

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.horse.identification.extensions.disableMultipleClicking
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivityPremiumScreenBinding
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.utils.Constants
import com.base.find_phone_clap_detector.utils.LocaleHelper
import com.base.find_phone_clap_detector.utils.RemoteConfigAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class PremiumScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumScreenBinding

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            showSplashInter()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.setAppLocale(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        Glide.with(this)
            .asGif()
            .load(R.raw.premium_discount_tag)
            .into(binding.offerIcon)

        val shakeAnimator = ObjectAnimator.ofFloat(binding.premiumBtn, "translationX", 0f, 25f, -25f, 20f, -20f, 15f, -15f, 10f, -10f, 5f, -5f, 0f)
        shakeAnimator.duration = 1500
        shakeAnimator.repeatCount = ObjectAnimator.INFINITE
        shakeAnimator.repeatMode = ObjectAnimator.RESTART
        shakeAnimator.start()

        lifecycleScope.launch(Dispatchers.IO) {
            delay(3000.milliseconds)
            withContext(Dispatchers.Main) {
                binding.btnCross.visibility = View.VISIBLE
            }
        }

        binding.btnCross.setOnClickListener {
            disableMultipleClicking(it,1000)
            showSplashInter()
        }

        binding.restoreAccess.setOnClickListener {
            disableMultipleClicking(it,1000)
            MyApplication.isOutForRating =  true
            showCancelSub()
        }

        binding.licenseAgreement.setOnClickListener {
            disableMultipleClicking(it,1000)
            MyApplication.isOutForRating =  true
            val url = "https://iobitsofficial.blogspot.com/2023/12/privacy-policy-for-find-phone-by-clap.html"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No browser app found to open link", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        binding.privacyPolicy.setOnClickListener {
            disableMultipleClicking(it,1000)
            MyApplication.isOutForRating =  true
            val url = "https://iobitsofficial.blogspot.com/2023/12/privacy-policy-for-find-phone-by-clap.html"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No browser app found to open link", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        binding.premiumBtn.setOnClickListener {
            disableMultipleClicking(it, 1000)
            Toast.makeText(this, getString(R.string.please_wait_2), Toast.LENGTH_SHORT).show()
            MyApplication.mInstance.billingManagerV5.subscription(this)
        }
        MyApplication.mInstance.billingManagerV5.subscriptionPurchaseDetails(
            Constants.ITEM_SKU_PRO_USER_SUB
        )
        setPrice()
    }

    private fun setPrice() {
        try {
            if(Constants.afterDiscount == "" || Constants.afterDiscount.isEmpty()){
                if(Constants.oneTimeProductPremiumPrice.isEmpty()){
                    binding.discountedPrice.text = "$3.99/Week"
                    binding.planText.text = getString(R.string.weekly_plan)
                    binding.actualPrice.visibility = View.GONE
                    binding.offerIcon.visibility = View.GONE
                }else{
                    binding.discountedPrice.text = "${Constants.oneTimeProductPremiumPrice}/${getString(R.string.week)}"
                    binding.actualPrice.visibility = View.GONE
                    binding.planText.text = getString(R.string.weekly_plan)
                    binding.offerIcon.visibility = View.GONE
                }
            } else {
                binding.discountedPrice.text = "${Constants.oneTimeProductPremiumPrice}/${getString(R.string.week_for_one_week)}"
                binding.actualPrice.text = "${getString(R.string.then)} ${Constants.afterDiscount}/${getString(R.string.week)}"
                binding.planText.text = getString(R.string.get_50_off)
                binding.actualPrice.visibility = View.VISIBLE
                binding.offerIcon.visibility = View.VISIBLE
//                binding.cont.text = "Get  50% Off  in ${Constants.oneTimeProductPremiumPrice}/Week for two weeks, Then ${Constants.afterDiscount}/Week"
            }

        } catch (e: Exception) {
            Log.d("PREMIUM_PRO", "ERROR : ${e.localizedMessage}")
        }
    }
    private fun showSplashInter() {
        if (MyApplication.isFirstPremium && MyApplication.isFromMain) {

            if (!RemoteConfigAds.shouldShowAd(RemoteConfigAds.PREMIUM_CLOSE)) {
                MyApplication.isFirstPremium = false
                finish()
                return
            }

            MyApplication.isFirstPremium = false
            MyApplication.mInstance.adsManager.loadInterstitialAd(this,this.getString(R.string.ADMOB_INTERSTITIAL_PREMIUM_V2)){
                finish()
            }
        } else {
            finish()
        }
    }

    private fun showCancelSub() {
        val url = getString(R.string.cancel_subscription_url)

        if (URLUtil.isValidUrl(url)) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

            // Ensure an activity exists to handle the intent
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No application found to open the link", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (MyApplication.isGeneral == true) {
            MyApplication.isGeneral = false
        }
    }
}