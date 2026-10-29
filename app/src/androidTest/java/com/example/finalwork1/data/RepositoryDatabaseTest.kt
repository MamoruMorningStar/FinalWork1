package com.example.finalwork1.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.finalwork1.data.local.AppDatabase
import com.example.finalwork1.data.remote.ApiCharacter
import com.example.finalwork1.data.remote.ApiInfo
import com.example.finalwork1.data.remote.CharacterPage
import com.example.finalwork1.data.remote.RickMortyApi
import com.example.finalwork1.data.repo.CharacterRepository
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

    @Test fun refreshPersistsCharactersAndPreservesUserNotes() = runBlocking {
        val fakeApi = object : RickMortyApi {
            override suspend fun getCharacters(page: Int) = CharacterPage(
                ApiInfo(pages = 1),
                listOf(ApiCharacter(id = 1, name = "Rick Sanchez"))
            )
        }
        val repository = CharacterRepository(database.dao(), fakeApi)
        assertEquals(1, repository.refresh().getOrThrow())
        assertEquals("Rick Sanchez", repository.characters.first().single().name)

        repository.setNote(1, "Сохранить заметку")
        assertTrue(repository.refresh().isSuccess)
        assertEquals("Сохранить заметку", repository.characters.first().single().note)
    }
}
