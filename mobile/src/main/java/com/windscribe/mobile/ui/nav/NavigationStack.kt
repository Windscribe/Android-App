package com.windscribe.mobile.ui.nav

import android.os.Build
import android.util.Log
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.activity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.auth.AppStartScreen
import com.windscribe.mobile.ui.auth.AppStartViewModel
import com.windscribe.mobile.ui.auth.EmergencyConnectScreen
import com.windscribe.mobile.ui.auth.EmergencyConnectViewModal
import com.windscribe.mobile.ui.auth.LoginScreen
import com.windscribe.mobile.ui.auth.LoginViewModel
import com.windscribe.mobile.ui.auth.NoEmailAttentionScreen
import com.windscribe.mobile.ui.auth.SignupScreen
import com.windscribe.mobile.ui.auth.SignupViewModel
import com.windscribe.mobile.ui.connection.AllProtocolFailedScreen
import com.windscribe.mobile.ui.connection.ConnectionChangeScreen
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.connection.DebugLogSentScreen
import com.windscribe.mobile.ui.connection.SetupPreferredProtocolScreen
import com.windscribe.mobile.ui.home.HomeScreen
import com.windscribe.mobile.ui.home.HomeViewmodel
import com.windscribe.mobile.ui.model.AccountStatusDialogData
import com.windscribe.mobile.ui.popup.AccountStatusScreen
import com.windscribe.mobile.ui.popup.AllProtocolFailedDialogScreen
import com.windscribe.mobile.ui.popup.EditCustomConfigScreen
import com.windscribe.mobile.ui.popup.EditCustomConfigViewmodel
import com.windscribe.mobile.ui.popup.ExtraDataUseWarningScreen
import com.windscribe.mobile.ui.popup.LocationUnderMaintenanceScreen
import com.windscribe.mobile.ui.popup.NewsfeedScreen
import com.windscribe.mobile.ui.popup.NewsfeedViewmodel
import com.windscribe.mobile.ui.popup.OverlayDialogScreen
import com.windscribe.mobile.ui.popup.PowerWhitelistScreen
import com.windscribe.mobile.ui.popup.PowerWhitelistViewmodel
import com.windscribe.mobile.ui.popup.ShareLinkScreen
import com.windscribe.mobile.ui.popup.SharedLinkViewmodel
import com.windscribe.mobile.ui.preferences.about.AboutScreen
import com.windscribe.mobile.ui.preferences.account.AccountScreen
import com.windscribe.mobile.ui.preferences.account.AccountViewModel
import com.windscribe.mobile.ui.preferences.advance.AdvanceScreen
import com.windscribe.mobile.ui.preferences.advance.AdvanceViewModel
import com.windscribe.mobile.ui.preferences.connection.ConnectionScreen
import com.windscribe.mobile.ui.preferences.connection.ConnectionViewModel
import com.windscribe.mobile.ui.preferences.debug.DebugScreen
import com.windscribe.mobile.ui.preferences.debug.DebugViewModel
import com.windscribe.mobile.ui.preferences.email.AddEmailScreen
import com.windscribe.mobile.ui.preferences.email.ConfirmEmailScreen
import com.windscribe.mobile.ui.preferences.email.EmailViewModel
import com.windscribe.mobile.ui.preferences.general.GeneralScreen
import com.windscribe.mobile.ui.preferences.general.GeneralViewModel
import com.windscribe.mobile.ui.preferences.gps_spoofing.GpsSpoofing
import com.windscribe.mobile.ui.preferences.help.HelpScreen
import com.windscribe.mobile.ui.preferences.help.HelpViewModel
import com.windscribe.mobile.ui.preferences.lipstick.LipstickViewmodel
import com.windscribe.mobile.ui.preferences.lipstick.LookAndFeelScreen
import com.windscribe.mobile.ui.preferences.main.MainMenuScreen
import com.windscribe.mobile.ui.preferences.main.MainMenuViewModel
import com.windscribe.mobile.ui.preferences.network_details.NetworkDetailScreen
import com.windscribe.mobile.ui.preferences.network_details.NetworkDetailViewModel
import com.windscribe.mobile.ui.preferences.network_options.NetworkOptionsScreen
import com.windscribe.mobile.ui.preferences.network_options.NetworkOptionsViewModel
import com.windscribe.mobile.ui.preferences.robert.RobertScreen
import com.windscribe.mobile.ui.preferences.robert.RobertViewModel
import com.windscribe.mobile.ui.preferences.split_tunnel.SplitTunnelScreen
import com.windscribe.mobile.ui.preferences.split_tunnel.SplitTunnelViewModel
import com.windscribe.mobile.ui.preferences.ticket.TicketScreen
import com.windscribe.mobile.ui.preferences.ticket.TicketViewModel
import com.windscribe.mobile.ui.serverlist.ConfigViewmodel
import com.windscribe.mobile.ui.serverlist.ServerViewModel

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
        ViewModelRoute(AppStartViewModel::class.java) {
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
    composable(route = Screen.EmergencyConnect.route) {
        ViewModelRoute(EmergencyConnectViewModal::class.java) {
            EmergencyConnectScreen(it)
        }
    }
    composable(route = Screen.Home.route) { AddHomeScreenRoute() }
    composable(route = Screen.NoEmailAttention.route) { NoEmailAttentionScreen(false) {} }

    composable(route = Screen.Newsfeed.route, enterTransition = {
        slideInHorizontally(initialOffsetX = { -it })
    }, exitTransition = {
        slideOutHorizontally(targetOffsetX = { it })
    }) {
        val homeViewModel = getViewModel(HomeViewmodel::class.java)
        ViewModelRoute(NewsfeedViewmodel::class.java) {
            NewsfeedScreen(it, homeViewModel)
        }
    }
    composable(route = Screen.MainMenu.route, enterTransition = {
        slideInHorizontally(initialOffsetX = { -it })
    }, exitTransition = {
        slideOutHorizontally(targetOffsetX = { it })
    }) {
        val homeViewModel = getViewModel(HomeViewmodel::class.java)
        ViewModelRoute(MainMenuViewModel::class.java) {
            MainMenuScreen(it, homeViewModel)
        }
    }
    composable(route = Screen.General.route, enterTransition = {
        slideInHorizontally(initialOffsetX = { -it })
    }, exitTransition = {
        slideOutHorizontally(targetOffsetX = { it })
    }) {
        ViewModelRoute(GeneralViewModel::class.java) {
            GeneralScreen(it)
        }
    }
    composable(route = Screen.Account.route, enterTransition = {
        slideInHorizontally(initialOffsetX = { -it })
    }, exitTransition = {
        slideOutHorizontally(targetOffsetX = { it })
    }) {
        ViewModelRoute(AccountViewModel::class.java) {
            AccountScreen(it)
        }
    }
    composable(route = Screen.Connection.route, enterTransition = {
        slideInHorizontally(initialOffsetX = { -it })
    }, exitTransition = {
        slideOutHorizontally(targetOffsetX = { it })
    }) {
        ViewModelRoute(ConnectionViewModel::class.java) {
            ConnectionScreen(it)
        }
    }
    composable(route = Screen.Robert.route, enterTransition = {
        slideInHorizontally(initialOffsetX = { -it })
    }, exitTransition = {
        slideOutHorizontally(targetOffsetX = { it })
    }) {
        ViewModelRoute(RobertViewModel::class.java) {
            RobertScreen(it)
        }
    }
    composable(route = Screen.LookAndFeel.route, enterTransition = {
        slideInHorizontally(initialOffsetX = { -it })
    }, exitTransition = {
        slideOutHorizontally(targetOffsetX = { it })
    }) {
        ViewModelRoute(LipstickViewmodel::class.java) {
            LookAndFeelScreen(it)
        }
    }
    composable(route = Screen.HelpMe.route, enterTransition = {
        slideInHorizontally(initialOffsetX = { -it })
    }, exitTransition = {
        slideOutHorizontally(targetOffsetX = { it })
    }) {
        ViewModelRoute(HelpViewModel::class.java) {
            HelpScreen(it)
        }
    }
    composable(route = Screen.About.route, enterTransition = {
        slideInHorizontally(initialOffsetX = { -it })
    }, exitTransition = {
        slideOutHorizontally(targetOffsetX = { it })
    }) { AboutScreen() }
    composable(route = Screen.PowerWhitelist.route) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ViewModelRoute(PowerWhitelistViewmodel::class.java) { PowerWhitelistScreen(it) }
        }
    }
    composable(route = Screen.Ticket.route, enterTransition = {
        slideInHorizontally(initialOffsetX = { -it })
    }, exitTransition = {
        slideOutHorizontally(targetOffsetX = { it })
    }) {
        ViewModelRoute(TicketViewModel::class.java) {
            TicketScreen(it)
        }
    }
    composable(route = Screen.Advance.route, enterTransition = {
        slideInHorizontally(initialOffsetX = { -it })
    }, exitTransition = {
        slideOutHorizontally(targetOffsetX = { it })
    }) {
        ViewModelRoute(AdvanceViewModel::class.java) {
            AdvanceScreen(it)
        }
    }
    composable(route = Screen.Debug.route, enterTransition = {
        slideInHorizontally(initialOffsetX = { -it })
    }, exitTransition = {
        slideOutHorizontally(targetOffsetX = { it })
    }) {
        ViewModelRoute(DebugViewModel::class.java) {
            DebugScreen(it)
        }
    }
    composable(route = Screen.SplitTunnel.route, enterTransition = {
        slideInHorizontally(initialOffsetX = { -it })
    }, exitTransition = {
        slideOutHorizontally(targetOffsetX = { it })
    }) {
        ViewModelRoute(SplitTunnelViewModel::class.java) {
            SplitTunnelScreen(it)
        }
    }
    composable(route = Screen.NetworkOptions.route, enterTransition = {
        slideInHorizontally(initialOffsetX = { -it })
    }, exitTransition = {
        slideOutHorizontally(targetOffsetX = { it })
    }) {
        ViewModelRoute(NetworkOptionsViewModel::class.java) {
            NetworkOptionsScreen(it)
        }
    }
    composable(route = Screen.NetworkDetails.route, enterTransition = {
        slideInHorizontally(initialOffsetX = { -it })
    }, exitTransition = {
        slideOutHorizontally(targetOffsetX = { it })
    }) {
        ViewModelRoute(NetworkDetailViewModel::class.java) {
            val navController = LocalNavController.current
            val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
            val data = savedStateHandle?.get<String>("network_name")
            if (data != null) {
                LaunchedEffect(data) {
                    it.setNetworkName(data)
                }
                NetworkDetailScreen(it)
            }
        }
    }
    composable(route = Screen.ShareLink.route) {
        ViewModelRoute(SharedLinkViewmodel::class.java) {
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
        val viewModel = getViewModel(EditCustomConfigViewmodel::class.java)
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
        ViewModelRoute(EmailViewModel::class.java) {
            AddEmailScreen(it)
        }
    }
    composable(route = Screen.ConfirmEmail.route) {
        ViewModelRoute(EmailViewModel::class.java) {
            it.emailAdded = true
            ConfirmEmailScreen(it)
        }
    }
}

