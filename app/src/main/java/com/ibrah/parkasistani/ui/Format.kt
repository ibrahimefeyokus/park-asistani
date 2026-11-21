package com.ibrah.parkasistani.ui

import com.ibrah.parkasistani.data.ParkRecord
import com.ibrah.parkasistani.data.ParkSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val tr = Locale.forLanguageTag("tr-TR")

fun formatClock(millis: Long): String = SimpleDateFormat("HH:mm", tr).format(Date(millis))
fun formatDay(millis: Long): String = SimpleDateFormat("d MMMM EEEE", tr).format(Date(millis))
fun formatDateTime(millis: Long): String = SimpleDateFormat("d MMMM, HH:mm", tr).format(Date(millis))

fun formatAgo(millis: Long, now: Long = System.currentTimeMillis()): String {
    val min = ((now - millis) / 60_000).coerceAtLeast(0)
    return when {
        min < 1 -> "az önce"
        min < 60 -> "$min dk önce"
        min < 24 * 60 -> "${min / 60} sa ${min % 60} dk önce"
        else -> "${min / (24 * 60)} gün önce"
    }
}

fun ParkRecord.sourceLabel() = if (source == ParkSource.AUTO) "Otomatik" else "Elle"

fun ParkRecord.locationLabel() =
    if (hasLocation) "±${accuracyM?.toInt() ?: "?"} m" else "Konum yok"

/** Geçmiş listesi başlıkları: Bugün, Dün, 19 Kasım Salı. */
fun dayLabel(millis: Long, now: Long = System.currentTimeMillis()): String {
    val day = { m: Long -> SimpleDateFormat("yyyyMMdd", tr).format(Date(m)) }
    return when (day(millis)) {
        day(now) -> "Bugün"
        day(now - 86_400_000L) -> "Dün"
        else -> SimpleDateFormat("d MMMM EEEE", tr).format(Date(millis))
    }
}
