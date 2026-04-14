package com.windscribe.mobile.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.common.StyledTextFieldWithUpload
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
    var hashText by remember { mutableStateOf("") }

    // Update hash text when account hash changes (from file upload)
    LaunchedEffect(accountHash) {
        if (accountHash.isNotEmpty()) {
            hashText = accountHash
            onHashValueChange(accountHash)
        }
    }

    Column(modifier = modifier) {
        // Account Hash Label
        Text(
            text = stringResource(com.windscribe.vpn.R.string.account_hash),
            style = font16.copy(fontWeight = FontWeight.Medium),
            color = if (isError) AppColors.red else AppColors.white,
            modifier = Modifier.padding(start = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Account Hash Field with upload button
        StyledTextFieldWithUpload(
            value = hashText,
            onValueChange = {
                hashText = it
                onHashValueChange(it)
            },
            placeholder = stringResource(com.windscribe.vpn.R.string.enter_account_hash_or_upload),
            isError = isError,
            onUploadClick = onUploadClick,
            imeAction = ImeAction.Done
        )
    }
}
