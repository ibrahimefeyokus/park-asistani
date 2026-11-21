package com.ibrah.parkasistani.car

import com.ibrah.parkasistani.data.ParkSource
import com.ibrah.parkasistani.data.rec
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectPolicyTest {
    private val kensa = "AA:AA:AA:00:00:01"

    @Test fun discard_when_same_device_reconnects_within_2min() =
        assertTrue(ReconnectPolicy.shouldDiscard(rec(1_000, device = kensa), kensa.lowercase(), 1_000 + 90_000))

    @Test fun keep_after_2min() =
        assertFalse(ReconnectPolicy.shouldDiscard(rec(1_000, device = kensa), kensa, 1_000 + 121_000))

    @Test fun keep_for_manual_record() =
        assertFalse(ReconnectPolicy.shouldDiscard(rec(1_000, ParkSource.MANUAL, null), kensa, 30_000))

    @Test fun keep_for_other_device() =
        assertFalse(ReconnectPolicy.shouldDiscard(rec(1_000, device = "AA:AA:AA:00:00:02"), kensa, 30_000))
}
