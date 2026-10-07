import com.callbackdev.tempo.buildlogic.library
import com.callbackdev.tempo.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A `feature:*` module: an Android library with Compose and Hilt, wired to the core modules.
 * Features depend on `core:*` and never on each other (PLANNING.md §2).
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("tempo.android.library")
            pluginManager.apply("tempo.android.compose")
            pluginManager.apply("tempo.android.hilt")

            dependencies {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:data"))
                add("implementation", project(":core:designsystem"))
                add("testImplementation", project(":core:testing"))

                add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            }
        }
    }
}
