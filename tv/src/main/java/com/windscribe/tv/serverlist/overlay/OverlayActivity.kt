/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.overlay

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.transition.AutoTransition
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.windscribe.tv.R
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.databinding.ActivityOverlayBinding
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.disconnectalert.DisconnectActivity.Companion.getIntent
import com.windscribe.tv.serverlist.adapters.FavouriteAdapter
import com.windscribe.tv.serverlist.adapters.ServerAdapter
import com.windscribe.tv.serverlist.adapters.StaticIpAdapter
import com.windscribe.tv.serverlist.detail.DetailActivity
import com.windscribe.tv.serverlist.fragments.AllOverlayFragment
import com.windscribe.tv.serverlist.fragments.FavouriteFragment
import com.windscribe.tv.serverlist.fragments.StaticIpFragment
import com.windscribe.tv.serverlist.fragments.WindOverlayFragment
import com.windscribe.tv.windscribe.WindscribeActivity
import com.windscribe.vpn.Windscribe.Companion.appContext
import org.slf4j.LoggerFactory
import javax.inject.Inject

class OverlayActivity : BaseActivity(), OverlayView, OverlayListener {
    private lateinit var binding: ActivityOverlayBinding

    @Inject
    lateinit var presenter: OverlayPresenter
    private val requestDetailCode = 901
    private var isHeaderOpen = false
    private var maxHeader: ConstraintSet? = null
    private var minHeader: ConstraintSet? = null
    private val logger = LoggerFactory.getLogger("basic")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_overlay)
        onActivityLaunch()
        setConstraints()
        supportFragmentManager.beginTransaction()
            .replace(R.id.BrowseRow, AllOverlayFragment(), "1")
            .commit()
        registerDataChangeObservers()
        addClickListeners()
        addFocusListeners()
    }

    private fun addClickListeners() {
        binding.headerItemAll.setOnClickListener {
            onHeaderClick(
                0,
                AllOverlayFragment::class.java,
                binding.headerItemAllBar,
                binding.headerItemAllIcon,
                binding.headerItemAllText
            )
            onAllNodeClick()
        }
        binding.headerItemFav.setOnClickListener {
            onHeaderClick(
                1,
                FavouriteFragment::class.java,
                binding.headerItemFavBar,
                binding.headerItemFavIcon,
                binding.headerItemFavText
            )
            onFavNodeClick()
        }
        binding.headerItemWind.setOnClickListener {
            onHeaderClick(
                2,
                WindOverlayFragment::class.java,
                binding.headerItemWindBar,
                binding.headerItemWindIcon,
                binding.headerItemWindText
            )
            onWindNodeClick()
        }
        binding.headerItemStatic.setOnClickListener {
            onHeaderClick(
                3,
                StaticIpFragment::class.java,
                binding.headerItemStaticBar,
                binding.headerItemStaticIcon,
                binding.headerItemStaticText
            )
            onStaticClick()
        }
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

    private fun onAllNodeClick() {
        if (currentFragment is AllOverlayFragment) {
            return
        }
        val fragment = AllOverlayFragment()
        TransitionManager.beginDelayedTransition(binding.overlayParent, Slide())
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.BrowseRow, fragment, "1")
        transaction.commit()
        supportFragmentManager.executePendingTransactions()
    }

    override suspend fun onAllOverlayViewReady() {
        presenter.allLocationViewReady()
    }

    override fun onBackPressed() {
        goToWindActivity()
    }

    override fun onDisabledNodeClick() {
        appContext.startActivity(
            getIntent(
                appContext,
                getString(com.windscribe.vpn.R.string.node_under_construction_text),
                "Alert"
            )
        )
    }

    override fun onExit() {
        finish()
    }

    private fun onFavNodeClick() {
        if (currentFragment is FavouriteFragment) {
            return
        }
        val fragment = FavouriteFragment()
        TransitionManager.beginDelayedTransition(binding.overlayParent, Slide())
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.BrowseRow, fragment, "2")
        transaction.commit()
        supportFragmentManager.executePendingTransactions()
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

    private fun onStaticClick() {
        if (currentFragment is StaticIpFragment) {
            return
        }
        val fragment = StaticIpFragment()
        TransitionManager.beginDelayedTransition(binding.overlayParent, Slide())
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.BrowseRow, fragment, "4")
        transaction.commit()
        supportFragmentManager.executePendingTransactions()
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

    private fun onWindNodeClick() {
        if (currentFragment is WindOverlayFragment) {
            return
        }
        val fragment = WindOverlayFragment()
        TransitionManager.beginDelayedTransition(binding.overlayParent, Slide())
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.BrowseRow, fragment, "3")
        transaction.commit()
        supportFragmentManager.executePendingTransactions()
    }

    override suspend fun onWindOverlayReady() {
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

    override fun setState(
        state: LoadState,
        stateDrawable: Int,
        stateText: Int,
        fragmentIndex: Int
    ) {
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
        TransitionManager.beginDelayedTransition(binding.overlayParent)
        maxHeader?.applyTo(binding.overlayParent)
    }

    private fun minimizeHeader() {
        isHeaderOpen = false
        val autoTransition = AutoTransition()
        autoTransition.excludeTarget(R.id.header_item_all_text, true)
        autoTransition.excludeTarget(R.id.header_item_fav_text, true)
        autoTransition.excludeTarget(R.id.header_item_wind_text, true)
        autoTransition.excludeTarget(R.id.header_item_static_text, true)
        TransitionManager.beginDelayedTransition(binding.overlayParent, autoTransition)
        minHeader?.applyTo(binding.overlayParent)
    }

    private fun addFocusListeners() {
        val focusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus && !isHeaderOpen) {
                maximizeHeader()
            } else if (!hasFocus && isHeaderOpen) {
                minimizeHeader()
            }
        }
        with(binding) {
            headerItemAll.onFocusChangeListener = focusChangeListener
            headerItemFav.onFocusChangeListener = focusChangeListener
            headerItemWind.onFocusChangeListener = focusChangeListener
            headerItemStatic.onFocusChangeListener = focusChangeListener
        }
    }

    private fun onHeaderClick(
        fragmentIndex: Int,
        fragmentClass: Class<*>,
        selectedBar: View,
        selectedIcon: View,
        selectedText: View
    ) {
        binding.overlayParent.setCurrentFragment(fragmentIndex)
        if (fragmentClass.isInstance(currentFragment)) {
            return
        }

        // Update visibility for bars
        listOf(
            binding.headerItemAllBar,
            binding.headerItemFavBar,
            binding.headerItemWindBar,
            binding.headerItemStaticBar
        ).forEach { it.visibility = View.INVISIBLE }
        selectedBar.visibility = View.VISIBLE

        // Update alpha for icons
        listOf(
            binding.headerItemAllIcon,
            binding.headerItemFavIcon,
            binding.headerItemWindIcon,
            binding.headerItemStaticIcon
        ).forEach { it.alpha = 0.40f }
        selectedIcon.alpha = 1.0f

        // Update alpha for text
        listOf(
            binding.headerItemAllText,
            binding.headerItemFavText,
            binding.headerItemWindText,
            binding.headerItemStaticText
        ).forEach { it.alpha = 0.40f }
        selectedText.alpha = 1.0f
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
        activityScope { presenter.observeLatencyChange() }
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