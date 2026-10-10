pluginManagement {
    // The convention plugins (PLANNING.md §1): every module's Android, Kotlin, Compose and Hilt
    // setup is written once, in build-logic/, and applied by id.
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "tempo"

// The module map is PLANNING.md §2. core:model and core:domain are pure Kotlin/JVM; feature
// modules depend on core:*, never on each other; :app wires everything together.
include(":app")
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:calendar")
include(":core:designsystem")
include(":core:testing")
include(":feature:today")
include(":feature:settings")
include(":feature:onboarding")
include(":feature:guide")
include(":widget")
