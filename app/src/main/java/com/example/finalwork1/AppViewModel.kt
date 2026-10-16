package com.example.finalwork1

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.finalwork1.data.PreferencesRepository
import com.example.finalwork1.data.local.AppDatabase
import com.example.finalwork1.data.local.CharacterEntity
import com.example.finalwork1.data.local.UserCollectionEntity
import com.example.finalwork1.data.remote.RickMortyApi
import com.example.finalwork1.data.repo.CharacterRepository
import com.example.finalwork1.data.sync.SyncWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).dao()
    private val repository = CharacterRepository(dao, RickMortyApi.create())
    private val preferencesRepository = PreferencesRepository(application)

    val characters = repository.characters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val collections = repository.collections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val history = repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val preferences = preferencesRepository.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.example.finalwork1.data.AppPreferences())
    val syncMessage = kotlinx.coroutines.flow.MutableStateFlow("Локальная база готова")
    val busy = kotlinx.coroutines.flow.MutableStateFlow(false)

    init {
        scheduleBackgroundSync()
        viewModelScope.launch {
            // Кэш остаётся источником UI; сеть обновляет его только при истечении TTL.
            if (repository.isCacheStale()) refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            busy.value = true
            val result = repository.refresh()
            syncMessage.value = result.fold(
                { "Обновлено: $it персонажей · ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}" },
                { "Нет соединения. Используются сохранённые данные." },
            )
            busy.value = false
        }
    }

    private fun scheduleBackgroundSync() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = PeriodicWorkRequestBuilder<SyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints).build()
        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            "catalog_refresh", ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    fun track(id: Int, value: String) = viewModelScope.launch { repository.setTracking(id, value) }
    fun pin(id: Int, value: Boolean) = viewModelScope.launch { repository.setPinned(id, value) }
    fun hide(id: Int, value: Boolean) = viewModelScope.launch { repository.setHidden(id, value) }
    fun note(id: Int, value: String) = viewModelScope.launch { repository.setNote(id, value) }
    fun rate(id: Int, value: Int) = viewModelScope.launch { repository.setRating(id, value) }
    fun visit(character: CharacterEntity) = viewModelScope.launch { repository.visit(character) }
    fun createCollection(title: String, description: String = "") = viewModelScope.launch {
        runCatching { repository.createCollection(title, description) }
            .onSuccess { syncMessage.value = "Коллекция создана" }
            .onFailure { syncMessage.value = "Название уже используется или не удалось создать коллекцию" }
    }
    fun deleteCollection(id: Long) = viewModelScope.launch { repository.deleteCollection(id) }
    fun addToCollection(collectionId: Long, characterId: Int) = viewModelScope.launch { repository.addToCollection(collectionId, characterId) }
    fun removeFromCollection(collectionId: Long, characterId: Int) = viewModelScope.launch { repository.removeFromCollection(collectionId, characterId) }
    fun setShowHidden(value: Boolean) = viewModelScope.launch { preferencesRepository.setShowHidden(value) }
    fun setPreloadPages(value: Int) = viewModelScope.launch { preferencesRepository.setPreloadPages(value) }
    fun setDarkTheme(value: Boolean) = viewModelScope.launch { preferencesRepository.setDarkTheme(value) }
    fun collectionCharacters(id: Long) = repository.collectionCharacters(id)
}
