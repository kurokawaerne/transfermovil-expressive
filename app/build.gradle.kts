plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

// Release signing comes from the environment (GitHub Actions secrets);
// builds without the variables stay debug-signed.
val tmKeystoreFile = System.getenv("TM_KEYSTORE_FILE")

android {
    namespace = "dev.kurokawa.transfermovil.expressive"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "dev.kurokawa.transfermovil.expressive"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }
    signingConfigs {
        if (tmKeystoreFile != null) {
            create("release") {
                storeFile = file(tmKeystoreFile)
                storePassword = System.getenv("TM_STORE_PASSWORD")
                keyAlias = System.getenv("TM_KEY_ALIAS") ?: "tmex"
                keyPassword = System.getenv("TM_KEY_PASSWORD") ?: System.getenv("TM_STORE_PASSWORD")
            }
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (tmKeystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.compose.material3)
    implementation(libs.material.symbols.outlined)
    implementation(libs.material.symbols.outlined.filled)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.zxing.core)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)
}
