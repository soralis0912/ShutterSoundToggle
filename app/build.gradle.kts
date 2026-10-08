plugins {
    id("com.android.application")
}

// Signing comes from the environment: CI points STORE_FILE at the public AOSP
// testkey.jks, a tagged release at the key restored from secrets. Without it
// the release build falls back to the local debug key so that it still
// installs.
val keystorePath: String? = System.getenv("STORE_FILE")

android {
    namespace = "org.soralis.shuttersound"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.soralis.shuttersound"
        // MediaActionSound.mustPlayShutterSound() tells what to restore to.
        minSdk = 33
        targetSdk = 36
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0"
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("STORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
}
