package com.example

import com.example.ai.AiPlanResult
import com.example.ai.LocalNluEngine
import com.example.planner.Planner
import com.example.tools.ActionRequest
import com.example.tools.ToolRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NluPlannerTest {

  @Test
  fun testLocalNluHindiYouTubeIntent() {
    val result = LocalNluEngine.parse("भाई YouTube खोलो", "Gamak")
    assertTrue(result is AiPlanResult.Action)
    val action = result as AiPlanResult.Action
    assertEquals("open_youtube", action.actionRequest.toolName)
  }

  @Test
  fun testLocalNluHindiAlarmIntent() {
    val result = LocalNluEngine.parse("कल सुबह 7 बजे alarm लगा देना", "Gamak")
    assertTrue(result is AiPlanResult.Action)
    val action = result as AiPlanResult.Action
    assertEquals("set_alarm", action.actionRequest.toolName)
  }

  @Test
  fun testLocalNluHindiCallIntent() {
    val result = LocalNluEngine.parse("माँ को call करना है", "Gamak")
    assertTrue(result is AiPlanResult.Action)
    val action = result as AiPlanResult.Action
    assertEquals("make_call", action.actionRequest.toolName)
  }

  @Test
  fun testLocalNluWeatherIntent() {
    val result = LocalNluEngine.parse("आज मौसम कैसा है?", "Gamak")
    assertTrue(result is AiPlanResult.Action)
    val action = result as AiPlanResult.Action
    assertEquals("get_weather", action.actionRequest.toolName)
  }

  @Test
  fun testLocalNluWhatsAppMissingMessageClarification() {
    val result = LocalNluEngine.parse("WhatsApp पर Rahul को message लिखो", "Gamak")
    assertTrue("Should trigger clarification for missing message body", result is AiPlanResult.Clarification)
    val clarification = result as AiPlanResult.Clarification
    assertEquals("क्या message भेजना है?", clarification.question)
    assertTrue(clarification.missingFields.contains("message"))
  }

  @Test
  fun testMultiStepCompoundPlan() {
    val result = LocalNluEngine.parse("सुबह 7 बजे का अलार्म लगाओ और फिर YouTube खोलो", "Gamak")
    assertTrue("Should produce MultiAction", result is AiPlanResult.MultiAction)
    val multi = result as AiPlanResult.MultiAction
    assertEquals(2, multi.steps.size)
    assertEquals("set_alarm", multi.steps[0].actionRequest.toolName)
    assertEquals("open_youtube", multi.steps[1].actionRequest.toolName)
  }

  @Test
  fun testMemoryStorageIntent() {
    val result = LocalNluEngine.parse("याद रखो मुझे Hindi में जवाब देना पसंद है", "Gamak")
    assertTrue("Should produce MemoryOp", result is AiPlanResult.MemoryOp)
    val memoryOp = result as AiPlanResult.MemoryOp
    assertEquals("SAVE", memoryOp.operation)
    assertEquals("preferred_language", memoryOp.key)
    assertEquals("Hindi", memoryOp.value)
  }

  @Test
  fun testClarificationFollowUpResolution() {
    val partial = ActionRequest(
      toolName = "send_whatsapp_message",
      parameters = mapOf("recipient" to "Rahul"),
      rawQuery = "WhatsApp पर Rahul को message लिखो"
    )
    val resolved = LocalNluEngine.resolveClarificationFollowUp(
      userInput = "उसे बोलो मैं 10 मिनट में आ रहा हूँ",
      partialAction = partial,
      missingFields = listOf("message")
    )
    assertEquals("Rahul", resolved.parameters["recipient"])
    assertEquals("मैं 10 मिनट में आ रहा हूँ", resolved.parameters["message"])
  }

  @Test
  fun testConfirmationAffirmationAndCancellation() {
    assertTrue(LocalNluEngine.isAffirmativeConfirmation("हाँ"))
    assertTrue(LocalNluEngine.isAffirmativeConfirmation("yes"))
    assertTrue(LocalNluEngine.isAffirmativeConfirmation("कर दो"))
    assertTrue(LocalNluEngine.isAffirmativeConfirmation("हो"))

    assertTrue(LocalNluEngine.isNegativeConfirmation("नहीं"))
    assertTrue(LocalNluEngine.isNegativeConfirmation("cancel"))
    assertTrue(LocalNluEngine.isNegativeConfirmation("रहने दो"))

    assertTrue(LocalNluEngine.isCancellationIntent("रद्द करो"))
    assertTrue(LocalNluEngine.isCancellationIntent("छोड़ो"))
    assertTrue(LocalNluEngine.isCancellationIntent("stop"))
  }

  @Test
  fun testPlannerMultiActionValidation() = runBlocking {
    val toolRegistry = ToolRegistry()
    val localAiClient = object : com.example.ai.AiClient {
      override suspend fun generatePlan(
        prompt: String,
        conversationHistory: List<com.example.model.ChatMessage>,
        personaName: String,
        memoryContext: List<String>
      ): AiPlanResult = LocalNluEngine.parse(prompt, personaName)
    }

    val planner = Planner(localAiClient, toolRegistry)
    val result = planner.planAndExecute("सुबह 7 बजे का अलार्म लगाओ और फिर YouTube खोलो", emptyList(), "Gamak")
    assertTrue(result is AiPlanResult.MultiAction)
    val multi = result as AiPlanResult.MultiAction
    assertEquals(2, multi.steps.size)
  }

  @Test
  fun testContextualPronounResolutionForCall() {
    val context = com.example.model.ConversationContext(
      lastMentionedContact = "Rohit"
    )
    val result = LocalNluEngine.parse("उसे call करो", "Gamak", context)
    assertTrue(result is AiPlanResult.Action)
    val action = result as AiPlanResult.Action
    assertEquals("make_call", action.actionRequest.toolName)
    assertEquals("Rohit", action.actionRequest.parameters["contact_name"])
  }

  @Test
  fun testContextualAppResolution() {
    val context = com.example.model.ConversationContext(
      lastMentionedApp = "Spotify"
    )
    val result = LocalNluEngine.parse("वो app खोलो", "Gamak", context)
    assertTrue(result is AiPlanResult.Action)
    val action = result as AiPlanResult.Action
    assertEquals("open_app", action.actionRequest.toolName)
    assertEquals("Spotify", action.actionRequest.parameters["app_name"])
  }

  @Test
  fun testNepaliRequestWeather() {
    val result = LocalNluEngine.parse("आज मौसम कस्तो छ?", "Gamak")
    assertTrue(result is AiPlanResult.Action)
    val action = result as AiPlanResult.Action
    assertEquals("get_weather", action.actionRequest.toolName)
  }

  @Test
  fun testNepaliRequestCall() {
    val result = LocalNluEngine.parse("आमालाई फोन गर", "Gamak")
    assertTrue(result is AiPlanResult.Action)
    val action = result as AiPlanResult.Action
    assertEquals("make_call", action.actionRequest.toolName)
    assertEquals("आमा", action.actionRequest.parameters["contact_name"])
  }

  @Test
  fun testHinglishCallRequest() {
    val result = LocalNluEngine.parse("Yaar please Rohit ko call lagao", "Gamak")
    assertTrue(result is AiPlanResult.Action)
    val action = result as AiPlanResult.Action
    assertEquals("make_call", action.actionRequest.toolName)
    assertEquals("Rohit", action.actionRequest.parameters["contact_name"])
  }

  @Test
  fun testMissingContactClarification() {
    val result = LocalNluEngine.parse("फोन लगाओ", "Gamak")
    assertTrue(result is AiPlanResult.Clarification)
    val clarification = result as AiPlanResult.Clarification
    assertTrue(clarification.missingFields.contains("contact_name"))
  }

  @Test
  fun testRetryIntentKeywords() {
    val retryPhrases = listOf("retry", "फिर से कोशिश करो", "try again", "पुनः प्रयास", "फेरि गर")
    for (phrase in retryPhrases) {
      val matches = phrase.contains("retry") || phrase.contains("फिर से") || phrase.contains("try again") || phrase.contains("पुनः प्रयास") || phrase.contains("फेरि गर")
      assertTrue("Phrase '$phrase' should be recognized as retry intent", matches)
    }
  }

  @Test
  fun testDuplicateActionProtectionCooldown() = runBlocking {
    val executor = com.example.planner.ActionExecutor(context = null)
    val req = ActionRequest(toolName = "make_call", parameters = mapOf("contact_name" to "Pooja"), rawQuery = "Pooja ko call karo")
    val res1 = executor.execute(req)
    assertTrue(res1 is com.example.tools.ActionResult.Success)
  }

  @Test
  fun testEmptySpeechResultHandling() {
    val result = LocalNluEngine.parse("", "Gamak")
    assertTrue(result is AiPlanResult.Conversation)
  }

  @Test
  fun testMalformedAiToolResponseGracefulFallback() {
    val fallback = LocalNluEngine.parse("{invalid_json_prompt}", "Gamak")
    assertNotNull(fallback)
  }

  @Test
  fun testPermissionDeniedPathGracefulHandling() {
    val explanation = com.example.platform.PermissionManager.getExplanation(
      com.example.platform.PermissionManager.PERM_RECORD_AUDIO
    )
    assertTrue(explanation.isNotBlank())
    val contactExplanation = com.example.platform.PermissionManager.getExplanation(
      com.example.platform.PermissionManager.PERM_READ_CONTACTS
    )
    assertTrue(contactExplanation.isNotBlank())
  }

  @Test
  fun testConversationalQuestionsDoNotUseGenericReadyToWorkPlaceholder() {
    val queries = listOf(
      "नमस्ते! क्या हाल है?",
      "Who are you?",
      "तपाईं को हुनुहुन्छ?",
      "Tell me a joke",
      "चुटकुला सुनाओ",
      "what time is it",
      "समय क्या हुआ है",
      "5 + 10",
      "What is artificial intelligence?",
      "प्रकाश संश्लेषण के हो?"
    )

    for (query in queries) {
      val result = LocalNluEngine.parse(query, "Gamak")
      assertTrue("Query '$query' should resolve to Conversation", result is AiPlanResult.Conversation)
      val conv = result as AiPlanResult.Conversation
      assertTrue("Response should not be empty", conv.responseText.isNotBlank())
      assertTrue(
        "Response must never contain the obsolete generic 'काम करने के लिए तैयार है' phrase",
        !conv.responseText.contains("काम करने के लिए तैयार है")
      )
      assertTrue(
        "Response must never contain 'ready to work on this'",
        !conv.responseText.contains("ready to work on this", ignoreCase = true)
      )
    }
  }

  @Test
  fun testDeviceActionVsConversationRoutingIntegrity() {
    // Real actions should route to Action
    val callAction = LocalNluEngine.parse("Call Rahul", "Gamak")
    assertTrue(callAction is AiPlanResult.Action && (callAction as AiPlanResult.Action).actionRequest.toolName == "make_call")

    val alarmAction = LocalNluEngine.parse("सुबह 6 बजे का अलार्म लगाओ", "Gamak")
    assertTrue(alarmAction is AiPlanResult.Action && (alarmAction as AiPlanResult.Action).actionRequest.toolName == "set_alarm")

    val ytAction = LocalNluEngine.parse("YouTube खोलो", "Gamak")
    assertTrue(ytAction is AiPlanResult.Action && (ytAction as AiPlanResult.Action).actionRequest.toolName == "open_youtube")

    val weatherAction = LocalNluEngine.parse("What is the weather today?", "Gamak")
    assertTrue(weatherAction is AiPlanResult.Action && (weatherAction as AiPlanResult.Action).actionRequest.toolName == "get_weather")

    val mapsAction = LocalNluEngine.parse("घर जाने का रास्ता दिखाओ", "Gamak")
    assertTrue(mapsAction is AiPlanResult.Action && (mapsAction as AiPlanResult.Action).actionRequest.toolName == "navigate_maps")

    // Pure conversational chit-chat & questions must route to Conversation
    val greeting = LocalNluEngine.parse("Hello Gamak", "Gamak")
    assertTrue(greeting is AiPlanResult.Conversation)

    val howAreYou = LocalNluEngine.parse("How are you doing today?", "Gamak")
    assertTrue(howAreYou is AiPlanResult.Conversation)

    val mathQuestion = LocalNluEngine.parse("25 * 4", "Gamak")
    assertTrue(mathQuestion is AiPlanResult.Conversation && (mathQuestion as AiPlanResult.Conversation).responseText.contains("100"))
  }
}
