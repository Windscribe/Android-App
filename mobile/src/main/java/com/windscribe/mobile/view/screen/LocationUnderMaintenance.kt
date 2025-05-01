package com.windscribe.mobile.view.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.windscribe.mobile.R
import com.windscribe.mobile.view.AppStartActivity
import com.windscribe.mobile.view.LocalNavController
import com.windscribe.mobile.view.theme.backgroundColor
import com.windscribe.mobile.view.theme.font16
import com.windscribe.mobile.view.theme.font24
import com.windscribe.mobile.view.theme.primaryTextColor
import com.windscribe.mobile.view.ui.NextButton
import com.windscribe.mobile.view.ui.openUrl
import com.windscribe.vpn.constants.NetworkKeyConstants


@Composable
fun LocationUnderMaintenanceScreen() {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.backgroundColor)
            .clickable { },
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.weight(1f))
            CenterSection()
            Spacer(Modifier.weight(1f))
            BottomSection()
        }
    }
}

@Composable
private fun CenterSection() {
    Column(
        modifier = Modifier
            .width(400.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.garry_con),
            contentDescription = "Under maintenance icon",
            modifier = Modifier
                .size(100.dp)
                .padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.under_maintenance),
            style = font24,
            color = MaterialTheme.colorScheme.primaryTextColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.check_status_description),
            style = font16,
            color = MaterialTheme.colorScheme.primaryTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun BottomSection() {
    val navController = LocalNavController.current
    val activity = LocalContext.current as? AppStartActivity
    Column(
        modifier = Modifier
            .width(400.dp)
            .padding(16.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NextButton(Modifier, text = stringResource(R.string.check_status), true) {
            activity?.openUrl(NetworkKeyConstants.NODE_STATUS_URL)
            navController.popBackStack()
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primaryTextColor,
            ),
        ) {
            Text(stringResource(R.string.back), style = font16, textAlign = TextAlign.Start)
        }
    }
}

@Composable
private fun LocationUnderMaintenanceScreenPreviewContent() {
    CompositionLocalProvider(
        LocalNavController provides rememberNavController()
    ) {
        LocationUnderMaintenanceScreen()
    }
}

@Preview(
    name = "Light Theme",
    showBackground = true,
    showSystemUi = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Dark Theme",
    showBackground = true,
    showSystemUi = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@PreviewScreenSizes
@Composable
private fun LocationUnderMaintenanceScreenPreview() {
    LocationUnderMaintenanceScreenPreviewContent()
}