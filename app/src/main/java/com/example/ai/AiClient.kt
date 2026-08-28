package com.example.ai

import com.example.model.ChatMessage
import com.example.tools.ActionRequest
import com.example.tools.TaskStep

sealed interface AiPlanResult {
  data class Conversation(
    val responseText: String,
    val detectedLanguage: String = "auto"
  ) : AiPlanResult

  data class Action(
    val actionRequest: ActionRequest,
    val spokenConfirmation: String,
    val requiresConfirmation: Boolean = false
  ) : AiPlanResult

  data class MultiAction(
    val steps: List<TaskStep>,
    val spokenSummary: String = ""
  ) : AiPlanResult

  data class Clarification(
    val question: String,
    val missingFields: List<String>,
    val partialActionRequest: ActionRequest? = null
  ) : AiPlanResult

  data class MemoryOp(
    val operation: String, // "SAVE", "DELETE", "LIST"
    val key: String,
    val value: String,
    val responseText: String
  ) : AiPlanResult

  data class Error(
    val message: String
  ) : AiPlanResult
}

interface AiClient {
  suspend fun generatePlan(
    prompt: String,
    conversationHistory: List<ChatMessage>,
    personaName: String,
    memoryContext: List<String> = emptyList()
  ): AiPlanResult
}
