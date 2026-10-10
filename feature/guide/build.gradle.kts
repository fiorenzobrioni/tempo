// The guide (PLANNING.md §11 Phase 5): what Today and the widgets
// answer, where the events come from, who does what, and the things a screen cannot say out loud.
// Re-openable from Settings.
plugins {
    alias(libs.plugins.tempo.android.feature)
}

android {
    namespace = "com.callbackdev.tempo.feature.guide"
}

dependencies {
    testImplementation(libs.androidx.junit)
}
