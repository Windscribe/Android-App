package com.windscribe.vpn.robots

import androidx.test.espresso.ViewInteraction
import com.windscribe.mobile.R
import com.windscribe.test.*

class HomeRobot : BaseRobot() {
    fun pressConnectButton() = connectButton.click()
    fun pressHamburgerMenuButton() = hamburgerMenuButton.click()
    fun pressNewsFeedButton() = newsfeedButton.click()
    fun newsFeedViewVisible() = newsFeedLabel.isViewVisible()
    fun pressCloseNewsFeedButton() = newsFeedCloseButton.click()
    fun pressNavBackButton() = navButton.click()
    fun homeViewVisible() = homeView.isViewVisible()
    fun mainMenuVisible() = navTitle.textMatches("Preferences")
    fun pressSearchIcon() = searchIcon.click()
    fun searchLayoutExpanded() = searchLayout.isViewVisible()
    fun searchLayoutMinimized() = searchLayout.viewDoesNotExists()
    fun enterSearchQuery(query: String) = view(R.id.search_src_text).typeText(query)
    fun focusOnSearchView() = searchView.click()
    fun closeSearchLayout() = searchLayoutMinimizeButton.click()
    fun ipAddressMatch(ip: String) = ipView.textMatches(ip)
    fun ipAddressDoesNotMatch(ip: String) = ipView.textDoestNotMatch(ip)
    fun connectionFailureViewVisible(): ViewInteraction =
        connectionFailureTitle.textMatches("Connection Failure!")

    companion object {
        val connectButton: ViewInteraction = view(R.id.on_off_button)
        val hamburgerMenuButton: ViewInteraction = view(R.id.img_hamburger_menu)
        val newsfeedButton: ViewInteraction = view(R.id.img_windscribe_logo)
        val homeView: ViewInteraction = view(R.id.cl_windscribe_main)
        val navButton: ViewInteraction = view(R.id.nav_button)
        val navTitle: ViewInteraction = view(R.id.nav_title)
        val newsFeedCloseButton: ViewInteraction = view(R.id.img_news_feed_close_btn)
        val newsFeedLabel: ViewInteraction = view(R.id.tv_news_label)
        val searchIcon: ViewInteraction = view(R.id.img_search_list)
        val searchLayout: ViewInteraction = view(R.id.search_layout)
        val searchView: ViewInteraction = view(R.id.searchView)
        val searchLayoutMinimizeButton: ViewInteraction = view(R.id.minimize_icon)
        val ipView: ViewInteraction = view(R.id.ip_address)
        val connectionFailureTitle: ViewInteraction = view(R.id.title)
    }
}