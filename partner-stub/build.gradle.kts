/*
 * Stands in for a partner app.
 *
 * It depends on windscribe-core.aar and the dependency snippet that ships beside it, carries no
 * ProGuard rules of its own, and shrinks. R8 therefore has only the AAR's own proguard.txt to work
 * from, so a keep rule missing from that contract fails here instead of in a partner's build.
 *
 * Shrinking one of our own apps cannot do this: an app's rules cover the fused code too, so a rule
 * absent from the contract still passes.
 *
 * Not in the default build. Needs `:windscribe-core:bundle` first, since it consumes that output:
 *
 *     ./gradlew :windscribe-core:bundle
 *     ./gradlew :partner-stub:minifyReleaseWithR8
 *
 * Shrinking is all this does. To install and run it, it needs a launcher activity, the debug
 * keystore on `release`, and `@HiltAndroidApp` on an Application of its own — without which every
 * `@AndroidEntryPoint` in the AAR, `WindscribeVpnService` included, cannot be constructed.
 */

plugins {
    id("com.android.application")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.windscribe.partnerstub"
    compileSdk = rootProject.extra["appCompiledSdk"] as Int

    defaultConfig {
        applicationId = "com.windscribe.partnerstub"
        minSdk = rootProject.extra["appMinSdk"] as Int
        targetSdk = rootProject.extra["appTargetSdk"] as Int
        versionCode = 1
        versionName = "1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // AGP's defaults and nothing else. A partner's own rules are their business; everything
            // windscribe-core needs has to arrive in the AAR.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    // The stub is never shipped, and signing it would need the release keystore CI fetches for the
    // real apps.
    buildTypes.getByName("release").signingConfig = null
}

dependencies {
    implementation(files("$rootDir/windscribe-core/build/outputs/aar/windscribe-core.aar"))
}

// The same one line a partner applies. Checked in rather than read from build/, so configuring this
// module does not depend on an earlier build; `verifyConsumerDependencies` catches it drifting.
apply(from = "windscribe-core-dependencies.gradle")

tasks.register("verifyConsumerDependencies") {
    description = "Fails when the checked-in dependency snippet no longer matches the generated one."
    dependsOn(":windscribe-core:generateConsumerDependencies")
    val generated =
        project(":windscribe-core")
            .layout.buildDirectory
            .file("outputs/aar/windscribe-core-dependencies.gradle")
    val committed = layout.projectDirectory.file("windscribe-core-dependencies.gradle")
    inputs.file(generated)
    inputs.file(committed)
    doLast {
        val fresh = generated.get().asFile.readText()
        val ours = committed.asFile.readText()
        if (fresh != ours) {
            throw GradleException(
                "partner-stub/windscribe-core-dependencies.gradle is stale. " +
                    "Copy ${generated.get().asFile} over it.",
            )
        }
    }
}
