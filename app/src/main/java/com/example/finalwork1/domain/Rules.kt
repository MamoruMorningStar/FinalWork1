package com.example.finalwork1.domain

import com.example.finalwork1.data.local.CharacterEntity

enum class TrackingStatus { NONE, TRACKING, COMPLETED, PLANNED }

data class CharacterItem(
    val id: Int, val name: String, val status: String, val species: String, val gender: String,
    val origin: String, val location: String, val imageUrl: String, val episodeCount: Int, val fetchedAt: Long,
)

data class CatalogRow(
    val character: CharacterItem,
    val tracking: TrackingStatus = TrackingStatus.NONE,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val hasNote: Boolean = false,
    val inCollections: Int = 0,
)

data class CatalogFilter(
    val query: String = "",
    val status: String = "",
    val species: String = "",
    val showHidden: Boolean = false,
    val onlyHidden: Boolean = false,
    val onlyTracking: Boolean = false,
    val personalStatus: String = "",
)

object CatalogFilterLogic {
    fun matches(row: CatalogRow, filter: CatalogFilter): Boolean {
        if (filter.onlyHidden) {
            if (!row.isHidden) return false
        } else if (row.isHidden && !filter.showHidden) return false
        if (filter.onlyTracking && row.tracking == TrackingStatus.NONE && !row.isPinned && !row.hasNote && row.inCollections == 0) return false
        if (filter.personalStatus.isNotBlank() && row.tracking.name != filter.personalStatus) return false
        val query = filter.query.trim()
        if (query.isNotEmpty() && !row.character.name.contains(query, true) &&
            !row.character.species.contains(query, true) && !row.character.origin.contains(query, true)) return false
        if (filter.status.isNotBlank() && !row.character.status.equals(filter.status, true)) return false
        if (filter.species.isNotBlank() && !row.character.species.equals(filter.species, true)) return false
        return true
    }
    fun sort(rows: List<CatalogRow>): List<CatalogRow> = rows.sortedWith(
        compareByDescending<CatalogRow> { it.isPinned }.thenBy { it.character.name.lowercase() }
    )
}

object CollectionRules {
    fun canAdd(existingIds: Set<Int>, characterId: Int): Boolean = characterId > 0 && characterId !in existingIds
    fun normalizeTitle(title: String): String = title.trim().take(80)
}

object SyncPlanner {
    fun nextMissingPage(cachedPages: Set<Int>, minimumPages: Int): Int {
        val upper = maxOf(cachedPages.maxOrNull() ?: 0, minimumPages.coerceAtLeast(1))
        return (1..upper).firstOrNull { it !in cachedPages } ?: upper + 1
    }

    fun pagesToPreload(cachedPages: Set<Int>, totalPages: Int, preloadCount: Int): List<Int> {
        if (totalPages <= 0 || preloadCount <= 0) return emptyList()
        return (1..totalPages).filterNot { it in cachedPages }.take(preloadCount)
    }
}

fun CharacterEntity.toCatalogRow(collectionCount: Int = 0) = CatalogRow(
    character = CharacterItem(id, name, status, species, gender, origin, location, imageUrl, episodeCount, fetchedAt),
    tracking = runCatching { TrackingStatus.valueOf(tracking) }.getOrDefault(TrackingStatus.NONE),
    isPinned = isPinned, isHidden = isHidden, hasNote = note.isNotBlank(), inCollections = collectionCount,
)


object CachePolicy {
    fun isStale(lastFetchedAt: Long?, now: Long, ttlMillis: Long): Boolean {
        if (lastFetchedAt == null || lastFetchedAt <= 0L) return true
        if (ttlMillis <= 0L) return true
        return now - lastFetchedAt >= ttlMillis
    }
}
