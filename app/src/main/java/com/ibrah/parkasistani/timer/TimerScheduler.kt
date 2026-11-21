package com.ibrah.parkasistani.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ibrah.parkasistani.data.ParkRepository
import com.ibrah.parkasistani.notify.Notifications

/** Aynı anda tek bir park sayacı vardır (ActiveTimerStore); sabit istek kodları yenisinin eskisinin yerine geçmesini sağlar. */
class TimerScheduler(private val context: Context, private val repo: ParkRepository = ParkRepository.get(context)) {
    private val am = context.getSystemService(AlarmManager::class.java)
    private val store = ActiveTimerStore(context)

    /** Kayıt yoksa false döner. */
    suspend fun start(recordId: Long, durationMin: Int): Boolean {
        val record = repo.get(recordId) ?: return false
        val now = System.currentTimeMillis()
        val endAt = now + durationMin * 60_000L
        repo.update(record.copy(timerEndAt = endAt))
        store.set(ActiveTimer(recordId, endAt, startAt = now))
        schedule(recordId, endAt)
        return true
    }

    fun cancel() {
        cancelAlarms()
        store.clear()
    }

    fun rescheduleAll() {
        val t = store.get() ?: return
        if (t.endAt <= System.currentTimeMillis()) { store.clear(); return }
        schedule(t.recordId, t.endAt)
    }

    private fun schedule(recordId: Long, endAt: Long) {
        cancelAlarms()
        CountdownSpec.from(store.get(), System.currentTimeMillis())?.let { Notifications.showCountdown(context, it) }
        for (alarm in TimerPlan.alarms(endAt, System.currentTimeMillis())) {
            val pi = pendingIntent(recordId, alarm.minutesLeft)
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.at, pi)
            }
        }
    }

    /** Alarmları ve bildirim çubuğundaki geri sayımı kaldırır. */
    fun cancelAlarms() {
        listOf(TimerPlan.WARNING_MIN, 0).forEach { am.cancel(pendingIntent(0, it)) }
        Notifications.cancelCountdown(context)
    }

    private fun pendingIntent(recordId: Long, minutesLeft: Int): PendingIntent {
        val i = Intent(context, TimerAlarmReceiver::class.java)
            .putExtra(Notifications.EXTRA_RECORD_ID, recordId)
            .putExtra(TimerAlarmReceiver.EXTRA_MINUTES_LEFT, minutesLeft)
        val code = if (minutesLeft > 0) REQ_WARNING else REQ_END
        return PendingIntent.getBroadcast(context, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val REQ_WARNING = 7001
        const val REQ_END = 7002
    }
}
