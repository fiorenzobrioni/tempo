plugins {
    alias(libs.plugins.tempo.android.feature)
}

android {
    namespace = "com.callbackdev.tempo.feature.settings"
}

dependencies {
    // The phone's calendars for the list, the permission, and their changes while on screen.
    implementation(project(":core:calendar"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.androidx.junit)
}
