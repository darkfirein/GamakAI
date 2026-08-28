package com.example.tools

import java.util.UUID

data class ToolParameter(
  val name: String,
  val type: String, // string, number, boolean
  val description: String,
  val required: Boolean = true
)

enum class ToolCategory {
  COMMUNICATION,
  MEDIA,
  NAVIGATION,
  SYSTEM_UTILITY,
  INFORMATION,
  PRODUCTIVITY
}

data class ToolDefinition(
  val name: String,
  val description: String,
  val category: ToolCategory,
  val parameters: List<ToolParameter>
)

data class ActionRequest(
  val toolName: String,
  val parameters: Map<String, String> = emptyMap(),
  val rawQuery: String = "",
  val confidence: Float = 1.0f
)

data class PickerOption(
  val id: String,
  val title: String,
  val subtitle: String,
  val extra: Map<String, String> = emptyMap()
)

enum class ActionStatus {
  SUCCESS,
  FAILED,
  NEEDS_PERMISSION,
  NEEDS_CONFIRMATION,
  NEEDS_MORE_INFO,
  NEEDS_PICKER,
  PLANNED_PHASE2
}

enum class TaskStepStatus {
  PENDING,
  RUNNING,
  COMPLETED,
  FAILED,
  CANCELLED,
  WAITING_FOR_PERMISSION,
  WAITING_FOR_CONFIRMATION,
  WAITING_FOR_CLARIFICATION,
  WAITING_FOR_PICKER
}

data class TaskStep(
  val id: String = UUID.randomUUID().toString(),
  val actionRequest: ActionRequest,
  val description: String,
  val requiresConfirmation: Boolean = false,
  val isDependent: Boolean = false,
  val status: TaskStepStatus = TaskStepStatus.PENDING,
  val resultMessage: String? = null,
  val timestamp: Long = System.currentTimeMillis()
)

data class TaskPlan(
  val id: String = UUID.randomUUID().toString(),
  val rawQuery: String,
  val steps: List<TaskStep>,
  val spokenSummary: String = "",
  val isComplete: Boolean = false
)

data class ConfirmationDetails(
  val actionTitle: String,
  val targetRecipient: String? = null,
  val contentSummary: String? = null,
  val warning: String? = null
)

sealed class ActionResult(
  open val status: ActionStatus,
  open val responseMessage: String,
  open val data: Map<String, String> = emptyMap()
) {
  data class Success(
    override val responseMessage: String,
    override val data: Map<String, String> = emptyMap()
  ) : ActionResult(ActionStatus.SUCCESS, responseMessage, data)

  data class Failure(
    val reason: String,
    val errorType: String = "GENERAL_ERROR"
  ) : ActionResult(ActionStatus.FAILED, reason, mapOf("error" to reason, "type" to errorType))

  data class NeedsPermission(
    val permission: String,
    val explanation: String,
    val pendingAction: ActionRequest? = null
  ) : ActionResult(
    ActionStatus.NEEDS_PERMISSION,
    explanation,
    mapOf("permission" to permission)
  )

  data class NeedsConfirmation(
    val title: String,
    val details: String,
    val actionToConfirm: ActionRequest,
    val confirmationDetails: ConfirmationDetails? = null
  ) : ActionResult(
    ActionStatus.NEEDS_CONFIRMATION,
    details,
    actionToConfirm.parameters
  )

  data class NeedsMoreInfo(
    val question: String,
    val missingFields: List<String>,
    val partialAction: ActionRequest? = null
  ) : ActionResult(
    ActionStatus.NEEDS_MORE_INFO,
    question,
    mapOf("missing" to missingFields.joinToString(","))
  )

  data class NeedsPicker(
    val prompt: String,
    val options: List<PickerOption>,
    val pendingAction: ActionRequest? = null
  ) : ActionResult(
    ActionStatus.NEEDS_PICKER,
    prompt,
    mapOf("options_count" to options.size.toString())
  )
}
