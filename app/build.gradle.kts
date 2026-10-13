plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.maizemaster.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.maizemaster.app"
        minSdk = 26
        targetSdk = 35
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1.0.${System.getenv("GITHUB_RUN_NUMBER") ?: "1"}"
    }

    signingConfigs {
        create("permanent") {
            val keyFile = System.getenv("MAIZEMASTER_KEYSTORE_PATH")
            if (!keyFile.isNullOrBlank()) {
                storeFile = file(keyFile)
                storePassword = System.getenv("MAIZEMASTER_STORE_PASSWORD")
                keyAlias = System.getenv("MAIZEMASTER_KEY_ALIAS")
                keyPassword = System.getenv("MAIZEMASTER_KEY_PASSWORD")
                storeType = "pkcs12"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("permanent")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("permanent")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
}
