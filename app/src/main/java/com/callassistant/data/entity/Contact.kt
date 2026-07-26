package com.callassistant.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts",
    indices = [Index(value = ["phoneNumber"], unique = true)]
)
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val source: ContactSource = ContactSource.LOCAL,
    val photoUri: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

enum class ContactSource { LOCAL, IMPORTED }
