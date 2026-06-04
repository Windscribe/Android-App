/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.ui.upgrade

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.windscribe.mobile.ui.nav.LocalNavController

/**
 * F-Droid flavor entry for the upgrade route. The F-Droid build ships no billing client, so there
 * are no plans to show — mirror the old F-Droid `UpgradeActivity`, which surfaced a
 * "billing unavailable" toast and immediately backed out.
 */
@Composable
fun UpgradeScreen() {
    val context = LocalContext.current
    val navController = LocalNavController.current
    LaunchedEffect(Unit) {
        Toast
            .makeText(context, context.getString(com.windscribe.vpn.R.string.billing_unavailable), Toast.LENGTH_SHORT)
            .show()
        navController.popBackStack()
    }
}
