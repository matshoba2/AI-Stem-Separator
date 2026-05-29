package com.example.data.repository

import com.example.data.database.SongDao
import com.example.data.database.SongProject
import kotlinx.coroutines.flow.Flow

class SongRepository(private val songDao: SongDao) {
    val allSongs: Flow<List<SongProject>> = songDao.getAllSongs()

    suspend fun getSongById(id: Int): SongProject? {
        return songDao.getSongById(id)
    }

    suspend fun insert(song: SongProject): Long {
        return songDao.insertSong(song)
    }

    suspend fun update(song: SongProject) {
        songDao.updateSong(song)
    }

    suspend fun delete(song: SongProject) {
        songDao.deleteSong(song)
    }

    suspend fun clearAll() {
        songDao.clearAll()
    }
}
