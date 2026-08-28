package com.example.model

import com.example.tools.ActionRequest
import com.example.tools.ActionResult
import com.example.tools.ConfirmationDetails
import com.example.tools.PickerOption
import com.example.tools.TaskPlan
import java.util.UUID

data class ChatMessage(
  val id: String = UUID.randomUUID().toString(),
  val text: String,
  val isUser: Boolean,
  val timestamp: Long = System.currentTimeMillis(),
  val stateTag: AssistantState? = null,
  val actionResult: ActionResult? = null,
  val pendingAction: ActionRequest? = null,
  val taskPlan: TaskPlan? = null,
  val confirmationDetails: ConfirmationDetails? = null,
  val pickerOptions: List<PickerOption> = emptyList(),
  val permissionRequired: String? = null,
  val quickReplies: List<String> = emptyList()
)
