package com.callbackdev.tempo.debug

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Debug builds only: catches an uncaught exception and shows its stack trace in
 * [CrashReportActivity], in a process of its own, instead of the system's bare "Tempo keeps
 * stopping". Installed by a provider so a crash in the Application or in Hilt's graph is caught
 * too. Not a crash reporter: nothing is sent anywhere; the reader shares the text if they choose.
 */
class CrashCatcher : ContentProvider() {
    override fun onCreate(): Boolean {
        val context = context ?: return false
        // Under Robolectric a caught crash would end the test JVM, not show a page.
        if (Build.FINGERPRINT == "robolectric") return true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            val shown = runCatching { show(context, report(context, thread, error)) }.isSuccess
            if (shown) {
                Process.killProcess(Process.myPid())
                exitProcess(EXIT_CODE)
            } else {
                previous?.uncaughtException(thread, error)
            }
        }
        return true
    }

    private fun show(context: Context, report: String) {
        context.startActivity(
            Intent(context, CrashReportActivity::class.java)
                .putExtra(CrashReportActivity.EXTRA_REPORT, report)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
    }

    private fun report(context: Context, thread: Thread, error: Throwable): String {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
        // The binder caps an extra's size; the top of a trace is what names the cause.
        return buildString {
            appendLine("Tempo $version (${context.packageName})")
            appendLine(
                "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            )
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(trace.take(MAX_REPORT))
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        0

    private companion object {
        const val EXIT_CODE = 10
        const val MAX_REPORT = 60_000
    }
}
