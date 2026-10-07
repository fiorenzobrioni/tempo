plugins {
    alias(libs.plugins.tempo.android.feature)
}

android {
    namespace = "com.callbackdev.tempo.feature.today"
}

dependencies {
    // The day's instances, the changes while on screen, and the doors to the calendar app.
    implementation(project(":core:calendar"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.androidx.junit)
}
