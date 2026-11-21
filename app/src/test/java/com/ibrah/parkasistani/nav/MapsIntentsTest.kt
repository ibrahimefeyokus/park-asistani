package com.ibrah.parkasistani.nav

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MapsIntentsTest {
    private lateinit var saved: Locale

    @Before fun setUp() { saved = Locale.getDefault(); Locale.setDefault(Locale.forLanguageTag("tr-TR")) }
    @After fun tearDown() = Locale.setDefault(saved)

    @Test fun walking_uri_uses_dot_decimal() =
        assertEquals("google.navigation:q=41.0082,28.9784&mode=w", MapsIntents.walkingUri(41.0082, 28.9784))

    @Test fun web_fallback_uri() = assertEquals(
        "https://www.google.com/maps/dir/?api=1&destination=41.0082,28.9784&travelmode=walking",
        MapsIntents.webUri(41.0082, 28.9784),
    )
}
