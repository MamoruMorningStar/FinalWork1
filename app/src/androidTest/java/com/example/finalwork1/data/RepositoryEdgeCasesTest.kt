package com.example.finalwork1.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.finalwork1.data.local.AppDatabase
import com.example.finalwork1.data.local.CachedPageEntity
import com.example.finalwork1.data.local.CharacterEntity
import com.example.finalwork1.data.remote.ApiCharacter
import com.example.finalwork1.data.remote.ApiInfo
import com.example.finalwork1.data.remote.CharacterPage
import com.example.finalwork1.data.remote.RickMortyApi
import com.example.finalwork1.data.repo.CharacterRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryEdgeCasesTest {
    private fun character(id: Int) = CharacterEntity(id, "Local", "Alive", "Human", "Male", "Earth", "Earth", "", 0, 100)

    @Test fun remoteUpdateDoesNotOverwriteConcurrentLocalFields() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            val dao = db.dao()
            dao.upsertCharacters(listOf(character(1)))
            val staleSnapshot = dao.getCharacter(1)!!
            dao.setNote(1, "edited after snapshot")
            dao.setPinned(1, true)
            dao.setRating(1, 5)
            dao.setTracking(1, "WATCHED")
            dao.storeRemotePage(listOf(staleSnapshot.copy(name = "Server", note = "stale", isPinned = false, rating = 0, tracking = "NONE")), 1, 200, 2)
            val actual = dao.getCharacter(1)!!
            assertEquals("Server", actual.name)
            assertEquals("edited after snapshot", actual.note)
            assertTrue(actual.isPinned)
            assertEquals(5, actual.rating)
            assertEquals("WATCHED", actual.tracking)
        } finally { db.close() }
    }

    @Test fun pageFreshnessIsIndependentAndDoesNotExceedApiLimit() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            val now = System.currentTimeMillis()
            val dao = db.dao()
            dao.upsertPage(CachedPageEntity(1, now))
            dao.upsertPage(CachedPageEntity(2, now - 100_000L))
            dao.upsertPage(CachedPageEntity(3, now))
            dao.upsertMetadata(com.example.finalwork1.data.local.CatalogMetadataEntity(totalPages = 3))
            val repo = CharacterRepository(dao, object : RickMortyApi {
                override suspend fun getCharacters(page: Int): CharacterPage = error("no network")
            })
            assertEquals(listOf(2), repo.stalePages(10, ttlMillis = 50_000L))
        } finally { db.close() }
    }

    @Test fun invalidMetadataDoesNotMarkPageAsCached() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            val repo = CharacterRepository(db.dao(), object : RickMortyApi {
                override suspend fun getCharacters(page: Int) = CharacterPage(ApiInfo(1), listOf(ApiCharacter(1, "Rick")))
            })
            assertTrue(repo.refresh(2).isFailure)
            assertEquals(emptySet<Int>(), repo.cachedPageNumbers())
        } finally { db.close() }
    }
}
