package com.windscribe.mobile.view.screen

import FeatureSection
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.view.LocalNavController
import com.windscribe.mobile.view.NavigationStack
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.view.theme.font16
import com.windscribe.mobile.view.theme.font18
import com.windscribe.mobile.view.ui.AppProgressBar
import com.windscribe.mobile.viewmodel.AppStartViewModel
import com.windscribe.mobile.viewmodel.LoginState

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppStartScreen(
    windowSizeClass: WindowSizeClass? = currentWindowAdaptiveInfo().windowSizeClass,
    viewModel: AppStartViewModel? = null
) {
    val navController = LocalNavController.current
    val bundle = navController.currentBackStackEntry?.savedStateHandle?.get<Bundle>("result")
    bundle?.let {
        val data = it.getSerializable("data") as? WebViewResult
        if (data != null) {
            Toast.makeText(LocalContext.current, data.toString(), Toast.LENGTH_SHORT).show()
        }
        navController.currentBackStackEntry?.savedStateHandle?.remove<Bundle>("result")
    }
    LaunchedEffect(viewModel?.loggedIn) {
        viewModel?.loggedIn?.let {
            if (it) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Start.route) { inclusive = true }
                }
            }
        }
    }
    val loginState by viewModel?.loginState?.collectAsState() ?: remember {
        mutableStateOf(LoginState.Idle)
    }
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            navController.navigate(Screen.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val isConnected by viewModel?.isConnected?.collectAsState() ?: remember {
        mutableStateOf(false)
    }
    when (windowSizeClass?.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> ExpandedLayout(isConnected, viewModel)
        else -> CompactLayout(isConnected, viewModel)
    }
    if (loginState is LoginState.LoggingIn) {
        val message = (loginState as? LoginState.LoggingIn)?.message ?: ""
        AppProgressBar(true, message = message)
    }
}

@Composable
fun CompactLayout(isConnected: Boolean, viewModel: AppStartViewModel?) {
    Box(
        modifier = Modifier
            .padding(0.dp)
            .fillMaxSize()
            .background(AppColors.deepBlue)
    ) {
        Image(
            painter = painterResource(R.drawable.welcome_background),
            contentDescription = "Welcome Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(1.0f)
                .padding(Dimen.dp32)
                .statusBarsPadding()
        ) {
            Logo()
            Spacer(modifier = Modifier.weight(1f))
            FeatureSection()
            Spacer(modifier = Modifier.weight(1f))
            ActionSection(isConnected, viewModel)
        }
    }
}

@Composable
fun ExpandedLayout(isConnected: Boolean, viewModel: AppStartViewModel?) {
    Box(
        modifier = Modifier
            .padding(0.dp)
            .fillMaxSize()
            .background(AppColors.deepBlue)
    ) {
        Image(
            painter = painterResource(R.drawable.welcome_background),
            contentDescription = "Welcome Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimen.dp32),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Logo()
                Spacer(modifier = Modifier.weight(1f))
                FeatureSection()
                Spacer(modifier = Modifier.weight(1f))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                ActionSection(isConnected, viewModel)
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun isTabletInLandscapeMode(): Boolean {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    return configuration.screenWidthDp >= 900 && isLandscape
}

@Composable
fun Logo() {
    val topPadding = if (isTabletInLandscapeMode()) {
        144.dp
    } else {
        32.dp
    }
    Image(
        painter = painterResource(R.drawable.badge_logo),
        contentDescription = "Badge Logo",
        modifier = Modifier
            .padding(top = topPadding)
            .size(Dimen.dp54)
    )
}

@Composable
private fun GoogleButton(viewModel: AppStartViewModel?) {
    val interactionSource = remember { MutableInteractionSource() }
    val signInIntent = viewModel?.signInIntent
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.i("AppStartViewModel", "result: ${result.data}")
        viewModel?.onSignIntentResult(result.data)
    }
    Button(
        onClick = {
            signInIntent?.let {
                launcher.launch(it)
            }
        },
        Modifier
            .height(Dimen.dp48)
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.white,
            disabledContainerColor = AppColors.white,
            disabledContentColor = AppColors.black54,
            contentColor = AppColors.black54
        ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(Dimen.dp24),
    ) {
        Row {
            Image(
                painter = painterResource(R.drawable.google_logo),
                contentDescription = "Google Logo",
                modifier = Modifier.size(Dimen.dp24)
            )
            Spacer(modifier = Modifier.width(Dimen.dp10))
            Text(
                text = stringResource(R.string.continue_with_google),
                style = font18.copy(fontWeight = FontWeight.Medium),
                color = AppColors.black54,
            )
        }

    }
}

@Composable
private fun AppleButton() {
    val navController = LocalNavController.current
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = {
            navController.currentBackStackEntry?.savedStateHandle?.set("isAccountClaim", false)
            navController.navigate(Screen.Signup.route)
        },
        Modifier
            .height(Dimen.dp48)
            .fillMaxWidth()
            .border(1.dp, AppColors.white25, RoundedCornerShape(Dimen.dp24)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = AppColors.white,
            contentColor = AppColors.white
        ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(Dimen.dp24),
    ) {
        Row {
            Image(
                painter = painterResource(R.drawable.apple_logo),
                contentDescription = "Apple Logo",
                modifier = Modifier.size(Dimen.dp24)
            )
            Spacer(modifier = Modifier.width(Dimen.dp10))
            Text(
                text = stringResource(R.string.continue_with_apple),
                style = font18.copy(fontWeight = FontWeight.Medium),
                color = AppColors.white
            )
        }

    }
}

@Composable
fun EmergencyConnectButton(isConnected: Boolean) {
    val navController = LocalNavController.current
    Text(
        text = if (isConnected) "Emergency Connect On" else "Can’t Connect?",
        style = font18.copy(fontWeight = FontWeight.Medium),
        color = if (isConnected) Color(0xFF61FF8A) else Color(0xFF838D9B),
        modifier = Modifier.clickable {
            navController.navigate(Screen.EmergencyConnect.route)
        }
    )
}

@Composable
private fun LoginButton() {
    val navController = LocalNavController.current
    Text(
        text = stringResource(R.string.login),
        style = font16.copy(fontWeight = FontWeight.Medium),
        color = Color(0xFF838D9B),
        modifier = Modifier.clickable {
            navController.navigate(Screen.Login.route)
        }
    )
}

@Composable
private fun SignupButton() {
    val navController = LocalNavController.current
    Text(
        text = stringResource(R.string.text_sign_up),
        style = font16.copy(fontWeight = FontWeight.Medium),
        color = Color(0xFF838D9B),
        modifier = Modifier.clickable {
            navController.navigate(Screen.Signup.route)
        }
    )
}

@Composable
fun ActionSection(isConnected: Boolean, viewModel: AppStartViewModel?) {
    Column(
        modifier = Modifier
            .widthIn(min = 325.dp, max = 373.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Dimen.dp22),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GoogleButton(viewModel)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmergencyConnectButton(isConnected)
            Spacer(modifier = Modifier.weight(1f))
            SignupButton()
            Spacer(modifier = Modifier.width(8.dp))
            VerticalDivider(Modifier.height(21.dp), color = AppColors.white20, thickness = 1.dp)
            Spacer(modifier = Modifier.width(8.dp))
            LoginButton()
        }
    }
}

@Composable
@Preview(showSystemUi = true)
@PreviewScreenSizes
fun StartScreenPreview() {
    NavigationStack(Screen.Start)
}