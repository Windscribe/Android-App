package com.windscribe.mobile.ui.nav

import android.os.Build
import android.util.Log
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.auth.AppStartScreen
import com.windscribe.mobile.ui.auth.AppStartViewModel
import com.windscribe.mobile.ui.auth.AppStartViewModelImpl
import com.windscribe.mobile.ui.auth.EmergencyConnectScreen
import com.windscribe.mobile.ui.auth.EmergencyConnectViewModal
import com.windscribe.mobile.ui.auth.LoginScreen
import com.windscribe.mobile.ui.auth.LoginViewModel
import com.windscribe.mobile.ui.auth.NoEmailAttentionScreen
import com.windscribe.mobile.ui.auth.SignupScreen
import com.windscribe.mobile.ui.auth.SignupViewModel
import com.windscribe.mobile.ui.auth.TwoFactorScreen
import com.windscribe.mobile.ui.connection.AllProtocolFailedScreen
import com.windscribe.mobile.ui.connection.BridgeApiViewModel
import com.windscribe.mobile.ui.connection.BridgeApiViewModelImpl
import com.windscribe.mobile.ui.connection.ConnectionChangeScreen
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.connection.ConnectionViewmodelImpl
import com.windscribe.mobile.ui.connection.DebugLogSentScreen
import com.windscribe.mobile.ui.connection.ManualModeFailedScreen
import com.windscribe.mobile.ui.connection.SetupPreferredProtocolScreen
import com.windscribe.mobile.ui.home.HomeScreen
import com.windscribe.mobile.ui.home.HomeViewmodel
import com.windscribe.mobile.ui.home.HomeViewmodelImpl
import com.windscribe.mobile.ui.model.AccountStatusDialogData
import com.windscribe.mobile.ui.popup.AccountStatusScreen
import com.windscribe.mobile.ui.popup.AllProtocolFailedDialogScreen
import com.windscribe.mobile.ui.popup.EditCustomConfigScreen
import com.windscribe.mobile.ui.popup.EditCustomConfigViewmodel
import com.windscribe.mobile.ui.popup.EditCustomConfigViewmodelImpl
import com.windscribe.mobile.ui.popup.ExtraDataUseWarningScreen
import com.windscribe.mobile.ui.popup.IpActionResultDialog
import com.windscribe.mobile.ui.popup.LocationUnderMaintenanceScreen
import com.windscribe.mobile.ui.popup.UpdateAvailableScreen
import com.windscribe.mobile.ui.popup.NewsfeedScreen
import com.windscribe.mobile.ui.popup.NewsfeedViewmodel
import com.windscribe.mobile.ui.popup.OverlayDialogScreen
import com.windscribe.mobile.ui.popup.PowerWhitelistScreen
import com.windscribe.mobile.ui.popup.PowerWhitelistViewmodel
import com.windscribe.mobile.ui.popup.PowerWhitelistViewmodelImpl
import com.windscribe.mobile.ui.popup.ShareLinkScreen
import com.windscribe.mobile.ui.popup.SharedLinkViewmodel
import com.windscribe.mobile.ui.popup.SharedLinkViewmodelImpl
import com.windscribe.mobile.ui.preferences.about.AboutScreen
import com.windscribe.mobile.ui.preferences.account.AccountScreen
import com.windscribe.mobile.ui.preferences.account.AccountViewModel
import com.windscribe.mobile.ui.preferences.account.AccountViewModelImpl
import com.windscribe.mobile.ui.preferences.advance.AdvanceScreen
import com.windscribe.mobile.ui.preferences.advance.AdvanceViewModel
import com.windscribe.mobile.ui.preferences.advance.AdvanceViewModelImpl
import com.windscribe.mobile.ui.preferences.anticensorship.AntiCensorshipViewModelImpl
import com.windscribe.mobile.ui.preferences.connection.ConnectionScreen
import com.windscribe.mobile.ui.preferences.connection.ConnectionViewModel
import com.windscribe.mobile.ui.preferences.connection.ConnectionViewModelImpl
import com.windscribe.mobile.ui.preferences.debug.DebugScreen
import com.windscribe.mobile.ui.preferences.debug.DebugViewModel
import com.windscribe.mobile.ui.preferences.debug.DebugViewModelImpl
import com.windscribe.mobile.ui.preferences.email.AddEmailScreen
import com.windscribe.mobile.ui.preferences.email.ConfirmEmailScreen
import com.windscribe.mobile.ui.preferences.email.EmailViewModel
import com.windscribe.mobile.ui.preferences.email.EmailViewModelImpl
import com.windscribe.mobile.ui.preferences.general.GeneralScreen
import com.windscribe.mobile.ui.preferences.general.GeneralViewModel
import com.windscribe.mobile.ui.preferences.general.GeneralViewModelImpl
import com.windscribe.mobile.ui.preferences.gps_spoofing.GpsSpoofing
import com.windscribe.mobile.ui.preferences.help.HelpScreen
import com.windscribe.mobile.ui.preferences.help.HelpViewModel
import com.windscribe.mobile.ui.preferences.help.HelpViewModelImpl
import com.windscribe.mobile.ui.preferences.icons.CustomIconsScreen
import com.windscribe.mobile.ui.preferences.icons.CustomIconsViewModel
import com.windscribe.mobile.ui.preferences.icons.CustomIconsViewModelImpl
import com.windscribe.mobile.ui.preferences.lipstick.LipstickViewmodel
import com.windscribe.mobile.ui.preferences.lipstick.LipstickViewmodelImpl
import com.windscribe.mobile.ui.preferences.lipstick.LookAndFeelScreen
import com.windscribe.mobile.ui.preferences.main.MainMenuScreen
import com.windscribe.mobile.ui.preferences.main.MainMenuViewModel
import com.windscribe.mobile.ui.preferences.main.MainMenuViewModelImpl
import com.windscribe.mobile.ui.preferences.network_details.NetworkDetailScreen
import com.windscribe.mobile.ui.preferences.network_details.NetworkDetailViewModel
import com.windscribe.mobile.ui.preferences.network_details.NetworkDetailViewModelImpl
import com.windscribe.mobile.ui.preferences.network_options.NetworkOptionsScreen
import com.windscribe.mobile.ui.preferences.network_options.NetworkOptionsViewModel
import com.windscribe.mobile.ui.preferences.network_options.NetworkOptionsViewModelImpl
import com.windscribe.mobile.ui.preferences.robert.RobertScreen
import com.windscribe.mobile.ui.preferences.robert.RobertViewModel
import com.windscribe.mobile.ui.preferences.robert.RobertViewModelImpl
import com.windscribe.mobile.ui.preferences.split_tunnel.SplitTunnelScreen
import com.windscribe.mobile.ui.preferences.split_tunnel.SplitTunnelViewModel
import com.windscribe.mobile.ui.preferences.split_tunnel.SplitTunnelViewModelImpl
import com.windscribe.mobile.ui.preferences.ticket.TicketScreen
import com.windscribe.mobile.ui.preferences.ticket.TicketViewModel
import com.windscribe.mobile.ui.preferences.ticket.TicketViewModelImpl
import com.windscribe.mobile.ui.serverlist.ConfigViewmodel
import com.windscribe.mobile.ui.serverlist.ConfigViewmodelImpl
import com.windscribe.mobile.ui.serverlist.ServerViewModel
import com.windscribe.mobile.ui.serverlist.ServerViewModelImpl

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("No NavController provided")
}

