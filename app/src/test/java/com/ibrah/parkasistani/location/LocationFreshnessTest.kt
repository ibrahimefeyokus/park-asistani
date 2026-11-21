package com.ibrah.parkasistani.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFreshnessTest {
    @Test fun fetch_timeout_fits_receiver_budget() = assertTrue(FusedLocationFetcher.TIMEOUT_MS <= 8_000L)

    @Test fun recent_last_location_is_accepted() = assertTrue(LocationFreshness.acceptLast(ageMs = 4 * 60_000L))

    @Test fun stale_last_location_is_rejected() = assertFalse(LocationFreshness.acceptLast(ageMs = 6 * 60_000L))
}
