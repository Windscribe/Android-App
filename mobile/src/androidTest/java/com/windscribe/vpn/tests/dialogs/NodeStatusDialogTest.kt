package com.windscribe.vpn.tests.dialogs

import android.content.Intent
import androidx.fragment.app.FragmentOnAttachListener
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import com.windscribe.mobile.R
import com.windscribe.mobile.dialogs.NodeStatusDialog
import com.windscribe.mobile.windscribe.WindscribeActivity
import com.windscribe.vpn.tests.BaseTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeStatusDialogTest : BaseTest() {
    @Test
    fun testDataNodeStatusDialog() {
        updatedUserConfiguration()
        val activity = ActivityTestRule(WindscribeActivity::class.java).launchActivity(Intent())
        val listener = FragmentOnAttachListener { _, fragment ->
            if (fragment is NodeStatusDialog) {
                countingIdlingResource.decrement()
            }
        }
        activity.supportFragmentManager.addFragmentOnAttachListener(listener)

        countingIdlingResource.increment()
        activity.setUpLayoutForNodeUnderMaintenance(false)
        Espresso.onView(withId(R.id.nodeStatusTitle)).inRoot(RootMatchers.isDialog()).check(
            ViewAssertions.matches(
                ViewMatchers.withText(activity.getString(R.string.under_maintenance))
            )
        )
        Espresso.onView(withId(R.id.nodeStatusDescription)).inRoot(RootMatchers.isDialog()).check(
            ViewAssertions.matches(
                ViewMatchers.withText(activity.getString(R.string.check_status_description))
            )
        )
        Espresso.onView(withId(R.id.nodeStatusGaryIcon)).inRoot(RootMatchers.isDialog()).check(
            ViewAssertions.matches(
                ViewMatchers.isDisplayed()
            )
        )
        Espresso.onView(withId(R.id.nodeStatusPrimaryButton)).inRoot(RootMatchers.isDialog()).check(
            ViewAssertions.matches(
                ViewMatchers.withText(activity.getString(R.string.check_status))
            )
        )
        Espresso.onView(withId(R.id.nodeStatusSecondaryButton)).inRoot(RootMatchers.isDialog())
            .check(
                ViewAssertions.matches(
                    ViewMatchers.withText(activity.getString(R.string.back))
                )
            )

        activity.supportFragmentManager.findFragmentByTag(NodeStatusDialog.tag)?.let { fragment ->
            assert(fragment.isVisible)
        }
        Espresso.onView(withId(R.id.nodeStatusPrimaryButton)).inRoot(RootMatchers.isDialog())
            .perform(click())
        activity.supportFragmentManager.findFragmentByTag(NodeStatusDialog.tag)?.let { fragment ->
            assert(!fragment.isVisible)
        }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()

        countingIdlingResource.increment()
        activity.setUpLayoutForNodeUnderMaintenance(false)
        Espresso.onView(withId(R.id.nodeStatusSecondaryButton)).inRoot(RootMatchers.isDialog())
            .perform(click())
        activity.supportFragmentManager.findFragmentByTag(NodeStatusDialog.tag)?.let { fragment ->
            assert(!fragment.isVisible)
        }

        activity.supportFragmentManager.removeFragmentOnAttachListener(listener)
        activity.finish()
    }

}