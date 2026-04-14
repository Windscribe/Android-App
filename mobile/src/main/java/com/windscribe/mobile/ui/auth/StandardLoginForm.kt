package com.windscribe.mobile.ui.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.StyledTextField
import com.windscribe.mobile.ui.common.StyledTextFieldWithActions
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.vpn.constants.NetworkKeyConstants

@Composable
fun StandardLoginForm(
    modifier: Modifier = Modifier,
    isUsernameError: Boolean = false,
    isPasswordError: Boolean = false,
    is2FAError: Boolean = false,
    onUsernameChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    on2FAChange: (String) -> Unit = {},
    on2FAInfoClick: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var twoFA by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = modifier) {
        // Username Field Label
        Text(
            text = stringResource(com.windscribe.vpn.R.string.username),
            style = font16.copy(fontWeight = FontWeight.Medium),
            color = if (isUsernameError) AppColors.red else AppColors.white,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Username Field
        StyledTextField(
            value = username,
            onValueChange = {
                username = it
                onUsernameChange(it)
            },
            placeholder = stringResource(com.windscribe.vpn.R.string.enter_username),
            isError = isUsernameError,
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Password Label with Forgot Password Link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(com.windscribe.vpn.R.string.password),
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = if (isPasswordError) AppColors.red else AppColors.white
            )
            Text(
                text = stringResource(com.windscribe.vpn.R.string.forgot_password),
                style = font14.copy(
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline,
                    lineHeight = font14.fontSize * 1.5f
                ),
                color = AppColors.grayText,
                modifier = Modifier.clickable {
                    val url = NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_FORGOT_PASSWORD)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Password Field with visibility toggle
        StyledTextFieldWithActions(
            value = password,
            onValueChange = {
                password = it
                onPasswordChange(it)
            },
            placeholder = stringResource(com.windscribe.vpn.R.string.enter_password),
            isError = isPasswordError,
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2FA - Expandable Section
        ExpandableSection(
            text = stringResource(com.windscribe.vpn.R.string.two_fa)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            StyledTextFieldWithActions(
                value = twoFA,
                onValueChange = {
                    twoFA = it
                    on2FAChange(it)
                },
                placeholder = stringResource(com.windscribe.vpn.R.string.enter_two_fa_code),
                isError = is2FAError,
                showInfoButton = true,
                onInfoClick = on2FAInfoClick,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        }
    }
}

@Composable
private fun ExpandableSection(
    text: String,
    content: @Composable () -> Unit = {}
) {
    val expanded = remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val rotation by animateFloatAsState(
        if (expanded.value) 180f else 0f,
        label = "expandIconRotation"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = AppColors.white
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                OptionalBadge()
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = false, color = Color.White),
                            onClick = { expanded.value = !expanded.value }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_expand),
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotation),
                        colorFilter = ColorFilter.tint(AppColors.white.copy(alpha = 0.50f))
                    )
                }
            }
        }
        if (expanded.value) {
            content()
        }
    }
}

@Composable
private fun OptionalBadge() {
    Box(
        modifier = Modifier
            .background(
                color = AppColors.white.copy(alpha = 0.1f),
                shape = RoundedCornerShape(100.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = stringResource(com.windscribe.vpn.R.string.optional),
            style = font12.copy(fontWeight = FontWeight.Medium),
            color = AppColors.white.copy(alpha = 0.6f)
        )
    }
}
