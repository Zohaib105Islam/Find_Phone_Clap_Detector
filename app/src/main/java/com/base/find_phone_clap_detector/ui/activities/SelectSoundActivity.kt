package com.base.find_phone_clap_detector.ui.activities

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.base.find_phone_clap_detector.myApplication.MyApplication
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivitySelectSoundBinding
import com.base.find_phone_clap_detector.managers.AdsManager
import com.base.find_phone_clap_detector.managers.AnalyticsManager
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.ui.adapters.SoundsAdapter
import com.base.find_phone_clap_detector.ui.dataClasses.SoundsDataClass
import com.base.find_phone_clap_detector.ui.interfaces.SoundInterface
import com.base.find_phone_clap_detector.utils.Language
import com.base.find_phone_clap_detector.utils.LocaleHelper
import com.base.find_phone_clap_detector.utils.Utils
import com.base.find_phone_clap_detector.utils.disableMultipleClicking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectSoundActivity : AppCompatActivity(), SoundInterface {

    private lateinit var binding: ActivitySelectSoundBinding
    private var soundsArrayList: ArrayList<SoundsDataClass> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply current app locale before view inflation
        LocaleHelper.setAppLocale(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySelectSoundBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.main.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initAds()
        initViews()
        addSoundItems()
    }

    private fun initAds() {
        // loadNativeAd
        MyApplication.mInstance.adsManager.loadNativeAd(
            this,
            binding.adFrame,
            AdsManager.NativeAdType.MEDIA_SMALL_NEW,
            this.getString(R.string.ADMOB_NATIVE_WITHOUT_MEDIA_V2),
            binding.shimmerLayout
        )
    }

    private fun initViews() {
        val shake: Animation = AnimationUtils.loadAnimation(this.applicationContext, R.anim.shake)
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                withContext(Dispatchers.Main) {
                    binding.iconArrow2.startAnimation(shake)
                }
                delay(4000)
            }
        }


        if (!MyApplication.mInstance.preferenceManager.getBoolean(
                PreferenceManager.Key.isDarkTheme,
                false
            )
        ) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        binding.backIcon.setOnClickListener {
            finish()
        }

        binding.createSoundBtn.setOnClickListener {
            disableMultipleClicking(it, 1000)
            startActivity(Intent(this, CreateSoundActivity::class.java))
        }
    }

    private fun addSoundItems() {
        lifecycleScope.launch {
            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.door_bell),
                    R.drawable.door_bell_sound,
                    true,
                    Uri.parse("android.resource://" + packageName + "/" + R.raw.doorbell_sound)
                        .toString()
                )
            )
            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.cat_meow),
                    R.drawable.cat_sounds,
                    true,
                    Uri.parse("android.resource://" + packageName + "/" + R.raw.cat_sound)
                        .toString()
                )
            )
            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.dog_barking),
                    R.drawable.dog_sound,
                    true,
                    Uri.parse("android.resource://" + packageName + "/" + R.raw.dog_sound)
                        .toString()
                )
            )
            soundsArrayList.add(
                SoundsDataClass(
                    "R.string.say_he",
                    R.drawable.hey_sound,
                    false,
                    Uri.parse("android.resource://" + packageName + "/" + R.raw.hey_sound)
                        .toString()
                )
            )
            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.whistlee),
                    R.drawable.whistle_sound,
                    false,
                    Uri.parse("android.resource://" + packageName + "/" + R.raw.whistle_sound)
                        .toString()
                )
            )

            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.car_horn),
                    R.drawable.car_horn_sound,
                    false,
                    Uri.parse("android.resource://" + packageName + "/" + R.raw.car_horn_sound)
                        .toString()
                )
            )
            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.hello),
                    R.drawable.hello_sound,
                    false,
                    Uri.parse("android.resource://" + packageName + "/" + R.raw.hello_sound)
                        .toString()
                )
            )
            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.party_horn),
                    R.drawable.party_horn_sound,
                    false,
                    Uri.parse("android.resource://" + packageName + "/" + R.raw.party_sound)
                        .toString()
                )
            )
            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.police_horn),
                    R.drawable.police_whistle_sound,
                    false,
                    Uri.parse("android.resource://" + packageName + "/" + R.raw.police_sound)
                        .toString()
                )
            )

        }

        /**
         * RV Call
         */
        initRV()
    }

    private fun initRV() {
        binding.soundsRv.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(4)
            layoutManager = GridLayoutManager(this@SelectSoundActivity, 3)
            adapter =
                SoundsAdapter(this@SelectSoundActivity, soundsArrayList, -1,this@SelectSoundActivity)
        }
    }


    override fun clickOnSoundListener(
        position: Int,
        premium: Boolean,
        img: Int,
        title: String,
        audioUri: String
    ) {
        val safeTitle = sanitizeTitleForEvent(title)
        AnalyticsManager.logEvent("FA_${safeTitle}_sound")
        if (MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_PREMIUM)) {
            if (audioUri.isNullOrEmpty()) {

                Log.e(
                    ContentValues.TAG,
                    "audioUri is null or empty for sound: $title (position: $position)"
                )
                return
            }
            if (audioUri.isNullOrEmpty()) {

                Log.e(
                    ContentValues.TAG,
                    "❌ audioUri is null or empty for sound: $title (position: $position)"
                )
                return
            }
            val intent = Intent(this, SoundPreviewActivity::class.java)
            intent.putExtra("img", img)
            intent.putExtra("title", title)
            intent.putExtra("uri", audioUri.toString())
            Log.d(ContentValues.TAG, "uriStringCheck set: $audioUri")
            startActivity(intent)
        } else {
            if (position > 2) {
                Utils.watchAdOrBuyPremium(
                    this@SelectSoundActivity,
                    onBuyPremium = {
                        startActivity(
                            Intent(
                                this@SelectSoundActivity,
                                PremiumScreenActivity::class.java
                            )
                        )
                    }) {
                    // here code after ad
                    Log.d("SoundsPlaying", "clickOnSoundListener:$position ")

                    val intent = Intent(this, SoundPreviewActivity::class.java)
                    intent.putExtra("img", img)
                    intent.putExtra("title", title)
                    intent.putExtra("uri", audioUri.toString())
                    Log.d(ContentValues.TAG, "uriStringCheck set: $audioUri")
                    startActivity(intent)
                }
            } else {
                if (audioUri.isNullOrEmpty()) {

                    Log.e(
                        ContentValues.TAG,
                        "❌ audioUri is null or empty for sound: $title (position: $position)"
                    )
                    return
                }
                val intent = Intent(this, SoundPreviewActivity::class.java)
                intent.putExtra("img", img)
                intent.putExtra("title", title)
                intent.putExtra("uri", audioUri.toString())
                Log.d(ContentValues.TAG, "uriStringCheck set: $audioUri")
                startActivity(intent)
            }
        }
    }

    fun sanitizeTitleForEvent(title: String): String {
        val replaced = title.replace(" ", "_")
        return replaced.replace(Regex("[^A-Za-z0-9_]"), "")
    }

    override fun onDeleteSound(position: Int, sound: SoundsDataClass) {
        TODO("Not yet implemented")
    }
}