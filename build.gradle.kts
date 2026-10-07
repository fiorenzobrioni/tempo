// Root build file: the plugins are put on the classpath here and applied in the modules,
// through the convention plugins in build-logic/.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.spotless)
}

// Formatting (PLANNING.md §1): ktlint through Spotless, over every Kotlin source and build
// script in the repo, build-logic included. `./gradlew spotlessApply` fixes, CI runs
// `spotlessCheck`. The rules themselves live in .editorconfig, where the IDE reads them too.
spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**")
        ktlint(ktlintVersion)
    }
}
