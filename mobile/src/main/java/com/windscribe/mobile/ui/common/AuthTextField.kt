package com.windscribe.mobile.ui.common

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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16

@Composable
fun AuthTextField(
    modifier: Modifier = Modifier,
    hint: String,
    placeHolder: String? = null,
    isError: Boolean = false,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit = {},
    onHintClick: () -> Unit = {}
) {
    var text by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Hint label
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = hint,
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = if(isError) AppColors.red else AppColors.white,
                modifier = Modifier.clickable { onHintClick() }
            )
            if (isError) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_input_error_icon),
                    contentDescription = stringResource(id = com.windscribe.vpn.R.string.username),
                    tint = Color.Red,
                )
            }
        }

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
                    autoCorrect = false,
                    imeAction = ImeAction.Done
                ),
                placeholder = {
                    if (placeHolder != null) {
                        Text(
                            text = placeHolder,
                            style = font16.copy(fontWeight = FontWeight.Normal),
                            color = AppColors.white.copy(alpha = 0.70f),
                            textAlign = TextAlign.Start)
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = if(isError) AppColors.red else AppColors.white,
                    unfocusedTextColor = if(isError) AppColors.red else AppColors.white,
                    disabledTextColor = if (isError) AppColors.red else AppColors.white,
                    unfocusedContainerColor = AppColors.white.copy(0.05f),
                    focusedContainerColor =  AppColors.white.copy(0.05f),
                    disabledContainerColor =  AppColors.white.copy(0.05f),
                    errorContainerColor =  AppColors.white.copy(0.05f),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    cursorColor = AppColors.white,
                    disabledIndicatorColor = Color.Transparent,
                ),
                visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = {
                    if (isPassword) {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible }
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (passwordVisible) R.drawable.ic_hide_password else R.drawable.ic_show_password
                                ),
                                stringResource(id = com.windscribe.vpn.R.string.password),
                                tint = AppColors.white.copy(alpha = 0.50f)
                            )
                        }
                    }
                },
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
                textStyle = font16.copy(color = if (isError) AppColors.red else AppColors.white, textAlign = TextAlign.Start),
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun AuthTextFieldPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp, start = 16.dp, end = 16.dp)
    ) {
        AuthTextField(hint = "Username", isError = true)
        Spacer(modifier = Modifier.height(16.dp))
        AuthTextField(hint = "Password", isPassword = true)
    }
}
