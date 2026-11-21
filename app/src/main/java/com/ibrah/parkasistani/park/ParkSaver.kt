package com.ibrah.parkasistani.park

import android.content.Context
import com.ibrah.parkasistani.data.ParkRecord
import com.ibrah.parkasistani.data.ParkRepository
import com.ibrah.parkasistani.data.ParkSource
import com.ibrah.parkasistani.location.FusedLocationFetcher
import com.ibrah.parkasistani.location.LocationFetcher
import kotlinx.coroutines.sync.withLock

sealed interface SaveResult {
    data class Saved(val id: Long, val hasLocation: Boolean) : SaveResult
    data object SkippedDuplicate : SaveResult
    data object SkippedReconnected : SaveResult
}

class ParkSaver(
    private val repo: ParkRepository,
    private val location: LocationFetcher,
    private val connections: ConnectionLog = InMemoryConnectionLog(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun save(source: ParkSource, deviceAddress: String?): SaveResult = LOCK.withLock {
        saveLocked(source, deviceAddress)
    }

    private suspend fun saveLocked(source: ParkSource, deviceAddress: String?): SaveResult {
        val now = clock()
        if (source == ParkSource.AUTO) {
            val last = repo.latest()
            if (last != null && last.source == ParkSource.AUTO && now - last.savedAt in 0..DUPLICATE_WINDOW_MS) {
                return SaveResult.SkippedDuplicate
            }
        }
        val fix = location.current()
        // Konum alınırken araç yeniden bağlandıysa bu bir park değil, anlık kopmaydı.
        if (source == ParkSource.AUTO && deviceAddress != null) {
            val reconnected = connections.lastConnectedAt(deviceAddress)
            if (reconnected != null && reconnected >= now) return SaveResult.SkippedReconnected
        }
        val id = repo.insert(
            ParkRecord(
                savedAt = now, lat = fix?.lat, lng = fix?.lng, accuracyM = fix?.accuracyM,
                source = source, deviceAddress = deviceAddress,
            )
        )
        return SaveResult.Saved(id, fix != null)
    }

    companion object {
        const val DUPLICATE_WINDOW_MS = 120_000L
        /** Kontrol → konum → ekleme ve geri bağlanma kontrolü bu kilitle sıraya girer. */
        val LOCK = kotlinx.coroutines.sync.Mutex()
        fun get(context: Context) =
            ParkSaver(ParkRepository.get(context), FusedLocationFetcher(context), PrefsConnectionLog(context))
    }
}
