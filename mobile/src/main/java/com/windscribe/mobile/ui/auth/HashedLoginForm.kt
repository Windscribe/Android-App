package com.windscribe.mobile.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16

@Composable
fun HashedLoginForm(
    modifier: Modifier = Modifier,
    accountHash: String = "",
    isError: Boolean = false,
    onHashValueChange: (String) -> Unit = {},
    onUploadClick: () -> Unit = {}
) {
    var hashText by remember(accountHash) { mutableStateOf(accountHash) }

    Column(modifier = modifier) {
        // Label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(com.windscribe.vpn.R.string.account_hash),
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = if (isError) AppColors.red else AppColors.white
            )
            if (isError) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_input_error_icon),
                    contentDescription = stringResource(com.windscribe.vpn.R.string.account_hash),
                    tint = Color.Red,
                )
            }
        }

        // Text Field
        Box {
            TextField(
                value = hashText,
                onValueChange = {
                    hashText = it
                    onHashValueChange(it)
                },
                isError = isError,
                singleLine = true,
                shape = RoundedCornerShape(9.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
                placeholder = {
                    Text(
                        text = stringResource(com.windscribe.vpn.R.string.enter_account_hash_or_upload),
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
                trailingIcon = {
                    IconButton(onClick = onUploadClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_upload),
                            contentDescription = stringResource(com.windscribe.vpn.R.string.enter_account_hash_or_upload),
                            tint = AppColors.white.copy(alpha = 0.50f),
                            modifier = Modifier.size(24.dp)
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
                    ),
                textStyle = font16.copy(
                    color = if (isError) AppColors.red else AppColors.white,
                    textAlign = TextAlign.Start
                ),
            )
        }
    }
}

@Preview
@Composable
fun HashedLoginFormPreview() {
    Box(
        modifier = Modifier
            .background(AppColors.deepBlue)
            .padding(16.dp)
    ) {
        HashedLoginForm()
    }
}
