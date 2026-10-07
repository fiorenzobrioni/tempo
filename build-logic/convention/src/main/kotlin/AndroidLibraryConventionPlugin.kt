import com.android.build.api.dsl.LibraryExtension
import com.callbackdev.tempo.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Every Android library module: `core:data`, `core:tracking`, `core:designsystem`, features, widget. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                // Library modules are shrunk as part of the app; their own keep rules travel
                // with them as consumer rules.
                defaultConfig.consumerProguardFiles("consumer-rules.pro")
                // A library's lint runs as part of `:app:lintDebug` (checkDependencies).
                lint.abortOnError = true
            }
        }
    }
}
