package com.example.ai

import com.example.model.ConversationContext
import com.example.tools.ActionRequest
import com.example.tools.TaskStep
import com.example.tools.TaskStepStatus

object LocalNluEngine {

  fun parse(
    prompt: String,
    personaName: String,
    context: ConversationContext? = null
  ): AiPlanResult {
    val clean = prompt.trim()
    val lower = clean.lowercase()

    // 0. Cancellation check
    if (isCancellationIntent(clean)) {
      return AiPlanResult.Conversation(
        responseText = "कार्य रद्द कर दिया गया।"
      )
    }

    // 0.1 Memory save check ("याद रखो मुझे ... पसंद है", "मेरी भाषा Hindi रखो", etc.)
    if (containsAny(lower, "याद रखो", "याद रख", "remember that", "remember", "याद राख", "मेमोरी")) {
      val (key, value) = extractMemoryKeyValue(clean)
      if (key.isNotBlank() && value.isNotBlank()) {
        return AiPlanResult.MemoryOp(
          operation = "SAVE",
          key = key,
          value = value,
          responseText = "मैंने याद रख लिया कि $key: $value"
        )
      }
    }

    // 0.2 Multi-Step Sequential Intent Detection (Chained commands)
    // Matches: "माँ को call करो, फिर YouTube खोलो और उसके बाद 30 मिनट का timer लगाओ"
    val sequentialRegex = Regex(
      """(?:\s*,\s*फिर\s*|\s*और\s+फिर\s*|\s*और\s+उसके\s+बाद\s*|\s*उसके\s+बाद\s*|\s*and\s+then\s*|\s*after\s+that\s*|\s*tyaspachhi\s*|\s*त्यसपछि\s*|\s*अनि\s*|\s*र\s+त्यसपछि\s*|\s*,\s*and\s+|\s*,\s*और\s+|\s+and\s+|\s+और\s+)""",
      RegexOption.IGNORE_CASE
    )

    val parts = clean.split(sequentialRegex)
      .map { it.trim().trim(',', '.', ';') }
      .filter { it.isNotBlank() }

    if (parts.size >= 2) {
      val steps = mutableListOf<TaskStep>()
      for ((index, part) in parts.withIndex()) {
        val singlePlan = parseSingleIntent(part, personaName, context)
        if (singlePlan is AiPlanResult.Action) {
          steps.add(
            TaskStep(
              actionRequest = singlePlan.actionRequest,
              description = singlePlan.spokenConfirmation,
              requiresConfirmation = singlePlan.requiresConfirmation,
              isDependent = index > 0,
              status = TaskStepStatus.PENDING
            )
          )
        }
      }

      if (steps.size >= 2) {
        val countStr = when (steps.size) {
          2 -> "दोनों"
          3 -> "तीनों"
          else -> "${steps.size}"
        }
        val summary = "मैंने आपके $countStr कार्यों की योजना बना ली है। एक-एक करके शुरू कर रहा हूँ।"
        return AiPlanResult.MultiAction(steps = steps, spokenSummary = summary)
      }
    }

    // Single intent parsing
    return parseSingleIntent(clean, personaName, context)
  }

