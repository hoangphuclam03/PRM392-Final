plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.prm392"
    compileSdk = 36

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
    // ✅ Firebase BoM controls versions automatically
    implementation(platform(libs.firebase.bom))

    // Firebase core services
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.google.firebase.storage)
    implementation(libs.google.firebase.database)
    implementation(libs.firebase.messaging)

    // FirebaseUI (Firestore adapter)
    implementation(libs.firebase.ui.firestore)

    // OkHttp (network sync)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Glide (for image loading)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    // AndroidX core + Material
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.drawerlayout)
    implementation(libs.guava)
    // WorkManager
    implementation(libs.work.runtime)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
