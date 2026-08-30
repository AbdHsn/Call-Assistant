package com.callassistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callassistant.data.entity.BlockedNumber
import com.callassistant.data.entity.RuleType
import com.callassistant.data.entity.SpamRule
import com.callassistant.data.repository.SpamRuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SpamRulesUiState(
    val spamRules: List<SpamRule> = emptyList(),
    val blockedNumbers: List<BlockedNumber> = emptyList()
)

@HiltViewModel
class SpamRulesViewModel @Inject constructor(
    private val spamRuleRepository: SpamRuleRepository
) : ViewModel() {

    val uiState: StateFlow<SpamRulesUiState> = combine(
        spamRuleRepository.allRules,
        spamRuleRepository.allBlockedNumbers
    ) { spamRules, blockedNumbers ->
        SpamRulesUiState(spamRules = spamRules, blockedNumbers = blockedNumbers)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SpamRulesUiState())

    fun addSpamRule(pattern: String, type: RuleType, label: String, isBlocking: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            spamRuleRepository.add(
                SpamRule(pattern = pattern, type = type, label = label, isBlocking = isBlocking)
            )
        }
    }

    fun deleteSpamRule(rule: SpamRule) {
        viewModelScope.launch(Dispatchers.IO) {
            spamRuleRepository.delete(rule)
        }
    }

    fun blockNumber(number: String, reason: String = "Manually blocked", name: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            spamRuleRepository.blockNumber(number, reason, name)
        }
    }

    fun unblockNumber(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            spamRuleRepository.unblockNumber(number)
        }
    }
}
