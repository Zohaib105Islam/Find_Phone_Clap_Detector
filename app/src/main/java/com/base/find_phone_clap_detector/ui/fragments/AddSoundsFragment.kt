package com.base.find_phone_clap_detector.ui.fragments

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.FragmentAddSoundsBinding
import com.base.find_phone_clap_detector.ui.activities.CreateSoundActivity
import com.base.find_phone_clap_detector.ui.activities.SoundPreviewActivity
import com.base.find_phone_clap_detector.ui.adapters.SoundsAdapter
import com.base.find_phone_clap_detector.ui.dataClasses.SoundsDataClass
import com.base.find_phone_clap_detector.ui.interfaces.SoundInterface
import com.base.find_phone_clap_detector.utils.KEY
import com.base.find_phone_clap_detector.utils.TinyDB

@SuppressLint("LogNotTimber")
class AddSoundsFragment : Fragment(), SoundInterface {

    private lateinit var binding: FragmentAddSoundsBinding
    private lateinit var tinyDB: TinyDB
    private  var adapter: SoundsAdapter?=null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_add_sounds, container, false)

        initViews()

        return binding.root
    }

    private fun initViews() {
        binding.createSoundBtn.setOnClickListener{
            startActivity(Intent(requireContext() , CreateSoundActivity::class.java))
        }

        tinyDB = TinyDB(requireContext())
    }

    override fun onResume() {
        super.onResume()

        val favoriteItems = getFavoriteItems()
        if (favoriteItems.isNotEmpty()){
            binding.soundsRv.visibility = View.VISIBLE
            binding.nothingImg.visibility = View.GONE
            adapter = SoundsAdapter(requireContext() , getFavoriteItems(), -1,this)
            binding.soundsRv.adapter = adapter
        } else {
            binding.soundsRv.visibility = View.GONE
            binding.nothingImg.visibility = View.VISIBLE
        }

    }
    private fun getFavoriteItems(): ArrayList<SoundsDataClass> {
        return tinyDB.getListObject(KEY.sounds, SoundsDataClass::class.java)
    }

    override fun clickOnSoundListener(
        position: Int,
        premium: Boolean,
        img: Int,
        title: String,
        audioUri: String
    ) {
        val intent = Intent(requireContext(), SoundPreviewActivity::class.java)
        intent.putExtra("img", img)
        intent.putExtra("title", title)
        intent.putExtra("uri", audioUri)
        Log.d(ContentValues.TAG, "uriStringCheck set: $audioUri")
        startActivity(intent)
    }

    override fun onDeleteSound(position: Int, sound: SoundsDataClass) {
        TODO("Not yet implemented")
    }
}
