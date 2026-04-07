package com.windscribe.mobile.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16

@Composable
fun StandardSignupForm(
    modifier: Modifier = Modifier,
    isUsernameError: Boolean = false,
    isPasswordError: Boolean = false,
    isEmailError: Boolean = false,
    generatedUsername: String = "",
    generatedPassword: String = "",
    onUsernameChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onVoucherChange: (String) -> Unit = {},
    onReferralUsernameChange: (String) -> Unit = {},
    onGenerateUsername: () -> Unit = {},
    onGeneratePassword: () -> Unit = {}
) {
    Column(modifier = modifier) {
        // Username Field with refresh icon
        SignupTextField(
            label = stringResource(com.windscribe.vpn.R.string.choose_username),
            placeholder = stringResource(com.windscribe.vpn.R.string.enter_username),
            isError = isUsernameError,
            autofillType = ContentType.Username,
            initialValue = generatedUsername,
            trailingIcon = {
                IconButton(onClick = onGenerateUsername) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refresh),
                        contentDescription = stringResource(com.windscribe.vpn.R.string.choose_username),
                        tint = AppColors.white.copy(alpha = 0.50f)
                    )
                }
            },
            onValueChange = onUsernameChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Section
        Column {
            SignupTextField(
                label = stringResource(com.windscribe.vpn.R.string.choose_password),
                placeholder = stringResource(com.windscribe.vpn.R.string.enter_password),
                isError = isPasswordError,
                isPassword = true,
                autofillType = ContentType.Password,
                initialValue = generatedPassword,
                initialPasswordVisible = true,
                trailingIcon = {
                    IconButton(onClick = onGeneratePassword) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = stringResource(com.windscribe.vpn.R.string.choose_password),
                            tint = AppColors.white.copy(alpha = 0.50f)
                        )
                    }
                },
                onValueChange = onPasswordChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            SignupTextField(
                label = null,
                placeholder = stringResource(com.windscribe.vpn.R.string.confirm_password),
                isError = isPasswordError,
                isPassword = true,
                initialPasswordVisible = true,
                onValueChange = {}
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Email Field with info icon
        SignupTextField(
            label = stringResource(com.windscribe.vpn.R.string.add_email) + " " + stringResource(com.windscribe.vpn.R.string.optional),
            placeholder = stringResource(com.windscribe.vpn.R.string.enter_email_address),
            isError = isEmailError,
            trailingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info_icon),
                    contentDescription = stringResource(com.windscribe.vpn.R.string.email),
                    tint = AppColors.white.copy(alpha = 0.50f),
                    modifier = Modifier.size(24.dp)
                )
            },
            onValueChange = onEmailChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Expandable sections
        ExpandableSection(
            text = stringResource(com.windscribe.vpn.R.string.got_voucher_code) + " " + stringResource(com.windscribe.vpn.R.string.optional)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            SignupTextField(
                label = null,
                placeholder = stringResource(com.windscribe.vpn.R.string.voucher_code),
                isError = false,
                onValueChange = onVoucherChange
            )
        }

        ExpandableSection(
            text = stringResource(com.windscribe.vpn.R.string.referred_by_someone) + " " + stringResource(com.windscribe.vpn.R.string.optional)
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                ReferralFeature(stringResource(com.windscribe.vpn.R.string.first_reason_to_use_referral))
                Spacer(modifier = Modifier.height(16.dp))
                ReferralFeature(stringResource(com.windscribe.vpn.R.string.if_you_go_pro_they_ll_go_pro_too))
                Spacer(modifier = Modifier.height(8.dp))
                SignupTextField(
                    label = null,
                    placeholder = stringResource(com.windscribe.vpn.R.string.referral_username),
                    isError = false,
                    onValueChange = onReferralUsernameChange
                )
            }
        }
    }
}

@Composable
private fun ReferralFeature(text: String) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_check),
            contentDescription = text,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text,
            style = font12.copy(textAlign = TextAlign.Start),
            color = AppColors.white.copy(alpha = 0.50f),
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun SignupTextField(
    label: String?,
    placeholder: String,
    isError: Boolean,
    isPassword: Boolean = false,
    autofillType: ContentType? = null,
    initialValue: String = "",
    initialPasswordVisible: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    var passwordVisible by remember { mutableStateOf(initialPasswordVisible) }

    Column {
        // Label
        if (label != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, start = 8.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label,
                    style = font16.copy(fontWeight = FontWeight.Medium),
                    color = if (isError) AppColors.red else AppColors.white
                )
                if (isError) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_input_error_icon),
                        contentDescription = label,
                        tint = Color.Red,
                    )
                }
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
                    keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text
                ),
                placeholder = {
                    Text(
                        text = placeholder,
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
                visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = trailingIcon,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .drawBehind {
                        // Bottom shadow effect: 0px 1px 0px 0px rgba(255,255,255,0.1)
                        drawLine(
                            color = Color.White.copy(alpha = 0.1f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1f
                        )
                    }
                    .then(
                        if (isError) Modifier.border(
                            width = 1.dp,
                            color = AppColors.red,
                            shape = RoundedCornerShape(9.dp)
                        ) else Modifier
                    )
                    .then(
                        if (autofillType != null) {
                            Modifier.semantics {
                                contentType = autofillType
                                contentDataType = androidx.compose.ui.autofill.ContentDataType.Text
                            }
                        } else if (isPassword) {
                            Modifier.semantics {
                                contentType = ContentType.Password
                                contentDataType = androidx.compose.ui.autofill.ContentDataType.Text
                            }
                        } else Modifier
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
private fun ExpandableSection(text: String, content: @Composable () -> Unit = {}) {
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
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = AppColors.white
            )
            Image(
                painter = painterResource(id = R.drawable.ic_expand),
                contentDescription = stringResource(id = com.windscribe.vpn.R.string.image_description),
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = false, color = Color.White),
                        onClick = { expanded.value = !expanded.value }
                    ),
                colorFilter = ColorFilter.tint(AppColors.white.copy(alpha = 0.50f))
            )
        }
        if (expanded.value) {
            content()
        }
    }
}
