package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(val text: String)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(val contents: List<Content>)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(val candidates: List<Candidate>?)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun analyzeSong(songTitle: String, artist: String, genre: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "API_KEY_MISSING"
        }

        val prompt = """
            You are "Acoustics AI", an elite mastering engineer and expert in music-source separation (stem separation). 
            Analyze the following song and provide a flawless technical plan to separate its stems into 4 channels: Vocals, Drums, Bass, and Melody.
            
            Song Title: "$songTitle"
            Artist: "$artist"
            Genre/Style: "$genre"
            
            Provide your response exactly in these 4 sections, clearly formatted:
            
            1. KEY & BPM DESIGNATIONS
            State the estimated master key and standard BPM of this composition with extreme authority.
            
            2. STEMS SIGNAL BREAKDOWN
            Give a brief visual 4-tier diagram mapping out estimated decibel levels and audio presence (high, mid, low) for:
            - Vocals: [frequency range and presence description]
            - Drums: [frequency range and transient description]
            - Bass: [frequency range and sub-low presence]
            - Melody: [harmonic keys, synthesizers, and instruments]
            
            3. ISOLATION CROSSOVER RECOMMANDATION
            Provide precise cutoff frequencies in Hertz (Hz) to minimize acoustic bleeding and artifacts for our real-time DSP Equalizer bands:
            - Low-Pass (Bass cutoff): e.g. 150 Hz
            - Mid-Bandpass (Vocal focus): e.g. 350 Hz to 2400 Hz
            - High-Pass (Drums snap and sparkle): e.g. 3000 Hz
            Explain why this specific setup guarantees a flawless separation without mistakes for "$songTitle".
            
            4. TIME-ALIGNMENT BREAKDOWN
            Give a breakdown of sections (Intro, Verse, Chorus, Outro) detailing exactly which stem channel dominates each area.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "Unable to generate analysis. Please try again."
        } catch (e: Exception) {
            "Error: ${e.localizedMessage ?: e.message ?: "Unknown issue connecting to Gemini"}"
        }
    }
}
