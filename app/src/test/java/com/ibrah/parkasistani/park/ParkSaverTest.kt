package com.ibrah.parkasistani.park

import com.ibrah.parkasistani.data.AppDatabase
import com.ibrah.parkasistani.data.ParkRepository
import com.ibrah.parkasistani.data.ParkSource
import com.ibrah.parkasistani.data.inMemoryDb
import com.ibrah.parkasistani.location.Fix
import com.ibrah.parkasistani.location.LocationFetcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ParkSaverTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ParkRepository
    private var now = 0L
    private var fix: Fix? = Fix(41.0082, 28.9784, 6f)
    private val fetcher = object : LocationFetcher { override suspend fun current() = fix }
    private lateinit var saver: ParkSaver

    @Before fun setUp() {
        db = inMemoryDb(); repo = ParkRepository(db.parkDao())
        saver = ParkSaver(repo, fetcher) { now }
    }
    @After fun tearDown() = db.close()

    @Test fun save_with_fix_stores_coordinates() = runTest {
        val r = saver.save(ParkSource.AUTO, "AA:AA:AA:00:00:01") as SaveResult.Saved
        assertTrue(r.hasLocation)
        val saved = repo.get(r.id)!!
        assertEquals(41.0082, saved.lat!!, 1e-9)
        assertEquals(28.9784, saved.lng!!, 1e-9)
    }

    @Test fun save_without_fix_still_saves() = runTest {
        fix = null
        val r = saver.save(ParkSource.AUTO, "AA:AA:AA:00:00:01") as SaveResult.Saved
        assertEquals(false, r.hasLocation)
        assertNull(repo.get(r.id)!!.lat)
    }

    @Test fun second_auto_within_2min_is_skipped() = runTest {
        saver.save(ParkSource.AUTO, "AA:AA:AA:00:00:01")
        now = 60_000
        assertEquals(SaveResult.SkippedDuplicate, saver.save(ParkSource.AUTO, "AA:AA:AA:00:00:02"))
        assertEquals(1, repo.observeHistory().first().size)
    }

    @Test fun manual_within_2min_is_not_skipped() = runTest {
        saver.save(ParkSource.AUTO, "AA:AA:AA:00:00:01")
        now = 60_000
        assertTrue(saver.save(ParkSource.MANUAL, null) is SaveResult.Saved)
    }
}
