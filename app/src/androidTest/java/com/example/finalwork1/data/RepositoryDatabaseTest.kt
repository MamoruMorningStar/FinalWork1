package com.example.finalwork1.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.finalwork1.data.local.AppDatabase
import com.example.finalwork1.data.local.CharacterEntity
import com.example.finalwork1.data.remote.ApiCharacter
import com.example.finalwork1.data.remote.ApiInfo
import com.example.finalwork1.data.remote.CharacterPage
import com.example.finalwork1.data.remote.RickMortyApi
import com.example.finalwork1.data.repo.CharacterRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryDatabaseTest {
    private lateinit var database: AppDatabase

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java
        ).build()
    }

    @After fun cleanup() = database.close()

    private fun entity(id: Int, name: String = "Character $id") = CharacterEntity(
        id = id, name = name, status = "Alive", species = "Human", gender = "Unknown",
        origin = "Earth", location = "Earth", imageUrl = "", episodeCount = 0,
        fetchedAt = System.currentTimeMillis(),
    )

    @Test fun refreshPersistsCharactersAndPreservesUserNotes() = runBlocking {
        val fakeApi = object : RickMortyApi {
            override suspend fun getCharacters(page: Int) = CharacterPage(
                ApiInfo(pages = 1), listOf(ApiCharacter(id = 1, name = "Rick Sanchez"))
            )
        }
        val repository = CharacterRepository(database.dao(), fakeApi)
        assertEquals(1, repository.refresh().getOrThrow())
        repository.setNote(1, "Сохранить заметку")
        assertTrue(repository.refresh().isSuccess)
        assertEquals("Сохранить заметку", repository.characters.first().single().note)
    }

    @Test fun userNoteChangedDuringNetworkRequestIsPreserved() = runBlocking {
        val requestStarted = CompletableDeferred<Unit>()
        val releaseResponse = CompletableDeferred<Unit>()
        val api = object : RickMortyApi {
            override suspend fun getCharacters(page: Int): CharacterPage {
                requestStarted.complete(Unit)
                releaseResponse.await()
                return CharacterPage(ApiInfo(pages = 1), listOf(ApiCharacter(id = 1, name = "Updated")))
            }
        }
        database.dao().upsertCharacters(listOf(entity(1)))
        val repository = CharacterRepository(database.dao(), api)
        val request = async { repository.refresh() }
        requestStarted.await()
        repository.setNote(1, "Changed during fetch")
        releaseResponse.complete(Unit)
        assertTrue(request.await().isSuccess)
        assertEquals("Changed during fetch", database.dao().getCharacter(1)?.note)
    }

    @Test fun repeatedVisitsDoNotPushOtherCharactersOutOfHistory() = runBlocking {
        val repo = CharacterRepository(database.dao(), object : RickMortyApi {
            override suspend fun getCharacters(page: Int): CharacterPage = error("unused")
        })
        val first = entity(1)
        val second = entity(2)
        repo.visit(second)
        repeat(100) { repo.visit(first) }
        val history = repo.history.first()
        assertEquals(2, history.size)
        assertEquals(1, history.first().characterId)
        assertEquals(2, history.last().characterId)
    }

    @Test fun oldestCachedRecordDeterminesFreshness() = runBlocking {
        val now = System.currentTimeMillis()
        database.dao().upsertCharacters(listOf(entity(1), entity(2)))
        database.dao().upsertPage(com.example.finalwork1.data.local.CachedPageEntity(1, now))
        database.dao().upsertPage(com.example.finalwork1.data.local.CachedPageEntity(2, now - 72L * 60 * 60 * 1000))
        val repo = CharacterRepository(database.dao(), object : RickMortyApi {
            override suspend fun getCharacters(page: Int): CharacterPage = error("unused")
        })
        assertTrue(repo.isCacheStale(pages = 2))
    }
}
