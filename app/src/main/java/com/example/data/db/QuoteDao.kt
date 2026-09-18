package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes_of_the_day WHERE dateFormatted = :date LIMIT 1")
    suspend fun getQuoteForDate(date: String): QuoteEntity?

    @Query("SELECT * FROM quotes_of_the_day ORDER BY id DESC LIMIT 1")
    suspend fun getLatestQuote(): QuoteEntity?

    @Query("SELECT * FROM quotes_of_the_day ORDER BY id DESC LIMIT 1")
    fun getLatestQuoteFlow(): Flow<QuoteEntity?>

    @Query("SELECT * FROM quotes_of_the_day ORDER BY id DESC")
    fun getAllQuotes(): Flow<List<QuoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: QuoteEntity): Long

    @Update
    suspend fun updateQuote(quote: QuoteEntity)

    @Query("UPDATE quotes_of_the_day SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)
}
