package com.ibrah.parkasistani.timer

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActiveTimerTest {
    private lateinit var store: ActiveTimerStore

    @Before fun setUp() { store = ActiveTimerStore(ApplicationProvider.getApplicationContext()); store.clear() }

    @Test fun store_survives_new_instance() {
        store.set(ActiveTimer(recordId = 7, endAt = 9_000))
        assertEquals(ActiveTimer(7, 9_000), ActiveTimerStore(ApplicationProvider.getApplicationContext()).get())
    }

    @Test fun remaining_fraction_for_progress_ring() {
        assertEquals(0.5f, ActiveTimer(1, endAt = 2_000, startAt = 1_000).remainingFraction(1_500), 0.001f)
        assertEquals(0f, ActiveTimer(1, endAt = 2_000, startAt = 1_000).remainingFraction(3_000), 0.001f)
        assertEquals(1f, ActiveTimer(1, endAt = 2_000).remainingFraction(1_500), 0.001f)
    }

    @Test fun start_time_is_persisted() {
        store.set(ActiveTimer(7, endAt = 9_000, startAt = 3_000))
        assertEquals(3_000L, ActiveTimerStore(ApplicationProvider.getApplicationContext()).get()!!.startAt)
    }

    @Test fun clearIfFor_only_clears_matching_record() {
        store.set(ActiveTimer(7, 9_000))
        assertFalse(store.clearIfFor(8))
        assertTrue(store.clearIfFor(7))
        assertNull(store.get())
    }

    @Test fun alarm_for_deleted_or_replaced_timer_is_silent() {
        assertFalse(TimerGuard.shouldNotify(null, recordId = 7))
        assertFalse(TimerGuard.shouldNotify(ActiveTimer(8, 9_000), recordId = 7))
        assertTrue(TimerGuard.shouldNotify(ActiveTimer(7, 9_000), recordId = 7))
    }
}
