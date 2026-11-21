package com.ibrah.parkasistani.timer

/** Bildirim çubuğundaki geri sayımın verisi; süre bitmişse gösterilmez. */
data class CountdownSpec(val recordId: Long, val endAt: Long, val timeoutMs: Long) {
    companion object {
        fun from(timer: ActiveTimer?, now: Long): CountdownSpec? {
            if (timer == null || timer.endAt <= now) return null
            return CountdownSpec(timer.recordId, timer.endAt, timer.endAt - now)
        }
    }
}
