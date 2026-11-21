package com.ibrah.parkasistani.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarDeviceMatcherTest {
    private val kensa = BtDevice("AA:AA:AA:00:00:01", "Kensa")
    private val fiat = BtDevice("AA:AA:AA:00:00:02", "FIAT_4504")
    private val hp = BtDevice("AA:AA:AA:00:00:03", "HP")
    private val philips = BtDevice("AA:AA:AA:00:00:04", "Philips TAH5209")

    @Test fun isCar_matches_case_insensitive() =
        assertTrue(CarDeviceMatcher.isCar("AA:AA:AA:00:00:01", setOf("AA:AA:AA:00:00:01")))

    @Test fun isCar_false_for_other() =
        assertFalse(CarDeviceMatcher.isCar(philips.address, setOf(kensa.address)))

    @Test fun default_selects_Kensa_only() =
        assertEquals(setOf(kensa.address), CarDeviceMatcher.defaultSelection(listOf(kensa, fiat, hp)))

    @Test fun default_empty_when_no_Kensa() =
        assertEquals(emptySet<String>(), CarDeviceMatcher.defaultSelection(listOf(fiat, hp)))
}
