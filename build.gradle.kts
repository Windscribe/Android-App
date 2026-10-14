// Top-level build file where you can add configuration options common to all sub-projects/modules.
extra["AppName"] = "Windscribe"
extra["AppId"] = "com.windscribe.vpn"
extra["appMinSdk"] = 24
extra["appTargetSdk"] = 36
extra["appCompiledSdk"] = 36
extra["appBuildTool"] = "36.0.0"
extra["appVersionCode"] = 2384
extra["appVersionName"] = "4.4"
extra["java"] = "17"

buildscript {
    // Plugin classpath versions live here as literals: buildscript {} cannot read
    // the version catalog (libs.*). Library versions are in gradle/libs.versions.toml.
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.9")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.59.2")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:14.2.0")
        // Baseline profile plugin — version must track gradle/libs.versions.toml baselineProfilePlugin.
        // 1.5.0-alpha is required for AGP 9 (1.4.x fails: "TestExtension does not exist").
        classpath("androidx.benchmark:benchmark-baseline-profile-gradle-plugin:1.5.0-alpha06")
    }
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.3.1")
        android.set(true)
    }
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://jitpack.io") }
        // wsnet has to resolve as a module rather than a file dependency: AGP refuses to
        // package a library that has a direct local .aar file dependency. Declared for
        // every project because :base exposes wsnet on its consumers' runtime classpath.
        flatDir {
            dirs("$rootDir/base/src/main/libs")
            // flatDir carries no metadata or checksums, so it stays confined to the one artifact
            // it exists for rather than standing as a candidate for every coordinate.
            content { includeModule("", "wsnet") }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
