package com.windscribe.mobile.ui.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.room.util.TableInfo
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.AppBackground
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.vpn.constants.NetworkKeyConstants

@Composable
fun StandardLoginForm(
    modifier: Modifier = Modifier,
    isUsernameError: Boolean = false,
    isPasswordError: Boolean = false,
    onUsernameChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {}
) {
    Column(modifier = modifier) {
        // Username Field
        StandardTextField(
            label = stringResource(com.windscribe.vpn.R.string.username),
            placeholder = stringResource(com.windscribe.vpn.R.string.enter_username),
            isError = isUsernameError,
            isPassword = false,
            autofillType = ContentType.Username,
            onValueChange = onUsernameChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field with Forgot Password
        PasswordFieldWithForgotLink(
            isError = isPasswordError,
            onValueChange = onPasswordChange
        )
    }
}

@Composable
private fun StandardTextField(
    label: String,
    placeholder: String,
    isError: Boolean,
    isPassword: Boolean,
    autofillType: ContentType? = null,
    onValueChange: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        // Label
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
                    selectionColors = TextSelectionColors(
                        handleColor = AppColors.white,
                        backgroundColor = AppColors.white.copy(alpha = 0.3f)
                    )
                ),
                visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = {
                    if (isPassword) {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible }
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (passwordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
                                ),
                                stringResource(id = com.windscribe.vpn.R.string.password),
                                tint = AppColors.white.copy(alpha = 0.50f)
                            )
                        }
                    }
                },
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
                                contentDataType = ContentDataType.Text
                            }
                        } else if (isPassword) {
                            Modifier.semantics {
                                contentType = ContentType.Password
                                contentDataType = ContentDataType.Text
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
private fun PasswordFieldWithForgotLink(
    isError: Boolean,
    onValueChange: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column {
        // Label with Forgot Password Link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(com.windscribe.vpn.R.string.password),
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = if (isError) AppColors.red else AppColors.white
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isError) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_input_error_icon),
                        contentDescription = stringResource(com.windscribe.vpn.R.string.password),
                        tint = Color.Red,
                    )
                }
                Text(
                    text = stringResource(com.windscribe.vpn.R.string.forgot_password),
                    style = font16.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline
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
        }

        // Password TextField
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
                    keyboardType = KeyboardType.Password
                ),
                placeholder = {
                    Text(
                        text = stringResource(com.windscribe.vpn.R.string.enter_password),
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
                    selectionColors = TextSelectionColors(
                        handleColor = AppColors.white,
                        backgroundColor = AppColors.white.copy(alpha = 0.3f)
                    )
                ),
                visualTransformation = if (!passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible }
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (passwordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
                            ),
                            stringResource(id = com.windscribe.vpn.R.string.password),
                            tint = AppColors.white.copy(alpha = 0.50f)
                        )
                    }
                },
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
                    .semantics {
                        contentType = ContentType.Password
                        contentDataType = ContentDataType.Text
                    },
                textStyle = font16.copy(
                    color = if (isError) AppColors.red else AppColors.white,
                    textAlign = TextAlign.Start
                ),
            )
        }
    }
}

@Composable
@Preview
private fun StandardTextFieldPreview() {
    AppBackground {
        Column(modifier = Modifier.statusBarsPadding().padding(24.dp)) {
            StandardTextField("Password", "Placeholder", false, false, onValueChange = {})
        }
    }
}
