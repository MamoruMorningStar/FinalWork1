package com.example.finalwork1.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionRulesTest {
    @Test
    fun rejectsDuplicates() {
        assertTrue(CollectionRules.canAdd(emptySet(), 1))
        assertFalse(CollectionRules.canAdd(setOf(1), 1))
        assertFalse(CollectionRules.canAdd(emptySet(), 0))
    }

    @Test
    fun normalizeTitleTrimsTo80() {
        val long = "A".repeat(200)
        val result = CollectionRules.normalizeTitle(long)
        assertEquals(80, result.length)
    }

    @Test
    fun normalizeTitleTrimsWhitespace() {
        assertEquals("Rick", CollectionRules.normalizeTitle("  Rick  "))
    }

    @Test
    fun normalizeTitleEmptyAfterTrimIsEmptyString() {
        // blank → при вставке в createCollection будет заменено на "Без названия",
        // но сама normalizeTitle возвращает "" (trim + take не вставляют дефолт)
        assertEquals("", CollectionRules.normalizeTitle("   "))
    }
}
