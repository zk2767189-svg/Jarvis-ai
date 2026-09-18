package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jarvis_tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "General", // "Project", "Idea", "Automation", "General", "AI Video", "YouTube"
    val priority: String = "Normal", // "High", "Normal", "Low"
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
