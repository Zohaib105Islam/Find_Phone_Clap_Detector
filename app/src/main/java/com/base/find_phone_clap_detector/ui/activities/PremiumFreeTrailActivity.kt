package com.base.find_phone_clap_detector.ui.activities

import android.animation.ObjectAnimator
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.horse.identification.extensions.disableMultipleClicking
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivityPremiumFreeTrailBinding
import com.base.find_phone_clap_detector.databinding.ActivityPremiumScreenBinding
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.utils.Constants

class PremiumFreeTrailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumFreeTrailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumFreeTrailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() {
        setPrice()

        val shakeAnimator = ObjectAnimator.ofFloat(binding.premiumBtn, "translationX", 0f, 25f, -25f, 20f, -20f, 15f, -15f, 10f, -10f, 5f, -5f, 0f)
        shakeAnimator.duration = 1500
        shakeAnimator.repeatCount = ObjectAnimator.INFINITE
        shakeAnimator.repeatMode = ObjectAnimator.RESTART
        shakeAnimator.start()

        binding.premiumBtn.setOnClickListener {
            disableMultipleClicking(it, 1000)
            Toast.makeText(this, "Please Wait...!", Toast.LENGTH_SHORT).show()
            MyApplication.mInstance.billingManagerV5.subscription(
                this
            )
        }

        binding.btnCross.setOnClickListener {
            finish()
        }

        binding.restoreAccess.setOnClickListener {
            showCancelSub()
        }

        binding.licenseAgreement.setOnClickListener {
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
            val url = "https://iobitsofficial.blogspot.com/2023/12/privacy-policy-for-find-phone-by-clap.html"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No browser app found to open link", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
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

    private fun setPrice() {
        try {
            if(Constants.afterDiscount == "" || Constants.afterDiscount.isEmpty()){
                if(Constants.oneTimeProductPremiumPrice.isEmpty()){
                    binding.pricingText.text = "3 Days Free Trail\n then $3.99/Week"

                }else{
                    binding.pricingText.text = "3 Days Free Trail\n then ${Constants.oneTimeProductPremiumPrice}/Week"
                }
            } else {
                binding.pricingText.text = "3 Days Free Trail\n then ${Constants.afterDiscount}/Week"
//                binding.cont.text = "Get  50% Off  in ${Constants.oneTimeProductPremiumPrice}/Week for two weeks, Then ${Constants.afterDiscount}/Week"
            }

        } catch (e: Exception) {
            Log.d("PREMIUM_PRO", "ERROR : ${e.localizedMessage}")
        }
    }
}