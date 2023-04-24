package com.windscribe.vpn.tests.dialogs

import android.content.Intent
import androidx.fragment.app.FragmentOnAttachListener
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import com.windscribe.mobile.R
import com.windscribe.mobile.dialogs.AccountStatusDialog
import com.windscribe.mobile.dialogs.AccountStatusDialogData
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.mobile.windscribe.WindscribeActivity
import com.windscribe.vpn.di.TestConfiguration
import com.windscribe.vpn.tests.BaseTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountStatusDialogTest : BaseTest() {

    @Test
    fun testAccountStatusDialog() {
        countingIdlingResource.increment()
        updatedUserConfiguration(
            TestConfiguration(
                accountStatus = 1, lastAccountStatus = 1
            )
        )
        val activity = ActivityTestRule(WindscribeActivity::class.java).launchActivity(Intent())
        val listener = FragmentOnAttachListener { _, fragment ->
            if (fragment is AccountStatusDialog) {
                countingIdlingResource.decrement()
            }
        }
        activity.supportFragmentManager.addFragmentOnAttachListener(listener)
        // Show expired account dialog
        updatedUserConfiguration(
            TestConfiguration(
                accountStatus = 2, lastAccountStatus = 1
            )
        )
        var data = AccountStatusDialogData(
            title = activity.getString(R.string.you_re_out_of_data),
            icon = R.drawable.garry_nodata,
            description = activity.getString(R.string.upgrade_to_stay_protected),
            showSkipButton = true,
            skipText = activity.getString(R.string.upgrade_later),
            showUpgradeButton = true,
            upgradeText = activity.getString(R.string.upgrade),
            bannedLayout = false
        )
        // Verify dialog data
        verifyData(data)
        Assert.assertFalse(activity.isFinishing)
        onView(withId(R.id.userAccountStatusPrimaryButton)).inRoot(RootMatchers.isDialog())
            .perform(click())
        intended(hasComponent(UpgradeActivity::class.java.name))
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        // Show banned account dialog
        countingIdlingResource.increment()
        updatedUserConfiguration(
            TestConfiguration(
                accountStatus = 3, lastAccountStatus = 1
            )
        )
        data = AccountStatusDialogData(
            title = activity.getString(R.string.you_ve_been_banned),
            icon = R.drawable.garry_angry,
            description = activity.getString(R.string.you_ve_violated_our_terms),
            showSkipButton = false,
            skipText = "",
            showUpgradeButton = true,
            upgradeText = activity.getString(R.string.ok),
            bannedLayout = true
        )
        // Verify dialog data
        verifyData(data)
        onView(withId(R.id.userAccountStatusPrimaryButton)).inRoot(RootMatchers.isDialog())
            .perform(click())
        Assert.assertTrue(activity.isFinishing)

        activity.supportFragmentManager.removeFragmentOnAttachListener(listener)
        activity.finish()
    }

    private fun verifyData(data: AccountStatusDialogData) {
        onView(withId(R.id.userAccountStatusIcon)).inRoot(RootMatchers.isDialog())
            .check(matches(isDisplayed()))
        onView(withId(R.id.userAccountStatusTitle)).inRoot(RootMatchers.isDialog())
            .check(matches(withText(data.title)))
        onView(withId(R.id.userAccountStatusDescription)).inRoot(RootMatchers.isDialog())
            .check(matches(withText(data.description)))
        onView(withId(R.id.userAccountStatusPrimaryButton)).inRoot(RootMatchers.isDialog())
            .check(matches(withText(data.upgradeText)))
        onView(withId(R.id.userAccountStatusSecondaryButton)).inRoot(RootMatchers.isDialog())
            .check(matches(withText(data.skipText)))
    }
}