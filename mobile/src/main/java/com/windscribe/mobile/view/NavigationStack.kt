package com.windscribe.mobile.view

import android.os.Build
import android.util.Log
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
import com.windscribe.mobile.dialogs.AccountStatusDialogData
import com.windscribe.mobile.view.screen.*
import com.windscribe.mobile.viewmodel.*

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
    composable(route = Screen.Start.route) {
        ViewModelRoute(AppStartViewModel::class.java) {
            AppStartScreen(
                null,
                it
            )
        }
    }
    composable(route = Screen.Login.route) {
        ViewModelRoute(LoginViewModel::class.java) {
            LoginScreen(
                null,
                it
            )
        }
    }
    composable(route = Screen.Signup.route) {
        ViewModelRoute(SignupViewModel::class.java) {
            SignupScreen(
                null,
                it
            )
        }
    }
    composable(route = Screen.EmergencyConnect.route) {
        ViewModelRoute(EmergencyConnectViewModal::class.java) {
            EmergencyConnectScreen(
                it
            )
        }
    }
    composable(route = Screen.Home.route) { AddHomeScreenRoute() }
    composable(route = Screen.NoEmailAttention.route) { NoEmailAttentionScreen(false) {} }
    composable(route = Screen.Newsfeed.route) {
        ViewModelRoute(NewsfeedViewmodel::class.java) {
            NewsfeedScreen(
                it
            )
        }
    }
    composable(route = Screen.Web.route) { WebViewScreenUI(LocalNavController.current) }
    composable(route = Screen.PowerWhitelist.route) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ViewModelRoute(PowerWhitelistViewmodel::class.java) { PowerWhitelistScreen(it) }
        }
    }
    composable(route = Screen.ShareLink.route) {
        ViewModelRoute(SharedLinkViewmodel::class.java) {
            ShareLinkScreen(
                it
            )
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