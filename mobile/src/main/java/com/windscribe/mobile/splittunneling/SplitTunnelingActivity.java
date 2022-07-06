/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.splittunneling;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;

import com.windscribe.mobile.R;
import com.windscribe.mobile.adapter.InstalledAppsAdapter;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.mobile.di.DaggerActivityComponent;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.backend.utils.WindVpnController;
import com.windscribe.vpn.commonutils.ThemeUtils;
import com.windscribe.vpn.constants.AnimConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemSelected;

public class SplitTunnelingActivity extends BaseActivity implements SplitTunnelingView {

    @BindView(R.id.tv_current_tunnel_mode)
    TextView currentRoutingMode;

    @BindView(R.id.img_tunnel_toggle_btn)
    ImageView imgTunnelToggle;

    @BindView(R.id.nav_title)
    TextView mActivityTitle;

    @BindView(R.id.recycler_view_app_list)
    RecyclerView mAppListRecyclerView;

    final ConstraintSet mConstraintSetTunnel = new ConstraintSet();

    @BindView(R.id.cl_split_tunnel_settings)
    ConstraintLayout mMainContainer;

    @Inject
    SplitTunnelingPresenter mSplitPresenter;

    androidx.transition.AutoTransition mTransition;

    @BindView(R.id.minimize_icon)
    ImageView minimizeIcon;

    @BindView(R.id.progress)
    ProgressBar progressBar;

    @BindView(R.id.searchView)
    SearchView searchView;

    @BindView(R.id.spinner_tunnel_mode)
    Spinner splitRoutingModeSpinner;

    @BindView(R.id.tv_tunnel_mode_description)
    TextView tunnelModeDescription;

    @Inject
    WindVpnController windVpnController;

    private final String TAG = "split_settings_a";

    private final Logger mSplitViewLog = LoggerFactory.getLogger(TAG);

    private final AtomicBoolean setView = new AtomicBoolean();

