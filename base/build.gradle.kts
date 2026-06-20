plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
}
apply(from = "$rootDir/config/config.gradle")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    ndkVersion = "27.2.12479018"
    compileSdk = rootProject.extra["appCompiledSdk"] as Int
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    defaultConfig {
        minSdk = rootProject.extra["appMinSdk"] as Int
    }
    testOptions {
        targetSdk = rootProject.extra["appTargetSdk"] as Int
    }
    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
        maybeCreate("fdroid").jniLibs.srcDirs("/frdroid/src/libs", "libs")
        maybeCreate("google").jniLibs.srcDirs("/google/src/libs", "libs")
    }
    packaging {
        jniLibs {
            excludes +=
                setOf(
                    "lib/arm64-v8a/libwg-quick.so",
                    "lib/armeabi-v7a/libwg-quick.so",
                    "lib/x86/libwg-quick.so",
                    "lib/x86_64/libwg-quick.so",
                )
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += setOf("MissingTranslation", "NonConstantResourceId", "VectorRaster", "ContentDescription")
    }
    namespace = "com.windscribe.vpn"
    flavorDimensions += "dim"
    productFlavors {
        create("fdroid") {
            dimension = "dim"
            buildConfigField("String", "CLOUD_PROJECT_NUMBER", "\"\"")
        }
        create("google") {
            dimension = "dim"
            // Play Integrity API cloud project number
            val cloudProjectNumber = System.getenv("CLOUD_PROJECT_NUMBER") ?: ""
            buildConfigField("String", "CLOUD_PROJECT_NUMBER", "\"${cloudProjectNumber}\"")
        }
    }
}

dependencies {
    // Android
    "api"("androidx.appcompat:appcompat:${libs.versions.appcompat.get()}")
    "api"("androidx.multidex:multidex:2.0.1")
    "api"("androidx.security:security-crypto:1.1.0")
    // VPN
    implementation(project(":openvpn"))
    implementation(project(":strongswan"))
    implementation(project(":common"))
    "api"(project(":wgtunnel"))
    // Gson
    "api"("com.google.code.gson:gson:2.14.0")
    // Okhttp
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.0")
    "api"("com.squareup.okhttp3:mockwebserver:5.3.0")
    // Logging
    "api"("org.slf4j:slf4j-api:2.0.18")
    "api"("com.github.tony19:logback-android:3.0.0")
    // 6.x+ requires minSdk 26 via LMAX disruptor (MethodHandle.invoke).
    implementation("net.logstash.logback:logstash-logback-encoder:5.0") {
        exclude(group = "ch.qos.logback", module = "logback-core")
    }
    implementation("androidx.lifecycle:lifecycle-process:${libs.versions.archLifecycle.get()}")
    // Room
    "api"("androidx.room:room-ktx:${libs.versions.room.get()}")
    "api"("androidx.room:room-runtime:${libs.versions.room.get()}")
    ksp("androidx.room:room-compiler:${libs.versions.room.get()}")
    implementation("androidx.room:room-testing:${libs.versions.room.get()}")
    androidTestImplementation("androidx.room:room-testing:${libs.versions.room.get()}")
    // Work manager
    "api"("androidx.work:work-runtime:${libs.versions.workManager.get()}")
    implementation("androidx.work:work-runtime-ktx:${libs.versions.workManager.get()}")
    androidTestImplementation("androidx.work:work-testing:${libs.versions.workManager.get()}")
    // Hilt
    implementation("com.google.dagger:hilt-android:${libs.versions.hilt.get()}")
    ksp("com.google.dagger:hilt-compiler:${libs.versions.hilt.get()}")
    implementation("androidx.hilt:hilt-work:1.3.0")
    ksp("androidx.hilt:hilt-compiler:1.3.0")
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.coroutines.get()}")
    // DataStore (replaces Tray)
    "api"("androidx.datastore:datastore-preferences:1.2.1")
    "api"("androidx.datastore:datastore-preferences-core:1.2.1")
    // Supporting libraries
    "api"("com.github.seancfoley:ipaddress:5.6.1")
    // Test
    testImplementation("junit:junit:${libs.versions.junit.get()}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.coroutines.get()}")
    testImplementation("io.mockk:mockk:1.14.7")
    // Google only dependencies
    "googleApi"("com.google.firebase:firebase-messaging:${libs.versions.firebase.get()}")
    "googleApi"(files("$projectDir/src/google/libs/in-app-purchasing-2.0.76.jar"))
    implementation(files("$projectDir/src/main/libs/wsnet.aar"))
    "googleApi"("com.android.billingclient:billing:9.0.0")
    "googleApi"("com.google.android.gms:play-services-appset:16.0.2")
    "googleApi"("com.google.android.play:review-ktx:2.0.2")
    "googleApi"("com.google.android.gms:play-services-auth:21.4.0")
    "googleApi"("com.google.android.play:integrity:1.4.0")
}
