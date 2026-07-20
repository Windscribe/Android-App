package com.windscribe.mobile.ui.helper

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.home.HomeViewmodel
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

@Composable
fun HandleScrollHaptic(
    lazyListState: LazyListState,
    viewmodel: HomeViewmodel,
) {
    val haptic = LocalHapticFeedback.current
    // Increase threshold to reduce haptic frequency (equivalent to ~3-4 items)
    val itemScrollThreshold = 3
    var lastItemIndex by remember { mutableIntStateOf(0) }
    val hapticEnabled by viewmodel.hapticFeedbackEnabled.collectAsState()
    if (!hapticEnabled) return

    LaunchedEffect(Unit) {
        snapshotFlow {
            lazyListState.firstVisibleItemIndex
        }.collectLatest { currentItemIndex ->
            // Only trigger haptic when scrolling past multiple items
            if (abs(currentItemIndex - lastItemIndex) >= itemScrollThreshold) {
                // Use TextHandleMove for lighter haptic feedback during scrolling
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastItemIndex = currentItemIndex
            }
        }
    }
}

fun Modifier.hapticClickableRipple(onClick: () -> Unit): Modifier =
    composed {
        val haptics = LocalHapticFeedback.current
        val interactionSource = remember { MutableInteractionSource() }
        val activity = LocalActivity.current as? AppStartActivity
        val hapticEnabled by activity?.viewmodel?.hapticFeedback?.collectAsState()
            ?: remember { mutableStateOf(false) }
        this.then(
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, color = Color.White),
                enabled = true,
            ) {
                if (hapticEnabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                onClick()
            },
        )
    }

fun Modifier.hapticClickable(onClick: () -> Unit): Modifier =
    composed {
        val haptics = LocalHapticFeedback.current
        val activity = LocalActivity.current as? AppStartActivity
        val hapticEnabled by activity?.viewmodel?.hapticFeedback?.collectAsState()
            ?: remember { mutableStateOf(false) }
        this.then(
            Modifier.clickable {
                if (hapticEnabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                onClick()
            },
        )
    }
