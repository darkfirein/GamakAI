package com.example.model

import com.example.tools.ActionRequest

data class ConversationContext(
  val lastMentionedContact: String? = null,
  val lastMentionedApp: String? = null,
  val lastMentionedTime: String? = null,
  val lastMentionedLocation: String? = null,
  val lastActionTool: String? = null,
  val pendingActionRequest: ActionRequest? = null,
  val pendingMissingFields: List<String> = emptyList(),
  val lastUpdatedTimestamp: Long = System.currentTimeMillis()
) {
  companion object {
    const val DEFAULT_CONTEXT_EXPIRATION_MS = 5 * 60 * 1000L // 5 minutes validity
  }

  fun isExpired(timeoutMs: Long = DEFAULT_CONTEXT_EXPIRATION_MS): Boolean {
    return (System.currentTimeMillis() - lastUpdatedTimestamp) > timeoutMs
  }

  fun resolvePronounForContact(text: String): String? {
    if (isExpired() || lastMentionedContact.isNullOrBlank()) return null
    val lower = text.lowercase()
    val pronounKeywords = listOf(
      "उसे", "उसको", "उन्हें", "उनको", "उसपे", "उसने",
      "उसलाई", "उहाँलाई", "उनीलाई",
      "him", "her", "them"
    )
    val hasPronoun = pronounKeywords.any { lower.contains(it) }
    return if (hasPronoun) lastMentionedContact else null
  }

  fun resolveAppReference(text: String): String? {
    if (isExpired() || lastMentionedApp.isNullOrBlank()) return null
    val lower = text.lowercase()
    val appReferenceKeywords = listOf(
      "वो app", "वो वाला app", "वही app", "त्यो app", "त्यही app",
      "that app", "previous app", "same app", "it", "वो", "त्यो"
    )
    val hasAppRef = appReferenceKeywords.any { lower.contains(it) }
    return if (hasAppRef) lastMentionedApp else null
  }
}
