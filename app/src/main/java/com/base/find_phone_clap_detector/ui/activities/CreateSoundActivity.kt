package com.base.find_phone_clap_detector.ui.activities

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.horse.identification.extensions.setSafeOnClickListener
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.ActivityCreateSoundBinding
import com.base.find_phone_clap_detector.managers.AdsManager
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.myApplication.MyApplication
import com.base.find_phone_clap_detector.ui.adapters.CreatedSoundsAdapter
import com.base.find_phone_clap_detector.ui.dataClasses.SoundsDataClass
import com.base.find_phone_clap_detector.ui.repository.AudioRepository
import com.base.find_phone_clap_detector.utils.KEY
import com.base.find_phone_clap_detector.utils.LocaleHelper
import com.base.find_phone_clap_detector.utils.TinyDB
import com.base.find_phone_clap_detector.utils.disableMultipleClicking
import com.base.find_phone_clap_detector.utils.singleToast
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CreateSoundActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateSoundBinding

    @Inject
    lateinit var audioRepository: AudioRepository
    private lateinit var tinyDB: TinyDB
    private var adapter: CreatedSoundsAdapter? = null

    private var soundsList = ArrayList<SoundsDataClass>()


    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply current app locale before view inflation
        LocaleHelper.setAppLocale(this)
        super.onCreate(savedInstanceState)
        binding = ActivityCreateSoundBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.main.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        tinyDB = TinyDB(this@CreateSoundActivity)
        initViews()
        initAdapter()
        initAds()
    }

    private fun initAds() {
        // loadNativeAd
        MyApplication.mInstance.adsManager.loadNativeAd(
            this,
            binding.adFrame,
            AdsManager.NativeAdType.NOMEDIA_MEDIUM,
            this.getString(R.string.ADMOB_NATIVE_WITHOUT_MEDIA_V2),
            binding.shimmerLayout
        )
    }

    private fun initViews() {
        if (!MyApplication.mInstance.preferenceManager.getBoolean(
                PreferenceManager.Key.isDarkTheme,
                false
            )
        ) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        binding.backIcon.setSafeOnClickListener {
            disableMultipleClicking(it, 1000)
            finish()
        }
    }

    private fun initAdapter() {

        soundsList = getFavoriteItems()

        if (soundsList.isNotEmpty()) {

            binding.soundsRv.visibility = View.VISIBLE
            binding.nothingImg.visibility = View.GONE

            adapter = CreatedSoundsAdapter(
                this@CreateSoundActivity,
                soundsList,

                onDeleteSound = { position, sound ->
                    showDeleteDialog(position, sound)
                },

                onApplySound = { position, sound ->

                    if (sound.audioUri.isNullOrEmpty()) {
                        Toast.makeText(
                            this,
                            "Audio URI missing! Unable to open preview.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@CreatedSoundsAdapter
                    }

                    val intent = Intent(this, SoundPreviewActivity::class.java)
                    intent.putExtra("img", sound.img)
                    intent.putExtra("position", position)
                    intent.putExtra("title", sound.title)
                    intent.putExtra("isFromCreateSound", true)
                    intent.putExtra("uri", sound.audioUri.toString())
                    startActivity(intent)
                    finish()
                }
            )

            binding.soundsRv.adapter = adapter

        } else {
            binding.soundsRv.visibility = View.GONE
            binding.nothingImg.visibility = View.VISIBLE
        }
    }

    private fun getFavoriteItems(): ArrayList<SoundsDataClass> {
        return tinyDB.getListObject(KEY.sounds, SoundsDataClass::class.java)
    }


    private fun showDeleteDialog(position: Int, sound: SoundsDataClass) {
        val dialog = Dialog(this@CreateSoundActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.delete_dialogue)

        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        dialog.findViewById<TextView>(R.id.yes).setSafeOnClickListener {

            disableMultipleClicking(it, 1000)

            // Remove from current list
            soundsList.removeAt(position)

            // Save updated list
            tinyDB.putListObject(KEY.sounds, soundsList)

            // Notify adapter properly
            adapter?.notifyItemRemoved(position)
            adapter?.notifyItemRangeChanged(position, soundsList.size)

            // Handle empty state
            if (soundsList.isEmpty()) {
                binding.soundsRv.visibility = View.GONE
                binding.nothingImg.visibility = View.VISIBLE
            }

            singleToast("${sound.title} deleted")

            dialog.dismiss()
        }

        dialog.findViewById<ImageView>(R.id.closeBtn).setSafeOnClickListener {
            disableMultipleClicking(it, 1000)
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.no).setSafeOnClickListener {
            disableMultipleClicking(it, 1000)
            dialog.dismiss()
        }

        try {
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}