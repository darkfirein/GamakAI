package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.model.ChatMessage
import com.example.tools.ActionRequest
import com.example.tools.TaskStep
import com.example.tools.TaskStepStatus
import com.example.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class OpenAiClient(
  private val toolRegistry: ToolRegistry = ToolRegistry(),
  private val apiKeyProvider: () -> String = {
    System.getenv("OPENAI_API_KEY")?.trim()?.takeIf { it.isNotBlank() && it != "MY_OPENAI_API_KEY" }
      ?: System.getProperty("OPENAI_API_KEY")?.trim()?.takeIf { it.isNotBlank() && it != "MY_OPENAI_API_KEY" }
      ?: BuildConfig.OPENAI_API_KEY.trim()
  },
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(10, TimeUnit.SECONDS)
    .build(),
  private val model: String = "gpt-4o-mini",
  private val baseUrl: String = "https://api.openai.com/v1"
) : AiClient {

  companion object {
    private const val TAG = "OpenAiClient"
    private const val MAX_RETRIES = 1
    private const val RETRY_DELAY_MS = 500L
    private const val DEBOUNCE_WINDOW_MS = 1200L
  }

  // Deduplication cache: Key = clean prompt, Value = (Timestamp, Result)
  private val recentRequests = ConcurrentHashMap<String, Pair<Long, AiPlanResult>>()

  override fun isAvailable(): Boolean {
    val key = try {
      apiKeyProvider().trim()
    } catch (_: Exception) {
      ""
    }
    return key.isNotBlank() && key != "MY_OPENAI_API_KEY"
  }

  override suspend fun generatePlan(
    prompt: String,
    conversationHistory: List<ChatMessage>,
    personaName: String,
    memoryContext: List<String>
  ): AiPlanResult = withContext(Dispatchers.IO) {
    currentCoroutineContext().ensureActive()

    val apiKey = apiKeyProvider().trim()
    if (apiKey.isBlank() || apiKey == "MY_OPENAI_API_KEY") {
      Log.d(TAG, "OpenAI API key is not configured or is placeholder.")
      return@withContext AiPlanResult.Error("OpenAI API key not configured")
    }

    val cleanPrompt = prompt.trim()
    val now = System.currentTimeMillis()

    // 1. Duplicate Request / Debounce Check
    recentRequests[cleanPrompt]?.let { (timestamp, cachedResult) ->
      if (now - timestamp < DEBOUNCE_WINDOW_MS && cachedResult !is AiPlanResult.Error) {
        Log.d(TAG, "Returning cached duplicate request result for: '$cleanPrompt'")
        return@withContext cachedResult
      }
    }

    val systemPrompt = buildSystemPrompt(personaName, memoryContext)
    val requestJson = buildRequestBody(systemPrompt, cleanPrompt, conversationHistory)

    var attempt = 0
    var lastException: Exception? = null

    while (attempt <= MAX_RETRIES) {
      currentCoroutineContext().ensureActive()
      try {
        val result = executeChatCompletion(apiKey, requestJson, cleanPrompt, personaName)
        if (result !is AiPlanResult.Error) {
          recentRequests[cleanPrompt] = Pair(now, result)
          return@withContext result
        }

        // If it's a non-retryable error (e.g. 401 unauthorized), return immediately
        if (result.message.contains("401") || result.message.contains("invalid_api_key")) {
          return@withContext result
        }

        lastException = IOException(result.message)
      } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        Log.w(TAG, "OpenAI API call attempt $attempt failed: ${e.message}")
        lastException = e
      }

      attempt++
      if (attempt <= MAX_RETRIES) {
        delay(RETRY_DELAY_MS)
      }
    }

    val errorMsg = lastException?.localizedMessage ?: "Unknown OpenAI network error"
    AiPlanResult.Error("OpenAI request failed after $MAX_RETRIES retries: $errorMsg")
  }

  private fun buildSystemPrompt(personaName: String, memoryContext: List<String>): String {
    val toolsSchema = buildToolsJsonDescription()
    val memoryBlock = if (memoryContext.isNotEmpty()) {
      "\nUSER PREFERENCES & LONG-TERM MEMORIES:\n" + memoryContext.joinToString("\n") { "- $it" }
    } else ""

    return """
      You are $personaName, the primary intelligent, context-aware conversational AI Assistant in Gamak AI.
      You excel at natural, human-like, multi-turn conversation and deep understanding of Hindi (हिंदी), Nepali (नेपाली), Hinglish, English, and code-mixed Indian/Nepali queries.
      $memoryBlock

      AVAILABLE ANDROID DEVICE TOOLS:
      $toolsSchema

      CRITICAL ROUTING & BEHAVIOR DIRECTIVES:
      1. NATURAL CONVERSATION & QUESTIONS (type: "CONVERSATION"):
         - For any general knowledge question, science, history, explanations, calculations, translations, casual chit-chat, greetings, identity, reasoning, jokes, storytelling, or advice, return type: "CONVERSATION".
         - Always give a comprehensive, warm, accurate, direct spoken answer in the exact language or mix used by the user.
         - NEVER use generic placeholders like "I am ready to work on this". Always provide the actual answer.

      2. ANDROID DEVICE ACTIONS (type: "ACTION" or "MULTI_ACTION"):
         - ONLY select type "ACTION" or "MULTI_ACTION" when the user explicitly instructs to perform an action on their phone (e.g., make phone call, send SMS/WhatsApp, open camera/gallery/settings/apps, search/play on YouTube, play music, set alarm/timer/reminder, create calendar event, check live weather, navigate on Maps).
         - Provide a short, pleasant spoken confirmation in the user's language.

      3. CLARIFICATION (type: "CLARIFICATION"):
         - If an action is requested but mandatory details are missing (e.g. contact name for call/SMS, message text for WhatsApp, time for alarm), ask a polite, concise clarifying question.

      4. MEMORY PERSISTENCE (type: "MEMORY_OP"):
         - When the user asks you to remember a fact or personal preference (e.g., "याद रखो मुझे...", "remember that..."), return type "MEMORY_OP" with operation "SAVE".

      OUTPUT SCHEMAS (Return ONE strict valid JSON object):

      1. CONVERSATION / GENERAL KNOWLEDGE:
      {
        "type": "CONVERSATION",
        "response": "<Natural, fluent, informative spoken answer in user's language>",
        "language": "hi/ne/en"
      }

      2. SINGLE DEVICE ACTION:
      {
        "type": "ACTION",
        "tool_name": "<registered_tool_name>",
        "parameters": { "<param_name>": "<param_value>" },
        "spoken_response": "<Friendly 1-sentence confirmation in user's language>",
        "requires_confirmation": false
      }

      3. MULTI-STEP ACTIONS:
      {
        "type": "MULTI_ACTION",
        "steps": [
          {
            "tool_name": "<registered_tool_name>",
            "parameters": { "<param_name>": "<param_value>" },
            "description": "<Action step summary>",
            "requires_confirmation": false,
            "is_dependent": false
          }
        ],
        "spoken_summary": "<Spoken summary of the multi-step plan in user's language>"
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

      OUTPUT ONLY VALID JSON. No markdown code blocks or wrapping.
    """.trimIndent()
  }

  private fun buildRequestBody(
    systemPrompt: String,
    prompt: String,
    conversationHistory: List<ChatMessage>
  ): JSONObject {
    val messagesArray = JSONArray()

    // 1. System Prompt
    messagesArray.put(JSONObject().apply {
      put("role", "system")
      put("content", systemPrompt)
    })

    // 2. Recent Multi-Turn Conversation History (up to 8 recent messages)
    val recentHistory = conversationHistory.takeLast(8)
    for (msg in recentHistory) {
      val role = if (msg.isUser) "user" else "assistant"
      if (msg.text.isNotBlank()) {
        messagesArray.put(JSONObject().apply {
          put("role", role)
          put("content", msg.text)
        })
      }
    }

    // 3. Current User Message
    messagesArray.put(JSONObject().apply {
      put("role", "user")
      put("content", prompt)
    })

    return JSONObject().apply {
      put("model", model)
      put("messages", messagesArray)
      put("temperature", 0.3)
      put("response_format", JSONObject().apply {
        put("type", "json_object")
      })
    }
  }

  private fun executeChatCompletion(
    apiKey: String,
    requestJson: JSONObject,
    rawPrompt: String,
    personaName: String
  ): AiPlanResult {
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = requestJson.toString().toRequestBody(mediaType)

    val request = Request.Builder()
      .url("$baseUrl/chat/completions")
      .addHeader("Authorization", "Bearer $apiKey")
      .addHeader("Content-Type", "application/json")
      .post(requestBody)
      .build()

    val response = client.newCall(request).execute()
    val responseBody = response.body?.string()

    if (!response.isSuccessful || responseBody.isNullOrBlank()) {
      val code = response.code
      Log.w(TAG, "OpenAI API returned HTTP $code: $responseBody")
      return AiPlanResult.Error("OpenAI API error ($code): ${responseBody ?: "Empty response"}")
    }

    return parseOpenAiResponse(responseBody, rawPrompt, personaName)
  }

  fun parseOpenAiResponse(
    responseBody: String,
    rawPrompt: String,
    personaName: String
  ): AiPlanResult {
    return try {
      val root = JSONObject(responseBody)
      val choices = root.optJSONArray("choices") ?: return AiPlanResult.Error("Missing choices array in OpenAI response")
      if (choices.length() == 0) return AiPlanResult.Error("Empty choices returned from OpenAI")


      val firstChoice = choices.getJSONObject(0)
      val messageObj = firstChoice.optJSONObject("message") ?: return AiPlanResult.Error("Missing message in choice")
      val rawContent = messageObj.optString("content", "").trim()

      if (rawContent.isBlank()) {
        return AiPlanResult.Error("Empty content from OpenAI")
      }

      val cleanJson = rawContent
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()

      val parsed = try {
        JSONObject(cleanJson)
      } catch (e: Exception) {
        // If it's natural plain text, treat directly as Conversation
        return AiPlanResult.Conversation(responseText = cleanJson)
      }

      val type = parsed.optString("type", "CONVERSATION").uppercase()

      return when (type) {
        "ACTION" -> {
          val toolName = parsed.optString("tool_name", "")
          val spoken = parsed.optString("spoken_response", "कार्य शुरू किया जा रहा है।")
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

          val summary = parsed.optString("spoken_summary", "मल्टी-स्टेप योजना तैयार है।")
          if (stepsList.isNotEmpty()) {
            AiPlanResult.MultiAction(steps = stepsList, spokenSummary = summary)
          } else {
            AiPlanResult.Error("Empty steps list in MultiAction")
          }
        }

        "CLARIFICATION" -> {
          val question = parsed.optString("question", "कृपया अधिक विवरण प्रदान करें।")
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
          val response = parsed.optString("response", "याद रख लिया गया।")
          AiPlanResult.MemoryOp(operation = op, key = key, value = value, responseText = response)
        }

        else -> {
          val responseText = parsed.optString("response", cleanJson)
          val lang = parsed.optString("language", "auto")
          AiPlanResult.Conversation(responseText = responseText, detectedLanguage = lang)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing OpenAI response", e)
      AiPlanResult.Error("Failed to parse OpenAI response: ${e.localizedMessage}")
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
