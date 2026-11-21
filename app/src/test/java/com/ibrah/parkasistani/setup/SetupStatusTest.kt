package com.ibrah.parkasistani.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupStatusTest {
    private val all = SetupItem.entries.toSet()

    @Test fun all_granted_ready() = assertTrue(SetupStatus.autoParkReady(SetupState(all)))

    @Test fun missing_background_location_not_ready() =
        assertFalse(SetupStatus.autoParkReady(SetupState(all - SetupItem.BACKGROUND_LOCATION)))

    @Test fun missing_exact_alarm_still_ready() {
        val state = SetupState(all - SetupItem.EXACT_ALARM)
        assertTrue(SetupStatus.autoParkReady(state))
        assertEquals(listOf(SetupItem.EXACT_ALARM), SetupStatus.missing(state))
    }

    @Test fun missing_lists_in_enum_order() = assertEquals(
        listOf(SetupItem.NOTIFICATIONS, SetupItem.BLUETOOTH, SetupItem.CAR_SELECTED),
        SetupStatus.missing(SetupState(all - SetupItem.CAR_SELECTED - SetupItem.NOTIFICATIONS - SetupItem.BLUETOOTH)),
    )
}