    public static Intent getStartIntent(Context context) {
        return new Intent(context, SplitTunnelingActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityModule(new ActivityModule(this, this)).inject(this);
        setContentLayout(R.layout.activity_split_tunneling,true);
        mConstraintSetTunnel.clone(mMainContainer);
        setView.set(true);
        mMainContainer.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (setView.getAndSet(false)) {
                mSplitPresenter.setupLayoutBasedOnPreviousSettings();
            }

        });
        setUpCustomSearchBox();
        mActivityTitle.setText(getString(R.string.split_tunneling));
    }

    @Override
    protected void onDestroy() {
        mSplitPresenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public String[] getSplitRoutingModes() {
        return getResources().getStringArray(R.array.split_mode_list);
    }

    @Override
    public void hideTunnelSettingsLayout() {
        mSplitViewLog.info("Setting up layout for split tunnel settings on..");
        mConstraintSetTunnel.setVisibility(R.id.cl_mode, ConstraintSet.GONE);
        mConstraintSetTunnel.setVisibility(R.id.cl_app_list, ConstraintSet.GONE);
        minimizeIcon.setVisibility(View.GONE);
        mConstraintSetTunnel.setVisibility(R.id.minimize_icon, ConstraintSet.GONE);

        //Start transition
        mTransition = new AutoTransition();
        mTransition.setDuration(AnimConstants.CONNECTION_MODE_ANIM_DURATION);
        mTransition.addListener(new androidx.transition.Transition.TransitionListener() {
            @Override
            public void onTransitionCancel(@NonNull androidx.transition.Transition transition) {
                transition.removeListener(this);
            }

            @Override
            public void onTransitionEnd(@NonNull androidx.transition.Transition transition) {
                mSplitViewLog.info("Show split tunnel mode transition finished...");
                //ConnSettingsPresenter.onManualLayoutSetupCompleted();
                transition.removeListener(this);
            }

            @Override
            public void onTransitionPause(@NonNull androidx.transition.Transition transition) {
                transition.removeListener(this);
            }

            @Override
            public void onTransitionResume(@NonNull androidx.transition.Transition transition) {

            }

            @Override
            public void onTransitionStart(@NonNull androidx.transition.Transition transition) {

            }
        });
        androidx.transition.TransitionManager.beginDelayedTransition(mMainContainer, mTransition);
        mConstraintSetTunnel.applyTo(mMainContainer);
    }

    @OnClick(R.id.nav_button)
    public void onBackButtonPressed() {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        mSplitPresenter.onBackPressed();
        super.onBackPressed();
    }

    @OnClick({R.id.tv_current_tunnel_mode, R.id.img_tunnel_drop_down_btn})
    public void onCurrentTunnelModeClick() {
        splitRoutingModeSpinner.performClick();
    }

    @OnItemSelected(R.id.spinner_tunnel_mode)
    public void onNewRoutingModeSelected(View view, @SuppressWarnings("unused") int position) {
        if (view != null) {
            ((TextView) view.findViewById(R.id.tv_drop_down)).setText("");
        }
        if (mSplitPresenter != null) {
            mSplitPresenter.onNewRoutingModeSelected(splitRoutingModeSpinner.getSelectedItem().toString());
        }
    }

    @Override
    public boolean onSearchRequested() {
        return false;
    }

    @OnClick(R.id.img_tunnel_toggle_btn)
    public void onToggleButtonClick() {
        mSplitViewLog.info("User clicked on split tunnel toggle button...");
        mSplitPresenter.onToggleButtonClicked();
    }

    @Override
    public void restartConnection() {
        windVpnController.connect();
    }

    @Override
    public void setRecyclerViewAdapter(InstalledAppsAdapter mAdapter) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setItemPrefetchEnabled(false);
        mAppListRecyclerView.setLayoutManager(layoutManager);
        mAppListRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void setSplitModeTextView(String mode, int textDescription) {
        currentRoutingMode.setText(mode);
        tunnelModeDescription.setText(getString(textDescription));
    }

    @Override
    public void setSplitRoutingModeAdapter(String[] modes, String savedMode) {
        ArrayAdapter<String> routeModeAdapter = new ArrayAdapter<>(this, R.layout.drop_down_layout, R.id.tv_drop_down,
                modes);
        splitRoutingModeSpinner.setAdapter(routeModeAdapter);
        splitRoutingModeSpinner.setSelected(false);
        splitRoutingModeSpinner.setSelection(routeModeAdapter.getPosition(savedMode));
        currentRoutingMode.setText(savedMode);
    }

    @Override
    public void setupToggleImage(Integer resourceId) {
        imgTunnelToggle.setImageResource(resourceId);
    }

    @Override
    public void showProgress(boolean progress) {
        if (progress) {
            minimizeIcon.setVisibility(View.GONE);
            searchView.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            searchView.setVisibility(View.VISIBLE);
            minimizeIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public void showTunnelSettingsLayout() {
        mSplitViewLog.info("Setting up layout for split tunnel settings on..");
        mConstraintSetTunnel.setVisibility(R.id.cl_mode, ConstraintSet.VISIBLE);
        mConstraintSetTunnel.setVisibility(R.id.cl_app_list, ConstraintSet.VISIBLE);

        //Start transition
        mTransition = new AutoTransition();
        mTransition.excludeTarget(R.id.minimize_icon, true);
        mTransition.setDuration(AnimConstants.CONNECTION_MODE_ANIM_DURATION);
        mTransition.addListener(new androidx.transition.Transition.TransitionListener() {
            @Override
            public void onTransitionCancel(@NonNull androidx.transition.Transition transition) {
                transition.removeListener(this);
            }

            @Override
            public void onTransitionEnd(@NonNull androidx.transition.Transition transition) {
                mSplitViewLog.info("Show split tunnel mode transition finished...");
                //ConnSettingsPresenter.onManualLayoutSetupCompleted();
                transition.removeListener(this);
            }

            @Override
            public void onTransitionPause(@NonNull androidx.transition.Transition transition) {
                transition.removeListener(this);
            }

            @Override
            public void onTransitionResume(@NonNull androidx.transition.Transition transition) {

            }

            @Override
            public void onTransitionStart(@NonNull androidx.transition.Transition transition) {

            }
        });
        androidx.transition.TransitionManager.beginDelayedTransition(mMainContainer, mTransition);
        mConstraintSetTunnel.applyTo(mMainContainer);

    }

    @OnClick(R.id.minimize_icon)
    void onMinimizeIconClick() {
        searchView.setQuery("", false);
        searchView.clearFocus();
        minimizeTopView(false);
    }

    private void minimizeTopView(boolean minimize) {
        mSplitViewLog.info("Setting up layout to max.." + minimize);
        if (minimize) {
            mConstraintSetTunnel.setVisibility(R.id.cl_mode, ConstraintSet.GONE);
            mConstraintSetTunnel.setVisibility(R.id.cl_top_bar, ConstraintSet.GONE);
            mConstraintSetTunnel.setVisibility(R.id.cl_switch, ConstraintSet.GONE);
            mConstraintSetTunnel.setVisibility(R.id.cl_app_list, ConstraintSet.VISIBLE);
        } else {
            mConstraintSetTunnel.setVisibility(R.id.minimize_icon, ConstraintSet.GONE);
            mConstraintSetTunnel.setVisibility(R.id.cl_top_bar, ConstraintSet.VISIBLE);
            mConstraintSetTunnel.setVisibility(R.id.cl_switch, ConstraintSet.VISIBLE);
            mConstraintSetTunnel.setVisibility(R.id.cl_app_list, ConstraintSet.VISIBLE);
            mConstraintSetTunnel.setVisibility(R.id.cl_mode, ConstraintSet.VISIBLE);

        }
        //Start transition
        mTransition = new AutoTransition();
        mTransition.setDuration(300);
        mTransition.addListener(new androidx.transition.Transition.TransitionListener() {
            @Override
            public void onTransitionCancel(@NonNull androidx.transition.Transition transition) {
                transition.removeListener(this);
            }

            @Override
            public void onTransitionEnd(@NonNull androidx.transition.Transition transition) {
                minimizeIcon.setVisibility(minimize ? View.VISIBLE : View.GONE);
                transition.removeListener(this);
            }

            @Override
            public void onTransitionPause(@NonNull androidx.transition.Transition transition) {
                transition.removeListener(this);
            }

            @Override
            public void onTransitionResume(@NonNull androidx.transition.Transition transition) {

            }

            @Override
            public void onTransitionStart(@NonNull androidx.transition.Transition transition) {

            }
        });
        mTransition.excludeChildren(R.id.recycler_view_app_list, true);
        androidx.transition.TransitionManager.beginDelayedTransition(mMainContainer, mTransition);
        mConstraintSetTunnel.applyTo(mMainContainer);
    }

    private void setUpCustomSearchBox() {
        // Search view
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("Search");
        searchView.setFocusable(false);
        // Filter results on text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String s) {
                mSplitPresenter.onFilter(s);
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String s) {
                searchView.clearFocus();
                return true;
            }
        });
        // Hide top layout items to make more room for search view and apps
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                minimizeTopView(true);
            }
        });

        // Search text
        TextView searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchText.setTextColor(ThemeUtils.getColor(this, R.attr.searchTextColor, R.color.colorWhite));
        searchText.setHintTextColor(ThemeUtils.getColor(this, R.attr.searchTextColor, R.color.colorWhite));
        searchText.setTextSize(Dimension.SP, 12);
        Typeface typeface = ResourcesCompat.getFont(this, R.font.ibm_plex_sans_bold);
        searchText.setTypeface(typeface);
        searchText.setPadding(0, 0, 0, 0);

        // Close button
        ImageView closeButton = this.searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        closeButton.setPadding(0, 0, 0, 0);
        closeButton.setOnClickListener(v -> {
            searchView.clearFocus();
            searchView.setQuery("", false);
            mSplitPresenter.onFilter("");
        });
        // Search icon
        ImageView searchIcon = this.searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
        searchIcon.setPadding(0, 0, 0, 0);
        searchIcon.setScaleType(ImageView.ScaleType.FIT_START);
        searchIcon.setImageTintList(
                ColorStateList.valueOf(ThemeUtils.getColor(this, R.attr.searchTextColor, R.color.colorWhite)));
    }
}
