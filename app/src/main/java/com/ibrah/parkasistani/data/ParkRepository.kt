package com.ibrah.parkasistani.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class ParkRepository(private val dao: ParkDao) {
    suspend fun insert(r: ParkRecord): Long {
        val id = dao.insert(r)
        dao.trimTo(MAX_HISTORY)
        return id
    }
    fun observeLatest(): Flow<ParkRecord?> = dao.observeLatest()
    fun observeHistory(): Flow<List<ParkRecord>> = dao.observeHistory()
    suspend fun get(id: Long): ParkRecord? = dao.get(id)
    suspend fun latest(): ParkRecord? = dao.latest()
    suspend fun update(r: ParkRecord) = dao.update(r)
    suspend fun delete(id: Long) = dao.delete(id)

    companion object {
        const val MAX_HISTORY = 50
        fun get(context: Context) = ParkRepository(AppDatabase.get(context).parkDao())
    }
}
