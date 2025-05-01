package com.windscribe.mobile.lipstick

import AppTheme
import android.annotation.SuppressLint
import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.view.theme.backgroundColor
import com.windscribe.mobile.view.theme.backgroundColorInverted
import com.windscribe.mobile.view.theme.font12
import com.windscribe.mobile.view.theme.font24
import com.windscribe.mobile.view.theme.primaryTextColor
import com.windscribe.mobile.viewmodel.ToastMessage
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.constants.PreferencesKeyConstants.DARK_THEME

class LipstickActivity : AppCompatActivity() {
    lateinit var di: ComposeComponent
    override fun onCreate(savedInstanceState: Bundle?) {
        val applicationComponent = appContext.applicationComponent
        di = DaggerComposeComponent.builder().applicationComponent(applicationComponent).build()
        val isDark = appContext.preference.selectedTheme == DARK_THEME
        if (isDark) {
            setTheme(R.style.DarkTheme)
        } else {
            setTheme(R.style.LightTheme)
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AndroidTheme(isDark) {
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
            .background(color = MaterialTheme.colorScheme.backgroundColor)
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
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor,),
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(bounded = false, color = MaterialTheme.colorScheme.primaryTextColor),
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
            color = MaterialTheme.colorScheme.primaryTextColor,
            maxLines = 1,
            modifier = Modifier
                .padding(horizontal = Dimen.dp8)
                .align(Alignment.Center)
        )
    }
}

@Composable
fun PreferencesBottomSection(@StringRes description: Int) {
    val color = MaterialTheme.colorScheme.backgroundColorInverted.copy(alpha = 0.08f)
    Box(
        modifier = Modifier
            .offset(y = (-16).dp)
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 0.8.dp)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val cornerRadius = 16.dp.toPx()

                // Draw left side
                drawLine(
                    color = color,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height - cornerRadius),
                    strokeWidth = strokeWidth
                )

                // Draw right side
                drawLine(
                    color = color,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height - cornerRadius),
                    strokeWidth = strokeWidth
                )

                // Draw bottom with rounded corners
                val path = Path().apply {
                    moveTo(0f, size.height - cornerRadius)
                    quadraticBezierTo(0f, size.height, cornerRadius, size.height)
                    lineTo(size.width - cornerRadius, size.height)
                    quadraticBezierTo(size.width, size.height, size.width, size.height - cornerRadius)
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = strokeWidth)
                )
            }
    ) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(description),
                modifier = Modifier.padding(12.dp),
                style = font12,
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.5f)
            )
        }
    }
}


@Composable
@Preview(showBackground = true)
private fun LipstickScreenPreview() {
    AndroidTheme {
        LipstickScreen()
    }
}