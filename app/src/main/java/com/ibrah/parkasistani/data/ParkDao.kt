package com.ibrah.parkasistani.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkDao {
    @Insert suspend fun insert(r: ParkRecord): Long
    @Update suspend fun update(r: ParkRecord)

    @Query("DELETE FROM park_records WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM park_records WHERE id = :id")
    suspend fun get(id: Long): ParkRecord?

    @Query("SELECT * FROM park_records ORDER BY savedAt DESC, id DESC LIMIT 1")
    suspend fun latest(): ParkRecord?

    @Query("SELECT * FROM park_records ORDER BY savedAt DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<ParkRecord?>

    @Query("SELECT * FROM park_records ORDER BY savedAt DESC, id DESC")
    fun observeHistory(): Flow<List<ParkRecord>>

    @Query(
        "DELETE FROM park_records WHERE id NOT IN " +
            "(SELECT id FROM park_records ORDER BY savedAt DESC, id DESC LIMIT :keep)"
    )
    suspend fun trimTo(keep: Int)
}
