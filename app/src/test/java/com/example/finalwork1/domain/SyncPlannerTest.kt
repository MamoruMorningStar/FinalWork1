package com.example.finalwork1.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncPlannerTest {
    @Test
    fun preloadsMissingPages() {
        assertEquals(listOf(2, 3), SyncPlanner.pagesToPreload(setOf(1), totalPages = 42, preloadCount = 2))
        assertEquals(emptyList<Int>(), SyncPlanner.pagesToPreload(setOf(1, 2), totalPages = 2, preloadCount = 3))
    }

    @Test
    fun negativeArgsReturnEmpty() {
        assertEquals(emptyList<Int>(), SyncPlanner.pagesToPreload(emptySet(), totalPages = 0, preloadCount = 2))
        assertEquals(emptyList<Int>(), SyncPlanner.pagesToPreload(setOf(1), totalPages = 5, preloadCount = 0))
        assertEquals(emptyList<Int>(), SyncPlanner.pagesToPreload(setOf(1), totalPages = -1, preloadCount = 2))
    }

    @Test
    fun preloadCountRespectedAndOrderedAsc() {
        val pages = SyncPlanner.pagesToPreload(
            cachedPages = setOf(3, 5),
            totalPages = 10,
            preloadCount = 3,
        )
        // пропущенные в порядке возрастания: 1, 2, 4, 6, 7, 8, 9, 10 — берём первые 3
        assertEquals(listOf(1, 2, 4), pages)
    }

    @org.junit.Test fun `next page fills holes before advancing`() {
        org.junit.Assert.assertEquals(2, SyncPlanner.nextMissingPage(setOf(1, 3, 4), 2))
        org.junit.Assert.assertEquals(4, SyncPlanner.nextMissingPage(setOf(1, 2, 3), 2))
        org.junit.Assert.assertEquals(1, SyncPlanner.nextMissingPage(emptySet(), 5))
    }
}
