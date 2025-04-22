package com.windscribe.mobile.viewmodel

import android.text.Html
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.localdatabase.tables.NewsfeedAction
import com.windscribe.vpn.localdatabase.tables.WindNotification
import com.windscribe.vpn.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.util.Locale
import javax.inject.Inject

data class NewsfeedItem(
    val id: Int,
    val title: String,
    val message: String,
    val action: Action
)

sealed class Action {
    data class Url(val label: String, val url: String) : Action()
    data class Newsfeed(val newsfeedAction: NewsfeedAction) : Action()
    object None : Action()
}

sealed class GoToRoute {
    data class Browser(val url: String) : GoToRoute()
    data class Upgrade(val pushNotificationAction: PushNotificationAction) : GoToRoute()
    object None : GoToRoute()
}

sealed class NewsfeedState {
    object Loading : NewsfeedState()
    data class Success(val itemToExpand: Int, val newsfeed: List<NewsfeedItem>) : NewsfeedState()
    data class Error(val message: String) : NewsfeedState()
}

data class NewsfeedArguments(val showPopUp: Boolean, val popUpId: Int, val pcpID: String?): Serializable

class NewsfeedViewmodel @Inject constructor(
    backgroundScope: CoroutineScope,
    private val repository: NotificationRepository,
    private val preferencesHelper: PreferencesHelper
) : ViewModel() {
    private val _newsfeedState = MutableStateFlow<NewsfeedState>(NewsfeedState.Loading)
    val newsfeedState: StateFlow<NewsfeedState> = _newsfeedState
    private val _goTo = MutableStateFlow<GoToRoute>(GoToRoute.None)
    val goTo: StateFlow<GoToRoute> = _goTo
    var arguments: NewsfeedArguments? = null

    private val logger = LoggerFactory.getLogger("basic")


    init {
        backgroundScope.launch {
            getNewsfeed()
        }
    }

    private suspend fun getNewsfeed() {
        _newsfeedState.value = NewsfeedState.Loading
        preferencesHelper.setShowNewsFeedAlert(false)

        val newsfeed = repository.getNotifications()
        logger.info("Loaded notification data successfully...")

        var firstItemToOpen = -1
        for (wn in newsfeed) {
            val read = preferencesHelper.isNotificationAlreadyShown(wn.notificationId.toString())
            if (!read && firstItemToOpen == -1) {
                firstItemToOpen = wn.notificationId
            }
            wn.isRead = read
        }

        if (arguments?.showPopUp == true) {
            firstItemToOpen = arguments?.popUpId ?: -1
        } else if (firstItemToOpen != -1) {
            logger.debug("Showing unread message with Id: $firstItemToOpen")
        } else {
            logger.debug("No pop-up or unread message to show")
        }
        _newsfeedState.value = if (newsfeed.isEmpty()) {
            NewsfeedState.Error("Failed to load notifications")
        } else {
            NewsfeedState.Success(firstItemToOpen, newsfeed.map { createNotificationItem(it) })
        }
    }

    private fun createNotificationItem(notification: WindNotification): NewsfeedItem {
        val newsfeedAction = notification.action
        var message: String = notification.notificationMessage

        // Find the last <p> tag
        val bodyEndIndex = message.lastIndexOf("<p")
        if (bodyEndIndex > 0) {
            val body = message.substring(0, bodyEndIndex)
            val pTag = message.substring(bodyEndIndex)
            if (pTag.contains("ncta")) {
                // Extract the <a> tag URL if present
                val linkRegex = "<a\\s+href=\"(.*?)\".*?>(.*?)<\\/a>".toRegex()
                val match = linkRegex.find(pTag)

                if (match != null) {
                    val url = match.groupValues[1]
                    val label = match.groupValues[2]
                    return NewsfeedItem(
                        id = notification.notificationId,
                        title = Html.fromHtml(notification.notificationTitle.uppercase(Locale.getDefault()))
                            .toString(),
                        message = Html.fromHtml(body).toString().trim(),
                        action = Action.Url(label, url)
                    )
                }
                message = body
            }
        }
        val htmlBody = Html.fromHtml(message).toString().trim()
        return NewsfeedItem(
            id = notification.notificationId,
            title = Html.fromHtml(notification.notificationTitle)
                .toString(),
            message = htmlBody,
            action = if (newsfeedAction == null) Action.None else Action.Newsfeed(newsfeedAction)
        )
    }

    fun onNotificationActionClick(action: Action) {
        when (action) {
            is Action.Newsfeed -> {
                val pushNotificationAction = PushNotificationAction(
                    action.newsfeedAction.pcpID,
                    action.newsfeedAction.promoCode, action.newsfeedAction.type
                )
                if (pushNotificationAction.type == "promo") {
                    viewModelScope.launch {
                        _goTo.emit(GoToRoute.Upgrade(pushNotificationAction))
                    }
                }
            }

            is Action.Url -> {
                viewModelScope.launch {
                    _goTo.emit(GoToRoute.Browser(action.url))
                }
            }

            Action.None -> {}
        }
    }

    fun onExpandClick(itemToExpand: String) {
        preferencesHelper.saveNotificationId(itemToExpand)
    }

    fun clearGoToRoute() {
        _goTo.value = GoToRoute.None
    }
}