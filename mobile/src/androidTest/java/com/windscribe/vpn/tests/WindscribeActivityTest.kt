package com.windscribe.vpn.tests

import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.windscribe.mobile.windscribe.WindscribeActivity
import com.windscribe.test.Constants
import com.windscribe.test.Constants.ConnectionTime
import com.windscribe.test.Constants.DisconnectTime
import com.windscribe.test.Constants.FragmentLaunchTime
import com.windscribe.test.Constants.LayoutReadyTime
import com.windscribe.test.Constants.Padding
import com.windscribe.test.Constants.TestIp
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.mocks.TestWindVpnController
import de.codecentric.androidtestktx.espresso.extensions.waitFor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WindscribeActivityTest : BaseTest() {
    @get:Rule
    var activityScenarioRule = activityScenarioRule<WindscribeActivity>()

    @Test
    fun mainMenu() {
        launchHomeScreen()
        buildHomeRobot {
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
            (appContext.vpnController as TestWindVpnController).mockState =
                VPNState(VPNState.Status.Connected, ip = TestIp)
            homeViewVisible()
            waitFor(LayoutReadyTime)
            pressConnectButton()
            waitFor(ConnectionTime + Padding)
            ipAddressMatch(TestIp)
            pressConnectButton()
            waitFor(DisconnectTime)
            ipAddressDoesNotMatch(TestIp)
            waitFor(LayoutReadyTime)
        }
    }

    @Test
    fun launchConnectionFailureView() {
        launchHomeScreen()
        buildHomeRobot {
            (appContext.vpnController as TestWindVpnController).mockState =
                VPNState(VPNState.Status.Disconnected)
            homeViewVisible()
            waitFor(LayoutReadyTime)
            pressConnectButton()
            waitFor(ConnectionTime * 2)
            waitFor(FragmentLaunchTime)
            connectionFailureViewVisible()
        }
    }

    @Test
    fun connectionFailed() {
        launchHomeScreen()
        buildHomeRobot {
            (appContext.vpnController as TestWindVpnController).mockState =
                VPNState(VPNState.Status.Disconnected)
            homeViewVisible()
            waitFor(500)
            pressConnectButton()
            waitFor(8000)
        }
    }

    private fun launchHomeScreen() {
        buildLoginRobot {
            waitFor(2000)
            pressLoginButton()
            waitFor(500)
            enterUserName(Constants.Username)
            enterPassword(Constants.Password)
            pressContinueButton()
            waitFor(8 * 1000)
        }
    }
}