package com.callbackdev.tempo.core.model

/**
 * How a time is written (VISION.md, Settings). [SYSTEM] follows the phone's 24-hour switch, which
 * is what every other clock on the phone does, so it is the default; the other two override it
 * for Tempo alone, the app and the widget alike.
 */
enum class ClockFormat {
    SYSTEM,
    H24,
    H12,
}

/**
 * How today's date is written under the clock: [LONG] «Tuesday 7 October», [MEDIUM] «Tue 7 Oct»,
 * [NUMERIC] «07/10/2026», each in the order and the words of the reader's language.
 */
enum class DateStyle {
    LONG,
    MEDIUM,
    NUMERIC,
}
