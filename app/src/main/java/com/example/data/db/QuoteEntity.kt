package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quotes_of_the_day")
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quoteText: String,
    val author: String,
    val category: String, // "Inspirational", "Thought-Provoking", or "Humorous"
    val dateFormatted: String, // YYYY-MM-DD
    val source: String = "Global Wisdom Stream",
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
