package com.example.util

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object WebSearchHelper {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Fetches search completions from Google Suggest API on Dispatchers.IO.
     * Response schema: ["query", ["suggestion1", "suggestion2", ...], ...]
     */
    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://suggestqueries.google.com/complete/search?client=chrome&q=$encoded"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()

                val jsonArray = JSONArray(body)
                val suggestionsArray = jsonArray.optJSONArray(1) ?: return@withContext emptyList()

                val results = mutableListOf<String>()
                for (i in 0 until suggestionsArray.length().coerceAtMost(5)) {
                    val item = suggestionsArray.optString(i)
                    if (item.isNotBlank()) {
                        results.add(item)
                    }
                }
                return@withContext results
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    /**
     * Launches the default browser or search app for the specified search query.
     */
    fun launchWebSearch(context: Context, query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, trimmed)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val encoded = URLEncoder.encode(trimmed, "UTF-8")
                val fallbackIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=$encoded")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (err: Exception) {
                // Ignored
            }
        }
    }
}
