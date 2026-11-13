package com.nexcode.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val packageName: String,
    val path: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val minSdk: Int = 26,
    val targetSdk: Int = 35,
    val gitBranch: String = "main",
    val gitRepo: String = "",
    val language: String = "Kotlin",
    val uiFramework: String = "Compose",
    val onGitHub: Boolean = false,
    val isFavorite: Boolean = false
)
