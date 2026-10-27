plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// CI signs every build with the one release key, so each GitHub release installs over the last.
// Without these variables (local builds) the debug key is used instead.
val releaseKeystore = System.getenv("RELEASE_KEYSTORE_FILE")?.let(::file)?.takeIf { it.exists() }
val releaseKeystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")

android {
    namespace = "com.tvapkmanager.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tvapkmanager.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.3.0"

        // Where SelfUpdater looks for newer releases.
        buildConfigField("String", "UPDATE_REPO", "\"albertmarkish/android-tv-apk-manager\"")
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = releaseKeystorePassword
                keyAlias = "apkmanager"
                keyPassword = releaseKeystorePassword
            }
        }
    }

    buildTypes {
        val releaseSigning = signingConfigs.findByName("release")
        release {
            isMinifyEnabled = false
            signingConfig = releaseSigning ?: signingConfigs.getByName("debug")
        }
        debug {
            if (releaseSigning != null) signingConfig = releaseSigning
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.tv.material)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
