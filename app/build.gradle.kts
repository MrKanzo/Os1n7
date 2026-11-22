plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.mrkanzo.os1n7"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.mrkanzo.os1n7"
        minSdk = 26
        targetSdk = 35
        versionCode = 38
        versionName = "0.7.4"
    }
    signingConfigs {
        create("production") {
            storeFile = file(System.getenv("OS1N7_KEYSTORE_PATH") ?: "os1n7-release.jks")
            storePassword = System.getenv("OS1N7_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("OS1N7_KEY_ALIAS") ?: "os1n7"
            keyPassword = System.getenv("OS1N7_KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("production")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
