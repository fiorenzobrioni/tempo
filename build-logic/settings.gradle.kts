// The convention plugins (PLANNING.md §1-§2), an included build so that every module's setup
// is written once. It reads the same version catalog as the main build.
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
