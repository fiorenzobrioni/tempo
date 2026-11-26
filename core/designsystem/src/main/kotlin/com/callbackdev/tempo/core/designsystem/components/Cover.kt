package com.callbackdev.tempo.core.designsystem.components

/**
 * Marks a bar drawn over a scrolling page, which the page's rows pass under: Today's compact bar.
 * What it covers of a row is not on screen and takes no touch, as what a list's edge cuts; the UI
 * tests' accessibility checks (`:core:testing`) measure such a row when it is scrolled clear of
 * the bar, and one wholly under it not at all.
 */
const val COVER_TAG = "tempo_cover"
