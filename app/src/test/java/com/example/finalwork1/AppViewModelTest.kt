package com.example.finalwork1

import com.example.finalwork1.data.PreferencesRepository
import com.example.finalwork1.data.AppPreferences
import com.example.finalwork1.data.repo.CharacterRepository
import com.example.finalwork1.data.sync.SyncScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private val scheduler: SyncScheduler = mockk(relaxed = true)

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.characters } returns flowOf(emptyList())
        every { repository.collections } returns flowOf(emptyList())
        every { repository.history } returns flowOf(emptyList())
        every { preferences.preferences } returns flowOf(AppPreferences())
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `initialization schedules periodic sync`() = runTest(dispatcher) {
        coEvery { repository.isCacheStale(any()) } returns false
        AppViewModel(repository, preferences, scheduler)
        advanceUntilIdle()
        verify(exactly = 1) { scheduler.schedule() }
        coVerify(exactly = 0) { repository.refresh(any()) }
    }

    @Test fun `stale cache triggers refresh`() = runTest(dispatcher) {
        coEvery { repository.isCacheStale(any()) } returns true
        coEvery { repository.refresh(any()) } returns Result.success(20)
        val model = AppViewModel(repository, preferences, scheduler)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.refresh(any()) }
        assertEquals(false, model.busy.value)
    }
}
