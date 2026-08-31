package com.callassistant.data.repository

import com.callassistant.data.db.BlockedNumberDao
import com.callassistant.data.db.SpamRuleDao
import com.callassistant.data.entity.BlockedNumber
import com.callassistant.data.entity.RuleType
import com.callassistant.data.entity.SpamRule
import com.callassistant.util.PhoneNumberNormalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.regex.PatternSyntaxException

@Singleton
class SpamRuleRepositoryImpl @Inject constructor(
    private val dao: SpamRuleDao,
    private val blockedNumberDao: BlockedNumberDao
) : SpamRuleRepository {

    override val allRules: Flow<List<SpamRule>> = dao.getAll()

    override val allBlockedNumbers: Flow<List<BlockedNumber>> = blockedNumberDao.getAll()

    override suspend fun add(rule: SpamRule) {
        dao.insert(rule)
    }

    override suspend fun update(rule: SpamRule) {
        dao.update(rule)
    }

    override suspend fun delete(rule: SpamRule) {
        dao.delete(rule)
    }

    override suspend fun matchesAny(input: String): SpamRule? {
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

    override suspend fun isNumberBlocked(number: String): Boolean {
        val normalized = PhoneNumberNormalizer.normalize(number)
        if (blockedNumberDao.isBlocked(normalized)) return true
        val matchedRule = matchesAny(number) ?: return false
        blockedNumberDao.insert(BlockedNumber(number = normalized, reason = matchedRule.label))
        return true
    }

    override suspend fun recordBlockedAttempt(number: String) {
        blockedNumberDao.recordAttempt(PhoneNumberNormalizer.normalize(number))
    }

    override suspend fun blockNumber(number: String, reason: String, name: String?) {
        val normalized = PhoneNumberNormalizer.normalize(number)
        blockedNumberDao.insert(BlockedNumber(number = normalized, name = name, reason = reason))
    }

    override suspend fun unblockNumber(number: String) {
        blockedNumberDao.delete(PhoneNumberNormalizer.normalize(number))
    }

    override suspend fun seedDefaultsIfEmpty() {
        if (dao.getAll().first().isNotEmpty()) return
        dao.insert(
            SpamRule(
                pattern = "+1900",
                type = RuleType.PREFIX,
                label = "Premium/scam prefix",
                isBlocking = true
            )
        )
        dao.insert(
            SpamRule(
                pattern = "^(\\d)\\1{6,}$",
                type = RuleType.REGEX,
                label = "Repeated-digit spoofed number",
                isBlocking = true
            )
        )
        dao.insert(
            SpamRule(
                pattern = "^0*123456789\$|^0*987654321\$",
                type = RuleType.REGEX,
                label = "Sequential-digit spoofed number",
                isBlocking = true
            )
        )
        dao.insert(
            SpamRule(
                pattern = "000000000",
                type = RuleType.EXACT,
                label = "Known spam number",
                isBlocking = true
            )
        )
        dao.insert(
            SpamRule(
                pattern = "^\\+?880?1[3-9]\\d{8}$",
                type = RuleType.REGEX,
                label = "Unverified BD mobile pattern",
                isBlocking = false
            )
        )
        dao.insert(
            SpamRule(
                pattern = "you have won",
                type = RuleType.PREFIX,
                label = "Prize/lottery scam wording",
                isBlocking = false
            )
        )
    }
}
