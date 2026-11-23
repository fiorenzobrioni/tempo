plugins {
    alias(libs.plugins.tempo.android.feature)
}

android {
    namespace = "com.callbackdev.tempo.feature.month"
}

dependencies {
    // The month's instances, their changes while on screen, and the doors to the calendar app.
    implementation(project(":core:calendar"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.androidx.junit)
}
