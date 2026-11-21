package com.ibrah.parkasistani.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

fun rec(savedAt: Long, source: ParkSource = ParkSource.AUTO, device: String? = "AA:AA:AA:00:00:01") =
    ParkRecord(savedAt = savedAt, lat = 41.0, lng = 29.0, accuracyM = 5f, source = source, deviceAddress = device)

fun inMemoryDb(): AppDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries().build()

@RunWith(RobolectricTestRunner::class)
class ParkRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ParkRepository

    @Before fun setUp() { db = inMemoryDb(); repo = ParkRepository(db.parkDao()) }
    @After fun tearDown() = db.close()

    @Test fun insert_then_latest_returns_newest() = runTest {
        repo.insert(rec(1000))
        repo.insert(rec(2000))
        assertEquals(2000L, repo.latest()!!.savedAt)
        assertEquals(2000L, repo.observeLatest().first()!!.savedAt)
    }

    @Test fun history_is_capped_at_50() = runTest {
        for (i in 1..51) repo.insert(rec(i * 1000L))
        val history = repo.observeHistory().first()
        assertEquals(50, history.size)
        assertFalse(history.any { it.savedAt == 1000L })
        assertEquals(51_000L, history.first().savedAt)
    }

    @Test fun delete_removes_record() = runTest {
        val id = repo.insert(rec(1000))
        repo.delete(id)
        assertNull(repo.get(id))
    }
}
