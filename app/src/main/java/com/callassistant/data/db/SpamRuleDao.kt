package com.callassistant.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.callassistant.data.entity.SpamRule
import kotlinx.coroutines.flow.Flow

@Dao
interface SpamRuleDao {
    @Query("SELECT * FROM spam_rules ORDER BY createdAt DESC")
    fun getAll(): Flow<List<SpamRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: SpamRule): Long

    @Update
    suspend fun update(rule: SpamRule)

    @Delete
    suspend fun delete(rule: SpamRule)

    @Query("SELECT * FROM spam_rules WHERE isBlocking = 1")
    suspend fun getActiveBlockingRules(): List<SpamRule>
}
