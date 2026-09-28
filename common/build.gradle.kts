plugins {
    id("com.android.library")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.windscribe.common"
    compileSdk = rootProject.extra["appCompiledSdk"] as Int
    ndkVersion = "27.2.12479018"

    defaultConfig {
        minSdk = rootProject.extra["appMinSdk"] as Int

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        (project.findProperty("abiFilter") as String?)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.let { ndk.abiFilters += it }
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }
    testOptions {
        targetSdk = rootProject.extra["appTargetSdk"] as Int
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.15.0")
    // Binding comes from :base at runtime; this module only needs the API to log into the same
    // applog.txt the rest of the app writes.
    implementation("org.slf4j:slf4j-api:2.0.18")
    implementation("androidx.appcompat:appcompat:1.7.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    implementation("org.pcap4j:pcap4j-core:1.7.7")
    implementation("org.pcap4j:pcap4j-packetfactory-static:1.7.7")
    implementation("org.minidns:minidns-client:1.0.5")
}
