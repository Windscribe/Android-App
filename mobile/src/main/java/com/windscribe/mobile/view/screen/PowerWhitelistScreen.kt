package com.windscribe.mobile.view.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windscribe.mobile.R
import com.windscribe.mobile.view.LocalNavController
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.font16
import com.windscribe.mobile.view.ui.NextButton
import com.windscribe.mobile.viewmodel.PowerWhitelistViewmodel

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun PowerWhitelistScreen(viewmodel: PowerWhitelistViewmodel?) {
    val navController = LocalNavController.current
    val packageName = LocalContext.current.packageName
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewmodel?.onPermissionResult(it.resultCode == Activity.RESULT_OK)
    }
    val shouldExit by viewmodel?.shouldExit?.collectAsState() ?: remember { mutableStateOf(false) }
    LaunchedEffect(shouldExit) {
        if (shouldExit) {
            navController.popBackStack()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppColors.darkBlue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.battery),
                contentDescription = null,
                modifier = Modifier
                    .size(86.dp)
                    .padding(top = 32.dp)
            )
            Text(
                text = stringResource(id = R.string.power_whitelist_title),
                fontFamily = FontFamily(Font(R.font.ibm_font_family)),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth()
            )
            Text(
                text = stringResource(id = R.string.power_whitelist_summary),
                fontFamily = FontFamily(Font(R.font.ibm_font_family)),
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 550.dp)
            )
            NextButton(
                text = stringResource(R.string.grant_permission), enabled = true, onClick = {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                    filePickerLauncher.launch(intent)
                }, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = {
                viewmodel?.onLaterClicked()
            }) {
                Text(
                    stringResource(id = R.string.may_be_later),
                    style = font16,
                    color = AppColors.white50
                )
            }
            TextButton(onClick = {
                viewmodel?.onNeverAskAgainClicked()
            }) {
                Text(
                    stringResource(id = R.string.never_aks_again_for_permission),
                    style = font16,
                    color = AppColors.white50
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
@Preview
fun PowerWhitelistScreenPreview() {
    PowerWhitelistScreen(null)
}