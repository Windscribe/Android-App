/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.windscribe.tv.R
import com.windscribe.tv.databinding.ForgotPasswordBinding

class ForgotPasswordFragment : Fragment() {
    private lateinit var binding: ForgotPasswordBinding
    private lateinit var carouselHelper: CarouselHelper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup carousel
        carouselHelper = CarouselHelper(requireContext())
        val viewPager = binding.root.findViewById<ViewPager2>(R.id.feature_carousel)
        val indicators = binding.root.findViewById<LinearLayout>(R.id.carousel_indicators)
        carouselHelper.setupCarousel(viewPager, indicators)

        binding.back.requestFocus()
        binding.back.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.back.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.back.setTextColor(resources.getColor(R.color.colorWhite))
            } else {
                binding.back.setTextColor(resources.getColor(R.color.colorWhite50))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        carouselHelper.onResume()
    }

    override fun onPause() {
        super.onPause()
        carouselHelper.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        carouselHelper.onDestroy()
    }
}
