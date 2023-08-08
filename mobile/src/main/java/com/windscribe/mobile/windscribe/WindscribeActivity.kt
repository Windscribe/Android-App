/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.windscribe

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.app.ActivityOptions
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BlurMaskFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.AutoTransition
import android.transition.Slide
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.*
import butterknife.BindView
import butterknife.OnClick
import butterknife.OnItemSelected
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup
import com.windscribe.mobile.R
import com.windscribe.mobile.adapter.*
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.connectionsettings.ConnectionSettingsActivity
import com.windscribe.mobile.connectionui.*
import com.windscribe.mobile.custom_view.CustomDialog
import com.windscribe.mobile.custom_view.CustomDrawableCrossFadeFactory
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.dialogs.*
import com.windscribe.mobile.fragments.SearchFragment
import com.windscribe.mobile.fragments.ServerListFragment
import com.windscribe.mobile.mainmenu.MainMenuActivity
import com.windscribe.mobile.newsfeedactivity.NewsFeedActivity
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.mobile.utils.PermissionManager
import com.windscribe.mobile.welcome.WelcomeActivity
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.ThemeUtils
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.AnimConstants
import com.windscribe.vpn.constants.AnimConstants.VPN_CONNECTING_ANIMATION_DELAY
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.NetworkKeyConstants.getWebsiteLink
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.constants.RateDialogConstants
import com.windscribe.vpn.constants.RateDialogConstants.PLAY_STORE_URL
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.serverlist.entity.ConfigFile
import com.windscribe.vpn.serverlist.entity.ServerListData
import com.windscribe.vpn.serverlist.interfaces.ListViewClickListener
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.DeviceStateManager.DeviceStateListener
import com.windscribe.vpn.state.PreferenceChangeObserver
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Named

