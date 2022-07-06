/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.overlay

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.transition.AutoTransition
import androidx.transition.Slide
import androidx.transition.TransitionManager
import butterknife.BindView
import butterknife.OnClick
import butterknife.OnFocusChange
import com.windscribe.tv.R
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.disconnectalert.DisconnectActivity.Companion.getIntent
import com.windscribe.tv.serverlist.adapters.FavouriteAdapter
import com.windscribe.tv.serverlist.adapters.ServerAdapter
import com.windscribe.tv.serverlist.adapters.StaticIpAdapter
import com.windscribe.tv.serverlist.customviews.OverlayFocusAware
import com.windscribe.tv.serverlist.detail.DetailActivity
import com.windscribe.tv.serverlist.fragments.AllOverlayFragment
import com.windscribe.tv.serverlist.fragments.FavouriteFragment
import com.windscribe.tv.serverlist.fragments.StaticIpFragment
import com.windscribe.tv.serverlist.fragments.WindOverlayFragment
import com.windscribe.tv.windscribe.WindscribeActivity
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.state.PreferenceChangeObserver
import javax.inject.Inject
import org.slf4j.LoggerFactory

class OverlayActivity : BaseActivity(), OverlayView, OverlayListener {
    @JvmField
    @BindView(R.id.BrowseRow)
    var fragmentContainer: ConstraintLayout? = null

