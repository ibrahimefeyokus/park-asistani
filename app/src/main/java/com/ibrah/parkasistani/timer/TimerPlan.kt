package com.ibrah.parkasistani.timer

data class Alarm(val at: Long, val minutesLeft: Int)

object TimerPlan {
    const val WARNING_MIN = 10
    private const val WARNING_MS = WARNING_MIN * 60_000L

    /** Geçmişte kalan alarmlar dahil edilmez (ör. yeniden başlatmadan sonra). */
    fun alarms(endAt: Long, now: Long): List<Alarm> = buildList {
        if (endAt - WARNING_MS > now) add(Alarm(endAt - WARNING_MS, WARNING_MIN))
        if (endAt > now) add(Alarm(endAt, 0))
    }
}
