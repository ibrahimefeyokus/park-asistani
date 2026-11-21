package com.ibrah.parkasistani.park

import com.ibrah.parkasistani.data.AppDatabase
import com.ibrah.parkasistani.data.ParkRepository
import com.ibrah.parkasistani.data.ParkSource
import com.ibrah.parkasistani.data.inMemoryDb
import com.ibrah.parkasistani.location.Fix
import com.ibrah.parkasistani.location.LocationFetcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ParkSaverRaceTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ParkRepository
    private val kensa = "AA:AA:AA:00:00:01"
    private val fiat = "AA:AA:AA:00:00:02"

    @Before fun setUp() { db = inMemoryDb(); repo = ParkRepository(db.parkDao()) }
    @After fun tearDown() = db.close()

    @Test fun two_devices_disconnecting_together_create_one_record() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val slow = object : LocationFetcher {
            override suspend fun current(): Fix? { calls++; if (calls == 1) gate.await(); return Fix(41.0, 29.0, 5f) }
        }
        val saver = ParkSaver(repo, slow) { 1_000L }
        val a = async(Dispatchers.Default) { saver.save(ParkSource.AUTO, kensa) }
        val b = async(Dispatchers.Default) { kotlinx.coroutines.delay(100); saver.save(ParkSource.AUTO, fiat) }
        kotlinx.coroutines.delay(300)
        gate.complete(Unit)
        val results = listOf(a, b).awaitAll()
        assertEquals(1, repo.observeHistory().first().size)
        assertEquals(1, results.count { it is SaveResult.Saved })
    }

    @Test fun reconnect_during_location_fetch_discards_auto_save() = runBlocking {
        val log = InMemoryConnectionLog()
        val fetcher = object : LocationFetcher {
            override suspend fun current(): Fix? { log.markConnected(kensa, 5_000L); return Fix(41.0, 29.0, 5f) }
        }
        val saver = ParkSaver(repo, fetcher, log) { 1_000L }
        assertEquals(SaveResult.SkippedReconnected, saver.save(ParkSource.AUTO, kensa))
        assertEquals(0, repo.observeHistory().first().size)
    }

    @Test fun earlier_connection_does_not_block_save() = runBlocking {
        val log = InMemoryConnectionLog().apply { markConnected(kensa, 500L) }
        val saver = ParkSaver(repo, object : LocationFetcher { override suspend fun current() = Fix(41.0, 29.0, 5f) }, log) { 1_000L }
        assert(saver.save(ParkSource.AUTO, kensa) is SaveResult.Saved)
    }
}
