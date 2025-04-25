package com.windscribe.mobile.view

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.windscribe.mobile.dialogs.AccountStatusDialogData
import com.windscribe.mobile.view.screen.AccountStatusScreen
import com.windscribe.mobile.view.screen.AppStartScreen
import com.windscribe.mobile.view.screen.EmergencyConnectScreen
import com.windscribe.mobile.view.screen.HomeScreen
import com.windscribe.mobile.view.screen.LoginScreen
import com.windscribe.mobile.view.screen.NewsfeedScreen
import com.windscribe.mobile.view.screen.NoEmailAttentionScreen
import com.windscribe.mobile.view.screen.PowerWhitelistScreen
import com.windscribe.mobile.view.screen.Screen
import com.windscribe.mobile.view.screen.ShareLinkScreen
import com.windscribe.mobile.view.screen.SignupScreen
import com.windscribe.mobile.view.screen.WebViewScreenUI
import com.windscribe.mobile.viewmodel.AppStartViewModel
import com.windscribe.mobile.viewmodel.ConfigViewmodel
import com.windscribe.mobile.viewmodel.ConnectionViewmodel
import com.windscribe.mobile.viewmodel.EmergencyConnectViewModal
import com.windscribe.mobile.viewmodel.HomeViewmodel
import com.windscribe.mobile.viewmodel.LoginViewModel
import com.windscribe.mobile.viewmodel.NewsfeedViewmodel
import com.windscribe.mobile.viewmodel.PowerWhitelistViewmodel
import com.windscribe.mobile.viewmodel.ServerViewModel
import com.windscribe.mobile.viewmodel.SharedLinkViewmodel
import com.windscribe.mobile.viewmodel.SignupViewModel

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("No NavController provided")
}

@Composable
fun NavigationStack(startDestination: Screen) {
    val navController = rememberNavController()
    CompositionLocalProvider(LocalNavController provides navController) {
        NavHost(navController = navController, startDestination = startDestination.route) {
            addNavigationScreens()
        }
    }
}

private fun NavGraphBuilder.addNavigationScreens() {
    composable(route = Screen.Start.route) { AddStartScreenRoute() }
    composable(route = Screen.Login.route) { LoginViewRoute() }
    composable(route = Screen.Signup.route) { SignupViewRoute() }
    composable(route = Screen.EmergencyConnect.route) { EmergencyConnectViewRoute() }
    composable(route = Screen.Home.route) { AddHomeScreenRoute() }
    composable(route = Screen.NoEmailAttention.route) { NoEmailAttentionScreen(false) {} }
    composable(route = Screen.Newsfeed.route) { NewsfeedViewRoute() }
    composable(route = Screen.Web.route) { WebViewScreenUI(LocalNavController.current) }
    composable(route = Screen.PowerWhitelist.route) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerWhitelistViewRoute()
        }
    }
    composable(route = Screen.ShareLink.route) { ShareLinkViewRoute() }
    composable(route = Screen.AccountStatus.route) {
        val navController = LocalNavController.current
        val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
        val data = savedStateHandle?.get<AccountStatusDialogData>("accountStatusDialogData")
        if (data != null) {
            AccountStatusScreen(data)
        }
    }
}

@Composable
private fun AddStartScreenRoute() {
    val composeComponent = (LocalContext.current as? AppStartActivity)?.di
    if (composeComponent == null) {
        AppStartScreen()
    } else {
        val viewModel: AppStartViewModel =
            viewModel(factory = composeComponent.getViewModelFactory())
        AppStartScreen(viewModel = viewModel)
    }
}

@Composable
private fun AddHomeScreenRoute() {
    val composeComponent = (LocalContext.current as? AppStartActivity)?.di
    if (composeComponent == null) {
        HomeScreen(null, null, null, null)
    } else {
        val serverViewModel: ServerViewModel =
            viewModel(factory = composeComponent.getViewModelFactory())
        val connectionViewModel: ConnectionViewmodel =
            viewModel(factory = composeComponent.getViewModelFactory())
        val configViewModel: ConfigViewmodel =
            viewModel(factory = composeComponent.getViewModelFactory())
        val homeViewModel: HomeViewmodel =
            viewModel(factory = composeComponent.getViewModelFactory())
        HomeScreen(serverViewModel, connectionViewModel, configViewModel, homeViewModel)
    }
}

@Composable
private fun EmergencyConnectViewRoute() {
    val composeComponent = (LocalContext.current as? AppStartActivity)?.di
    if (composeComponent == null) {
        EmergencyConnectScreen()
    } else {
        val viewModel: EmergencyConnectViewModal =
            viewModel(factory = composeComponent.getViewModelFactory())
        EmergencyConnectScreen(viewModel)
    }
}

@Composable
private fun LoginViewRoute() {
    val composeComponent = (LocalContext.current as? AppStartActivity)?.di
    if (composeComponent == null) {
        LoginScreen()
    } else {
        val viewModel: LoginViewModel =
            viewModel(factory = composeComponent.getViewModelFactory())
        LoginScreen(null, viewModel)
    }
}

@Composable
private fun SignupViewRoute() {
    val composeComponent = (LocalContext.current as? AppStartActivity)?.di
    if (composeComponent == null) {
        SignupScreen()
    } else {
        val viewModel: SignupViewModel =
            viewModel(factory = composeComponent.getViewModelFactory())
        SignupScreen(null, viewModel)
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
private fun PowerWhitelistViewRoute() {
    val composeComponent = (LocalContext.current as? AppStartActivity)?.di
    if (composeComponent == null) {
        PowerWhitelistScreen(null)
    } else {
        val viewModel: PowerWhitelistViewmodel =
            viewModel(factory = composeComponent.getViewModelFactory())
        PowerWhitelistScreen(viewModel)
    }
}

@Composable
private fun ShareLinkViewRoute() {
    val composeComponent = (LocalContext.current as? AppStartActivity)?.di
    if (composeComponent == null) {
        ShareLinkScreen(null)
    } else {
        val viewModel: SharedLinkViewmodel =
            viewModel(factory = composeComponent.getViewModelFactory())
        ShareLinkScreen(viewModel)
    }
}

@Composable
private fun NewsfeedViewRoute() {
    val composeComponent = (LocalContext.current as? AppStartActivity)?.di
    if (composeComponent == null) {
        NewsfeedScreen()
    } else {
        val viewModel: NewsfeedViewmodel =
            viewModel(factory = composeComponent.getViewModelFactory())
        NewsfeedScreen(viewModel)
    }
}