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
  private val toolRegistry: ToolRegistry = ToolRegistry()
) : AiClient {

  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  companion object {
    private const val TAG = "GeminiAiClient"
    private const val MODEL_ENDPOINT =
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
  }

  override suspend fun generatePlan(
    prompt: String,
    conversationHistory: List<ChatMessage>,
    personaName: String,
    memoryContext: List<String>
  ): AiPlanResult = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY

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
        You are $personaName, the intelligent, context-aware AI Voice Assistant in Gamak AI.
        You understand Hindi (हिंदी), Nepali (नेपाली), Hinglish, English, and mixed multilingual code-switching naturally.
        $memoryBlock

        AVAILABLE TOOLS:
        $toolsSchema

        YOUR TASK:
        Analyze the user's query in conversation context and return ONE JSON object adhering strictly to one of these schemas:

        1. SINGLE ACTION:
        {
          "type": "ACTION",
          "tool_name": "<registered_tool_name>",
          "parameters": { "<param_name>": "<param_value>" },
          "spoken_response": "<Natural polite 1-sentence confirmation in user's language>",
          "requires_confirmation": false
        }

        2. MULTI-STEP ACTIONS (Sequential execution):
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

        3. CLARIFICATION (Missing critical info):
        {
          "type": "CLARIFICATION",
          "question": "<Friendly question in user's language, e.g. 'क्या message भेजना है?'>",
          "missing_fields": ["<field_name>"],
          "partial_tool_name": "<tool_name_if_known>"
        }

        4. MEMORY STORAGE (User explicitly states a preference to remember):
        {
          "type": "MEMORY_OP",
          "operation": "SAVE",
          "key": "<key_name>",
          "value": "<preference_value>",
          "response": "<Friendly confirmation in user's language>"
        }

        5. CONVERSATION / GENERAL KNOWLEDGE:
        {
          "type": "CONVERSATION",
          "response": "<Natural, concise, helpful spoken response in user's language>",
          "language": "hi/ne/en"
        }

        OUTPUT ONLY VALID JSON. Do not include markdown formatting like ```json.
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

  private fun parseGeminiResponse(
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