@Composable
fun NavigationStack(startDestination: Screen) {
    val navController = rememberNavController()
    val activity = LocalContext.current as AppStartActivity
    activity.navController = navController
    CompositionLocalProvider(LocalNavController provides navController) {
        NavHost(navController = navController, startDestination = startDestination.route) {
            addNavigationScreens()
        }
    }
}

private fun NavGraphBuilder.addNavigationScreens() {
    composable(route = Screen.Start.route) {
        ViewModelRoute(AppStartViewModelImpl::class.java) {
            AppStartScreen(null, it)
        }
    }
    composable(route = Screen.Login.route) {
        ViewModelRoute(LoginViewModel::class.java) {
            LoginScreen(null, it)
        }
    }
    composable(route = Screen.Signup.route) {
        ViewModelRoute(SignupViewModel::class.java) {
            SignupScreen(null, it)
        }
    }
    composable(route = Screen.TwoFactor.route) {
        ViewModelRoute(LoginViewModel::class.java) {
            TwoFactorScreen(it)
        }
    }
    composable(route = Screen.EmergencyConnect.route) {
        ViewModelRoute(EmergencyConnectViewModal::class.java) {
            EmergencyConnectScreen(it)
        }
    }
    composable(route = Screen.Home.route) { AddHomeScreenRoute() }
    composable(route = Screen.NoEmailAttention.route) { NoEmailAttentionScreen(false) {} }

    composable(route = Screen.Newsfeed.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        val homeViewModel: HomeViewmodel = getViewModel(HomeViewmodelImpl::class.java)
        ViewModelRoute(NewsfeedViewmodel::class.java) {
            NewsfeedScreen(it, homeViewModel)
        }
    }
    composable(route = Screen.MainMenu.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        val homeViewModel: HomeViewmodel = getViewModel(HomeViewmodelImpl::class.java)
        ViewModelRoute(MainMenuViewModelImpl::class.java) {
            MainMenuScreen(it, homeViewModel)
        }
    }
    composable(route = Screen.General.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        ViewModelRoute(GeneralViewModelImpl::class.java) {
            GeneralScreen(it)
        }
    }
    composable(route = Screen.Account.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        ViewModelRoute(AccountViewModelImpl::class.java) {
            AccountScreen(it)
        }
    }
    composable(route = Screen.Connection.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        ViewModelRoute(ConnectionViewModelImpl::class.java) {
            ConnectionScreen(it)
        }
    }
    composable(route = Screen.AntiCensorship.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        ViewModelRoute(AntiCensorshipViewModelImpl::class.java) {
            com.windscribe.mobile.ui.preferences.anticensorship.AntiCensorshipScreen(it)
        }
    }
    composable(route = Screen.Robert.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        ViewModelRoute(RobertViewModelImpl::class.java) {
            RobertScreen(it)
        }
    }
    composable(route = Screen.LookAndFeel.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        ViewModelRoute(LipstickViewmodelImpl::class.java) {
            LookAndFeelScreen(it)
        }
    }
    composable(route = Screen.HelpMe.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        ViewModelRoute(HelpViewModelImpl::class.java) {
            HelpScreen(it)
        }
    }
    composable(route = Screen.About.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) { AboutScreen() }
    composable(route = Screen.PowerWhitelist.route) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ViewModelRoute(PowerWhitelistViewmodelImpl::class.java) { PowerWhitelistScreen(it) }
        }
    }
    composable(route = Screen.Ticket.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        ViewModelRoute(TicketViewModelImpl::class.java) {
            TicketScreen(it)
        }
    }
    composable(route = Screen.Advance.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        ViewModelRoute(AdvanceViewModelImpl::class.java) {
            AdvanceScreen(it)
        }
    }
    composable(route = Screen.Debug.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        ViewModelRoute(DebugViewModelImpl::class.java) {
            DebugScreen(it)
        }
    }
    composable(route = Screen.SplitTunnel.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        ViewModelRoute(SplitTunnelViewModelImpl::class.java) {
            SplitTunnelScreen(it)
        }
    }
    composable(route = Screen.NetworkOptions.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        ViewModelRoute(NetworkOptionsViewModelImpl::class.java) {
            NetworkOptionsScreen(it)
        }
    }
    composable(route = Screen.NetworkDetails.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        val navController = LocalNavController.current
        val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
        val data = savedStateHandle?.get<String>("network_name")
        ViewModelRoute(NetworkDetailViewModelImpl::class.java) {
            data?.let { networkName ->
                it.setNetworkName(networkName)
            }
            NetworkDetailScreen(it)
        }
    }
    composable(route = Screen.CustomIcon.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { -it })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { it })
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { it })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it })
        }
    ) {
        ViewModelRoute(CustomIconsViewModelImpl::class.java) {
            CustomIconsScreen(it)
        }
    }
    composable(route = Screen.ShareLink.route) {
        ViewModelRoute(SharedLinkViewmodelImpl::class.java) {
            ShareLinkScreen(it)
        }
    }
    composable(route = Screen.AccountStatus.route) {
        val navController = LocalNavController.current
        val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
        val data = savedStateHandle?.get<AccountStatusDialogData>("accountStatusDialogData")
        data?.let { AccountStatusScreen(it) }
    }
    composable(route = Screen.LocationUnderMaintenance.route) { LocationUnderMaintenanceScreen() }
    composable(route = Screen.EditCustomConfig.route) {
        val viewModel: EditCustomConfigViewmodel = getViewModel(EditCustomConfigViewmodelImpl::class.java)
        val navController = LocalNavController.current
        val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
        val id = savedStateHandle?.get<Int>("config_id")
        val shouldConnect = savedStateHandle?.get<Boolean>("connect")
        id?.let {
            viewModel.load(id, shouldConnect ?: false)
        }
        EditCustomConfigScreen(viewModel)
    }
    composable(route = Screen.ConnectionChange.route) {
        val activity = LocalContext.current as AppStartActivity
        ConnectionChangeScreen(appStartActivityViewModel = activity.viewmodel, false)
    }
    composable(route = Screen.ConnectionFailure.route) {
        val activity = LocalContext.current as AppStartActivity
        ConnectionChangeScreen(appStartActivityViewModel = activity.viewmodel, true)
    }
    composable(route = Screen.SetupPreferredProtocol.route) {
        val activity = LocalContext.current as AppStartActivity
        SetupPreferredProtocolScreen(appStartActivityViewModel = activity.viewmodel)
    }
    composable(route = Screen.DebugLogSent.route) {
        val activity = LocalContext.current as AppStartActivity
        DebugLogSentScreen(appStartActivityViewModel = activity.viewmodel)
    }
    composable(route = Screen.AllProtocolFailed.route) {
        val activity = LocalContext.current as AppStartActivity
        AllProtocolFailedScreen(appStartActivityViewModel = activity.viewmodel)
    }
    composable(route = Screen.ManualModeFailed.route) {
        val activity = LocalContext.current as AppStartActivity
        ManualModeFailedScreen(appStartActivityViewModel = activity.viewmodel)
    }
    composable(route = Screen.OverlayDialog.route) {
        val activity = LocalContext.current as AppStartActivity
        OverlayDialogScreen(appStartActivityViewModel = activity.viewmodel)
    }
    composable(route = Screen.AllProtocolFailedDialog.route) {
        AllProtocolFailedDialogScreen()
    }
    composable(route = Screen.ExtraDataUseWarning.route) {
        val activity = LocalContext.current as AppStartActivity
        ExtraDataUseWarningScreen(activity.viewmodel)
    }
    composable(route = Screen.GpsSpoofing.route) {
        val activity = LocalContext.current as AppStartActivity
        GpsSpoofing(activity.viewmodel)
    }
    composable(route = Screen.AddEmail.route) {
        ViewModelRoute(EmailViewModelImpl::class.java) {
            AddEmailScreen(it)
        }
    }
    composable(route = Screen.ConfirmEmail.route) {
        ViewModelRoute(EmailViewModelImpl::class.java) {
            it.emailAdded = true
            ConfirmEmailScreen(it)
        }
    }
    composable(route = Screen.IpActionResult.route) {
        val navController = LocalNavController.current
        val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
        val message = savedStateHandle?.get<String>("message")
        val description = savedStateHandle?.get<String>("description")
        if (message != null && description != null) {
            IpActionResultDialog(message, description)
        }
    }
    composable(route = Screen.UpdateAvailable.route) {
        val navController = LocalNavController.current
        val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
        val latestVersion = savedStateHandle?.get<String?>("latest_version")
        val force = savedStateHandle?.get<Boolean>("force_upgrade") ?: false
        UpdateAvailableScreen(latestVersion, force)
    }
}

