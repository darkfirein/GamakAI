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
        Log.d(TAG, "Executing plan with primary AI engine (OpenAI)")
        val result = primaryClient.generatePlan(prompt, conversationHistory, personaName, memoryContext)
        if (result !is AiPlanResult.Error) {
          return result
        }
        Log.w(TAG, "Primary AI returned error: ${result.message}. Cascading to secondary AI engine.")
      } catch (e: Exception) {
        Log.w(TAG, "Primary AI engine execution failed. Cascading to secondary AI engine.", e)
      }
    } else {
      Log.d(TAG, "Primary AI engine (OpenAI) not available/configured.")
    }

    // 2. Tier 2: Secondary AI Engine (Gemini)
    if (secondaryClient.isAvailable()) {
      try {
        Log.d(TAG, "Executing plan with secondary AI engine (Gemini)")
        val result = secondaryClient.generatePlan(prompt, conversationHistory, personaName, memoryContext)
        if (result !is AiPlanResult.Error) {
          return result
        }
        Log.w(TAG, "Secondary AI returned error: ${result.message}. Cascading to Local NLU.")
      } catch (e: Exception) {
        Log.w(TAG, "Secondary AI engine execution failed. Cascading to Local NLU.", e)
      }
    } else {
      Log.d(TAG, "Secondary AI engine (Gemini) not available/configured.")
    }

    // 3. Tier 3: Deterministic & Multilingual Local NLU Fallback
    Log.d(TAG, "Executing plan with deterministic LocalNluEngine fallback")
    return LocalNluEngine.parse(prompt, personaName)
  }
}
