package com.example.finalwork1

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

class AppViewModel(
    private val repository: CharacterRepository,
    private val preferencesRepository: PreferencesRepository,
    private val scheduleSync: () -> Unit,
) : ViewModel() {

    val characters = repository.characters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val collections = repository.collections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val history = repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val preferences = preferencesRepository.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.example.finalwork1.data.AppPreferences())
    val syncMessage = kotlinx.coroutines.flow.MutableStateFlow("Локальная база готова")
    val busy = kotlinx.coroutines.flow.MutableStateFlow(false)

    init {
        scheduleSync()
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

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AppViewModel::class.java))
            val repository = CharacterRepository(AppDatabase.get(appContext).dao(), RickMortyApi.create())
            val preferences = PreferencesRepository(appContext)
            return AppViewModel(repository, preferences) {
                val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                val request = PeriodicWorkRequestBuilder<SyncWorker>(12, TimeUnit.HOURS)
                    .setConstraints(constraints).build()
                WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                    "catalog_refresh", ExistingPeriodicWorkPolicy.KEEP, request
                )
            } as T
        }
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
