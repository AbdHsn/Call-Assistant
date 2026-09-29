package com.callassistant.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.callassistant.data.entity.CallLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<CallLogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<CallLogEntry>)

    @Query("DELETE FROM call_logs")
    suspend fun deleteAll()

    @Query("DELETE FROM call_logs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE call_logs SET blocked = 1 WHERE number = :number")
    suspend fun markBlocked(number: String)

    @Query("SELECT * FROM call_logs WHERE type = 'MISSED' ORDER BY timestamp DESC")
    suspend fun getMissedEntries(): List<CallLogEntry>
}
