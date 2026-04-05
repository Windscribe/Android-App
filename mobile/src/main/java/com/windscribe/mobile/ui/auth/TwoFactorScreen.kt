package com.windscribe.mobile.ui.auth

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.AppBackground
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16

@Composable
fun TwoFactorScreen(
    viewModel: LoginViewModel? = null
) {
    val navController = LocalNavController.current

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header with back arrow and tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                navController.popBackStack()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back_arrow),
                            contentDescription = stringResource(com.windscribe.vpn.R.string.back),
                            tint = AppColors.white,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = stringResource(com.windscribe.vpn.R.string.two_fa),
                        style = font16.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.white
                    )
                }

                // Tabs hidden with alpha 0 as per Figma design
                AuthTabSelector(
                    selectedTab = AuthType.STANDARD,
                    onTabSelected = {},
                    modifier = Modifier.alpha(0f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Description text
            Text(
                text = stringResource(com.windscribe.vpn.R.string.two_fa_check_app_description),
                style = font16.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = font16.fontSize * 1.5f,
                    textAlign = TextAlign.Start
                ),
                color = AppColors.grayText,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2FA Code Input
            var twoFactorCode by remember { mutableStateOf("") }
            TwoFactorCodeField(
                isError = false,
                onValueChange = {
                    twoFactorCode = it
                    viewModel?.onTwoFactorChanged(it)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Continue Button
            TwoFactorContinueButton(viewModel, twoFactorCode)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TwoFactorCodeField(
    isError: Boolean,
    onValueChange: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Column {
        // Label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(com.windscribe.vpn.R.string.two_fa),
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = if (isError) AppColors.red else AppColors.white
            )
            if (isError) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_input_error_icon),
                    contentDescription = stringResource(com.windscribe.vpn.R.string.two_fa),
                    tint = Color.Red,
                )
            }
        }

        // Text Field
        Box {
            TextField(
                value = text,
                onValueChange = {
                    text = it
                    onValueChange(it)
                },
                isError = isError,
                singleLine = true,
                shape = RoundedCornerShape(9.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                placeholder = {
                    Text(
                        text = stringResource(com.windscribe.vpn.R.string.enter_two_fa_code),
                        style = font16.copy(fontWeight = FontWeight.Normal),
                        color = AppColors.grayText,
                        textAlign = TextAlign.Start
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = if (isError) AppColors.red else AppColors.white,
                    unfocusedTextColor = if (isError) AppColors.red else AppColors.white,
                    disabledTextColor = if (isError) AppColors.red else AppColors.white,
                    unfocusedContainerColor = AppColors.white.copy(0.05f),
                    focusedContainerColor = AppColors.white.copy(0.05f),
                    disabledContainerColor = AppColors.white.copy(0.05f),
                    errorContainerColor = AppColors.white.copy(0.05f),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    cursorColor = AppColors.white,
                    disabledIndicatorColor = Color.Transparent,
                    selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                        handleColor = AppColors.white,
                        backgroundColor = AppColors.white.copy(alpha = 0.3f)
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .then(
                        if (isError) Modifier.border(
                            width = 1.dp,
                            color = AppColors.red,
                            shape = RoundedCornerShape(9.dp)
                        ) else Modifier
                    ),
                textStyle = font16.copy(
                    color = if (isError) AppColors.red else AppColors.white,
                    textAlign = TextAlign.Start
                ),
            )
        }
    }
}

@Composable
private fun TwoFactorContinueButton(viewModel: LoginViewModel? = null, twoFactorCode: String) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val isButtonEnabled = twoFactorCode.isNotEmpty()

    NextButton(
        text = stringResource(com.windscribe.vpn.R.string.next),
        enabled = isButtonEnabled,
        onClick = {
            keyboardController?.hide()
            viewModel?.loginButtonClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    )
}
