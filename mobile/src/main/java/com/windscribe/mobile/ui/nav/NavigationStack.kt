package com.windscribe.mobile.ui.nav

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.AppStartActivityViewModel
import com.windscribe.mobile.ui.auth.AppStartScreen
import com.windscribe.mobile.ui.auth.EmergencyConnectScreen
import com.windscribe.mobile.ui.auth.LoginScreen
import com.windscribe.mobile.ui.auth.NoEmailAttentionScreen
import com.windscribe.mobile.ui.auth.SignupScreen
import com.windscribe.mobile.ui.auth.TwoFactorScreen
import com.windscribe.mobile.ui.connection.AllProtocolFailedScreen
import com.windscribe.mobile.ui.connection.ConnectionChangeScreen
import com.windscribe.mobile.ui.connection.DebugLogSentScreen
import com.windscribe.mobile.ui.connection.ManualModeFailedScreen
import com.windscribe.mobile.ui.connection.SetupPreferredProtocolScreen
import com.windscribe.mobile.ui.home.HomeScreen
import com.windscribe.mobile.ui.model.AccountStatusDialogData
import com.windscribe.mobile.ui.popup.AccountStatusScreen
import com.windscribe.mobile.ui.popup.AllProtocolFailedDialogScreen
import com.windscribe.mobile.ui.popup.EditCustomConfigScreen
import com.windscribe.mobile.ui.popup.EditCustomConfigViewmodel
import com.windscribe.mobile.ui.popup.EditCustomConfigViewmodelImpl
import com.windscribe.mobile.ui.popup.ExtraDataUseWarningScreen
import com.windscribe.mobile.ui.popup.IpActionResultDialog
import com.windscribe.mobile.ui.popup.LocationUnderMaintenanceScreen
import com.windscribe.mobile.ui.popup.NewsfeedScreen
import com.windscribe.mobile.ui.popup.OverlayDialogScreen
import com.windscribe.mobile.ui.popup.PowerWhitelistScreen
import com.windscribe.mobile.ui.popup.ShareLinkScreen
import com.windscribe.mobile.ui.popup.UpdateAvailableScreen
import com.windscribe.mobile.ui.preferences.about.AboutScreen
import com.windscribe.mobile.ui.preferences.account.AccountScreen
import com.windscribe.mobile.ui.preferences.advance.AdvanceScreen
import com.windscribe.mobile.ui.preferences.anticensorship.AntiCensorshipScreen
import com.windscribe.mobile.ui.preferences.connection.ConnectionScreen
import com.windscribe.mobile.ui.preferences.debug.DebugScreen
import com.windscribe.mobile.ui.preferences.email.AddEmailScreen
import com.windscribe.mobile.ui.preferences.email.ConfirmEmailScreen
import com.windscribe.mobile.ui.preferences.general.GeneralScreen
import com.windscribe.mobile.ui.preferences.gps_spoofing.GpsSpoofing
import com.windscribe.mobile.ui.preferences.help.HelpScreen
import com.windscribe.mobile.ui.preferences.icons.CustomIconsScreen
import com.windscribe.mobile.ui.preferences.lipstick.LookAndFeelScreen
import com.windscribe.mobile.ui.preferences.main.MainMenuScreen
import com.windscribe.mobile.ui.preferences.network_details.NetworkDetailScreen
import com.windscribe.mobile.ui.preferences.network_options.NetworkOptionsScreen
import com.windscribe.mobile.ui.preferences.robert.RobertScreen
import com.windscribe.mobile.ui.preferences.split_tunnel.SplitTunnelScreen
import com.windscribe.mobile.ui.preferences.ticket.TicketScreen
import com.windscribe.mobile.ui.upgrade.UpgradeScreen
import com.windscribe.mobile.ui.upgrade.UpgradeSuccessScreen

val LocalNavController =
    staticCompositionLocalOf<NavController> {
        error("No NavController provided")
    }

/**
 * The activity-scoped [AppStartActivityViewModel]. Screens that need to share connection
 * state with the Activity must use this instance rather than [hiltViewModel], which would
 * create a separate copy scoped to the NavBackStackEntry.
 */
@Composable
private fun appStartViewModel(): AppStartActivityViewModel = (LocalActivity.current as AppStartActivity).viewmodel

/**
 * The [SavedStateHandle] of the previous back stack entry — i.e. the arguments the screen
 * that navigated here stashed for this destination. Returns null if there is no previous entry.
 */
@Composable
private fun navArgs(): SavedStateHandle? = LocalNavController.current.previousBackStackEntry?.savedStateHandle

@Composable
fun NavigationStack(startDestination: Screen) {
    val navController = rememberNavController()
    val activity = LocalActivity.current as AppStartActivity
    activity.navController = navController
    // Honour deep links the host activity stashed from an intent (e.g. a promo push -> Upgrade).
    // Collected here because the intent is handled in onCreate before this NavHost composes.
    val pendingDeepLink by activity.viewmodel.pendingDeepLinkRoute.collectAsState()
    LaunchedEffect(pendingDeepLink) {
        pendingDeepLink?.let { route ->
            navController.navigate(route)
            activity.viewmodel.clearPendingDeepLink()
        }
    }
    CompositionLocalProvider(LocalNavController provides navController) {
        NavHost(navController = navController, startDestination = startDestination.route) {
            addNavigationScreens()
        }
    }
}

