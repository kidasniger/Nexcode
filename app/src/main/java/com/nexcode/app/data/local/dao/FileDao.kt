package com.nexcode.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nexcode.app.data.local.entity.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE projectId = :projectId ORDER BY isDirectory DESC, name ASC")
    fun getFilesByProject(projectId: Long): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE projectId = :projectId AND relativePath = :path LIMIT 1")
    suspend fun getFileByPath(projectId: Long, path: String): FileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity): Long

    @Update
    suspend fun updateFile(file: FileEntity)

    @Delete
    suspend fun deleteFile(file: FileEntity)

    @Query("DELETE FROM files WHERE projectId = :projectId")
    suspend fun deleteAllFilesForProject(projectId: Long)
}
