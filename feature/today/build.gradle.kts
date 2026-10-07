plugins {
    alias(libs.plugins.tempo.android.feature)
}

android {
    namespace = "com.callbackdev.tempo.feature.today"
}

dependencies {
    // The day's instances, and the doors to the calendar app (view, new event).
    implementation(project(":core:calendar"))
    testImplementation(libs.androidx.junit)
}
