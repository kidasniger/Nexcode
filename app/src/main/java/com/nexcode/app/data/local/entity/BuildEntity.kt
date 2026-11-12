package com.nexcode.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "builds",
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
data class BuildEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val buildType: String = "debug", // "debug" ou "release"
    val status: String, // "SUCCESS", "FAILED", "RUNNING"
    val startTime: Long = System.currentTimeMillis(),
    val durationMs: Long = 0,
    val apkPath: String? = null,
    val apkSize: Long = 0,
    val logOutput: String? = null,
    val errorCount: Int = 0
)
