package com.ibrah.parkasistani.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VibrationPlanTest {
    @Test fun end_alert_pulses_for_one_minute() {
        val p = VibrationPlan.forAlert(minutesLeft = 0)
        assertEquals(0L, p.timings.first())            // hemen başlar
        assertEquals(60_000L, p.timings.sum())          // toplam 1 dakika
        assertTrue(p.timings.drop(1).all { it == 1_000L }) // 1 sn titreşim / 1 sn ara
        assertEquals(p.timings.size, p.amplitudes.size)
        assertTrue(p.amplitudes.filterIndexed { i, _ -> i % 2 == 0 }.all { it == 0 })   // aralar sessiz
        assertTrue(p.amplitudes.filterIndexed { i, _ -> i % 2 == 1 }.all { it > 0 })    // titreşimler dolu
    }

    @Test fun warning_is_a_short_double_buzz() {
        val p = VibrationPlan.forAlert(minutesLeft = 10)
        assertTrue(p.timings.sum() <= 2_000L)
        assertEquals(2, p.amplitudes.count { it > 0 })
    }
}
