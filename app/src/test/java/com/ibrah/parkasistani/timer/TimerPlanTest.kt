package com.ibrah.parkasistani.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerPlanTest {
    @Test fun two_alarms_for_60min() = assertEquals(
        listOf(Alarm(3_000_000, 10), Alarm(3_600_000, 0)),
        TimerPlan.alarms(endAt = 3_600_000, now = 0),
    )

    @Test fun only_end_alarm_when_under_10min() =
        assertEquals(listOf(Alarm(300_000, 0)), TimerPlan.alarms(endAt = 300_000, now = 0))

    @Test fun no_alarms_when_past() =
        assertEquals(emptyList<Alarm>(), TimerPlan.alarms(endAt = 4_000, now = 5_000))

    @Test fun restart_after_boot_skips_past_warning() =
        assertEquals(listOf(Alarm(3_600_000, 0)), TimerPlan.alarms(endAt = 3_600_000, now = 3_100_000))
}
