/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.welcome.fragment

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.windscribe.mobile.R
import com.windscribe.mobile.fragments.FeatureFragments
import com.windscribe.mobile.fragments.FeaturePageTransformer
import java.util.*

class WelcomeFragment : Fragment(), OnPageChangeListener {
    @BindView(R.id.logo)
    lateinit var logo: ImageView

    @BindView(R.id.feature_pager)
    lateinit var mViewPager: ViewPager

    @BindView(R.id.featureTabDots)
    lateinit var tabLayout: TabLayout

    private var fragmentCallback: FragmentCallback? = null
    private var autoPagingRunnable: Runnable? = null
    private var pagerAdapter: PagerAdapter? = null
    private var scrollState = 0
    private var slideLeft = true
    private var pagerTimer: Timer? = null

    override fun onAttach(context: Context) {
        if (activity is FragmentCallback) {
            fragmentCallback = activity as FragmentCallback?
        }
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setGif()
        setPagerAdapter()
        setUpAutoPaging()
    }

    override fun onDestroyView() {
        stopPagerSchedule()
        super.onDestroyView()
    }

    override fun onPageScrollStateChanged(state: Int) {
        scrollState = state
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) {}
    private fun setPagerAdapter() {
        pagerAdapter = object :
            FragmentPagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getCount(): Int {
                return 4
            }

            override fun getItem(position: Int): Fragment {
                return FeatureFragments.newInstance(position)
            }
        }
        tabLayout.setupWithViewPager(mViewPager, true)
        mViewPager.addOnPageChangeListener(this)
        mViewPager.setPageTransformer(false, FeaturePageTransformer())
        mViewPager.adapter = pagerAdapter
    }

    private fun setUpAutoPaging() {
        autoPagingRunnable = Runnable {
            if (!mViewPager.isFakeDragging && scrollState == 0) {
                mViewPager.beginFakeDrag()
                val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
                valueAnimator.interpolator = AccelerateInterpolator(1.5f)
                valueAnimator.duration = 2000
                valueAnimator.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationCancel(animator: Animator) {
                        mViewPager.endFakeDrag()
                    }

                    override fun onAnimationEnd(animator: Animator) {
                        try {
                            if (mViewPager.isFakeDragging) {
                                mViewPager.endFakeDrag()
                            }
                            if (mViewPager.currentItem == (pagerAdapter?.count ?: 0) - 1) {
                                slideLeft = false
                                mViewPager.currentItem = 0
                            } else if (mViewPager.currentItem == 0) {
                                slideLeft = true
                            }
                        } catch (ignored: Exception) {
                        }
                    }

                    override fun onAnimationRepeat(animator: Animator) {}
                    override fun onAnimationStart(animator: Animator) {
                        mViewPager.beginFakeDrag()
                    }
                })
                valueAnimator.addUpdateListener { valueAnimator1: ValueAnimator ->
                    if (pagerAdapter == null) {
                        return@addUpdateListener
                    }
                    try {
                        if (mViewPager.isFakeDragging && (pagerAdapter?.count ?: 0) > 0) {
                            if (slideLeft) {
                                mViewPager.fakeDragBy(
                                    -valueAnimator1.animatedFraction * mViewPager.width / 2
                                )
                            }
                        }
                    } catch (ignored: Exception) {
                    }
                }
                valueAnimator.start()
            }
        }
        startPagerSchedule()
    }

    private fun startPagerSchedule() {
        val handler = Handler()
        pagerTimer = Timer()
        pagerTimer?.schedule(object : TimerTask() {
            override fun run() {
                autoPagingRunnable?.let { handler.post(it) }
            }
        }, 3000, 3000)
    }

    private fun stopPagerSchedule() {
        pagerTimer?.cancel()
        pagerTimer = null
    }

    @OnClick(R.id.get_started_button)
    fun onGetStartedButtonClick() {
        fragmentCallback?.onContinueWithOutAccountClick()
    }

    @OnClick(R.id.loginButton)
    fun onLoginButtonClick() {
        fragmentCallback?.onLoginClick()
    }

    @OnClick(R.id.emergencyConnectButton)
    fun onEmergencyButtonClick() {
        fragmentCallback?.onEmergencyClick()
    }

    private fun setGif() {
        Glide.with(this).load(R.raw.wsbadge).into(logo)
    }
}