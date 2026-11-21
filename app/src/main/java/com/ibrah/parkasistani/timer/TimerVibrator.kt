package com.ibrah.parkasistani.timer

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager

/** Titreşim deseni: timings[i] süresince amplitudes[i] gücünde (0 = ara). */
class VibrationPlan(val timings: LongArray, val amplitudes: IntArray) {
    companion object {
        private const val PULSE_MS = 1_000L
        private const val END_TOTAL_MS = 60_000L

        fun forAlert(minutesLeft: Int): VibrationPlan =
            if (minutesLeft > 0) {
                // 10 dk kala: kısa çift titreşim
                VibrationPlan(longArrayOf(0, 300, 200, 300), intArrayOf(0, 255, 0, 255))
            } else {
                // Süre doldu: 1 dk boyunca 1 sn titreşim / 1 sn ara
                val n = (END_TOTAL_MS / PULSE_MS).toInt()
                VibrationPlan(
                    LongArray(n + 1) { if (it == 0) 0 else PULSE_MS },
                    IntArray(n + 1) { if (it % 2 == 1) 255 else 0 },
                )
            }
    }
}

object TimerVibrator {
    private fun vibrator(context: Context) =
        context.getSystemService(VibratorManager::class.java).defaultVibrator

    fun start(context: Context, plan: VibrationPlan) {
        val v = vibrator(context)
        if (!v.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(plan.timings, plan.amplitudes, -1)
        // Alarm kullanımı: arka planda ve Rahatsız Etmeyin'de de titreşir.
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }

    fun stop(context: Context) = vibrator(context).cancel()
}
