/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.windscribe.tv.R
import com.windscribe.tv.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {
    private lateinit var binding: FragmentWelcomeBinding
    private var fragmentCallback: FragmentCallback? = null
    private lateinit var carouselHelper: CarouselHelper

    override fun onAttach(context: Context) {
        if (activity is FragmentCallback) {
            fragmentCallback = activity as FragmentCallback?
        }
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup carousel
        carouselHelper = CarouselHelper(requireContext())
        val viewPager = binding.root.findViewById<ViewPager2>(R.id.feature_carousel)
        val indicators = binding.root.findViewById<LinearLayout>(R.id.carousel_indicators)
        carouselHelper.setupCarousel(viewPager, indicators)

        binding.continueWithoutAccount.requestFocus()
        binding.login.setOnFocusChangeListener { v, hasFocus ->
            resetButtonTextColor()
        }
        binding.continueWithoutAccount.setOnFocusChangeListener { v, hasFocus ->
            resetButtonTextColor()
        }
        binding.login.setOnClickListener {
            fragmentCallback?.onLoginClick()
        }
        binding.continueWithoutAccount.setOnClickListener {
            fragmentCallback?.onGetStartedClick()
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

    private fun resetButtonTextColor() {
        if (activity == null) {
            return
        }
        binding.login.setTextColor(
            if (binding.login.hasFocus()) requireActivity().resources.getColor(R.color.colorWhite) else requireActivity().resources.getColor(
                R.color.colorWhite50
            )
        )
        binding.continueWithoutAccount.setTextColor(
            if (binding.continueWithoutAccount.hasFocus()) requireActivity().resources.getColor(R.color.colorWhite) else requireActivity().resources.getColor(
                R.color.colorWhite50
            )
        )
        if (binding.continueWithoutAccount.hasFocus()) {
            binding.buttonLabel.text = ""
        } else {
            binding.buttonLabel.text = getString(com.windscribe.vpn.R.string.login_label)
        }
    }
}
