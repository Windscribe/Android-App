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
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.windscribe.tv.R
import com.windscribe.tv.R.*
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.customview.ErrorPrimaryFragment
import com.windscribe.tv.databinding.ActivityWindscribeBinding
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.news.NewsFeedActivity
import com.windscribe.tv.rate.RateMyAppActivity
import com.windscribe.tv.serverlist.adapters.ServerAdapter
import com.windscribe.tv.serverlist.customviews.FocusAwareConstraintLayout
import com.windscribe.tv.serverlist.overlay.OverlayActivity
import com.windscribe.tv.settings.SettingActivity
import com.windscribe.tv.support.HelpActivity
import com.windscribe.tv.upgrade.UpgradeActivity
import com.windscribe.tv.welcome.WelcomeActivity
import com.windscribe.tv.windscribe.WindscribeView.ConnectionStateAnimationListener
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.ServerCredentialsResponse
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.*
import com.windscribe.vpn.constants.AnimConstants
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.state.PreferenceChangeObserver
import com.windscribe.vpn.state.VPNConnectionStateManager
import org.slf4j.LoggerFactory
import javax.inject.Inject

class WindscribeActivity : BaseActivity(), WindscribeView, FocusAwareConstraintLayout.OnWindowResizeListener {

    @Inject
    lateinit var preferenceChangeObserver: PreferenceChangeObserver

    @Inject
    lateinit var windscribePresenter: WindscribePresenter

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    @JvmField
    @Inject
    var rgbEvaluator: ArgbEvaluator? = null

    private lateinit var binding: ActivityWindscribeBinding