@Composable
private fun AddHomeScreenRoute() {
    val composeComponent = (LocalContext.current as? AppStartActivity)?.di
    val viewModels = composeComponent?.let {
        ViewModels(
            serverViewModel = viewModel(factory = it.getViewModelFactory()),
            connectionViewModel = viewModel(factory = it.getViewModelFactory()),
            configViewModel = viewModel(factory = it.getViewModelFactory()),
            homeViewModel = viewModel(factory = it.getViewModelFactory())
        )
    }
    Log.i("AppStartViewModel", "Adding home screen.")
    viewModels?.let {
        HomeScreen(it.serverViewModel, it.connectionViewModel, it.configViewModel, it.homeViewModel)
    } ?: HomeScreen(null, null, null, null)
}

@Composable
private inline fun <reified VM : ViewModel> ViewModelRoute(
    viewModelClass: Class<VM>,
    content: @Composable (VM) -> Unit
) {
    val viewModel: VM = getViewModel(viewModelClass)
    content(viewModel)
}

@Composable
private inline fun <reified VM : ViewModel> getViewModel(viewModelClass: Class<VM>): VM {
    val composeComponent = (LocalContext.current as? AppStartActivity)?.di
    return if (composeComponent != null) {
        viewModel(factory = composeComponent.getViewModelFactory())
    } else {
        viewModel(modelClass = viewModelClass)
    }
}

data class ViewModels(
    val serverViewModel: ServerViewModel,
    val connectionViewModel: ConnectionViewmodel,
    val configViewModel: ConfigViewmodel,
    val homeViewModel: HomeViewmodel
)