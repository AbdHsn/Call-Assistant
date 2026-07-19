package com.callassistant.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.callassistant.data.entity.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE phoneNumber = :number LIMIT 1")
    suspend fun findByNumber(number: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<Contact>)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}
