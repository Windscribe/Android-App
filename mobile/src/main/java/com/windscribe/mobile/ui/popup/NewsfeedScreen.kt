package com.windscribe.mobile.ui.popup

import NavBar
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.ActionButtonLighter
import com.windscribe.mobile.ui.common.AppBackground
import com.windscribe.mobile.ui.common.AppProgressBar
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.NavigationStack
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font18
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.constants.ExtraConstants.PROMO_EXTRA

@Composable
fun NewsfeedScreen(viewModel: NewsfeedViewmodel? = null) {
    val context = LocalContext.current
    val goToRoute by viewModel?.goTo?.collectAsState()
        ?: remember { mutableStateOf(GoToRoute.None) }
    val state by viewModel?.newsfeedState?.collectAsState() ?: remember {
        mutableStateOf(
            mockNewsfeedState()
        )
    }
    val navController = LocalNavController.current
    viewModel?.arguments =
        navController.previousBackStackEntry?.savedStateHandle?.get<NewsfeedArguments>("arguments")
    LaunchedEffect(goToRoute) { handleActions(context, goToRoute, viewModel) }

    AppBackground {
        Column(
            modifier = Modifier
                .background(AppColors.charcoalBlue)
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .statusBarsPadding()
        ) {
            NavBar(stringResource(com.windscribe.vpn.R.string.news_feed)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (state is NewsfeedState.Success) {
                NotificationList(
                    (state as NewsfeedState.Success).itemToExpand,
                    (state as NewsfeedState.Success).newsfeed, viewModel
                )
            } else if (state is NewsfeedState.Error) {
                Text((state as NewsfeedState.Error).message, style = font14, color = Color.White)
            }
        }
        AppProgressBar(state == NewsfeedState.Loading, message = "Loading newsfeed...")
    }
}

private fun handleActions(context: Context, goToRoute: GoToRoute, viewModel: NewsfeedViewmodel?) {
    when (goToRoute) {
        is GoToRoute.Browser -> openBrowser(context, goToRoute.url, viewModel)
        is GoToRoute.Upgrade -> openUpgradeScreen(
            context,
            goToRoute.pushNotificationAction,
            viewModel
        )

        GoToRoute.None -> viewModel?.clearGoToRoute()
    }
}

private fun openBrowser(context: Context, url: String, viewModel: NewsfeedViewmodel?) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
    }
    viewModel?.clearGoToRoute()
}

private fun openUpgradeScreen(
    context: Context,
    promo: PushNotificationAction,
    viewModel: NewsfeedViewmodel?
) {
    val launchIntent = UpgradeActivity.getStartIntent(context).apply {
        putExtra(PROMO_EXTRA, promo)
    }
    context.startActivity(launchIntent)
    viewModel?.clearGoToRoute()
}

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
private fun Title() {
    val navController = LocalNavController.current
    val interactionSource = MutableInteractionSource()
    Row(modifier = Modifier.padding(16.dp)) {
        Text(stringResource(com.windscribe.vpn.R.string.news_feed), style = font18, color = AppColors.white)
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(id = R.drawable.ic_close),
            contentDescription = null,
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = false, color = AppColors.white)
            ) { navController.popBackStack() }
        )
    }
}

@Composable
private fun NotificationList(
    itemToExpand: Int,
    list: List<NewsfeedItem>,
    viewModel: NewsfeedViewmodel?
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .background(
                color = AppColors.white.copy(0.05f),
                shape = RoundedCornerShape(9.dp)
            )
            .verticalScroll(scrollState)
    ) {
        list.forEachIndexed { index, notification ->
            NotificationItem(
                itemToExpand,
                notification,
                index == 0,
                index == list.lastIndex,
                viewModel
            )
            if (index != list.lastIndex) HorizontalDivider(color = AppColors.charcoalBlue)
        }
    }
}

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
private fun NotificationItem(
    itemToExpand: Int,
    notification: NewsfeedItem,
    isFirst: Boolean,
    isLast: Boolean,
    viewModel: NewsfeedViewmodel?
) {
    val expanded = remember { mutableStateOf(notification.id == itemToExpand) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded.value) 180f else 0f,
        animationSpec = tween(durationMillis = 300) // Smooth 300ms animation
    )
    val interactionSource = MutableInteractionSource()
    val shape = RoundedCornerShape(
        topStart = if (isFirst) 9.dp else 0.dp,
        topEnd = if (isFirst) 9.dp else 0.dp,
        bottomStart = if (isLast) 9.dp else 0.dp,
        bottomEnd = if (isLast) 9.dp else 0.dp
    )

    Column(
        modifier = Modifier.background(
            if (expanded.value) AppColors.white.copy(alpha = 0.08f) else Color.Transparent,
            shape
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(bounded = true, color = AppColors.white)
                ) {
                    expanded.value = !expanded.value
                }
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
        ) {
            Text(
                notification.title,
                style = font16.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Start,
                color = AppColors.white,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.ic_expand_small),
                contentDescription = null,
                alpha = if (expanded.value) 1f else 0.4f,
                modifier = Modifier
                    .rotate(rotationAngle)
                    .clickable { expanded.value = !expanded.value }
            )
        }
        Text(
            notification.date,
            style = font14.copy(fontWeight = FontWeight.Normal),
            textAlign = TextAlign.Start,
            color = AppColors.slateGray,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (expanded.value) {
            ExpandedNotificationContent(notification, viewModel)
            viewModel?.onExpandClick(notification.id.toString())
        }
    }
}

@Composable
private fun ExpandedNotificationContent(notification: NewsfeedItem, viewModel: NewsfeedViewmodel?) {
    Text(
        text = notification.message,
        style = font14.copy(fontWeight = FontWeight.Normal),
        color = AppColors.white,
        textAlign = TextAlign.Start,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, end = 16.dp)
    )
    notification.action.getLabel()?.let { label ->
        ActionButtonLighter(
            modifier = Modifier.padding(bottom = 16.dp, top = 8.dp),
            text = label
        ) {
            viewModel?.onNotificationActionClick(notification.action)
        }
    }
}

private fun Action.getLabel(): String? {
    return when (this) {
        is Action.Newsfeed -> newsfeedAction.label
        is Action.Url -> label
        else -> null
    }
}

private fun mockNewsfeedState(): NewsfeedState {
    return NewsfeedState.Success(
        -1,
        listOf(
            NewsfeedItem(1, "Title 1", "Message 1", "2023-10-11", Action.None),
            NewsfeedItem(2, "Title 2", "Message 2", "2023-10-11", Action.Url("", "Watch Now!"))
        )
    )
}

@Preview
@Composable
fun NewsfeedScreenPreview() {
    NavigationStack(Screen.Newsfeed)
}