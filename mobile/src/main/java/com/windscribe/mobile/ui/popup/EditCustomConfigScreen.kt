package com.windscribe.mobile.ui.popup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16

@Composable
fun EditCustomConfigScreen(viewmodel: EditCustomConfigViewmodel?) {
    val navController = LocalNavController.current
    val shouldExit by viewmodel?.shouldExit?.collectAsState() ?: remember { mutableStateOf(false) }
    LaunchedEffect(shouldExit) {
        if (shouldExit) {
            navController.popBackStack()
        }
    }
    val name by viewmodel?.name?.collectAsState() ?: remember { mutableStateOf("") }
    val username by viewmodel?.username?.collectAsState() ?: remember { mutableStateOf("") }
    val password by viewmodel?.password?.collectAsState() ?: remember { mutableStateOf("") }
    val isOpenVPN by viewmodel?.isOpenVPN?.collectAsState() ?: remember { mutableStateOf(true) }
    val isRemember by viewmodel?.isRemember?.collectAsState() ?: remember { mutableStateOf(false) }
    val connect by viewmodel?.connect?.collectAsState() ?: remember { mutableStateOf(true) }
    val tintColor = if (isRemember) {
        AppColors.neonGreen
    } else {
        AppColors.white
    }
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppColors.deepBlue)
    ) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .fillMaxHeight()
                .verticalScroll(scrollState)
                .imePadding()
                .statusBarsPadding()
                .padding(horizontal = 32.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1.0f))
            Image(
                painter = painterResource(id = R.drawable.ic_network_security_feature_icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(AppColors.white),
            )
            Text(
                text = stringResource(id = com.windscribe.vpn.R.string.edit_config_file),
                fontFamily = FontFamily(Font(com.windscribe.vpn.R.font.ibm_font_family)),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            CustomTextField(
                onValueChange = { viewmodel?.onNameChange(it) },
                modifier = Modifier.fillMaxWidth(),
                hint = stringResource(com.windscribe.vpn.R.string.config_title),
                value = name
            )
            if (isOpenVPN) {
                Spacer(modifier = Modifier.height(16.dp))
                CustomTextField(
                    onValueChange = { viewmodel?.onUsernameChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    hint = stringResource(com.windscribe.vpn.R.string.username),
                    value = username
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomTextField(
                    onValueChange = { viewmodel?.onPasswordChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    hint = stringResource(com.windscribe.vpn.R.string.password),
                    value = password
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Save credentials", style = font16, color = Color.White)
                    Spacer(modifier = Modifier.weight(1.0f))
                    Image(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(tintColor),
                        modifier = Modifier
                            .clickable {
                                viewmodel?.onToggleIsRemember()
                            }
                            .size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
            NextButton(
                text = stringResource(if (connect) com.windscribe.vpn.R.string.connect else com.windscribe.vpn.R.string.update),
                enabled = true,
                onClick = {
                    viewmodel?.onSaveClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
            )
            Spacer(modifier = Modifier.weight(1.0f))
        }
    }
}

@Composable
private fun CustomTextField(
    modifier: Modifier = Modifier,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit = {},
) {
    var text by remember { mutableStateOf(value) }

    // ADD THIS
    LaunchedEffect(value) {
        text = value
    }

    Column(modifier = modifier) {
        Text(
            text = hint,
            style = font16.copy(fontWeight = FontWeight.Medium),
            color = AppColors.white,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        Box {
            TextField(
                value = text,
                onValueChange = {
                    text = it
                    onValueChange(it)
                },
                isError = false,
                singleLine = true,
                shape = RoundedCornerShape(9.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = AppColors.white,
                    unfocusedTextColor = AppColors.white,
                    disabledTextColor = AppColors.white,
                    unfocusedContainerColor =  AppColors.white.copy(0.05f),
                    focusedContainerColor = AppColors.white.copy(0.05f),
                    disabledContainerColor =  AppColors.white.copy(0.05f),
                    errorContainerColor =  AppColors.white.copy(0.05f),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    cursorColor = AppColors.white,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                textStyle = font16.copy(
                    color = AppColors.white,
                    textAlign = TextAlign.Start
                ),
            )
        }
    }
}

@Composable
@MultiDevicePreview
fun EditCustomConfigScreenPreview() {
    PreviewWithNav {
        EditCustomConfigScreen(viewmodel = null)
    }
}