    // header
    @JvmField
    @BindView(R.id.header_item_all)
    var headerAll: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.header_item_all_bar)
    var headerAllItemBar: ImageView? = null

    @JvmField
    @BindView(R.id.header_item_all_icon)
    var headerAllItemIcon: ImageView? = null

    @JvmField
    @BindView(R.id.header_item_all_text)
    var headerAllItemText: TextView? = null

    @JvmField
    @BindView(R.id.header_item_fav)
    var headerFav: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.header_item_fav_bar)
    var headerFavItemBar: ImageView? = null

    @JvmField
    @BindView(R.id.header_item_fav_icon)
    var headerFavItemIcon: ImageView? = null

    @JvmField
    @BindView(R.id.header_item_fav_text)
    var headerFavItemText: TextView? = null

    @JvmField
    @BindView(R.id.headerRow)
    var headerRow: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.header_item_static)
    var headerStatic: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.header_item_static_bar)
    var headerStaticItemBar: ImageView? = null

    @JvmField
    @BindView(R.id.header_item_static_icon)
    var headerStaticItemIcon: ImageView? = null

    @JvmField
    @BindView(R.id.header_item_static_text)
    var headerStaticItemText: TextView? = null

    @JvmField
    @BindView(R.id.header_item_wind)
    var headerWind: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.header_item_wind_bar)
    var headerWindItemBar: ImageView? = null

    @JvmField
    @BindView(R.id.header_item_wind_icon)
    var headerWindItemIcon: ImageView? = null

    @JvmField
    @BindView(R.id.header_item_wind_text)
    var headerWindItemText: TextView? = null

    @Inject
    lateinit var mPreferenceChangeObserver: PreferenceChangeObserver

    @JvmField
    @BindView(R.id.overlayParent)
    var overlayParent: OverlayFocusAware? = null

    @Inject
    lateinit var presenter: OverlayPresenter

    private val requestDetailCode = 901
    private val firstTimeServerListLoad = true
    private var isHeaderOpen = false
    private var maxHeader: ConstraintSet? = null
    private var minHeader: ConstraintSet? = null
    private val logger = LoggerFactory.getLogger("overlay:a")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_overlay)
        setConstraints()
        supportFragmentManager.beginTransaction()
            .replace(R.id.BrowseRow, AllOverlayFragment(), "1")
            .commit()
        registerDataChangeObservers()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestDetailCode == requestCode && resultCode == RESULT_OK) {
            logger.debug("Closing overlay view to connect.")
            setResult(RESULT_OK)
            finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    @OnClick(R.id.header_item_all)
    fun onAllNodeClick() {
        if (currentFragment is AllOverlayFragment) {
            return
        }
        val fragment = AllOverlayFragment()
        overlayParent?.let {
            TransitionManager.beginDelayedTransition(it, Slide())
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.BrowseRow, fragment, "1")
            transaction.commit()
            supportFragmentManager.executePendingTransactions()
        }
    }

    override fun onAllOverlayViewReady() {
        presenter.allLocationViewReady()
    }

    override fun onBackPressed() {
        goToWindActivity()
    }

    override fun onDisabledNodeClick() {
        appContext.startActivity(
            getIntent(
                appContext,
                getString(R.string.node_under_construction_text),
                "Alert"
            )
        )
    }

    override fun onExit() {
        finish()
    }

    @OnClick(R.id.header_item_fav)
    fun onFavNodeClick() {
        if (currentFragment is FavouriteFragment) {
            return
        }
        overlayParent?.let {
            val fragment = FavouriteFragment()
            TransitionManager.beginDelayedTransition(it, Slide())
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.BrowseRow, fragment, "2")
            transaction.commit()
            supportFragmentManager.executePendingTransactions()
        }
    }

    override fun onFavouriteOverlayReady() {
        presenter.favouriteViewReady()
    }

    override fun onLocationSelected(regionId: Int) {
        currentFragment?.tag?.let {
            val startIntent = Intent(this, DetailActivity::class.java)
            startIntent.putExtra(DetailActivity.SELECTED_ID, regionId)
            startIntent.putExtra("fragment_tag", it)
            startActivityForResult(startIntent, requestDetailCode)
            overridePendingTransition(R.anim.slide_up, R.anim.slide_down)
        }
    }

    override fun onNodeSelected(cityID: Int) {
        logger.debug("Closing overlay view to connect.")
        val startIntent = Intent(this, WindscribeActivity::class.java)
        startIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startIntent.putExtra("city", cityID)
        startIntent.putExtra("connectingToStaticIP", false)
        startActivity(startIntent)
    }

    @OnClick(R.id.header_item_static)
    fun onStaticClick() {
        if (currentFragment is StaticIpFragment) {
            return
        }
        overlayParent?.let {
            val fragment = StaticIpFragment()
            TransitionManager.beginDelayedTransition(it, Slide())
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.BrowseRow, fragment, "4")
            transaction.commit()
            supportFragmentManager.executePendingTransactions()
        }
    }

    override fun onStaticOverlayReady() {
        presenter.staticIpViewReady()
    }

    override fun onStaticSelected(regionID: Int, userNameEncoded: String, passwordEncoded: String) {
        logger.debug("Closing overlay view to connect to static ip")
        val startIntent = Intent(this, WindscribeActivity::class.java)
        startIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startIntent.putExtra("city", regionID)
        startIntent.putExtra("connectingToStaticIP", true)
        startIntent.putExtra("username", userNameEncoded)
        startIntent.putExtra("password", passwordEncoded)
        startActivity(startIntent)
    }

    @OnClick(R.id.header_item_wind)
    fun onWindNodeClick() {
        if (currentFragment is WindOverlayFragment) {
            return
        }
        overlayParent?.let {
            val fragment = WindOverlayFragment()
            TransitionManager.beginDelayedTransition(it, Slide())
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.BrowseRow, fragment, "3")
            transaction.commit()
            supportFragmentManager.executePendingTransactions()
        }
    }

    override fun onWindOverlayReady() {
        presenter.windLocationViewReady()
    }

    override fun setAllAdapter(serverAdapter: ServerAdapter) {
        if (currentFragment is AllOverlayFragment) {
            (currentFragment as AllOverlayFragment).setAllOverlayAdapter(serverAdapter)
        }
    }

    override fun setFavouriteAdapter(favouriteAdapter: FavouriteAdapter) {
        if (currentFragment is FavouriteFragment) {
            (currentFragment as FavouriteFragment).setAdapter(favouriteAdapter)
        }
    }

    override fun setState(state: LoadState, stateDrawable: Int, stateText: Int, fragmentIndex: Int) {
        currentFragment?.let { fragment ->
            fragment.tag?.let { tag ->
                if (tag == fragmentIndex.toString()) {
                    val stateLayout = fragment.view?.findViewById<TextView>(R.id.state_layout)
                    when (state) {
                        LoadState.Loaded -> stateLayout?.visibility = View.GONE
                        LoadState.Loading, LoadState.NoResult, LoadState.Error -> {
                            stateLayout?.visibility = View.VISIBLE
                            stateLayout?.text = getString(stateText)
                            stateLayout?.setCompoundDrawablesWithIntrinsicBounds(
                                null,
                                ResourcesCompat.getDrawable(resources, stateDrawable, theme), null,
                                null
                            )
                        }
                    }
                }
            }
        }
    }

    override fun setStaticAdapter(staticAdapter: StaticIpAdapter) {
        if (currentFragment is StaticIpFragment) {
            (currentFragment as StaticIpFragment).setAdapter(staticAdapter)
        }
    }

    override fun setWindAdapter(serverAdapter: ServerAdapter) {
        if (currentFragment is WindOverlayFragment) {
            (currentFragment as WindOverlayFragment).setWindOverlayAdapter(serverAdapter)
        }
    }

    override fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun maximizeHeader() {
        isHeaderOpen = true
        overlayParent?.let {
            TransitionManager.beginDelayedTransition(it)
            maxHeader?.applyTo(overlayParent)
        }
    }

    private fun minimizeHeader() {
        isHeaderOpen = false
        val autoTransition = AutoTransition()
        autoTransition.excludeTarget(R.id.header_item_all_text, true)
        autoTransition.excludeTarget(R.id.header_item_fav_text, true)
        autoTransition.excludeTarget(R.id.header_item_wind_text, true)
        autoTransition.excludeTarget(R.id.header_item_static_text, true)
        overlayParent?.let {
            TransitionManager.beginDelayedTransition(it, autoTransition)
            minHeader?.applyTo(overlayParent)
        }
    }

    @OnFocusChange(R.id.header_item_all)
    fun onFocusToAll() {
        headerAll?.let {
            if (it.hasFocus() && !isHeaderOpen) {
                maximizeHeader()
            }
            if (!it.hasFocus() && isHeaderOpen) {
                minimizeHeader()
            }
        }
    }

    @OnFocusChange(R.id.header_item_fav)
    fun onFocusToFav() {
        headerFav?.let {
            if (it.hasFocus() && !isHeaderOpen) {
                maximizeHeader()
            }
            if (!it.hasFocus() && isHeaderOpen) {
                minimizeHeader()
            }
        }
    }

    @OnFocusChange(R.id.header_item_static)
    fun onFocusToStatic() {
        headerStatic?.let {
            if (it.hasFocus() && !isHeaderOpen) {
                maximizeHeader()
            }
            if (!it.hasFocus() && isHeaderOpen) {
                minimizeHeader()
            }
        }
    }

    @OnFocusChange(R.id.header_item_wind)
    fun onFocusToWind() {
        headerWind?.let {
            if (it.hasFocus() && !isHeaderOpen) {
                maximizeHeader()
            }
            if (!it.hasFocus() && isHeaderOpen) {
                minimizeHeader()
            }
        }
    }

    @OnClick(R.id.header_item_all)
    fun onHeaderAllClick() {
        if (currentFragment is AllOverlayFragment) {
            return
        }
        headerAllItemBar?.visibility = View.VISIBLE
        headerFavItemBar?.visibility = View.INVISIBLE
        headerWindItemBar?.visibility = View.INVISIBLE
        headerStaticItemBar?.visibility = View.INVISIBLE
        headerAllItemIcon?.alpha = 1.0f
        headerFavItemIcon?.alpha = 0.40f
        headerWindItemIcon?.alpha = 0.40f
        headerStaticItemIcon?.alpha = 0.40f
        headerAllItemText?.alpha = 1.0f
        headerFavItemText?.alpha = 0.40f
        headerWindItemText?.alpha = 0.40f
        headerStaticItemText?.alpha = 0.40f
        overlayParent?.setCurrentFragment(0)
    }

    @OnClick(R.id.header_item_fav)
    fun onHeaderFavClick() {
        if (currentFragment is FavouriteFragment) {
            return
        }
        headerFavItemBar?.visibility = View.VISIBLE
        headerWindItemBar?.visibility = View.INVISIBLE
        headerStaticItemBar?.visibility = View.INVISIBLE
        headerAllItemBar?.visibility = View.INVISIBLE
        headerAllItemIcon?.alpha = 0.40f
        headerFavItemIcon?.alpha = 1.0f
        headerWindItemIcon?.alpha = 0.40f
        headerStaticItemIcon?.alpha = 0.40f
        headerAllItemText?.alpha = 0.40f
        headerFavItemText?.alpha = 1.0f
        headerWindItemText?.alpha = 0.40f
        headerStaticItemText?.alpha = 0.40f
        overlayParent?.setCurrentFragment(1)
    }

    @OnClick(R.id.header_item_static)
    fun onHeaderStaticClick() {
        if (currentFragment is StaticIpFragment) {
            return
        }
        headerAllItemBar?.visibility = View.INVISIBLE
        headerFavItemBar?.visibility = View.INVISIBLE
        headerWindItemBar?.visibility = View.INVISIBLE
        headerStaticItemBar?.visibility = View.VISIBLE
        headerAllItemIcon?.alpha = 0.40f
        headerFavItemIcon?.alpha = 0.40f
        headerWindItemIcon?.alpha = 0.40f
        headerStaticItemIcon?.alpha = 1.0f
        headerAllItemText?.alpha = 0.40f
        headerFavItemText?.alpha = 0.40f
        headerWindItemText?.alpha = 0.40f
        headerStaticItemText?.alpha = 1.0f
        overlayParent?.setCurrentFragment(3)
    }

    @OnClick(R.id.header_item_wind)
    fun onHeaderWindClick() {
        if (currentFragment is WindOverlayFragment) {
            return
        }
        headerFavItemBar?.visibility = View.INVISIBLE
        headerWindItemBar?.visibility = View.VISIBLE
        headerStaticItemBar?.visibility = View.INVISIBLE
        headerAllItemBar?.visibility = View.INVISIBLE
        headerAllItemIcon?.alpha = 0.40f
        headerFavItemIcon?.alpha = 0.40f
        headerWindItemIcon?.alpha = 1.0f
        headerStaticItemIcon?.alpha = 0.40f
        headerAllItemText?.alpha = 0.40f
        headerFavItemText?.alpha = 0.40f
        headerWindItemText?.alpha = 1.0f
        headerStaticItemText?.alpha = 0.40f
        overlayParent?.setCurrentFragment(2)
    }

    private val currentFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.BrowseRow)

    private fun goToWindActivity() {
        overridePendingTransition(R.anim.slide_up, R.anim.slide_down)
        super.onBackPressed()
    }

    private fun registerDataChangeObservers() {
        activityScope { presenter.observeStaticRegions() }
        activityScope { presenter.observeAllLocations() }
    }

    private fun setConstraints() {
        maxHeader = ConstraintSet()
        maxHeader?.clone(this, R.layout.activity_overlay)
        maxHeader?.connect(R.id.headerRow, ConstraintSet.END, R.id.headerMax, ConstraintSet.END)
        minHeader = ConstraintSet()
        minHeader?.clone(this, R.layout.activity_overlay)
        minHeader?.connect(R.id.headerRow, ConstraintSet.END, R.id.headerMin, ConstraintSet.END)
    }
}