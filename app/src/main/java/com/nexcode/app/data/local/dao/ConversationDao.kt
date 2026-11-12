package com.nexcode.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nexcode.app.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getMessagesByProject(projectId: Long): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ConversationEntity): Long

    @Delete
    suspend fun deleteMessage(message: ConversationEntity)

    @Query("DELETE FROM conversations WHERE projectId = :projectId")
    suspend fun clearHistory(projectId: Long)
}
