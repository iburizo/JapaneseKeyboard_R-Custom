package com.kazumaproject.markdownhelperkeyboard.converter.utility

import java.util.Calendar

object DialTimeDateConverter {

    private val adanToNumberMap = mapOf(
        'あ' to '1',
        'か' to '2',
        'さ' to '3',
        'た' to '4',
        'な' to '5',
        'は' to '6',
        'ま' to '7',
        'や' to '8',
        'ら' to '9',
        'わ' to '0',
        '、' to ',',
        '。' to '.'
    )

    fun convertAdanToNumber(text: String): String {
        val builder = StringBuilder()
        for (char in text) {
            builder.append(adanToNumberMap[char] ?: char)
        }
        return builder.toString()
    }

    fun generateCandidates(text: String): List<String> {
        val numberStr = convertAdanToNumber(text)
        if (numberStr == text || numberStr.isEmpty()) return emptyList()

        val candidates = mutableListOf<String>()
        candidates.add(numberStr)

        if (numberStr.length == 2) {
            val num = numberStr.toIntOrNull()
            if (num != null && num in 0..23) {
                candidates.add("$numberStr:00")
                candidates.add("${numberStr}時")
                candidates.add("${numberStr}日")
            }
            if (numberStr[0] in '0'..'9' && numberStr[1] in '0'..'9') {
                candidates.add("${numberStr[0]}:0${numberStr[1]}")
                candidates.add("${numberStr[0]}時${numberStr[1]}分")
                candidates.add("${numberStr[0]}月${numberStr[1]}日")
                candidates.add("${numberStr[0]}/${numberStr[1]}")
            }
        } else if (numberStr.length == 3) {
            val h = numberStr.substring(0, 1).toIntOrNull()
            val m = numberStr.substring(1, 3).toIntOrNull()
            if (h != null && m != null && m in 0..59) {
                candidates.add("$h:${numberStr.substring(1, 3)}")
                candidates.add("${h}時${numberStr.substring(1, 3)}分")
                if (m == 30) candidates.add("${h}時半")
            }
            val month = numberStr.substring(0, 1).toIntOrNull()
            val day = numberStr.substring(1, 3).toIntOrNull()
            if (month != null && day != null && day in 1..31) {
                candidates.add("${month}月${day}日")
                candidates.add("$month/$day")
            }
            if (numberStr.substring(0, 2).toIntOrNull() in 0..23) {
                candidates.add("${numberStr.substring(0, 2)}:0${numberStr.substring(2, 3)}")
            }
        } else if (numberStr.length == 4) {
            val h = numberStr.substring(0, 2).toIntOrNull()
            val m = numberStr.substring(2, 4).toIntOrNull()
            if (h != null && m != null && h in 0..23 && m in 0..59) {
                candidates.add("$h:${numberStr.substring(2, 4)}")
                candidates.add("${h}時${numberStr.substring(2, 4)}分")
            }
            val month = numberStr.substring(0, 2).toIntOrNull()
            val day = numberStr.substring(2, 4).toIntOrNull()
            if (month != null && day != null && month in 1..12 && day in 1..31) {
                candidates.add("${month}月${day}日")
                candidates.add("$month/${numberStr.substring(2, 4)}")
            }
        }

        // Try calculation
        try {
            val result = evaluateExpression(numberStr)
            if (result != null) {
                candidates.add(result)
            }
        } catch (e: Exception) {}

        return candidates.distinct()
    }

    private fun evaluateExpression(expr: String): String? {
        if (!expr.any { it in listOf('+', '-', '*', '/', '^', '%') }) return null
        return try {
            val result = CalculationParser().calculate(expr, AngleMode.DEGREES)
            if (result != null) {
                val decimalFormat = java.text.DecimalFormat("#.###")
                decimalFormat.format(result)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
