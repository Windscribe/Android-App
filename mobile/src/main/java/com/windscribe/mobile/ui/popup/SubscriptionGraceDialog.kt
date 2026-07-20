package com.windscribe.mobile.ui.popup

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesBackgroundColor
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R

@Composable
fun SubscriptionGraceDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        shape = RoundedCornerShape(16.dp),
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.preferencesBackgroundColor,
        modifier =
            Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
            ),
        title = {
            Text(
                text = stringResource(R.string.subscription_payment_issue),
                style = font16.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primaryTextColor,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.subscription_payment_issue_description),
                style = font12,
                color = MaterialTheme.colorScheme.primaryTextColor,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.ok),
                    style = font16.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primaryTextColor,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = font16,
                    color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                )
            }
        },
    )
}

@MultiDevicePreview
@Composable
private fun SubscriptionGraceDialogPreview() {
    PreviewWithNav {
        SubscriptionGraceDialog({}, {})
    }
}
