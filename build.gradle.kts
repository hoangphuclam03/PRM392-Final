// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    // Plugin Android (giữ nguyên alias)
    alias(libs.plugins.android.application) apply false

    // Plugin Google Services (Firebase)
    id("com.google.gms.google-services") version "4.4.2" apply false
}
