package com.example.finalwork1.data.di

import com.example.finalwork1.data.remote.RickMortyApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://rickandmortyapi.com/api/")
        .addConverterFactory(
            Json { ignoreUnknownKeys = true; coerceInputValues = true }
                .asConverterFactory("application/json".toMediaType())
        )
        .build()

    @Provides @Singleton
    fun provideApi(retrofit: Retrofit): RickMortyApi = retrofit.create(RickMortyApi::class.java)
}
