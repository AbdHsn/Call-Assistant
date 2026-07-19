package com.callassistant.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_numbers",
    indices = [Index(value = ["number"], unique = true)]
)
data class BlockedNumber(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val number: String,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
