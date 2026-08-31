plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.floydwiz.googlemdm"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.floydwiz.googlemdm"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Local dev EMM backend. Reached via `adb reverse tcp:8080 tcp:8080`
        // so the device/emulator can use the literal "localhost" hostname
        // the backend's dev TLS cert and CORS/host checks expect.
        buildConfigField("String", "EMM_BASE_URL", "\"https://localhost:8080/\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.1.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    //Dependencies for EMM
    //Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    //Navigation
    implementation("androidx.navigation:navigation-compose:2.9.4")

    //Hilt
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-compiler:2.59.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    //WorkManager
    implementation("androidx.work:work-runtime-ktx:2.11.0")
    implementation("androidx.hilt:hilt-work:1.3.0")
    ksp("androidx.hilt:hilt-compiler:1.3.0")

    //Secure storage (Keystore-backed EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    //Timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    //Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    //Retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    //OkHttp
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")

    //Android Management API SDK
    implementation("com.google.android.libraries.enterprise.amapi:amapi:1.8.2")

    //QR code scanning for enrollment (pure Java/Camera1-2, no Play Services dependency)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}