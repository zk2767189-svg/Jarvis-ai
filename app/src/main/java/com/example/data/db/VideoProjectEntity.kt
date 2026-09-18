package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_projects")
data class VideoProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val storyIdea: String,
    val aspectRatio: String, // "9:16" or "16:9"
    val visualStyle: String,
    val characterContinuitySheet: String,
    val scenesJson: String,
    val durationSeconds: Int = 30,
    val status: String = "COMPLETED", // "IN_PROGRESS", "COMPLETED", "FAILED"
    val createdAt: Long = System.currentTimeMillis()
)
