/*
 * Copyright (c) 2026 Windscribe Limited.
 */
package com.windscribe.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a Baseline Profile for the :mobile app by driving the real
 * cold-launch -> login -> Home -> preferences journey.
 *
 * The output (a startup + navigation ART profile) is consumed automatically by
 * the `androidx.baselineprofile` plugin applied in :mobile/build.gradle.kts and
 * packaged into the release APK/AAB.
 *
 * Credentials come from instrumentation runner arguments TEST_EMAIL /
 * TEST_PASSWORD — the SAME staging account the Maestro E2E suite uses. They are
 * passed by CI (and locally) via:
 *   -Pandroid.testInstrumentationRunnerArguments.TEST_EMAIL=...
 *   -Pandroid.testInstrumentationRunnerArguments.TEST_PASSWORD=...
 * No fake data: the journey performs a real login against staging.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        val args = InstrumentationRegistry.getArguments()
        val email = args.getString("TEST_EMAIL").orEmpty()
        val password = args.getString("TEST_PASSWORD").orEmpty()

        // Fail loudly rather than silently emitting a startup-only profile if
        // credentials were not wired through — the whole point is the full journey.
        if (email.isBlank() || password.isBlank()) {
            fail(
                "TEST_EMAIL / TEST_PASSWORD instrumentation args are required. " +
                    "Pass them with -Pandroid.testInstrumentationRunnerArguments.TEST_EMAIL=... " +
                    "(same staging account as the Maestro E2E suite).",
            )
        }

        rule.collect(packageName = TARGET_PACKAGE) {
            // 1. Cold start — captures Application/Hilt init, AppStartActivity,
            //    splash screen, and the initial Compose frame.
            pressHome()
            startActivityAndWait()

            // 2. Real login on the Start screen using the shared testTag IDs.
            login(device, email, password)

            // 3. Land on Home — exercises the connect button / home Compose tree.
            if (!device.waitForTag("home_connect_button", LONG_TIMEOUT)) {
                fail("Did not reach Home screen after login")
            }

            // 4. Open the preferences graph and scroll it — covers the
            //    MainMenu + sliding-composable navigation hot path.
            openMainMenuAndScroll(device)
        }
    }

    private fun login(
        device: UiDevice,
        email: String,
        password: String,
    ) {
        // Mirrors maestro/subflows/login.yaml exactly.
        device.tapText("Log In")
        device.tapTag("login_username_field")
        device.typeInto("login_username_field", email)
        device.tapTag("login_password_field")
        device.typeInto("login_password_field", password)
        device.tapTag("login_submit_button")
    }

    private fun openMainMenuAndScroll(device: UiDevice) {
        if (device.tapTag("main_menu_button")) {
            device.waitForTag("main_menu_screen", SHORT_TIMEOUT)
            // Scroll the menu to lay down the rendering path for the list of
            // preference rows (each is a separate sliding Compose screen entry).
            repeat(2) { device.swipeUp() }
            device.pressBack()
            device.waitForTag("home_connect_button", SHORT_TIMEOUT)
        }
    }

    companion object {
        // :mobile applicationId — no flavor suffix (see mobile/build.gradle.kts).
        private const val TARGET_PACKAGE = "com.windscribe.vpn"
        private const val SHORT_TIMEOUT = 5_000L
        private const val LONG_TIMEOUT = 20_000L
    }
}

// ---------------------------------------------------------------------------
// UiAutomator helpers. testTag IDs are exposed as resource-ids because
// AppStartActivity sets `testTagsAsResourceId = true` on its root semantics.
// ---------------------------------------------------------------------------

private fun UiDevice.waitForTag(
    tag: String,
    timeout: Long,
): Boolean = wait(Until.hasObject(By.res(tag)), timeout)

private fun UiDevice.tapTag(tag: String): Boolean {
    if (!waitForTag(tag, 5_000L)) return false
    findObject(By.res(tag))?.click() ?: return false
    return true
}

private fun UiDevice.tapText(text: String): Boolean {
    if (!wait(Until.hasObject(By.text(text)), 5_000L)) return false
    findObject(By.text(text))?.click() ?: return false
    return true
}

private fun UiDevice.typeInto(
    tag: String,
    value: String,
) {
    findObject(By.res(tag))?.text = value
}

private fun UiDevice.swipeUp() {
    val w = displayWidth
    val h = displayHeight
    swipe(w / 2, (h * 0.7).toInt(), w / 2, (h * 0.3).toInt(), 10)
}