@Composable
private fun AddHomeScreenRoute() {
    val serverViewModel: ServerViewModel = androidx.hilt.navigation.compose.hiltViewModel<ServerViewModelImpl>()
    val connectionViewModel: ConnectionViewmodel = androidx.hilt.navigation.compose.hiltViewModel<ConnectionViewmodelImpl>()
    val configViewModel: ConfigViewmodel = androidx.hilt.navigation.compose.hiltViewModel<ConfigViewmodelImpl>()
    val homeViewModel: HomeViewmodel = androidx.hilt.navigation.compose.hiltViewModel<HomeViewmodelImpl>()
    val bridgeApiViewModel: BridgeApiViewModel = androidx.hilt.navigation.compose.hiltViewModel<BridgeApiViewModelImpl>()
    Log.i("AppStartViewModel", "Adding home screen.")
    HomeScreen(serverViewModel, connectionViewModel, configViewModel, homeViewModel, bridgeApiViewModel)
}

@Composable
private inline fun <reified VM : ViewModel> ViewModelRoute(
    @Suppress("UNUSED_PARAMETER") viewModelClass: Class<VM>,
    content: @Composable (VM) -> Unit
) {
    val viewModel: VM = androidx.hilt.navigation.compose.hiltViewModel()
    content(viewModel)
}

@Composable
private inline fun <reified VM : ViewModel> getViewModel(
    @Suppress("UNUSED_PARAMETER") viewModelClass: Class<VM>
): VM = androidx.hilt.navigation.compose.hiltViewModel()

data class ViewModels(
    val serverViewModel: ServerViewModel,
    val connectionViewModel: ConnectionViewmodel,
    val configViewModel: ConfigViewmodel,
    val homeViewModel: HomeViewmodel,
    val bridgeApiViewModel: BridgeApiViewModel
)