/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.tv.windscribe

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import butterknife.BindView
import butterknife.OnClick
import butterknife.OnFocusChange
import com.bumptech.glide.Glide
import com.windscribe.tv.R
import com.windscribe.tv.R.*
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.customview.ErrorPrimaryFragment
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.news.NewsFeedActivity
import com.windscribe.tv.rate.RateMyAppActivity
import com.windscribe.tv.serverlist.adapters.ServerAdapter
import com.windscribe.tv.serverlist.customviews.AutoFitRecyclerView
import com.windscribe.tv.serverlist.customviews.FocusAwareConstraintLayout
import com.windscribe.tv.serverlist.customviews.HomeUpgradeButton
import com.windscribe.tv.serverlist.overlay.OverlayActivity
import com.windscribe.tv.settings.SettingActivity
import com.windscribe.tv.support.HelpActivity
import com.windscribe.tv.upgrade.UpgradeActivity
import com.windscribe.tv.welcome.WelcomeActivity
import com.windscribe.tv.windscribe.WindscribeView.ConnectionStateAnimationListener
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.ServerCredentialsResponse
import com.windscribe.vpn.constants.AnimConstants
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.DeviceStateManager.DeviceStateListener
import com.windscribe.vpn.state.PreferenceChangeObserver
import com.windscribe.vpn.state.VPNConnectionStateManager
import org.slf4j.LoggerFactory
import javax.inject.Inject

