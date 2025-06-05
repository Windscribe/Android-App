package com.windscribe.mobile.ui.auth

import FeatureSection
import android.content.res.Configuration
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
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.NavigationStack
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font18
import com.windscribe.mobile.ui.common.AppProgressBar
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppStartScreen(
    windowSizeClass: WindowSizeClass? = currentWindowAdaptiveInfo().windowSizeClass,
    viewModel: AppStartViewModel? = null
) {
    val navController = LocalNavController.current
 //   val bundle = navController.currentBackStackEntry?.savedStateHandle?.get<Bundle>("result")
//    bundle?.let {
//        val data = it.getSerializable("data") as? WebViewResult
//        if (data != null) {
//            Toast.makeText(LocalContext.current, data.toString(), Toast.LENGTH_SHORT).show()
//        }
//        navController.currentBackStackEntry?.savedStateHandle?.remove<Bundle>("result")
//    }
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
        if (loginState is SsoLoginState.Success) {
            Log.i("AppStartViewModel", "Logged in successfully.")
            navController.navigate(Screen.Home.route) {
                Log.i("AppStartViewModel", "Routing to home.")
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
    if (loginState is SsoLoginState.LoggingIn) {
        val message = (loginState as? SsoLoginState.LoggingIn)?.message ?: ""
        AppProgressBar(true, message = message)
    }
    val context = LocalContext.current
    LaunchedEffect(loginState) {
        if (loginState is SsoLoginState.Error) {
            val errorMessage = (loginState as SsoLoginState.Error).error
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            viewModel?.clearLoginState()
        }
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
                .padding(32.dp)
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
                .padding(32.dp),
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
            .size(54.dp)
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
    val activity = LocalContext.current as? AppStartActivity
    Button(
        onClick = {
            if (signInIntent == null){
                Toast.makeText(activity, "Google Signin not available.", Toast.LENGTH_SHORT).show()
                return@Button
            }
            launcher.launch(signInIntent)
        },
        Modifier
            .height(48.dp)
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.white,
            disabledContainerColor = AppColors.white,
            disabledContentColor = AppColors.black,
            contentColor = AppColors.black
        ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(24.dp),
    ) {
        Row {
            Image(
                painter = painterResource(R.drawable.google_logo),
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.continue_with_google),
                style = font18.copy(fontWeight = FontWeight.Medium),
                color = AppColors.black,
            )
        }

    }
}

@Composable
private fun LoginButton() {
    val navController = LocalNavController.current
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = {
            navController.currentBackStackEntry?.savedStateHandle?.set("isAccountClaim", false)
            navController.navigate(Screen.Login.route)
        },
        Modifier
            .height(48.dp)
            .fillMaxWidth()
            .border(1.dp, AppColors.white.copy(alpha = 0.30f), RoundedCornerShape(24.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = AppColors.white,
            contentColor = AppColors.white
        ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(24.dp),
    ) {
        Text(
            text = stringResource(R.string.login),
            style = font18.copy(fontWeight = FontWeight.Medium),
            color = AppColors.white
        )
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
        verticalArrangement = Arrangement.spacedBy(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GoogleButton(viewModel)
        LoginButton()
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmergencyConnectButton(isConnected)
            Spacer(modifier = Modifier.weight(1f))
            SignupButton()
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}
@Composable
@MultiDevicePreview
fun StartScreenPreview() {
    PreviewWithNav {
        AppStartScreen()
    }
}