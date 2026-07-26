package com.callassistant.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_logs",
    indices = [Index(value = ["number"]), Index(value = ["timestamp"])]
)
data class CallLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val number: String,
    val name: String? = null,
    val type: CallType,
    val timestamp: Long,
    val duration: Long = 0,
    val blocked: Boolean = false
)

enum class CallType { INCOMING, OUTGOING, MISSED }