  fun parseSingleIntent(
    clean: String,
    personaName: String,
    context: ConversationContext? = null
  ): AiPlanResult {
    val lower = clean.lowercase()

    // 1. Camera Intent
    if (containsAny(lower, "camera", "कैमरा", "क्यामेरा") ||
      (containsAny(lower, "photo", "फोटो", "तस्वीर") && containsAny(lower, "खींचो", "लो", "खिच", "take", "click"))
    ) {
      val isVideo = containsAny(lower, "video", "विडियो", "भिडियो")
      return AiPlanResult.Action(
        actionRequest = ActionRequest(
          toolName = "open_camera",
          parameters = mapOf("mode" to if (isVideo) "video" else "photo"),
          rawQuery = clean
        ),
        spokenConfirmation = "Camera खोला जा रहा है..."
      )
    }

    // 2. Gallery / Photos Intent
    if (containsAny(lower, "gallery", "गैलरी", "ग्यालरी", "photos", "तस्वीरहरू") &&
      containsAny(lower, "खोलो", "open", "दिखाओ", "hernu", "show", "kholo")
    ) {
      return AiPlanResult.Action(
        actionRequest = ActionRequest(
          toolName = "open_gallery",
          rawQuery = clean
        ),
        spokenConfirmation = "Gallery खोली जा रही है..."
      )
    }

    // 3. Settings Intent
    if (containsAny(lower, "settings", "सेटिंग्स", "सेटिङ्ग", "setting")) {
      val settingType = when {
        containsAny(lower, "wifi", "वाईफाई", "wi-fi") -> "wifi"
        containsAny(lower, "bluetooth", "ब्लूटूथ") -> "bluetooth"
        containsAny(lower, "location", "लोकेशन", "स्थान") -> "location"
        containsAny(lower, "sound", "volume", "आवाज") -> "sound"
        containsAny(lower, "display", "brightness") -> "display"
        containsAny(lower, "app", "application") -> "app"
        else -> "main"
      }
      return AiPlanResult.Action(
        actionRequest = ActionRequest(
          toolName = "open_settings",
          parameters = mapOf("setting_type" to settingType),
          rawQuery = clean
        ),
        spokenConfirmation = "$settingType settings खोली जा रही है..."
      )
    }

    // 4. WhatsApp Intent (App open or message)
    if (containsAny(lower, "whatsapp", "व्हाट्सएप", "ह्वाट्सएप")) {
      if (containsAny(lower, "खोलो", "open", "launch", "kholo", "start", "खोल दे", "खोल") &&
        !containsAny(lower, "message", "मैसेज", "लिखो", "भेजो", "send", "bolo", "बोलो")
      ) {
        return AiPlanResult.Action(
          actionRequest = ActionRequest(toolName = "open_whatsapp", rawQuery = clean),
          spokenConfirmation = "WhatsApp खोला जा रहा है..."
        )
      }

      var recipient = extractRecipient(clean)
      if (recipient == null && context != null && !context.isExpired()) {
        recipient = context.resolvePronounForContact(clean)
      }
      val messageBody = extractMessageBody(clean)

      return if (recipient.isNullOrBlank()) {
        AiPlanResult.Clarification(
          question = "WhatsApp पर किसे संदेश भेजना चाहते हैं?",
          missingFields = listOf("recipient")
        )
      } else if (messageBody.isNullOrBlank()) {
        AiPlanResult.Clarification(
          question = "क्या message भेजना है?",
          missingFields = listOf("message"),
          partialActionRequest = ActionRequest(
            toolName = "send_whatsapp_message",
            parameters = mapOf("recipient" to recipient),
            rawQuery = clean
          )
        )
      } else {
        AiPlanResult.Action(
          actionRequest = ActionRequest(
            toolName = "send_whatsapp_message",
            parameters = mapOf("recipient" to recipient, "message" to messageBody),
            rawQuery = clean
          ),
          spokenConfirmation = "$recipient को WhatsApp पर संदेश तैयार है: \"$messageBody\"",
          requiresConfirmation = true
        )
      }
    }

    // 5. SMS / Messaging Intent
    if (containsAny(lower, "sms", "text message", "टेक्स्ट") ||
      (containsAny(lower, "message", "मैसेज", "सन्देश") && containsAny(lower, "भेजो", "लिखो", "पठाऊ", "send", "write", "करो", "दे", "gar", "bhejo")) ||
      lower.startsWith("message ")
    ) {
      var recipient = extractRecipient(clean)
      if (recipient == null && context != null && !context.isExpired()) {
        recipient = context.resolvePronounForContact(clean)
      }
      val messageBody = extractMessageBody(clean)

      return if (recipient.isNullOrBlank()) {
        AiPlanResult.Clarification(
          question = "किसे SMS भेजना चाहते हैं?",
          missingFields = listOf("recipient")
        )
      } else if (messageBody.isNullOrBlank()) {
        AiPlanResult.Clarification(
          question = "क्या संदेश भेजना है?",
          missingFields = listOf("message"),
          partialActionRequest = ActionRequest(
            toolName = "send_sms",
            parameters = mapOf("recipient" to recipient),
            rawQuery = clean
          )
        )
      } else {
        AiPlanResult.Action(
          actionRequest = ActionRequest(
            toolName = "send_sms",
            parameters = mapOf("recipient" to recipient, "message" to messageBody),
            rawQuery = clean
          ),
          spokenConfirmation = "$recipient को SMS भेजा जा रहा है: \"$messageBody\"",
          requiresConfirmation = true
        )
      }
    }

    // 6. YouTube Intent (e.g. "भाई YouTube खोल दे", "Open YouTube", "YouTube पर Arijit Singh चलाओ")
    if (containsAny(lower, "youtube", "यूटुब", "यूट्यूब")) {
      val query = extractYouTubeQuery(clean)
      return AiPlanResult.Action(
        actionRequest = ActionRequest(
          toolName = "open_youtube",
          parameters = if (query.isNotBlank()) mapOf("query" to query) else emptyMap(),
          rawQuery = clean
        ),
        spokenConfirmation = if (query.isNotBlank()) {
          "YouTube पर '$query' खोज रहे हैं..."
        } else {
          "YouTube खोला जा रहा है..."
        }
      )
    }

    // 7. Context-Aware or General App Launch Intent (e.g. "वो वाला app खोलो", "Chrome खोलो")
    if (containsAny(lower, "खोलो", "open", "launch", "kholo", "start", "खोल दे", "खोल", "खोलिदिनु")) {
      // Check pronoun/context app reference
      if (context != null && !context.isExpired()) {
        val refApp = context.resolveAppReference(clean)
        if (!refApp.isNullOrBlank()) {
          return AiPlanResult.Action(
            actionRequest = ActionRequest(
              toolName = "open_app",
              parameters = mapOf("app_name" to refApp),
              rawQuery = clean
            ),
            spokenConfirmation = "$refApp खोला जा रहा है..."
          )
        }
      }

      val appCandidate = extractAppName(clean)
      if (appCandidate.isNotBlank() && !isActionKeyword(appCandidate)) {
        return AiPlanResult.Action(
          actionRequest = ActionRequest(
            toolName = "open_app",
            parameters = mapOf("app_name" to appCandidate),
            rawQuery = clean
          ),
          spokenConfirmation = "$appCandidate खोला जा रहा है..."
        )
      } else if (containsAny(lower, "वो वाला", "that app", "tyo app")) {
        return AiPlanResult.Clarification(
          question = "कौन-सा app खोलूँ?",
          missingFields = listOf("app_name")
        )
      }
    }

    // 8. Phone Call Intent (e.g. "यार माँ को फोन मिला दे", "माँ को call करो", "Call Rahul")
    if ((containsAny(lower, "call", "कॉल", "फोन", "फ़ोन", "dial", "मिला दे", "मिलाओ", "फोन गर") &&
      containsAny(lower, "करो", "करना", "लगाओ", "गर", "गर्नु", "make", "dial", "to", "phone", "मिला दे", "दे", "call")) ||
      lower.startsWith("call ") || lower.startsWith("dial ")
    ) {
      var contact = extractCallContact(clean)
      if (contact.isBlank() && context != null && !context.isExpired()) {
        contact = context.resolvePronounForContact(clean) ?: ""
      }

      return if (contact.isNotBlank()) {
        AiPlanResult.Action(
          actionRequest = ActionRequest(
            toolName = "make_call",
            parameters = mapOf("contact_name" to contact),
            rawQuery = clean
          ),
          spokenConfirmation = "$contact को कॉल मिलाया जा रहा है...",
          requiresConfirmation = true
        )
      } else {
        AiPlanResult.Clarification(
          question = "किसे कॉल करना चाहते हैं?",
          missingFields = listOf("contact_name")
        )
      }
    }

    // 9. Timer Intent
    if (containsAny(lower, "timer", "टाइमर", "ताइमर")) {
      if (containsAny(lower, "stop", "cancel", "बंद", "हटाओ", "रोको", "बन्द")) {
        return AiPlanResult.Action(
          actionRequest = ActionRequest(
            toolName = "set_timer",
            parameters = mapOf("action" to "stop"),
            rawQuery = clean
          ),
          spokenConfirmation = "टाइमर बंद किया गया।"
        )
      }
      val minutes = extractDurationMinutes(clean)
      return AiPlanResult.Action(
        actionRequest = ActionRequest(
          toolName = "set_timer",
          parameters = mapOf("duration_minutes" to minutes.toString(), "label" to "Gamak Timer"),
          rawQuery = clean
        ),
        spokenConfirmation = "$minutes मिनट का टाइमर शुरू किया गया।"
      )
    }

    // 10. Alarm Intent (e.g. "कल सुबह सात बजे जगा देना", "सुबह 7 बजे का alarm लगाओ")
    if (containsAny(lower, "alarm", "अलार्म", "अलर्म", "जगा देना", "उठा देना", "जगा दो", "उठा दो", "wake me up")) {
      val timeStr = extractAlarmTime(clean)
      val relMinutes = if (containsAny(lower, "मिनट बाद", "minute baad", "minutes later")) {
        extractDurationMinutes(clean)
      } else null

      return if (timeStr.isNotBlank() || relMinutes != null) {
        val params = mutableMapOf("label" to "Gamak Alarm")
        if (timeStr.isNotBlank()) params["time"] = timeStr
        if (relMinutes != null) params["relative_minutes"] = relMinutes.toString()

        AiPlanResult.Action(
          actionRequest = ActionRequest(
            toolName = "set_alarm",
            parameters = params,
            rawQuery = clean
          ),
          spokenConfirmation = if (relMinutes != null) "$relMinutes मिनट बाद के लिए अलार्म सेट कर दिया गया है।" else "$timeStr के लिए अलार्म सेट कर दिया गया है।"
        )
      } else {
        AiPlanResult.Clarification(
          question = "अलार्म किस समय का लगाना है? (e.g. सुबह 7 बजे या 6:30 AM)",
          missingFields = listOf("time")
        )
      }
    }

    // 11. Reminder Intent (e.g. "आज शाम मुझे याद दिला देना", "मुझे 6 बजे याद दिलाना")
    if (containsAny(lower, "remind", "याद दिलाना", "रिमाइंडर", "याद गराउनु", "याद दिलाओ", "याद दिला देना", "याद दिला")) {
      val (task, time) = extractReminderDetails(clean)
      return if (task.isNotBlank() && time.isNotBlank()) {
        AiPlanResult.Action(
          actionRequest = ActionRequest(
            toolName = "set_reminder",
            parameters = mapOf("title" to task, "time" to time, "date" to "today"),
            rawQuery = clean
          ),
          spokenConfirmation = "$time बजे आपको '$task' याद दिला दिया जाएगा।"
        )
      } else if (task.isBlank()) {
        AiPlanResult.Clarification(
          question = "किस बात का रिमाइंडर लगाना है?",
          missingFields = listOf("title")
        )
      } else {
        AiPlanResult.Clarification(
          question = "किस समय याद दिलाऊँ?",
          missingFields = listOf("time"),
          partialActionRequest = ActionRequest("set_reminder", mapOf("title" to task), clean)
        )
      }
    }

    // 12. Calendar Event Intent
    if (containsAny(lower, "event", "calendar", "मीटिंग", "इवेंट", "कैलेंडर", "meeting")) {
      val title = extractCalendarTitle(clean)
      val time = extractAlarmTime(clean)
      return if (title.isNotBlank() && time.isNotBlank()) {
        AiPlanResult.Action(
          actionRequest = ActionRequest(
            toolName = "create_calendar_event",
            parameters = mapOf("title" to title, "time" to time, "date" to "tomorrow"),
            rawQuery = clean
          ),
          spokenConfirmation = "कैलेंडर में '$title' इवेंट तैयार किया जा रहा है ($time)..."
        )
      } else if (title.isBlank()) {
        AiPlanResult.Clarification(
          question = "कैलेंडर इवेंट का क्या नाम रखना है?",
          missingFields = listOf("title")
        )
      } else {
        AiPlanResult.Clarification(
          question = "इवेंट किस समय का रखना है?",
          missingFields = listOf("time"),
          partialActionRequest = ActionRequest("create_calendar_event", mapOf("title" to title), clean)
        )
      }
    }

    // 13. Weather Intent
    if (containsAny(lower, "मौसम", "weather", "तापमान", "temperature", "बारिश", "पानी पर्छ", "धूप")) {
      val location = extractLocation(clean)
      return AiPlanResult.Action(
        actionRequest = ActionRequest(
          toolName = "get_weather",
          parameters = mapOf("location" to location, "day" to "today"),
          rawQuery = clean
        ),
        spokenConfirmation = "$location के लिए मौसम की जानकारी प्राप्त की जा रही है..."
      )
    }

    // 14. Navigation & Maps Intent (e.g. "मुझे घर का रास्ता दिखा", "घर जाने का रास्ता दिखाओ")
    if (containsAny(lower, "रास्ता", "navigation", "दिशा", "बाटो", "नक्शा", "map", "maps", "directions", "जाना है", "रास्ता दिखा") &&
      !containsAny(lower, "कहाँ", "where")
    ) {
      val destination = extractDestination(clean)
      return if (destination.isNotBlank()) {
        AiPlanResult.Action(
          actionRequest = ActionRequest(
            toolName = "navigate_maps",
            parameters = mapOf("destination" to destination),
            rawQuery = clean
          ),
          spokenConfirmation = "$destination के लिए रास्ता दिखाया जा रहा है..."
        )
      } else {
        AiPlanResult.Clarification(
          question = "कहाँ जाने का रास्ता देखना चाहते हैं?",
          missingFields = listOf("destination")
        )
      }
    }

    // 15. Media Control Intent
    if (containsAny(lower, "pause", "रोक", "बन्द गर", "stop") && !containsAny(lower, "timer", "टाइमर", "अलार्म")) {
      return AiPlanResult.Action(
        actionRequest = ActionRequest("media_control", mapOf("command" to "pause"), clean),
        spokenConfirmation = "Media रोक दिया गया।"
      )
    }
    if (containsAny(lower, "next song", "अगला गाना", "अर्को गीत", "skip", "next track")) {
      return AiPlanResult.Action(
        actionRequest = ActionRequest("media_control", mapOf("command" to "next"), clean),
        spokenConfirmation = "अगला गाना चलाया जा रहा है..."
      )
    }
    if (containsAny(lower, "previous song", "पिछला गाना", "अघिल्लो गीत", "prev")) {
      return AiPlanResult.Action(
        actionRequest = ActionRequest("media_control", mapOf("command" to "previous"), clean),
        spokenConfirmation = "पिछला गाना चलाया जा रहा है..."
      )
    }

    // 16. Music Play Intent
    if (containsAny(lower, "music", "गाने", "गाना", "गीत", "सङ्गीत", "songs", "song", "gaana") ||
      (containsAny(lower, "play", "चलाओ", "बजाऊ") && !containsAny(lower, "trailer", "video"))
    ) {
      val musicQuery = extractMusicQuery(clean)
      return AiPlanResult.Action(
        actionRequest = ActionRequest(
          toolName = "play_music",
          parameters = if (musicQuery.isNotBlank()) mapOf("query" to musicQuery) else emptyMap(),
          rawQuery = clean
        ),
        spokenConfirmation = if (musicQuery.isNotBlank()) {
          "'$musicQuery' चलाया जा रहा है..."
        } else {
          "सङ्गीत शुरू किया जा रहा है..."
        }
      )
    }

    // 17. Share Intent
    if (containsAny(lower, "share", "शेयर", "साझा")) {
      val textToShare = clean.replace(Regex("""(?:share|शेयर|साझा|करो|गर)?\s*""", RegexOption.IGNORE_CASE), "").trim()
      return AiPlanResult.Action(
        actionRequest = ActionRequest("share_content", mapOf("text" to textToShare.ifBlank { "Shared from Gamak AI" }), clean),
        spokenConfirmation = "शेयर शीट खोली जा रही है..."
      )
    }

    // 18. Conversational Dialogue & Fallback Responses
    val isNepali = containsAny(lower, "छ", "हुनुहुन्छ", "गर्नु", "गरिदेऊ", "आमा", "दाजु", "कस्तो", "सन्चै", "बज्यो", "हुन्न", "पर्दैन", "भनेर", "दिनु")
    val isEnglish = clean.matches(Regex("""^[a-zA-Z0-9\s?,.!'":;@#$%^&*()_\-+=/]+$""")) && !containsAny(lower, "bhai", "yaar", "karo", "khol", "kholo", "lagao", "batao", "de", "do", "hai", "kaise", "kya")

    // Greetings
    val greetings = listOf("नमस्ते", "hello", "hi", "namaste", "hey", "hola", "नमस्कार", "pranam", "प्रणाम")
    if (greetings.any { lower.startsWith(it) || lower == it }) {
      val reply = when {
        isNepali -> "नमस्ते! म $personaName हुँ। म तपाईंलाई के सहयोग गर्न सक्छु?"
        isEnglish -> "Hello! I'm $personaName. How can I help you today?"
        else -> "नमस्ते! मैं $personaName हूँ। आपकी क्या मदद कर सकता हूँ?"
      }
      return AiPlanResult.Conversation(responseText = reply)
    }

    // Well-being & Casual Small Talk
    if (containsAny(lower, "how are you", "kya haal hai", "kasto chha", "sanchai chhau", "क्या हाल है", "कस्तो छ", "हालचाल", "सब ठीक", "thik chha", "kya chal raha hai")) {
      val reply = when {
        isNepali -> "म एकदम सन्चै छु! भन्नुहोस्, आज म तपाईंलाई के सहयोग गर्न सक्छु?"
        isEnglish -> "I'm doing great, thank you! How can I assist you today?"
        else -> "मैं बिल्कुल ठीक हूँ! आप बताइए, आज मैं आपकी क्या मदद करूँ?"
      }
      return AiPlanResult.Conversation(responseText = reply)
    }

    // Identity & Capabilities
    if (containsAny(lower, "who are you", "तुम कौन हो", "तपाईं को हुनुहुन्छ", "about yourself", "identity", "what is gamak", "what can you do", "क्या कर सकते हो", "तिमी के गर्न सक्छौ", "help", "मदद", "features", "सुविधा")) {
      val reply = when {
        isNepali -> "म $personaName हुँ, Gamak AI को बुद्धिमत्ता सहायक। म नेपाली, हिन्दी, हिंग्लिश र अंग्रेजीमा आदेश बुझेर तपाईंको फोनमा कल, म्यासेज, अलार्म, युट्युब, मौसम र एपहरू चलाउन सक्छु।"
        isEnglish -> "I am $personaName, your intelligent voice assistant in Gamak AI. I can make calls, send SMS/WhatsApp, set alarms & timers, navigate, check weather, open apps, and answer your questions."
        else -> "मैं $personaName हूँ, Gamak AI का व्यक्तिगत बुद्धिमत्ता सहायक। मैं हिंदी, नेपाली, हिंग्लिश और अंग्रेजी में आदेश समझ कर आपके डिवाइस पर कॉल, मैसेज, अलार्म, टाइमर, रिमाइंडर, यूट्यूब, मैप्स और ऐप्स खोलने जैसे कार्य करता हूँ।"
      }
      return AiPlanResult.Conversation(responseText = reply)
    }

    // Current Time / Date
    if (containsAny(lower, "what time is it", "time kya hua", "समय क्या हुआ", "कति बज्यो", "time batao", "current time", "kitne baje")) {
      val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
      val reply = when {
        isNepali -> "अहिलेको समय $timeFormat हो।"
        isEnglish -> "The current time is $timeFormat."
        else -> "अभी का समय $timeFormat है।"
      }
      return AiPlanResult.Conversation(responseText = reply)
    }
    if (containsAny(lower, "date", "तारीख", "मिति", "aaj konsa din", "what day is today", "what is today's date")) {
      val dateFormat = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
      val reply = when {
        isNepali -> "आजको मिति $dateFormat हो।"
        isEnglish -> "Today is $dateFormat."
        else -> "आज $dateFormat है।"
      }
      return AiPlanResult.Conversation(responseText = reply)
    }

    // Gratitude & Compliments
    if (containsAny(lower, "thank you", "thanks", "धन्यवाद", "shukriya", "dhanyabad", "शुक्रिया", "great job", "शाबाश", "बढ़िया", "ramro", "very good", "nice")) {
      val reply = when {
        isNepali -> "धेरै धेरै धन्यवाद! केही थप सहयोग चाहिएमा कृपया भन्नुहोस्।"
        isEnglish -> "You're very welcome! Let me know if you need anything else."
        else -> "आपका बहुत-बहुत धन्यवाद! अगर आपको कुछ और सहायता चाहिए तो अवश्य बताएं।"
      }
      return AiPlanResult.Conversation(responseText = reply)
    }

    // Jokes & Humor
    if (containsAny(lower, "joke", "चुटकुला", "chutkula", "hasau", "रमाइलो", "मज़ाक", "हँसाओ")) {
      val reply = when {
        isNepali -> "शिक्षक: 'तिमी किन ढिलो आयौ?' विद्यार्थी: 'बाटोको बोर्डले भन्यो - अगाडि विद्यालय छ, बिस्तारै जानुहोस्!'"
        isEnglish -> "Why did the computer show up at work late? It had a hard drive!"
        else -> "शिक्षक ने पूछा: 'अगर तुम्हारे पास 4 समोसे हैं और 2 खा लिए, तो क्या बचेगा?' छात्र: 'सर, सिर्फ चटनी और पछतावा!'"
      }
      return AiPlanResult.Conversation(responseText = reply)
    }

    // Basic Math Calculation Evaluation
    val mathMatch = Regex("""(?:calculate|what is|कितना होता है|कति हुन्छ)?\s*(\d+(?:\.\d+)?)\s*([\+\-\*\/]|plus|minus|into|times|divided by|गुना|भागा|जोड़)\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE).find(clean)
    if (mathMatch != null) {
      val num1 = mathMatch.groupValues[1].toDoubleOrNull()
      val op = mathMatch.groupValues[2].lowercase()
      val num2 = mathMatch.groupValues[3].toDoubleOrNull()
      if (num1 != null && num2 != null) {
        val calc = when {
          op == "+" || op == "plus" || op == "जोड़" -> num1 + num2
          op == "-" || op == "minus" -> num1 - num2
          op == "*" || op == "into" || op == "times" || op == "गुना" -> num1 * num2
          op == "/" || op == "divided by" || op == "भागा" -> if (num2 != 0.0) num1 / num2 else null
          else -> null
        }
        if (calc != null) {
          val formatted = if (calc % 1.0 == 0.0) calc.toLong().toString() else "%.2f".format(calc)
          val reply = when {
            isNepali -> "उत्तर $formatted हुन्छ।"
            isEnglish -> "The result is $formatted."
            else -> "उत्तर $formatted है।"
          }
          return AiPlanResult.Conversation(responseText = reply)
        }
      }
    }

    // General Questions / Informational queries fallback
    if (containsAny(lower, "what is", "why", "who is", "how", "क्या है", "किसे कहते", "किन", "कसरी", "कहाँ", "कसको", "बताओ", "explain", "meaning", "define")) {
      val reply = when {
        isNepali -> "विस्तृत ज्ञान र खोजका लागि इन्टरनेट वा Gemini AI सेवा आवश्यक पर्दछ। तर म तपाईंको फोनमा कल गर्न, म्यासेज पठाउन, अलार्म लगाउन, मौसम हेर्न वा एपहरू खोल्न सक्छु।"
        isEnglish -> "For in-depth online answers, an active connection with Gemini AI is recommended. Meanwhile, I can help you make calls, send messages, set alarms, navigate, or launch apps on your device."
        else -> "विस्तृत जानकारी और ऑनलाइन उत्तरों के लिए Gemini AI सेवा का उपयोग किया जा सकता है। इस बीच, मैं आपके डिवाइस पर कॉल, मैसेज, अलार्म, टाइमर, मौसम या ऐप्स खोलने में पूरी मदद कर सकता हूँ।"
      }
      return AiPlanResult.Conversation(responseText = reply)
    }

    // Default natural conversational fallback (Never generic "ready to work" placeholder)
    val defaultReply = when {
      isNepali -> "म तपाईंको कुरा सुन्दै छु। तपाईं मलाई कल, म्यासेज, अलार्म, मौसम वा एपहरू खोल्ने निर्देशन दिन सक्नुहुन्छ।"
      isEnglish -> "I'm listening. You can ask me questions or ask me to make calls, send messages, set alarms, check the weather, or open apps."
      else -> "मैं आपकी बात सुन रहा हूँ। आप मुझसे कॉल करने, मैसेज भेजने, अलार्म/टाइमर लगाने, मौसम जानने या ऐप्स खोलने के निर्देश दे सकते हैं।"
    }

    return AiPlanResult.Conversation(responseText = defaultReply)
  }

  fun resolveClarificationFollowUp(
    userInput: String,
    partialAction: ActionRequest,
    missingFields: List<String>
  ): ActionRequest {
    val clean = userInput.trim()
    val updatedParams = partialAction.parameters.toMutableMap()

    when (partialAction.toolName) {
      "send_whatsapp_message", "send_sms" -> {
        if (missingFields.contains("message")) {
          // Cleans pronoun prefixes like "उसे बोलो", "say", "tell him/her", "उसको बोलो"
          val cleanedMsg = clean.replace(
            Regex("""^(?:उसे|उसको|उन्हें|उनको|उहाँलाई|उसलाई|him|her|them)?\s*(?:बोलो|बताओ|भन|भनिदेऊ|tell\s+him|tell\s+her|saying|that|लिखो|भेजो|पठाऊ)[:\s]*""", RegexOption.IGNORE_CASE),
            ""
          ).trim()
          updatedParams["message"] = cleanedMsg.ifBlank { clean }
        } else if (missingFields.contains("recipient")) {
          val recipient = extractRecipient(clean) ?: clean
          updatedParams["recipient"] = recipient
        }
      }
      "make_call" -> {
        val contact = extractCallContact(clean).ifBlank { clean }
        updatedParams["contact_name"] = contact
      }
      "set_alarm" -> {
        val time = extractAlarmTime(clean).ifBlank { clean }
        updatedParams["time"] = time
      }
      "set_reminder" -> {
        if (missingFields.contains("title")) {
          updatedParams["title"] = clean
        } else if (missingFields.contains("time")) {
          val time = extractAlarmTime(clean).ifBlank { clean }
          updatedParams["time"] = time
        }
      }
      "create_calendar_event" -> {
        if (missingFields.contains("title")) {
          updatedParams["title"] = clean
        } else if (missingFields.contains("time")) {
          val time = extractAlarmTime(clean).ifBlank { clean }
          updatedParams["time"] = time
        }
      }
      "navigate_maps" -> {
        val destination = extractDestination(clean).ifBlank { clean }
        updatedParams["destination"] = destination
      }
      "open_app" -> {
        val app = extractAppName(clean).ifBlank { clean }
        updatedParams["app_name"] = app
      }
      else -> {
        val field = missingFields.firstOrNull() ?: "value"
        updatedParams[field] = clean
      }
    }

    return partialAction.copy(parameters = updatedParams)
  }

  fun isAffirmativeConfirmation(text: String): Boolean {
    val lower = text.trim().lowercase()
    val affirmativeWords = listOf(
      "हाँ", "हां", "yes", "sure", "कर दो", "करिए", "करदे", "लगा दो", "भेज दो",
      "कॉल करो", "ok", "okay", "हो", "हुन्छ", "गरिदेऊ", "गर", "yep", "yeah", "proceed",
      "confirm", "confirmed", "बिलकुल", "ज़रूर", "चलाओ"
    )
    return affirmativeWords.any { lower == it || lower.startsWith("$it ") || lower.endsWith(" $it") }
  }

  fun isNegativeConfirmation(text: String): Boolean {
    val lower = text.trim().lowercase()
    val negativeWords = listOf(
      "नहीं", "ना", "no", "cancel", "रद्द करो", "रहने दो", "छोड़ो", "मत करो",
      "हुन्न", "पर्दैन", "nope", "nah", "stop", "रद्द गर", "बन्द गर", "don't", "नहीं चाहिए", "चुप"
    )
    return negativeWords.any { lower == it || lower.startsWith("$it ") || lower.endsWith(" $it") }
  }

  fun isCancellationIntent(text: String): Boolean {
    val lower = text.trim().lowercase()
    val cancelWords = listOf(
      "cancel", "रद्द करो", "रद्द", "छोड़ो", "रहने दो", "नहीं करना",
      "बंद करो", "stop", "रद्द गर", "पर्दैन", "मत करो", "चुप", "नहीं चाहिए", "बन्द गर"
    )
    return cancelWords.any { lower == it || lower.startsWith(it) }
  }

  private fun extractMemoryKeyValue(text: String): Pair<String, String> {
    val clean = text.replace(Regex("""(?:याद\s+रखो|याद\s+रख|remember\s+that|remember|याद\s+राख|मेमोरी|ki|कि)?\s*""", RegexOption.IGNORE_CASE), "").trim()

    // Example 1: "मुझे Hindi में जवाब देना पसंद है" -> key: "preferred_language", value: "Hindi"
    if (containsAny(clean.lowercase(), "hindi", "हिंदी", "हिन्दी")) {
      return Pair("preferred_language", "Hindi")
    }
    if (containsAny(clean.lowercase(), "nepali", "नेपाली")) {
      return Pair("preferred_language", "Nepali")
    }
    if (containsAny(clean.lowercase(), "english", "अंग्रेजी")) {
      return Pair("preferred_language", "English")
    }

    // Example 2: "acoustic music पसंद है" -> key: "favorite_music", value: "Acoustic"
    val musicMatch = Regex("""([A-Za-z\u0900-\u097F]+)\s*(?:music|गीत|गाना)""", RegexOption.IGNORE_CASE).find(clean)
    if (musicMatch != null) {
      return Pair("favorite_music", musicMatch.groupValues[1].trim())
    }

    return Pair("general_preference", clean)
  }

  private fun containsAny(text: String, vararg keywords: String): Boolean {
    return keywords.any { text.contains(it, ignoreCase = true) }
  }

  private fun extractRecipient(text: String): String? {
    val patterns = listOf(
      Regex("""(?:message|मैसेज|sms)\s*(?:to|for)?\s*([A-Za-z\u0900-\u097F]+)\s*(?:that|saying|लिखो|बोलो|कि|भनेर)?""", RegexOption.IGNORE_CASE),
      Regex("""(?:whatsapp\s+(?:पर|पे)?\s+)?([A-Za-z\u0900-\u097F]+)\s*(?:को|लाई)\s*(?:message|मैसेज|sms|बोलो|बताओ)""", RegexOption.IGNORE_CASE),
      Regex("""([A-Za-z\u0900-\u097F]+)\s*(?:को|लाई)\s*(?:whatsapp|मैसेज|sms)""", RegexOption.IGNORE_CASE)
    )

    for (p in patterns) {
      val match = p.find(text)
      if (match != null && match.groupValues.size > 1) {
        val name = match.groupValues[1].trim()
        if (!isStopWord(name)) return name
      }
    }
    return null
  }

  private fun extractMessageBody(text: String): String? {
    val quoteMatch = Regex("""["'](.*?)["']""").find(text)
    if (quoteMatch != null) return quoteMatch.groupValues[1]

    val writeMatch = Regex("""(?:लिखो|भेजो|send|saying|texting|पठाऊ|बोलो|बताओ|भन)\s*[:\s]+(.*)""", RegexOption.IGNORE_CASE).find(text)
    if (writeMatch != null) return writeMatch.groupValues[1].trim()

    val sayMatch = Regex("""(?:कि|भनेर|that)\s+(.*)""", RegexOption.IGNORE_CASE).find(text)
    if (sayMatch != null) return sayMatch.groupValues[1].trim()

    return null
  }

  private fun extractYouTubeQuery(text: String): String {
    val cleaned = text.replace(Regex("""(?:भाई|daju|yaar|यार|please)?\s*youtube\s*(?:खोलो|open|kholo|खोलिदिनु|चलाओ|खोल\s+दे|खोल)?\s*""", RegexOption.IGNORE_CASE), "")
      .replace(Regex("""(?:search|खोजो|play|लगाओ|video)""", RegexOption.IGNORE_CASE), "")
      .trim()
    return if (cleaned.length > 2) cleaned else ""
  }

  private fun extractAppName(text: String): String {
    val cleaned = text.replace(Regex("""(?:भाई|daju|yaar|यार|please)?\s*(?:app|application)?\s*(?:खोलो|open|launch|kholo|खोलिदिनु|खोल\s+दे|खोल)?\s*""", RegexOption.IGNORE_CASE), "").trim()
    return cleaned
  }

  private fun extractAlarmTime(text: String): String {
    // Matches "सुबह 7 बजे", "7:00 AM", "7 am", "साँझ 6 बजे", etc.
    val timePattern = Regex("""((?:सुबह|कल\s+सुबह|शाम|रात|साँझ)?\s*\d{1,2}(?::\d{2})?\s*(?:am|pm|बजे|बजेको|hours)?)""", RegexOption.IGNORE_CASE)
    val match = timePattern.find(text)
    return match?.groupValues?.getOrNull(1)?.trim() ?: ""
  }

  private fun extractDurationMinutes(text: String): Int {
    val match = Regex("""(\d+)\s*(?:मिनट|min|minute|minutes)""", RegexOption.IGNORE_CASE).find(text)
    return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 5
  }

  private fun extractCallContact(text: String): String {
    val clean = text.replace(Regex("""^(?:यार|भाई|daju|दाजु|please|कृपया|zara|ज़रा|arrey|अरे|suno|सुनो|हे)?\s+""", RegexOption.IGNORE_CASE), "")
      .replace(Regex("""^(?:यार|भाई|daju|दाजु|please|कृपया|zara|ज़रा|arrey|अरे|suno|सुनो|हे)?\s+""", RegexOption.IGNORE_CASE), "")
      .trim()

    val patterns = listOf(
      Regex("""([A-Za-z\u0900-\u097F]+)\s*(?:ko|lai|को|लाई)\s*(?:call|कॉल|फोन|phone|मिला\s+दे|लगाओ|गर|गर्नु)""", RegexOption.IGNORE_CASE),
      Regex("""([A-Za-z\u0900-\u097F]+)\s*(?:ko|lai|को|लाई)?\s*(?:call|कॉल|फोन|phone)\s*(?:करो|करना|लगाओ|मिलाओ|गर|गर्नु|lagao|karo)""", RegexOption.IGNORE_CASE),
      Regex("""(?:call|dial|फोन गर|कॉल करो|फोन मिला दे|फोन मिलाओ)\s*([A-Za-z\u0900-\u097F]+)""", RegexOption.IGNORE_CASE)
    )
    for (p in patterns) {
      val m = p.find(clean)
      if (m != null && m.groupValues.size > 1) {
        val name = m.groupValues[1].trim()
        if (!isStopWord(name)) return name
      }
    }
    return ""
  }

  private fun extractLocation(text: String): String {
    val pattern = Regex("""(?:in|at|में|का|को|मा)\s+([A-Za-z\u0900-\u097F]+)""", RegexOption.IGNORE_CASE)
    val m = pattern.find(text)
    return m?.groupValues?.getOrNull(1)?.trim() ?: "Kathmandu"
  }

  private fun extractDestination(text: String): String {
    if (containsAny(text.lowercase(), "घर", "home", "house")) return "घर (Home)"
    if (containsAny(text.lowercase(), "office", "दफ्तर", "अफिस")) return "Office"
    val pattern = Regex("""(?:to|का|को|जाने)\s+([A-Za-z\u0900-\u097F]+(?:\s+[A-Za-z\u0900-\u097F]+)?)\s*(?:का|का बाटो|रास्ता)?""", RegexOption.IGNORE_CASE)
    val m = pattern.find(text)
    return m?.groupValues?.getOrNull(1)?.trim() ?: "घर (Home)"
  }

  private fun extractReminderDetails(text: String): Pair<String, String> {
    val time = extractAlarmTime(text)
    val task = text.replace(Regex("""(?:आज\s+शाम|शाम|सुबह|रात|\d{1,2}(?::\d{2})?\s*बजे|remind|me|याद|दिलाना|दिलाओ|दिला\s+देना|homework|को|मुझे)""", RegexOption.IGNORE_CASE), "").trim()
    return Pair(task.ifBlank { "Homework" }, time.ifBlank { "18:00" })
  }

  private fun extractCalendarTitle(text: String): String {
    val cleaned = text.replace(Regex("""(?:कल|शाम|\d{1,2}(?::\d{2})?\s*बजे|event|meeting|बना दो|कैलेंडर|में)""", RegexOption.IGNORE_CASE), "").trim()
    return cleaned.ifBlank { "Meeting" }
  }

  private fun extractMusicQuery(text: String): String {
    val cleaned = text.replace(Regex("""(?:play|some|music|गाने|गाना|गीत|बजाऊ|चलाओ|लगाओ|sangeet)""", RegexOption.IGNORE_CASE), "")
      .replace(Regex("""(?:ke|का|को)""", RegexOption.IGNORE_CASE), "")
      .trim()
    return cleaned
  }

  private fun isStopWord(word: String): Boolean {
    val stopWords = setOf(
      "भाई", "दाजु", "please", "me", "mujhe", "whatsapp", "message", "call", "sms", "एक", "app", "यार",
      "उसे", "उसको", "उन्हें", "उनको", "उसपे", "उसलाई", "उहाँलाई", "him", "her", "them", "वो", "त्यो", "it",
      "करो", "करना", "लगाओ", "गर", "गर्नु", "देना", "दे", "दो", "दिनु", "मैसेज", "कॉल", "फोन"
    )
    return stopWords.contains(word.lowercase())
  }

  private fun isActionKeyword(word: String): Boolean {
    val actions = setOf(
      "call", "sms", "alarm", "timer", "weather", "music", "play", "settings",
      "वो", "त्यो", "that", "this", "app"
    )
    return actions.contains(word.lowercase())
  }
}
