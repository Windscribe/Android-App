/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.windscribe.mobile.R
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.di.DaggerActivityComponent
import com.windscribe.mobile.welcome.WelcomeActivity
import com.windscribe.mobile.windscribe.WindscribeActivity
import com.windscribe.vpn.Windscribe.Companion.appContext
import org.slf4j.LoggerFactory
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity(), SplashView {

    @Inject
    lateinit var presenter: SplashPresenter

    private val logger = LoggerFactory.getLogger("splash_a")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        DaggerActivityComponent.builder().activityModule(ActivityModule(this, this))
                .applicationComponent(
                        appContext
                                .applicationComponent
                ).build().inject(this)
        if (Build.VERSION.SDK_INT >= 23) {
            splashScreen.setKeepOnScreenCondition { true }
        } else {
            setContentView(R.layout.activity_splash)
        }
        logger.info("OnCreate: Splash Activity")
        presenter.checkNewMigration()
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun navigateToAccountSetUp() {
        logger.info("Navigating to account set up activity...")
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.putExtra("startFragmentName", "AccountSetUp")
        intent.putExtra("skipToHome", true)
        startActivity(intent)
        finish()
    }

    override fun navigateToHome() {
        logger.info("Navigating to home activity...")
        val homeIntent = Intent(this, WindscribeActivity::class.java)
        if (intent.extras != null) {
            logger.debug("Forwarding intent extras home activity.")
            homeIntent.putExtras(intent.extras!!)
        }
        startActivity(homeIntent)
        finish()
    }

    override fun navigateToLogin() {
        logger.info("Navigating to login activity...")
        val loginIntent = Intent(this, WelcomeActivity::class.java)
        startActivity(loginIntent)
        finish()
    }
}