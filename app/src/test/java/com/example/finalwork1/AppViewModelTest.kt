package com.example.finalwork1

import com.example.finalwork1.data.PreferencesRepository
import com.example.finalwork1.data.AppPreferences
import com.example.finalwork1.data.repo.CharacterRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository: CharacterRepository = mockk()
    private val preferences: PreferencesRepository = mockk()
    private val scheduler: com.example.finalwork1.data.sync.SyncScheduler = mockk(relaxed = true)

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.characters } returns flowOf(emptyList())
        every { repository.collections } returns flowOf(emptyList())
        every { repository.history } returns flowOf(emptyList())
        every { repository.totalPages } returns flowOf(null)
        every { repository.membershipCounts } returns flowOf(emptyList())
        every { preferences.preferences } returns flowOf(AppPreferences())
        coEvery { repository.getTotalPages() } returns null
        coEvery { repository.cachedPageNumbers() } returns emptySet()
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `fresh cache does not refresh`() = runTest(dispatcher) {
        coEvery { repository.highestCachedPage() } returns 0
        coEvery { repository.isCacheStale(any(), any()) } returns false
        AppViewModel(repository, preferences, scheduler)
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.refresh(any()) }
    }

    @Test fun `stale cache triggers refresh`() = runTest(dispatcher) {
        coEvery { repository.highestCachedPage() } returns 0
        coEvery { repository.isCacheStale(any(), any()) } returns true
        coEvery { repository.refresh(any()) } returns Result.success(20)
        coEvery { repository.stalePages(any(), any()) } returns listOf(1, 2)
        val model = AppViewModel(repository, preferences, scheduler)
        advanceUntilIdle()
        coVerify(exactly = 2) { repository.refresh(any()) }
        assertEquals(false, model.busy.value)
    }

    @Test fun `next page fills gap in cache`() = runTest(dispatcher) {
        coEvery { repository.highestCachedPage() } returns 3
        coEvery { repository.isCacheStale(any(), any()) } returns false
        coEvery { repository.cachedPageNumbers() } returns setOf(1, 3)
        coEvery { repository.refresh(2) } returns Result.success(20)
        val model = AppViewModel(repository, preferences, scheduler)
        advanceUntilIdle()
        model.loadNextPage()
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.refresh(2) }
    }

    @Test fun `missing page keeps load more enabled despite highest cached page`() = runTest(dispatcher) {
        coEvery { repository.highestCachedPage() } returns 3
        coEvery { repository.isCacheStale(any(), any()) } returns false
        coEvery { repository.cachedPageNumbers() } returns setOf(1, 3)
        coEvery { repository.getTotalPages() } returns 3
        coEvery { repository.refresh(2) } returns Result.success(20)
        val model = AppViewModel(repository, preferences, scheduler)
        advanceUntilIdle()
        assertEquals(true, model.hasMorePages.value)
        model.loadNextPage()
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.refresh(2) }
    }

    @Test fun `complete catalog prevents new requests`() = runTest(dispatcher) {
        coEvery { repository.highestCachedPage() } returns 2
        coEvery { repository.isCacheStale(any(), any()) } returns false
        coEvery { repository.cachedPageNumbers() } returns setOf(1, 2)
        coEvery { repository.getTotalPages() } returns 2
        val model = AppViewModel(repository, preferences, scheduler)
        advanceUntilIdle()
        assertEquals(false, model.hasMorePages.value)
        model.loadNextPage()
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.refresh(any()) }
    }

    @Test fun `refresh only stale pages`() = runTest(dispatcher) {
        coEvery { repository.highestCachedPage() } returns 3
        coEvery { repository.isCacheStale(any(), any()) } returns false
        coEvery { repository.stalePages(any(), any()) } returns listOf(2)
        coEvery { repository.refresh(2) } returns Result.success(20)
        val model = AppViewModel(repository, preferences, scheduler)
        advanceUntilIdle()
        model.refresh()
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.refresh(2) }
        coVerify(exactly = 0) { repository.refresh(1) }
        coVerify(exactly = 0) { repository.refresh(3) }
    }

    @Test fun `preload setting immediately downloads missing pages`() = runTest(dispatcher) {
        coEvery { repository.highestCachedPage() } returns 2
        coEvery { repository.isCacheStale(any(), any()) } returns false
        coEvery { preferences.setPreloadPages(5) } returns Unit
        coEvery { repository.stalePages(5, any()) } returns listOf(3, 4, 5)
        coEvery { repository.refresh(any()) } returns Result.success(20)
        val model = AppViewModel(repository, preferences, scheduler)
        advanceUntilIdle()
        model.setPreloadPages(5)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.refresh(3) }
        coVerify(exactly = 1) { repository.refresh(4) }
        coVerify(exactly = 1) { repository.refresh(5) }
    }
}
