package com.example.finalwork1.data.di

import android.content.Context
import androidx.room.Room
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
            .addMigrations(Migrations.V1_TO_V2, Migrations.V2_TO_V3, Migrations.V3_TO_V4).build()

    @Provides
    fun provideDao(database: AppDatabase): AppDao = database.dao()
}
