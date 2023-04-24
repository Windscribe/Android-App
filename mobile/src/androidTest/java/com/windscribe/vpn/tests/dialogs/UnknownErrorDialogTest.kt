package com.windscribe.vpn.tests.dialogs

import android.content.Intent
import androidx.fragment.app.FragmentOnAttachListener
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import com.windscribe.mobile.R
import com.windscribe.mobile.dialogs.UnknownErrorDialog
import com.windscribe.mobile.welcome.WelcomeActivity
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.tests.BaseTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnknownErrorDialogTest : BaseTest() {

    @Test
    fun testUnknownErrorDialog() {
        val activity = ActivityTestRule(WelcomeActivity::class.java).launchActivity(Intent())
        val listener = FragmentOnAttachListener { _, fragment ->
            if (fragment is UnknownErrorDialog) {
                countingIdlingResource.decrement()
            }
        }
        activity.supportFragmentManager.addFragmentOnAttachListener(listener)

        countingIdlingResource.increment()
        activity.showFailedAlert(activity.getString(R.string.failed_network_alert))

        onView(withId(R.id.unknownErrorDescription)).inRoot(isDialog()).check(
            ViewAssertions.matches(
                withText(appContext.getString(R.string.failed_network_alert))
            )
        )
        onView(withId(R.id.unknownErrorIcon)).inRoot(isDialog()).check(
            ViewAssertions.matches(
                isDisplayed()
            )
        )
        onView(withId(R.id.unknownErrorContactSupportButton)).inRoot(isDialog()).check(
            ViewAssertions.matches(
                withText(appContext.getString(R.string.contact_support))
            )
        )
        onView(withId(R.id.unknownErrorSendLogButton)).inRoot(isDialog()).check(
            ViewAssertions.matches(
                withText(appContext.getString(R.string.export_log))
            )
        )
        onView(withId(R.id.unknownErrorCancelButton)).inRoot(isDialog()).check(
            ViewAssertions.matches(
                withText(appContext.getString(R.string.close))
            )
        )

        onView(withId(R.id.unknownErrorContactSupportButton)).inRoot(isDialog()).perform(click())
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()

        countingIdlingResource.increment()
        activity.showFailedAlert(activity.getString(R.string.failed_network_alert))
        val intent = IntentMatchers.hasAction(Intent.ACTION_CHOOSER)
        onView(withId(R.id.unknownErrorSendLogButton)).inRoot(isDialog()).perform(click())
        Intents.intended(intent)
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()

        countingIdlingResource.increment()
        activity.showFailedAlert(activity.getString(R.string.failed_network_alert))
        onView(withId(R.id.unknownErrorCancelButton)).inRoot(isDialog()).perform(click())
        onView(withId(R.id.unknownErrorCancelButton)).check { view, noViewFoundException ->
            Assert.assertNotNull(noViewFoundException)
        }

        activity.supportFragmentManager.removeFragmentOnAttachListener(listener)
        activity.finish()
    }
}