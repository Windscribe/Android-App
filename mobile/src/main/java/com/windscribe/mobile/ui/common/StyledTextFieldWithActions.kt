package com.windscribe.mobile.ui.common

import android.R.attr.end
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16

/**
 * A styled text field component with action buttons on the right side.
 * Supports multiple action buttons like password visibility toggle, refresh, info, etc.
 *
 * States:
 * - Default: Gray border (white 10% opacity), gray placeholder
 * - Active: White border, gray placeholder (focused state)
 * - Error: Red border (#ff7f7f), red text
 *
 * @param modifier Modifier to be applied to the text field
 * @param value Current text value
 * @param onValueChange Callback when text changes
 * @param placeholder Placeholder text to display when empty
 * @param isError Whether the field is in error state
 * @param isPassword Whether this is a password field (uses PasswordVisualTransformation)
 * @param passwordVisible For password fields, controls visibility of text
 * @param onPasswordVisibilityToggle Callback for password visibility toggle
 * @param showRefreshButton Whether to show the refresh/action button
 * @param onRefreshClick Callback for refresh button click
 * @param showInfoButton Whether to show the info button
 * @param onInfoClick Callback for info button click
 * @param keyboardType Type of keyboard to show
 * @param imeAction IME action for the keyboard
 */
@Composable
fun StyledTextFieldWithActions(
    modifier: Modifier = Modifier,
    value: String = "",
    onValueChange: (String) -> Unit = {},
    placeholder: String = "",
    isError: Boolean = false,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityToggle: () -> Unit = {},
    showRefreshButton: Boolean = false,
    onRefreshClick: () -> Unit = {},
    showInfoButton: Boolean = false,
    onInfoClick: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Define colors based on state
    val borderColor = when {
        isError -> Color(0xFFFF7F7F) // Red border for error state
        isFocused -> AppColors.white // White border for active/focused state
        else -> AppColors.white.copy(alpha = 0.1f) // Gray border for default state
    }

    val textColor = if (isError) Color(0xFFFF7F7F) else AppColors.white
    val placeholderColor = if (isError) Color(0xFFFF7F7F) else Color(0xFF898F9D)

    // Calculate number of buttons to determine end padding
    val buttonCount = listOf(isPassword, showRefreshButton, showInfoButton).count { it }
    val endPadding = if (buttonCount > 1) 12.dp else 0.dp

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
            singleLine = true,
            shape = RoundedCornerShape(9.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = imeAction,
                keyboardType = keyboardType
            ),
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = endPadding)
                ) {
                    // Password visibility toggle button
                    if (isPassword) {
                        IconButton(
                            onClick = onPasswordVisibilityToggle,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (passwordVisible) {
                                        R.drawable.ic_eye_off
                                    } else {
                                        R.drawable.ic_eye
                                    }
                                ),
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = AppColors.white.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Refresh/Action button
                    if (showRefreshButton) {
                        if (isPassword) {
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        IconButton(
                            onClick = onRefreshClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_refresh),
                                contentDescription = "Refresh",
                                tint = AppColors.white.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Info button
                    if (showInfoButton) {
                        if (isPassword || showRefreshButton) {
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        IconButton(
                            onClick = onInfoClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_info_circle),
                                contentDescription = "Information",
                                tint = AppColors.white.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
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
                selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                    handleColor = AppColors.white,
                    backgroundColor = AppColors.white.copy(alpha = 0.3f)
                )
            ),
            modifier = modifier
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
@Preview(showBackground = true, backgroundColor = 0xFF090E19)
fun StyledTextFieldWithActionsPreview() {
    var emailText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordWithRefresh by remember { mutableStateOf("MyPassword123") }
    var passwordWithRefreshVisible by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("Wrong") }
    var errorVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.deepBlue)
            .padding(16.dp)
    ) {
        // Email field with info button (like Figma design)
        Text(
            text = "Email with info button",
            style = font16,
            color = AppColors.white,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        StyledTextFieldWithActions(
            value = emailText,
            onValueChange = { emailText = it },
            placeholder = "Enter Email Address",
            showInfoButton = true,
            onInfoClick = {
                // Show info dialog/tooltip
            },
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Password field with visibility toggle
        Text(
            text = "Password with visibility toggle",
            style = font16,
            color = AppColors.white,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        StyledTextFieldWithActions(
            value = passwordText,
            onValueChange = { passwordText = it },
            placeholder = "Enter Password",
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
            keyboardType = KeyboardType.Password
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Password field with both buttons (like Figma design)
        Text(
            text = "Password with visibility + refresh",
            style = font16,
            color = AppColors.white,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        StyledTextFieldWithActions(
            value = passwordWithRefresh,
            onValueChange = { passwordWithRefresh = it },
            placeholder = "Enter Password",
            isPassword = true,
            passwordVisible = passwordWithRefreshVisible,
            onPasswordVisibilityToggle = { passwordWithRefreshVisible = !passwordWithRefreshVisible },
            showRefreshButton = true,
            onRefreshClick = {
                // Generate random password or refresh action
                passwordWithRefresh = "Generated${(1000..9999).random()}"
            },
            keyboardType = KeyboardType.Password
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error state with actions
        Text(
            text = "Error state with actions",
            style = font16,
            color = AppColors.white,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        StyledTextFieldWithActions(
            value = errorText,
            onValueChange = { errorText = it },
            placeholder = "Enter Password",
            isError = true,
            isPassword = true,
            passwordVisible = errorVisible,
            onPasswordVisibilityToggle = { errorVisible = !errorVisible },
            showRefreshButton = true,
            onRefreshClick = { errorText = "" },
            keyboardType = KeyboardType.Password
        )
    }
}
