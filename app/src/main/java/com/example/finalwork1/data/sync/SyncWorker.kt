package com.example.finalwork1.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.finalwork1.data.PreferencesRepository
import com.example.finalwork1.data.repo.CharacterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: CharacterRepository,
    private val preferencesRepository: PreferencesRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val preloadPages = preferencesRepository.preferences.first().preloadPages
        var failed = false
        for (page in 1..preloadPages) {
            if (repository.refresh(page).isFailure) { failed = true; break }
        }
        if (!failed) Result.success() else if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
