// The system calendar's Android half (PLANNING.md §4): the Calendar Provider read through the
// ContentResolver (Instances, Calendars), the change observer, the intents that open the
// reader's calendar app to view, create or edit an event. Read-only: READ_CALENDAR and nothing
// else. What the instances mean (a day's agenda, its sentence) is pure Kotlin, in core:domain.
plugins {
    alias(libs.plugins.tempo.android.library)
    alias(libs.plugins.tempo.android.hilt)
}

android {
    namespace = "com.callbackdev.tempo.core.calendar"
}

dependencies {
    api(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
