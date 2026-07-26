package com.callassistant.data.repository

import com.callassistant.data.db.BlockedNumberDao
import com.callassistant.data.db.SpamRuleDao
import com.callassistant.data.entity.BlockedNumber
import com.callassistant.data.entity.RuleType
import com.callassistant.data.entity.SpamRule
import com.callassistant.util.PhoneNumberNormalizer
import kotlinx.coroutines.flow.Flow
import java.util.regex.PatternSyntaxException

class SpamRuleRepository(
    private val dao: SpamRuleDao,
    private val blockedNumberDao: BlockedNumberDao
) {

    val allRules: Flow<List<SpamRule>> = dao.getAll()

    suspend fun add(rule: SpamRule) = dao.insert(rule)

    suspend fun update(rule: SpamRule) = dao.update(rule)

    suspend fun delete(rule: SpamRule) = dao.delete(rule)

    suspend fun matchesAny(input: String): SpamRule? {
        val rules = dao.getActiveBlockingRules()
        for (rule in rules) {
            when (rule.type) {
                RuleType.EXACT -> if (input == rule.pattern) return rule
                RuleType.PREFIX -> if (input.startsWith(rule.pattern)) return rule
                RuleType.REGEX -> {
                    try {
                        if (Regex(rule.pattern).matches(input)) return rule
                    } catch (_: PatternSyntaxException) {
                        // ignore malformed rules
                    }
                }
            }
        }
        return null
    }

    /**
     * Single source of truth for whether a phone number should be blocked: true if it was
     * manually blocked OR it matches an active blocking [SpamRule]. If a rule match is what
     * triggers the block, the number is also persisted to [BlockedNumberDao] (if not already
     * there) so it stays blocked even if that rule is later edited or deleted.
     */
    suspend fun isNumberBlocked(number: String): Boolean {
        val normalized = PhoneNumberNormalizer.normalize(number)
        if (blockedNumberDao.isBlocked(normalized)) return true
        val matchedRule = matchesAny(number) ?: return false
        blockedNumberDao.insert(BlockedNumber(number = normalized, reason = matchedRule.label))
        return true
    }

    /** Records that a blocked number just attempted a call, for tracking how often it retries. */
    suspend fun recordBlockedAttempt(number: String) {
        blockedNumberDao.recordAttempt(PhoneNumberNormalizer.normalize(number))
    }
}
