plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.prm392"
    compileSdk = 36  // Dùng số trực tiếp, không cần "version = release(36)"

    defaultConfig {
        applicationId = "com.example.prm392"
        minSdk = 27
        targetSdk = 36
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
// WorkManager runtime
    implementation(libs.work.runtime)

    implementation(libs.guava)
    // AndroidX cơ bản
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase Authentication
    implementation(libs.firebase.auth)

    // ✅ Firestore Database (thay cho Realtime)
    implementation(libs.firebase.firestore)

    // Navigation Drawer
    implementation(libs.drawerlayout)
    implementation(libs.material)
    implementation(libs.work.runtime)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
