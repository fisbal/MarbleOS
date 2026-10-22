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
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    // Family build key: kept in the repo so every new APK installs over the previous one.
    signingConfigs {
        create("family") {
            storeFile = file("marbleos.jks")
            storePassword = "marbleos2026"
            keyAlias = "marbleos"
            keyPassword = "marbleos2026"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("family")
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
