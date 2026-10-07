import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.callbackdev.tempo.buildlogic.CheckForbiddenPermissionsTask
import com.callbackdev.tempo.buildlogic.ForbiddenPermissionPatterns
import com.callbackdev.tempo.buildlogic.TempoSdk
import com.callbackdev.tempo.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/** `:app`: the Android application, plus the forbidden-permission gate on its merged manifest. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = TempoSdk.TARGET
                lint {
                    // One `:app:lintDebug` covers every module the app is built from.
                    checkDependencies = true
                    abortOnError = true
                }
            }

            // One check per variant, on the manifest that actually ships: the merged one.
            val gate = tasks.register("checkForbiddenPermissions") {
                group = "verification"
                description = "Fails if any merged manifest requests a forbidden permission (PLANNING.md §10)."
            }
            extensions.configure<ApplicationAndroidComponentsExtension> {
                onVariants { variant ->
                    val name = variant.name.replaceFirstChar { it.uppercase() }
                    val check = tasks.register<CheckForbiddenPermissionsTask>("check${name}ForbiddenPermissions") {
                        group = "verification"
                        description = "Checks the merged ${variant.name} manifest for forbidden permissions."
                        mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                        forbiddenPatterns.set(ForbiddenPermissionPatterns)
                        report.set(layout.buildDirectory.file("reports/forbidden-permissions/${variant.name}.txt"))
                    }
                    gate.configure { dependsOn(check) }
                }
            }
            tasks.named("check") { dependsOn(gate) }
        }
    }
}
