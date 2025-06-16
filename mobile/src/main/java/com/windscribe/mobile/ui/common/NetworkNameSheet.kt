import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.RequestLocationPermissions
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.connection.NetworkInfoState
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.home.HomeViewmodel
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16

internal enum class PermissionDialogType {
    ForegroundLocation, BackgroundLocation, OpenSettings, None
}

@Composable
fun NetworkNameSheet(connectionViewmodel: ConnectionViewmodel, homeViewmodel: HomeViewmodel) {
    val networkInfo by connectionViewmodel.networkInfoState.collectAsState()
    val hapticEnabled by homeViewmodel.hapticFeedbackEnabled.collectAsState()
    var showPermissionRequest by remember { mutableStateOf(false) }
    val navController = LocalNavController.current
    if (showPermissionRequest) {
        RequestLocationPermissions {
            showPermissionRequest = false
            navController.currentBackStackEntry?.savedStateHandle?.set(
                "network_name",
                networkInfo.name
            )
            navController.navigate(Screen.NetworkDetails.route)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(
                if (networkInfo is NetworkInfoState.Unsecured)
                    R.drawable.ic_wifi_unsecure
                else
                    R.drawable.ic_wifi
            ),
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp)
        )

        val hideIp = remember { mutableStateOf(false) }

        Text(
            text = networkInfo.name ?: stringResource(R.string.unknown),
            style = font16.copy(fontWeight = FontWeight.Medium),
            color = AppColors.white,
            modifier = Modifier
                .alpha(0.7f)
                .padding(start = 4.dp)
                .graphicsLayer {
                    renderEffect = if (hideIp.value) BlurEffect(15f, 15f) else null
                }
                .clickable { hideIp.value = !hideIp.value }
        )

        Image(
            painter = painterResource(R.drawable.arrow_right),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .hapticClickable(hapticEnabled = hapticEnabled) { showPermissionRequest = true },
            contentScale = ContentScale.None
        )
    }
}