// The Material 3 theme, typography and the shared components, for the app screens and the
// widget's settings (PLANNING.md §2): the design language of docs/adr/0002-design-language.md.
plugins {
    alias(libs.plugins.tempo.android.library)
    alias(libs.plugins.tempo.android.compose)
}

android {
    namespace = "com.callbackdev.tempo.core.designsystem"
}

dependencies {
    // The formatters (times and dates for the locale) and the settings the theme reads.
    api(project(":core:domain"))

    // The BOM is exported with the libraries, so a module that reaches Compose through this one
    // (the widget, for GlanceTheme's colors) resolves the same versions.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.material3)

    implementation(libs.androidx.core.ktx)

    testImplementation(libs.androidx.junit)
}
