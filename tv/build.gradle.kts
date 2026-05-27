/*
 * Copyright (c) 2021 Windscribe Limited.
 */
plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
}
apply(from = "$rootDir/config/config.gradle")
apply(from = "$rootDir/depedencycheck.gradle")

android {
    namespace = "com.windscribe.tv"
    ndkVersion = "27.2.12479018"
    bundle {
        language {
            enableSplit = false
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileSdk = rootProject.extra["appCompiledSdk"] as Int
    defaultConfig {
        applicationId = rootProject.extra["AppId"] as String
        minSdk = rootProject.extra["appMinSdk"] as Int
        targetSdk = rootProject.extra["appTargetSdk"] as Int
        versionName = System.getenv().getOrDefault("VERSION_NAME", rootProject.extra["appVersionName"] as String)
        versionCode = System.getenv("VERSION_CODE")?.toInt() ?: ((rootProject.extra["appVersionCode"] as Int) + 1)
        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            manifestPlaceholders["networkSecurityConfig"] = "@xml/network_security_config"
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
        getByName("debug") {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["networkSecurityConfig"] = "@xml/test_network_security_config"
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
            excludes += setOf(
                "lib/arm64-v8a/libwg-quick.so",
                "lib/armeabi-v7a/libwg-quick.so",
                "lib/x86/libwg-quick.so",
                "lib/x86_64/libwg-quick.so"
            )
            useLegacyPackaging = true
        }
    }
}

dependencies {
    //Android
    implementation("androidx.appcompat:appcompat:${libs.versions.appcompat.get()}")
    implementation("com.google.android.material:material:${libs.versions.material.get()}")
    implementation("androidx.leanback:leanback:${libs.versions.leanback.get()}")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    //Core
    implementation(project(":base"))
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${libs.versions.archLifecycle.get()}")
    // Hilt
    implementation("com.google.dagger:hilt-android:${libs.versions.hilt.get()}")
    ksp("com.google.dagger:hilt-compiler:${libs.versions.hilt.get()}")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    //Glide (runtime only — no @GlideModule, compiler not needed)
    implementation("com.github.bumptech.glide:glide:${libs.versions.glide.get()}")

    // Baseline Profile for startup optimization
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
}
