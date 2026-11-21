package com.ibrah.parkasistani.car

import com.ibrah.parkasistani.data.ParkRecord
import com.ibrah.parkasistani.data.ParkSource

/** Sürüş sırasında anlık Bluetooth kopmasıyla oluşan yanlış otomatik kaydı ayıklar. */
object ReconnectPolicy {
    const val WINDOW_MS = 120_000L

    fun shouldDiscard(latest: ParkRecord?, reconnectedAddress: String, now: Long): Boolean {
        if (latest == null || latest.source != ParkSource.AUTO) return false
        if (!reconnectedAddress.equals(latest.deviceAddress, ignoreCase = true)) return false
        return now - latest.savedAt in 0..WINDOW_MS
    }
}
