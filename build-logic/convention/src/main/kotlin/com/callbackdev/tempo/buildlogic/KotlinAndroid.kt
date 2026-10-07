package com.callbackdev.tempo.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * What every Android module shares: SDK levels, Java 21, the source layout and the test
 * libraries. Kotlin is compiled by AGP itself (AGP 9's built-in Kotlin), so there is no
 * kotlin-android plugin to apply.
 */
internal fun Project.configureKotlinAndroid(android: CommonExtension) {
    android.apply {
        compileSdk = TempoSdk.COMPILE
        defaultConfig.minSdk = TempoSdk.MIN
        defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        compileOptions.sourceCompatibility = TempoJavaVersion
        compileOptions.targetCompatibility = TempoJavaVersion

        // Robolectric (Room, receivers, the notification builder) reads the merged resources.
        testOptions.unitTests.isIncludeAndroidResources = true

        // Kotlin sources live under src/*/kotlin, as everywhere in the series.
        sourceSets.getByName("main").java.directories.add("src/main/kotlin")
        sourceSets.getByName("test").java.directories.add("src/test/kotlin")
        sourceSets.getByName("androidTest").java.directories.add("src/androidTest/kotlin")
    }
    configureKotlinCompile()

    // Hilt's aggregating task generates test-source stubs in every module it is applied to, so a
    // module with Hilt and no tests yet (the Phase 0 skeletons) reads to Gradle 9 as "tests
    // present, none found". The real guard is the tests themselves, written with the code.
    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests.set(false)
    }

    // The README's screenshots (docs/screenshots) are drawn by the `ReadmeScreenshots` tests,
    // and only on request: `./gradlew test -PupdateScreenshots`. Without the property those
    // tests are skipped, so an ordinary run never rewrites a committed image.
    if (providers.gradleProperty("updateScreenshots").isPresent) {
        val screenshots = rootProject.layout.projectDirectory.dir("docs/screenshots").asFile.absolutePath
        tasks.withType<Test>().configureEach {
            systemProperty("tempo.readmeScreenshots", screenshots)
            outputs.upToDateWhen { false }
        }
    }

    dependencies {
        add("testImplementation", libs.library("junit"))
        add("testImplementation", libs.library("truth"))
        add("testImplementation", libs.library("turbine"))
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
    }
}

internal fun Project.configureKotlinCompile() {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(TempoJvmTarget)
        }
    }
}
