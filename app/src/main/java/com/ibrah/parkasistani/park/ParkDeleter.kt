package com.ibrah.parkasistani.park

import android.content.Context
import com.ibrah.parkasistani.data.ParkRepository
import com.ibrah.parkasistani.notify.Notifications
import com.ibrah.parkasistani.timer.ActiveTimerStore
import com.ibrah.parkasistani.timer.TimerScheduler
import com.ibrah.parkasistani.widget.WidgetUpdater
import java.io.File

/** Bir park kaydını tüm izleriyle siler: fotoğraf, sayaç, bildirim, widget. */
class ParkDeleter(
    private val repo: ParkRepository,
    private val timers: ActiveTimerStore,
    private val cancelAlarms: () -> Unit,
    private val afterDelete: suspend (Long) -> Unit,
) {
    suspend fun delete(id: Long) {
        repo.get(id)?.photoPath?.let { File(it).delete() }
        repo.delete(id)
        if (timers.clearIfFor(id)) cancelAlarms()
        afterDelete(id)
    }

    companion object {
        fun get(context: Context): ParkDeleter {
            val repo = ParkRepository.get(context)
            return ParkDeleter(
                repo, ActiveTimerStore(context),
                cancelAlarms = { TimerScheduler(context, repo).cancelAlarms() },
                afterDelete = { id -> Notifications.cancelPark(context, id); WidgetUpdater.update(context) },
            )
        }
    }
}
