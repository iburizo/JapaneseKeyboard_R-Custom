package com.kazumaproject.markdownhelperkeyboard.converter.utility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DialTimeDateConverterTest {

    @Test
    fun testConvertAdanToNumber() {
        assertEquals("12", DialTimeDateConverter.convertAdanToNumber("あか"))
        assertEquals("345", DialTimeDateConverter.convertAdanToNumber("さたな"))
        assertEquals("123,456.7890", DialTimeDateConverter.convertAdanToNumber("あかさ、たなは。まやらわ"))
    }

    @Test
    fun testGenerateCandidates() {
        // "あか" -> "12"
        val candidates12 = DialTimeDateConverter.generateCandidates("あか")
        assertTrue(candidates12.contains("12"))
        assertTrue(candidates12.contains("12:00"))
        assertTrue(candidates12.contains("12時"))
        assertTrue(candidates12.contains("12日"))
        assertTrue(candidates12.contains("1:02"))
        assertTrue(candidates12.contains("1時2分"))
        assertTrue(candidates12.contains("1月2日"))
        assertTrue(candidates12.contains("1/2"))

        // "あさわ" -> "130"
        val candidates130 = DialTimeDateConverter.generateCandidates("あさわ")
        assertTrue(candidates130.contains("130"))
        assertTrue(candidates130.contains("1:30"))
        assertTrue(candidates130.contains("1時30分"))
        assertTrue(candidates130.contains("1時半"))
        assertTrue(candidates130.contains("1月30日"))
        assertTrue(candidates130.contains("1/30"))
        assertTrue(candidates130.contains("13:00"))

        // "わ。ああは" -> "0.116"
        val candidatesDecimal = DialTimeDateConverter.generateCandidates("わ。ああは")
        assertTrue(candidatesDecimal.contains("0.116"))

        // "か、わわわ" -> "2,000"
        val candidatesComma = DialTimeDateConverter.generateCandidates("か、わわわ")
        assertTrue(candidatesComma.contains("2,000"))

        // Calculation: "12+3" -> "15" (Wait, DialTimeDateConverter doesn't map symbols to plus! Assuming string literal "12+3")
        val candidatesCalc1 = DialTimeDateConverter.generateCandidates("12+3")
        assertTrue(candidatesCalc1.contains("15") || candidatesCalc1.contains("15.0") || candidatesCalc1.contains("15.000") || candidatesCalc1.contains("15.000") || "15" in candidatesCalc1.map { it.replace(".000", "").replace(".0", "") })

        val candidatesCalc2 = DialTimeDateConverter.generateCandidates("2^3")
        assertTrue(candidatesCalc2.contains("8") || "8" in candidatesCalc2.map { it.replace(".000", "").replace(".0", "") })
        
        val candidatesCalc3 = DialTimeDateConverter.generateCandidates("10%3")
        assertTrue(candidatesCalc3.contains("1") || "1" in candidatesCalc3.map { it.replace(".000", "").replace(".0", "") })
    }
}
