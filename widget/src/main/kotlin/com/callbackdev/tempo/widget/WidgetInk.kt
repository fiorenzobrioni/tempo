package com.callbackdev.tempo.widget

/**
 * Which set of inks a card writes with, named after the ground it is written on. Chiaro's rule
 * (`WidgetInk`, DESIGN §2.6), kept pure so a test pins it: [OVER_COLOR] is the white pair every
 * card colour is picked dark enough to carry; the other two are the schemes' own.
 */
enum class WidgetInk {
    OVER_COLOR,
    ON_LIGHT,
    ON_DARK,
    ;

    /** Whether the ground is dark, so semantic colours pick the set selected for dark. */
    val darkGround: Boolean get() = this != ON_LIGHT
}

/** Below this solidity the card stops being the ink's ground. */
const val INK_TRUST_FLOOR_PCT: Int = 50

/**
 * The whole ink rule. Above [INK_TRUST_FLOOR_PCT] the card is its own ground. Below it the card is
 * see-through: a light or dark card is the reader naming an ink, so it keeps deciding; the phone's
 * card and a colour hand the question to the wallpaper, whose hint is read as the affirmative
 * signal it is ([wallpaperCarriesDarkInk]: dark ink only where the system says the ground is
 * bright). [night] is the phone's mode, which only the phone's card at full solidity asks for.
 */
fun widgetInk(
    background: WidgetBackground,
    opacityPct: Int,
    night: Boolean,
    wallpaperCarriesDarkInk: Boolean,
): WidgetInk {
    if (opacityPct < INK_TRUST_FLOOR_PCT) {
        return when (background) {
            WidgetBackground.LIGHT -> WidgetInk.ON_LIGHT

            WidgetBackground.DARK -> WidgetInk.ON_DARK

            WidgetBackground.SYSTEM, WidgetBackground.COLOR ->
                if (wallpaperCarriesDarkInk) WidgetInk.ON_LIGHT else WidgetInk.ON_DARK
        }
    }
    return when (background) {
        WidgetBackground.COLOR -> WidgetInk.OVER_COLOR
        WidgetBackground.LIGHT -> WidgetInk.ON_LIGHT
        WidgetBackground.DARK -> WidgetInk.ON_DARK
        WidgetBackground.SYSTEM -> if (night) WidgetInk.ON_DARK else WidgetInk.ON_LIGHT
    }
}
