package com.callbackdev.tempo.core.model

/** Light, dark, or whatever the phone says: the reader's choice, the phone's by default. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Which of the two dresses the app wears when it is not wearing the wallpaper's:
 * [PAPER], warm white and amber; [VIVID], cool white and azure, the default.
 */
enum class AppPalette {
    PAPER,
    VIVID,
}

/**
 * The typeface: two bundled faces, the same drawing on every phone, and
 * the phone's own sans as the third answer.
 */
enum class AppFont {
    GOOGLE_SANS,
    INTER,
    SYSTEM,
}
