package com.example.finalwork1.data.di

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Locale

object Migrations {
val V1_TO_V2 = object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `cached_pages` (`page` INTEGER NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`page`))")
                }
            }
    val V2_TO_V3 = object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `user_collections` ADD COLUMN `titleKey` TEXT NOT NULL DEFAULT ''")
                    val existing = mutableSetOf<String>()
                    db.query("SELECT id, title FROM user_collections ORDER BY id").use { cursor ->
                        val idIndex = cursor.getColumnIndexOrThrow("id")
                        val titleIndex = cursor.getColumnIndexOrThrow("title")
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idIndex)
                            val title = cursor.getString(titleIndex)
                            val base = title.trim().lowercase(java.util.Locale.ROOT)
                            var key = base
                            if (!existing.add(key)) {
                                key = "$base#legacy$id"
                                existing.add(key)
                            }
                            db.execSQL("UPDATE user_collections SET titleKey = ? WHERE id = ?", arrayOf(key, id))
                        }
                    }
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_user_collections_titleKey` ON `user_collections` (`titleKey`)")
                }
            }
    val V3_TO_V4 = object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `catalog_metadata` (`id` INTEGER NOT NULL, `totalPages` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                }
            }
}
