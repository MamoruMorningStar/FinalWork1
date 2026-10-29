package com.example.finalwork1.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.finalwork1.data.local.AppDao
import com.example.finalwork1.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "media_shelf.db")
            .addMigrations(object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `cached_pages` (`page` INTEGER NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`page`))")
                }
            }, object : Migration(2, 3) {
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
            }).build()

    @Provides
    fun provideDao(database: AppDatabase): AppDao = database.dao()
}
