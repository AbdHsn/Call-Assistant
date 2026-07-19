package com.callassistant.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sms_messages",
    indices = [Index(value = ["number"]), Index(value = ["timestamp"])]
)
data class SmsMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val number: String,
    val name: String? = null,
    val body: String,
    val timestamp: Long,
    val direction: SmsDirection
)

enum class SmsDirection { IN, OUT }
