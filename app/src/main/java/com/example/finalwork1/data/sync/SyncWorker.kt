package com.example.finalwork1.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.finalwork1.data.PreferencesRepository
import com.example.finalwork1.data.repo.CharacterRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: CharacterRepository,
    private val preferencesRepository: PreferencesRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val preload = preferencesRepository.preferences.first().preloadPages
        val knownTotal = repository.getTotalPages() ?: Int.MAX_VALUE
        val stale = repository.stalePages(preload)
        // Cached pages are treated as fresh input, so planner selects exactly the stale subset.
        val fresh = (1..minOf(preload, knownTotal)).toSet() - stale.toSet()
        val outcome = SyncBatch { page -> repository.refresh(page).isSuccess }.run(
            fresh, minOf(preload, knownTotal), preload
        ) { progress ->
            setProgress(workDataOf(
                "completed" to progress.completed, "total" to progress.total,
                "updated" to progress.succeeded, "failed" to progress.failed
            ))
        }
        if (!outcome.shouldRetry) Result.success()
        else Result.retry() // Periodic work must remain scheduled; retry until constraints allow success.
    }
}
