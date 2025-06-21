package com.windscribe.mobile.ui.helper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.windscribe.mobile.ui.home.HomeViewmodel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlin.math.abs


@Composable
fun HandleScrollHaptic(lazyListState: LazyListState, viewmodel: HomeViewmodel) {
    val haptic = LocalHapticFeedback.current
    val scrollThreshold = 100
    var lastOffset by remember { mutableIntStateOf(0) }
    val hapticEnabled by viewmodel.hapticFeedbackEnabled.collectAsState()
    if (!hapticEnabled) return
    LaunchedEffect(Unit) {
        snapshotFlow {
            lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
        }.map { (index, offset) ->
            index * 1000 + offset
        }.collectLatest { currentPosition ->
            if (abs(currentPosition - lastOffset) >= scrollThreshold) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastOffset = currentPosition
            }
        }
    }
}

fun Modifier.hapticClickable(
    enabled: Boolean = true,
    hapticEnabled: Boolean,
    onClick: () -> Unit
): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    this.then(
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = false, color = Color.White),
            enabled = enabled
        ) {
            if (hapticEnabled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick()
        }
    )
}