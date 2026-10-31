package com.example.finalwork1.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.finalwork1.data.di.Migrations
import com.example.finalwork1.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/** Real on-disk legacy tables, not a fresh in-memory database. */
@RunWith(AndroidJUnit4::class)
class MigrationChainTest {
    @Test fun version1To4PreservesUserData() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val filename = "legacy-v1-upgrade.db"
        context.deleteDatabase(filename)
        val path = context.getDatabasePath(filename)
        path.parentFile?.mkdirs()
        val old = SQLiteDatabase.openOrCreateDatabase(path, null)
        try {
            old.execSQL("CREATE TABLE characters (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, status TEXT NOT NULL, species TEXT NOT NULL, gender TEXT NOT NULL, origin TEXT NOT NULL, location TEXT NOT NULL, imageUrl TEXT NOT NULL, episodeCount INTEGER NOT NULL, fetchedAt INTEGER NOT NULL, tracking TEXT NOT NULL, isPinned INTEGER NOT NULL, isHidden INTEGER NOT NULL, note TEXT NOT NULL, rating INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE user_collections (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            old.execSQL("CREATE UNIQUE INDEX index_user_collections_title ON user_collections (title)")
            old.execSQL("CREATE TABLE collection_members (collectionId INTEGER NOT NULL, characterId INTEGER NOT NULL, addedAt INTEGER NOT NULL, PRIMARY KEY(collectionId, characterId), FOREIGN KEY(collectionId) REFERENCES user_collections(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(characterId) REFERENCES characters(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            old.execSQL("CREATE INDEX index_collection_members_characterId ON collection_members(characterId)")
            old.execSQL("CREATE TABLE recent_history (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, characterId INTEGER NOT NULL, characterName TEXT NOT NULL, visitedAt INTEGER NOT NULL)")
            old.execSQL("CREATE INDEX index_recent_history_visitedAt ON recent_history(visitedAt)")
            old.execSQL("INSERT INTO characters VALUES (1,'Rick','Alive','Human','Male','Earth','Earth','',4,100,'WATCHED',1,0,'User note',5)")
            old.execSQL("INSERT INTO user_collections (id,title,description,createdAt) VALUES (7,'My list','Kept',123)")
            old.execSQL("INSERT INTO collection_members VALUES (7,1,321)")
            old.execSQL("INSERT INTO recent_history (characterId,characterName,visitedAt) VALUES (1,'Rick',555)")
            old.version = 1
        } finally { old.close() }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, filename)
            .addMigrations(Migrations.V1_TO_V2, Migrations.V2_TO_V3, Migrations.V3_TO_V4)
            .build()
        try {
            val dao = db.dao()
            assertEquals("User note", dao.getCharacter(1)?.note)
            assertEquals(true, dao.getCharacter(1)?.isPinned)
            assertEquals("My list", dao.observeCollections().first().single().title)
            assertEquals("my list", dao.observeCollections().first().single().titleKey)
            assertEquals(listOf(1), dao.collectionMemberIds(7))
            assertEquals(1, dao.observeHistory().first().size)
            assertEquals(null, dao.totalPages())
            assertNotNull(dao.getCharacter(1))
        } finally { db.close(); context.deleteDatabase(filename) }
    }
}
