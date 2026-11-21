package com.ibrah.parkasistani.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class Fix(val lat: Double, val lng: Double, val accuracyM: Float)

interface LocationFetcher {
    suspend fun current(): Fix?
}

object LocationFreshness {
    const val MAX_LAST_AGE_MS = 5 * 60_000L
    fun acceptLast(ageMs: Long) = ageMs in 0..MAX_LAST_AGE_MS
}

class FusedLocationFetcher(context: Context) : LocationFetcher {
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    override suspend fun current(): Fix? {
        val cts = CancellationTokenSource()
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(30_000)
            .setDurationMillis(TIMEOUT_MS)
            .build()
        val fresh = runCatching {
            withTimeoutOrNull(TIMEOUT_MS) { client.getCurrentLocation(request, cts.token).await() }
        }.getOrNull()
        cts.cancel()
        val loc = fresh ?: runCatching { client.lastLocation.await() }.getOrNull()
            ?.takeIf { LocationFreshness.acceptLast((SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos) / 1_000_000) }
            ?: return null
        return Fix(loc.latitude, loc.longitude, loc.accuracy)
    }

    companion object {
        /** goAsync() bütçesi ~10 sn; DB işi için pay bırakılır. */
        const val TIMEOUT_MS = 8_000L
    }
}
