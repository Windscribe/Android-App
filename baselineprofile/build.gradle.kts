/*
 * Copyright (c) 2026 Windscribe Limited.
 *
 * Baseline Profile generator module for :mobile.
 *
 * This module drives a real user journey (cold launch -> login -> Home ->
 * preferences) on a device/emulator and records the classes/methods that ART
 * should pre-compile (AOT) at install time. The generated profile is written
 * back into :mobile and shipped in the APK/AAB.
 *
 * Generate locally:
 *   ./gradlew :mobile:generateGoogleReleaseBaselineProfile \
 *     -Pandroid.testInstrumentationRunnerArguments.TEST_EMAIL=$TEST_EMAIL \
 *     -Pandroid.testInstrumentationRunnerArguments.TEST_PASSWORD=$TEST_PASSWORD
 *
 * Credentials are the SAME staging account used by the Maestro E2E suite
 * (TEST_EMAIL / TEST_PASSWORD). No fake data — the journey logs in for real.
 */
plugins {
    // Kotlin compilation is provided by AGP 9's built-in support, the same way
    // the other modules (:common, :base) compile Kotlin without an explicit
    // org.jetbrains.kotlin.android plugin. Do not add it here — it conflicts.
    id("com.android.test")
    id("androidx.baselineprofile")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.windscribe.baselineprofile"
    compileSdk = rootProject.extra["appCompiledSdk"] as Int

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // Baseline profile generation requires API 28+ (UiAutomator + ART profile tooling),
        // even though the app itself ships down to minSdk 24.
        minSdk = 28
        targetSdk = rootProject.extra["appTargetSdk"] as Int
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // :mobile is the app under test. Flavors must match so the plugin can wire
    // the generated profile into the right :mobile variant.
    targetProjectPath = ":mobile"
    flavorDimensions += "dim"
    productFlavors {
        create("fdroid") { dimension = "dim" }
        create("google") { dimension = "dim" }
    }

    // Reproducible, headless generation device. CI also runs the existing
    // `ws_test_avd` emulator; either works, but the managed device makes
    // `./gradlew ...generateBaselineProfile` self-contained for local runs.
    @Suppress("UnstableApiUsage")
    testOptions.managedDevices.allDevices {
        create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel6Api34") {
            device = "Pixel 6"
            apiLevel = 34
            systemImageSource = "aosp"
        }
    }
}

// Local runs use the self-contained managed device. CI boots its own emulator
// (the same `ws_test_avd` as the E2E job) and passes -PuseConnectedDevices=true
// so generation runs against that already-connected device instead.
val runOnConnected =
    providers
        .gradleProperty("useConnectedDevices")
        .map { it.toBoolean() }
        .getOrElse(false)
baselineProfile {
    if (runOnConnected) {
        useConnectedDevices = true
    } else {
        managedDevices += "pixel6Api34"
        useConnectedDevices = false
    }
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
