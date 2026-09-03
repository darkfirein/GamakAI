package com.example.ai

import android.util.Log
import com.example.model.ChatMessage
import com.example.tools.ToolRegistry

class CompositeAiClient(
  val primaryClient: AiClient,
  val secondaryClient: AiClient,
  val toolRegistry: ToolRegistry = ToolRegistry()
) : AiClient {

  companion object {
    private const val TAG = "CompositeAiClient"
  }

  override fun isAvailable(): Boolean {
    return primaryClient.isAvailable() || secondaryClient.isAvailable()
  }

  override suspend fun generatePlan(
    prompt: String,
    conversationHistory: List<ChatMessage>,
    personaName: String,
    memoryContext: List<String>
  ): AiPlanResult {
    // 1. Tier 1: Primary AI Engine (OpenAI)
    if (primaryClient.isAvailable()) {
      try {
        Log.d(TAG, "Routing query to primary AI engine (OpenAI)")
        val result = primaryClient.generatePlan(prompt, conversationHistory, personaName, memoryContext)
        if (result !is AiPlanResult.Error) {
          Log.d(TAG, "Primary AI engine (OpenAI) successfully handled request.")
          return result
        }
        Log.w(TAG, "Primary AI (OpenAI) fallback reason: ${result.message}. Cascading to secondary engine.")
      } catch (e: Exception) {
        Log.w(TAG, "Primary AI engine execution threw exception: ${e.message}. Cascading to secondary engine.", e)
      }
    } else {
      Log.d(TAG, "Primary AI engine (OpenAI) is not available/configured.")
    }

    // 2. Tier 2: Secondary AI Engine (Gemini)
    if (secondaryClient.isAvailable()) {
      try {
        Log.d(TAG, "Routing query to secondary AI engine (Gemini)")
        val result = secondaryClient.generatePlan(prompt, conversationHistory, personaName, memoryContext)
        if (result !is AiPlanResult.Error) {
          Log.d(TAG, "Secondary AI engine (Gemini) successfully handled request.")
          return result
        }
        Log.w(TAG, "Secondary AI (Gemini) fallback reason: ${result.message}. Cascading to Local NLU.")
      } catch (e: Exception) {
        Log.w(TAG, "Secondary AI engine execution threw exception: ${e.message}. Cascading to Local NLU.", e)
      }
    } else {
      Log.d(TAG, "Secondary AI engine (Gemini) is not available/configured.")
    }

    // 3. Tier 3: Deterministic & Multilingual Local NLU Fallback
    Log.d(TAG, "Fallback triggered: Using deterministic LocalNluEngine fallback.")
    return LocalNluEngine.parse(prompt, personaName)
  }
}