private fun NavGraphBuilder.addNavigationScreens() {
    composable(route = Screen.Start.route) {
        AppStartScreen()
    }
    composable(route = Screen.Login.route) {
        LoginScreen()
    }
    composable(route = Screen.Signup.route) {
        SignupScreen()
    }
    composable(route = Screen.TwoFactor.route) {
        TwoFactorScreen()
    }
    composable(route = Screen.EmergencyConnect.route) {
        EmergencyConnectScreen()
    }
    composable(route = Screen.Home.route) {
        HomeScreen()
    }
    composable(route = Screen.NoEmailAttention.route) {
        NoEmailAttentionScreen(false) {}
    }

    slidingComposable(route = Screen.Newsfeed.route) {
        NewsfeedScreen()
    }
    slidingComposable(route = Screen.MainMenu.route) {
        MainMenuScreen()
    }
    slidingComposable(
        route = Screen.General.route,
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
    ) {
        GeneralScreen()
    }
    slidingComposable(route = Screen.Account.route) {
        AccountScreen()
    }
    slidingComposable(route = Screen.Connection.route) {
        ConnectionScreen()
    }
    slidingComposable(route = Screen.AntiCensorship.route) {
        AntiCensorshipScreen()
    }
    slidingComposable(route = Screen.Robert.route) {
        RobertScreen()
    }
    slidingComposable(route = Screen.LookAndFeel.route) {
        LookAndFeelScreen()
    }
    slidingComposable(route = Screen.HelpMe.route) {
        HelpScreen()
    }
    slidingComposable(route = Screen.About.route) {
        AboutScreen()
    }
    composable(route = Screen.PowerWhitelist.route) {
        PowerWhitelistScreen()
    }
    slidingComposable(route = Screen.Ticket.route) {
        TicketScreen()
    }
    slidingComposable(route = Screen.Advance.route) {
        AdvanceScreen()
    }
    slidingComposable(route = Screen.Debug.route) {
        DebugScreen()
    }
    slidingComposable(route = Screen.SplitTunnel.route) {
        SplitTunnelScreen()
    }
    slidingComposable(route = Screen.NetworkOptions.route) {
        NetworkOptionsScreen()
    }
    slidingComposable(route = Screen.NetworkDetails.route) {
        NetworkDetailScreen()
    }
    slidingComposable(route = Screen.CustomIcon.route) {
        CustomIconsScreen()
    }
    composable(route = Screen.ShareLink.route) {
        ShareLinkScreen()
    }
    composable(route = Screen.AccountStatus.route) {
        val data = navArgs()?.get<AccountStatusDialogData>("accountStatusDialogData")
        data?.let { AccountStatusScreen(it) }
    }
    composable(route = Screen.LocationUnderMaintenance.route) {
        LocationUnderMaintenanceScreen()
    }
    composable(route = Screen.ConnectionChange.route) {
        ConnectionChangeScreen(appStartActivityViewModel = appStartViewModel(), false)
    }
    composable(route = Screen.ConnectionFailure.route) {
        ConnectionChangeScreen(appStartActivityViewModel = appStartViewModel(), true)
    }
    composable(route = Screen.SetupPreferredProtocol.route) {
        SetupPreferredProtocolScreen(appStartActivityViewModel = appStartViewModel())
    }
    composable(route = Screen.DebugLogSent.route) {
        DebugLogSentScreen(appStartActivityViewModel = appStartViewModel())
    }
    composable(route = Screen.AllProtocolFailed.route) {
        AllProtocolFailedScreen(appStartActivityViewModel = appStartViewModel())
    }
    composable(route = Screen.ManualModeFailed.route) {
        ManualModeFailedScreen(appStartActivityViewModel = appStartViewModel())
    }
    composable(route = Screen.OverlayDialog.route) {
        OverlayDialogScreen(appStartActivityViewModel = appStartViewModel())
    }
    composable(route = Screen.AllProtocolFailedDialog.route) {
        AllProtocolFailedDialogScreen()
    }
    composable(route = Screen.ExtraDataUseWarning.route) {
        ExtraDataUseWarningScreen(appStartViewModel())
    }
    composable(route = Screen.GpsSpoofing.route) {
        GpsSpoofing(appStartViewModel())
    }
    composable(route = Screen.AddEmail.route) {
        AddEmailScreen()
    }
    composable(route = Screen.ConfirmEmail.route) {
        ConfirmEmailScreen()
    }
    composable(route = Screen.IpActionResult.route) {
        val savedStateHandle = navArgs()
        val message = savedStateHandle?.get<String>("message")
        val description = savedStateHandle?.get<String>("description")
        if (message != null && description != null) {
            IpActionResultDialog(message, description)
        }
    }
    composable(route = Screen.UpdateAvailable.route) {
        val savedStateHandle = navArgs()
        val latestVersion = savedStateHandle?.get<String?>("latest_version")
        val force = savedStateHandle?.get<Boolean>("force_upgrade") ?: false
        UpdateAvailableScreen(latestVersion, force)
    }
    composable(route = Screen.Upgrade.route) {
        UpgradeScreen()
    }
    composable(route = Screen.UpgradeSuccess.route) {
        val savedStateHandle = navArgs()
        val isGhostAccount = savedStateHandle?.get<Boolean>("isGhostAccount") ?: false
        UpgradeSuccessScreen(isGhostAccount)
    }
    composable(route = Screen.EditCustomConfig.route) {
        val viewModel: EditCustomConfigViewmodel = hiltViewModel<EditCustomConfigViewmodelImpl>()
        val savedStateHandle = navArgs()
        val id = savedStateHandle?.get<Int>("config_id")
        val shouldConnect = savedStateHandle?.get<Boolean>("connect")
        id?.let {
            viewModel.load(id, shouldConnect ?: false)
        }
        EditCustomConfigScreen(viewModel)
    }
}
