/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.welcome.fragment;

import static androidx.fragment.app.FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.windscribe.mobile.R;
import com.windscribe.mobile.fragments.FeatureFragments;
import com.windscribe.mobile.fragments.FeaturePageTransformer;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class WelcomeFragment extends Fragment implements ViewPager.OnPageChangeListener {

    @BindView(R.id.logo)
    ImageView logo;

    @BindView(R.id.feature_pager)
    ViewPager mViewPager;

    @BindView(R.id.featureTabDots)
    TabLayout tabLayout;

    private FragmentCallback fragmentCallback;

    private Runnable mAutoPagingRunnable;

    private PagerAdapter mPagerAdapter;

    private int mScrollState = 0;

    private boolean mSlideLeft = true;

    private Timer pagerTimer;

    public WelcomeFragment() {

    }

    @Override
    public void onAttach(@NonNull Context context) {
        if (getActivity() instanceof FragmentCallback) {
            fragmentCallback = (FragmentCallback) getActivity();
        }
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setGif();
        setPagerAdapter();
        setUpAutoPaging();
    }

    @Override
    public void onDestroyView() {
        stopPagerSchedule();
        super.onDestroyView();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        mScrollState = state;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
    }

    public void setPagerAdapter() {
        mPagerAdapter = new FragmentPagerAdapter(getChildFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            @Override
            public int getCount() {
                return 4;
            }

            @NonNull
            @Override
            public Fragment getItem(int position) {
                return FeatureFragments.newInstance(position);
            }
        };
        tabLayout.setupWithViewPager(mViewPager, true);
        mViewPager.addOnPageChangeListener(this);
        mViewPager.setPageTransformer(false, new FeaturePageTransformer());
        mViewPager.setAdapter(mPagerAdapter);
    }

    public void setUpAutoPaging() {
        mAutoPagingRunnable = () -> {
            if (!mViewPager.isFakeDragging() && mScrollState == 0) {
                mViewPager.beginFakeDrag();
                ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
                valueAnimator.setInterpolator(new AccelerateInterpolator(1.5f));
                valueAnimator.setDuration(2000);
                valueAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationCancel(Animator animator) {
                        if (mViewPager == null) {
                            return;
                        }
                        mViewPager.endFakeDrag();
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        try {
                            if (mViewPager == null | mPagerAdapter == null) {
                                return;
                            }
                            if (mViewPager.isFakeDragging()) {
                                mViewPager.endFakeDrag();
                            }
                            if (mViewPager.getCurrentItem() == mPagerAdapter.getCount() - 1) {
                                mSlideLeft = false;
                            } else if (mViewPager.getCurrentItem() == 0) {
                                mSlideLeft = true;
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }

                    @Override
                    public void onAnimationStart(Animator animator) {
                        mViewPager.beginFakeDrag();
                    }
                });
                valueAnimator.addUpdateListener(valueAnimator1 -> {
                    if (mViewPager == null | mPagerAdapter == null) {
                        return;
                    }
                    try {
                        if (mViewPager.isFakeDragging() && mPagerAdapter.getCount() > 0) {
                            if (mSlideLeft) {
                                mViewPager.fakeDragBy(
                                        -valueAnimator1.getAnimatedFraction() * mViewPager.getWidth() / 2);
                            } else {
                                mViewPager
                                        .fakeDragBy(valueAnimator1.getAnimatedFraction() * mViewPager.getWidth() / 2);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                });
                valueAnimator.start();
            }
        };
        startPagerSchedule();
    }

    public void startPagerSchedule() {
        Handler handler = new Handler();
        pagerTimer = new Timer();
        pagerTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(mAutoPagingRunnable);
            }
        }, 3000, 3000);
    }

    public void stopPagerSchedule() {
        pagerTimer.cancel();
        pagerTimer = null;
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.get_started_button)
    void onGetStartedButtonClick() {
        fragmentCallback.onContinueWithOutAccountClick();
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.loginButton)
    void onLoginButtonClick() {
        fragmentCallback.onLoginClick();
    }

    private void setGif() {
        Glide.with(this).load(R.raw.wsbadge).into(logo);
    }
}