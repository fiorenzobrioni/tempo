// The home-screen widgets (Jetpack Glance, PLANNING.md §7): «Agenda» and «In words», with their
// clock and date, their receivers, the per-widget settings screen, and what refreshes them (the calendar's
// content-URI trigger, an inexact alarm at the next boundary). The card, its inks and its
// colours are Chiaro's and Passo's, so the family's widgets sit side by side as one.
plugins {
    alias(libs.plugins.tempo.android.library)
    alias(libs.plugins.tempo.android.compose)
    alias(libs.plugins.tempo.android.hilt)
}

android {
    namespace = "com.callbackdev.tempo.widget"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:calendar"))
    implementation(project(":core:designsystem"))
    testImplementation(project(":core:testing"))

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.androidx.junit)
}
