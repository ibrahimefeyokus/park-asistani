package com.ibrah.parkasistani.car

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ibrah.parkasistani.data.ParkRepository
import com.ibrah.parkasistani.data.ParkSource
import com.ibrah.parkasistani.notify.Notifications
import com.ibrah.parkasistani.park.ParkDeleter
import com.ibrah.parkasistani.park.ParkSaver
import com.ibrah.parkasistani.park.PrefsConnectionLog
import kotlinx.coroutines.sync.withLock
import com.ibrah.parkasistani.park.SaveResult
import com.ibrah.parkasistani.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CarBluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
        val address = device.address ?: return
        val action = intent.action
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!CarDeviceMatcher.isCar(address, CarDevicePrefs(context).selectedOnce())) return@launch
                when (action) {
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> onCarDisconnected(context, address)
                    BluetoothDevice.ACTION_ACL_CONNECTED -> onCarConnected(context, address)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Bluetooth olayı işlenemedi", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun onCarDisconnected(context: Context, address: String) {
        val result = ParkSaver.get(context).save(ParkSource.AUTO, address)
        Log.i(TAG, "Araç koptu ($address): $result")
        if (result is SaveResult.Saved) {
            Notifications.showSaved(context, result.id, result.hasLocation)
            WidgetUpdater.update(context)
        }
    }

    private suspend fun onCarConnected(context: Context, address: String) {
        val now = System.currentTimeMillis()
        // Önce kaydet: süren bir park kaydı konum alırken bunu görüp kaydı iptal eder.
        PrefsConnectionLog(context).markConnected(address, now)
        ParkSaver.LOCK.withLock {
            val latest = ParkRepository.get(context).latest() ?: return
            if (ReconnectPolicy.shouldDiscard(latest, address, now)) {
                Log.i(TAG, "Araç 2 dk içinde geri bağlandı, kayıt ${latest.id} siliniyor")
                ParkDeleter.get(context).delete(latest.id)
            }
        }
    }

    private companion object { const val TAG = "ParkAsistani" }
}
