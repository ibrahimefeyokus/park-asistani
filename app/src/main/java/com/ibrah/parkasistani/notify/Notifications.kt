package com.ibrah.parkasistani.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ibrah.parkasistani.MainActivity

object Notifications {
    const val CH_PARK = "park"
    /** Sessiz kanal: uyarı sesi yok, titreşimi TimerVibrator yönetir. Kanal ayarları değiştirilemediği için yeni kimlik. */
    const val CH_TIMER = "timer_vibrate"
    private const val CH_TIMER_OLD = "timer"
    const val ACTION_STOP_ALERT = "com.ibrah.parkasistani.STOP_ALERT"
    /** Bildirim çubuğundaki sessiz geri sayım. */
    const val CH_COUNTDOWN = "countdown"
    const val ACTION_STOP_TIMER = "com.ibrah.parkasistani.STOP_TIMER"
    private const val COUNTDOWN_ID = 300_000
    const val EXTRA_RECORD_ID = "recordId"
    const val EXTRA_OPEN_TIMER = "openTimer"
    const val ACTION_DELETE = "com.ibrah.parkasistani.DELETE_PARK"

    fun parkNotificationId(id: Long) = (id % 100_000).toInt() + 1
    fun timerNotificationId(id: Long) = (id % 100_000).toInt() + 200_000

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_PARK, "Park kayıtları", NotificationManager.IMPORTANCE_DEFAULT))
        nm.deleteNotificationChannel(CH_TIMER_OLD)
        nm.createNotificationChannel(
            NotificationChannel(CH_TIMER, "Park süresi", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
                description = "Süre bitimine 10 dk kala ve süre dolunca titreşimli uyarı"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_COUNTDOWN, "Kalan park süresi", NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                description = "Sayaç çalışırken bildirim çubuğunda kalan süre"
            }
        )
    }

    fun showCountdown(ctx: Context, spec: com.ibrah.parkasistani.timer.CountdownSpec) {
        ensureChannels(ctx)
        val stop = PendingIntent.getBroadcast(
            ctx, COUNTDOWN_ID,
            Intent(ctx, NotificationActionReceiver::class.java).setAction(ACTION_STOP_TIMER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val end = java.text.SimpleDateFormat("HH:mm", java.util.Locale.forLanguageTag("tr-TR")).format(java.util.Date(spec.endAt))
        val n = NotificationCompat.Builder(ctx, CH_COUNTDOWN)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Park süresi")
            .setContentText("$end'da bitiyor")
            .setSubText("Park Asistanı")
            .setWhen(spec.endAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setTimeoutAfter(spec.timeoutMs)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            // Kilit ekranında ayrıntı yok, yalnızca genel bir satır.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(lockScreenVersion(ctx, CH_COUNTDOWN, "Sayaç çalışıyor"))
            .setColor(0xFF1F5EFF.toInt())
            .setContentIntent(openAppIntent(ctx, spec.recordId, openTimer = false, requestCode = COUNTDOWN_ID))
            .addAction(0, "Durdur", stop)
            .build()
        post(ctx, COUNTDOWN_ID, n)
    }

    private fun lockScreenVersion(ctx: Context, channel: String, text: String) =
        NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Park Asistanı")
            .setContentText(text)
            .build()

    fun cancelCountdown(ctx: Context) =
        ctx.getSystemService(NotificationManager::class.java).cancel(COUNTDOWN_ID)

    fun savedText(hasLocation: Boolean) = if (hasLocation) "Park yeri kaydedildi" else "Konum alınamadı, not ekle"

    fun timerText(minutesLeft: Int) =
        if (minutesLeft > 0) "Park süren $minutesLeft dakika içinde doluyor" else "Park süren doldu"

    fun openAppIntent(ctx: Context, id: Long, openTimer: Boolean, requestCode: Int): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_RECORD_ID, id)
            .putExtra(EXTRA_OPEN_TIMER, openTimer)
        return PendingIntent.getActivity(ctx, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun showSaved(ctx: Context, id: Long, hasLocation: Boolean) {
        ensureChannels(ctx)
        val nid = parkNotificationId(id)
        val delete = PendingIntent.getBroadcast(
            ctx, nid,
            Intent(ctx, NotificationActionReceiver::class.java).setAction(ACTION_DELETE).putExtra(EXTRA_RECORD_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CH_PARK)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(savedText(hasLocation))
            .setContentText("Arabana dönmek için dokun")
            .setContentIntent(openAppIntent(ctx, id, openTimer = false, requestCode = nid))
            .addAction(0, "Süre başlat", openAppIntent(ctx, id, openTimer = true, requestCode = nid + 500_000))
            .addAction(0, "Yanlış, sil", delete)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(lockScreenVersion(ctx, CH_PARK, "Yeni bildirim"))
            .setAutoCancel(true)
            .build()
        post(ctx, nid, n)
    }

    fun showTimer(ctx: Context, id: Long, minutesLeft: Int) {
        ensureChannels(ctx)
        val nid = timerNotificationId(id)
        val stop = PendingIntent.getBroadcast(
            ctx, nid,
            Intent(ctx, NotificationActionReceiver::class.java).setAction(ACTION_STOP_ALERT).putExtra(EXTRA_RECORD_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CH_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(timerText(minutesLeft))
            .setContentIntent(openAppIntent(ctx, id, openTimer = false, requestCode = nid))
            .addAction(0, "Tamam", stop)
            .setDeleteIntent(stop)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        post(ctx, nid, n)
    }

    fun cancelPark(ctx: Context, id: Long) =
        ctx.getSystemService(NotificationManager::class.java).cancel(parkNotificationId(id))

    private fun post(ctx: Context, nid: Int, n: android.app.Notification) {
        runCatching { ctx.getSystemService(NotificationManager::class.java).notify(nid, n) }
    }
}
