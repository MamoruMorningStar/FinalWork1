package com.example.finalwork1.data.repo

import com.example.finalwork1.data.local.AppDao
import com.example.finalwork1.data.local.CachedPageEntity
import com.example.finalwork1.data.local.CharacterEntity
import com.example.finalwork1.data.local.RecentVisitEntity
import com.example.finalwork1.data.remote.RickMortyApi
import com.example.finalwork1.domain.CachePolicy
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import com.example.finalwork1.domain.CollectionRules
import com.example.finalwork1.data.local.UserCollectionEntity
import com.example.finalwork1.data.local.CollectionMemberEntity

class CharacterRepository @Inject constructor(private val dao: AppDao, private val api: RickMortyApi) {
    val characters: Flow<List<CharacterEntity>> = dao.observeCharacters()
    val collections = dao.observeCollections()
    val history = dao.observeHistory()

    suspend fun isCacheStale(ttlMillis: Long = 24L * 60 * 60 * 1000, pages: Int = 1): Boolean =
        stalePages(pages, ttlMillis).isNotEmpty()

    suspend fun highestCachedPage(): Int = dao.cachedPages().maxOfOrNull { it.page } ?: 0

    suspend fun stalePages(pages: Int, ttlMillis: Long = 24L * 60 * 60 * 1000): List<Int> {
        val fetched = dao.cachedPages().associate { it.page to it.fetchedAt }
        val now = System.currentTimeMillis()
        return (1..pages.coerceAtLeast(1)).filter { page ->
            CachePolicy.isStale(fetched[page], now, ttlMillis)
        }
    }

    suspend fun refresh(page: Int = 1): Result<Int> = runCatching {
        val response = api.getCharacters(page)
        val now = System.currentTimeMillis()
        response.results.forEach { remote ->
            dao.insertRemoteIfMissing(CharacterEntity(
                id = remote.id, name = remote.name, status = remote.status,
                species = remote.species, gender = remote.gender,
                origin = remote.origin.name, location = remote.location.name,
                imageUrl = remote.image, episodeCount = remote.episode.size,
                fetchedAt = now,
            ))
            dao.updateRemoteFields(remote.id, remote.name, remote.status, remote.species,
                remote.gender, remote.origin.name, remote.location.name, remote.image,
                remote.episode.size, now)
        }
        dao.upsertPage(CachedPageEntity(page, now))
        response.results.size
    }

    suspend fun visit(character: CharacterEntity) {
        dao.recordVisit(RecentVisitEntity(characterId = character.id, characterName = character.name))
    }

    suspend fun setTracking(id: Int, tracking: String) = dao.setTracking(id, tracking)
    suspend fun setPinned(id: Int, value: Boolean) = dao.setPinned(id, value)
    suspend fun setHidden(id: Int, value: Boolean) = dao.setHidden(id, value)
    suspend fun setNote(id: Int, note: String) = dao.setNote(id, note.take(2000))
    suspend fun setRating(id: Int, rating: Int) = dao.setRating(id, rating.coerceIn(0, 5))
    suspend fun createCollection(title: String, description: String = ""): Long =
        dao.createCollection(UserCollectionEntity(
            title = CollectionRules.normalizeTitle(title).ifBlank { "Без названия" },
            description = description.trim().take(300),
        ))
    suspend fun editCollection(id: Long, title: String, description: String) {
        val normalized = CollectionRules.normalizeTitle(title)
        require(normalized.isNotBlank()) { "Название не может быть пустым" }
        dao.editCollection(id, normalized, description.trim().take(300))
    }
    suspend fun deleteCollection(id: Long) = dao.deleteCollection(id)
    suspend fun addToCollection(collectionId: Long, characterId: Int) {
        val existing = dao.collectionMemberIds(collectionId).toSet()
        if (CollectionRules.canAdd(existing, characterId)) dao.addToCollection(CollectionMemberEntity(collectionId, characterId))
    }
    suspend fun removeFromCollection(collectionId: Long, characterId: Int) =
        dao.removeFromCollection(collectionId, characterId)
    fun collectionCharacters(id: Long): Flow<List<CharacterEntity>> = dao.observeCollectionCharacters(id)
}
