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
            }).build()

    @Provides
    fun provideDao(database: AppDatabase): AppDao = database.dao()
}
