package com.example.finalwork1.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

@Serializable
data class ApiInfo(val pages: Int = 1)

@Serializable
data class ApiCharacter(
    val id: Int,
    val name: String,
    val status: String = "Unknown",
    val species: String = "Unknown",
    val gender: String = "Unknown",
    val origin: ApiNamed = ApiNamed(),
    val location: ApiNamed = ApiNamed(),
    val image: String = "",
    val episode: List<String> = emptyList(),
)

@Serializable
data class ApiNamed(val name: String = "Unknown")

@Serializable
data class CharacterPage(val info: ApiInfo, val results: List<ApiCharacter>)

interface RickMortyApi {
    @GET("character")
    suspend fun getCharacters(@Query("page") page: Int = 1): CharacterPage

    companion object {
        fun create(): RickMortyApi {
            val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
            return Retrofit.Builder()
                .baseUrl("https://rickandmortyapi.com/api/")
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(RickMortyApi::class.java)
        }
    }
}
