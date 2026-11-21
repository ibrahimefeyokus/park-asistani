package com.ibrah.parkasistani

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ibrah.parkasistani.ui.theme.ParkTheme
import com.ibrah.parkasistani.notify.Notifications
import com.ibrah.parkasistani.timer.TimerVibrator
import com.ibrah.parkasistani.ui.AppNav

class MainActivity : ComponentActivity() {
    private var openTimerFor by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifications.ensureChannels(this)
        handle(intent)
        setContent {
            ParkTheme {
                AppNav(openTimerFor = openTimerFor, onTimerHandled = { openTimerFor = null })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Uygulamaya dönen kullanıcı uyarıyı görmüştür; süren titreşimi durdur.
        TimerVibrator.stop(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.getBooleanExtra(Notifications.EXTRA_OPEN_TIMER, false) == true) {
            openTimerFor = intent.getLongExtra(Notifications.EXTRA_RECORD_ID, -1).takeIf { it >= 0 }
            intent.removeExtra(Notifications.EXTRA_OPEN_TIMER)
        }
    }
}
