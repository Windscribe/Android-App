package com.windscribe.vpn.tests

import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.windscribe.mobile.welcome.WelcomeActivity
import com.windscribe.test.Constants
import com.windscribe.test.Constants.TestIp
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.mocks.TestWindVpnController
import de.codecentric.androidtestktx.espresso.extensions.waitFor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WindscribeActivityTest : BaseTest() {
    @get:Rule
    var activityScenarioRule = activityScenarioRule<WelcomeActivity>()

    @Test
    fun mainMenu() {
        launchHomeScreen()
        buildHomeRobot {
            waitFor(500)
            homeViewVisible()
            pressHamburgerMenuButton()
            waitFor(500)
            mainMenuVisible()
            pressNavBackButton()
            waitFor(500)
            homeViewVisible()
            waitFor(500)
        }
    }

    @Test
    fun newsfeed() {
        launchHomeScreen()
        buildHomeRobot {
            waitFor(500)
            homeViewVisible()
            pressNewsFeedButton()
            waitFor(500)
            newsFeedViewVisible()
            pressCloseNewsFeedButton()
            waitFor(500)
            homeViewVisible()
            waitFor(500)
        }
    }

    @Test
    fun searchItem() {
        launchHomeScreen()
        buildHomeRobot {
            waitFor(500)
            homeViewVisible()
            pressSearchIcon()
            waitFor(2000)
            searchLayoutExpanded()
            focusOnSearchView()
            enterSearchQuery("Skydome")
            waitFor(1000)
            closeSearchLayout()
            searchLayoutMinimized()
            homeViewVisible()
            waitFor(500)
        }
    }

    @Test
    fun connectionSuccess() {
        launchHomeScreen()
        buildHomeRobot {
            (Windscribe.appContext.vpnController as TestWindVpnController).nextState = VPNState(VPNState.Status.Connected, ip = TestIp)
            waitFor(500)
            homeViewVisible()
            waitFor(500)
            pressConnectButton()
            waitFor(8000)
            ipAddressMatch(TestIp)
            pressConnectButton()
            waitFor(2000)
            ipAddressDoesNotMatch(TestIp)
            waitFor(1000)
        }
    }

    @Test
    fun connectionFailed() {
        launchHomeScreen()
        buildHomeRobot {
            (Windscribe.appContext.vpnController as TestWindVpnController).nextState = VPNState(VPNState.Status.Disconnected)
            waitFor(500)
            homeViewVisible()
            waitFor(500)
            pressConnectButton()
            waitFor(8000)
        }
    }

    private fun launchHomeScreen() {
        buildLoginRobot {
            pressLoginButton()
            waitFor(500)
            enterUserName(Constants.Username)
            enterPassword(Constants.Password)
            pressContinueButton()
        }
    }
}