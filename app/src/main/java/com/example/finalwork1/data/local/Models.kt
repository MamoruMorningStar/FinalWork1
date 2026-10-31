package com.example.finalwork1.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val gender: String,
    val origin: String,
    val location: String,
    val imageUrl: String,
    val episodeCount: Int,
    val fetchedAt: Long,
    val tracking: String = "NONE",
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val note: String = "",
    val rating: Int = 0,
)

@Entity(tableName = "user_collections", indices = [Index(value = ["title"], unique = true), Index(value = ["titleKey"], unique = true)])
data class UserCollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    @ColumnInfo(defaultValue = "''") val titleKey: String = title.trim().lowercase(java.util.Locale.ROOT),
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "collection_members",
    primaryKeys = ["collectionId", "characterId"],
    foreignKeys = [
        ForeignKey(entity = UserCollectionEntity::class, parentColumns = ["id"], childColumns = ["collectionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CharacterEntity::class, parentColumns = ["id"], childColumns = ["characterId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["characterId"])]
)
data class CollectionMemberEntity(val collectionId: Long, val characterId: Int, val addedAt: Long = System.currentTimeMillis())

@Entity(tableName = "recent_history", indices = [Index(value = ["visitedAt"])])
data class RecentVisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Int,
    val characterName: String,
    val visitedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_pages")
data class CachedPageEntity(@PrimaryKey val page: Int, val fetchedAt: Long)

@Entity(tableName = "catalog_metadata")
data class CatalogMetadataEntity(@PrimaryKey val id: Int = 1, val totalPages: Int)


data class CharacterMembershipCount(val characterId: Int, val count: Int)
