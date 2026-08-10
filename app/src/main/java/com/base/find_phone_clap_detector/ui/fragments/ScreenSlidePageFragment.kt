package com.base.find_phone_clap_detector.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.databinding.FragmentScreenSlidePageBinding


class ScreenSlidePageFragment : Fragment() {
    private val ARG_OBJECT = "position"


    private var _binding: FragmentScreenSlidePageBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentScreenSlidePageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.takeIf { it.containsKey(ARG_OBJECT) }?.apply {
            when (getInt(ARG_OBJECT)) {
                1 -> {
                    binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.onboarding_clap_new))
                    binding.titleTv.text = getString(R.string.find_your_phone_with_a_clap)
                    binding.descriptionTv.text =
                        getString(R.string.simply_clap_your_hands_and_your_phone_will_ring_instantly_no_searching_needed)
                }
                2 -> {
                    binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.onboarding_pocket_guard_new))
                    binding.titleTv.text = getString(R.string.onboarding_pocket_guard_title)
                    binding.descriptionTv.text =
                        getString(R.string.onboarding_pocket_guard_description)
                }
                3 -> {
                    binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.onboarding_whistle_new))
                    binding.titleTv.text = getString(R.string.find_phone_via_whistle)
                    binding.descriptionTv.text =
                        getString(R.string.just_whistle_and_your_phone_will_ring_instantly)
                }
            }
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
