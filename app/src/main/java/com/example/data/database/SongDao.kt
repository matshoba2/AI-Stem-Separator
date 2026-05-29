package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM song_projects ORDER BY createdAt DESC")
    fun getAllSongs(): Flow<List<SongProject>>

    @Query("SELECT * FROM song_projects WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: Int): SongProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongProject): Long

    @Update
    suspend fun updateSong(song: SongProject)

    @Delete
    suspend fun deleteSong(song: SongProject)

    @Query("DELETE FROM song_projects")
    suspend fun clearAll()
}
