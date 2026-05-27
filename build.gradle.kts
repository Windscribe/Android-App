// Top-level build file where you can add configuration options common to all sub-projects/modules.
extra["AppName"] = "Windscribe"
extra["AppId"] = "com.windscribe.vpn"
extra["appMinSdk"] = 21
extra["appTargetSdk"] = 35
extra["appCompiledSdk"] = 35
extra["appBuildTool"] = "35.0.0"
extra["appVersionCode"] = 2410
extra["appVersionName"] = "4.1"
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
        classpath("com.android.tools.build:gradle:8.6.1")
        classpath("org.owasp:dependency-check-gradle:8.4.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.0.0-1.0.24")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.51.1")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:12.1.2")
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
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
