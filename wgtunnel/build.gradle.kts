plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 33
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    namespace = "com.wireguard.tunnel"
    defaultConfig {
        minSdk = 21
    }
    externalNativeBuild {
        cmake {
            path("tools/CMakeLists.txt")
        }
    }
    buildTypes {
        all {
            externalNativeBuild {
                cmake {
                    targets("libwg-go.so", "libwg.so")
                    arguments("-DGRADLE_USER_HOME=${project.gradle.gradleUserHomeDir}")
                }
            }
        }
        release {
            externalNativeBuild {
                cmake {
                    arguments("-DANDROID_PACKAGE_NAME=com.wireguard.tunnel")
                }
            }
        }
        debug {
            externalNativeBuild {
                cmake {

                }
            }
        }
    }
    lint {
        disable += "LongLogTag"
        disable += "NewApi"
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.7.0")
    implementation("androidx.collection:collection:1.3.0")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    testImplementation("junit:junit:4.13.2")
    implementation(project(":common"))
}