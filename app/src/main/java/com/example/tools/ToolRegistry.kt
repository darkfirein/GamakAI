package com.example.tools

class ToolRegistry {

  private val tools = mutableMapOf<String, ToolDefinition>()

  init {
    registerDefaultTools()
  }

  fun registerTool(tool: ToolDefinition) {
    tools[tool.name] = tool
  }

  fun getTool(name: String): ToolDefinition? = tools[name]

  fun getAllTools(): List<ToolDefinition> = tools.values.toList()

  private fun registerDefaultTools() {
    // 1. App Launching
    registerTool(
      ToolDefinition(
        name = "open_app",
        description = "Opens any installed Android app by natural name (e.g. 'Camera खोलो', 'Gallery खोलो', 'Settings खोलो', 'YouTube खोलो', 'WhatsApp खोलो', 'Chrome खोलो')",
        category = ToolCategory.SYSTEM_UTILITY,
        parameters = listOf(
          ToolParameter("app_name", "string", "Name of the application to open (e.g. camera, gallery, settings, youtube, whatsapp, calculator)", required = true)
        )
      )
    )

    registerTool(
      ToolDefinition(
        name = "open_youtube",
        description = "Opens YouTube or searches for a video/channel on YouTube (e.g. 'YouTube खोलो', 'Play trailer on YouTube')",
        category = ToolCategory.MEDIA,
        parameters = listOf(
          ToolParameter("query", "string", "Search query or video topic on YouTube", required = false)
        )
      )
    )

    registerTool(
      ToolDefinition(
        name = "open_camera",
        description = "Opens the device camera app to take a photo or video (e.g. 'Camera खोलो', 'Take a picture')",
        category = ToolCategory.SYSTEM_UTILITY,
        parameters = listOf(
          ToolParameter("mode", "string", "Camera mode: photo or video", required = false)
        )
      )
    )

    registerTool(
      ToolDefinition(
        name = "open_gallery",
        description = "Opens the photo gallery or photo picker (e.g. 'Gallery खोलो', 'Photos दिखाओ')",
        category = ToolCategory.SYSTEM_UTILITY,
        parameters = emptyList()
      )
    )

    registerTool(
      ToolDefinition(
        name = "open_settings",
        description = "Opens Android system settings screen (e.g. 'Wi-Fi settings खोलो', 'Bluetooth settings खोलो', 'Location settings खोलो', 'Settings खोलो')",
        category = ToolCategory.SYSTEM_UTILITY,
        parameters = listOf(
          ToolParameter("setting_type", "string", "Type of setting: wifi, bluetooth, location, sound, display, app, main", required = false)
        )
      )
    )

    // 2. Communication
    registerTool(
      ToolDefinition(
        name = "make_call",
        description = "Initiates a real phone call to a contact or phone number (e.g. 'माँ को call करो', 'Rahul को फोन लगाओ', 'Mom लाई फोन गर')",
        category = ToolCategory.COMMUNICATION,
        parameters = listOf(
          ToolParameter("contact_name", "string", "Name or relationship of the contact to call", required = true),
          ToolParameter("phone_number", "string", "Phone number if provided directly", required = false)
        )
      )
    )

    registerTool(
      ToolDefinition(
        name = "send_sms",
        description = "Sends an SMS message to a contact (e.g. 'Rahul को SMS करो कि मैं घर आ रहा हूँ')",
        category = ToolCategory.COMMUNICATION,
        parameters = listOf(
          ToolParameter("recipient", "string", "Recipient contact name or phone number", required = true),
          ToolParameter("message", "string", "Message text content to send", required = true)
        )
      )
    )

    registerTool(
      ToolDefinition(
        name = "send_whatsapp_message",
        description = "Opens WhatsApp conversation or compose flow to send a message (e.g. 'WhatsApp पर Rahul को message लिखो', 'WhatsApp message to Mom')",
        category = ToolCategory.COMMUNICATION,
        parameters = listOf(
          ToolParameter("recipient", "string", "Recipient contact name or phone", required = true),
          ToolParameter("message", "string", "Body of the message to send", required = true)
        )
      )
    )

    registerTool(
      ToolDefinition(
        name = "open_whatsapp",
        description = "Opens the WhatsApp application (e.g. 'WhatsApp खोलो')",
        category = ToolCategory.COMMUNICATION,
        parameters = emptyList()
      )
    )

    // 3. Alarms, Timers & Productivity
    registerTool(
      ToolDefinition(
        name = "set_alarm",
        description = "Sets an alarm for a specific time (e.g. 'कल सुबह 7 बजे alarm लगा दो', '10 मिनट बाद alarm लगा दो')",
        category = ToolCategory.SYSTEM_UTILITY,
        parameters = listOf(
          ToolParameter("time", "string", "Time for the alarm (e.g. '07:00 AM', '7:00', '19:30')", required = true),
          ToolParameter("label", "string", "Alarm description or label", required = false),
          ToolParameter("relative_minutes", "number", "Relative minutes from now if expressed relatively", required = false)
        )
      )
    )

    registerTool(
      ToolDefinition(
        name = "set_timer",
        description = "Sets a real countdown timer (e.g. '10 मिनट का timer लगाओ', '5 minute timer', 'Timer बंद करो')",
        category = ToolCategory.SYSTEM_UTILITY,
        parameters = listOf(
          ToolParameter("duration_minutes", "number", "Duration in minutes", required = false),
          ToolParameter("duration_seconds", "number", "Duration in seconds", required = false),
          ToolParameter("label", "string", "Timer label", required = false),
          ToolParameter("action", "string", "start or stop", required = false)
        )
      )
    )

    registerTool(
      ToolDefinition(
        name = "set_reminder",
        description = "Schedules a reminder notification for a specific time and task (e.g. 'शाम 6 बजे मुझे homework याद दिलाना', 'Remind me tomorrow at 9 AM to call doctor')",
        category = ToolCategory.PRODUCTIVITY,
        parameters = listOf(
          ToolParameter("title", "string", "What to remember / reminder message", required = true),
          ToolParameter("time", "string", "Time to trigger reminder (e.g. '18:00', '6:00 PM')", required = true),
          ToolParameter("date", "string", "Date (e.g. 'today', 'tomorrow', '2026-08-27')", required = false)
        )
      )
    )

    registerTool(
      ToolDefinition(
        name = "create_calendar_event",
        description = "Creates a calendar event in the device calendar (e.g. 'कल शाम 5 बजे Rahul के साथ meeting का event बना दो')",
        category = ToolCategory.PRODUCTIVITY,
        parameters = listOf(
          ToolParameter("title", "string", "Event title or meeting subject", required = true),
          ToolParameter("date", "string", "Date for the event (e.g. 'tomorrow', '2026-08-27')", required = true),
          ToolParameter("time", "string", "Start time for the event (e.g. '17:00', '5:00 PM')", required = true),
          ToolParameter("duration_minutes", "number", "Duration in minutes (defaults to 60)", required = false),
          ToolParameter("location", "string", "Location for the event", required = false)
        )
      )
    )

    // 4. Navigation & Weather
    registerTool(
      ToolDefinition(
        name = "navigate_maps",
        description = "Opens GPS navigation or directions in Google Maps (e.g. 'मुझे Kathmandu का रास्ता दिखाओ', 'Delhi जाने का रास्ता दिखाओ', 'घर का रास्ता दिखाओ')",
        category = ToolCategory.NAVIGATION,
        parameters = listOf(
          ToolParameter("destination", "string", "Destination address, city, place, or landmark", required = true),
          ToolParameter("mode", "string", "Travel mode: driving, walking, transit, bicycling", required = false)
        )
      )
    )

    registerTool(
      ToolDefinition(
        name = "get_weather",
        description = "Gets real weather conditions for a city or current location (e.g. 'आज मौसम कैसा है?', 'Kathmandu में मौसम कैसा है?', 'Delhi का weather बताओ')",
        category = ToolCategory.INFORMATION,
        parameters = listOf(
          ToolParameter("location", "string", "City, country, or location name", required = false),
          ToolParameter("day", "string", "Target day: today, tomorrow", required = false)
        )
      )
    )

    // 5. Media, Music & Sharing
    registerTool(
      ToolDefinition(
        name = "play_music",
        description = "Plays music, songs, or playlists (e.g. 'Music play करो', 'Arijit Singh ke gaane chalao')",
        category = ToolCategory.MEDIA,
        parameters = listOf(
          ToolParameter("query", "string", "Song, artist, or music query", required = false)
        )
      )
    )

    registerTool(
      ToolDefinition(
        name = "media_control",
        description = "Controls system media playback (e.g. 'Pause करो', 'Next song', 'Previous song', 'Resume music')",
        category = ToolCategory.MEDIA,
        parameters = listOf(
          ToolParameter("command", "string", "Media command: play, pause, next, previous, toggle", required = true)
        )
      )
    )

    registerTool(
      ToolDefinition(
        name = "share_content",
        description = "Shares text, photos, or links using the Android Sharesheet (e.g. 'इस photo को share करो', 'Share message')",
        category = ToolCategory.COMMUNICATION,
        parameters = listOf(
          ToolParameter("text", "string", "Text or link to share", required = false),
          ToolParameter("target_app", "string", "Optional target app (e.g. whatsapp)", required = false)
        )
      )
    )
  }
}
