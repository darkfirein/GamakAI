package com.example.data

import android.util.Log
import com.example.ai.AiPlanResult
import com.example.ai.LocalNluEngine
import com.example.memory.MemoryRepository
import com.example.model.AssistantState
import com.example.model.ChatMessage
import com.example.planner.Planner
import com.example.tools.ActionRequest
import com.example.tools.ActionResult
import com.example.tools.ConfirmationDetails
import com.example.tools.PickerOption
import com.example.tools.TaskPlan
import com.example.tools.TaskStep
import com.example.tools.TaskStepStatus
import com.example.voice.AssistantAudioManager
import com.example.voice.SpeechRecognitionCallback
import com.example.voice.SpeechToTextManager
import com.example.voice.TextToSpeechManager
import com.example.voice.wakeword.WakeEngineStatus
import com.example.voice.wakeword.WakeWordEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AssistantEngine(
  private val scope: CoroutineScope,
  private val speechToTextManager: SpeechToTextManager? = null,
  private val textToSpeechManager: TextToSpeechManager? = null,
  private val planner: Planner? = null,
  private val memoryRepository: MemoryRepository? = null,
  private val wakeWordEngine: WakeWordEngine? = null,
  private val assistantAudioManager: AssistantAudioManager? = null
) {

  companion object {
    private const val TAG = "AssistantEngine"
  }

  private val _state = MutableStateFlow(AssistantState.IDLE)
  val state: StateFlow<AssistantState> = _state.asStateFlow()

  private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

  private val _audioLevels = MutableStateFlow(List(18) { 0.2f })
  val audioLevels: StateFlow<List<Float>> = _audioLevels.asStateFlow()

  private var activeJob: Job? = null
  private var audioSimulationJob: Job? = null

  // Conversational Context State
  private var conversationContext = com.example.model.ConversationContext()
  private var pendingAction: ActionRequest? = null
  private var pendingClarificationAction: ActionRequest? = null
  private var pendingMissingFields: List<String> = emptyList()
  private var activeTaskPlan: TaskPlan? = null
  private var lastFailedAction: ActionRequest? = null

  init {
    startWaveformSimulation()
  }

  fun initializeWithWelcome(assistantName: String) {
    if (_messages.value.isEmpty()) {
      _messages.value = listOf(
        ChatMessage(
          text = "Namaste! I am $assistantName, your personal AI voice & action assistant. Speak or type in Hindi, Nepali, Hinglish, or English!",
          isUser = false,
          stateTag = AssistantState.IDLE
        )
      )
    }
  }

  fun onMicTapped(assistantName: String, ttsSpeed: Float = 1.0f) {
    assistantAudioManager?.triggerTapHaptic()
    when (_state.value) {
      AssistantState.IDLE, AssistantState.ERROR -> {
        startRealSpeechListening(assistantName, ttsSpeed)
      }
      AssistantState.LISTENING -> {
        speechToTextManager?.stopListening()
      }
      AssistantState.SPEAKING, AssistantState.THINKING, AssistantState.CONFIRMING, AssistantState.CLARIFYING -> {
        interruptAndReset()
      }
    }
  }

  fun onWakeWordDetected(assistantName: String, ttsSpeed: Float = 1.0f) {
    Log.d(TAG, "Wake word detected -> triggering voice listening for $assistantName")
    startRealSpeechListening(assistantName, ttsSpeed)
  }

  private fun startRealSpeechListening(assistantName: String, ttsSpeed: Float) {
    interruptAndReset()
    wakeWordEngine?.pause(WakeEngineStatus.PAUSED_FOR_MIC)

    if (speechToTextManager == null) {
      simulateListeningFlow(assistantName, ttsSpeed)
      return
    }

    _state.value = AssistantState.LISTENING

    scope.launch {
      speechToTextManager.startListening(
        preferredLanguage = "hi-IN",
        callback = object : SpeechRecognitionCallback {
          override fun onReady() {
            _state.value = AssistantState.LISTENING
          }

          override fun onBeginningOfSpeech() {
            _state.value = AssistantState.LISTENING
          }

          override fun onRmsChanged(rmsDb: Float) {
            if (_state.value == AssistantState.LISTENING) {
              updateWaveformFromRms(rmsDb)
            }
          }

          override fun onPartialResult(partialText: String) {
            Log.d(TAG, "STT Partial result: $partialText")
          }

          override fun onFinalResult(finalText: String) {
            wakeWordEngine?.resume()
            if (finalText.isNotBlank()) {
              sendUserPrompt(finalText.trim(), assistantName, ttsSpeed = ttsSpeed)
            } else {
              _state.value = AssistantState.IDLE
            }
          }

          override fun onError(errorCode: Int, errorMessage: String) {
            wakeWordEngine?.resume()
            Log.w(TAG, "STT Error ($errorCode): $errorMessage")
            if (errorCode == 7 || errorCode == 6) { // ERROR_NO_MATCH or ERROR_SPEECH_TIMEOUT
              _state.value = AssistantState.IDLE
            } else {
              _state.value = AssistantState.ERROR
              _messages.value = _messages.value + ChatMessage(
                text = "⚠️ Mic error: $errorMessage. Please try again.",
                isUser = false,
                stateTag = AssistantState.ERROR
              )
            }
          }

          override fun onEndOfSpeech() {
            if (_state.value == AssistantState.LISTENING) {
              _state.value = AssistantState.THINKING
            }
          }
        }
      )
    }
  }

  fun sendUserPrompt(
    prompt: String,
    assistantName: String,
    ttsSpeed: Float = 1.0f,
    forceState: AssistantState? = null
  ) {
    if (prompt.isBlank()) return
    val cleanPrompt = prompt.trim()
    interruptAndReset()

    val userMsg = ChatMessage(text = cleanPrompt, isUser = true)
    _messages.value = _messages.value + userMsg

    activeJob = scope.launch {
      _state.value = AssistantState.THINKING

      if (forceState != null) {
        handleForcedDemoState(cleanPrompt, assistantName, forceState, ttsSpeed)
        return@launch
      }

      // 1. Check for cancellation intent ("रद्द करो", "cancel", "छोड़ो", "stop")
      if (LocalNluEngine.isCancellationIntent(cleanPrompt)) {
        cancelAllPendingTasks(assistantName, ttsSpeed)
        return@launch
      }

      // 2. Check for Confirmation response while in CONFIRMING state
      if (pendingAction != null) {
        if (LocalNluEngine.isAffirmativeConfirmation(cleanPrompt)) {
          confirmAction(true, assistantName, ttsSpeed)
          return@launch
        } else if (LocalNluEngine.isNegativeConfirmation(cleanPrompt)) {
          confirmAction(false, assistantName, ttsSpeed)
          return@launch
        } else {
          // New independent prompt spoken during confirmation; cancel pending action and proceed
          pendingAction = null
        }
      }

      // 3. Check for Clarification Follow-up response while in CLARIFYING state
      if (pendingClarificationAction != null) {
        val resolvedAction = LocalNluEngine.resolveClarificationFollowUp(
          cleanPrompt,
          pendingClarificationAction!!,
          pendingMissingFields
        )
        pendingClarificationAction = null
        pendingMissingFields = emptyList()

        handleActionExecutionFlow(
          actionRequest = resolvedAction,
          spokenConfirmation = "${resolvedAction.toolName.replace('_', ' ')} शुरू किया जा रहा है...",
          requiresConfirmation = isActionSensitive(resolvedAction.toolName),
          assistantName = assistantName,
          ttsSpeed = ttsSpeed
        )
        return@launch
      }

      // 4. Check for Retry intent
      if (containsAny(cleanPrompt.lowercase(), "retry", "फिर से", "try again", "पुनः प्रयास", "फेरि गर")) {
        val toRetry = lastFailedAction
        if (toRetry != null) {
          lastFailedAction = null
          handleActionExecutionFlow(
            actionRequest = toRetry,
            spokenConfirmation = "पुनः प्रयास किया जा रहा है...",
            requiresConfirmation = false,
            assistantName = assistantName,
            ttsSpeed = ttsSpeed
          )
          return@launch
        }
      }

      // 5. Standard Processing (Planner / Gemini with memory fallback)
      if (planner != null) {
        processPromptWithPlanner(cleanPrompt, assistantName, ttsSpeed)
      } else {
        processPromptLocally(cleanPrompt, assistantName, ttsSpeed)
      }
    }
  }

  private suspend fun processPromptWithPlanner(prompt: String, assistantName: String, ttsSpeed: Float) {
    try {
      // Gather long-term memories if available
      val memories = memoryRepository?.memoriesFlow?.firstOrNull()?.map { "${it.key}: ${it.value}" } ?: emptyList()

      val planResult = planner!!.planAndExecute(prompt, _messages.value, assistantName, memories)
      when (planResult) {
        is AiPlanResult.Conversation -> {
          val responseMsg = ChatMessage(
            text = planResult.responseText,
            isUser = false,
            stateTag = AssistantState.SPEAKING
          )
          _messages.value = _messages.value + responseMsg
          speakResponse(planResult.responseText, ttsSpeed)
        }

        is AiPlanResult.Clarification -> {
          _state.value = AssistantState.CLARIFYING
          pendingClarificationAction = planResult.partialActionRequest
          pendingMissingFields = planResult.missingFields

          val quickReplies = when {
            planResult.missingFields.contains("time") -> listOf("सुबह 7:00 बजे", "दोपहर 2:00 बजे", "शाम 6:00 बजे", "10 मिनट बाद")
            planResult.missingFields.contains("message") -> listOf("मैं 10 मिनट में आ रहा हूँ", "घर पहुँच कर बात करता हूँ", "Ok", "Call me back")
            else -> emptyList()
          }

          val clarMsg = ChatMessage(
            text = planResult.question,
            isUser = false,
            stateTag = AssistantState.CLARIFYING,
            pendingAction = planResult.partialActionRequest,
            quickReplies = quickReplies
          )
          _messages.value = _messages.value + clarMsg
          speakResponse(planResult.question, ttsSpeed, returnToIdleOnFinish = false)
        }

        is AiPlanResult.Action -> {
          handleActionExecutionFlow(
            actionRequest = planResult.actionRequest,
            spokenConfirmation = planResult.spokenConfirmation,
            requiresConfirmation = planResult.requiresConfirmation,
            assistantName = assistantName,
            ttsSpeed = ttsSpeed
          )
        }

        is AiPlanResult.MultiAction -> {
          handleMultiActionExecutionFlow(planResult.steps, planResult.spokenSummary, prompt, assistantName, ttsSpeed)
        }

        is AiPlanResult.MemoryOp -> {
          handleMemoryOperation(planResult, assistantName, ttsSpeed)
        }

        is AiPlanResult.Error -> {
          _state.value = AssistantState.ERROR
          _messages.value = _messages.value + ChatMessage(
            text = "⚠️ ${planResult.message}",
            isUser = false,
            stateTag = AssistantState.ERROR
          )
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error evaluating AI plan", e)
      processPromptLocally(prompt, assistantName, ttsSpeed)
    }
  }

  private suspend fun handleMultiActionExecutionFlow(
    steps: List<TaskStep>,
    spokenSummary: String,
    rawPrompt: String,
    assistantName: String,
    ttsSpeed: Float
  ) {
    val taskPlan = TaskPlan(
      rawQuery = rawPrompt,
      steps = steps,
      spokenSummary = spokenSummary
    )
    activeTaskPlan = taskPlan

    val planMsg = ChatMessage(
      text = "📋 [Multi-Step Plan]\n${spokenSummary.ifBlank { "${steps.size} कार्यों की योजना बनाई गई:" }}",
      isUser = false,
      stateTag = AssistantState.THINKING,
      taskPlan = taskPlan
    )
    _messages.value = _messages.value + planMsg

    // Execute steps sequentially
    var previousStepFailed = false
    val updatedSteps = steps.toMutableList()

    for ((index, step) in steps.withIndex()) {
      if (previousStepFailed && step.isDependent) {
        updatedSteps[index] = step.copy(
          status = TaskStepStatus.CANCELLED,
          resultMessage = "पिछला चरण विफल होने के कारण यह कार्य रद्द कर दिया गया।"
        )
        updateActiveTaskPlan(updatedSteps)
        continue
      }

      updatedSteps[index] = step.copy(status = TaskStepStatus.RUNNING)
      updateActiveTaskPlan(updatedSteps)

      delay(350)
      val result = planner!!.executeAction(step.actionRequest)

      when (result) {
        is ActionResult.Success -> {
          updatedSteps[index] = step.copy(
            status = TaskStepStatus.COMPLETED,
            resultMessage = result.responseMessage
          )
          updateActiveTaskPlan(updatedSteps)
        }
        is ActionResult.Failure -> {
          previousStepFailed = true
          lastFailedAction = step.actionRequest
          updatedSteps[index] = step.copy(
            status = TaskStepStatus.FAILED,
            resultMessage = result.reason
          )
          updateActiveTaskPlan(updatedSteps)
        }
        is ActionResult.NeedsPermission -> {
          updatedSteps[index] = step.copy(
            status = TaskStepStatus.WAITING_FOR_PERMISSION,
            resultMessage = result.explanation
          )
          updateActiveTaskPlan(updatedSteps)
          handleActionResult(result, step.actionRequest, assistantName, ttsSpeed)
          return
        }
        is ActionResult.NeedsPicker -> {
          updatedSteps[index] = step.copy(
            status = TaskStepStatus.WAITING_FOR_PICKER,
            resultMessage = result.prompt
          )
          updateActiveTaskPlan(updatedSteps)
          handleActionResult(result, step.actionRequest, assistantName, ttsSpeed)
          return
        }
        is ActionResult.NeedsConfirmation -> {
          updatedSteps[index] = step.copy(
            status = TaskStepStatus.WAITING_FOR_CONFIRMATION,
            resultMessage = result.details
          )
          updateActiveTaskPlan(updatedSteps)
          handleActionResult(result, step.actionRequest, assistantName, ttsSpeed)
          return
        }
        is ActionResult.NeedsMoreInfo -> {
          updatedSteps[index] = step.copy(
            status = TaskStepStatus.WAITING_FOR_CLARIFICATION,
            resultMessage = result.question
          )
          updateActiveTaskPlan(updatedSteps)
          handleActionResult(result, step.actionRequest, assistantName, ttsSpeed)
          return
        }
      }
    }

    val completedCount = updatedSteps.count { it.status == TaskStepStatus.COMPLETED }
    val finalResponseText = if (completedCount == updatedSteps.size) {
      "सभी कार्य सफलतापूर्वक पूर्ण कर दिए गए।"
    } else {
      "$completedCount / ${updatedSteps.size} कार्य पूर्ण हुए।"
    }

    _state.value = AssistantState.SPEAKING
    _messages.value = _messages.value + ChatMessage(
      text = "✅ $finalResponseText",
      isUser = false,
      stateTag = AssistantState.SPEAKING
    )
    speakResponse(finalResponseText, ttsSpeed, returnToIdleOnFinish = true)
  }

  private fun updateActiveTaskPlan(steps: List<TaskStep>) {
    val current = activeTaskPlan ?: return
    val updated = current.copy(
      steps = steps,
      isComplete = steps.all { it.status == TaskStepStatus.COMPLETED || it.status == TaskStepStatus.FAILED || it.status == TaskStepStatus.CANCELLED }
    )
    activeTaskPlan = updated

    // Update last message containing this task plan
    val lastIndex = _messages.value.indexOfLast { it.taskPlan?.id == current.id }
    if (lastIndex >= 0) {
      val mutable = _messages.value.toMutableList()
      mutable[lastIndex] = mutable[lastIndex].copy(taskPlan = updated)
      _messages.value = mutable
    }
  }

  private suspend fun handleMemoryOperation(
    op: AiPlanResult.MemoryOp,
    assistantName: String,
    ttsSpeed: Float
  ) {
    if (memoryRepository != null) {
      val saved = memoryRepository.saveMemory(op.key, op.value, "preference")
      val text = if (saved) op.responseText else "गोपनीयता नीति के तहत यह जानकारी सहेजी नहीं गई।"
      _messages.value = _messages.value + ChatMessage(
        text = "🧠 $text",
        isUser = false,
        stateTag = AssistantState.SPEAKING
      )
      speakResponse(text, ttsSpeed)
    } else {
      _messages.value = _messages.value + ChatMessage(
        text = op.responseText,
        isUser = false,
        stateTag = AssistantState.SPEAKING
      )
      speakResponse(op.responseText, ttsSpeed)
    }
  }

  private suspend fun handleActionExecutionFlow(
    actionRequest: ActionRequest,
    spokenConfirmation: String,
    requiresConfirmation: Boolean,
    assistantName: String,
    ttsSpeed: Float
  ) {
    val contact = actionRequest.parameters["contact_name"] ?: actionRequest.parameters["recipient"] ?: actionRequest.parameters["phone_number"]
    val app = actionRequest.parameters["app_name"]
    val time = actionRequest.parameters["time"]
    val location = actionRequest.parameters["location"] ?: actionRequest.parameters["destination"]

    conversationContext = conversationContext.copy(
      lastMentionedContact = contact?.ifBlank { null } ?: conversationContext.lastMentionedContact,
      lastMentionedApp = app?.ifBlank { null } ?: conversationContext.lastMentionedApp,
      lastMentionedTime = time?.ifBlank { null } ?: conversationContext.lastMentionedTime,
      lastMentionedLocation = location?.ifBlank { null } ?: conversationContext.lastMentionedLocation,
      lastActionTool = actionRequest.toolName,
      lastUpdatedTimestamp = System.currentTimeMillis()
    )

    if (requiresConfirmation) {
      pendingAction = actionRequest
      _state.value = AssistantState.CONFIRMING

      val details = createConfirmationDetails(actionRequest)
      val confirmMsg = ChatMessage(
        text = "⚡ [${actionRequest.toolName.uppercase()}]\n$spokenConfirmation\n\nक्या आप इसे आगे बढ़ाना चाहते हैं?",
        isUser = false,
        stateTag = AssistantState.CONFIRMING,
        pendingAction = actionRequest,
        confirmationDetails = details
      )
      _messages.value = _messages.value + confirmMsg
      speakResponse(spokenConfirmation, ttsSpeed, returnToIdleOnFinish = false)
      return
    }

    // Execute through planner's ActionExecutor
    val result = planner!!.executePlannedAction(AiPlanResult.Action(actionRequest, spokenConfirmation))
    handleActionResult(result, actionRequest, assistantName, ttsSpeed)
  }

  private fun createConfirmationDetails(request: ActionRequest): ConfirmationDetails {
    val title = when (request.toolName) {
      "make_call" -> "Phone Call"
      "send_sms" -> "Send SMS"
      "send_whatsapp_message" -> "WhatsApp Message"
      "create_calendar_event" -> "Calendar Event"
      else -> request.toolName.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    }
    val recipient = request.parameters["contact_name"] ?: request.parameters["recipient"] ?: request.parameters["phone_number"]
    val content = request.parameters["message"] ?: request.parameters["title"]
    return ConfirmationDetails(
      actionTitle = title,
      targetRecipient = recipient,
      contentSummary = content
    )
  }

  private fun handleActionResult(
    result: ActionResult,
    actionRequest: ActionRequest,
    assistantName: String,
    ttsSpeed: Float
  ) {
    when (result) {
      is ActionResult.Success -> {
        _state.value = AssistantState.SPEAKING
        val responseMsg = ChatMessage(
          text = "✅ ${result.responseMessage}",
          isUser = false,
          stateTag = AssistantState.SPEAKING,
          actionResult = result
        )
        _messages.value = _messages.value + responseMsg
        speakResponse(result.responseMessage, ttsSpeed, returnToIdleOnFinish = true)
      }
      is ActionResult.Failure -> {
        lastFailedAction = actionRequest
        _state.value = AssistantState.ERROR
        val errorMsg = ChatMessage(
          text = "❌ ${result.reason}",
          isUser = false,
          stateTag = AssistantState.ERROR,
          actionResult = result
        )
        _messages.value = _messages.value + errorMsg
        speakResponse(result.reason, ttsSpeed, returnToIdleOnFinish = true)
      }
      is ActionResult.NeedsPermission -> {
        _state.value = AssistantState.CONFIRMING
        val permMsg = ChatMessage(
          text = "🔒 [अनुमति आवश्यक / Permission Required]\n${result.explanation}",
          isUser = false,
          stateTag = AssistantState.CONFIRMING,
          actionResult = result,
          pendingAction = result.pendingAction ?: actionRequest,
          permissionRequired = result.permission
        )
        _messages.value = _messages.value + permMsg
        speakResponse(result.explanation, ttsSpeed, returnToIdleOnFinish = false)
      }
      is ActionResult.NeedsPicker -> {
        _state.value = AssistantState.CLARIFYING
        val pickerMsg = ChatMessage(
          text = "📋 ${result.prompt}",
          isUser = false,
          stateTag = AssistantState.CLARIFYING,
          actionResult = result,
          pendingAction = result.pendingAction ?: actionRequest,
          pickerOptions = result.options
        )
        _messages.value = _messages.value + pickerMsg
        speakResponse(result.prompt, ttsSpeed, returnToIdleOnFinish = false)
      }
      is ActionResult.NeedsConfirmation -> {
        pendingAction = result.actionToConfirm
        _state.value = AssistantState.CONFIRMING
        val confMsg = ChatMessage(
          text = "⚠️ [${result.title}]\n${result.details}",
          isUser = false,
          stateTag = AssistantState.CONFIRMING,
          actionResult = result,
          pendingAction = result.actionToConfirm,
          confirmationDetails = result.confirmationDetails ?: createConfirmationDetails(result.actionToConfirm)
        )
        _messages.value = _messages.value + confMsg
        speakResponse(result.details, ttsSpeed, returnToIdleOnFinish = false)
      }
      is ActionResult.NeedsMoreInfo -> {
        _state.value = AssistantState.CLARIFYING
        pendingClarificationAction = result.partialAction ?: actionRequest
        pendingMissingFields = result.missingFields
        val infoMsg = ChatMessage(
          text = "❓ ${result.question}",
          isUser = false,
          stateTag = AssistantState.CLARIFYING,
          actionResult = result,
          pendingAction = result.partialAction ?: actionRequest
        )
        _messages.value = _messages.value + infoMsg
        speakResponse(result.question, ttsSpeed, returnToIdleOnFinish = false)
      }
    }
  }

  fun onOptionSelected(option: PickerOption, assistantName: String, ttsSpeed: Float = 1.0f) {
    interruptAndReset()
    val phone = option.extra["phone_number"] ?: option.extra["recipient"] ?: option.subtitle
    val name = option.extra["contact_name"] ?: option.title

    activeJob = scope.launch {
      _state.value = AssistantState.THINKING
      delay(300)

      val lastMsg = _messages.value.lastOrNull { it.pendingAction != null }
      val originalAction = lastMsg?.pendingAction

      val nextAction = if (originalAction != null) {
        val updatedParams = originalAction.parameters.toMutableMap().apply {
          put("phone_number", phone)
          put("contact_name", name)
          put("recipient", phone)
        }
        originalAction.copy(parameters = updatedParams)
      } else {
        ActionRequest("make_call", mapOf("contact_name" to name, "phone_number" to phone))
      }

      val result = planner!!.executePlannedAction(AiPlanResult.Action(nextAction, "$name को चुना गया।"))
      handleActionResult(result, nextAction, assistantName, ttsSpeed)
    }
  }

  fun onClarificationChosen(reply: String, assistantName: String, ttsSpeed: Float = 1.0f) {
    sendUserPrompt(reply, assistantName, ttsSpeed)
  }

  fun confirmAction(confirmed: Boolean, assistantName: String, ttsSpeed: Float = 1.0f) {
    interruptAndReset()
    val actionToRun = pendingAction
    pendingAction = null

    activeJob = scope.launch {
      _state.value = AssistantState.THINKING
      delay(250)

      if (confirmed && actionToRun != null && planner != null) {
        val result = planner.executePlannedAction(AiPlanResult.Action(actionToRun, "Confirmed"))
        handleActionResult(result, actionToRun, assistantName, ttsSpeed)
      } else {
        val reply = if (confirmed) {
          "पुष्टि की गई। $assistantName ने कार्य शुरू किया।"
        } else {
          "कार्य रद्द कर दिया गया।"
        }
        _messages.value = _messages.value + ChatMessage(text = reply, isUser = false, stateTag = AssistantState.SPEAKING)
        speakResponse(reply, ttsSpeed)
      }
    }
  }

  private fun cancelAllPendingTasks(assistantName: String, ttsSpeed: Float) {
    pendingAction = null
    pendingClarificationAction = null
    pendingMissingFields = emptyList()
    activeTaskPlan = null

    val reply = "कार्य रद्द कर दिया गया।"
    _messages.value = _messages.value + ChatMessage(text = reply, isUser = false, stateTag = AssistantState.SPEAKING)
    speakResponse(reply, ttsSpeed, returnToIdleOnFinish = true)
  }

  private suspend fun processPromptLocally(prompt: String, assistantName: String, ttsSpeed: Float) {
    delay(400)
    val localResult = LocalNluEngine.parse(prompt, assistantName, conversationContext)
    when (localResult) {
      is AiPlanResult.Action -> {
        handleActionExecutionFlow(localResult.actionRequest, localResult.spokenConfirmation, localResult.requiresConfirmation, assistantName, ttsSpeed)
      }
      is AiPlanResult.MultiAction -> {
        handleMultiActionExecutionFlow(localResult.steps, localResult.spokenSummary, prompt, assistantName, ttsSpeed)
      }
      is AiPlanResult.MemoryOp -> {
        handleMemoryOperation(localResult, assistantName, ttsSpeed)
      }
      is AiPlanResult.Clarification -> {
        _state.value = AssistantState.CLARIFYING
        pendingClarificationAction = localResult.partialActionRequest
        pendingMissingFields = localResult.missingFields
        val clarMsg = ChatMessage(
          text = localResult.question,
          isUser = false,
          stateTag = AssistantState.CLARIFYING,
          pendingAction = localResult.partialActionRequest
        )
        _messages.value = _messages.value + clarMsg
        speakResponse(localResult.question, ttsSpeed, returnToIdleOnFinish = false)
      }
      is AiPlanResult.Conversation -> {
        _messages.value = _messages.value + ChatMessage(
          text = localResult.responseText,
          isUser = false,
          stateTag = AssistantState.SPEAKING
        )
        speakResponse(localResult.responseText, ttsSpeed)
      }
      is AiPlanResult.Error -> {
        _state.value = AssistantState.ERROR
        _messages.value = _messages.value + ChatMessage(
          text = "⚠️ ${localResult.message}",
          isUser = false,
          stateTag = AssistantState.ERROR
        )
      }
    }
  }

  private fun speakResponse(text: String, ttsSpeed: Float, returnToIdleOnFinish: Boolean = true) {
    assistantAudioManager?.requestAudioFocus()
    wakeWordEngine?.pause(WakeEngineStatus.PAUSED_FOR_TTS)

    if (textToSpeechManager != null) {
      _state.value = AssistantState.SPEAKING
      textToSpeechManager.speak(
        text = text,
        speechRate = ttsSpeed,
        onStart = { _state.value = AssistantState.SPEAKING },
        onDone = {
          assistantAudioManager?.abandonAudioFocus()
          wakeWordEngine?.resume()
          if (returnToIdleOnFinish && _state.value == AssistantState.SPEAKING) {
            _state.value = AssistantState.IDLE
          }
        },
        onError = {
          assistantAudioManager?.abandonAudioFocus()
          wakeWordEngine?.resume()
          if (returnToIdleOnFinish && _state.value == AssistantState.SPEAKING) {
            _state.value = AssistantState.IDLE
          }
        }
      )
    } else {
      _state.value = AssistantState.SPEAKING
      scope.launch {
        delay((text.length * 30L).coerceIn(800L, 2500L))
        assistantAudioManager?.abandonAudioFocus()
        wakeWordEngine?.resume()
        if (returnToIdleOnFinish && _state.value == AssistantState.SPEAKING) {
          _state.value = AssistantState.IDLE
        }
      }
    }
  }

  fun triggerErrorSimulation(assistantName: String) {
    interruptAndReset()
    activeJob = scope.launch {
      _state.value = AssistantState.THINKING
      delay(400)
      _state.value = AssistantState.ERROR
      _messages.value = _messages.value + ChatMessage(
        text = "⚠️ [Gamak Engine Alert] Connection timeout encountered. Tap the microphone to retry.",
        isUser = false,
        stateTag = AssistantState.ERROR
      )
    }
  }

  fun clearChat(assistantName: String) {
    interruptAndReset()
    pendingAction = null
    pendingClarificationAction = null
    pendingMissingFields = emptyList()
    activeTaskPlan = null
    lastFailedAction = null

    _messages.value = listOf(
      ChatMessage(
        text = "Session refreshed. $assistantName is listening and ready for your instruction.",
        isUser = false,
        stateTag = AssistantState.IDLE
      )
    )
    _state.value = AssistantState.IDLE
  }

  fun setStateExplicitly(newState: AssistantState) {
    interruptAndReset()
    _state.value = newState
  }

  private fun interruptAndReset() {
    assistantAudioManager?.abandonAudioFocus()
    speechToTextManager?.cancel()
    textToSpeechManager?.stop()
    wakeWordEngine?.resume()
    activeJob?.cancel()
    activeJob = null
  }

  private fun simulateListeningFlow(assistantName: String, ttsSpeed: Float) {
    _state.value = AssistantState.LISTENING
    activeJob = scope.launch {
      delay(2500)
      sendUserPrompt("YouTube खोलो", assistantName, ttsSpeed)
    }
  }

  private suspend fun handleForcedDemoState(
    prompt: String,
    assistantName: String,
    forceState: AssistantState,
    ttsSpeed: Float
  ) {
    delay(400)
    when (forceState) {
      AssistantState.CONFIRMING -> {
        _state.value = AssistantState.CONFIRMING
        val response = ChatMessage(
          text = "⚠️ Confirmation required for '$prompt'. Are you sure you want $assistantName to execute this operation across your system?",
          isUser = false,
          stateTag = AssistantState.CONFIRMING
        )
        _messages.value = _messages.value + response
        speakResponse("Confirmation required for $prompt. Would you like me to proceed?", ttsSpeed, returnToIdleOnFinish = false)
      }
      AssistantState.CLARIFYING -> {
        _state.value = AssistantState.CLARIFYING
        val response = ChatMessage(
          text = "❓ Before I proceed with '$prompt', could you specify the preferred time, details, or contact?",
          isUser = false,
          stateTag = AssistantState.CLARIFYING
        )
        _messages.value = _messages.value + response
        speakResponse("Before I proceed with $prompt, could you specify additional details?", ttsSpeed, returnToIdleOnFinish = false)
      }
      else -> {
        processPromptLocally(prompt, assistantName, ttsSpeed)
      }
    }
  }

  private fun isActionSensitive(toolName: String): Boolean {
    return toolName == "make_call" || toolName == "send_sms" || toolName == "send_whatsapp_message"
  }

  private fun containsAny(text: String, vararg keywords: String): Boolean {
    return keywords.any { text.contains(it, ignoreCase = true) }
  }

  private fun updateWaveformFromRms(rmsdB: Float) {
    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.15f, 1.0f)
    val newLevels = List(18) { index ->
      val variation = kotlin.math.sin(index * 0.7f).toFloat() * 0.25f
      (normalized + variation).coerceIn(0.1f, 1.0f)
    }
    _audioLevels.value = newLevels
  }

  private fun startWaveformSimulation() {
    audioSimulationJob?.cancel()
    audioSimulationJob = scope.launch {
      var step = 0.0
      while (true) {
        val currentState = _state.value
        if (currentState != AssistantState.LISTENING) {
          val multiplier = when (currentState) {
            AssistantState.SPEAKING -> 0.85f
            AssistantState.THINKING -> 0.45f
            AssistantState.CONFIRMING, AssistantState.CLARIFYING -> 0.5f
            AssistantState.ERROR -> 0.15f
            AssistantState.IDLE -> 0.18f
            AssistantState.LISTENING -> 0.9f
          }

          val newLevels = List(18) { index ->
            val wave = kotlin.math.sin(step + index * 0.45).toFloat()
            val secondary = kotlin.math.cos(step * 1.3 + index * 0.3).toFloat()
            val combined = ((wave + secondary + 2f) / 4f) * multiplier
            combined.coerceIn(0.08f, 1.0f)
          }

          _audioLevels.value = newLevels
        }
        step += 0.25
        delay(45)
      }
    }
  }
}