class WindscribeActivity : BaseActivity(), WindscribeView, OnPageChangeListener,
    RateAppDialogCallback, EditConfigFileDialogCallback, FragmentClickListener, DeviceStateListener, NodeStatusDialogCallback,
    AccountStatusDialogCallback {
    enum class NetworkLayoutState {
        CLOSED, OPEN_1, OPEN_2, OPEN_3
    }

    @Inject
    lateinit var customDialog: CustomDialog

    @Inject
    lateinit var argbEvaluator: ArgbEvaluator

    @Inject
    lateinit var deviceStateManager: DeviceStateManager

    @Inject
    lateinit var preferenceChangeObserver: PreferenceChangeObserver

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var presenter: WindscribePresenter

    @Inject
    lateinit var serverListRepository: ServerListRepository

    @Inject
    @Named("serverListFragments")
    lateinit var serverListFragments: List<ServerListFragment>

    @JvmField
    @BindView(R.id.auto_secure_divider)
    var autoSecureDivider: ImageView? = null

    @JvmField
    @BindView(R.id.auto_secure_toggle)
    var autoSecureToggle: ImageView? = null

    @JvmField
    @BindView(R.id.cl_auto_secure)
    var clAutoSecure: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.cl_port)
    var clPort: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.cl_preferred)
    var clPreferred: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.cl_preferred_protocol)
    var clPreferredProtocol: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.cl_protocol)
    var clProtocol: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.collapse_expand_icon)
    var collapseExpandExpandIcon: ImageView? = null

    @JvmField
    @BindView(R.id.connecting_icon)
    var connectionIcon: ProgressBar? = null

    @JvmField
    @BindView(R.id.tv_connection_state)
    var connectionState: TextView? = null

    @JvmField
    @BindView(R.id.tv_current_port)
    var currentPort: TextView? = null

    @JvmField
    @BindView(R.id.tv_current_protocol)
    var currentProtocol: TextView? = null

    @JvmField
    @BindView(R.id.flag_dimensions_guide)
    var flagDimensionsGuideView: ImageView? = null

    @JvmField
    @BindView(R.id.img_hamburger_menu)
    var hamburgerIcon: ImageView? = null

    @JvmField
    @BindView(R.id.bottom_gradient)
    var bottomGradient: ImageView? = null

    @JvmField
    @BindView(R.id.connection_gradient)
    var connectionGradient: ImageView? = null

    @JvmField
    @BindView(R.id.cl_windscribe_main)
    var constraintLayoutMain: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.server_list_tool_bar)
    var constraintLayoutServerList: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.flag)
    var flagView: ImageView? = null

    @JvmField
    @BindView(R.id.userAccountStatusIcon)
    var imgAccountGarryEmotion: ImageView? = null

    @JvmField
    @BindView(R.id.img_config_loc_list)
    var imgConfigLocList: ImageView? = null

    @JvmField
    @BindView(R.id.img_server_list_all)
    var imgServerListAll: ImageView? = null

    @JvmField
    @BindView(R.id.img_server_list_favorites)
    var imgServerListFavorites: ImageView? = null

    @JvmField
    @BindView(R.id.img_server_list_flix)
    var imgServerListFlix: ImageView? = null

    @JvmField
    @BindView(R.id.img_static_ip_list)
    var imgStaticIpList: ImageView? = null

    @JvmField
    @BindView(R.id.location_list_fragment_pager)
    var locationFragmentViewPager: ViewPager? = null

    @JvmField
    @BindView(R.id.safe_unsafe_icon)
    var lockIcon: ImageView? = null

    @JvmField
    @BindView(R.id.progress_bar_recyclerview)
    var progressBarRecyclerView: ProgressBar? = null

    @JvmField
    @BindView(R.id.toolbar_background_slope)
    var slopedView: ImageView? = null

    @JvmField
    @BindView(R.id.network_name)
    var textViewConnectedNetworkName: TextView? = null

    @JvmField
    @BindView(R.id.ip_address)
    var textViewIpAddress: TextView? = null

    @JvmField
    @BindView(R.id.tv_connected_city_name)
    var textViewLocationName: TextView? = null

    @JvmField
    @BindView(R.id.tv_connected_city_nick_name)
    var textViewLocationNick: TextView? = null

    @JvmField
    @BindView(R.id.toolbar_background_square)
    var toolBarSquare: ImageView? = null

    @JvmField
    @BindView(R.id.network_icon)
    var networkIcon: ImageView? = null

    @JvmField
    @BindView(R.id.text_view_notification)
    var notificationCountView: TextView? = null

    @JvmField
    @BindView(R.id.on_off_button)
    var onOffButton: ImageView? = null

    @JvmField
    @BindView(R.id.on_off_progress_bar)
    var onOffProgressBar: ProgressBar? = null

    @JvmField
    @BindView(R.id.on_off_ring)
    var onOffRing: ImageView? = null

    @JvmField
    @BindView(R.id.tv_port)
    var port: TextView? = null
    private var portAdapter: ArrayAdapter<String>? = null

    @JvmField
    @BindView(R.id.port_protocol_divider)
    var portProtocolDivider: ImageView? = null

    @JvmField
    @BindView(R.id.spinner_port)
    var portSpinner: Spinner? = null

    @JvmField
    @BindView(R.id.img_preferred_protocol_status)
    var preferredProtocolStatus: ImageView? = null

    @JvmField
    @BindView(R.id.tv_decoy_label)
    var tvDecoy: TextView? = null

    @JvmField
    @BindView(R.id.tv_decoy_divider)
    var decoyDivider: ImageView? = null

    @JvmField
    @BindView(R.id.img_decoy_traffic_arrow)
    var decoyArrow: ImageView? = null

    @JvmField
    @BindView(R.id.anti_censor_ship_status)
    var antiCensorShipIcon: ImageView? = null

    @JvmField
    @BindView(R.id.img_protocol_change_arrow)
    var changeProtocolArrow: ImageView? = null

    @JvmField
    @BindView(R.id.preferred_protocol_toggle)
    var preferredProtocolToggle: ImageView? = null

    @JvmField
    @BindView(R.id.tv_protocol)
    var protocol: TextView? = null

    @JvmField
    @BindView(R.id.spinner_protocol)
    var protocolSpinner: Spinner? = null

    @JvmField
    @BindView(R.id.server_list_toolbar)
    var serverListToolbar: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.top_gradient)
    var topGradient: ImageView? = null

    @JvmField
    @BindView(R.id.autoSecureLabel)
    var tvAutoSecureLabel: TextView? = null

    @JvmField
    @BindView(R.id.tv_port_label)
    var tvPortLabel: TextView? = null

    @JvmField
    @BindView(R.id.preferredProtocolLabel)
    var tvPreferredProtocolLabel: TextView? = null

    @JvmField
    @BindView(R.id.tv_protocol_label)
    var tvProtocolLabel: TextView? = null

    @Inject
    lateinit var permissionManager: PermissionManager

    private var protocolAdapter: ArrayAdapter<String>? = null

    var transition: AutoTransition? = null

    val constraintSetMain = ConstraintSet()

    private val constraintSetServerList = ConstraintSet()

    private var lastFlag = 0

    private val logger = LoggerFactory.getLogger("windscribe_a")

    override var uiConnectionState: ConnectionUiState? = null
        private set
    private var lastCustomBackgroundPath: String? = null
    private val drawableCrossFadeFactory =
        CustomDrawableCrossFadeFactory.Builder(1500).setCrossFadeEnabled(true).build()
    override var isBannedLayoutShown = false
        private set
    override var networkLayoutState = NetworkLayoutState.CLOSED
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_windscribe, true)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        val layoutTransition = constraintLayoutMain?.layoutTransition
        layoutTransition?.enableTransitionType(LayoutTransition.CHANGING)
        presenter.setMainCustomConstraints()
        setServerListView(false)
        permissionManager.register(this)
        registerDataChangeObserver()
        activityScope { presenter.observeVPNState() }
        activityScope { presenter.observeNextProtocolToConnect() }
        activityScope { presenter.observeConnectedProtocol() }
        activityScope { presenter.observeStaticRegions() }
        activityScope { presenter.observeAllLocations() }
        activityScope { presenter.observerSelectedLocation() }
        activityScope { presenter.observeDecoyTrafficState() }
        activityScope { presenter.showShareLinkDialog() }
        activityScope { presenter.observeLatency() }
        presenter.registerNetworkInfoListener()
        presenter.handlePushNotification(intent.extras)
        presenter.observeUserData(this)
    }

    override fun onStart() {
        super.onStart()
        if (presenter.userHasAccess()) {
            presenter.onStart()
            if (intent != null && intent.action != null && (intent.action == NotificationConstants.DISCONNECT_VPN_INTENT)) {
                logger.info("Disconnect intent received...")
                presenter.onDisconnectIntentReceived()
            }
            deviceStateManager.addListener(this)
        } else {
            presenter.logoutFromCurrentSession()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_PICK_REQUEST && resultCode == RESULT_OK && data != null) {
            presenter.loadConfigFile(data)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setServerListView(true)
    }

    override fun onResume() {
        super.onResume()
        if (!coldLoad.getAndSet(false)) {
            setLanguage()
            presenter.onHotStart()
        }
        presenter.checkForWgIpChange()
        presenter.checkPendingAccountUpgrades()
    }

    override fun onStop() {
        super.onStop()
        logger.info("Activity on stop method,un-registering network and vpn status listener")
        deviceStateManager.removeListener(this)
    }

    override fun onDestroy() {
        locationFragmentViewPager?.currentItem?.let {
            presenter.saveLastSelectedTabIndex(it)
        }
        presenter.onDestroy()
        super.onDestroy()
    }

    fun adjustToolBarHeight(adjustBy: Int) {
        val toolBar = findViewById<ConstraintLayout>(R.id.toolbar_windscribe)
        val toolBarHeight = toolBar.layoutParams.height + adjustBy
        constraintSetMain.constrainHeight(R.id.toolbar_windscribe, toolBarHeight)
        constraintSetMain.setMargin(R.id.on_off_button, ConstraintSet.TOP, toolBarHeight / 2)
        constraintSetMain.applyTo(constraintLayoutMain)
    }

    override fun checkNodeStatus() {
        logger.info("User clicked on check node status button...")
        presenter.onCheckNodeStatusClick()
    }

    override fun exitSearchLayout() {
        val fragment = supportFragmentManager.findFragmentById(R.id.cl_windscribe_main)
        if (fragment is SearchFragment) {
            fragment.setSearchView(false)
        }
    }

    private fun getColorFromTheme(id: Int, defaultValue: Int): Int {
        return ThemeUtils.getColor(this, id, defaultValue)
    }

    override val flagViewHeight: Int
        get() = flagDimensionsGuideView?.measuredHeight ?: 0
    override val flagViewWidth: Int
        get() = flagDimensionsGuideView?.measuredWidth ?: 0

    override fun gotoLoginRegistrationActivity() {
        val intent = WelcomeActivity.getStartIntent(this)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        startActivity(intent)
        finish()
    }

    override fun handleRateView() {
        RateAppDialog.show(this)
    }

    override fun hideProgressView() {
        runOnUiThread {
            if (customDialog.isShowing) {
                customDialog.hide()
            }
        }
    }

    override fun hideRecyclerViewProgressBar() {
        runOnUiThread { progressBarRecyclerView?.visibility = View.GONE }
    }

    override fun neverAskAgainClicked() {
        presenter.saveRateDialogPreference(RateDialogConstants.STATUS_NEVER_ASK)
    }

    override fun onAddConfigClick() {
        presenter.onAddConfigLocation()
    }

    @OnClick(R.id.auto_secure_info)
    fun onAutoSecureInfoClick() {
        presenter.onAutoSecureInfoClick()
    }

    @OnClick(R.id.auto_secure_toggle)
    fun onAutoSecureToggleClick() {
        logger.info("User clicked on auto secure toggle")
        presenter.onAutoSecureToggleClick()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.findFragmentById(R.id.cl_windscribe_main) == null) {
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }

    @OnClick(R.id.collapse_expand_icon)
    fun onCollapseExpandClick() {
        logger.info("User clicked on collapse/expand icon")
        presenter.onCollapseExpandIconClick()
    }

    override fun onConfigFileUpdated(configFile: ConfigFile) {
        presenter.updateConfigFile(configFile)
    }

    @OnClick(R.id.on_off_button)
    fun onConnectButtonClick() {
        logger.info("User clicked on connect button...")
        presenter.onConnectClicked()
        onOffButton?.isEnabled = false
        // Disable connect button to avoid mismatched state between animations.
        Looper.myLooper()?.let { Handler(it).postDelayed({ onOffButton?.isEnabled = true }, 1000) }
    }

    @OnClick(R.id.tv_current_port, R.id.img_port_drop_down_btn)
    fun onCurrentPortClick() {
        portSpinner?.performClick()
    }

    @OnClick(R.id.tv_current_protocol, R.id.img_protocol_drop_down_btn)
    fun onCurrentProtocolClick() {
        protocolSpinner?.performClick()
    }

    @OnClick(R.id.ip_address)
    fun onIpClick() {
        if (textViewIpAddress?.alpha != 0.0F) {
            presenter.onIpClicked()
        }
    }

    @OnClick(R.id.img_hamburger_menu)
    fun onMenuClicked() {
        logger.info("User clicked menu...")
        presenter.onMenuButtonClicked()
    }

    @OnClick(R.id.network_icon)
    fun onNetworkIconClick() {

    }

    @OnClick(R.id.network_name)
    fun onNetworkNameClick() {
        presenter.onNetworkNameClick()
    }

    override fun onNetworkStateChanged() {
        presenter.onNetworkStateChanged()
    }

    @OnClick(R.id.text_view_notification, R.id.img_windscribe_logo)
    fun onNotificationClick() {
        logger.info("User clicked news feed icon...")
        presenter.onNewsFeedItemClick()
    }

    override fun onPageScrollStateChanged(i: Int) {}
    override fun onPageScrolled(i: Int, v: Float, i1: Int) {}
    override fun onPageSelected(i: Int) {
        when (i) {
            0 -> {
                performSwipeHapticFeedback(imgServerListAll)
                onShowAllServerClick()
            }
            1 -> {
                performSwipeHapticFeedback(imgServerListFavorites)
                onShowFavoritesClicked()
            }
            2 -> {
                performSwipeHapticFeedback(imgServerListFlix)
                onShowFlixListClick()
            }
            3 -> {
                performSwipeHapticFeedback(imgStaticIpList)
                onShowStaticIpList()
            }
            4 -> {
                performSwipeHapticFeedback(imgConfigLocList)
                onShowConfigLocList()
            }
        }
    }

    @OnItemSelected(R.id.spinner_port)
    fun onPortSelected(position: Int) {
        portAdapter?.let {
            val selected = it.getItem(position)
            currentPort?.text = selected
            selected?.let { presenter.onPortSelected(selected) }
        }
    }

    @OnClick(R.id.preferred_protocol_info)
    fun onPreferredProtocolInfoClick() {
        presenter.onPreferredProtocolInfoClick()
    }

    @OnClick(R.id.preferred_protocol_toggle)
    fun onPreferredProtocolToggleClick() {
        logger.info("User clicked on preferred protocol toggle")
        presenter.onPreferredProtocolToggleClick()
    }

    @OnItemSelected(R.id.spinner_protocol)
    fun onProtocolSelected(position: Int) {
        protocolAdapter?.let {
            val selected = it.getItem(position)
            currentProtocol?.text = selected
            selected?.let { presenter.onProtocolSelected(selected) }
        }
    }

    override fun onRefreshPingsForAllServers() {
        cancelRefreshing(0)
        presenter.onRefreshPingsForAllServers()
    }

    override fun onRefreshPingsForConfigServers() {
        cancelRefreshing(4)
        presenter.onRefreshPingsForConfigServers()
    }

    override fun onRefreshPingsForFavouritesServers() {
        cancelRefreshing(1)
        presenter.onRefreshPingsForFavouritesServers()
    }

    override fun onRefreshPingsForStaticServers() {
        cancelRefreshing(3)
        presenter.onRefreshPingsForStaticServers()
    }

    override fun onRefreshPingsForStreamingServers() {
        cancelRefreshing(2)
        presenter.onRefreshPingsForStreamingServers()
    }

    override fun onReloadClick() {
        logger.debug("User clicked on reload list...")
        presenter.onReloadClick()
    }

    override fun onRenewPlanClick() {
        logger.debug("User clicked to renew and upgrade plan...")
        presenter.onRenewPlanClicked()
    }

    @OnClick(R.id.img_search_list)
    fun onSearchBtnClick() {
        logger.debug("User clicked on search button...")
        presenter.onSearchButtonClicked()
    }

    @OnClick(R.id.img_server_list_all)
    fun onShowAllServerClick() {
        logger.info("User clicked show all servers...")
        if (locationFragmentViewPager?.currentItem != 0) {
            logger.debug("Setting pager item to 0: Server List Fragment")
            locationFragmentViewPager?.currentItem = 0
        }
        presenter.onShowAllServerListClicked()
    }

    @OnClick(R.id.img_config_loc_list)
    fun onShowConfigLocList() {
        logger.info("User clicked show config loc list...")
        if (locationFragmentViewPager?.currentItem != 4) {
            // Change fragment to config list fragment
            logger.debug("Setting pager item to 4: Config Loc Fragment")
            locationFragmentViewPager?.currentItem = 4
        }
        presenter.onShowConfigLocListClicked()
    }

    @OnClick(R.id.img_server_list_favorites)
    fun onShowFavoritesClicked() {
        logger.debug("User clicked show favorites...")
        if (locationFragmentViewPager?.currentItem != 1) {
            // Change fragment to all server list fragment
            logger.debug("Setting pager item to 1: Favourites list fragment")
            locationFragmentViewPager?.currentItem = 1
        }
        presenter.onShowFavoritesClicked()
    }

    @OnClick(R.id.img_server_list_flix)
    fun onShowFlixListClick() {
        logger.debug("User clicked show flix locations...")
        if (locationFragmentViewPager?.currentItem != 2) {
            // Change fragment to all server list fragment
            logger.debug("Setting pager item to 2: Flix List Fragment")
            locationFragmentViewPager?.currentItem = 2
        }
        presenter.onShowFlixListClicked()
    }

    @OnClick(R.id.img_static_ip_list)
    fun onShowStaticIpList() {
        logger.debug("User clicked show static ips...")
        if (locationFragmentViewPager?.currentItem != 3) {
            logger.debug("Setting pager item to 3: Static IP Fragment")
            locationFragmentViewPager?.currentItem = 3
        }
        presenter.onShowStaticIpListClicked()
    }

    override fun onStaticIpClick() {
        logger.debug("User clicked on add static ip button...")
        presenter.onAddStaticIPClicked()
    }

    override fun onSubmitUsernameAndPassword(configFile: ConfigFile) {
        presenter.updateConfigFileConnect(configFile)
    }

    override fun onUpgradeClicked() {
        logger.debug("User clicked on upgrade button...")
        presenter.onUpgradeClicked()
    }

    override fun openEditConfigFileDialog(configFile: ConfigFile) {
        EditConfigFileDialog.show(this, configFile)
    }

    override fun openFileChooser() {
        val pickIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        pickIntent.type = "*/*"
        if (pickIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(pickIntent, FILE_PICK_REQUEST)
        } else {
            Toast.makeText(this, "Unable to access shared storage.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun openHelpUrl() {
        logger.debug("Showing help me page in browser")
        openURLInBrowser(getWebsiteLink(NetworkKeyConstants.URL_HELP_ME))
    }

    override fun openMenuActivity() {
        val intent = MainMenuActivity.getStartIntent(this)
        val options = ActivityOptions.makeSceneTransitionAnimation(this)
        startActivity(intent, options.toBundle())
    }

    override fun openConnectionActivity() {
        val intent = ConnectionSettingsActivity.getStartIntent(this)
        val options = ActivityOptions.makeSceneTransitionAnimation(this)
        startActivity(intent, options.toBundle())
    }

    override fun openNewsFeedActivity(showPopUp: Boolean, popUp: Int) {
        val intent = NewsFeedActivity.getStartIntent(this@WindscribeActivity, showPopUp, popUp)
        startActivity(intent)
    }

    override fun openNodeStatusPage(url: String) {
        logger.debug("Opening node status url in browser.")
        openURLInBrowser(url)
    }

    override fun openProvideUsernameAndPasswordDialog(configFile: ConfigFile) {
        UsernameAndPasswordRequestDialog.show(this, configFile)
    }

    override fun openStaticIPUrl(url: String) {
        openURLInBrowser(url)
    }

    override fun openUpgradeActivity() {
        startActivity(UpgradeActivity.getStartIntent(this))
    }

    override fun performButtonClickHapticFeedback() {
        if (presenter.isHapticFeedbackEnabled) {
            hamburgerIcon?.isHapticFeedbackEnabled = true
            hamburgerIcon?.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    override fun performConfirmConnectionHapticFeedback() {
        runOnUiThread {
            if (presenter.isHapticFeedbackEnabled) {
                onOffButton?.isHapticFeedbackEnabled = true
                onOffButton?.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
        }
    }

    private fun performSwipeHapticFeedback(view: View?) {
        if (presenter.isHapticFeedbackEnabled) {
            view?.isHapticFeedbackEnabled = true
            view?.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    override fun rateLaterClicked() {
        presenter.saveRateDialogPreference(RateDialogConstants.STATUS_ASK_LATER)
    }

    override fun rateNowClicked() {
        presenter.saveRateDialogPreference(RateDialogConstants.STATUS_ALREADY_ASKED)
        val urlIntent = Intent(Intent.ACTION_VIEW)
        urlIntent.data = Uri.parse(PLAY_STORE_URL)
        try {
            packageManager.getPackageInfo(PLAY_STORE_URL, 0)
            urlIntent.setPackage(RateDialogConstants.PLAY_STORE_PACKAGE)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        startActivity(urlIntent)
    }

    override fun scrollTo(scrollTo: Int) {
        if (serverListFragments[0].recyclerView != null) {
            serverListFragments[0].scrollTo(scrollTo)
        }
        val fragment = supportFragmentManager.findFragmentById(R.id.cl_windscribe_main)
        if (fragment is SearchFragment) {
            fragment.scrollTo(scrollTo)
        }
    }

    override fun setAdapter(adapter: RegionsAdapter) {
        if (serverListFragments[0].recyclerView != null) {
            serverListFragments[0].clearErrors()
            serverListFragments[0].recyclerView?.adapter = adapter
        }
    }

    override fun setConfigLocListAdapter(configLocListAdapter: ConfigAdapter?) {
        if (serverListFragments[4].recyclerView != null) {
            serverListFragments[4].clearErrors()
            serverListFragments[4].recyclerView?.adapter = configLocListAdapter
            serverListFragments[4].addSwipeListener()
        }
    }

    override fun setConnectionStateText(connectionStateText: String) {
        runOnUiThread { connectionState?.text = connectionStateText }
    }

    private fun setConnectionUIState(state: ConnectionUiState?) {
        uiConnectionState = state
    }

    override fun setCountryFlag(flagIconResource: Int) {
        setupLayoutForAppFlagBackground()
        clearFlagAnimation()
        lastFlag = flagIconResource
        flagView?.setImageResource(flagIconResource)
        flagView?.isFocusable = true
    }

    override fun setFavouriteAdapter(favouriteAdapter: FavouriteAdapter?) {
        if (serverListFragments[1].recyclerView != null) {
            serverListFragments[1].clearErrors()
            serverListFragments[1].recyclerView?.adapter = favouriteAdapter
        }
    }

    override fun setIpAddress(ipAddress: String) {
        runOnUiThread { textViewIpAddress?.text = ipAddress }
    }

    override fun setIpBlur(blur: Boolean) {
        textViewIpAddress?.let {
            it.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            if (blur) {
                val radius = it.textSize / 3
                val filter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
                it.paint.maskFilter = filter
            } else {
                it.paint.maskFilter = null
            }
        }
    }

    override fun setNetworkNameBlur(blur: Boolean) {
        textViewConnectedNetworkName?.let {
            it.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            if (blur) {
                val radius = it.textSize / 3
                val filter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
                it.paint.maskFilter = filter
            } else {
                it.paint.maskFilter = null
            }
        }
    }

    override fun setLastConnectionState(state: ConnectionUiState) {
        setConnectionUIState(state)
        setConnectionState(state)
    }

    override fun setMainConstraints(customBackground: Boolean) {
        constraintSetMain.clone(constraintLayoutMain)
        if (customBackground) {
            constraintSetMain.connect(
                R.id.connection_gradient,
                ConstraintSet.BOTTOM,
                R.id.toolbar_windscribe,
                ConstraintSet.BOTTOM
            )
        } else {
            constraintSetMain.connect(
                R.id.connection_gradient,
                ConstraintSet.BOTTOM,
                R.id.cl_preferred_protocol,
                ConstraintSet.TOP
            )
        }
        constraintSetMain.setVisibility(
            R.id.top_gradient,
            if (customBackground) ConstraintSet.INVISIBLE else ConstraintSet.VISIBLE
        )
        topGradient?.visibility = if (customBackground) View.INVISIBLE else View.VISIBLE
        constraintSetMain.setVisibility(
            R.id.top_gradient_custom,
            if (customBackground) ConstraintSet.VISIBLE else ConstraintSet.INVISIBLE
        )
        findViewById<View>(R.id.top_gradient_custom).visibility =
            if (customBackground) View.VISIBLE else View.INVISIBLE
        constraintSetMain.setVerticalBias(R.id.cl_flag, if (customBackground) 0.0f else 1.0f)
        constraintSetMain.applyTo(constraintLayoutMain)
        constraintSetServerList.clone(constraintLayoutServerList)
    }

    override fun setNetworkLayout(
        info: NetworkInfo?, state: NetworkLayoutState?, resetAdapter: Boolean
    ) {
        if (clPreferredProtocol?.layoutTransition?.isRunning == true) {
            return
        }
        if (resetAdapter) {
            protocolAdapter = null
            portAdapter = null
        }
        when (state) {
            NetworkLayoutState.CLOSED -> setNetworkLayoutCollapsed(info)
            NetworkLayoutState.OPEN_1 -> setNetworkLayoutExpandedLevel1(info)
            NetworkLayoutState.OPEN_2 -> setNetworkLayoutExpandedLevel2(info)
            NetworkLayoutState.OPEN_3 -> setNetworkLayoutExpandedLevel3(info)
            else -> {}
        }
    }

    private fun setNetworkLayoutCollapsed(networkInfo: NetworkInfo?) {
        if (networkInfo != null) {
            networkIcon?.setImageDrawable(
                if (networkInfo.isAutoSecureOn) getDrawableFromTheme(R.drawable.ic_wifi_secure) else getDrawableFromTheme(
                    R.drawable.ic_wifi_unsecure
                )
            )
            textViewConnectedNetworkName?.text = networkInfo.networkName
        } else {
            networkIcon?.setImageDrawable(getDrawableFromTheme(R.drawable.ic_wifi_secure))
            textViewConnectedNetworkName?.text =
                if (WindUtilities.isOnline()) "Unknown Network" else getString(R.string.no_internet)
        }
        val checkForReconnect =
            (networkLayoutState == NetworkLayoutState.OPEN_3 || networkLayoutState == NetworkLayoutState.OPEN_2)
        networkLayoutState = NetworkLayoutState.CLOSED
        animateBottomGradient(true)
        collapseExpandExpandIcon?.animate()?.rotation(0f)?.alpha(0.5f)?.setDuration(300)
            ?.withEndAction { presenter.onNetworkLayoutCollapsed(checkForReconnect) }?.start()
        networkIcon?.alpha = 0.5f
        lockIcon?.alpha = 1.0f
        textViewIpAddress?.alpha = 0.5f
        textViewConnectedNetworkName?.alpha = 0.5f
        autoSecureDivider?.visibility = View.GONE
        clAutoSecure?.visibility = View.GONE
        clPreferred?.visibility = View.GONE
        clProtocol?.visibility = View.GONE
        clPort?.visibility = View.GONE
        portProtocolDivider?.visibility = View.GONE
        portAdapter = null
        protocolAdapter = null
    }

    private fun setNetworkLayoutExpandedLevel1(networkInfo: NetworkInfo?) {
        networkLayoutState = NetworkLayoutState.OPEN_1
        animateBottomGradient(false)
        collapseExpandExpandIcon?.animate()?.rotation(-180f)?.alpha(1.0f)?.setDuration(300)?.start()
        autoSecureToggle?.setImageDrawable(
            if (networkInfo?.isAutoSecureOn == true) getDrawableFromTheme(R.drawable.ic_toggle_button_on) else getDrawableFromTheme(
                R.drawable.ic_toggle_button_off_dark
            )
        )
        preferredProtocolToggle?.setImageDrawable(
            if (networkInfo?.isPreferredOn == true) getDrawableFromTheme(R.drawable.ic_toggle_button_on) else getDrawableFromTheme(
                R.drawable.ic_toggle_button_off_dark
            )
        )
        networkIcon?.setImageDrawable(
            if (networkInfo?.isAutoSecureOn == true) getDrawableFromTheme(R.drawable.ic_wifi_secure) else getDrawableFromTheme(
                R.drawable.ic_wifi_unsecure
            )
        )
        textViewConnectedNetworkName?.text = networkInfo?.networkName
        networkIcon?.alpha = 1.0f
        lockIcon?.alpha = 0.0f
        textViewIpAddress?.alpha = 0.0f
        textViewConnectedNetworkName?.alpha = 1.0f
        clAutoSecure?.visibility = View.VISIBLE
        autoSecureDivider?.visibility =
            if (networkInfo?.isAutoSecureOn == true) View.VISIBLE else View.GONE
        clPreferred?.visibility =
            if (networkInfo?.isAutoSecureOn == true) View.VISIBLE else View.GONE
        clProtocol?.visibility =
            if (networkInfo?.isAutoSecureOn == true && networkInfo.isPreferredOn) View.VISIBLE else View.GONE
        clPort?.visibility =
            if (networkInfo?.isAutoSecureOn == true && networkInfo.isPreferredOn) View.VISIBLE else View.GONE
        portProtocolDivider?.visibility =
            if (networkInfo?.isAutoSecureOn == true && networkInfo.isPreferredOn) View.VISIBLE else View.GONE
        portAdapter = null
        protocolAdapter = null
    }

    private fun setNetworkLayoutExpandedLevel2(networkInfo: NetworkInfo?) {
        networkLayoutState = NetworkLayoutState.OPEN_2
        animateBottomGradient(false)
        collapseExpandExpandIcon?.animate()?.rotation(-180f)?.alpha(1.0f)?.setDuration(300)?.start()
        autoSecureToggle?.setImageDrawable(
            if (networkInfo?.isAutoSecureOn == true) getDrawableFromTheme(R.drawable.ic_toggle_button_on) else getDrawableFromTheme(
                R.drawable.ic_toggle_button_off_dark
            )
        )
        preferredProtocolToggle?.setImageDrawable(
            if (networkInfo?.isPreferredOn == true) getDrawableFromTheme(R.drawable.ic_toggle_button_on) else getDrawableFromTheme(
                R.drawable.ic_toggle_button_off_dark
            )
        )
        networkIcon?.setImageDrawable(
            if (networkInfo?.isAutoSecureOn == true) getDrawableFromTheme(R.drawable.ic_wifi_secure) else getDrawableFromTheme(
                R.drawable.ic_wifi_unsecure
            )
        )
        textViewConnectedNetworkName?.text = networkInfo?.networkName
        networkIcon?.alpha = 1.0f
        lockIcon?.alpha = 0.0f
        textViewIpAddress?.alpha = 0.0f
        textViewConnectedNetworkName?.alpha = 1.0f
        clAutoSecure?.visibility = View.VISIBLE
        autoSecureDivider?.visibility =
            if (networkInfo?.isAutoSecureOn == true) View.VISIBLE else View.GONE
        clPreferred?.visibility =
            if (networkInfo?.isAutoSecureOn == true) View.VISIBLE else View.GONE
        clProtocol?.visibility =
            if (networkInfo?.isAutoSecureOn == true && networkInfo.isPreferredOn) View.VISIBLE else View.GONE
        clPort?.visibility =
            if (networkInfo?.isAutoSecureOn == true && networkInfo.isPreferredOn) View.VISIBLE else View.GONE
        portProtocolDivider?.visibility =
            if (networkInfo?.isAutoSecureOn == true && networkInfo.isPreferredOn) View.VISIBLE else View.GONE
    }

    private fun setNetworkLayoutExpandedLevel3(networkInfo: NetworkInfo?) {
        if (protocolAdapter == null) {
            if (presenter.isConnectedOrConnecting && networkLayoutState == NetworkLayoutState.OPEN_2) {
                networkInfo?.protocol = presenter.selectedProtocol
                networkInfo?.port = presenter.selectedPort
            }
            if (networkInfo != null) {
                presenter.setProtocolAdapter(networkInfo.protocol)
            }
        }
        networkLayoutState = NetworkLayoutState.OPEN_3
        animateBottomGradient(false)
        collapseExpandExpandIcon?.animate()?.rotation(-180f)?.alpha(1.0f)?.setDuration(300)?.start()
        autoSecureToggle?.setImageDrawable(
            if (networkInfo?.isAutoSecureOn == true) getDrawableFromTheme(R.drawable.ic_toggle_button_on) else getDrawableFromTheme(
                R.drawable.ic_toggle_button_off_dark
            )
        )
        preferredProtocolToggle?.setImageDrawable(
            if (networkInfo?.isPreferredOn == true) getDrawableFromTheme(R.drawable.ic_toggle_button_on) else getDrawableFromTheme(
                R.drawable.ic_toggle_button_off_dark
            )
        )
        networkIcon?.setImageDrawable(
            if (networkInfo?.isAutoSecureOn == true) getDrawableFromTheme(R.drawable.ic_wifi_secure) else getDrawableFromTheme(
                R.drawable.ic_wifi_unsecure
            )
        )
        textViewConnectedNetworkName?.text = networkInfo?.networkName
        networkIcon?.alpha = 1.0f
        lockIcon?.alpha = 0.0f
        textViewIpAddress?.alpha = 0.0f
        textViewConnectedNetworkName?.alpha = 1.0f
        clAutoSecure?.visibility = View.VISIBLE
        autoSecureDivider?.visibility =
            if (networkInfo?.isAutoSecureOn == true) View.VISIBLE else View.GONE
        clPreferred?.visibility =
            if (networkInfo?.isAutoSecureOn == true) View.VISIBLE else View.GONE
        clProtocol?.visibility =
            if (networkInfo?.isAutoSecureOn == true && networkInfo.isPreferredOn) View.VISIBLE else View.GONE
        clPort?.visibility =
            if (networkInfo?.isAutoSecureOn == true && networkInfo.isPreferredOn) View.VISIBLE else View.GONE
        portProtocolDivider?.visibility =
            if (networkInfo?.isAutoSecureOn == true && networkInfo.isPreferredOn) View.VISIBLE else View.GONE
    }

    override fun setPortAndProtocol(protocol: String, port: String) {
        this.protocol?.text = protocol
        this.port?.text = port
    }

    override fun setRefreshLayout(refreshing: Boolean) {
        runOnUiThread {
            serverListFragments.let {
                for (serverListFragment in it) {
                    serverListFragment.setRefreshingLayout(refreshing)
                }
            }
        }
    }

    private fun setRefreshLayoutEnabled(enabled: Boolean) {
        runOnUiThread {
            serverListFragments.let {
                if (it.isNotEmpty()) {
                    for (serverListFragment in it) {
                        serverListFragment.setSwipeRefreshLayoutEnabled(enabled)
                    }
                }
            }
        }
    }

    override fun setServerListToolbarElevation(elevation: Int) {
        serverListToolbar?.elevation = elevation.toFloat()
    }

    override fun setStaticRegionAdapter(staticRegionAdapter: StaticRegionAdapter) {
        serverListFragments.let {
            if (it[3].recyclerView != null) {
                it[3].clearErrors()
                it[3].recyclerView?.adapter = staticRegionAdapter
            }
        }
    }

    override fun setStreamingNodeAdapter(streamingNodeAdapter: StreamingNodeAdapter) {
        serverListFragments.let {
            if (it[2].recyclerView != null) {
                it[2].clearErrors()
                it[2].recyclerView?.adapter = streamingNodeAdapter
            }
        }
    }

    override fun setUpLayoutForNodeUnderMaintenance() {
        NodeStatusDialog.show(this)
    }

    override fun setupAccountStatusBanned() {
        AccountStatusDialogData(
            title = resources.getString(R.string.you_ve_been_banned),
            icon = R.drawable.garry_angry,
            description = resources.getString(R.string.you_ve_violated_our_terms),
            showSkipButton = false,
            skipText = "",
            showUpgradeButton = true,
            upgradeText = resources.getString(R.string.ok),
            bannedLayout = true
        ).let {
            AccountStatusDialog.show(this, it)
        }
    }

    override fun setupAccountStatusExpired() {
        AccountStatusDialogData(
            title = resources.getString(R.string.you_re_out_of_data),
            icon = R.drawable.garry_nodata,
            description = resources.getString(R.string.upgrade_to_stay_protected),
            showSkipButton = true,
            skipText = resources.getString(R.string.upgrade_later),
            showUpgradeButton = true,
            upgradeText = resources.getString(R.string.upgrade),
        ).let {
            AccountStatusDialog.show(this, it)
        }
    }

    override fun setupAccountStatusOkay() {
        AccountStatusDialog.hide(this)
    }

    override fun setupLayoutConnected(state: ConnectedState) {
        setConnectionUIState(state)
        runOnUiThread {
            lockIcon?.setImageResource(state.lockIconResource)
            setToolBarColors(state.flagGradientEndColor)
            lockIcon?.setImageResource(state.lockIconResource)
            setOnOffButton(state)
            setConnectionState(state)
            constraintSetMain.applyTo(constraintLayoutMain)
        }
        setRefreshLayoutEnabled(false)
    }

    override fun setupLayoutConnecting(state: ConnectingState) {
        setConnectionUIState(state)
        runOnUiThread {
            setToolBarColors(state.flagGradientEndColor)
            lockIcon?.setImageResource(state.lockIconResource)
            setOnOffButton(state)
            setConnectionState(state)
            constraintSetMain.applyTo(constraintLayoutMain)
        }
        setRefreshLayoutEnabled(false)
    }

    override fun setupLayoutDisconnected(connectionState: DisconnectedState) {
        setConnectionUIState(connectionState)
        runOnUiThread {
            setToolBarColors(connectionState.flagGradientEndColor)
            lockIcon?.setImageResource(connectionState.lockIconResource)
            setOnOffButton(connectionState)
            setConnectionState(connectionState)
            if (connectionState.isCustomBackgroundEnabled) {
                connectionState.disconnectedFlagPath?.let {
                    setupLayoutForCustomBackground(it)
                }
            } else {
                setCountryFlag(connectionState.flag)
            }
            constraintSetMain.applyTo(constraintLayoutMain)
        }
        setRefreshLayoutEnabled(true)
    }

    override fun setupLayoutDisconnecting(
        connectionState: String, connectionStateTextColor: Int
    ) {
        runOnUiThread {
            setConnectionStateText(connectionState)
            this.connectionState?.setTextColor(connectionStateTextColor)
        }
    }

    override fun setupLayoutForCustomBackground(path: String) {
        if (lastCustomBackgroundPath != null && lastCustomBackgroundPath == path) {
            return
        }
        lastCustomBackgroundPath = path
        Glide.with(this).load(path).skipMemoryCache(true)
            .listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any,
                    target: Target<Drawable?>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    if (resource != null) {
                        flagView?.layoutParams?.height = flagDimensionsGuideView?.measuredHeight
                        flagView?.scaleType = ImageView.ScaleType.FIT_XY
                    }
                    constraintSetMain.setVisibility(R.id.top_gradient, ConstraintSet.INVISIBLE)
                    findViewById<View>(R.id.top_gradient).visibility = View.INVISIBLE
                    constraintSetMain.setVisibility(
                        R.id.top_gradient_custom, ConstraintSet.VISIBLE
                    )
                    findViewById<View>(R.id.top_gradient_custom).visibility = View.VISIBLE
                    constraintSetMain.setVerticalBias(R.id.cl_flag, 0.0f)
                    constraintSetMain.connect(
                        R.id.connection_gradient,
                        ConstraintSet.BOTTOM,
                        R.id.toolbar_windscribe,
                        ConstraintSet.BOTTOM
                    )
                    constraintSetMain.applyTo(constraintLayoutMain)
                    return false
                }
            }).diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.with(drawableCrossFadeFactory)).into(flagView!!)
        setTextShadows()
    }

    override fun setupLayoutForFreeUser(dataLeft: String, upgradeLabel: String, color: Int) {
        for (fragment in serverListFragments) {
            fragment.showUpgradeLayout(color, upgradeLabel, dataLeft)
        }
        serverListFragments[3].hideUpgradeLayout()
        serverListFragments[4].hideUpgradeLayout()
    }

    override fun setupLayoutForProUser() {
        serverListFragments.let {
            for (fragment in it) {
                fragment.hideUpgradeLayout()
            }
        }
    }

    override fun setupLayoutForReconnect(connectionState: String, connectionStateTextColor: Int) {
        runOnUiThread {
            setConnectionStateText(connectionState)
            this.connectionState?.setTextColor(connectionStateTextColor)
        }
    }

    override fun setupLayoutUnsecuredNetwork(uiState: ConnectionUiState) {
        uiConnectionState = uiState
        runOnUiThread {
            runOnUiThread {
                lockIcon?.setImageResource(uiState.lockIconResource)
                setToolBarColors(uiState.flagGradientEndColor)
                setOnOffButton(uiState)
                setConnectionState(uiState)
                constraintSetMain.setVisibility(R.id.on_off_progress_bar, ConstraintSet.GONE)
                constraintSetMain.setVisibility(R.id.on_off_progress_bar, ConstraintSet.VISIBLE)
                constraintSetMain.setVisibility(R.id.on_off_ring, ConstraintSet.GONE)
            }
        }
    }

    override fun setupPortMapAdapter(savedPort: String, ports: List<String>) {
        portAdapter = ArrayAdapter(this, R.layout.drop_down_layout, R.id.tv_drop_down, ports)
        portSpinner?.adapter = portAdapter
        portSpinner?.isSelected = false
        portAdapter?.let {
            portSpinner?.setSelection(it.getPosition(savedPort))
        }
        currentPort?.text = savedPort
    }

    override fun setupProtocolAdapter(savedProtocol: String, protocols: Array<String>) {
        protocolAdapter =
            ArrayAdapter(this, R.layout.drop_down_layout, R.id.tv_drop_down, protocols)
        protocolAdapter?.let {
            protocolSpinner?.adapter = protocolAdapter
            protocolSpinner?.isSelected = false
            protocolSpinner?.setSelection(it.getPosition(savedProtocol))
            currentProtocol?.text = savedProtocol
        }
    }

    override fun setupSearchLayout(
        groups: List<ExpandableGroup<*>>,
        serverListData: ServerListData,
        listViewClickListener: ListViewClickListener
    ) {
        val fragment = supportFragmentManager.findFragmentById(R.id.cl_windscribe_main)
        if (fragment is SearchFragment) {
            return
        }
        try {
            val searchFragment =
                SearchFragment.newInstance(groups, serverListData, listViewClickListener)
            searchFragment.enterTransition = Slide(Gravity.BOTTOM).addTarget(R.id.search_layout)
            supportFragmentManager.beginTransaction()
                .replace(R.id.cl_windscribe_main, searchFragment).addToBackStack(null).commit()
        } catch (e: IllegalStateException) {
            logger.info("Illegal state to add search layout.")
        }
    }

    override fun showConfigLocAdapterLoadError(errorText: String, configCount: Int) {
        serverListFragments.let {
            if (it[4].recyclerView != null) {
                it[4].setAddMoreConfigLayout(errorText, configCount)
            }
        }
    }

    override fun showDialog(message: String) {
        val alertDialog = AlertDialog.Builder(this, R.style.alert_dialog_theme).setCancelable(true)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok)) { dialog: DialogInterface, _: Int -> dialog.cancel() }
            .create()
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        alertDialog.show()
    }

    override fun showFavouriteAdapterLoadError(errorText: String) {
        serverListFragments.let {
            if (it[1].recyclerView != null) {
                it[1].setErrorNoItems(errorText)
            }
        }
    }

    override fun showListBarSelectTransition(resourceSelected: Int) {
        logger.info("In server list menu selection transition...")
        runOnUiThread {
            constraintSetServerList.connect(
                R.id.img_server_list_selection_mask,
                ConstraintSet.START,
                resourceSelected,
                ConstraintSet.START
            )
            constraintSetServerList.connect(
                R.id.img_server_list_selection_mask,
                ConstraintSet.END,
                resourceSelected,
                ConstraintSet.END
            )
            when (resourceSelected) {
                R.id.img_server_list_all -> setBarSelected(
                    serverAll = true,
                    favNav = false,
                    flixLoc = false,
                    staticIp = false,
                    configLoc = false
                )
                R.id.img_server_list_favorites -> setBarSelected(
                    serverAll = false,
                    favNav = true,
                    flixLoc = false,
                    staticIp = false,
                    configLoc = false
                )
                R.id.img_server_list_flix -> setBarSelected(
                    serverAll = false,
                    favNav = false,
                    flixLoc = true,
                    staticIp = false,
                    configLoc = false
                )
                R.id.img_static_ip_list -> setBarSelected(
                    serverAll = false,
                    favNav = false,
                    flixLoc = false,
                    staticIp = true,
                    configLoc = false
                )
                R.id.img_config_loc_list -> setBarSelected(
                    serverAll = false,
                    favNav = false,
                    flixLoc = false,
                    staticIp = false,
                    configLoc = true
                )
            }
            transition = AutoTransition()
            transition?.duration = AnimConstants.CONNECTION_MODE_ANIM_DURATION
            android.transition.TransitionManager.beginDelayedTransition(
                constraintLayoutServerList, transition
            )
            constraintSetServerList.applyTo(constraintLayoutServerList)
        }
    }

    override fun showNotificationCount(count: Int) {
        if (count > 0) {
            notificationCountView?.text = count.toString()
            notificationCountView?.visibility = View.VISIBLE
        } else {
            notificationCountView?.visibility = View.INVISIBLE
        }
    }

    override fun showRecyclerViewProgressBar() {
        runOnUiThread {
            serverListFragments.let {
                if (it[0].recyclerView != null) {
                    it[0].clearErrors()
                }
            }
            progressBarRecyclerView?.visibility = View.VISIBLE
            val color = getColorFromTheme(R.attr.progressBarColor, R.color.colorWhite40)
            progressBarRecyclerView?.indeterminateDrawable?.colorFilter =
                PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    override fun showReloadError(error: String) {
        runOnUiThread {
            serverListFragments.let {
                if (it[0].recyclerView != null) {
                    it[0].setLoadRetry(error)
                }
            }
        }
    }

    override fun showStaticIpAdapterLoadError(
        errorText: String, buttonText: String, deviceName: String
    ) {
        serverListFragments.let {
            if (it[3].recyclerView != null) {
                it[3].setErrorNoStaticIp(buttonText, errorText, deviceName)
            }
        }
    }

    override fun showToast(toastMessage: String) {
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
    }

    override fun startVpnConnectedAnimation(state: ConnectedAnimationState) {
        setConnectionUIState(state)
        runOnUiThread {
            lockIcon?.setImageResource(state.lockIconResource)
            setOnOffButton(state)
            setConnectionState(state)
            constraintSetMain.applyTo(constraintLayoutMain)
            clearFlagAnimation()
            if (state.isCustomBackgroundEnabled) {
                state.connectedFlagPath?.let { setupLayoutForCustomBackground(it) }
            } else {
                clearFlagAnimation()
                flagView?.setImageResource(state.flag)
            }
            // Gradient Animation
            val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
            valueAnimator.addUpdateListener {
                val gradientColor = argbEvaluator.evaluate(
                    valueAnimator.animatedFraction,
                    state.flagGradientStartColor,
                    state.flagGradientEndColor
                ) as Int
                setToolBarColors(gradientColor)
                connectionState?.setTextColor(
                    (argbEvaluator.evaluate(
                        valueAnimator.animatedFraction,
                        state.connectionStateStatusStartColor,
                        state.connectionStateStatusEndColor
                    ) as Int)
                )
            }
            valueAnimator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationCancel(animation: Animator) {
                    valueAnimator.removeAllListeners()
                    presenter.onConnectedAnimationCompleted()
                }

                override fun onAnimationEnd(animation: Animator) {
                    valueAnimator.removeAllListeners()
                    presenter.onConnectedAnimationCompleted()
                }

                override fun onAnimationRepeat(animation: Animator) {}
                override fun onAnimationStart(animation: Animator) {}
            })
            valueAnimator.duration = 1000
            valueAnimator.start()
        }
    }

    private var connectingAnimation: ValueAnimator? = null
    override fun clearConnectingAnimation() {
        connectingAnimation?.cancel()
    }

    override fun startVpnConnectingAnimation(state: ConnectingAnimationState) {
        setConnectionUIState(state)
        runOnUiThread {
            lockIcon?.setImageResource(state.lockIconResource)
            setToolBarColors(state.flagGradientEndColor)
            lockIcon?.setImageResource(state.lockIconResource)
            setOnOffButton(state)
            setConnectionState(state)
            constraintSetMain.applyTo(constraintLayoutMain)
            if (state.isCustomBackgroundEnabled) {
                state.disconnectedFlagPath?.let { setupLayoutForCustomBackground(it) }
            } else {
                animateFlag(state)
            }
            // Gradient Animation
            connectingAnimation = ValueAnimator.ofFloat(0f, 1f)
            connectingAnimation?.let { valueAnimator ->
                valueAnimator.addUpdateListener {
                    val gradientColor = argbEvaluator.evaluate(
                        valueAnimator.animatedFraction,
                        state.flagGradientStartColor,
                        state.flagGradientEndColor
                    ) as Int
                    setToolBarColors(gradientColor)
                    connectionState?.setTextColor(
                        (argbEvaluator.evaluate(
                            valueAnimator.animatedFraction,
                            state.connectionStateStatusStartColor,
                            state.connectionStateStatusEndColor
                        ) as Int)
                    )
                }
                valueAnimator.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationCancel(animation: Animator) {
                        valueAnimator.removeAllListeners()
                        if (uiConnectionState is ConnectingAnimationState) {
                            presenter.onConnectingAnimationCompleted()
                        }
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        valueAnimator.removeAllListeners()
                        if (uiConnectionState is ConnectingAnimationState) {
                            presenter.onConnectingAnimationCompleted()
                        }
                    }

                    override fun onAnimationRepeat(animation: Animator) {}
                    override fun onAnimationStart(animation: Animator) {}
                })
                valueAnimator.duration = VPN_CONNECTING_ANIMATION_DELAY
                valueAnimator.start()
            }
        }
    }

    override fun updateLocationName(nodeName: String, nodeNickName: String) {
        logger.info("Updating selected location. Name: $nodeName Nickname: $nodeNickName")
        textViewLocationName?.text = nodeName
        textViewLocationNick?.text = nodeNickName
    }

    override fun updateProgressView(text: String) {
        runOnUiThread {
            if (!customDialog.isShowing) {
                customDialog.show()
                customDialog.setOwnerActivity(this@WindscribeActivity)
                customDialog.setCancelable(true)
                customDialog.setCanceledOnTouchOutside(false)
            }
            customDialog.setTitle(text)
        }
    }

    override fun updateSearchAdapter(serverListData: ServerListData) {
        val fragment = supportFragmentManager.findFragmentById(R.id.cl_windscribe_main)
        if (fragment is SearchFragment) {
            fragment.updateDataSet(serverListData)
        }
    }

    private fun animateBottomGradient(hide: Boolean) {
        if (hide) {
            Looper.myLooper()?.let {
                Handler(it).postDelayed({
                    bottomGradient?.clearAnimation()
                    bottomGradient?.animate()?.alpha(0.0f)?.setDuration(200)?.start()
                }, 300)
            }
        } else {
            bottomGradient?.clearAnimation()
            bottomGradient?.animate()?.alpha(1.0f)?.setDuration(70)?.start()
        }
    }

    private fun animateFlag(state: ConnectionUiState) {
        flagView?.clearAnimation()
        flagView?.y = 0f
        topGradient?.y = 0f
        flagView?.alpha = if (state.isCustomBackgroundEnabled) 1.0f else 0.5f
        if (lastFlag != state.flag) {
            lastFlag = state.flag
            flagView?.let {
                it.animate().translationYBy(VPN_CONNECTING_ANIMATION_DELAY.toFloat())
                    .setInterpolator(AccelerateInterpolator())
                    .setDuration(AnimConstants.FLAG_IMAGE_ANIMATION_PERIOD).withEndAction {
                        it.setImageResource(state.flag)
                        it.animate().translationYBy((-1 * VPN_CONNECTING_ANIMATION_DELAY).toFloat())
                            .setInterpolator(AccelerateInterpolator()).duration =
                            AnimConstants.FLAG_IMAGE_ANIMATION_PERIOD
                    }
            }

            topGradient?.let {
                it.animate().translationYBy(VPN_CONNECTING_ANIMATION_DELAY.toFloat())
                    .setInterpolator(AccelerateInterpolator())
                    .setDuration(AnimConstants.FLAG_IMAGE_ANIMATION_PERIOD).withEndAction {
                        it.animate().translationYBy((-1 * VPN_CONNECTING_ANIMATION_DELAY).toFloat())
                            .setInterpolator(AccelerateInterpolator()).duration =
                            AnimConstants.FLAG_IMAGE_ANIMATION_PERIOD
                    }
            }
        } else {
            flagView?.setImageResource(state.flag)
        }
    }

    private fun cancelRefreshing(ignore: Int) {
        serverListFragments.let {
            for (i in it.indices) {
                if (i != ignore) {
                    it[i].setRefreshingLayout(false)
                }
            }
        }
    }

    private fun clearFlagAnimation() {
        flagView?.clearAnimation()
        topGradient?.clearAnimation()
        flagView?.y = 0f
        topGradient?.y = 0f
    }

    private fun clearTextShadows() {
        val shadowViews = arrayOf(
            textViewConnectedNetworkName, protocol, port, textViewLocationName, textViewLocationNick
        )
        for (view in shadowViews) {
            view?.setShadowLayer(0f, 0f, 0f, resources.getColor(R.color.colorDeepBlue25))
        }
    }

    private fun getDrawableFromTheme(resourceId: Int): Drawable? {
        return ResourcesCompat.getDrawable(resources, resourceId, theme)
    }

    private fun registerDataChangeObserver() {
        preferenceChangeObserver.addConfigListObserver(
            this
        ) { presenter.loadConfigLocations() }
        preferenceChangeObserver.addLanguageChangeObserver(
            this
        ) { presenter.onLanguageChanged() }
        preferenceChangeObserver.addShowLocationHealthChangeObserver(
            this
        ) { presenter.onShowLocationHealthChanged() }
        preferenceChangeObserver.addLocationSettingsChangeObserver(this) {
            presenter.onLocationSettingsChanged()
        }
        preferenceChangeObserver.addAntiCensorShipStatusChangeObserver(this) {
            presenter.onAntiCensorShipStatusChanged()
        }
    }

    private fun setBarSelected(
        serverAll: Boolean, favNav: Boolean, flixLoc: Boolean, staticIp: Boolean, configLoc: Boolean
    ) {
        imgServerListAll?.isSelected = serverAll
        imgServerListFavorites?.isSelected = favNav
        imgServerListFlix?.isSelected = flixLoc
        imgStaticIpList?.isSelected = staticIp
        imgConfigLocList?.isSelected = configLoc
    }

    private fun setConnectionState(state: ConnectionUiState) {
        toolBarSquare?.setImageDrawable(state.headerBackgroundLeft)
        slopedView?.setImageDrawable(state.headerBackgroundRight)
        setProgressBarDrawable(connectionIcon, state.connectionStatusIcon)
        setConnectionStateText(state.connectionStateStatusText)
        connectionState?.setTextColor(state.connectionStateStatusEndColor)
        connectionState?.background = state.connectionStatusBackground
        protocol?.setTextColor(state.portAndProtocolEndTextColor)
        port?.setTextColor(state.portAndProtocolEndTextColor)
        connectionIcon?.isIndeterminate = state.rotateConnectingIcon()
        connectionIcon?.visibility = state.connectingIconVisibility
        preferredProtocolStatus?.setImageDrawable(state.preferredProtocolStatusDrawable)
        preferredProtocolStatus?.visibility = state.preferredProtocolStatusVisibility
        tvDecoy?.visibility = state.decoyTrafficBadgeVisibility
        decoyDivider?.visibility = state.decoyTrafficBadgeVisibility
        decoyArrow?.visibility = state.decoyTrafficBadgeVisibility
        if (state.decoyTrafficBadgeVisibility != VISIBLE && state is ConnectedState) {
            changeProtocolArrow?.visibility = VISIBLE
        } else {
            changeProtocolArrow?.visibility = GONE
        }
        antiCensorShipIcon?.visibility = state.antiCensorShipStatusVisibility
        antiCensorShipIcon?.setImageDrawable(state.antiCensorShipStatusDrawable)

        constraintSetMain.setAlpha(R.id.tv_protocol, state.badgeViewAlpha)
    }

    private fun setLastServerTabSelected() {
        val lastIndex = presenter.lastSelectedTabIndex
        locationFragmentViewPager?.currentItem = lastIndex
    }

    private fun setOnOffButton(state: ConnectionUiState) {
        setProgressBarDrawable(onOffProgressBar, state.progressRingResource)
        onOffButton?.setImageResource(state.onOffButtonResource)
        onOffProgressBar?.visibility = state.progressRingVisibility
        constraintSetMain.setVisibility(R.id.on_off_ring, state.connectedCenterIconVisibility)
        constraintSetMain.setVisibility(R.id.on_off_progress_bar, state.progressRingVisibility)
    }

    private fun setProgressBarDrawable(progressBar: ProgressBar?, drawable: Drawable?) {
        progressBar?.let {
            val bounds =
                progressBar.indeterminateDrawable.bounds // re-use bounds from current drawable
            progressBar.indeterminateDrawable = drawable // set new drawable
            progressBar.indeterminateDrawable.bounds = bounds
        }
    }

    private fun setServerListView(reload: Boolean) {
        val pagerAdapter = ServerListFragmentPager(
            supportFragmentManager, serverListFragments
        )
        locationFragmentViewPager?.offscreenPageLimit = 4
        locationFragmentViewPager?.adapter = pagerAdapter
        locationFragmentViewPager?.addOnPageChangeListener(this)
        serverListFragments.let {
            for (item in it) {
                item.setFragmentClickListener(this)
            }
        }
        setLastServerTabSelected()
        constraintLayoutMain?.let {
            it.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    it.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    if (!reload) {
                        logger.info("Activity layout drawing completed.")
                        presenter.init()
                    }
                    //Set adapters if they were created before view was ready.
                    presenter.setAdapters()
                }
            })
        }
    }

    private fun setTextShadows() {
        val shadowViews = arrayOf(
            textViewConnectedNetworkName, protocol, port, textViewLocationName, textViewLocationNick
        )
        for (view in shadowViews) {
            view?.setShadowLayer(0.01f, 0f, 6f, resources.getColor(R.color.colorDeepBlue25))
        }
    }

    private fun setToolBarColors(gradientColor: Int) {
        connectionGradient?.let {
            val drawable = it.drawable as GradientDrawable
            drawable.colors =
                intArrayOf(resources.getColor(android.R.color.transparent), gradientColor)
        }
    }

    private fun setupLayoutForAppFlagBackground() {
        lastCustomBackgroundPath = null
        flagView?.let {
            it.layoutParams.height = ConstraintSet.WRAP_CONTENT
            it.scaleType = ImageView.ScaleType.FIT_CENTER
        }
        constraintSetMain.connect(
            R.id.connection_gradient,
            ConstraintSet.BOTTOM,
            R.id.cl_preferred_protocol,
            ConstraintSet.TOP
        )
        constraintSetMain.setVisibility(R.id.top_gradient_custom, ConstraintSet.INVISIBLE)
        findViewById<View>(R.id.top_gradient_custom).visibility = View.INVISIBLE
        constraintSetMain.setVisibility(R.id.top_gradient, ConstraintSet.VISIBLE)
        findViewById<View>(R.id.top_gradient).visibility = View.VISIBLE
        constraintSetMain.setVerticalBias(R.id.cl_flag, 1.0f)
        constraintSetMain.applyTo(constraintLayoutMain)
        clearTextShadows()
    }

    override fun setDecoyTrafficInfoVisibility(visibility: Int) {
        decoyArrow?.visibility = visibility
        decoyDivider?.visibility = visibility
        tvDecoy?.visibility = visibility
    }

    override fun showShareLinkDialog() {
        ShareAppLinkDialog.show(this)
    }

    @OnClick(R.id.tv_decoy_label, R.id.img_decoy_traffic_arrow)
    fun onDecoyTrafficClick() {
        presenter.onDecoyTrafficClick()
    }

    @OnClick(R.id.img_protocol_change_arrow)
    fun onProtocolChangeClick() {
        presenter.onProtocolChangeClick()
    }

    override fun setCensorShipIconVisibility(visible: Int) {
        antiCensorShipIcon?.visibility = visible
    }

    companion object {
        @JvmStatic
        fun getStartIntent(context: Context): Intent {
            return Intent(context, WindscribeActivity::class.java)
        }
    }
}
