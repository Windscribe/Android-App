plugins {
    id("com.android.library")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

android {
    namespace = "org.strongswan.android"
    compileSdk = rootProject.extra["appCompiledSdk"] as Int
    ndkVersion = "27.2.12479018"

    defaultConfig {
        minSdk = rootProject.extra["appMinSdk"] as Int

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    testOptions {
        targetSdk = rootProject.extra["appTargetSdk"] as Int
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }
}

dependencies {
    implementation(project(":common"))
    implementation("androidx.appcompat:appcompat:${libs.versions.appcompat.get()}")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.google.android.material:material:${libs.versions.material.get()}")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
