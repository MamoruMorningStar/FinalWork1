package com.example.finalwork1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finalwork1.data.PreferencesRepository
import com.example.finalwork1.data.local.CharacterEntity
import com.example.finalwork1.data.repo.CharacterRepository
import com.example.finalwork1.data.sync.SyncScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.finalwork1.data.AppPreferences
import com.example.finalwork1.domain.SyncPlanner
import kotlinx.coroutines.CancellationException
import java.io.IOException
import retrofit2.HttpException

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: CharacterRepository,
    private val preferencesRepository: PreferencesRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    val characters = repository.characters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val collections = repository.collections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val membershipCounts = repository.membershipCounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val totalPages = repository.totalPages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val history = repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val preferences = preferencesRepository.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences())
    val syncMessage = MutableStateFlow("Локальная база готова")
    val busy = MutableStateFlow(false)
    private fun errorLabel(error: Throwable): String = when (error) {
        is IOException -> "Ошибка сети: ${error.message ?: "нет соединения"}"
        is HttpException -> "Ошибка сервера: HTTP ${error.code()}"
        else -> "Ошибка обновления: ${error.message ?: error::class.java.simpleName}"
    }

    private val refreshMutex = Mutex()
    val loadedPages = MutableStateFlow(0)
    val hasMorePages = MutableStateFlow(true)

    private suspend fun updatePageState() {
        val cached = repository.cachedPageNumbers()
        loadedPages.value = cached.size
        val total = repository.getTotalPages()
        hasMorePages.value = total == null || (1..total).any { it !in cached }
    }

    init {
        viewModelScope.launch {
            val pages = preferencesRepository.preferences.first().preloadPages
            updatePageState()
            if (repository.isCacheStale(pages = pages)) refresh()
        }
    }

    private suspend fun refreshPages(pages: List<Int>) {
        refreshMutex.withLock {
            busy.value = true
            try {
                var updated = 0
                val failures = mutableListOf<String>()
                for (page in pages.distinct().sorted()) {
                    repository.refresh(page).fold(
                        onSuccess = { updated += it },
                        onFailure = { failures += "страница $page — ${errorLabel(it)}" },
                    )
                }
                updatePageState()
                if (failures.isNotEmpty()) syncScheduler.scheduleRecovery()
                syncMessage.value = if (failures.isEmpty()) "Обновлено: $updated персонажей"
                    else "Обновлено: $updated · Ошибки: ${failures.joinToString()}"
            } finally {
                busy.value = false
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        val count = maxOf(preferences.first().preloadPages, repository.highestCachedPage(), 1)
        refreshPages(repository.stalePages(count))
    }

    fun loadNextPage() = viewModelScope.launch {
        refreshMutex.withLock {
            busy.value = true
            try {
                val cached = repository.cachedPageNumbers()
                val next = SyncPlanner.nextMissingPage(cached, 1)
                val total = repository.getTotalPages()
                if (total != null && next > total) {
                    updatePageState()
                    syncMessage.value = "Все $total страниц уже загружены"
                    return@withLock
                }
                repository.refresh(next).fold(
                    onSuccess = {
                        updatePageState()
                        syncMessage.value = "Загружена страница $next"
                    },
                    onFailure = { syncScheduler.scheduleRecovery(); syncMessage.value = errorLabel(it) },
                )
            } finally {
                busy.value = false
            }
        }
    }

    fun deleteVisit(id: Long) = viewModelScope.launch { repository.deleteVisit(id) }
    fun clearHistory() = viewModelScope.launch { repository.clearHistory() }

    fun track(id: Int, value: String) = viewModelScope.launch { repository.setTracking(id, value) }
    fun pin(id: Int, value: Boolean) = viewModelScope.launch { repository.setPinned(id, value) }
    fun hide(id: Int, value: Boolean) = viewModelScope.launch { repository.setHidden(id, value) }
    fun note(id: Int, value: String) = viewModelScope.launch { repository.setNote(id, value) }
    fun rate(id: Int, value: Int) = viewModelScope.launch { repository.setRating(id, value) }
    fun visit(character: CharacterEntity) = viewModelScope.launch { repository.visit(character) }
    fun createCollection(title: String, description: String = "", onResult: (Boolean) -> Unit = {}) = viewModelScope.launch {
        runCatching { repository.createCollection(title, description) }.also { if (it.exceptionOrNull() is CancellationException) throw it.exceptionOrNull()!! }
            .onSuccess { syncMessage.value = "Коллекция создана"; onResult(true) }
            .onFailure { syncMessage.value = "Название уже используется или не удалось создать коллекцию"; onResult(false) }
    }
    fun editCollection(id: Long, title: String, description: String, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        runCatching { repository.editCollection(id, title, description) }.also { if (it.exceptionOrNull() is CancellationException) throw it.exceptionOrNull()!! }
            .onSuccess { onResult(true) }
            .onFailure { onResult(false) }
    }
    fun deleteCollection(id: Long) = viewModelScope.launch { repository.deleteCollection(id) }
    fun addToCollection(collectionId: Long, characterId: Int) = viewModelScope.launch { repository.addToCollection(collectionId, characterId) }
    fun removeFromCollection(collectionId: Long, characterId: Int) = viewModelScope.launch { repository.removeFromCollection(collectionId, characterId) }
    fun setShowHidden(value: Boolean) = viewModelScope.launch { preferencesRepository.setShowHidden(value) }
    fun setPreloadPages(value: Int) = viewModelScope.launch {
        preferencesRepository.setPreloadPages(value)
        refreshMutex.withLock {
            val missing = repository.stalePages(value)
            if (missing.isNotEmpty()) {
                busy.value = true
                try {
                    val failed = mutableListOf<String>()
                    for (page in missing) repository.refresh(page).onFailure { failed += errorLabel(it) }
                    updatePageState()
                    syncMessage.value = if (failed.isEmpty()) "Предзагрузка завершена" else "Ошибки: ${failed.joinToString()}"
                    if (failed.isNotEmpty()) syncScheduler.scheduleRecovery()
                } finally { busy.value = false }
            }
        }
    }
    fun setDarkTheme(value: Boolean) = viewModelScope.launch { preferencesRepository.setDarkTheme(value) }
    fun collectionCharacters(id: Long) = repository.collectionCharacters(id)
}
