package com.example.platform

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class WeatherInfo(
  val locationName: String,
  val temperature: Double,
  val weatherCode: Int,
  val conditionDescription: String,
  val humidity: Int,
  val windSpeed: Double,
  val highTemp: Double? = null,
  val lowTemp: Double? = null
)

interface WeatherService {
  suspend fun fetchWeather(locationQuery: String, language: String = "hi"): Result<WeatherInfo>
}

class OpenMeteoWeatherService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()
) : WeatherService {

  companion object {
    private const val TAG = "WeatherService"
    private const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
    private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
  }

  override suspend fun fetchWeather(locationQuery: String, language: String): Result<WeatherInfo> =
    withContext(Dispatchers.IO) {
      try {
        val targetCity = cleanLocationQuery(locationQuery)
        val (lat, lon, resolvedName) = geocodeCity(targetCity)

        val forecastUrl = "$FORECAST_URL?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
        val request = Request.Builder().url(forecastUrl).build()
        val response = client.newCall(request).execute()

        val body = response.body?.string()
        if (!response.isSuccessful || body.isNullOrBlank()) {
          return@withContext Result.failure(Exception("Weather API returned HTTP ${response.code}"))
        }

        val json = JSONObject(body)
        val current = json.optJSONObject("current") ?: return@withContext Result.failure(Exception("Missing current weather data"))
        val temp = current.optDouble("temperature_2m", 25.0)
        val code = current.optInt("weather_code", 0)
        val humidity = current.optInt("relative_humidity_2m", 50)
        val windSpeed = current.optDouble("wind_speed_10m", 5.0)

        val daily = json.optJSONObject("daily")
        val maxTemps = daily?.optJSONArray("temperature_2m_max")
        val minTemps = daily?.optJSONArray("temperature_2m_min")
        val maxT = if (maxTemps != null && maxTemps.length() > 0) maxTemps.optDouble(0) else null
        val minT = if (minTemps != null && minTemps.length() > 0) minTemps.optDouble(0) else null

        val condition = translateWeatherCode(code, language)

        val info = WeatherInfo(
          locationName = resolvedName,
          temperature = temp,
          weatherCode = code,
          conditionDescription = condition,
          humidity = humidity,
          windSpeed = windSpeed,
          highTemp = maxT,
          lowTemp = minT
        )
        Result.success(info)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to retrieve real-time weather", e)
        Result.failure(e)
      }
    }

  private fun cleanLocationQuery(query: String): String {
    val lower = query.lowercase().trim()
    return when {
      lower.contains("kathmandu") || lower.contains("काठमाडौं") || lower.contains("काठमाण्डौ") -> "Kathmandu"
      lower.contains("delhi") || lower.contains("दिल्ली") -> "Delhi"
      lower.contains("mumbai") || lower.contains("मुम्बई") || lower.contains("बॉम्बे") -> "Mumbai"
      lower.contains("pokhara") || lower.contains("पोखरा") -> "Pokhara"
      lower.contains("kolkata") || lower.contains("कोलकाता") -> "Kolkata"
      lower.contains("patna") || lower.contains("पटना") -> "Patna"
      lower.contains("bangalore") || lower.contains("बेंगलुरु") -> "Bangalore"
      lower.contains("london") || lower.contains("लंदन") -> "London"
      lower.contains("new york") || lower.contains("न्यूयॉर्क") -> "New York"
      lower.contains("tokyo") || lower.contains("टोक्यो") -> "Tokyo"
      lower.isBlank() || lower.contains("वर्तमान") || lower.contains("current") || lower.contains("आज") || lower.contains("here") -> "New Delhi"
      else -> {
        query.replace(Regex("""(?:in|at|में|का|को|मा|weather|मौसम|तापमान)""", RegexOption.IGNORE_CASE), "").trim()
          .ifBlank { "New Delhi" }
      }
    }
  }

  private fun geocodeCity(cityName: String): Triple<Double, Double, String> {
    try {
      val encoded = URLEncoder.encode(cityName, "UTF-8")
      val url = "$GEOCODING_URL?name=$encoded&count=1&language=en&format=json"
      val request = Request.Builder().url(url).build()
      val response = client.newCall(request).execute()
      val body = response.body?.string()
      if (response.isSuccessful && !body.isNullOrBlank()) {
        val root = JSONObject(body)
        val results = root.optJSONArray("results")
        if (results != null && results.length() > 0) {
          val first = results.getJSONObject(0)
          val lat = first.optDouble("latitude")
          val lon = first.optDouble("longitude")
          val name = first.optString("name", cityName)
          val country = first.optString("country", "")
          val fullName = if (country.isNotBlank()) "$name, $country" else name
          return Triple(lat, lon, fullName)
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Geocoding lookup failed for '$cityName', defaulting to coordinates", e)
    }

    // Default fallback coordinates for top Indian Subcontinent hubs
    return when (cityName.lowercase()) {
      "kathmandu" -> Triple(27.7172, 85.3240, "Kathmandu, Nepal")
      "mumbai" -> Triple(19.0760, 72.8777, "Mumbai, India")
      "pokhara" -> Triple(28.2096, 83.9856, "Pokhara, Nepal")
      else -> Triple(28.6139, 77.2090, "New Delhi, India")
    }
  }

  private fun translateWeatherCode(code: Int, language: String): String {
    val isNepali = language == "ne"
    return when (code) {
      0 -> if (isNepali) "सफा आकाश (Clear Sky)" else "साफ़ आसमान (Clear Sky)"
      1, 2, 3 -> if (isNepali) "आंशिक बदली (Partly Cloudy)" else "हल्के बादल (Partly Cloudy)"
      45, 48 -> if (isNepali) "कुहिरो (Foggy)" else "कोहरा (Foggy)"
      51, 53, 55 -> if (isNepali) "हल्का झरी (Light Drizzle)" else "हल्की बूंदाबांदी (Drizzle)"
      61, 63, 65 -> if (isNepali) "वर्षा (Rain)" else "बारिश (Rain)"
      71, 73, 75 -> if (isNepali) "हिउँ पर्ने सम्भावना (Snow)" else "बर्फबारी (Snowfall)"
      80, 81, 82 -> if (isNepali) "भारी वर्षा (Rain Showers)" else "तेज बारिश (Showers)"
      95, 96, 99 -> if (isNepali) "चट्याङ र वर्षा (Thunderstorm)" else "गरज के साथ तूफान (Thunderstorm)"
      else -> if (isNepali) "सामान्य मौसम (Pleasant)" else "सुहावना मौसम (Pleasant)"
    }
  }
}
