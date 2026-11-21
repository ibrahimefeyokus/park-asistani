package com.ibrah.parkasistani.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ibrah.parkasistani.MainActivity
import com.ibrah.parkasistani.data.ParkRepository
import com.ibrah.parkasistani.data.ParkSource
import com.ibrah.parkasistani.notify.Notifications
import com.ibrah.parkasistani.park.ParkSaver
import com.ibrah.parkasistani.park.SaveResult
import com.ibrah.parkasistani.ui.formatClock

class ParkWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val latest = ParkRepository.get(context).latest()
        val label = latest?.let { "Son park: ${formatClock(it.savedAt)}" } ?: "Kayıt yok"
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(16.dp).padding(8.dp).clickable(actionStartActivity<MainActivity>()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(text = "Park ettim", onClick = actionRunCallback<SaveParkAction>())
                    Text(label, style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurface),
                        modifier = GlanceModifier.padding(top = 4.dp))
                }
            }
        }
    }
}

class SaveParkAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val r = ParkSaver.get(context).save(ParkSource.MANUAL, null)
        if (r is SaveResult.Saved) Notifications.showSaved(context, r.id, r.hasLocation)
        WidgetUpdater.update(context)
    }
}

class ParkWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ParkWidget()
}
