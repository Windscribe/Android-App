/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.ui.upgrade

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.vpn.Windscribe.Companion.appContext

/**
 * Google-flavor stateful entry for the upgrade route. Owns [UpgradeViewModel], registers the
 * correct billing manager (Google vs Amazon, decided by the install source), then renders the
 * shared, stateless [UpgradeContent].
 */
@Composable
fun UpgradeScreen(viewModel: UpgradeViewModel = hiltViewModel()) {
    val activity = LocalActivity.current as AppCompatActivity
    val context = LocalContext.current
    val navController = LocalNavController.current
    // The destination's own lifecycle (the NavBackStackEntry), NOT the activity's. The billing
    // client must connect when this screen opens and end its connection when it closes; binding to
    // the activity would keep one stale connection alive across visits, so a second visit would
    // never get a fresh onBillingSetUpSuccess and would hang on the loading state.
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Register the matching billing manager on the destination lifecycle for the duration of this
    // screen. GoogleBillingManager.onCreate opens the connection (emitting onBillingSetUpSuccess)
    // and onDestroy ends it — so connect/disconnect track each visit, exactly like the old Activity.
    DisposableEffect(lifecycleOwner) {
        val billingType = resolveBillingType(activity)
        val manager =
            if (billingType == UpgradeViewModel.BillingType.Amazon) {
                viewModel.amazonBillingManager
            } else {
                viewModel.googleBillingManager
            }
        viewModel.start(billingType, appContext.appLifeCycleObserver.pushNotificationAction)
        lifecycleOwner.lifecycle.addObserver(manager)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(manager)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UpgradeEvent.Toast ->
                    android.widget.Toast
                        .makeText(context, event.message, android.widget.Toast.LENGTH_SHORT)
                        .show()
                is UpgradeEvent.OpenUrl -> {
                    runCatching {
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, event.url.toUri()))
                    }
                }
                is UpgradeEvent.Success -> {
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("isGhostAccount", event.isGhostAccount)
                    navController.navigate(Screen.UpgradeSuccess.route) {
                        popUpTo(Screen.Upgrade.route) { inclusive = true }
                    }
                }
                is UpgradeEvent.Close -> navController.popBackStack()
            }
        }
    }

    val actions =
        remember(viewModel) {
            UpgradeActions(
                onClose = { navController.popBackStack() },
                onRestore = viewModel::restorePurchase,
                onSelectMonthly = viewModel::selectMonthly,
                onSelectYearly = viewModel::selectYearly,
                onSubscribe = {
                    viewModel.subscribe { params -> viewModel.googleBillingManager.launchBillingFlow(activity, params) }
                },
                onTermsClick = viewModel::onTermsClick,
                onPrivacyClick = viewModel::onPrivacyClick,
                onDismissError = viewModel::dismissError,
            )
        }

    UpgradeContent(state = state, actions = actions)
}

private fun resolveBillingType(activity: AppCompatActivity): UpgradeViewModel.BillingType {
    val installer =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.packageManager.getInstallSourceInfo(activity.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            activity.packageManager.getInstallerPackageName(activity.packageName)
        }
    return if (installer != null && installer.startsWith("com.amazon")) {
        UpgradeViewModel.BillingType.Amazon
    } else {
        UpgradeViewModel.BillingType.Google
    }
}
