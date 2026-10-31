package com.example.finalwork1.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.Configuration
import com.example.finalwork1.data.AppPreferences
import com.example.finalwork1.data.PreferencesRepository
import com.example.finalwork1.data.repo.CharacterRepository
import com.example.finalwork1.data.sync.SyncWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import android.content.Context
import java.util.concurrent.TimeUnit

/** Runs the real CoroutineWorker with injected fakes using WorkManager's testing API. */
@RunWith(AndroidJUnit4::class)
class SyncWorkerInstrumentationTest {
    private fun worker(repository: CharacterRepository): SyncWorker {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().build())
        val preferences = mockk<PreferencesRepository>()
        every { preferences.preferences } returns flowOf(AppPreferences(preloadPages = 3))
        val factory = object : WorkerFactory() {
            override fun createWorker(context: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
                if (workerClassName == SyncWorker::class.java.name)
                    SyncWorker(context, workerParameters, repository, preferences) else null
        }
        return TestListenableWorkerBuilder<SyncWorker>(context).setWorkerFactory(factory).build()
    }

    @Test fun onlyStalePagesAreRequestedAndFailuresCauseRetry() {
        val repository = mockk<CharacterRepository>()
        coEvery { repository.getTotalPages() } returns 3
        coEvery { repository.stalePages(3, any()) } returns listOf(2)
        coEvery { repository.refresh(2) } returns kotlin.Result.failure(IllegalStateException("offline"))
        val result = worker(repository).startWork().get(15, TimeUnit.SECONDS)
        assertEquals(ListenableWorker.Result.retry(), result)
        coVerify(exactly = 1) { repository.refresh(2) }
        coVerify(exactly = 0) { repository.refresh(1) }
        coVerify(exactly = 0) { repository.refresh(3) }
    }

    @Test fun successfulBatchCompletesWithoutRedownloadingFreshPages() {
        val repository = mockk<CharacterRepository>()
        coEvery { repository.getTotalPages() } returns 3
        coEvery { repository.stalePages(3, any()) } returns listOf(3)
        coEvery { repository.refresh(3) } returns kotlin.Result.success(20)
        val result = worker(repository).startWork().get(15, TimeUnit.SECONDS)
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { repository.refresh(3) }
        coVerify(exactly = 0) { repository.refresh(1) }
    }
}
