package com.example

import com.example.ai.AiPlanResult
import com.example.ai.GeminiAiClient
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
class GeminiConversationEngineTest {

  private val toolRegistry = ToolRegistry()

  @Test
  fun testGeminiClientAvailabilityCheck() {
    val unconfiguredClient = GeminiAiClient(apiKeyProvider = { "" })
    assertFalse(unconfiguredClient.isAvailable())

    val placeholderClient = GeminiAiClient(apiKeyProvider = { "MY_GEMINI_API_KEY" })
    assertFalse(placeholderClient.isAvailable())

    val configuredClient = GeminiAiClient(apiKeyProvider = { "AIzaSyValidTestKey" })
    assertTrue(configuredClient.isAvailable())
  }

  @Test
  fun testGeminiResponseParsingConversation() {
    val client = GeminiAiClient(toolRegistry = toolRegistry, apiKeyProvider = { "AIzaSyTest" })

    val geminiResponse = """
      {
        "candidates": [
          {
            "content": {
              "parts": [
                {
                  "text": "{\"type\":\"CONVERSATION\",\"response\":\"नमस्ते! म Gamak AI हुँ।\",\"language\":\"ne\"}"
                }
              ]
            }
          }
        ]
      }
    """.trimIndent()

    val result = client.parseGeminiResponse(geminiResponse, "तपाईं को हुनुहुन्छ?", "Gamak")
    assertTrue(result is AiPlanResult.Conversation)
    val conv = result as AiPlanResult.Conversation
    assertTrue(conv.responseText.contains("Gamak AI"))
    assertEquals("ne", conv.detectedLanguage)
  }

  @Test
  fun testGeminiResponseParsingAction() {
    val client = GeminiAiClient(toolRegistry = toolRegistry, apiKeyProvider = { "AIzaSyTest" })

    val geminiResponse = """
      {
        "candidates": [
          {
            "content": {
              "parts": [
                {
                  "text": "{\"type\":\"ACTION\",\"tool_name\":\"make_call\",\"parameters\":{\"contact_name\":\"Mom\"},\"spoken_response\":\"Calling Mom...\",\"requires_confirmation\":true}"
                }
              ]
            }
          }
        ]
      }
    """.trimIndent()

    val result = client.parseGeminiResponse(geminiResponse, "Call Mom", "Gamak")
    assertTrue(result is AiPlanResult.Action)
    val action = result as AiPlanResult.Action
    assertEquals("make_call", action.actionRequest.toolName)
    assertEquals("Mom", action.actionRequest.parameters["contact_name"])
    assertTrue(action.requiresConfirmation)
  }

  @Test
  fun testGeminiResponseParsingMultiAction() {
    val client = GeminiAiClient(toolRegistry = toolRegistry, apiKeyProvider = { "AIzaSyTest" })

    val geminiResponse = """
      {
        "candidates": [
          {
            "content": {
              "parts": [
                {
                  "text": "{\"type\":\"MULTI_ACTION\",\"steps\":[{\"tool_name\":\"set_alarm\",\"parameters\":{\"time\":\"07:00\"}},{\"tool_name\":\"open_youtube\",\"parameters\":{\"query\":\"news\"}}],\"spoken_summary\":\"Setting alarm and opening YouTube.\"}"
                }
              ]
            }
          }
        ]
      }
    """.trimIndent()

    val result = client.parseGeminiResponse(geminiResponse, "Set alarm for 7am and open youtube news", "Gamak")
    assertTrue(result is AiPlanResult.MultiAction)
    val multi = result as AiPlanResult.MultiAction
    assertEquals(2, multi.steps.size)
    assertEquals("set_alarm", multi.steps[0].actionRequest.toolName)
    assertEquals("open_youtube", multi.steps[1].actionRequest.toolName)
  }

  @Test
  fun testGeminiResponseParsingClarification() {
    val client = GeminiAiClient(toolRegistry = toolRegistry, apiKeyProvider = { "AIzaSyTest" })

    val geminiResponse = """
      {
        "candidates": [
          {
            "content": {
              "parts": [
                {
                  "text": "{\"type\":\"CLARIFICATION\",\"question\":\"Who do you want to message?\",\"missing_fields\":[\"recipient\"],\"partial_tool_name\":\"send_sms\"}"
                }
              ]
            }
          }
        ]
      }
    """.trimIndent()

    val result = client.parseGeminiResponse(geminiResponse, "Send SMS", "Gamak")
    assertTrue(result is AiPlanResult.Clarification)
    val clar = result as AiPlanResult.Clarification
    assertEquals("Who do you want to message?", clar.question)
    assertTrue(clar.missingFields.contains("recipient"))
  }

  @Test
  fun testGeminiResponseParsingMemoryOp() {
    val client = GeminiAiClient(toolRegistry = toolRegistry, apiKeyProvider = { "AIzaSyTest" })

    val geminiResponse = """
      {
        "candidates": [
          {
            "content": {
              "parts": [
                {
                  "text": "{\"type\":\"MEMORY_OP\",\"operation\":\"SAVE\",\"key\":\"preferred_language\",\"value\":\"Hindi\",\"response\":\"याद रख लिया गया।\"}"
                }
              ]
            }
          }
        ]
      }
    """.trimIndent()

    val result = client.parseGeminiResponse(geminiResponse, "याद रखो मुझे Hindi पसंद है", "Gamak")
    assertTrue(result is AiPlanResult.MemoryOp)
    val mem = result as AiPlanResult.MemoryOp
    assertEquals("SAVE", mem.operation)
    assertEquals("preferred_language", mem.key)
    assertEquals("Hindi", mem.value)
  }

  @Test
  fun testGeminiUnconfiguredFallbackToLocalNlu() = runBlocking {
    val unconfiguredClient = GeminiAiClient(apiKeyProvider = { "" })
    val result = unconfiguredClient.generatePlan("YouTube खोलो", emptyList(), "Gamak")
    assertTrue(result is AiPlanResult.Action)
    assertEquals("open_youtube", (result as AiPlanResult.Action).actionRequest.toolName)
  }
}
