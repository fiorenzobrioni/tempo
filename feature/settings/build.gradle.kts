plugins {
    alias(libs.plugins.tempo.android.feature)
}

android {
    namespace = "com.callbackdev.tempo.feature.settings"
}

dependencies {
    testImplementation(libs.androidx.junit)
}
