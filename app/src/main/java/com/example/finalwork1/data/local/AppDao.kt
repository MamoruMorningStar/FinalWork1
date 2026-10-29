package com.example.finalwork1.data.local

import androidx.room.Dao
import androidx.room.Transaction
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM characters ORDER BY isPinned DESC, name COLLATE NOCASE")
    fun observeCharacters(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1")
    suspend fun getCharacter(id: Int): CharacterEntity?

    @Upsert
    suspend fun upsertCharacters(items: List<CharacterEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRemoteIfMissing(item: CharacterEntity): Long

    @Query("""UPDATE characters SET name=:name, status=:status, species=:species, gender=:gender,
        origin=:origin, location=:location, imageUrl=:imageUrl, episodeCount=:episodeCount,
        fetchedAt=:fetchedAt WHERE id=:id""")
    suspend fun updateRemoteFields(id: Int, name: String, status: String, species: String,
        gender: String, origin: String, location: String, imageUrl: String, episodeCount: Int, fetchedAt: Long)


    @Query("UPDATE characters SET tracking = :tracking WHERE id = :id")
    suspend fun setTracking(id: Int, tracking: String)

    @Query("UPDATE characters SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Int, pinned: Boolean)

    @Query("UPDATE characters SET isHidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: Int, hidden: Boolean)

    @Query("UPDATE characters SET note = :note WHERE id = :id")
    suspend fun setNote(id: Int, note: String)

    @Query("UPDATE characters SET rating = :rating WHERE id = :id")
    suspend fun setRating(id: Int, rating: Int)

    @Query("SELECT * FROM user_collections ORDER BY createdAt DESC")
    fun observeCollections(): Flow<List<UserCollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun createCollection(collection: UserCollectionEntity): Long

    @Query("DELETE FROM user_collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToCollection(member: CollectionMemberEntity): Long

    @Query("DELETE FROM collection_members WHERE collectionId = :collectionId AND characterId = :characterId")
    suspend fun removeFromCollection(collectionId: Long, characterId: Int)

    @Query("SELECT COUNT(*) FROM collection_members WHERE collectionId = :collectionId")
    fun observeCollectionCount(collectionId: Long): Flow<Int>

    @Query("SELECT c.* FROM characters c INNER JOIN collection_members m ON c.id = m.characterId WHERE m.collectionId = :collectionId ORDER BY c.name")
    fun observeCollectionCharacters(collectionId: Long): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM recent_history ORDER BY visitedAt DESC, id DESC LIMIT 100")
    fun observeHistory(): Flow<List<RecentVisitEntity>>

    @Insert
    suspend fun addVisit(visit: RecentVisitEntity)

    @Query("DELETE FROM recent_history WHERE id NOT IN (SELECT id FROM recent_history ORDER BY visitedAt DESC, id DESC LIMIT 100)")
    suspend fun trimHistory()

    @Query("SELECT DISTINCT characterId FROM recent_history")
    suspend fun visitedCharacterIds(): List<Int>

    @Upsert
    suspend fun upsertPage(page: CachedPageEntity)

    @Query("SELECT * FROM cached_pages")
    suspend fun cachedPages(): List<CachedPageEntity>

    @Query("SELECT COUNT(*) FROM characters")
    suspend fun characterCount(): Int

    @Query("SELECT MIN(fetchedAt) FROM characters")
    suspend fun latestFetchTime(): Long?

    @Query("SELECT COUNT(*) FROM characters WHERE fetchedAt < :cutoff")
    suspend fun staleCharacterCount(cutoff: Long): Int

    @Query("SELECT characterId FROM collection_members WHERE collectionId = :collectionId")
    suspend fun collectionMemberIds(collectionId: Long): List<Int>

    @Query("DELETE FROM recent_history WHERE characterId = :characterId")
    suspend fun removePreviousVisits(characterId: Int)

    @androidx.room.Transaction
    suspend fun recordVisit(visit: RecentVisitEntity) {
        removePreviousVisits(visit.characterId)
        addVisit(visit)
        trimHistory()
    }

    @Query("UPDATE user_collections SET title = :title, description = :description WHERE id = :id")
    suspend fun editCollection(id: Long, title: String, description: String)
}
