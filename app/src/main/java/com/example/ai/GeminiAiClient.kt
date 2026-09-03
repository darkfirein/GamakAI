package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.model.ChatMessage
import com.example.tools.ActionRequest
import com.example.tools.TaskStep
import com.example.tools.TaskStepStatus
import com.example.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAiClient(
  private val toolRegistry: ToolRegistry = ToolRegistry(),
  private val apiKeyProvider: () -> String = {
    System.getenv("GEMINI_API_KEY")?.trim()?.takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }
      ?: System.getProperty("GEMINI_API_KEY")?.trim()?.takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }
      ?: BuildConfig.GEMINI_API_KEY.trim()
  }
) : AiClient {

  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  companion object {
    private const val TAG = "GeminiAiClient"
    private const val MODEL_ENDPOINT =
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
  }

  override fun isAvailable(): Boolean {
    val key = try {
      apiKeyProvider().trim()
    } catch (_: Exception) {
      ""
    }
    return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
  }

  override suspend fun generatePlan(
    prompt: String,
    conversationHistory: List<ChatMessage>,
    personaName: String,
    memoryContext: List<String>
  ): AiPlanResult = withContext(Dispatchers.IO) {
    val apiKey = apiKeyProvider()

    // If API key is empty or placeholder, gracefully use LocalNluEngine
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      Log.d(TAG, "Using LocalNluEngine (API key not configured or default placeholder)")
      return@withContext LocalNluEngine.parse(prompt, personaName)
    }


    try {
      val toolsSchema = buildToolsJsonDescription()
      val memoryBlock = if (memoryContext.isNotEmpty()) {
        "\nUSER PREFERENCES & MEMORIES:\n" + memoryContext.joinToString("\n") { "- $it" }
      } else ""

      val systemPrompt = """
        You are $personaName, the highly intelligent, context-aware AI Voice & Action Assistant in Gamak AI.
        You naturally understand Hindi (हिंदी), Nepali (नेपाली), Hinglish, English, and multilingual code-switching.
        $memoryBlock

        AVAILABLE DEVICE TOOLS:
        $toolsSchema

        ROUTING INSTRUCTIONS:
        1. CONVERSATION & QUESTIONS (type: "CONVERSATION"):
           - For general knowledge questions, science, history, explanations, calculations, translations, casual chit-chat, greetings, identity questions, advice, stories, jokes, etc., YOU MUST return type: "CONVERSATION".
           - Provide a direct, fluent, helpful, natural spoken response in the user's language.
           - NEVER return a generic placeholder like "I understood... ready to work on this". Always provide the real answer.

        2. DEVICE ACTIONS (type: "ACTION" or "MULTI_ACTION"):
           - ONLY choose type "ACTION" or "MULTI_ACTION" when the user explicitly requests an action to be performed on their Android device (e.g. make a call, send SMS/WhatsApp, open camera/gallery/settings/apps, launch YouTube/music, set alarm/timer/reminder, schedule calendar event, check weather, navigation).

        3. CLARIFICATION (type: "CLARIFICATION"):
           - When an action is requested but critical required information is missing (e.g. recipient name for call/SMS, message body for WhatsApp, time for alarm), ask a short, polite clarification question.

        4. MEMORY STORAGE (type: "MEMORY_OP"):
           - When the user explicitly asks you to remember a preference or detail (e.g. "याद रखो मुझे...", "remember that...").

        OUTPUT SCHEMAS (Return ONE strict JSON object):

        1. CONVERSATION / GENERAL KNOWLEDGE:
        {
          "type": "CONVERSATION",
          "response": "<Comprehensive, natural, accurate spoken response in the user's language>",
          "language": "hi/ne/en"
        }

        2. SINGLE ACTION:
        {
          "type": "ACTION",
          "tool_name": "<registered_tool_name>",
          "parameters": { "<param_name>": "<param_value>" },
          "spoken_response": "<Natural polite 1-sentence confirmation in user's language>",
          "requires_confirmation": false
        }

        3. MULTI-STEP ACTIONS:
        {
          "type": "MULTI_ACTION",
          "steps": [
            {
              "tool_name": "<registered_tool_name>",
              "parameters": { "<param_name>": "<param_value>" },
              "description": "<Short action step description in Hindi/English>",
              "requires_confirmation": false,
              "is_dependent": false
            }
          ],
          "spoken_summary": "<Natural summary of the plan in user's language>"
        }

        4. CLARIFICATION:
        {
          "type": "CLARIFICATION",
          "question": "<Friendly clarifying question in user's language>",
          "missing_fields": ["<field_name>"],
          "partial_tool_name": "<tool_name_if_known>"
        }

        5. MEMORY STORAGE:
        {
          "type": "MEMORY_OP",
          "operation": "SAVE",
          "key": "<key_name>",
          "value": "<preference_value>",
          "response": "<Friendly confirmation in user's language>"
        }

        OUTPUT ONLY VALID JSON. No markdown code fences.
      """.trimIndent()

      val requestJson = JSONObject().apply {
        put("systemInstruction", JSONObject().apply {
          put("parts", JSONArray().apply {
            put(JSONObject().put("text", systemPrompt))
          })
        })

        val contentsArray = JSONArray()
        val recentHistory = conversationHistory.takeLast(6)
        for (msg in recentHistory) {
          val role = if (msg.isUser) "user" else "model"
          contentsArray.put(JSONObject().apply {
            put("role", role)
            put("parts", JSONArray().apply {
              put(JSONObject().put("text", msg.text))
            })
          })
        }

        contentsArray.put(JSONObject().apply {
          put("role", "user")
          put("parts", JSONArray().apply {
            put(JSONObject().put("text", prompt))
          })
        })

        put("contents", contentsArray)

        put("generationConfig", JSONObject().apply {
          put("temperature", 0.2)
          put("responseMimeType", "application/json")
        })
      }

      val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
      val url = "$MODEL_ENDPOINT?key=$apiKey"

      val httpRequest = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

      val response = client.newCall(httpRequest).execute()
      val responseBodyString = response.body?.string()

      if (!response.isSuccessful || responseBodyString.isNullOrBlank()) {
        Log.w(TAG, "Gemini API returned code ${response.code}: $responseBodyString. Falling back to LocalNluEngine.")
        return@withContext LocalNluEngine.parse(prompt, personaName)
      }

      parseGeminiResponse(responseBodyString, prompt, personaName)
    } catch (e: Exception) {
      Log.e(TAG, "Gemini API error, using LocalNluEngine fallback", e)
      LocalNluEngine.parse(prompt, personaName)
    }
  }

  fun parseGeminiResponse(
    responseBody: String,
    rawPrompt: String,
    personaName: String
  ): AiPlanResult {
    try {
      val root = JSONObject(responseBody)
      val candidates = root.optJSONArray("candidates") ?: return LocalNluEngine.parse(rawPrompt, personaName)
      if (candidates.length() == 0) return LocalNluEngine.parse(rawPrompt, personaName)

      val firstCandidate = candidates.getJSONObject(0)
      val content = firstCandidate.optJSONObject("content") ?: return LocalNluEngine.parse(rawPrompt, personaName)
      val parts = content.optJSONArray("parts") ?: return LocalNluEngine.parse(rawPrompt, personaName)
      if (parts.length() == 0) return LocalNluEngine.parse(rawPrompt, personaName)

      val rawJsonText = parts.getJSONObject(0).optString("text", "")
      val cleanJson = rawJsonText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

      val parsed = JSONObject(cleanJson)
      val type = parsed.optString("type", "CONVERSATION").uppercase()

      return when (type) {
        "ACTION" -> {
          val toolName = parsed.optString("tool_name", "")
          val spoken = parsed.optString("spoken_response", "Operation initiated.")
          val requiresConfirm = parsed.optBoolean("requires_confirmation", false)
          val paramsMap = extractJsonMap(parsed.optJSONObject("parameters"))

          AiPlanResult.Action(
            actionRequest = ActionRequest(
              toolName = toolName,
              parameters = paramsMap,
              rawQuery = rawPrompt
            ),
            spokenConfirmation = spoken,
            requiresConfirmation = requiresConfirm
          )
        }
        "MULTI_ACTION" -> {
          val stepsArray = parsed.optJSONArray("steps")
          val stepsList = mutableListOf<TaskStep>()
          if (stepsArray != null) {
            for (i in 0 until stepsArray.length()) {
              val sObj = stepsArray.getJSONObject(i)
              val tName = sObj.optString("tool_name", "")
              val tParams = extractJsonMap(sObj.optJSONObject("parameters"))
              val tDesc = sObj.optString("description", "Step ${i + 1}")
              val tReqConf = sObj.optBoolean("requires_confirmation", false)
              val tIsDep = sObj.optBoolean("is_dependent", i > 0)

              stepsList.add(
                TaskStep(
                  actionRequest = ActionRequest(toolName = tName, parameters = tParams, rawQuery = rawPrompt),
                  description = tDesc,
                  requiresConfirmation = tReqConf,
                  isDependent = tIsDep,
                  status = TaskStepStatus.PENDING
                )
              )
            }
          }

          val summary = parsed.optString("spoken_summary", "Multi-step plan ready.")
          if (stepsList.isNotEmpty()) {
            AiPlanResult.MultiAction(steps = stepsList, spokenSummary = summary)
          } else {
            LocalNluEngine.parse(rawPrompt, personaName)
          }
        }
        "CLARIFICATION" -> {
          val question = parsed.optString("question", "Could you provide more details?")
          val missingFieldsArray = parsed.optJSONArray("missing_fields")
          val missingList = mutableListOf<String>()
          if (missingFieldsArray != null) {
            for (i in 0 until missingFieldsArray.length()) {
              missingList.add(missingFieldsArray.optString(i))
            }
          }
          val partialTool = parsed.optString("partial_tool_name", "")
          val partialRequest = if (partialTool.isNotBlank()) {
            ActionRequest(toolName = partialTool, parameters = emptyMap(), rawQuery = rawPrompt)
          } else null

          AiPlanResult.Clarification(
            question = question,
            missingFields = missingList,
            partialActionRequest = partialRequest
          )
        }
        "MEMORY_OP" -> {
          val op = parsed.optString("operation", "SAVE")
          val key = parsed.optString("key", "preference")
          val value = parsed.optString("value", "")
          val response = parsed.optString("response", "Remembered.")
          AiPlanResult.MemoryOp(operation = op, key = key, value = value, responseText = response)
        }
        else -> {
          val responseText = parsed.optString("response", "I have processed your request.")
          val lang = parsed.optString("language", "auto")
          AiPlanResult.Conversation(responseText = responseText, detectedLanguage = lang)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing Gemini structured response: $responseBody", e)
      return LocalNluEngine.parse(rawPrompt, personaName)
    }
  }

  private fun extractJsonMap(jsonObj: JSONObject?): Map<String, String> {
    val map = mutableMapOf<String, String>()
    if (jsonObj != null) {
      val keys = jsonObj.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        map[key] = jsonObj.optString(key, "")
      }
    }
    return map
  }

  private fun buildToolsJsonDescription(): String {
    val allTools = toolRegistry.getAllTools()
    val sb = StringBuilder()
    for (tool in allTools) {
      sb.append("- Tool: `").append(tool.name).append("`: ").append(tool.description).append("\n")
      sb.append("  Params: ")
      val params = tool.parameters.joinToString(", ") { "${it.name} (${it.type}, required=${it.required}): ${it.description}" }
      sb.append(params).append("\n")
    }
    return sb.toString()
  }
}
