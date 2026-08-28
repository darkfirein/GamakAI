package com.example.planner

import com.example.ai.AiClient
import com.example.ai.AiPlanResult
import com.example.model.ChatMessage
import com.example.tools.ActionRequest
import com.example.tools.ActionResult
import com.example.tools.TaskStep
import com.example.tools.ToolRegistry

class Planner(
  private val aiClient: AiClient,
  private val toolRegistry: ToolRegistry = ToolRegistry(),
  private val actionExecutor: ActionExecutor = ActionExecutor()
) {

  suspend fun planAndExecute(
    prompt: String,
    conversationHistory: List<ChatMessage>,
    personaName: String,
    memoryContext: List<String> = emptyList()
  ): AiPlanResult {
    val rawPlan = aiClient.generatePlan(prompt, conversationHistory, personaName, memoryContext)

    return when (rawPlan) {
      is AiPlanResult.Action -> {
        validateAction(rawPlan)
      }
      is AiPlanResult.MultiAction -> {
        val validatedSteps = mutableListOf<TaskStep>()
        for (step in rawPlan.steps) {
          val toolDef = toolRegistry.getTool(step.actionRequest.toolName)
          if (toolDef != null) {
            val missing = toolDef.parameters.filter { it.required && step.actionRequest.parameters[it.name].isNullOrBlank() }
            if (missing.isNotEmpty() && !step.isDependent) {
              // Non-dependent step missing required fields -> need clarification
              val clarQ = buildClarificationQuestion(step.actionRequest.toolName, missing.first().name)
              return AiPlanResult.Clarification(
                question = clarQ,
                missingFields = missing.map { it.name },
                partialActionRequest = step.actionRequest
              )
            }
          }
          validatedSteps.add(step)
        }
        rawPlan.copy(steps = validatedSteps)
      }
      is AiPlanResult.Clarification -> rawPlan
      is AiPlanResult.MemoryOp -> rawPlan
      is AiPlanResult.Conversation -> rawPlan
      is AiPlanResult.Error -> rawPlan
    }
  }

  suspend fun executePlannedAction(action: AiPlanResult.Action): ActionResult =
    actionExecutor.execute(action.actionRequest)

  suspend fun executeAction(actionRequest: ActionRequest): ActionResult =
    actionExecutor.execute(actionRequest)

  private fun validateAction(actionPlan: AiPlanResult.Action): AiPlanResult {
    val toolDef = toolRegistry.getTool(actionPlan.actionRequest.toolName)
    if (toolDef != null) {
      val missingRequired = toolDef.parameters.filter { it.required && actionPlan.actionRequest.parameters[it.name].isNullOrBlank() }
      if (missingRequired.isNotEmpty()) {
        val missingFieldNames = missingRequired.map { it.name }
        val clarQuestion = buildClarificationQuestion(actionPlan.actionRequest.toolName, missingFieldNames.first())
        return AiPlanResult.Clarification(
          question = clarQuestion,
          missingFields = missingFieldNames,
          partialActionRequest = actionPlan.actionRequest
        )
      }
    }
    return actionPlan
  }

  private fun buildClarificationQuestion(toolName: String, missingField: String): String {
    return when (toolName) {
      "send_whatsapp_message", "send_sms" -> {
        if (missingField == "message") "क्या message भेजना है?" else "किसे message भेजना है?"
      }
      "make_call" -> "किसे कॉल करना चाहते हैं?"
      "set_alarm" -> "अलार्म किस समय का लगाना है?"
      "set_reminder" -> if (missingField == "time") "किस समय याद दिलाऊँ?" else "किस बात का रिमाइंडर लगाना है?"
      "create_calendar_event" -> if (missingField == "title") "इवेंट का क्या नाम रखना है?" else "किस समय का इवेंट बनाना है?"
      "navigate_maps" -> "कहाँ जाने का रास्ता देखना चाहते हैं?"
      else -> "कृपया $missingField की जानकारी प्रदान करें।"
    }
  }
}
