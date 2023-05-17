package com.windscribe.vpn.tests.dialogs

import android.content.Intent
import android.content.Intent.EXTRA_TITLE
import androidx.fragment.app.FragmentOnAttachListener
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import com.windscribe.mobile.R
import com.windscribe.mobile.dialogs.ShareAppLinkDialog
import com.windscribe.mobile.mainmenu.MainMenuActivity
import com.windscribe.vpn.tests.BaseTest
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareAppLinkDialogTest : BaseTest() {

    @Test
    fun testDataShareAppLinkDialog() {
        val activity = ActivityTestRule(MainMenuActivity::class.java).launchActivity(Intent())
        val listener = FragmentOnAttachListener { _, fragment ->
            if (fragment is ShareAppLinkDialog) {
                countingIdlingResource.decrement()
            }
        }
        activity.supportFragmentManager.addFragmentOnAttachListener(listener)
        countingIdlingResource.increment()
        activity.showShareLinkDialog()
        // Verify all the elements are correctly displayed
        onView(withId(R.id.shareAppTitle)).inRoot(isDialog()).check(
            ViewAssertions.matches(
                ViewMatchers.withText(activity.getString(R.string.share_windscribe_with_a_friend))
            )
        )
        onView(withId(R.id.shareAppExplainer)).inRoot(isDialog()).check(
            ViewAssertions.matches(
                ViewMatchers.withText(activity.getString(R.string.referee_must_provide_your_username_at_sign_up_and_confirm_their_email_in_order_for_the_benefits_above_to_apply_to_your_account))
            )
        )
        onView(withId(R.id.shareAppIcon)).inRoot(isDialog()).check(
            ViewAssertions.matches(
                ViewMatchers.isDisplayed()
            )
        )
        onView(withId(R.id.shareAppLinkButton)).inRoot(isDialog()).check(
            ViewAssertions.matches(
                ViewMatchers.withText(activity.getString(R.string.share_invite_link))
            )
        )
        // Verify the share link button works
        onView(withId(R.id.shareAppLinkButton)).inRoot(isDialog()).perform(click())
        val title = IntentMatchers.hasExtra(EXTRA_TITLE, activity.getString(R.string.share_app))
        assertFalse(title == null)
        Intents.intended(title)
        Assert.assertTrue(activity.supportFragmentManager.findFragmentByTag(ShareAppLinkDialog.tag) == null)
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        // Verify the back button works
        countingIdlingResource.increment()
        activity.showShareLinkDialog()
        onView(withId(R.id.shareAppNavButton)).inRoot(isDialog()).perform(click())
        Assert.assertTrue(activity.supportFragmentManager.findFragmentByTag(ShareAppLinkDialog.tag) == null)
        activity.supportFragmentManager.removeFragmentOnAttachListener(listener)
        activity.finish()
    }
}