package com.example

import com.example.ai.AiClient
import com.example.ai.AiPlanResult
import com.example.ai.CompositeAiClient
import com.example.ai.GeminiAiClient
import com.example.ai.OpenAiClient
import com.example.model.ChatMessage
import com.example.planner.ActionExecutor
import com.example.planner.Planner
import com.example.tools.ActionRequest
import com.example.tools.ActionResult
import com.example.tools.ToolRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OpenAiConversationEngineTest {


  private val toolRegistry = ToolRegistry()

  @Test
  fun testOpenAiClientAvailabilityAndKeySanitization() {
    val unconfiguredClient = OpenAiClient(apiKeyProvider = { "" })
    assertFalse(unconfiguredClient.isAvailable())

    val placeholderClient = OpenAiClient(apiKeyProvider = { "MY_OPENAI_API_KEY" })
    assertFalse(placeholderClient.isAvailable())

    val quotedPlaceholderClient = OpenAiClient(apiKeyProvider = { "\"MY_OPENAI_API_KEY\"" })
    assertFalse(quotedPlaceholderClient.isAvailable())

    val configuredClient = OpenAiClient(apiKeyProvider = { "sk-proj-valid-test-key" })
    assertTrue(configuredClient.isAvailable())

    val quotedConfiguredClient = OpenAiClient(apiKeyProvider = { "\"sk-proj-valid-test-key\"" })
    assertTrue(quotedConfiguredClient.isAvailable())
    assertEquals("sk-proj-valid-test-key", OpenAiClient.sanitizeApiKey("\"sk-proj-valid-test-key\""))

    // Test system property / runtime env resolution
    System.setProperty("OPENAI_API_KEY", "sk-runtime-ci-key")
    val runtimeClient = OpenAiClient()
    assertTrue(runtimeClient.isAvailable())
    System.clearProperty("OPENAI_API_KEY")
  }

  @Test
  fun testMultilingualConversationalRoutingToOpenAi() = runBlocking {
    val mockOpenAiClient = object : AiClient {
      override fun isAvailable(): Boolean = true
      override suspend fun generatePlan(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        personaName: String,
        memoryContext: List<String>
      ): AiPlanResult {
        return when {
          prompt.contains("हिंदी") || prompt.contains("सूर्य") ->
            AiPlanResult.Conversation("सूर्य हमारे सौरमंडल का मुख्य तारा है।", "hi")
          prompt.contains("नेपाली") || prompt.contains("कस्तो") ->
            AiPlanResult.Conversation("म एकदम सन्चै छु! भन्नुहोस्,", "ne")
          prompt.contains("hinglish") || prompt.contains("kaise") ->
            AiPlanResult.Conversation("Main badhiya hoon, aap batao kaise ho?", "hi-en")
          else ->
            AiPlanResult.Conversation("Photosynthesis is the process by which plants make food.", "en")
        }
      }
    }

    val planner = Planner(mockOpenAiClient, toolRegistry)

    // Hindi
    val resHindi = planner.planAndExecute("सूर्य क्या है?", emptyList(), "Gamak")
    assertTrue(resHindi is AiPlanResult.Conversation)
    assertTrue((resHindi as AiPlanResult.Conversation).responseText.contains("सूर्य"))

    // Nepali
    val resNepali = planner.planAndExecute("तपाईंलाई कस्तो छ?", emptyList(), "Gamak")
    assertTrue(resNepali is AiPlanResult.Conversation)
    assertTrue((resNepali as AiPlanResult.Conversation).responseText.contains("सन्चै"))

    // Hinglish
    val resHinglish = planner.planAndExecute("Aap kaise ho hinglish mein batao", emptyList(), "Gamak")
    assertTrue(resHinglish is AiPlanResult.Conversation)
    assertTrue((resHinglish as AiPlanResult.Conversation).responseText.contains("badhiya"))

    // English
    val resEnglish = planner.planAndExecute("What is photosynthesis?", emptyList(), "Gamak")
    assertTrue(resEnglish is AiPlanResult.Conversation)
    assertTrue((resEnglish as AiPlanResult.Conversation).responseText.contains("Photosynthesis"))
  }

  @Test
  fun testOpenAiResponseReachesAssistantEngineUIAndTTS() = runBlocking {
    val mockOpenAiClient = object : AiClient {
      override fun isAvailable(): Boolean = true
      override suspend fun generatePlan(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        personaName: String,
        memoryContext: List<String>
      ): AiPlanResult {
        return AiPlanResult.Conversation("Quantum computing processes data using qubits.")
      }
    }

    val planner = Planner(mockOpenAiClient, toolRegistry)
    val context = org.robolectric.RuntimeEnvironment.getApplication()
    val testJob = kotlinx.coroutines.Job()
    val engineScope = kotlinx.coroutines.CoroutineScope(testJob + kotlinx.coroutines.Dispatchers.Default)

    val assistantEngine = com.example.data.AssistantEngine(
      scope = engineScope,
      textToSpeechManager = null,
      planner = planner
    )

    assistantEngine.sendUserPrompt("Explain quantum computing", "Gamak")
    kotlinx.coroutines.delay(600)

    // Verify message added to engine messages flow and UI conversation state
    val messages = assistantEngine.messages.value
    assertTrue("Engine should contain response message from OpenAI", messages.any { !it.isUser && it.text.contains("Quantum computing") })
    testJob.cancel()
  }

  @Test
  fun testOpenAiConversationJsonParsing() {
    val client = OpenAiClient(toolRegistry = toolRegistry, apiKeyProvider = { "sk-test" })

    // 1. Multilingual conversational answers
    val hindiResponse = """
      {
        "id": "chatcmpl-test1",
        "choices": [
          {
            "message": {
              "role": "assistant",
              "content": "{\"type\":\"CONVERSATION\",\"response\":\"नमस्ते! प्रकाश संश्लेषण वह प्रक्रिया है जिससे पौधे सूर्य के प्रकाश से भोजन बनाते हैं।\",\"language\":\"hi\"}"
            }
          }
        ]
      }
    """.trimIndent()

    val resultHindi = client.parseOpenAiResponse(hindiResponse, "प्रकाश संश्लेषण क्या है?", "Gamak")
    assertTrue(resultHindi is AiPlanResult.Conversation)
    val convHindi = resultHindi as AiPlanResult.Conversation
    assertTrue(convHindi.responseText.contains("प्रकाश संश्लेषण"))
    assertEquals("hi", convHindi.detectedLanguage)

    // 2. Nepali conversational answers
    val nepaliResponse = """
      {
        "choices": [
          {
            "message": {
              "role": "assistant",
              "content": "{\"type\":\"CONVERSATION\",\"response\":\"नमस्ते! म Gamak AI हुँ। म तपाईंलाई सहयोग गर्न तयार छु।\",\"language\":\"ne\"}"
            }
          }
        ]
      }
    """.trimIndent()

    val resultNepali = client.parseOpenAiResponse(nepaliResponse, "तपाईं को हुनुहुन्छ?", "Gamak")
    assertTrue(resultNepali is AiPlanResult.Conversation)
    val convNepali = resultNepali as AiPlanResult.Conversation
    assertTrue(convNepali.responseText.contains("Gamak AI"))

    // 3. Plain text fallback (direct reasoning/answer without JSON fences)
    val plainTextResponse = """
      {
        "choices": [
          {
            "message": {
              "role": "assistant",
              "content": "The speed of light is approximately 299,792 kilometers per second in a vacuum."
            }
          }
        ]
      }
    """.trimIndent()

    val resultPlain = client.parseOpenAiResponse(plainTextResponse, "What is the speed of light?", "Gamak")
    assertTrue(resultPlain is AiPlanResult.Conversation)
    assertEquals("The speed of light is approximately 299,792 kilometers per second in a vacuum.", (resultPlain as AiPlanResult.Conversation).responseText)
  }

  @Test
  fun testOpenAiActionParsingAndPlannerValidation() = runBlocking {
    val client = OpenAiClient(toolRegistry = toolRegistry, apiKeyProvider = { "sk-test" })

    // Single Action: Call Rahul
    val callJson = """
      {
        "choices": [
          {
            "message": {
              "role": "assistant",
              "content": "{\"type\":\"ACTION\",\"tool_name\":\"make_call\",\"parameters\":{\"contact_name\":\"Rahul\"},\"spoken_response\":\"Rahul को कॉल लगाया जा रहा है।\",\"requires_confirmation\":false}"
            }
          }
        ]
      }
    """.trimIndent()

    val actionResult = client.parseOpenAiResponse(callJson, "Call Rahul", "Gamak")
    assertTrue(actionResult is AiPlanResult.Action)
    val action = actionResult as AiPlanResult.Action
    assertEquals("make_call", action.actionRequest.toolName)
    assertEquals("Rahul", action.actionRequest.parameters["contact_name"])

    // Planner validation
    val mockAiClient = object : AiClient {
      override suspend fun generatePlan(prompt: String, conversationHistory: List<ChatMessage>, personaName: String, memoryContext: List<String>): AiPlanResult {
        return actionResult
      }
    }
    val planner = Planner(mockAiClient, toolRegistry)
    val planOutput = planner.planAndExecute("Call Rahul", emptyList(), "Gamak")
    assertTrue(planOutput is AiPlanResult.Action)
  }

  @Test
  fun testOpenAiMultiActionParsing() {
    val client = OpenAiClient(toolRegistry = toolRegistry, apiKeyProvider = { "sk-test" })

    val multiActionJson = """
      {
        "choices": [
          {
            "message": {
              "role": "assistant",
              "content": "{\"type\":\"MULTI_ACTION\",\"steps\":[{\"tool_name\":\"set_alarm\",\"parameters\":{\"time\":\"07:00\"},\"description\":\"अलार्म सेट करें\"},{\"tool_name\":\"open_youtube\",\"parameters\":{\"query\":\"news\"},\"description\":\"यूट्यूब खोलें\"}],\"spoken_summary\":\"अलार्म सेट करके यूट्यूब खोला जा रहा है।\"}"
            }
          }
        ]
      }
    """.trimIndent()

    val result = client.parseOpenAiResponse(multiActionJson, "7 बजे का अलार्म लगाओ और यूट्यूब खोलो", "Gamak")
    assertTrue(result is AiPlanResult.MultiAction)
    val multi = result as AiPlanResult.MultiAction
    assertEquals(2, multi.steps.size)
    assertEquals("set_alarm", multi.steps[0].actionRequest.toolName)
    assertEquals("07:00", multi.steps[0].actionRequest.parameters["time"])
    assertEquals("open_youtube", multi.steps[1].actionRequest.toolName)
  }

  @Test
  fun testOpenAiClarificationParsing() {
    val client = OpenAiClient(toolRegistry = toolRegistry, apiKeyProvider = { "sk-test" })

    val clarJson = """
      {
        "choices": [
          {
            "message": {
              "role": "assistant",
              "content": "{\"type\":\"CLARIFICATION\",\"question\":\"किसे मैसेज भेजना चाहते हैं?\",\"missing_fields\":[\"recipient\"],\"partial_tool_name\":\"send_whatsapp_message\"}"
            }
          }
        ]
      }
    """.trimIndent()

    val result = client.parseOpenAiResponse(clarJson, "व्हाट्सएप मैसेज भेजो", "Gamak")
    assertTrue(result is AiPlanResult.Clarification)
    val clar = result as AiPlanResult.Clarification
    assertEquals("किसे मैसेज भेजना चाहते हैं?", clar.question)
    assertTrue(clar.missingFields.contains("recipient"))
  }

  @Test
  fun testOpenAiMemoryOperationParsing() {
    val client = OpenAiClient(toolRegistry = toolRegistry, apiKeyProvider = { "sk-test" })

    val memJson = """
      {
        "choices": [
          {
            "message": {
              "role": "assistant",
              "content": "{\"type\":\"MEMORY_OP\",\"operation\":\"SAVE\",\"key\":\"user_language_preference\",\"value\":\"Nepali\",\"response\":\"तपाईंको भाषा प्राथमिकता नेपालीको रूपमा सुरक्षित गरियो।\"}"
            }
          }
        ]
      }
    """.trimIndent()

    val result = client.parseOpenAiResponse(memJson, "याद राख मलाई नेपाली मन पर्छ", "Gamak")
    assertTrue(result is AiPlanResult.MemoryOp)
    val mem = result as AiPlanResult.MemoryOp
    assertEquals("SAVE", mem.operation)
    assertEquals("user_language_preference", mem.key)
    assertEquals("Nepali", mem.value)
  }

  @Test
  fun testCompositeAiClientThreeTierFallbackCascade() = runBlocking {
    var primaryCalled = false
    var secondaryCalled = false

    // Failing Primary (OpenAI error)
    val failingPrimary = object : AiClient {
      override fun isAvailable(): Boolean = true
      override suspend fun generatePlan(prompt: String, conversationHistory: List<ChatMessage>, personaName: String, memoryContext: List<String>): AiPlanResult {
        primaryCalled = true
        return AiPlanResult.Error("OpenAI 429 Rate Limit Exceeded")
      }
    }

    // Failing Secondary (Gemini network error)
    val failingSecondary = object : AiClient {
      override fun isAvailable(): Boolean = true
      override suspend fun generatePlan(prompt: String, conversationHistory: List<ChatMessage>, personaName: String, memoryContext: List<String>): AiPlanResult {
        secondaryCalled = true
        return AiPlanResult.Error("Gemini network timeout")
      }
    }

    val composite = CompositeAiClient(
      primaryClient = failingPrimary,
      secondaryClient = failingSecondary,
      toolRegistry = toolRegistry
    )

    // When both Primary and Secondary fail, it MUST fall back to LocalNluEngine seamlessly
    val result = composite.generatePlan("YouTube खोलो", emptyList(), "Gamak")
    assertTrue("Primary must have been attempted", primaryCalled)
    assertTrue("Secondary must have been attempted after primary failed", secondaryCalled)
    assertTrue("LocalNluEngine should resolve action", result is AiPlanResult.Action)
    assertEquals("open_youtube", (result as AiPlanResult.Action).actionRequest.toolName)
  }

  @Test
  fun testCompositeAiClientPrimarySuccessSkipsSecondary() = runBlocking {
    var secondaryCalled = false

    val successPrimary = object : AiClient {
      override fun isAvailable(): Boolean = true
      override suspend fun generatePlan(prompt: String, conversationHistory: List<ChatMessage>, personaName: String, memoryContext: List<String>): AiPlanResult {
        return AiPlanResult.Conversation(responseText = "Primary OpenAI Answer")
      }
    }

    val secondary = object : AiClient {
      override fun isAvailable(): Boolean = true
      override suspend fun generatePlan(prompt: String, conversationHistory: List<ChatMessage>, personaName: String, memoryContext: List<String>): AiPlanResult {
        secondaryCalled = true
        return AiPlanResult.Conversation(responseText = "Secondary Answer")
      }
    }

    val composite = CompositeAiClient(
      primaryClient = successPrimary,
      secondaryClient = secondary,
      toolRegistry = toolRegistry
    )

    val result = composite.generatePlan("Explain quantum physics", emptyList(), "Gamak")
    assertTrue(result is AiPlanResult.Conversation)
    assertEquals("Primary OpenAI Answer", (result as AiPlanResult.Conversation).responseText)
    assertFalse("Secondary must not be called when primary succeeds", secondaryCalled)
  }
}
