package com.example.finalwork1.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachePolicyTest {
    @Test
    fun missingCacheIsStale() {
        assertTrue(CachePolicy.isStale(null, now = 100_000L, ttlMillis = 10_000L))
        assertTrue(CachePolicy.isStale(0L, now = 100_000L, ttlMillis = 10_000L))
    }

    @Test
    fun cacheFreshUntilTtlExpires() {
        assertFalse(CachePolicy.isStale(95_000L, now = 100_000L, ttlMillis = 10_000L))
        assertTrue(CachePolicy.isStale(90_000L, now = 100_000L, ttlMillis = 10_000L))
    }

    @Test
    fun invalidTtlIsStale() {
        assertTrue(CachePolicy.isStale(99_000L, now = 100_000L, ttlMillis = 0L))
    }
}
