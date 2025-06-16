package com.windscribe.mobile.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesBackgroundColor
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor

@Composable
fun SwitchItemView(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    @StringRes description: Int,
    enabled: Boolean,
    explainer: String? = null,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    onSelect: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = shape
            )
            .padding(14.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(title),
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primaryTextColor
            )
            Spacer(modifier = Modifier.weight(1f))
            if (enabled) {
                Image(
                    painter = painterResource(id = R.drawable.ic_toggle_button_on),
                    contentDescription = null,
                    modifier = Modifier.clickable {
                        onSelect(!enabled)
                    }
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_toggle_button_off),
                    contentDescription = null,
                    modifier = Modifier.clickable {
                        onSelect(!enabled)
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(13.5.dp))
        if (explainer != null) {
            DescriptionWithLearnMore(stringResource(description), explainer)
        } else {
            Text(
                text = stringResource(description),
                style = font14.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                textAlign = TextAlign.Start
            )
        }
    }
}

@MultiDevicePreview
@Composable
private fun SwitchItemViewPreview() {
    PreviewWithNav {
        Column(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.preferencesBackgroundColor)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Spacer(Modifier.height(62.dp))
            SwitchItemView(
                title = R.string.app_background,
                icon = R.drawable.ic_apple,
                description = R.string.appearance_description,
                false,
                onSelect = {}
            )
            Spacer(Modifier.height(16.dp))
            SwitchItemView(
                title = R.string.app_background,
                icon = R.drawable.ic_apple,
                description = R.string.appearance_description,
                true,
                onSelect = {}
            )
        }
    }
}