package com.example.model

enum class AssistantPersona(
  val defaultName: String,
  val tagline: String,
  val description: String
) {
  GAMAK(
    defaultName = "Gamak",
    tagline = "Original Core AI",
    description = "Balanced, analytical, and futuristic intelligence core."
  ),
  MAYA(
    defaultName = "Maya",
    tagline = "Intuitive & Creative",
    description = "Empathic conversationalist focused on clarity and brainstorming."
  ),
  VIKRAM(
    defaultName = "Vikram",
    tagline = "Command & Action",
    description = "Direct, efficient, and action-driven executive assistant."
  ),
  SATHI(
    defaultName = "Sathi",
    tagline = "Your Loyal Companion",
    description = "Supportive, warm, and proactive daily assistant."
  ),
  MITRA(
    defaultName = "Mitra",
    tagline = "Wise Mentor & Advisor",
    description = "Knowledgeable guide designed for productivity and reasoning."
  ),
  RIYA(
    defaultName = "Riya",
    tagline = "Vibrant & Expressive",
    description = "Dynamic and cheerful assistant for daily communication and entertainment."
  ),
  CUSTOM(
    defaultName = "Custom",
    tagline = "Personalized Identity",
    description = "Configure any custom assistant name of your choice."
  );

  companion object {
    fun fromName(name: String): AssistantPersona {
      return entries.firstOrNull { it.defaultName.equals(name, ignoreCase = true) } ?: CUSTOM
    }
  }
}
