// DataStore (the reader's settings, the widgets' looks), behind repositories that expose Flow
// (PLANNING.md §5). Tempo stores no event: the system calendar is the only truth, read by
// :core:calendar. No Room until something worth a table appears (PLANNING.md §15).
plugins {
    alias(libs.plugins.tempo.android.library)
    alias(libs.plugins.tempo.android.hilt)
}

android {
    namespace = "com.callbackdev.tempo.core.data"
}

dependencies {
    api(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
}
