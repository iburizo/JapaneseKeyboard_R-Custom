package com.kazumaproject.markdownhelperkeyboard.converter.utility

import java.util.Locale

object DialTimeDateConverter {

    private val A_DAN_MAP = mapOf(
        'あ' to '1', 'か' to '2', 'さ' to '3', 'た' to '4', 'な' to '5',
        'は' to '6', 'ま' to '7', 'や' to '8', 'ら' to '9', 'わ' to '0'
    )

    fun toDialNumbers(text: String): String? {
        if (text.isEmpty()) return null
        val sb = StringBuilder()
        for (ch in text) {
            val num = A_DAN_MAP[ch] ?: return null
            sb.append(num)
        }
        return sb.toString()
    }

    fun generateCandidates(rawInput: String): List<String> {
        val numbers = toDialNumbers(rawInput) ?: return emptyList()
        val candidates = linkedSetOf<String>()
        candidates.add(numbers)

        when (numbers.length) {
            2 -> {
                val val2 = numbers.toIntOrNull() ?: return emptyList()
                // 1. 正時・日（例: 12 -> 12:00, 12時, 12日）
                if (val2 in 0..23) {
                    candidates.add("$val2:00")
                    candidates.add("${val2}時")
                }
                if (val2 in 1..31) {
                    candidates.add("${val2}日")
                }

                // 2. 1桁時 + 1桁分（例: 12 -> 1:02, 1時2分）
                val d1 = numbers.substring(0, 1).toInt()
                val d2 = numbers.substring(1, 2).toInt()
                addTime(candidates, d1, d2)

                // 3. 1桁月 + 1桁日（例: 12 -> 1月2日, 1/2）
                addDate(candidates, d1, d2)
            }
            3 -> {
                val h1 = numbers.substring(0, 1).toInt()
                val m1 = numbers.substring(1, 3).toInt()
                addTime(candidates, h1, m1)
                addDate(candidates, h1, m1)

                val h2 = numbers.substring(0, 2).toInt()
                val m2 = numbers.substring(2, 3).toInt()
                addTime(candidates, h2, m2)
                addDate(candidates, h2, m2)
            }
            4 -> {
                val h = numbers.substring(0, 2).toInt()
                val m = numbers.substring(2, 4).toInt()
                addTime(candidates, h, m)
                addDate(candidates, h, m)
            }
        }
        return candidates.toList()
    }

    private fun addTime(set: MutableSet<String>, hour: Int, minute: Int) {
        if (hour in 0..23 && minute in 0..59) {
            val minStr = String.format(Locale.US, "%02d", minute)
            set.add("$hour:$minStr")
            set.add("${hour}時${minute}分")
            if (minute == 30) set.add("${hour}時半")
        }
    }

    private fun addDate(set: MutableSet<String>, month: Int, day: Int) {
        if (month in 1..12 && isValidDate(month, day)) {
            set.add("${month}月${day}日")
            set.add("$month/$day")
        }
    }

    private fun isValidDate(month: Int, day: Int): Boolean {
        if (day !in 1..31) return false
        val maxDays = when (month) {
            2 -> 29
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return day <= maxDays
    }
}
