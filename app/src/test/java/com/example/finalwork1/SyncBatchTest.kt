package com.example.finalwork1

import com.example.finalwork1.data.sync.SyncBatch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncBatchTest {
    @Test fun `continues to later pages after a failure`() = runTest {
        val attempts = mutableListOf<Int>()
        val result = SyncBatch { page -> attempts += page; page != 2 }.run(emptySet(), 5, 5)
        assertEquals(listOf(1, 2, 3, 4, 5), attempts)
        assertEquals(4, result.succeeded)
        assertEquals(1, result.failed)
        assertTrue(result.shouldRetry)
    }

    @Test fun `exceptions count as failed pages without stopping batch`() = runTest {
        val attempts = mutableListOf<Int>()
        val result = SyncBatch { page -> attempts += page; if (page == 2) error("offline"); true }.run(emptySet(), 3, 3)
        assertEquals(listOf(1, 2, 3), attempts)
        assertTrue(result.shouldRetry)
    }

    @Test fun `cancellation is never converted to a failed page`() = runTest {
        var propagated = false
        try {
            SyncBatch { throw kotlinx.coroutines.CancellationException("stop") }.run(emptySet(), 2, 2)
        } catch (_: kotlinx.coroutines.CancellationException) {
            propagated = true
        }
        assertTrue(propagated)
    }

    @Test fun `planner is used by production entry point and progress updates after each page`() = runTest {
        val completed = mutableListOf<Int>()
        val pages = mutableListOf<Int>()
        val result = SyncBatch { page -> pages += page; true }.run(
            cachedPages = setOf(1, 3), totalPages = 4, preloadCount = 4
        ) { completed += it.completed }
        assertEquals(listOf(2, 4), pages)
        assertEquals(listOf(0, 1, 2), completed)
        assertEquals(2, result.succeeded)
    }

    @Test fun `only requested stale pages are retried and later pages run`() = runTest {
        val attempts = mutableListOf<Int>()
        val result = SyncBatch { page -> attempts.add(page); page != 2 }.runPages(listOf(5, 2, 5))
        assertEquals(listOf(2, 5), attempts)
        assertEquals(1, result.failed)
        assertEquals(1, result.succeeded)
    }
}
