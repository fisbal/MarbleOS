plugins {
    id("com.android.application")
}

val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.martinb.marbleos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.martinb.marbleos"
        minSdk = 24
        targetSdk = 34
        // +2000: above the MartinB hub factory builds (1000+), so this APK installs over them and keeps the saved game.
        versionCode = buildNumber + 2000
        versionName = "1.0.$buildNumber"
    }

    // Family build key: the workflow decodes the MARBLEOS_KEYSTORE_B64 secret into marbleos.jks and passes
    // the password as an env var, so every new APK installs over the previous one and keeps the saved game.
    // Never commit the key or its password.
    val keystore = file("marbleos.jks")
    signingConfigs {
        create("family") {
            storeFile = keystore
            storePassword = System.getenv("MARBLEOS_KEYSTORE_PASSWORD")
            keyAlias = "marbleos"
            keyPassword = System.getenv("MARBLEOS_KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // Pull requests build without the secret: they get the debug key and are never published.
            signingConfig = signingConfigs.getByName(if (keystore.exists()) "family" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.11.0")
}
