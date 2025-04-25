package com.windscribe.mobile.lipstick

import AppTheme
import android.annotation.SuppressLint
import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.windscribe.mobile.R
import com.windscribe.mobile.di.ComposeComponent
import com.windscribe.mobile.di.DaggerComposeComponent
import com.windscribe.mobile.mainmenu.MainMenuActivity
import com.windscribe.mobile.view.AppStartActivity
import com.windscribe.mobile.view.theme.AndroidTheme
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.view.theme.font24
import com.windscribe.mobile.view.ui.theme
import com.windscribe.mobile.viewmodel.ToastMessage
import com.windscribe.vpn.Windscribe.Companion.appContext

class LipstickActivity : AppCompatActivity() {
    lateinit var di: ComposeComponent
    override fun onCreate(savedInstanceState: Bundle?) {
        val applicationComponent = appContext.applicationComponent
        di = DaggerComposeComponent.builder().applicationComponent(applicationComponent).build()
        setTheme(R.style.DarkTheme)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AndroidTheme {
                val viewModel: LipstickViewmodel = viewModel(factory = di.getViewModelFactory())
                LipstickScreen(viewModel)
            }
        }
    }

   fun reloadApp() {
        TaskStackBuilder.create(this).addNextIntent(Intent(this, AppStartActivity::class.java))
            .addNextIntent(MainMenuActivity.getStartIntent(this))
            .addNextIntentWithParentStack(intent).startActivities()
    }
}

@Composable
private fun LipstickScreen(viewmodel: LipstickViewmodel? = null) {
    val scrollState = rememberScrollState()
    HandleToast(viewmodel)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = theme(R.attr.containerBackground))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            NavBar()
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                AppTheme(viewmodel)
                AppCustomBackground(viewmodel)
                AppCustomSound(viewmodel)
                RenameLocations(viewmodel)
            }
        }
    }
}

@Composable
fun HandleToast(lipstickViewmodel: LipstickViewmodel?) {
    val context = LocalContext.current
    val toastMessage by lipstickViewmodel?.toastMessage?.collectAsState() ?: return
    LaunchedEffect(toastMessage) {
        when (toastMessage) {
            is ToastMessage.Localized -> {
                Toast.makeText(
                    context,
                    (toastMessage as ToastMessage.Localized).message,
                    Toast.LENGTH_SHORT
                ).show()
                lipstickViewmodel?.clearToast()
            }

            is ToastMessage.Raw -> {
                Toast.makeText(
                    context,
                    (toastMessage as ToastMessage.Raw).message,
                    Toast.LENGTH_SHORT
                ).show()
                lipstickViewmodel?.clearToast()
            }

            else -> {}
        }
    }
}

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
private fun NavBar() {
    val activity = LocalContext.current as? LipstickActivity?
    val interactionSource = MutableInteractionSource()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_back_arrow),
            contentDescription = stringResource(id = R.string.image_description),
            colorFilter = ColorFilter.tint(AppColors.white),
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(bounded = false, color = AppColors.white),
                    onClick = {
                        activity?.finish()
                    }
                )
                .padding(4.dp)
                .align(Alignment.CenterStart)
        )

        Text(
            text = stringResource(R.string.look_and_feel),
            style = font24,
            color = AppColors.white,
            maxLines = 1,
            modifier = Modifier
                .padding(horizontal = Dimen.dp8)
                .align(Alignment.Center)
        )
    }
}


@Composable
@Preview(showBackground = true)
private fun LipstickScreenPreview() {
    AndroidTheme {
        LipstickScreen()
    }
}