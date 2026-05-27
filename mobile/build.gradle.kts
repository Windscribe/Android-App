/*
 * Copyright (c) 2021 Windscribe Limited.
 */
plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    id("dagger.hilt.android.plugin")
}
apply(from = "$rootDir/config/config.gradle")
apply(from = "$rootDir/depedencycheck.gradle")

android {
    namespace = "com.windscribe.mobile"
    ndkVersion = "27.2.12479018"
    bundle {
        language {
            enableSplit = false
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }

    compileSdk = rootProject.extra["appCompiledSdk"] as Int
    defaultConfig {
        applicationId = rootProject.extra["AppId"] as String
        minSdk = rootProject.extra["appMinSdk"] as Int
        targetSdk = rootProject.extra["appTargetSdk"] as Int
        versionName = System.getenv().getOrDefault("VERSION_NAME", rootProject.extra["appVersionName"] as String)
        versionCode = System.getenv("VERSION_CODE")?.toInt() ?: (rootProject.extra["appVersionCode"] as Int)
        vectorDrawables.useSupportLibrary = true
        if (project.hasProperty("abiFilter")) {
            ndk {
                abiFilters += project.property("abiFilter").toString().split(",")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
        getByName("debug") {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    flavorDimensions += "dim"
    productFlavors {
        create("fdroid") { dimension = "dim" }
        create("google") { dimension = "dim" }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += setOf("NonConstantResourceId", "ContentDescription", "VectorRaster")
    }
    packagingOptions {
        jniLibs {
            excludes +=
                setOf(
                    "lib/arm64-v8a/libwg-quick.so",
                    "lib/armeabi-v7a/libwg-quick.so",
                    "lib/x86/libwg-quick.so",
                    "lib/x86_64/libwg-quick.so",
                )
            useLegacyPackaging = true
        }
    }
    installation {
        installOptions("-g")
    }
}

dependencies {
    // Android
    implementation("androidx.appcompat:appcompat:${libs.versions.appcompat.get()}")
    implementation("com.google.android.material:material:${libs.versions.material.get()}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${libs.versions.archLifecycle.get()}")
    implementation("androidx.navigation:navigation-fragment-ktx:${libs.versions.navigation.get()}")
    implementation("androidx.navigation:navigation-ui-ktx:${libs.versions.navigation.get()}")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    // Core Module
    implementation(project(":base"))
    // Hilt
    implementation("com.google.dagger:hilt-android:${libs.versions.hilt.get()}")
    ksp("com.google.dagger:hilt-compiler:${libs.versions.hilt.get()}")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    // hilt-navigation-compose for hiltViewModel<VM>() in Compose (Phase 5).
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.test.espresso:espresso-intents:3.5.1")
    implementation("androidx.test.uiautomator:uiautomator:2.2.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    // Compose
    implementation("androidx.activity:activity-ktx:1.9.0")
    val composeBom = platform("androidx.compose:compose-bom:2025.11.01")
    implementation(composeBom)
    testImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material3:material3-adaptive-android:1.0.0-alpha06")
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Baseline Profile for startup optimization
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
}
