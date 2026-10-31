package com.example.domain

import java.util.Locale
import kotlin.math.roundToInt

data class ValidationResult(
    val isValid: Boolean,
    val parsedValue: Float?,
    val errorMessage: String? = null
)

object DecimalInputValidator {

    /**
     * Sanitizes raw text input to allow valid decimal numbers.
     * Replaces comma with period, removes any invalid characters, allows at most one decimal point.
     */
    fun sanitizeDecimalInput(input: String, maxDecimals: Int = 2): String {
        val trimmed = input.trim().replace(',', '.')
        val builder = StringBuilder()
        var hasDecimalPoint = false
        var decimalPlaces = 0

        for (ch in trimmed) {
            when {
                ch.isDigit() -> {
                    if (hasDecimalPoint) {
                        if (decimalPlaces < maxDecimals) {
                            builder.append(ch)
                            decimalPlaces++
                        }
                    } else {
                        builder.append(ch)
                    }
                }
                ch == '.' -> {
                    if (!hasDecimalPoint) {
                        // If decimal is first character, prefix with 0
                        if (builder.isEmpty()) {
                            builder.append('0')
                        }
                        builder.append('.')
                        hasDecimalPoint = true
                    }
                }
            }
        }
        return builder.toString()
    }

    /**
     * Parses a string safely into a Float.
     */
    fun parseDecimal(text: String): Float? {
        val sanitized = text.trim().replace(',', '.')
        return sanitized.toFloatOrNull()
    }

    /**
     * Validates that the input is a valid positive decimal within physical limits.
     */
    fun validateRange(
        text: String,
        min: Float,
        max: Float,
        fieldName: String = "Value",
        unitSymbol: String = ""
    ): ValidationResult {
        if (text.isBlank()) {
            return ValidationResult(isValid = false, parsedValue = null, errorMessage = "$fieldName cannot be empty")
        }
        val value = parseDecimal(text)
            ?: return ValidationResult(isValid = false, parsedValue = null, errorMessage = "Please enter a valid numeric $fieldName")

        if (value < min) {
            return ValidationResult(
                isValid = false,
                parsedValue = value,
                errorMessage = "$fieldName must be at least $min $unitSymbol".trim()
            )
        }
        if (value > max) {
            return ValidationResult(
                isValid = false,
                parsedValue = value,
                errorMessage = "$fieldName cannot exceed $max $unitSymbol".trim()
            )
        }
        return ValidationResult(isValid = true, parsedValue = value, errorMessage = null)
    }

    /**
     * Formats a float value to the appropriate decimal representation.
     * If the value is a whole number, can optionally display without trailing zeros or with 1 decimal.
     */
    fun formatDecimal(value: Float, decimals: Int = 1): String {
        return if (decimals == 0) {
            value.roundToInt().toString()
        } else {
            String.format(Locale.US, "%.${decimals}f", value)
        }
    }
}
