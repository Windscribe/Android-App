package com.windscribe.mobile.view.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
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
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.viewmodel.ServerListType
import com.windscribe.mobile.viewmodel.ServerViewModel

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
fun ServerListNavigation(
    modifier: Modifier,
    viewModel: ServerViewModel,
    onTabSelected: (Int) -> Unit
) {
    val selectedType by viewModel.selectedServerListType.collectAsState()
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
            listOf(
                R.drawable.ic_location_all to ServerListType.All,
                R.drawable.ic_location_fav to ServerListType.Fav,
                R.drawable.ic_location_static to ServerListType.Static,
                R.drawable.ic_location_config to ServerListType.Config
            ).forEachIndexed { index, (icon, type) ->
                val interactionSource = MutableInteractionSource()
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(bounded = false, color = AppColors.white)
                    ) {
                        onTabSelected(index)
                    },
                    colorFilter = if (selectedType == type) ColorFilter.tint(AppColors.white)
                    else ColorFilter.tint(AppColors.white70),
                )
                if (index < 3) Spacer(modifier = Modifier.width(16.dp))
            }
            Spacer(modifier = Modifier.weight(1.0f))
            val interactionSource = MutableInteractionSource()
            Image(
                painter = painterResource(R.drawable.ic_location_search),
                contentDescription = null,
                modifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(bounded = false, color = AppColors.white)
                ) {
                    viewModel.toggleSearch()
                },
                colorFilter = ColorFilter.tint(AppColors.white70)
            )
        }
    }
}