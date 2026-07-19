package com.callassistant.data.repository

import com.callassistant.data.db.SpamRuleDao
import com.callassistant.data.entity.RuleType
import com.callassistant.data.entity.SpamRule
import kotlinx.coroutines.flow.Flow
import java.util.regex.PatternSyntaxException

class SpamRuleRepository(private val dao: SpamRuleDao) {

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
}
