package com.callassistant.util

/**
 * Normalizes phone numbers for comparison/storage so the same real-world number is always
 * represented identically, regardless of formatting differences between sources (contacts,
 * call log, telecom call handles, manual entry) e.g. "+8801712345678", "01712345678",
 * "1712345678", "017-1234-5678" should all resolve to the same key.
 *
 * Strategy: strip all non-digit characters, then keep only the last 10 digits (the typical
 * length of a local subscriber number once country/trunk code is stripped). Numbers shorter
 * than 10 digits (e.g. short codes) are kept as-is.
 */
object PhoneNumberNormalizer {
    fun normalize(number: String): String {
        val digits = number.filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }
}
