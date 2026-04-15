package com.windscribe.mobile.ui.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.StyledTextField
import com.windscribe.mobile.ui.common.StyledTextFieldWithActions
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16

@Composable
fun StandardSignupForm(
    modifier: Modifier = Modifier,
    generatedUsername: String = "",
    generatedPassword: String = "",
    isUsernameError: Boolean = false,
    isPasswordError: Boolean = false,
    isConfirmPasswordError: Boolean = false,
    isEmailError: Boolean = false,
    onUsernameChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onConfirmPasswordChange: (String) -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onVoucherChange: (String) -> Unit = {},
    onReferralUsernameChange: (String) -> Unit = {},
    onGenerateUsername: () -> Unit = {},
    onGeneratePassword: () -> Unit = {},
    onEmailInfoClick: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var voucher by remember { mutableStateOf("") }
    var referralUsername by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Update username when generated
    LaunchedEffect(generatedUsername) {
        if (generatedUsername.isNotEmpty()) {
            username = generatedUsername
            onUsernameChange(generatedUsername)
        }
    }

    // Update password and confirm password when generated
    LaunchedEffect(generatedPassword) {
        if (generatedPassword.isNotEmpty()) {
            password = generatedPassword
            confirmPassword = generatedPassword
            onPasswordChange(generatedPassword)
            onConfirmPasswordChange(generatedPassword)
        }
    }

    Column(modifier = modifier) {
        // Username Field Label
        FieldLabel(
            text = stringResource(com.windscribe.vpn.R.string.choose_username),
            isError = isUsernameError
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Username Field with refresh icon
        StyledTextFieldWithActions(
            value = username,
            onValueChange = {
                username = it
                onUsernameChange(it)
            },
            placeholder = stringResource(com.windscribe.vpn.R.string.enter_username),
            isError = isUsernameError,
            showRefreshButton = true,
            onRefreshClick = onGenerateUsername,
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Password Section Label
        FieldLabel(
            text = stringResource(com.windscribe.vpn.R.string.choose_password),
            isError = isPasswordError
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Password Field with visibility + refresh
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
            showRefreshButton = true,
            onRefreshClick = onGeneratePassword,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Password Field (with copy button)
        ConfirmPasswordField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                onConfirmPasswordChange(it)
            },
            placeholder = stringResource(com.windscribe.vpn.R.string.confirm_password),
            isError = isConfirmPasswordError,
            passwordVisible = passwordVisible,
            passwordToCopy = password,
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Email Field Label with Optional badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(com.windscribe.vpn.R.string.add_email),
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = if (isEmailError) AppColors.red else AppColors.white
            )
            OptionalBadge()
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Email Field with info icon
        StyledTextFieldWithActions(
            value = email,
            onValueChange = {
                email = it
                onEmailChange(it)
            },
            placeholder = stringResource(com.windscribe.vpn.R.string.enter_email_address),
            isError = isEmailError,
            showInfoButton = true,
            onInfoClick = onEmailInfoClick,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Voucher Code - Expandable Section
        ExpandableSection(
            text = stringResource(com.windscribe.vpn.R.string.got_voucher_code)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            StyledTextField(
                value = voucher,
                onValueChange = {
                    voucher = it
                    onVoucherChange(it)
                },
                placeholder = stringResource(com.windscribe.vpn.R.string.voucher_code),
                imeAction = ImeAction.Next
            )
        }

        // Referral - Expandable Section
        ExpandableSection(
            text = stringResource(com.windscribe.vpn.R.string.referred_by_someone)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            StyledTextField(
                value = referralUsername,
                onValueChange = {
                    referralUsername = it
                    onReferralUsernameChange(it)
                },
                placeholder = stringResource(com.windscribe.vpn.R.string.referral_username),
                imeAction = ImeAction.Done
            )
        }
    }
}

@Composable
private fun FieldLabel(
    text: String,
    isError: Boolean = false
) {
    Text(
        text = text,
        style = font16.copy(fontWeight = FontWeight.Medium),
        color = if (isError) AppColors.red else AppColors.white,
        modifier = Modifier
    )
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

@Composable
private fun ConfirmPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean,
    passwordVisible: Boolean,
    passwordToCopy: String,
    imeAction: ImeAction
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = when {
        isError -> Color(0xFFFF7F7F)
        isFocused -> AppColors.white
        else -> AppColors.white.copy(alpha = 0.1f)
    }

    val textColor = if (isError) Color(0xFFFF7F7F) else AppColors.white
    val placeholderColor = if (isError) Color(0xFFFF7F7F) else Color(0xFF898F9D)

    Box {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = font16,
                    color = placeholderColor,
                    textAlign = TextAlign.Start
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (passwordToCopy.isNotEmpty()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Password", passwordToCopy)
                            clipboard.setPrimaryClip(clip)
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_copy),
                        contentDescription = "Copy password",
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(9.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = imeAction,
                keyboardType = KeyboardType.Password
            ),
            visualTransformation = if (!passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                disabledTextColor = textColor,
                errorTextColor = textColor,
                unfocusedContainerColor = AppColors.white.copy(alpha = 0.05f),
                focusedContainerColor = AppColors.white.copy(alpha = 0.05f),
                disabledContainerColor = AppColors.white.copy(alpha = 0.05f),
                errorContainerColor = AppColors.white.copy(alpha = 0.05f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = AppColors.white,
                selectionColors = TextSelectionColors(
                    handleColor = AppColors.white,
                    backgroundColor = AppColors.white.copy(alpha = 0.3f)
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppColors.white.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(9.dp)
                )
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(9.dp)
                ),
            textStyle = font16.copy(
                color = textColor,
                textAlign = TextAlign.Start,
                lineHeight = 20.sp
            ),
            interactionSource = interactionSource
        )
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
                .padding(vertical = 8.dp),
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
