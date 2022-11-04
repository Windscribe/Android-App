/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.splittunneling

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.adapter.InstalledAppsAdapter
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.custom_view.preferences.ExpandableToggleView
import com.windscribe.mobile.custom_view.preferences.SplitRoutingModeView
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.ThemeUtils.getColor
import com.windscribe.vpn.constants.AnimConstants
import com.windscribe.vpn.constants.FeatureExplainer
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class SplitTunnelingActivity : BaseActivity(), SplitTunnelingView {

    @BindView(R.id.nav_title)
    lateinit var activityTitle: TextView

    @BindView(R.id.recycler_view_app_list)
    lateinit var appListRecyclerView: RecyclerView

    @BindView(R.id.cl_split_tunnel_settings)
    lateinit var mainContainer: ConstraintLayout

    @BindView(R.id.minimize_icon)
    lateinit var minimizeIcon: ImageView

    @BindView(R.id.progress)
    lateinit var progressBar: ProgressBar

    @BindView(R.id.searchView)
    lateinit var searchView: SearchView

    @BindView(R.id.cl_switch)
    lateinit var modeToggleView: ExpandableToggleView

    @Inject
    lateinit var windVpnController: WindVpnController

    @Inject
    lateinit var presenter: SplitTunnelingPresenter

    private var mTransition: AutoTransition? = null
    private val constraintSetTunnel = ConstraintSet()

    private val mSplitViewLog = LoggerFactory.getLogger("split_settings_a")
    private val setView = AtomicBoolean()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_split_tunneling, true)
        constraintSetTunnel.clone(mainContainer)
        setView.set(true)
        mainContainer.viewTreeObserver.addOnGlobalLayoutListener {
            if (setView.getAndSet(false)) {
                presenter.setupLayoutBasedOnPreviousSettings()
            }
        }
        setUpCustomSearchBox()
        activityTitle.text = getString(R.string.split_tunneling)
        setupCustomLayoutDelegates()
    }

    private fun setupCustomLayoutDelegates() {
        modeToggleView.delegate = object : ExpandableToggleView.Delegate {
            override fun onToggleClick() {
                presenter.onToggleButtonClicked()
            }

            override fun onExplainClick() {}
        }
        val splitRoutingModeView = modeToggleView.childView as SplitRoutingModeView
        splitRoutingModeView.delegate = object : SplitRoutingModeView.Delegate {
            override fun onModeSelect(mode: String) {
                presenter.onNewRoutingModeSelected(mode)
            }
        }
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override val splitRoutingModes: Array<String>
        get() = resources.getStringArray(R.array.split_mode_list)

    override fun hideTunnelSettingsLayout() {
        mSplitViewLog.info("Setting up layout for split tunnel settings on..")
        constraintSetTunnel.setVisibility(R.id.cl_app_list, ConstraintSet.GONE)
        minimizeIcon.visibility = View.GONE
        constraintSetTunnel.setVisibility(R.id.minimize_icon, ConstraintSet.GONE)

        //Start transition
        mTransition = AutoTransition()
        mTransition?.duration = AnimConstants.CONNECTION_MODE_ANIM_DURATION
        mTransition?.addListener(object : Transition.TransitionListener {
            override fun onTransitionCancel(transition: Transition) {
                transition.removeListener(this)
            }

            override fun onTransitionEnd(transition: Transition) {
                mSplitViewLog.info("Show split tunnel mode transition finished...")
                //ConnSettingsPresenter.onManualLayoutSetupCompleted();
                transition.removeListener(this)
            }

            override fun onTransitionPause(transition: Transition) {
                transition.removeListener(this)
            }

            override fun onTransitionResume(transition: Transition) {}
            override fun onTransitionStart(transition: Transition) {}
        })
        TransitionManager.beginDelayedTransition(mainContainer, mTransition)
        constraintSetTunnel.applyTo(mainContainer)
    }

    @OnClick(R.id.nav_button)
    fun onBackButtonPressed() {
        onBackPressed()
    }

    @OnClick(R.id.learn_more)
    fun onLearMoreClick() {
        openURLInBrowser(FeatureExplainer.SPLIT_TUNNELING)
    }

    override fun onBackPressed() {
        presenter.onBackPressed()
        super.onBackPressed()
    }

    override fun onSearchRequested(): Boolean {
        return false
    }

    override fun restartConnection() {
        windVpnController.connectAsync(false)
    }

    override fun setRecyclerViewAdapter(mAdapter: InstalledAppsAdapter) {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.isItemPrefetchEnabled = false
        appListRecyclerView.layoutManager = layoutManager
        appListRecyclerView.adapter = mAdapter
    }

    override fun setSplitModeTextView(mode: String, textDescription: Int) {
        modeToggleView.setDescription(textDescription)
    }

    override fun setSplitRoutingModeAdapter(modes: Array<String>, savedMode: String) {
        val splitRoutingModeView = modeToggleView.childView as SplitRoutingModeView
        splitRoutingModeView.setAdapter(savedMode, modes)
    }

    override fun setupToggleImage(resourceId: Int) {
        modeToggleView.setToggleImage(resourceId)
    }

    override fun showProgress(progress: Boolean) {
        if (progress) {
            minimizeIcon.visibility = View.GONE
            searchView.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
            searchView.visibility = View.VISIBLE
            minimizeIcon.visibility = View.GONE
        }
    }

    override fun showTunnelSettingsLayout() {
        mSplitViewLog.info("Setting up layout for split tunnel settings on..")
        constraintSetTunnel.setVisibility(R.id.cl_app_list, ConstraintSet.VISIBLE)

        //Start transition
        mTransition = AutoTransition()
        mTransition?.excludeTarget(R.id.minimize_icon, true)
        mTransition?.duration = AnimConstants.CONNECTION_MODE_ANIM_DURATION
        mTransition?.addListener(object : Transition.TransitionListener {
            override fun onTransitionCancel(transition: Transition) {
                transition.removeListener(this)
            }

            override fun onTransitionEnd(transition: Transition) {
                mSplitViewLog.info("Show split tunnel mode transition finished...")
                //ConnSettingsPresenter.onManualLayoutSetupCompleted();
                transition.removeListener(this)
            }

            override fun onTransitionPause(transition: Transition) {
                transition.removeListener(this)
            }

            override fun onTransitionResume(transition: Transition) {}
            override fun onTransitionStart(transition: Transition) {}
        })
        TransitionManager.beginDelayedTransition(mainContainer, mTransition)
        constraintSetTunnel.applyTo(mainContainer)
    }

    @OnClick(R.id.minimize_icon)
    fun onMinimizeIconClick() {
        searchView.setQuery("", false)
        searchView.clearFocus()
        minimizeTopView(false)
    }

    private fun minimizeTopView(minimize: Boolean) {
        mSplitViewLog.info("Setting up layout to max..$minimize")
        if (minimize) {
            constraintSetTunnel.setMargin(
                R.id.cl_app_list, ConstraintSet.TOP, resources.getDimension(
                    R.dimen.reg_16dp
                ).toInt()
            )
            constraintSetTunnel.setVisibility(R.id.cl_top_bar, ConstraintSet.GONE)
            constraintSetTunnel.setVisibility(R.id.cl_switch, ConstraintSet.GONE)
            constraintSetTunnel.setVisibility(R.id.cl_app_list, ConstraintSet.VISIBLE)
        } else {
            constraintSetTunnel.setMargin(R.id.cl_app_list, ConstraintSet.TOP, 0)
            constraintSetTunnel.setVisibility(R.id.minimize_icon, ConstraintSet.GONE)
            constraintSetTunnel.setVisibility(R.id.cl_top_bar, ConstraintSet.VISIBLE)
            constraintSetTunnel.setVisibility(R.id.cl_switch, ConstraintSet.VISIBLE)
            constraintSetTunnel.setVisibility(R.id.cl_app_list, ConstraintSet.VISIBLE)
        }
        //Start transition
        mTransition = AutoTransition()
        mTransition?.duration = 300
        mTransition?.addListener(object : Transition.TransitionListener {
            override fun onTransitionCancel(transition: Transition) {
                transition.removeListener(this)
            }

            override fun onTransitionEnd(transition: Transition) {
                minimizeIcon.visibility = if (minimize) View.VISIBLE else View.GONE
                transition.removeListener(this)
            }

            override fun onTransitionPause(transition: Transition) {
                transition.removeListener(this)
            }

            override fun onTransitionResume(transition: Transition) {}
            override fun onTransitionStart(transition: Transition) {}
        })
        mTransition?.excludeChildren(R.id.recycler_view_app_list, true)
        TransitionManager.beginDelayedTransition(mainContainer, mTransition)
        constraintSetTunnel.applyTo(mainContainer)
    }

    private fun setUpCustomSearchBox() {
        // Search view
        searchView.setIconifiedByDefault(false)
        searchView.queryHint = "Search"
        searchView.isFocusable = false
        // Filter results on text change
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(s: String): Boolean {
                presenter.onFilter(s)
                return false
            }

            override fun onQueryTextSubmit(s: String): Boolean {
                searchView.clearFocus()
                return true
            }
        })
        // Hide top layout items to make more room for search view and apps
        searchView.setOnQueryTextFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (hasFocus) {
                minimizeTopView(true)
            }
        }

        // Search text
        val searchText =
            searchView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(getColor(this, R.attr.wdSecondaryColor, R.color.colorWhite))
        searchText.setHintTextColor(getColor(this, R.attr.wdSecondaryColor, R.color.colorWhite))
        searchText.setTextSize(Dimension.SP, 14f)
        val typeface = ResourcesCompat.getFont(this, R.font.ibm_plex_sans_regular)
        searchText.typeface = typeface
        searchText.setPadding(0, 0, 0, 0)

        // Close button
        val closeButton =
            searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeButton.setPadding(0, 0, 0, 0)
        closeButton.setOnClickListener {
            searchView.clearFocus()
            searchView.setQuery("", false)
            presenter.onFilter("")
        }
        // Search icon
        val searchIcon =
            searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon.setPadding(0, 0, 0, 0)
        searchIcon.scaleType = ImageView.ScaleType.FIT_START
        searchIcon.imageTintList =
            ColorStateList.valueOf(getColor(this, R.attr.searchTextColor, R.color.colorWhite))
    }

    companion object {
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, SplitTunnelingActivity::class.java)
        }
    }
}