/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.windscribe.tv.R

class CarouselHelper(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var currentPage = 0
    private val indicators = mutableListOf<ImageView>()
    private var viewPager: ViewPager2? = null

    private val features = listOf(
        FeatureItem(R.drawable.feature_servers, "Servers in over 69 countries and 134 cities."),
        FeatureItem(R.drawable.feature_secure, "Automatically Secure any Network"),
        FeatureItem(R.drawable.feature_logging, "Strict No-Logging Policy"),
        FeatureItem(R.drawable.feature_quick, "Works with Shortcuts & Quick Settings")
    )

    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            currentPage = (currentPage + 1) % features.size
            viewPager?.setCurrentItem(currentPage, true)
            handler.postDelayed(this, 3000)
        }
    }

    fun setupCarousel(carouselView: ViewPager2, indicatorsLayout: LinearLayout) {
        viewPager = carouselView

        val adapter = FeatureCarouselAdapter(features)
        viewPager?.adapter = adapter

        // Disable user input and focus
        viewPager?.isUserInputEnabled = false
        viewPager?.isFocusable = false
        viewPager?.isFocusableInTouchMode = false

        // Disable focus on RecyclerView inside ViewPager2
        viewPager?.getChildAt(0)?.isFocusable = false
        viewPager?.getChildAt(0)?.isFocusableInTouchMode = false

        // Setup indicators
        setupIndicators(indicatorsLayout, features.size)
        setCurrentIndicator(0)

        viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
                setCurrentIndicator(position)
            }
        })

        // Start auto-scroll
        handler.postDelayed(autoScrollRunnable, 3000)
    }

    private fun setupIndicators(layout: LinearLayout, count: Int) {
        indicators.clear()
        layout.removeAllViews()

        val indicatorSize = context.resources.getDimensionPixelSize(R.dimen.reg_8dp)
        val indicatorMargin = context.resources.getDimensionPixelSize(R.dimen.reg_4dp)

        for (i in 0 until count) {
            val indicator = ImageView(context).apply {
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(context, R.color.colorWhite))
                }
                setImageDrawable(drawable)
                layoutParams = LinearLayout.LayoutParams(indicatorSize, indicatorSize).apply {
                    setMargins(indicatorMargin, 0, indicatorMargin, 0)
                }
                alpha = 0.4f
            }
            indicators.add(indicator)
            layout.addView(indicator)
        }
    }

    private fun setCurrentIndicator(position: Int) {
        indicators.forEachIndexed { index, imageView ->
            imageView.alpha = if (index == position) 1.0f else 0.4f
        }
    }

    fun onResume() {
        handler.postDelayed(autoScrollRunnable, 3000)
    }

    fun onPause() {
        handler.removeCallbacks(autoScrollRunnable)
    }

    fun onDestroy() {
        handler.removeCallbacks(autoScrollRunnable)
        viewPager = null
        indicators.clear()
    }
}