    private var connectedAnimator: ValueAnimator? = null
    private var connectingAnimator: ValueAnimator? = null
    private var icon = 0
    private val mainLogger = LoggerFactory.getLogger(TAG)
    private var state = 0
    private val overlayStartRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_windscribe)
        onActivityLaunch()
        setViews()
        registerDataChangeObserver()
        addClickListeners()
    }

    // Life cycle
    override fun onStart() {
        super.onStart()
        if (intent != null && intent.action != null && intent.action == NotificationConstants.DISCONNECT_VPN_INTENT) {
            mainLogger.info("Disconnect intent received...")
            windscribePresenter.onDisconnectIntentReceived()
        }
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

    override fun onDestroy() {
        mainLogger.info("Activity on destroy method")
        windscribePresenter.onDestroy()
        super.onDestroy()
    }

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
        intent.putExtra(ERROR_TAG, com.windscribe.vpn.R.string.you_ve_been_banned)
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

    override fun setProtocolAndPortInfo(protocol: String, port: String, disconnected: Boolean) {
        runOnUiThread {
            binding.protocolText.text = protocol
            binding.portText.text = port
        }
    }

    private fun setConnectionStateText(status: VPNState.Status) {
        runOnUiThread {
            when (status) {
                Connecting -> {
                    binding.connectionStatus.background = ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_connecting_status_bg,
                        theme
                    )
                    binding.connectionStatus.text = getString(com.windscribe.vpn.R.string.ON)
                    binding.connectionStatus.setTextColor(resources.getColor(R.color.colorLightBlue))
                    binding.protocolDividerView.setBackgroundColor(resources.getColor(R.color.colorWhite20))
                    binding.protocolText.setTextColor(resources.getColor(R.color.colorLightBlue))
                    binding.portText.setTextColor(resources.getColor(R.color.colorLightBlue))
                }

                Connected -> {
                    binding.connectionStatus.background = ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_connected_status_bg,
                        theme
                    )
                    binding.connectionStatus.text = getString(com.windscribe.vpn.R.string.ON)
                    binding.connectionStatus.setTextColor(resources.getColor(R.color.sea_green))
                    binding.protocolDividerView.setBackgroundColor(resources.getColor(R.color.colorWhite20))
                    binding.protocolText.setTextColor(resources.getColor(R.color.sea_green))
                    binding.portText.setTextColor(resources.getColor(R.color.sea_green))
                }

                Disconnecting, Disconnected -> {
                    binding.connectionStatus.background = ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_disconnected_status_bg,
                        theme
                    )
                    binding.connectionStatus.text = getString(com.windscribe.vpn.R.string.OFF)
                    binding.connectionStatus.setTextColor(resources.getColor(R.color.colorWhite))
                    binding.protocolDividerView.setBackgroundColor(resources.getColor(R.color.colorWhite20))
                    binding.protocolText.setTextColor(resources.getColor(R.color.colorWhite50))
                    binding.portText.setTextColor(resources.getColor(R.color.colorWhite50))
                }

                else -> {}
            }
        }
    }

    override fun setCountryFlag(flagIconResource: Int) {
        if (icon != flagIconResource) {
            onFadeOut(flagIconResource)
        }
        icon = flagIconResource
    }

    // Connect btn
    override fun setGlowVisibility(visibility: Int) {
        runOnUiThread { binding.connectGlow.visibility = visibility }
    }

    override fun setIpAddress(ipAddress: String) {
        binding.ipAddressLabel.text = ipAddress
    }

    override fun setPartialAdapter(serverAdapter: ServerAdapter) {
        binding.partialOverlay.adapter = serverAdapter
    }

    override fun setState(state: Int) {
        this.state = state
    }

    override fun setVpnButtonState() {
        runOnUiThread {
            binding.vpn.let {
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

    override fun setupLayoutConnecting() {
        runOnUiThread {
            setGlowVisibility(View.GONE)
            setConnectionStateText(VPNState.Status.Connecting)
            binding.imgConnected.visibility = View.GONE
            binding.connectionProgressBar.visibility = View.VISIBLE
            state = 1
            setVpnButtonState()
        }
    }

    override fun setupLayoutDisconnected() {
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
            setConnectionStateText(VPNState.Status.Disconnected)
            binding.vpn.clearAnimation()
            state = 0
            setVpnButtonState()
            setGlowVisibility(View.INVISIBLE)
            binding.lockIcon.setImageResource(drawable.ic_ip_none_secure_icon)
            binding.imgConnected.visibility = View.GONE
            binding.connectionProgressBar.visibility = View.GONE
            binding.imgFlagGradientTop.clearColorFilter()
            binding.imgFlagGradientTop.clearAnimation()
        }
    }

    override fun setupLayoutDisconnecting() {
        setGlowVisibility(View.GONE)
        runOnUiThread {
            setConnectionStateText(VPNState.Status.Disconnecting)
            state = 0
        }
    }

    override fun setupLayoutForFreeUser(dataLeft: String, color: Int) {
        binding.dataLeftLabel.text = dataLeft
        binding.dataLeftLabel.setTextColor(color)
        binding.upgradeParent.visibility = View.VISIBLE
    }

    override fun setupLayoutForProUser() {
        binding.upgradeParent.visibility = View.GONE
    }

    override fun showErrorDialog(error: String) {
        ErrorPrimaryFragment.instance.add(error, this, id.cl_windscribe_main, true)
    }

    override fun showPartialViewProgress(inProgress: Boolean) {
        if (inProgress) {
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
        }
    }

    override fun showSplitViewIcon(show: Boolean) {
        if (show) {
            binding.imgConnected.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    drawable.ic_connected_split_ring,
                    theme
                )
            )
        } else {
            binding.imgConnected.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    drawable.ic_connected_ring,
                    theme
                )
            )
        }
    }

    override fun showToast(toastMessage: String) {
        if (toastMessage.isNotEmpty()) {
            Toast.makeText(appContext, toastMessage, Toast.LENGTH_SHORT).show()
        }
    }

    override fun startSessionServiceScheduler() {
        appContext.workManager.updateSession()
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
            setConnectionStateText(VPNState.Status.Connected)
            binding.lockIcon.setImageResource(drawable.ic_ip_secure_icon)
            connectedAnimator = ValueAnimator.ofFloat(0f, 1f)
            connectedAnimator?.let { animator ->
                animator.addUpdateListener {
                    setColorFilter(
                        animator,
                        rgbEvaluator,
                        backgroundColorStart,
                        backgroundColorFinal,
                        binding.imgFlagGradientTop
                    )
                    setTextColor(
                        animator,
                        rgbEvaluator,
                        textColorStart,
                        textColorFinal,
                        arrayOf(binding.connectionStatus, binding.portText, binding.protocolText)
                    )
                }
                animator.addListener(object : AnimatorListener {
                    override fun onAnimationCancel(animation: Animator) {
                        animator.removeAllListeners()
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        animator.removeAllListeners()
                        binding.connectionProgressBar.visibility = View.GONE
                        binding.imgConnected.visibility = View.VISIBLE
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
        runOnUiThread {
            state = 1
            setConnectionStateText(VPNState.Status.Connecting)
            binding.connectionProgressBar.visibility = View.VISIBLE
            binding.flagAlpha.alpha = 0.5f
            connectingAnimator = ValueAnimator.ofFloat(0f, 1f)
            connectingAnimator?.let { it ->
                it.addUpdateListener {
                    setColorFilter(
                        it,
                        rgbEvaluator,
                        backgroundColorStart,
                        backgroundColorFinal,
                        binding.imgFlagGradientTop
                    )
                    setTextColor(
                        it,
                        rgbEvaluator,
                        textColorStart,
                        textColorFinal,
                        arrayOf(binding.connectionStatus, binding.portText, binding.protocolText)
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
        textViews: Array<TextView?>
    ) {
        val color = argbEvaluator?.evaluate(
            valueAnimator.animatedFraction,
            textColorStart,
            textColorFinal
        ) as Int
        textViews.forEach {
            it?.setTextColor(color)
        }
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
        binding.cityName.text = nodeName
        binding.nodeName.text = nodeNickName
        mainLogger.info("Updating location name to:$nodeName")
    }

    private fun onFadeIn() {
        binding.flagAlpha.let { it.animate().alpha(0.5f).setDuration(500).withEndAction {} }
    }

    private fun onFadeOut(flagIconResource: Int) {
        binding.flagAlpha.let {
            it.animate().alpha(0.0f).setDuration(500).withEndAction { setFlag(flagIconResource) }
        }
    }

    private fun setFlag(flagIconResource: Int) {
        binding.flagAlpha.let {
            Glide.with(this@WindscribeActivity)
                .load(ResourcesCompat.getDrawable(resources, flagIconResource, theme))
                .dontAnimate()
                .into(it)
        }
        onFadeIn()
    }

    private fun addClickListeners() {
        binding.upgradeParent.setOnClickListener {
            openUpgradeActivity()
            mainLogger.debug("Upgrade button clicked.")
        }
        binding.vpn.setOnClickListener {
            windscribePresenter.onConnectClicked()
            mainLogger.debug("Connect button clicked.")
        }
        binding.btnSettings.setOnClickListener {
            windscribePresenter.onMenuButtonClicked()
        }
        binding.btnHelp.setOnClickListener {
            startActivity(HelpActivity.getStartIntent(this))
        }
        binding.btnNotifications.setOnClickListener {
            openNewsFeedActivity(false, -1)
            mainLogger.debug("News feed button clicked.")
        }
        binding.vpn.setOnFocusChangeListener { _, _ ->
            if (binding.vpn.hasFocus()) {
                binding.vpn.setImageResource(R.drawable.ic_on_button_off_focused)
            } else {
                binding.vpn.setImageResource(drawable.ic_on_button_off)
            }
        }
    }

    private fun registerDataChangeObserver() {
        activityScope { windscribePresenter.observeVPNState() }
        activityScope { windscribePresenter.observeServerList() }
        activityScope { windscribePresenter.observeSelectedLocation() }
        activityScope { windscribePresenter.observeDisconnectedProtocol() }
        activityScope { windscribePresenter.observeConnectedProtocol() }
        activityScope { windscribePresenter.observeVPNState() }
        activityScope { windscribePresenter.observeNetworkEvents() }
    }

    private fun setFocusListener() {
        binding.clWindscribeMain.setListener(this)
    }

    private fun setViews() {
        setFocusListener()
        windscribePresenter.init()
    }

    private fun startOverlayActivity() {
        val startIntent = Intent(this, OverlayActivity::class.java)
        startActivityForResult(startIntent, overlayStartRequestCode)
        overridePendingTransition(anim.slide_up, anim.slide_down)
    }

    companion object {

        const val ERROR_TAG = "login_error_tag"
        private const val TAG = "basic"

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
