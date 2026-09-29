package com.callassistant.util

/**
 * Detects USSD/MMI codes which are handled by the carrier, not as regular phone calls.
 * 
 * USSD (Unstructured Supplementary Service Data) codes are used for:
 * - Balance checks (*123#)
 * - Call forwarding settings (##002#)
 * - IMEI display (*#06#)
 * - Carrier menus (*100#)
 * 
 * These codes trigger a system dialog rather than a phone call, so we shouldn't
 * show our in-call UI for them.
 */
object UssdDetector {

    /**
     * Returns true if the given number appears to be a USSD/MMI code.
     */
    fun isUssdCode(number: String): Boolean {
        val trimmed = number.trim()
        if (trimmed.isEmpty()) return false
        
        // Must contain at least one special character (* or #)
        if (!trimmed.contains('*') && !trimmed.contains('#')) return false
        
        return when {
            // Standard USSD: starts with * and ends with #
            trimmed.startsWith("*") && trimmed.endsWith("#") -> true
            // Interrogation codes: start with *# 
            trimmed.startsWith("*#") -> true
            // Deactivation codes: start with ##
            trimmed.startsWith("##") -> true
            // Registration codes: start with **
            trimmed.startsWith("**") -> true
            // Erasure codes: start with #
            trimmed.startsWith("#") && trimmed.endsWith("#") -> true
            // Any short code with # that's not a regular phone number
            // (regular international numbers are typically 7+ digits without #)
            trimmed.contains("#") && trimmed.length < 15 && !isLikelyPhoneNumber(trimmed) -> true
            else -> false
        }
    }
    
    /**
     * Simple heuristic to check if a string looks like a regular phone number
     * rather than a USSD code.
     */
    private fun isLikelyPhoneNumber(number: String): Boolean {
        // Count actual digits
        val digitCount = number.count { it.isDigit() }
        // Regular phone numbers have many digits and may start with + but don't contain #
        // If it contains # and has few digits, it's likely USSD
        return digitCount >= 7 && !number.contains('#')
    }
}
