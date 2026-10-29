package com.example.finalwork1.data.repo

import com.example.finalwork1.data.local.AppDao
import com.example.finalwork1.data.local.CharacterEntity
import com.example.finalwork1.data.local.RecentVisitEntity
import com.example.finalwork1.data.remote.RickMortyApi
import com.example.finalwork1.domain.CachePolicy
import kotlinx.coroutines.flow.Flow

class CharacterRepository @javax.inject.Inject constructor(private val dao: AppDao, private val api: RickMortyApi) {
    val characters: Flow<List<CharacterEntity>> = dao.observeCharacters()
    val collections = dao.observeCollections()
    val history = dao.observeHistory()

    suspend fun isCacheStale(ttlMillis: Long = 24L * 60 * 60 * 1000): Boolean {
        return CachePolicy.isStale(dao.latestFetchTime(), System.currentTimeMillis(), ttlMillis)
    }

    suspend fun refresh(page: Int = 1): Result<Int> = runCatching {
        val response = api.getCharacters(page)
        val now = System.currentTimeMillis()
        val mapped = response.results.map { remote ->
            val local = dao.getCharacter(remote.id)
            CharacterEntity(
                id = remote.id,
                name = remote.name,
                status = remote.status,
                species = remote.species,
                gender = remote.gender,
                origin = remote.origin.name,
                location = remote.location.name,
                imageUrl = remote.image,
                episodeCount = remote.episode.size,
                fetchedAt = now,
                tracking = local?.tracking ?: "NONE",
                isPinned = local?.isPinned ?: false,
                isHidden = local?.isHidden ?: false,
                note = local?.note ?: "",
                rating = local?.rating ?: 0,
            )
        }
        dao.upsertCharacters(mapped)
        mapped.size
    }

    suspend fun visit(character: CharacterEntity) {
        dao.addVisit(RecentVisitEntity(characterId = character.id, characterName = character.name))
        dao.trimHistory()
    }

    suspend fun setTracking(id: Int, tracking: String) = dao.setTracking(id, tracking)
    suspend fun setPinned(id: Int, value: Boolean) = dao.setPinned(id, value)
    suspend fun setHidden(id: Int, value: Boolean) = dao.setHidden(id, value)
    suspend fun setNote(id: Int, note: String) = dao.setNote(id, note.take(2000))
    suspend fun setRating(id: Int, rating: Int) = dao.setRating(id, rating.coerceIn(0, 5))
    suspend fun createCollection(title: String, description: String = ""): Long =
        dao.createCollection(com.example.finalwork1.data.local.UserCollectionEntity(
            title = title.trim().take(80).ifBlank { "Без названия" },
            description = description.trim().take(300),
        ))
    suspend fun deleteCollection(id: Long) = dao.deleteCollection(id)
    suspend fun addToCollection(collectionId: Long, characterId: Int) =
        dao.addToCollection(com.example.finalwork1.data.local.CollectionMemberEntity(collectionId, characterId))
    suspend fun removeFromCollection(collectionId: Long, characterId: Int) =
        dao.removeFromCollection(collectionId, characterId)
    fun collectionCharacters(id: Long): Flow<List<CharacterEntity>> = dao.observeCollectionCharacters(id)
}
