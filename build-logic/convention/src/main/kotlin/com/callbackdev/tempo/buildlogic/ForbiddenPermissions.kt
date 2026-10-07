package com.callbackdev.tempo.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The permissions Tempo must never hold (PLANNING.md §10), as patterns over the full name.
 *
 * Privacy and battery are enforced here rather than promised: no network (so no event can
 * leave the device), no location, nothing that lets the app escape Doze or wake the CPU on a
 * schedule. And Tempo only reads the calendar: no permission to write it, none to read the
 * contacts behind an event's guests, no sensors it has no use for. A library that brings one of
 * these in must be removed, or the permission stripped in the app manifest with
 * `tools:node="remove"`.
 */
val ForbiddenPermissionPatterns: List<String> =
    listOf(
        "android\\.permission\\.INTERNET",
        "android\\.permission\\.ACCESS_[A-Z_]*LOCATION",
        "android\\.permission\\.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        "android\\.permission\\.SCHEDULE_EXACT_ALARM",
        "android\\.permission\\.USE_EXACT_ALARM",
        "android\\.permission\\.WRITE_CALENDAR",
        "android\\.permission\\.(READ|WRITE)_CONTACTS",
        "android\\.permission\\.HIGH_SAMPLING_RATE_SENSORS",
        "android\\.permission\\.BODY_SENSORS[A-Z_]*",
    )

/**
 * Reads a MERGED manifest (the app's own plus every library's) and fails the build if it
 * requests a forbidden permission. Wired to `check` and run by CI on every push, so adding
 * `INTERNET` to any manifest in the dependency graph turns the build red.
 */
@CacheableTask
abstract class CheckForbiddenPermissionsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val forbiddenPatterns: ListProperty<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val requested = requestedPermissions()
        val patterns = forbiddenPatterns.get().map(::Regex)
        val offending = requested.filter { permission -> patterns.any { it.matches(permission) } }

        report.get().asFile.writeText(
            buildString {
                appendLine("Requested permissions (${requested.size}):")
                requested.forEach { appendLine("  $it") }
                appendLine("Forbidden found: ${if (offending.isEmpty()) "none" else offending.joinToString()}")
            },
        )

        if (offending.isNotEmpty()) {
            throw GradleException(
                "The merged manifest requests forbidden permission(s): ${offending.joinToString()}.\n" +
                    "Tempo holds no network, location, exact-alarm, battery-exemption, calendar-writing, " +
                    "contacts or body-sensor " +
                    "permission (PLANNING.md §10). Remove the dependency that adds it, or strip it in " +
                    "app/src/main/AndroidManifest.xml with tools:node=\"remove\".\n" +
                    "Manifest: ${mergedManifest.get().asFile}",
            )
        }
    }

    private fun requestedPermissions(): List<String> {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(mergedManifest.get().asFile)
        return PermissionTags.flatMap { tag ->
            val nodes = document.getElementsByTagName(tag)
            (0 until nodes.length).mapNotNull { index ->
                nodes.item(index).attributes
                    ?.getNamedItemNS(ANDROID_NAMESPACE, "name")
                    ?.nodeValue
            }
        }.distinct().sorted()
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        val PermissionTags = listOf("uses-permission", "uses-permission-sdk-23")
    }
}
