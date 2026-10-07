package com.callbackdev.tempo.debug

import android.content.Intent
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The debug build's crash page shows the trace it was given, word for word. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "it-rIT")
class CrashReportActivityTest {
    @Test
    fun `it shows the report`() {
        val report = "java.lang.IllegalStateException: boom\n\tat com.callbackdev.tempo.MainActivity.onCreate"
        val intent = Intent(ApplicationProvider.getApplicationContext(), CrashReportActivity::class.java)
            .putExtra(CrashReportActivity.EXTRA_REPORT, report)
        ActivityScenario.launch<CrashReportActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val texts = mutableListOf<CharSequence>()
                fun walk(view: android.view.View) {
                    if (view is TextView) texts += view.text
                    if (view is android.view.ViewGroup) for (i in 0 until view.childCount) walk(view.getChildAt(i))
                }
                walk(activity.window.decorView)
                assertThat(texts.map(CharSequence::toString)).containsAtLeast("Tempo si è fermato", report, "Condividi")
            }
        }
    }
}
