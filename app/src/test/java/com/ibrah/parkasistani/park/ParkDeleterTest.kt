package com.ibrah.parkasistani.park

import androidx.test.core.app.ApplicationProvider
import com.ibrah.parkasistani.data.AppDatabase
import com.ibrah.parkasistani.data.ParkRepository
import com.ibrah.parkasistani.data.inMemoryDb
import com.ibrah.parkasistani.data.rec
import com.ibrah.parkasistani.timer.ActiveTimer
import com.ibrah.parkasistani.timer.ActiveTimerStore
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ParkDeleterTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ParkRepository
    private lateinit var store: ActiveTimerStore
    private var alarmsCancelled = 0
    private lateinit var deleter: ParkDeleter

    @Before fun setUp() {
        db = inMemoryDb(); repo = ParkRepository(db.parkDao())
        store = ActiveTimerStore(ApplicationProvider.getApplicationContext()).apply { clear() }
        deleter = ParkDeleter(repo, store, cancelAlarms = { alarmsCancelled++ }, afterDelete = {})
    }
    @After fun tearDown() = db.close()

    @Test fun deleting_timed_record_cancels_its_timer_and_photo() = runBlocking {
        val photo = File.createTempFile("park", ".jpg")
        val id = repo.insert(rec(1_000).copy(photoPath = photo.path))
        store.set(ActiveTimer(id, 9_000))
        deleter.delete(id)
        assertNull(repo.get(id))
        assertNull(store.get())
        assertEquals(1, alarmsCancelled)
        assertFalse(photo.exists())
    }

    @Test fun deleting_other_record_keeps_timer() = runBlocking {
        val timed = repo.insert(rec(1_000))
        val other = repo.insert(rec(2_000))
        store.set(ActiveTimer(timed, 9_000))
        deleter.delete(other)
        assertEquals(ActiveTimer(timed, 9_000), store.get())
        assertEquals(0, alarmsCancelled)
    }
}
