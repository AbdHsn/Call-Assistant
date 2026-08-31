package com.callassistant.data.repository

import com.callassistant.data.entity.BlockedNumber
import com.callassistant.data.entity.SpamRule
import kotlinx.coroutines.flow.Flow

interface SpamRuleRepository {
    val allRules: Flow<List<SpamRule>>
    val allBlockedNumbers: Flow<List<BlockedNumber>>
    suspend fun add(rule: SpamRule)
    suspend fun update(rule: SpamRule)
    suspend fun delete(rule: SpamRule)
    suspend fun matchesAny(input: String): SpamRule?
    suspend fun isNumberBlocked(number: String): Boolean
    suspend fun recordBlockedAttempt(number: String)
    suspend fun blockNumber(number: String, reason: String = "Manually blocked", name: String? = null)
    suspend fun unblockNumber(number: String)
    suspend fun seedDefaultsIfEmpty()
}
