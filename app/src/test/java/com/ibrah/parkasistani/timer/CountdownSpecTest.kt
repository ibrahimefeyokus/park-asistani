package com.ibrah.parkasistani.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountdownSpecTest {
    @Test fun running_timer_disappears_exactly_at_end() {
        val spec = CountdownSpec.from(ActiveTimer(7, endAt = 3_600_000, startAt = 0), now = 600_000)!!
        assertEquals(3_000_000L, spec.timeoutMs)
        assertEquals(3_600_000L, spec.endAt)
        assertEquals(7L, spec.recordId)
    }

    @Test fun finished_or_missing_timer_shows_nothing() {
        assertNull(CountdownSpec.from(ActiveTimer(7, endAt = 1_000), now = 1_000))
        assertNull(CountdownSpec.from(null, now = 0))
    }
}
