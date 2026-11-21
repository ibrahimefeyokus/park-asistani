package com.ibrah.parkasistani.car

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.carStore by preferencesDataStore(name = "car_devices")

class CarDevicePrefs(private val context: Context) {
    private val key = stringSetPreferencesKey("selected")

    /** Hiç ayar yapılmamışsa null; boş küme "kullanıcı hepsini kaldırdı" demektir. */
    private val raw: Flow<Set<String>?> = context.carStore.data.map { it[key] }

    val selected: Flow<Set<String>> = raw.map { it ?: emptySet() }

    suspend fun setSelected(addresses: Set<String>) {
        context.carStore.edit { it[key] = addresses }
    }

    suspend fun selectedOnce(): Set<String> {
        raw.first()?.let { return it }
        val defaults = CarDeviceMatcher.defaultSelection(BondedDevices.list(context))
        // İzin henüz yoksa liste boş gelir; varsayılanı ancak bir şey bulunduğunda kalıcı yaz.
        if (defaults.isNotEmpty()) setSelected(defaults)
        return defaults
    }
}
