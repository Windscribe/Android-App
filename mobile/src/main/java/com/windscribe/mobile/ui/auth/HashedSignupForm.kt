package com.windscribe.mobile.ui.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16

@Composable
fun HashedSignupForm(
    modifier: Modifier = Modifier,
    accountHash: String = "",
    isBackupConfirmed: Boolean = false,
    onBackupConfirmedChanged: (Boolean) -> Unit = {},
    onRegenerateHash: () -> Unit = {},
    onUploadHash: () -> Unit = {},
    onDownloadHash: () -> Unit = {},
    onCopyHash: () -> Unit = {},
    onVoucherChange: (String) -> Unit = {},
    onLearnMoreClick: () -> Unit = {}
) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        // Description text
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = stringResource(com.windscribe.vpn.R.string.account_hash_description) + " ",
                style = font16.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = font16.fontSize * 1.5f
                ),
                color = AppColors.grayText,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(com.windscribe.vpn.R.string.learn_more),
                style = font16.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = font16.fontSize * 1.5f,
                    textDecoration = TextDecoration.Underline
                ),
                color = AppColors.grayText,
                modifier = Modifier.clickable { onLearnMoreClick() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Account Hash Display Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppColors.white.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 2.dp,
                    color = AppColors.white,
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    BorderStroke(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(AppColors.white, AppColors.white)
                        )
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = accountHash.ifEmpty { "0x513b5d56ae30109e8ce\na7d7924ef15e0" },
                style = font16.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center
                ),
                color = AppColors.white,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                iconRes = R.drawable.ic_refresh,
                contentDescription = "Regenerate hash",
                onClick = onRegenerateHash
            )
            ActionButton(
                iconRes = R.drawable.ic_upload,
                contentDescription = "Upload hash",
                onClick = onUploadHash
            )
            ActionButton(
                iconRes = R.drawable.ic_download,
                contentDescription = "Download hash",
                onClick = onDownloadHash
            )
            ActionButton(
                iconRes = R.drawable.ic_copy,
                contentDescription = "Copy hash",
                onClick = {
                    onCopyHash()
                    copyToClipboard(context, accountHash)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Voucher Code expandable
        ExpandableSection(
            text = stringResource(com.windscribe.vpn.R.string.got_voucher_code) + " " + stringResource(com.windscribe.vpn.R.string.optional)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Backup confirmation checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppColors.white.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Checkbox(
                checked = isBackupConfirmed,
                onCheckedChange = onBackupConfirmedChanged,
                colors = CheckboxDefaults.colors(
                    checkedColor = AppColors.neonGreen,
                    uncheckedColor = AppColors.white.copy(alpha = 0.5f),
                    checkmarkColor = AppColors.green
                ),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(com.windscribe.vpn.R.string.account_hash_backup_confirmation),
                style = font12.copy(
                    lineHeight = font12.fontSize * 1.5f
                ),
                color = AppColors.white.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ActionButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 79.5.dp, height = 48.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppColors.white.copy(alpha = 0.1f),
                        AppColors.white.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(100.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = AppColors.white.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ExpandableSection(text: String) {
    val expanded = remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val rotation by animateFloatAsState(
        if (expanded.value) 180f else 0f,
        label = "expandIconRotation"
    )

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
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Account Hash", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Account hash copied to clipboard", Toast.LENGTH_SHORT).show()
}
