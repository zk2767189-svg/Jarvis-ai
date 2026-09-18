package com.example.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class QuoteResult(
    val quote: String,
    val author: String,
    val category: String, // "Inspirational", "Thought-Provoking", "Humorous"
    val source: String
)

class QuoteApiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "QuoteApiClient"

        // Rich curated daily catalog of inspirational, thought-provoking, and humorous quotes
        val CURATED_DAILY_QUOTES = listOf(
            QuoteResult(
                quote = "The only way to do great work is to love what you do. If you haven't found it yet, keep looking. Don't settle.",
                author = "Steve Jobs",
                category = "Inspirational",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "Two things are infinite: the universe and human stupidity; and I'm not sure about the universe.",
                author = "Albert Einstein",
                category = "Humorous",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "We suffer more often in imagination than in reality.",
                author = "Seneca",
                category = "Thought-Provoking",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "The future belongs to those who believe in the beauty of their dreams.",
                author = "Eleanor Roosevelt",
                category = "Inspirational",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "I have not failed. I've just found 10,000 ways that won't work.",
                author = "Thomas A. Edison",
                category = "Inspirational",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "I can resist everything except temptation.",
                author = "Oscar Wilde",
                category = "Humorous",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "You could leave life right now. Let that determine what you do and say and think.",
                author = "Marcus Aurelius",
                category = "Thought-Provoking",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "Beyond our ideas of right-doing and wrong-doing, there is a field. I'll meet you there.",
                author = "Jalal al-Din Rumi",
                category = "Thought-Provoking",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "The secret of getting ahead is getting started.",
                author = "Mark Twain",
                category = "Inspirational",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "A day without sunshine is like, you know, night.",
                author = "Steve Martin",
                category = "Humorous",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "Khudi ko kar buland itna ke har taqdeer se pehle, Khuda bande se khud pooche bata teri raza kya hai.",
                author = "Allama Muhammad Iqbal",
                category = "Inspirational",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "Simplicity is the ultimate sophistication.",
                author = "Leonardo da Vinci",
                category = "Thought-Provoking",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "If you tell the truth, you don't have to remember anything.",
                author = "Mark Twain",
                category = "Humorous",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "The unexamined life is not worth living.",
                author = "Socrates",
                category = "Thought-Provoking",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "Act as if what you do makes a difference. It does.",
                author = "William James",
                category = "Inspirational",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "Behind every great man is a woman rolling her eyes.",
                author = "Jim Carrey",
                category = "Humorous",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "Do not go where the path may lead, go instead where there is no path and leave a trail.",
                author = "Ralph Waldo Emerson",
                category = "Inspirational",
                source = "JARVIS Archive"
            ),
            QuoteResult(
                quote = "In the middle of difficulty lies opportunity.",
                author = "Albert Einstein",
                category = "Thought-Provoking",
                source = "JARVIS Archive"
            )
        )
    }

    /**
     * Attempts to fetch Quote of the Day from public REST APIs (ZenQuotes or DummyJSON),
     * and seamlessly falls back to the curated daily catalog if offline or network error.
     */
    suspend fun fetchDailyQuote(): QuoteResult = withContext(Dispatchers.IO) {
        // Attempt 1: ZenQuotes Daily API
        try {
            val request = Request.Builder()
                .url("https://zenquotes.io/api/today")
                .header("User-Agent", "JARVIS-Assistant/1.0")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                if (body.startsWith("[")) {
                    val jsonArray = JSONArray(body)
                    if (jsonArray.length() > 0) {
                        val obj = jsonArray.getJSONObject(0)
                        val quoteText = obj.optString("q").trim()
                        val author = obj.optString("a", "Unknown").trim()
                        if (quoteText.isNotBlank()) {
                            val category = classifyCategory(quoteText)
                            return@withContext QuoteResult(
                                quote = quoteText,
                                author = author.ifBlank { "Wise Mind" },
                                category = category,
                                source = "ZenQuotes Daily Stream"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ZenQuotes failed or offline: ${e.message}")
        }

        // Attempt 2: DummyJSON random quote API
        try {
            val request = Request.Builder()
                .url("https://dummyjson.com/quotes/random")
                .header("User-Agent", "JARVIS-Assistant/1.0")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                val obj = JSONObject(body)
                val quoteText = obj.optString("quote").trim()
                val author = obj.optString("author", "Unknown").trim()
                if (quoteText.isNotBlank()) {
                    val category = classifyCategory(quoteText)
                    return@withContext QuoteResult(
                        quote = quoteText,
                        author = author.ifBlank { "Wise Mind" },
                        category = category,
                        source = "Global Quotes Stream"
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DummyJSON fallback failed: ${e.message}")
        }

        // Deterministic Daily Archive fallback: day of the year ensures a fresh quote each calendar day
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val index = dayOfYear % CURATED_DAILY_QUOTES.size
        return@withContext CURATED_DAILY_QUOTES[index]
    }

    private fun classifyCategory(quote: String): String {
        val lower = quote.lowercase()
        return when {
            lower.contains("laugh") || lower.contains("fool") || lower.contains("stupid") ||
                    lower.contains("joke") || lower.contains("resist") || lower.contains("sunshine") ||
                    lower.contains("coffee") || lower.contains("money") -> "Humorous"

            lower.contains("think") || lower.contains("mind") || lower.contains("suffer") ||
                    lower.contains("imagination") || lower.contains("reality") || lower.contains("infinite") ||
                    lower.contains("truth") || lower.contains("universe") || lower.contains("death") ||
                    lower.contains("wisdom") || lower.contains("silence") -> "Thought-Provoking"

            else -> "Inspirational"
        }
    }

    fun getTodayDateFormatted(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }
}
