package com.windscribe.mobile.ui.auth

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.StyledTextFieldWithActions
import com.windscribe.mobile.ui.common.StyledTextFieldWithUpload
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16

@Composable
fun HashedLoginForm(
    modifier: Modifier = Modifier,
    accountHash: String = "",
    isError: Boolean = false,
    is2FAError: Boolean = false,
    onHashValueChange: (String) -> Unit = {},
    onUploadClick: () -> Unit = {},
    on2FAChange: (String) -> Unit = {},
    on2FAInfoClick: () -> Unit = {}
) {
    var hashText by remember { mutableStateOf("") }
    var twoFA by remember { mutableStateOf("") }

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
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2FA - Expandable Section
        ExpandableSection(
            text = stringResource(com.windscribe.vpn.R.string.add_two_fa)
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
