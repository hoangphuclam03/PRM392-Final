plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.prm392"
    compileSdk {
        version = release(36)
    }

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
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))

// --- Added for chat feature (Firestore, Storage, FirebaseUI, OkHttp, Glide) ---
implementation("com.google.firebase:firebase-firestore")
implementation("com.google.firebase:firebase-storage")
implementation("com.firebaseui:firebase-ui-firestore:8.0.2")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.auth)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    // removed: implementation(libs.firebase.auth) (using BoM below)
    implementation("com.google.firebase:firebase-database")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("com.google.android.material:material:1.12.0")



    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}