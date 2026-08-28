package com.example.memory

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.memoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "gamak_ai_memories")

class MemoryRepository(private val context: Context) {

  companion object {
    private const val TAG = "MemoryRepository"
    private val MEMORIES_JSON_KEY = stringPreferencesKey("memories_json_list")
  }

  val memoriesFlow: Flow<List<MemoryItem>> = context.memoryDataStore.data.map { prefs ->
    val jsonStr = prefs[MEMORIES_JSON_KEY] ?: "[]"
    parseMemories(jsonStr)
  }

  suspend fun saveMemory(key: String, value: String, category: String = "preference"): Boolean {
    // Privacy check: reject passwords, tokens, full phone numbers, or credit card numbers
    if (isSensitiveContent(key) || isSensitiveContent(value)) {
      Log.w(TAG, "Rejected memory save due to privacy protection policy.")
      return false
    }

    val trimmedKey = key.trim()
    val trimmedVal = value.trim()
    if (trimmedKey.isBlank() || trimmedVal.isBlank()) return false

    context.memoryDataStore.edit { prefs ->
      val currentJson = prefs[MEMORIES_JSON_KEY] ?: "[]"
      val list = parseMemories(currentJson).toMutableList()

      // Remove existing item with identical key if exists
      list.removeAll { it.key.equals(trimmedKey, ignoreCase = true) }

      // Add new memory item (enforcing max limit of 50 items for context safety)
      if (list.size >= 50) {
        list.removeAt(0)
      }
      list.add(
        MemoryItem(
          key = trimmedKey,
          value = trimmedVal,
          category = category
        )
      )

      prefs[MEMORIES_JSON_KEY] = serializeMemories(list)
    }
    return true
  }

  suspend fun deleteMemory(id: String) {
    context.memoryDataStore.edit { prefs ->
      val currentJson = prefs[MEMORIES_JSON_KEY] ?: "[]"
      val list = parseMemories(currentJson).filterNot { it.id == id }
      prefs[MEMORIES_JSON_KEY] = serializeMemories(list)
    }
  }

  suspend fun clearAllMemories() {
    context.memoryDataStore.edit { prefs ->
      prefs[MEMORIES_JSON_KEY] = "[]"
    }
  }

  private fun parseMemories(jsonStr: String): List<MemoryItem> {
    val items = mutableListOf<MemoryItem>()
    try {
      val array = JSONArray(jsonStr)
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        items.add(
          MemoryItem(
            id = obj.optString("id"),
            key = obj.optString("key"),
            value = obj.optString("value"),
            category = obj.optString("category", "general"),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
          )
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing memories JSON", e)
    }
    return items
  }

  private fun serializeMemories(list: List<MemoryItem>): String {
    val array = JSONArray()
    for (item in list) {
      val obj = JSONObject().apply {
        put("id", item.id)
        put("key", item.key)
        put("value", item.value)
        put("category", item.category)
        put("timestamp", item.timestamp)
      }
      array.put(obj)
    }
    return array.toString()
  }

  private fun isSensitiveContent(text: String): Boolean {
    val lower = text.lowercase()
    if (lower.contains("password") || lower.contains("पासवर्ड") ||
      lower.contains("pin") || lower.contains("पिन") ||
      lower.contains("cvv") || lower.contains("token") ||
      lower.contains("api_key") || lower.contains("apikey") ||
      lower.contains("secret") || lower.contains("गुप्त")
    ) {
      return true
    }

    // Check for raw 10+ consecutive digit numbers
    if (Regex("""\b\d{10,16}\b""").containsMatchIn(text)) {
      return true
    }

    return false
  }
}
