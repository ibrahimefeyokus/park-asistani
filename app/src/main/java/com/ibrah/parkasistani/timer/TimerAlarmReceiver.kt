package com.ibrah.parkasistani.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ibrah.parkasistani.notify.Notifications

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(Notifications.EXTRA_RECORD_ID, -1)
        val minutesLeft = intent.getIntExtra(EXTRA_MINUTES_LEFT, 0)
        val store = ActiveTimerStore(context)
        // Kayıt silindiyse veya sayaç durdurulduysa sessiz kal.
        if (!TimerGuard.shouldNotify(store.get(), id)) return
        Notifications.showTimer(context, id, minutesLeft)
        TimerVibrator.start(context, VibrationPlan.forAlert(minutesLeft))
        if (minutesLeft == 0) { store.clear(); Notifications.cancelCountdown(context) }
    }

    companion object { const val EXTRA_MINUTES_LEFT = "minutesLeft" }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        TimerScheduler(context).rescheduleAll()
    }
}
