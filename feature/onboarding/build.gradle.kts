plugins {
    alias(libs.plugins.tempo.android.feature)
}

android {
    namespace = "com.callbackdev.tempo.feature.onboarding"
}

dependencies {
    // The calendar permission's state, asked for on the first run.
    implementation(project(":core:calendar"))
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.androidx.junit)
}
