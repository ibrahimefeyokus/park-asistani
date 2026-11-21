package com.ibrah.parkasistani.timer

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveTimer(val recordId: Long, val endAt: Long, val startAt: Long = 0) {
    /** Kalan sürenin oranı (1 → yeni başladı, 0 → bitti). */
    fun remainingFraction(now: Long): Float {
        val total = endAt - startAt
        if (startAt <= 0 || total <= 0) return 1f
        return ((endAt - now).toFloat() / total).coerceIn(0f, 1f)
    }
}

/** Tek aktif park sayacı; arayüz ve yeniden başlatma bunu okur, en son kayıt değil. */
class ActiveTimerStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("timer", Context.MODE_PRIVATE)

    fun get(): ActiveTimer? {
        val id = prefs.getLong(K_ID, -1L)
        val end = prefs.getLong(K_END, -1L)
        return if (id >= 0 && end >= 0) ActiveTimer(id, end, prefs.getLong(K_START, 0L)) else null
    }

    fun set(t: ActiveTimer) {
        prefs.edit().putLong(K_ID, t.recordId).putLong(K_END, t.endAt).putLong(K_START, t.startAt).commit()
        state.value = t
    }

    fun clear() {
        prefs.edit().clear().commit()
        state.value = null
    }

    fun clearIfFor(recordId: Long): Boolean {
        if (get()?.recordId != recordId) return false
        clear(); return true
    }

    /** Uygulama içinde canlı izleme için. */
    fun observe(): StateFlow<ActiveTimer?> { if (state.value == null) state.value = get(); return state.asStateFlow() }

    private companion object {
        const val K_ID = "recordId"
        const val K_END = "endAt"
        const val K_START = "startAt"
        val state = MutableStateFlow<ActiveTimer?>(null)
    }
}

object TimerGuard {
    fun shouldNotify(active: ActiveTimer?, recordId: Long) = active?.recordId == recordId
}
