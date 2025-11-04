plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.prm392"
    compileSdk = 34  // Dùng số trực tiếp, không cần "version = release(36)"

    defaultConfig {
        applicationId = "com.example.prm392"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // AndroidX cơ bản
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase Authentication
    implementation(libs.firebase.auth)

    // ✅ Firestore Database (thay cho Realtime)
    implementation("com.google.firebase:firebase-firestore:25.0.0")

    // Navigation Drawer
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("com.google.android.material:material:1.12.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
