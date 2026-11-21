package com.example.model

import android.content.Context
import org.json.JSONArray

data class Personality(
    val id: Int,
    val name: String,
    val tamilName: String,
    val region: String,
    val category: String,
    val era: String,
    val imageUrl: String
) {
    val initials: String
        get() = name.split(" ")
            .filter { it.isNotBlank() && it.first().isLetter() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "?" }
}

class PersonalityRepository(context: Context) {
    private val appContext = context.applicationContext

    val allPersonalities: List<Personality> by lazy {
        loadFromAssets()
    }

    private val byId: Map<Int, Personality> by lazy {
        allPersonalities.associateBy { it.id }
    }

    fun getById(id: Int): Personality? = byId[id]

    fun getCategories(): List<String> = listOf(
        "All",
        "Cinema & Actors",
        "Directors & Music",
        "Sports",
        "Literature & History",
        "Public Figures"
    )

    fun getRegions(): List<String> = listOf("All", "Tamil", "Pan-Indian")

    private fun loadFromAssets(): List<Personality> {
        val jsonText = appContext.assets.open("personalities.json")
            .bufferedReader()
            .use { it.readText() }
        val array = JSONArray(jsonText)
        val result = ArrayList<Personality>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                Personality(
                    id = obj.getInt("id"),
                    name = obj.getString("name"),
                    tamilName = obj.optString("tamil_name", ""),
                    region = obj.getString("region"),
                    category = obj.getString("category"),
                    era = obj.getString("era"),
                    imageUrl = obj.optString("image_url", "")
                )
            )
        }
        return result.take(100)
    }
}
