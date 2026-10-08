package com.callbackdev.tempo.core.data.widget

/** The two cards, as a screen outside the widgets' module names them (PLANNING.md §7). */
enum class TempoWidget {
    /** «Agenda»: the clock and the date over the rest of the day. */
    AGENDA,

    /** «In words»: what comes next, large, and the day in a line. */
    WORDS,
}

/**
 * The home screen's side of the widgets, for the screens that offer them (the first run's widget
 * page, Settings; PLANNING.md §8): whether the launcher can place one on request, and the request.
 * Implemented by `:widget`, which the feature modules cannot see.
 */
interface HomeScreenWidgets {
    /** Whether the launcher accepts a pin request (`AppWidgetManager.isRequestPinAppWidgetSupported`). */
    fun canPin(): Boolean

    /** Asks the launcher to place [widget]; it shows its own dialog, and the reader decides there. */
    fun pin(widget: TempoWidget): Boolean

    /** Whether a card of Tempo's is on a home screen now. */
    fun placed(): Boolean
}
