package com.callbackdev.tempo.debug

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.callbackdev.tempo.R

/**
 * Test builds only: the stack trace [CrashCatcher] caught, selectable, with a button to copy it
 * and one to share it. Plain views, no Compose and no Hilt, so it draws whatever broke the app.
 */
class CrashReportActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = intent.getStringExtra(EXTRA_REPORT).orEmpty()
        val gap = (16 * resources.displayMetrics.density).toInt()

        val title = TextView(this).apply {
            setText(R.string.debug_crash_title)
            textSize = TITLE_SP
            setTypeface(typeface, Typeface.BOLD)
        }
        val body = TextView(this).apply {
            setText(R.string.debug_crash_body)
            setPadding(0, gap / 2, 0, gap)
        }
        val trace = TextView(this).apply {
            text = report
            typeface = Typeface.MONOSPACE
            textSize = TRACE_SP
            setTextIsSelectable(true)
        }
        val copy = Button(this).apply {
            setText(R.string.debug_crash_copy)
            setOnClickListener {
                getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText(getString(R.string.debug_crash_title), report))
                Toast.makeText(this@CrashReportActivity, R.string.debug_crash_copied, Toast.LENGTH_SHORT).show()
            }
        }
        val share = Button(this).apply {
            setText(R.string.debug_crash_share)
            setOnClickListener {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report)
                startActivity(Intent.createChooser(send, getString(R.string.debug_crash_share)))
            }
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(copy)
            addView(share)
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(gap, gap, gap, gap)
            addView(title)
            addView(body)
            addView(buttons)
            addView(trace, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = gap })
        }
        val page = ScrollView(this).apply {
            addView(column)
            // Edge to edge is enforced from Android 15: keep the text clear of the bars.
            setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }
        setContentView(page)
    }

    companion object {
        const val EXTRA_REPORT = "com.callbackdev.tempo.debug.extra.REPORT"
        private const val TITLE_SP = 22f
        private const val TRACE_SP = 12f
    }
}
