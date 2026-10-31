package com.example.finalwork1.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.finalwork1.data.di.Migrations
import com.example.finalwork1.data.local.AppDatabase
import com.example.finalwork1.data.local.CharacterEntity
import com.example.finalwork1.data.local.UserCollectionEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationDataTest {
    @Test fun upgradeFromVersion3RetainsLocalNoteAndCollections() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-preservation-test.db"
        context.deleteDatabase(name)
        val original = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        original.dao().upsertCharacters(listOf(CharacterEntity(1, "Rick", "Alive", "Human", "Male",
            "Earth", "Earth", "", 1, 100, note = "Keep this note", isPinned = true)))
        original.dao().createCollection(UserCollectionEntity(title = "Keep collection"))
        original.close()
        // Recreate the v3 schema from the current v4 file, dropping only the new table.
        val raw = context.openOrCreateDatabase(name, 0, null)
        raw.execSQL("DROP TABLE catalog_metadata")
        raw.version = 3
        raw.close()
        val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(Migrations.V1_TO_V2, Migrations.V2_TO_V3, Migrations.V3_TO_V4).build()
        try {
            assertEquals("Keep this note", upgraded.dao().getCharacter(1)?.note)
            assertEquals(true, upgraded.dao().getCharacter(1)?.isPinned)
            assertEquals("Keep collection", upgraded.dao().observeCollections().first().single().title)
            assertEquals(null, upgraded.dao().totalPages())
        } finally { upgraded.close(); context.deleteDatabase(name) }
    }
}
