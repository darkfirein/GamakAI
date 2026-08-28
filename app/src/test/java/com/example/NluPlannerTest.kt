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
}
