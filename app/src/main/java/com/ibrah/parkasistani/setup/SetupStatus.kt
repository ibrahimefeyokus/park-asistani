package com.ibrah.parkasistani.setup

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import com.ibrah.parkasistani.car.CarDevicePrefs

enum class SetupItem { NOTIFICATIONS, FINE_LOCATION, BACKGROUND_LOCATION, BLUETOOTH, EXACT_ALARM, BATTERY_UNRESTRICTED, CAR_SELECTED }

data class SetupState(val granted: Set<SetupItem>)

object SetupStatus {
    private val REQUIRED_FOR_AUTO = setOf(
        SetupItem.FINE_LOCATION, SetupItem.BACKGROUND_LOCATION, SetupItem.BLUETOOTH,
        SetupItem.BATTERY_UNRESTRICTED, SetupItem.CAR_SELECTED,
    )

    fun missing(state: SetupState): List<SetupItem> = SetupItem.entries.filter { it !in state.granted }

    fun autoParkReady(state: SetupState): Boolean = state.granted.containsAll(REQUIRED_FOR_AUTO)

    suspend fun read(context: Context): SetupState {
        fun perm(p: String) = context.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED
        val granted = buildSet {
            if (perm(Manifest.permission.POST_NOTIFICATIONS)) add(SetupItem.NOTIFICATIONS)
            if (perm(Manifest.permission.ACCESS_FINE_LOCATION)) add(SetupItem.FINE_LOCATION)
            if (perm(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) add(SetupItem.BACKGROUND_LOCATION)
            if (perm(Manifest.permission.BLUETOOTH_CONNECT)) add(SetupItem.BLUETOOTH)
            if (context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) add(SetupItem.EXACT_ALARM)
            if (context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)) {
                add(SetupItem.BATTERY_UNRESTRICTED)
            }
            if (CarDevicePrefs(context).selectedOnce().isNotEmpty()) add(SetupItem.CAR_SELECTED)
        }
        return SetupState(granted)
    }
}
