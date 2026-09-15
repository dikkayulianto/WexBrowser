plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.notebrowser"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vex.browser"
        minSdk = 21
        targetSdk = 34
        versionCode = 10
        versionName = "2.0"
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
        }
        create("release") {
            storeFile = file("${rootDir}/keystore/vex-release-key.jks")
            storePassword = "VexBrowser2026!"
            keyAlias = "vexkey"
            keyPassword = "VexBrowser2026!"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.webkit:webkit:1.10.0")
    implementation("com.google.android.gms:play-services-ads:23.0.0")
}
