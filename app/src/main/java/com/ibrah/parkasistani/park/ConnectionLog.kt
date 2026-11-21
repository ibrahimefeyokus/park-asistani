package com.ibrah.parkasistani.park

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/** Araç cihazlarının son bağlanma zamanı; kayıt sırasında geri bağlanmayı yakalamak için. */
interface ConnectionLog {
    fun markConnected(address: String, at: Long)
    fun lastConnectedAt(address: String): Long?
}

class InMemoryConnectionLog : ConnectionLog {
    private val map = ConcurrentHashMap<String, Long>()
    override fun markConnected(address: String, at: Long) { map[address.uppercase()] = at }
    override fun lastConnectedAt(address: String) = map[address.uppercase()]
}

class PrefsConnectionLog(context: Context) : ConnectionLog {
    private val prefs = context.applicationContext.getSharedPreferences("connections", Context.MODE_PRIVATE)
    override fun markConnected(address: String, at: Long) { prefs.edit().putLong(address.uppercase(), at).commit() }
    override fun lastConnectedAt(address: String) =
        prefs.getLong(address.uppercase(), -1L).takeIf { it >= 0 }
}
