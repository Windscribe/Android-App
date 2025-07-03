package com.windscribe.mobile.ui.serverlist

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.home.HomeViewmodel
import com.windscribe.mobile.ui.theme.serverListBackgroundColor
import com.windscribe.mobile.ui.theme.serverListNavigationGradientEnd
import com.windscribe.mobile.ui.theme.serverListSecondaryColor

data class ServerTabIcon(
    val unfilledIcon: Int,
    val filledIcon: Int,
    val type: ServerListType
)

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
fun ServerListNavigation(
    modifier: Modifier,
    viewModel: ServerViewModel,
    homeViewmodel: HomeViewmodel,
    onTabSelected: (Int) -> Unit
) {
    val selectedType by viewModel.selectedServerListType.collectAsState()
    val serverTabs = listOf(
        ServerTabIcon(
            R.drawable.ic_location_all,
            R.drawable.ic_location_all_filled,
            ServerListType.All
        ),
        ServerTabIcon(
            R.drawable.ic_location_fav,
            R.drawable.ic_location_fav_filled,
            ServerListType.Fav
        ),
        ServerTabIcon(
            R.drawable.ic_location_static,
            R.drawable.ic_location_static_filled,
            ServerListType.Static
        ),
        ServerTabIcon(
            R.drawable.ic_location_config,
            R.drawable.ic_location_config_filled,
            ServerListType.Config
        )
    )
    val gradientStart = MaterialTheme.colorScheme.serverListBackgroundColor
    val gradientEnd = MaterialTheme.colorScheme.serverListNavigationGradientEnd
    val borderColor = MaterialTheme.colorScheme.serverListSecondaryColor
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .drawWithContent {
                val cornerRadius = 24.dp.toPx()
                val strokeWidth = 1.dp.toPx()
                val backgroundPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = 0f,
                            top = 0f,
                            right = size.width,
                            bottom = size.height,
                            topLeftCornerRadius = CornerRadius(cornerRadius, cornerRadius),
                            topRightCornerRadius = CornerRadius(cornerRadius, cornerRadius),
                            bottomLeftCornerRadius = CornerRadius.Zero,
                            bottomRightCornerRadius = CornerRadius.Zero
                        )
                    )
                }
                clipPath(backgroundPath) {
                    drawPath(
                        path = backgroundPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(gradientStart, gradientEnd),
                            startY = size.height,
                            endY = 0f
                        )
                    )
                }
                val topBorderPath = Path().apply {
                    addArc(
                        Rect(0f, 0f, cornerRadius * 2, cornerRadius * 2),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 90f
                    )
                    lineTo(size.width - cornerRadius, 0f)
                    addArc(
                        Rect(size.width - 2 * cornerRadius, 0f, size.width, cornerRadius * 2),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = 90f
                    )
                }
                drawPath(
                    path = topBorderPath,
                    color = borderColor.copy(alpha = 0.10f),
                    style = Stroke(width = strokeWidth)
                )
                // Bottom straight border
                drawLine(
                    color = borderColor.copy(alpha = 0.10f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )
                this@drawWithContent.drawContent()
            }
            .padding(horizontal = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            serverTabs.forEachIndexed { index, tab ->
                val isSelected = selectedType == tab.type
                Image(
                    painter = painterResource(if (isSelected) tab.filledIcon else tab.unfilledIcon),
                    contentDescription = null,
                    modifier = Modifier.Companion.clickable {
                        onTabSelected(index)
                    },
                    colorFilter = ColorFilter.tint(
                        if (isSelected) MaterialTheme.colorScheme.serverListSecondaryColor
                        else MaterialTheme.colorScheme.serverListSecondaryColor.copy(alpha = 0.70f)
                    )
                )
                if (index < serverTabs.lastIndex) {
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1.0f))

            Image(
                painter = painterResource(R.drawable.ic_location_search),
                contentDescription = null,
                modifier = Modifier.hapticClickable() {
                    viewModel.toggleSearch()
                },
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.serverListSecondaryColor.copy(alpha = 0.70f))
            )
        }
    }
}