/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.test

import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.not

fun ViewInteraction.click(): ViewInteraction =
    perform(ViewActions.click())

fun ViewInteraction.typeText(text: String): ViewInteraction =
    perform(ViewActions.typeText(text), ViewActions.closeSoftKeyboard())

fun ViewInteraction.isTextDisplayed(text: String): ViewInteraction =
    check(ViewAssertions.matches(ViewMatchers.withText(text)))

fun ViewInteraction.isViewVisible(): ViewInteraction =
    check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

fun ViewInteraction.viewDoesNotExists(): ViewInteraction =
        check(doesNotExist())

fun ViewInteraction.isEnabled(): ViewInteraction =
    check(ViewAssertions.matches(ViewMatchers.isEnabled()))

fun ViewInteraction.isDisabled(): ViewInteraction =
    check(ViewAssertions.matches(ViewMatchers.isNotEnabled()))

fun ViewInteraction.clearInput(): ViewInteraction =
    perform(ViewActions.clearText())

fun ViewInteraction.scrollTo(): ViewInteraction =
    perform(ViewActions.scrollTo())

fun ViewInteraction.hidden(): ViewInteraction =
    check(ViewAssertions.matches(isPasswordHidden()))

fun ViewInteraction.showing(): ViewInteraction =
    check(ViewAssertions.matches(isPasswordShowing()))

fun ViewInteraction.textMatches(text: String): ViewInteraction =
check(matches(withText(text)))

fun ViewInteraction.textDoestNotMatch(text: String): ViewInteraction =
        check(matches(not(withText(text))))

fun isPasswordHidden(): BoundedMatcher<View, EditText> {
    return object : BoundedMatcher<View, EditText>(EditText::class.java) {
        override fun matchesSafely(editText: EditText): Boolean {
            return editText.transformationMethod is PasswordTransformationMethod
        }

        override fun describeTo(description: org.hamcrest.Description) {
            description.appendText("Password is hidden.")
        }
    }
}

fun view(id: Int): ViewInteraction = Espresso.onView(ViewMatchers.withId(id))

fun isPasswordShowing(): BoundedMatcher<View, EditText> {
    return object : BoundedMatcher<View, EditText>(EditText::class.java) {
        override fun matchesSafely(editText: EditText): Boolean {
            return editText.transformationMethod == null
        }

        override fun describeTo(description: org.hamcrest.Description) {
            description.appendText("Password is showing.")
        }
    }
}