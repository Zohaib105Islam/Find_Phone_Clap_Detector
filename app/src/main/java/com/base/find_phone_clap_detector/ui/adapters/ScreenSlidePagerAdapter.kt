package com.base.find_phone_clap_detector.ui.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.base.find_phone_clap_detector.ui.fragments.ScreenSlidePageFragment
import com.base.find_phone_clap_detector.utils.Constants

class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = Constants.NUMBER_OF_ON_BOARDING_SLIDER

    override fun createFragment(position: Int): Fragment {
        //create a new fragment for each slider

        val fragment = ScreenSlidePageFragment()
        fragment.arguments = Bundle().apply {
            val ARG_OBJECT = "position"
            putInt(ARG_OBJECT, position + 1)
        }
        return fragment
    }
}