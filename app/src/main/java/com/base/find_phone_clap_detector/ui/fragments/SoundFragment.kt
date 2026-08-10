package com.base.find_phone_clap_detector.ui.fragments

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.base.find_phone_clap_detector.myApplication.MyApplication
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.FragmentSoundBinding
import com.base.find_phone_clap_detector.managers.PreferenceManager
import com.base.find_phone_clap_detector.ui.activities.PremiumScreenActivity
import com.base.find_phone_clap_detector.ui.activities.SoundPreviewActivity
import com.base.find_phone_clap_detector.ui.adapters.SoundsAdapter
import com.base.find_phone_clap_detector.ui.dataClasses.SoundsDataClass
import com.base.find_phone_clap_detector.ui.interfaces.SoundInterface
import kotlinx.coroutines.launch

class SoundFragment : Fragment(),SoundInterface {

    private lateinit var binding: FragmentSoundBinding
    private var soundsArrayList: ArrayList<SoundsDataClass> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_sound, container, false)
        addSoundItems()
        // Inflate the layout for this fragment
        return binding.root
    }
    private fun initRV() {
        binding.soundsRv.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(4)
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = SoundsAdapter(requireContext(),soundsArrayList, -1,this@SoundFragment)
        }
    }
    private fun addSoundItems() {
        lifecycleScope.launch {
            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.door_bell),
                    R.drawable.door_bell_sound,
                    true,
                    Uri.parse("android.resource://" + requireContext().packageName  + "/" + R.raw.doorbell_sound).toString()
                )
            )
            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.cat_meow),
                    R.drawable.cat_sounds,
                    true,
                    Uri.parse("android.resource://" + requireContext().packageName  + "/" + R.raw.cat_sound).toString()
                )
            )
            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.dog_barking),
                    R.drawable.dog_sound,
                    true,
                    Uri.parse("android.resource://" + requireContext().packageName  + "/" + R.raw.dog_sound).toString()
                )
            )
            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.say_he),
                    R.drawable.hey_sound,
                    false,
                    Uri.parse("android.resource://" + requireContext().packageName  + "/" + R.raw.hey_sound).toString()
                )
            )
            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.whistlee),
                    R.drawable.whistle_sound,
                    false,
                    Uri.parse("android.resource://" + requireContext().packageName  + "/" + R.raw.whistle_sound).toString()
                )
            )

            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.car_horn),
                    R.drawable.car_horn_sound,
                    false,
                    Uri.parse("android.resource://" + requireContext().packageName  + "/" + R.raw.car_horn_sound).toString()
                )
            )
            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.hello),
                    R.drawable.hello_sound,
                    false,
                    Uri.parse("android.resource://" + requireContext().packageName  + "/" + R.raw.hello_sound).toString()
                )
            )
            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.party_horn),
                    R.drawable.party_horn_sound,
                    false,
                    Uri.parse("android.resource://" + requireContext().packageName  + "/" + R.raw.party_sound).toString()
                )
            )
            soundsArrayList.add(
                SoundsDataClass(
                    getString(R.string.police_horn),
                    R.drawable.police_whistle_sound,
                    false,
                    Uri.parse("android.resource://" + requireContext().packageName  + "/" + R.raw.police_sound).toString()
                )
            )

        }

        /**
         * RV Call
         */
        initRV()
    }

    override fun clickOnSoundListener(
        position: Int,
        premium: Boolean,
        img: Int,
        title: String,
        audioUri: String
    ) {

        if(MyApplication.mInstance.preferenceManager.getBoolean(PreferenceManager.Key.IS_APP_PREMIUM)){
            val intent = Intent(requireContext(),SoundPreviewActivity::class.java)
            intent.putExtra("img", img)
            intent.putExtra("title", title)
            intent.putExtra("uri", audioUri.toString())
            Log.d(ContentValues.TAG, "uriStringCheck set: $audioUri")
            startActivity(intent)
        } else {
            Log.d("SoundsPlaying", "clickOnSoundListener:$position ")
            if(position>2){
                startActivity(Intent(requireContext(),PremiumScreenActivity::class.java))
            }else{
                val intent = Intent(requireContext(),SoundPreviewActivity::class.java)
                intent.putExtra("img", img)
                intent.putExtra("title", title)
                intent.putExtra("uri", audioUri.toString())
                Log.d(ContentValues.TAG, "uriStringCheck set: $audioUri")
                startActivity(intent)
            }

        }

    }

    override fun onDeleteSound(position: Int, sound: SoundsDataClass) {

    }
}