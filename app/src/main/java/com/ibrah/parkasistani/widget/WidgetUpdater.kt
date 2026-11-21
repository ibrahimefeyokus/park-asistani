package com.ibrah.parkasistani.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object WidgetUpdater {
    suspend fun update(context: Context) {
        runCatching { ParkWidget().updateAll(context) }
    }
}
