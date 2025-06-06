/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import com.windscribe.tv.R
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.welcome.WelcomeActivity
import com.windscribe.tv.windscribe.WindscribeActivity
import org.slf4j.LoggerFactory
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity(), SplashView {
    @Inject
    lateinit var presenter: SplashPresenter
    private val logger = LoggerFactory.getLogger("basic")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_splash)
        logger.info("OnCreate: Splash Activity")
        presenter.checkNewMigration()
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun navigateToHome() {
        logger.info("Navigating to home activity...")
        overridePendingTransition(R.anim.slide_up, R.anim.slide_down)
        val homeIntent = Intent(this, WindscribeActivity::class.java)
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
