package com.callbackdev.tempo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * The application: Hilt's graph root. Nothing starts here on purpose: Tempo has no service and
 * no background work of its own (PLANNING.md §9). The widget's refresh is armed by the widget
 * itself (Phase 4), when there is a card on a home screen to refresh.
 */
@HiltAndroidApp
class TempoApplication : Application()
