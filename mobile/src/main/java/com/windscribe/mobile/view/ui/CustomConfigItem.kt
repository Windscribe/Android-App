package com.windscribe.mobile.view.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.font16
import com.windscribe.mobile.viewmodel.ConfigListItem
import com.windscribe.mobile.viewmodel.ConfigViewmodel
import com.windscribe.mobile.viewmodel.ConnectionViewmodel
import com.windscribe.mobile.viewmodel.ListState
import com.windscribe.mobile.viewmodel.ServerViewModel
import com.windscribe.vpn.commonutils.WindUtilities
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CustomConfigItem(
    item: ConfigListItem,
    viewModel: ServerViewModel,
    connectionViewModel: ConnectionViewmodel,
    configViewmodel: ConfigViewmodel
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val swipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }

    val iconSize = 48.dp
    val latencyState by viewModel.latencyListState.collectAsState()
    val latency by rememberUpdatedState(
        (latencyState as? ListState.Success)?.data?.find { it.id == item.id }?.time ?: -1
    )
    val isOpenVPN =
        WindUtilities.getConfigType(item.config.content) == WindUtilities.ConfigType.OpenVPN
    val icon = if (isOpenVPN) R.drawable.configsovpn else R.drawable.configswg
    val interactionSource = remember { MutableInteractionSource() }
    val editInteractionSource = remember { MutableInteractionSource() }
    val deleteInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val newOffset =
                                (offsetX.value + dragAmount.x).coerceIn(-2 * iconSize.toPx(), 0f)
                            offsetX.snapTo(newOffset)
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            val targetOffset = if (offsetX.value < -swipeThreshold) {
                                // Snap open (show actions)
                                -2 * iconSize.toPx()
                            } else {
                                // Snap back closed
                                0f
                            }
                            offsetX.animateTo(targetOffset)
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            val targetOffset = if (offsetX.value < -swipeThreshold) {
                                -2 * iconSize.toPx()
                            } else {
                                0f
                            }
                            offsetX.animateTo(targetOffset)
                        }
                    }
                )
            }
    ) {
        // Background actions
        Row(Modifier.align(Alignment.CenterEnd)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        interactionSource = editInteractionSource,
                        indication = rememberRipple(bounded = true, color = Color.White)
                    ) {
                        // Edit action
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        interactionSource = deleteInteractionSource,
                        indication = rememberRipple(bounded = true, color = Color.White)
                    ) {
                        configViewmodel.deleteCustomConfig(item.config)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Foreground (swipeable content)
        Row(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .height(48.dp)
                .clickable(
                    interactionSource,
                    indication = rememberRipple(bounded = true, color = AppColors.white)
                ) {
                    connectionViewModel.onConfigClick(item.config)
                }
                .padding(horizontal = 16.dp)
                .background(color = AppColors.homeBackground),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painter = painterResource(icon), contentDescription = "Custom config")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.config.name,
                style = font16.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
                color = AppColors.white,
                textAlign = TextAlign.Start
            )
            LatencyIcon(latency)
        }
    }
}