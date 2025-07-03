package com.windscribe.mobile.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.serverlist.ConfigListItem
import com.windscribe.mobile.ui.serverlist.ConfigViewmodel
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.serverlist.ListState
import com.windscribe.mobile.ui.serverlist.ServerViewModel
import com.windscribe.mobile.ui.theme.serverItemTextColor
import com.windscribe.mobile.ui.theme.serverListBackgroundColor
import com.windscribe.mobile.ui.theme.serverListSecondaryColor
import com.windscribe.vpn.commonutils.WindUtilities
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CustomConfigItem(
    item: ConfigListItem,
    viewModel: ServerViewModel,
    connectionViewModel: ConnectionViewmodel,
    configViewmodel: ConfigViewmodel,
) {
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
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val iconSizePx = with(LocalDensity.current) { 48.dp.toPx() }
    val swipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.x
                if (abs(delta) > abs(available.y)) {
                    coroutineScope.launch {
                        val newOffset = (offsetX.value + delta).coerceIn(-2 * iconSizePx, 0f)
                        offsetX.snapTo(newOffset)
                    }
                    // Consume horizontal drag
                    return Offset(x = delta, y = 0f)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val targetOffset = if (offsetX.value < -swipeThreshold) {
                    -2 * iconSizePx
                } else {
                    0f
                }
                offsetX.animateTo(targetOffset)
                return super.onPostFling(consumed, available)
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .nestedScroll(nestedScrollConnection)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    coroutineScope.launch {
                        val newOffset = (offsetX.value + delta).coerceIn(-2 * iconSizePx, 0f)
                        offsetX.snapTo(newOffset)
                    }
                },
                onDragStopped = {
                    coroutineScope.launch {
                        val targetOffset = if (offsetX.value < -swipeThreshold) {
                            -2 * iconSizePx
                        } else {
                            0f
                        }
                        offsetX.animateTo(targetOffset)
                    }
                }
            )
    ) {
        // Background actions
        val navController = LocalNavController.current
        Row(Modifier.align(Alignment.CenterEnd)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        interactionSource = editInteractionSource,
                        indication = ripple(
                            bounded = true,
                            color = MaterialTheme.colorScheme.serverListSecondaryColor
                        )
                    ) {
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            "config_id",
                            item.id
                        )
                        navController.navigate(Screen.EditCustomConfig.route)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.serverListSecondaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        interactionSource = deleteInteractionSource,
                        indication = ripple(
                            bounded = true,
                            color = MaterialTheme.colorScheme.serverListSecondaryColor
                        )
                    ) {
                        configViewmodel.deleteCustomConfig(item.config)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.serverListSecondaryColor,
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
                    indication = ripple(
                        bounded = true,
                        color = MaterialTheme.colorScheme.serverListSecondaryColor
                    )
                ) {
                    connectionViewModel.onConfigClick(item.config)
                }
                .padding(horizontal = 16.dp)
                .background(color = MaterialTheme.colorScheme.serverListBackgroundColor),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = "Custom config",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.serverItemTextColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.config.name,
                style = font16.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.serverItemTextColor,
                textAlign = TextAlign.Start
            )
            LatencyIcon(latency)
        }
    }
}