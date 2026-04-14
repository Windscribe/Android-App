package com.windscribe.mobile.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16

enum class AuthType {
    STANDARD,
    HASHED
}

@Composable
fun AuthTabSelector(
    selectedTab: AuthType,
    onTabSelected: (AuthType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = AppColors.white.copy(alpha = 0.05f),
                shape = RoundedCornerShape(100.dp)
            )
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabItem(
            text = stringResource(com.windscribe.vpn.R.string.standard),
            isSelected = selectedTab == AuthType.STANDARD,
            onClick = { onTabSelected(AuthType.STANDARD) }
        )
        TabItem(
            text = stringResource(com.windscribe.vpn.R.string.hashed),
            isSelected = selectedTab == AuthType.HASHED,
            onClick = { onTabSelected(AuthType.HASHED) }
        )
    }
}

@Composable
private fun TabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .then(
                if (isSelected) {
                    Modifier
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(100.dp),
                            spotColor = Color.Black.copy(alpha = 0.25f)
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    AppColors.tabGradientTop,
                                    AppColors.tabGradientBottom
                                )
                            ),
                            shape = RoundedCornerShape(100.dp)
                        )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = font16.copy(
                fontWeight = FontWeight.Medium,
                lineHeight = font16.fontSize * 1.25f
            ),
            color = if (isSelected) AppColors.white else AppColors.grayText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
fun AuthTabSelectorPreview() {
    Box(
        modifier = Modifier
            .background(AppColors.deepBlue)
            .padding(24.dp)
    ) {
        AuthTabSelector(
            selectedTab = AuthType.STANDARD,
            onTabSelected = {}
        )
    }
}
