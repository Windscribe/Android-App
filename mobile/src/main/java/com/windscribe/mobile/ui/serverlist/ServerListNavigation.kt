package com.windscribe.mobile.ui.serverlist

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.home.HomeViewmodel

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
    val isHapticEnabled by homeViewmodel.hapticFeedbackEnabled.collectAsState()
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
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = Color(0x1AFFFFFF),
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            )
            .fillMaxWidth()
            .height(54.dp)
            .drawWithContent {
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = 0f,
                            top = 0f,
                            right = size.width,
                            bottom = size.height,
                            topLeftCornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                            topRightCornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                            bottomLeftCornerRadius = CornerRadius.Zero,
                            bottomRightCornerRadius = CornerRadius.Zero
                        )
                    )
                }

                clipRect {
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF050A11), Color(0x4D050A11)),
                            startY = size.height,
                            endY = 0f
                        )
                    )
                }
                drawContent()
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
                    modifier = Modifier.Companion.hapticClickable(hapticEnabled = isHapticEnabled) {
                        onTabSelected(index)
                    }
                )
                if (index < serverTabs.lastIndex) {
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1.0f))

            Image(
                painter = painterResource(R.drawable.ic_location_search),
                contentDescription = null,
                modifier = Modifier.hapticClickable(hapticEnabled = isHapticEnabled) {
                    viewModel.toggleSearch()
                },
                colorFilter = ColorFilter.tint(AppColors.white70)
            )
        }
    }
}