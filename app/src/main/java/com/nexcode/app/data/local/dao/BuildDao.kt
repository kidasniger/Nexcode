package com.nexcode.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nexcode.app.data.local.entity.BuildEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BuildDao {
    @Query("SELECT * FROM builds WHERE projectId = :projectId ORDER BY startTime DESC")
    fun getBuildsByProject(projectId: Long): Flow<List<BuildEntity>>

    @Query("SELECT * FROM builds WHERE id = :id LIMIT 1")
    suspend fun getBuildById(id: Long): BuildEntity?

    @Query("SELECT * FROM builds WHERE projectId = :projectId ORDER BY startTime DESC LIMIT 1")
    fun getLatestBuild(projectId: Long): Flow<BuildEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuild(build: BuildEntity): Long

    @Update
    suspend fun updateBuild(build: BuildEntity)

    @Delete
    suspend fun deleteBuild(build: BuildEntity)

    @Query("DELETE FROM builds WHERE projectId = :projectId")
    suspend fun clearBuildsForProject(projectId: Long)
}
