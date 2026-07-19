package com.callassistant.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.callassistant.data.entity.BlockedNumber
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedNumberDao {
    @Query("SELECT * FROM blocked_numbers ORDER BY timestamp DESC")
    fun getAll(): Flow<List<BlockedNumber>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blocked: BlockedNumber): Long

    @Query("DELETE FROM blocked_numbers WHERE number = :number")
    suspend fun delete(number: String)
}
