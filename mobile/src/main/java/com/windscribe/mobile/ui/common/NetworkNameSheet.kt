import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.AppStartActivity
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
    ForegroundLocation, BackgroundLocation, None
}

private fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        locationManager?.isLocationEnabled == true
    } else {
        true
    }
}

@Composable
fun RowScope.NetworkNameSheet(connectionViewmodel: ConnectionViewmodel, homeViewmodel: HomeViewmodel) {
    val activity = LocalContext.current as AppStartActivity
    val networkInfo by connectionViewmodel.networkInfoState.collectAsState()
    var showPermissionRequest by remember { mutableStateOf(false) }
    val navController = LocalNavController.current
    if (showPermissionRequest) {
        RequestLocationPermissions {
            showPermissionRequest = false
            navController.currentBackStackEntry?.savedStateHandle?.set(
                "network_name",
                networkInfo.name
            )
            if (networkInfo is NetworkInfoState.Unknown) {
                if (!isLocationEnabled(activity)) {
                    Toast.makeText(
                        activity,
                        "Enable location services to access network name & restart app.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@RequestLocationPermissions
                }
            } else {
                navController.navigate(Screen.NetworkDetails.route)
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement =Arrangement.Start , modifier = Modifier.weight(1.0f)) {
        Image(
            painter = painterResource(
                if (networkInfo is NetworkInfoState.Unsecured)
                    R.drawable.ic_wifi_unsecure
                else
                    R.drawable.ic_wifi
            ),
            contentDescription = null,
            modifier = Modifier.padding(start = 12.dp)
        )

        val hideNetworkName by homeViewmodel.hideNetworkName.collectAsState()

        Box(
            modifier = Modifier
                .weight(1.0f, fill = false)
                .padding(start = 4.dp)
                .clickable { homeViewmodel.onHideNetworkNameClick() }
        ) {
            Text(
                text = networkInfo.name ?: stringResource(com.windscribe.vpn.R.string.unknown),
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = AppColors.white,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .alpha(0.7f)
                    .graphicsLayer {
                        renderEffect = if (hideNetworkName && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            BlurEffect(15f, 15f)
                        } else null
                    }
            )
            
            // Overlay box for Android 10 and below - only covers the text
            if (hideNetworkName && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            color = AppColors.midnightNavy.copy(alpha = 1.0f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }

        Image(
            painter = painterResource(R.drawable.arrow_right_small),
            contentDescription = null,
            colorFilter = ColorFilter.tint(AppColors.white.copy(alpha = 0.70f)),
            modifier = Modifier
                .size(24.dp)
                .hapticClickable() { showPermissionRequest = true },
            contentScale = ContentScale.None
        )
    }
}