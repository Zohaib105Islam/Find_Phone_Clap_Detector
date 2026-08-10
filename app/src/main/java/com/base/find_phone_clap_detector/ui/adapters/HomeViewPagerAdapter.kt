package com.base.find_phone_clap_detector.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.base.find_phone_clap_detector.ui.fragments.AddSoundsFragment
import com.base.find_phone_clap_detector.ui.fragments.ActivateDetectionFragment
import com.base.find_phone_clap_detector.ui.fragments.HomeFragment
import com.base.find_phone_clap_detector.ui.fragments.SoundFragment

class HomeViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> ActivateDetectionFragment()
            2 -> SoundFragment()
            3 -> AddSoundsFragment()
            else -> ActivateDetectionFragment()
        }
    }
}
