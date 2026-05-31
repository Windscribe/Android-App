package com.windscribe.mobile.ui.nav

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private typealias EnterTransform = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
private typealias ExitTransform = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

/** Standard horizontal slide transitions shared by most navigation screens. */
private val slideEnter: EnterTransform = { slideInHorizontally(initialOffsetX = { -it }) }
private val slideExit: ExitTransform = { slideOutHorizontally(targetOffsetX = { it }) }
private val slidePopEnter: EnterTransform = { slideInHorizontally(initialOffsetX = { it }) }
private val slidePopExit: ExitTransform = { slideOutHorizontally(targetOffsetX = { -it }) }

/**
 * Like [composable] but applies the standard horizontal slide transitions, so the
 * four enter/exit lambdas don't have to be repeated at every call site. Any of the
 * four can be overridden for screens that need a different animation.
 */
fun NavGraphBuilder.slidingComposable(
    route: String,
    enterTransition: EnterTransform = slideEnter,
    exitTransition: ExitTransform = slideExit,
    popEnterTransition: EnterTransform = slidePopEnter,
    popExitTransition: ExitTransform = slidePopExit,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable(
    route = route,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    popEnterTransition = popEnterTransition,
    popExitTransition = popExitTransition,
    content = content,
)
