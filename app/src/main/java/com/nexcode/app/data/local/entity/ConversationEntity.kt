package com.nexcode.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val role: String, // "user" ou "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachedFiles: String = "", // liste de chemins relatifs separés par des virgules
    val modelName: String = "gemini-1.5-pro",
    val hasDiffReview: Boolean = false
)
