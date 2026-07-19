package com.callassistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spam_rules")
data class SpamRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pattern: String,
    val type: RuleType,
    val label: String,
    val isBlocking: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class RuleType { REGEX, EXACT, PREFIX }
