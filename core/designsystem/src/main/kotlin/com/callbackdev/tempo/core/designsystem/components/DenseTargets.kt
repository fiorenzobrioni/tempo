package com.callbackdev.tempo.core.designsystem.components

/**
 * Marks targets packed tighter than a finger's 48dp on purpose: a chart's bars, a week of a
 * month's days, Material's clock dial (its hours stand on a circle and follow a drag). Each is
 * still its own labelled node, reached one by one by swiping in TalkBack; the UI tests'
 * accessibility checks (`:core:testing`) measure everything else, and these for their labels.
 */
const val DENSE_TARGETS_TAG = "tempo_dense_targets"
