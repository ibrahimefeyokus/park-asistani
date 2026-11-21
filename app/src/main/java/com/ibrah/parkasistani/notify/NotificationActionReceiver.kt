package com.ibrah.parkasistani.notify

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ibrah.parkasistani.park.ParkDeleter
import com.ibrah.parkasistani.timer.TimerScheduler
import com.ibrah.parkasistani.timer.TimerVibrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(Notifications.EXTRA_RECORD_ID, -1)
        when (intent.action) {
            Notifications.ACTION_STOP_ALERT -> {
                TimerVibrator.stop(context)
                if (id >= 0) context.getSystemService(NotificationManager::class.java)
                    .cancel(Notifications.timerNotificationId(id))
            }
            Notifications.ACTION_STOP_TIMER -> TimerScheduler(context).cancel()
            Notifications.ACTION_DELETE -> {
                if (id < 0) return
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try { ParkDeleter.get(context).delete(id) } finally { pending.finish() }
                }
            }
        }
    }
}
