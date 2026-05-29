package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_projects")
data class SongProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val filePath: String,
    val bpm: Int = 120,
    val keySignature: String = "C Major",
    val bassLevel: Float = 0.8f,
    val vocalsLevel: Float = 0.8f,
    val drumsLevel: Float = 0.8f,
    val melodyLevel: Float = 0.8f,
    val aiBpmAdvice: String = "",
    val aiStructureBreakdown: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
