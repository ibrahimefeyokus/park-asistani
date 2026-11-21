package com.ibrah.parkasistani.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class ParkSource { AUTO, MANUAL }

@Entity(tableName = "park_records")
data class ParkRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val savedAt: Long,
    val lat: Double?,
    val lng: Double?,
    val accuracyM: Float?,
    val source: ParkSource,
    val deviceAddress: String?,
    val note: String? = null,
    val photoPath: String? = null,
    val timerEndAt: Long? = null,
) {
    val hasLocation: Boolean get() = lat != null && lng != null
}

class Converters {
    @TypeConverter fun fromSource(s: ParkSource): String = s.name
    @TypeConverter fun toSource(s: String): ParkSource = ParkSource.valueOf(s)
}
