package com.kazumaproject.markdownhelperkeyboard.converter.utility

import org.junit.Assert.assertEquals
import org.junit.Test

class DialTimeDateConverterTest {

    @Test
    fun testCase_aka() {
        val result = DialTimeDateConverter.generateCandidates("あか")
        val expected = listOf("12", "12:00", "12時", "12日", "1:02", "1時2分", "1月2日", "1/2")
        assertEquals(expected, result)
    }

    @Test
    fun testCase_asa() {
        val result = DialTimeDateConverter.generateCandidates("あさ")
        val expected = listOf("13", "13:00", "13時", "13日", "1:03", "1時3分", "1月3日", "1/3")
        assertEquals(expected, result)
    }

    @Test
    fun testCase_akakasa() {
        val result = DialTimeDateConverter.generateCandidates("あかかさ")
        val expected = listOf("1223", "12:23", "12時23分", "12月23日", "12/23")
        assertEquals(expected, result)
    }

    @Test
    fun testCase_aaha() {
        val result = DialTimeDateConverter.generateCandidates("ああは")
        val expected = listOf("116", "1:16", "1時16分", "1月16日", "1/16", "11:06", "11時6分", "11月6日", "11/6")
        assertEquals(expected, result)
    }

    @Test
    fun testCase_asawa() {
        val result = DialTimeDateConverter.generateCandidates("あさわ")
        val expected = listOf("130", "1:30", "1時30分", "1時半", "1月30日", "1/30", "13:00", "13時0分")
        assertEquals(expected, result)
    }

    @Test
    fun testCase_ayasa() {
        val result = DialTimeDateConverter.generateCandidates("あやさ")
        val expected = listOf("183", "18:03", "18時3分")
        assertEquals(expected, result)
    }

    @Test
    fun testCase_rara() {
        val result = DialTimeDateConverter.generateCandidates("らら")
        val expected = listOf("99", "9:09", "9時9分", "9月9日", "9/9")
        assertEquals(expected, result)
    }

    @Test
    fun testCase_a() {
        val result = DialTimeDateConverter.generateCandidates("あ")
        val expected = listOf("1")
        assertEquals(expected, result)
    }

    @Test
    fun testCase_ai() {
        val result = DialTimeDateConverter.generateCandidates("あい")
        val expected = emptyList<String>()
        assertEquals(expected, result)
    }
}
