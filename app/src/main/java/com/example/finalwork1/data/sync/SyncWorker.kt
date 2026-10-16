package com.example.finalwork1.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.finalwork1.data.PreferencesRepository
import com.example.finalwork1.data.local.AppDatabase
import com.example.finalwork1.data.remote.RickMortyApi
import com.example.finalwork1.data.repo.CharacterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository = CharacterRepository(AppDatabase.get(applicationContext).dao(), RickMortyApi.create())
        val preferences = PreferencesRepository(applicationContext).preferences
        val preloadPages = preferences.first().preloadPages
        var failed = false
        // Page 1 plus the configured number of additional pages are cached for offline use.
        for (page in 1..(preloadPages + 1)) {
            if (repository.refresh(page).isFailure) { failed = true; break }
        }
        if (!failed) Result.success() else if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
