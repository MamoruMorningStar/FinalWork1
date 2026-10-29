package com.example.finalwork1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finalwork1.data.PreferencesRepository
import com.example.finalwork1.data.local.CharacterEntity
import com.example.finalwork1.data.local.UserCollectionEntity
import com.example.finalwork1.data.repo.CharacterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.finalwork1.data.AppPreferences

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: CharacterRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val characters = repository.characters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val collections = repository.collections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val history = repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val preferences = preferencesRepository.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences())
    val syncMessage = MutableStateFlow("Локальная база готова")
    val busy = MutableStateFlow(false)

    private val refreshMutex = Mutex()
    val loadedPages = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            val pages = preferencesRepository.preferences.first().preloadPages
            loadedPages.value = repository.highestCachedPage()
            if (repository.isCacheStale(pages = pages)) refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (!refreshMutex.tryLock()) return@launch
            busy.value = true
            try {
                val count = maxOf(preferences.first().preloadPages, loadedPages.value)
                var updated = 0
                var failures = 0
                for (page in 1..count) {
                    repository.refresh(page).fold(
                        onSuccess = { updated += it },
                        onFailure = { failures++ },
                    )
                }
                loadedPages.value = repository.highestCachedPage()
                syncMessage.value = if (failures == 0) "Обновлено: $updated персонажей"
                    else "Обновлено: $updated · Ошибки загрузки: $failures (кэш сохранён)"
            } finally {
                busy.value = false
                refreshMutex.unlock()
            }
        }
    }

    fun loadNextPage() {
        viewModelScope.launch {
            if (!refreshMutex.tryLock()) return@launch
            busy.value = true
            try {
                val next = maxOf(loadedPages.value, preferences.first().preloadPages) + 1
                repository.refresh(next).fold(
                    onSuccess = { loadedPages.value = next; syncMessage.value = "Загружена страница $next" },
                    onFailure = { syncMessage.value = "Не удалось загрузить страницу $next" },
                )
            } finally {
                busy.value = false
                refreshMutex.unlock()
            }
        }
    }

    fun track(id: Int, value: String) = viewModelScope.launch { repository.setTracking(id, value) }
    fun pin(id: Int, value: Boolean) = viewModelScope.launch { repository.setPinned(id, value) }
    fun hide(id: Int, value: Boolean) = viewModelScope.launch { repository.setHidden(id, value) }
    fun note(id: Int, value: String) = viewModelScope.launch { repository.setNote(id, value) }
    fun rate(id: Int, value: Int) = viewModelScope.launch { repository.setRating(id, value) }
    fun visit(character: CharacterEntity) = viewModelScope.launch { repository.visit(character) }
    fun createCollection(title: String, description: String = "", onResult: (Boolean) -> Unit = {}) = viewModelScope.launch {
        runCatching { repository.createCollection(title, description) }
            .onSuccess { syncMessage.value = "Коллекция создана"; onResult(true) }
            .onFailure { syncMessage.value = "Название уже используется или не удалось создать коллекцию"; onResult(false) }
    }
    fun editCollection(id: Long, title: String, description: String, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        runCatching { repository.editCollection(id, title, description) }
            .onSuccess { onResult(true) }
            .onFailure { onResult(false) }
    }
    fun deleteCollection(id: Long) = viewModelScope.launch { repository.deleteCollection(id) }
    fun addToCollection(collectionId: Long, characterId: Int) = viewModelScope.launch { repository.addToCollection(collectionId, characterId) }
    fun removeFromCollection(collectionId: Long, characterId: Int) = viewModelScope.launch { repository.removeFromCollection(collectionId, characterId) }
    fun setShowHidden(value: Boolean) = viewModelScope.launch { preferencesRepository.setShowHidden(value) }
    fun setPreloadPages(value: Int) = viewModelScope.launch {
        preferencesRepository.setPreloadPages(value)
        refresh()
    }
    fun setDarkTheme(value: Boolean) = viewModelScope.launch { preferencesRepository.setDarkTheme(value) }
    fun collectionCharacters(id: Long) = repository.collectionCharacters(id)
}