class WindscribeActivity : BaseActivity(), WindscribeView, DeviceStateListener,
    FocusAwareConstraintLayout.OnWindowResizeListener {

    @Inject
    lateinit var deviceStateManager: DeviceStateManager

    @Inject
    lateinit var preferenceChangeObserver: PreferenceChangeObserver

    @Inject
    lateinit var windscribePresenter: WindscribePresenter

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    @JvmField
    @BindView(id.btn_help)
    var btnHelp: ImageView? = null

    @JvmField
    @BindView(id.btn_notifications)
    var btnNotifications: ImageView? = null

    @JvmField
    @BindView(id.btn_settings)
    var btnSettings: ImageView? = null

    @JvmField
    @BindView(id.upgrade_parent)
    var btnUpgrade: HomeUpgradeButton? = null

    @JvmField
    @BindView(id.vpn)
    var btnVpn: ImageView? = null

    @JvmField
    @BindView(id.city_name)
    var cityNameLabel: TextView? = null

    @JvmField
    @BindView(id.connectGlow)
    var connectGlow: ImageView? = null

    @JvmField
    @BindView(id.connection_progress_bar)
    var connectionProgressBar: ProgressBar? = null

    @JvmField
    @BindView(id.connection_status)
    var connectionStateLabel: TextView? = null

    @JvmField
    @BindView(id.data_left_label)
    var dataLeftLabel: TextView? = null

    @JvmField
    @BindView(id.img_connected)
    var imgConnected: ImageView? = null

    @JvmField
    @BindView(id.ip_address_label)
    var ipAddressLabel: TextView? = null

    @JvmField
    @Inject
    var rgbEvaluator: ArgbEvaluator? = null

    @JvmField
    @BindView(id.protocol_badge_view)
    var protocolBadgeBackground: ImageView? = null

    @JvmField
    @BindView(id.protocol_badge_text)
    var protocolBadgeText: TextView? = null

    @JvmField
    @BindView(id.img_flag_gradient_top)
    var flagGradientTop: ImageView? = null

    @JvmField
    @BindView(id.flag_alpha)
    var flagView: ImageView? = null

    @JvmField
    @BindView(id.lockIcon)
    var lockIcon: ImageView? = null

    @JvmField
    @BindView(id.split_routing_view)
    var splitRoutingView: ImageView? = null

    @JvmField
    @BindView(id.cl_windscribe_main)
    var mainParentLayout: FocusAwareConstraintLayout? = null

    @JvmField
    @BindView(id.node_name)
    var nodeNameLabel: TextView? = null

    @JvmField
    @BindView(id.partialOverlay)
    var partialView: AutoFitRecyclerView? = null

    @JvmField
    @BindView(id.progressBar)
    var progressView: ProgressBar? = null

    @JvmField
    @BindView(id.upgrade_label)
    var upgradeLabel: TextView? = null

    @JvmField
    @BindView(id.vpnButtonWrapper)
    var vpnButtonWrapper: ConstraintLayout? = null

    private var connectedAnimator: ValueAnimator? = null
    private var connectingAnimator: ValueAnimator? = null
    private var icon = 0
    private val mainLogger = LoggerFactory.getLogger(TAG)
    private val serverListLoadedFirstTime = true
    private var state = 0
    private val overlayStartRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_windscribe)
        setViews()
        registerDataChangeObserver()
    }

    // Life cycle
    override fun onStart() {
        super.onStart()
        mainLogger.info("Activity on start method,registering network and vpn status listener")
        if (intent != null && intent.action != null && intent.action == NotificationConstants.DISCONNECT_VPN_INTENT) {
            mainLogger.info("Disconnect intent received...")
            windscribePresenter.onDisconnectIntentReceived()
        }
        deviceStateManager.addListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (!coldLoad.getAndSet(false)) {
            mainLogger.info("Activity on resume.")
            windscribePresenter.onHotStart()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            val connectingToStaticIP = intent.getBooleanExtra("connectingToStaticIP", false)
            val cityID = intent.getIntExtra("city", -1)
            if (connectingToStaticIP) {
                val serverCredentialsResponse = ServerCredentialsResponse()
                serverCredentialsResponse.userNameEncoded = intent.getStringExtra("username")
                serverCredentialsResponse.passwordEncoded = intent.getStringExtra("password")
                windscribePresenter.connectWithSelectedStaticIp(cityID, serverCredentialsResponse)
            } else {
                windscribePresenter.connectWithSelectedLocation(cityID)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mainLogger.info("Activity on stop method,un-registering network and vpn status listener")
        deviceStateManager.removeListener(this)
    }

    override fun onDestroy() {
        mainLogger.info("Activity on destroy method")
        windscribePresenter.onDestroy()
        super.onDestroy()
    }

    override fun flashProtocolBadge(flash: Boolean) {
        runOnUiThread {
            if (flash) {
                protocolBadgeBackground?.clearAnimation()
                val alphaAnimation = AlphaAnimation(0.0f, 0.3f)
                alphaAnimation.repeatCount = 50
                alphaAnimation.duration = 500
                alphaAnimation.isFillEnabled = false
                alphaAnimation.setAnimationListener(object : AnimationListener {
                    override fun onAnimationEnd(animation: Animation) {
                        protocolBadgeBackground?.alpha = 1.0f
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                    override fun onAnimationStart(animation: Animation) {}
                })
                protocolBadgeBackground?.startAnimation(alphaAnimation)
            } else {
                protocolBadgeBackground?.clearAnimation()
            }
            if (flash) {
                protocolBadgeText?.clearAnimation()
                val alphaAnimation = AlphaAnimation(0.0f, 0.3f)
                alphaAnimation.repeatCount = 50
                alphaAnimation.duration = 500
                alphaAnimation.isFillEnabled = false
                alphaAnimation.setAnimationListener(object : AnimationListener {
                    override fun onAnimationEnd(animation: Animation) {
                        protocolBadgeText?.alpha = 1.0f
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                    override fun onAnimationStart(animation: Animation) {}
                })
                protocolBadgeText?.startAnimation(alphaAnimation)
            } else {
                protocolBadgeText?.clearAnimation()
            }
        }
    }

    // Network
    override val networkInfo: NetworkInfo?
        get() {
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            return connectivityManager.activeNetworkInfo
        }

    override fun gotoLoginRegistrationActivity() {
        finish()
        val intent = WelcomeActivity.getStartIntent(this)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    // Rate view
    override fun handleRateView() {
        startActivity(Intent(this, RateMyAppActivity::class.java))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val window = window
        window.setFormat(PixelFormat.RGBA_8888)
    }

    override fun onBackPressed() {
        windscribePresenter.onBackPressed()
    }

    override fun onLogout() {
        finish()
        val intent = WelcomeActivity.getStartIntent(this)
        intent.putExtra(ERROR_TAG, string.you_ve_been_banned)
        startActivity(intent)
    }

    // Activity
    override fun openMenuActivity() {
        startActivity(SettingActivity.getStartIntent(this))
        mainLogger.info("Opening settings activity.")
    }

    override fun openNewsFeedActivity(showPopUp: Boolean, popUp: Int) {
        mainLogger.info("Opening notification activity.")
        val intent = NewsFeedActivity.getStartIntent(this@WindscribeActivity, showPopUp, popUp)
        startActivity(intent)
    }

    override fun openUpgradeActivity() {
        mainLogger.info("Opening upgrade activity with")
        startActivity(UpgradeActivity.getStartIntent(this))
    }

    override fun quitApplication() {
        super.onBackPressed()
    }

    override fun setBadgeIcon(protocolText: String, disconnected: Boolean) {
        runOnUiThread {
            if (disconnected) {
                protocolBadgeBackground?.alpha = 0.3f
                protocolBadgeText?.alpha = 0.3f
            } else {
                protocolBadgeBackground?.alpha = 1.0f
                protocolBadgeText?.alpha = 1.0f
            }
            protocolBadgeText?.text = protocolText
        }
    }

    override fun setConnectionStateColor(connectionStateColor: Int) {
        connectionStateLabel?.setTextColor(connectionStateColor)
    }

    override fun setConnectionStateText(connectionStateText: String) {
        runOnUiThread { connectionStateLabel?.text = connectionStateText }
    }

    override fun setCountryFlag(flagIconResource: Int) {
        if (icon != flagIconResource) {
            onFadeOut(flagIconResource)
        }
        icon = flagIconResource
        mainLogger.info("Setting country flag." + resources.getResourceName(flagIconResource))
    }

    // Connect btn
    override fun setGlowVisibility(visibility: Int) {
        mainLogger.info("Setting glow visibility changed")
        runOnUiThread { connectGlow?.visibility = visibility }
    }

    override fun setIpAddress(ipAddress: String) {
        mainLogger.info("Setting ip address")
        ipAddressLabel?.text = ipAddress
    }

    override fun setPartialAdapter(serverAdapter: ServerAdapter) {
        partialView?.adapter = serverAdapter
    }

    override fun setState(state: Int) {
        this.state = state
    }

    override fun setVpnButtonState() {
        runOnUiThread {
            btnVpn?.let {
                if (state == 1 && it.rotation >= 0.0f && it.rotation < 180.0f) {
                    it.animate().rotation(180.0f).duration =
                        AnimConstants.BROWSE_WINDOW_ANIM_DURATION
                } else if (state == 0 && it.rotation == 180f) {
                    it.animate().rotation(0.0f).duration = AnimConstants.BROWSE_WINDOW_ANIM_DURATION
                }
            }
        }
    }

    override fun setupAccountStatusBanned() {
        mainLogger.info("User status banned. logging out user.")
        windscribePresenter.logout()
    }

    override fun setupAccountStatusDowngraded() {
        mainLogger.info("User status downgraded. opening upgrade activity.")
        openUpgradeActivity()
    }

    // Set layout
    override fun setupAccountStatusExpired() {
        mainLogger.info("User status expired. opening upgrade activity.")
        openUpgradeActivity()
    }

    // Animation
    override fun setupLayoutConnected(
        finalColor: Int,
        connectionStateTextColor: Int,
        showSplitTunnelAView: Boolean
    ) {
        runOnUiThread {
            if (showSplitTunnelAView) {
                splitRoutingView?.visibility = View.VISIBLE
            } else {
                splitRoutingView?.visibility = View.GONE
            }
            protocolBadgeBackground?.visibility = View.VISIBLE
            flashProtocolBadge(false)
            mainLogger.info("Setting layout for connected state.")
            setGlowVisibility(View.GONE)
            setConnectionStateText(resources.getString(string.connected))
            connectionStateLabel?.setTextColor(connectionStateTextColor)
            imgConnected?.visibility = View.VISIBLE
            lockIcon?.setImageResource(drawable.ic_ip_secure_icon)
            flagGradientTop?.setColorFilter(finalColor)
            state = 1
            setVpnButtonState()
        }
    }

    override fun setupLayoutConnecting(connectionState: String) {
        runOnUiThread {
            splitRoutingView?.visibility = View.GONE
            protocolBadgeBackground?.visibility = View.VISIBLE
            flashProtocolBadge(true)
            mainLogger.info("Setting layout for connecting state.")
            setGlowVisibility(View.GONE)
            setConnectionStateText(connectionState)
            imgConnected?.visibility = View.GONE
            connectionProgressBar?.visibility = View.VISIBLE
            state = 1
            setVpnButtonState()
        }
    }

    override fun setupLayoutDisconnected(connectionStateTextColor: Int) {
        runOnUiThread {
            connectingAnimator?.let {
                if (it.isRunning) {
                    it.cancel()
                }
            }
            connectedAnimator?.let {
                if (it.isRunning) {
                    it.cancel()
                }
            }
            splitRoutingView?.visibility = View.GONE
            flashProtocolBadge(false)
            mainLogger.info("Setting layout for disconnected state.")
            setConnectionStateText(resources.getString(string.disconnected))
            connectionStateLabel?.setTextColor(connectionStateTextColor)
            btnVpn?.clearAnimation()
            state = 0
            setVpnButtonState()
            setGlowVisibility(View.INVISIBLE)
            lockIcon?.setImageResource(drawable.ic_ip_none_secure_icon)
            imgConnected?.visibility = View.GONE
            connectionProgressBar?.visibility = View.GONE
            flagGradientTop?.clearColorFilter()
            flagGradientTop?.clearAnimation()
        }
    }

    override fun setupLayoutDisconnecting(
        connectionState: String,
        connectionStateTextColor: Int
    ) {
        setGlowVisibility(View.GONE)
        runOnUiThread {
            splitRoutingView?.visibility = View.GONE
            mainLogger.info("Setting layout for disconnecting state.")
            setConnectionStateText(connectionState)
            connectionStateLabel?.setTextColor(connectionStateTextColor)
            state = 0
        }
    }

    override fun setupLayoutForFreeUser(dataLeft: String, color: Int) {
        mainLogger.debug("Setting layout for free user.")
        dataLeftLabel?.text = dataLeft
        dataLeftLabel?.setTextColor(color)
        btnUpgrade?.visibility = View.VISIBLE
    }

    override fun setupLayoutForProUser() {
        mainLogger.info("Setting layout for pro user.")
        btnUpgrade?.visibility = View.GONE
    }

    override fun showErrorDialog(error: String) {
        ErrorPrimaryFragment.instance.add(error, this, id.cl_windscribe_main, true)
    }

    override fun showNoNetworkDetected(connectionStatus: String, connectionStatusColor: Int) {
        connectionStateLabel?.text = connectionStatus
        connectionStateLabel?.setTextColor(connectionStatusColor)
        mainLogger.info("Setting connecting status to no internet found.")
    }

    override fun showPartialViewProgress(inProgress: Boolean) {
        if (inProgress) {
            progressView?.visibility = View.VISIBLE
        } else {
            progressView?.visibility = View.GONE
        }
    }

    override fun showSplitViewIcon() {
        splitRoutingView?.visibility = View.VISIBLE
    }

    override fun showToast(toastMessage: String) {
        if (toastMessage.isNotEmpty()) {
            Toast.makeText(appContext, toastMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override fun startSessionServiceScheduler() {
        appContext.workManager.updateSession()
        mainLogger.debug("starting a session service")
    }

    override fun startVpnConnectedAnimation(
        connectionStateString: String,
        backgroundColorStart: Int,
        backgroundColorFinal: Int,
        textColorStart: Int,
        textColorFinal: Int,
        listenerState: ConnectionStateAnimationListener
    ) {
        mainLogger.debug("Starting connected state animation")
        runOnUiThread {
            state = 1
            setVpnButtonState()
            setConnectionStateText(connectionStateString)
            lockIcon?.setImageResource(drawable.ic_ip_secure_icon)
            connectedAnimator = ValueAnimator.ofFloat(0f, 1f)
            connectedAnimator?.let { animator ->
                animator.addUpdateListener {
                    setColorFilter(
                        animator,
                        rgbEvaluator,
                        backgroundColorStart,
                        backgroundColorFinal,
                        flagGradientTop
                    )
                    setTextColor(
                        animator,
                        rgbEvaluator,
                        textColorStart,
                        textColorFinal,
                        connectionStateLabel
                    )
                }
                animator.addListener(object : AnimatorListener {
                    override fun onAnimationCancel(animation: Animator) {
                        animator.removeAllListeners()
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        animator.removeAllListeners()
                        connectionProgressBar?.visibility = View.GONE
                        imgConnected?.visibility = View.VISIBLE
                        listenerState.onConnectedAnimationCompleted()
                        mainLogger.info("Ending connected animation.")
                    }

                    override fun onAnimationRepeat(animation: Animator) {}
                    override fun onAnimationStart(animation: Animator) {}
                })
                animator.duration = 1000
                animator.start()
            }
        }
    }

    override fun startVpnConnectingAnimation(
        connectionStateString: String,
        flagIcon: Int,
        backgroundColorStart: Int,
        backgroundColorFinal: Int,
        textColorStart: Int,
        textColorFinal: Int,
        listenerState: ConnectionStateAnimationListener
    ) {
        mainLogger.debug("Starting vpn connecting state animation.")
        runOnUiThread {
            state = 1
            setConnectionStateText(connectionStateString)
            connectionProgressBar?.visibility = View.VISIBLE
            flashProtocolBadge(false)
            flagView?.alpha = 0.5f
            connectingAnimator = ValueAnimator.ofFloat(0f, 1f)
            connectingAnimator?.let { it ->
                it.addUpdateListener {
                    setColorFilter(
                        it,
                        rgbEvaluator,
                        backgroundColorStart,
                        backgroundColorFinal,
                        flagGradientTop
                    )
                    setTextColor(
                        it,
                        rgbEvaluator,
                        textColorStart,
                        textColorFinal,
                        connectionStateLabel
                    )
                }
                it.addListener(object : AnimatorListener {
                    override fun onAnimationCancel(animation: Animator) {
                        it.removeAllListeners()
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        it.removeAllListeners()
                        listenerState.onConnectingAnimationCompleted()
                    }

                    override fun onAnimationRepeat(animation: Animator) {}
                    override fun onAnimationStart(animation: Animator) {
                        setCountryFlag(flagIcon)
                    }
                })
                it.duration = 500
                it.interpolator = LinearInterpolator()
                it.start()
            }
        }
    }

    private fun setTextColor(
        valueAnimator: ValueAnimator,
        argbEvaluator: ArgbEvaluator?,
        textColorStart: Int,
        textColorFinal: Int,
        textView: TextView?
    ) {
        val color = argbEvaluator?.evaluate(
            valueAnimator.animatedFraction,
            textColorStart,
            textColorFinal
        ) as Int
        textView?.setTextColor(color)
    }

    private fun setColorFilter(
        valueAnimator: ValueAnimator,
        argbEvaluator: ArgbEvaluator?,
        textColorStart: Int,
        textColorFinal: Int,
        imageView: ImageView?
    ) {
        val color = argbEvaluator?.evaluate(
            valueAnimator.animatedFraction,
            textColorStart,
            textColorFinal
        ) as Int
        imageView?.setColorFilter(color)
    }

    override fun updateLocationName(nodeName: String, nodeNickName: String) {
        cityNameLabel?.text = nodeName
        nodeNameLabel?.text = nodeNickName
        mainLogger.info("Updating location name to:$nodeName")
    }

    @OnClick(id.btn_help)
    fun helpClick() {
        startActivity(HelpActivity.getStartIntent(this))
    }

    @OnClick(id.btn_notifications)
    fun notificationClick() {
        openNewsFeedActivity(false, -1)
        mainLogger.debug("News feed button clicked.")
    }

    private fun onFadeIn() {
        flagView?.let { it.animate().alpha(0.5f).setDuration(500).withEndAction {} }
    }

    private fun onFadeOut(flagIconResource: Int) {
        flagView?.let {
            it.animate().alpha(0.0f).setDuration(500).withEndAction { setFlag(flagIconResource) }
        }
    }

    @OnClick(id.btn_settings)
    fun onSettingClick() {
        windscribePresenter.onMenuButtonClicked()
        mainLogger.debug("Setting button clicked.")
    }

    @OnFocusChange(id.vpn)
    fun onVpnBtnFocus() {
        btnVpn?.let {
            if (it.hasFocus()) {
                it.setImageResource(drawable.ic_on_button_off_focused)
            } else {
                it.setImageResource(drawable.ic_on_button_off)
            }
        }
    }

    private fun setFlag(flagIconResource: Int) {
        flagView?.let {
            Glide.with(this@WindscribeActivity)
                .load(ResourcesCompat.getDrawable(resources, flagIconResource, theme))
                .dontAnimate()
                .into(it)
        }
        onFadeIn()
    }

    @OnClick(id.upgrade_parent)
    fun upGradeClick() {
        openUpgradeActivity()
        mainLogger.debug("Upgrade button clicked.")
    }

    @OnClick(id.vpn)
    fun vpnClick() {
        windscribePresenter.onConnectClicked()
        mainLogger.debug("Connect button clicked.")
    }

    private fun registerDataChangeObserver() {
        activityScope { windscribePresenter.observeVPNState() }
        activityScope { windscribePresenter.observeServerList() }
        activityScope { windscribePresenter.observeSelectedLocation() }
        activityScope { windscribePresenter.observeDisconnectedProtocol() }
        activityScope { windscribePresenter.observeConnectedProtocol() }
        windscribePresenter.observeUserState(this)
    }

    private fun setFocusListener() {
        mainParentLayout?.setListener(this)
    }

    private fun setRingDrawable(drawable: Drawable) {
        connectionProgressBar?.let {
            val bounds = it.indeterminateDrawable
                .bounds // re-use bounds from current drawable
            it.indeterminateDrawable = drawable // set new drawable
            it.indeterminateDrawable.bounds = bounds
        }
    }

    // Setup
    private fun setViews() {
        setFocusListener()
        windscribePresenter.init()
    }

    private fun startOverlayActivity() {
        val startIntent = Intent(this, OverlayActivity::class.java)
        startActivityForResult(startIntent, overlayStartRequestCode)
        overridePendingTransition(anim.slide_up, anim.slide_down)
    }

    override fun onNetworkStateChanged() {
        windscribePresenter.onNetworkStateChanged()
    }

    companion object {

        const val ERROR_TAG = "login_error_tag"
        private const val TAG = "windscribe_a"

        @JvmStatic
        fun getStartIntent(context: Context?): Intent {
            val intent = Intent(context, WindscribeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            return intent
        }
    }

    override fun focusEnterToHeader() {
        startOverlayActivity()
    }
}
