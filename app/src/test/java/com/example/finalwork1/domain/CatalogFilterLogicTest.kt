package com.example.finalwork1.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogFilterLogicTest {
    private val row = CatalogRow(
        character = CharacterItem(
            id = 1,
            name = "Rick Sanchez",
            status = "Alive",
            species = "Human",
            gender = "Male",
            origin = "Earth",
            location = "Citadel",
            imageUrl = "",
            episodeCount = 51,
            fetchedAt = 1,
        ),
        tracking = TrackingStatus.TRACKING,
        isPinned = true,
        isHidden = true,
        hasNote = true,
        inCollections = 2,
    )

    @Test
    fun hidesHiddenByDefault() {
        assertFalse(CatalogFilterLogic.matches(row, CatalogFilter()))
        assertTrue(CatalogFilterLogic.matches(row, CatalogFilter(showHidden = true)))
    }

    @Test
    fun queryAndStatus() {
        val visible = row.copy(isHidden = false)
        assertTrue(CatalogFilterLogic.matches(visible, CatalogFilter(query = "rick", status = "Alive")))
        assertFalse(CatalogFilterLogic.matches(visible, CatalogFilter(query = "morty")))
        assertFalse(CatalogFilterLogic.matches(visible, CatalogFilter(status = "Dead")))
    }

    @Test
    fun pinnedFirst() {
        val other = row.copy(isPinned = false, isHidden = false, character = row.character.copy(id = 2, name = "Birdperson"))
        val pinned = row.copy(isHidden = false)
        val sorted = CatalogFilterLogic.sort(listOf(other, pinned))
        assertEquals(1, sorted.first().character.id)
    }

    @Test
    fun speciesFilter() {
        val human = row.copy(isHidden = false)
        val alien = human.copy(
            isHidden = false,
            character = human.character.copy(id = 3, name = "Zeep", species = "Alien"),
        )
        val rows = listOf(human, alien)
        val onlyHuman = rows.filter { CatalogFilterLogic.matches(it, CatalogFilter(species = "Human")) }
        assertEquals(1, onlyHuman.size)
        assertEquals("Rick Sanchez", onlyHuman.first().character.name)
        val onlyAlien = rows.filter { CatalogFilterLogic.matches(it, CatalogFilter(species = "alien")) }
        assertEquals(1, onlyAlien.size)
        assertEquals("Zeep", onlyAlien.first().character.name)
    }

    @Test
    fun onlyTrackingExcludesNone() {
        val tracking = row.copy(isHidden = false, tracking = TrackingStatus.TRACKING)
        val none = row.copy(
            isHidden = false,
            tracking = TrackingStatus.NONE,
            character = row.character.copy(id = 5),
        )
        val rows = listOf(tracking, none)
        val filtered = rows.filter { CatalogFilterLogic.matches(it, CatalogFilter(onlyTracking = true)) }
        assertEquals(1, filtered.size)
        assertEquals(TrackingStatus.TRACKING, filtered.first().tracking)
    }

    @Test
    fun emptyLocalShowsEmptyFlag() {
        val mapped = CatalogFilterLogic.sort(emptyList())
        assertTrue(mapped.isEmpty())
    }
}
