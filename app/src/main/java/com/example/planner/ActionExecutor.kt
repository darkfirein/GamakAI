package com.example.planner

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import com.example.platform.ContactResolver
import com.example.platform.OpenMeteoWeatherService
import com.example.platform.PermissionManager
import com.example.platform.WeatherService
import com.example.receiver.ReminderBroadcastReceiver
import com.example.tools.ActionRequest
import com.example.tools.ActionResult
import com.example.tools.PickerOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ActionExecutor(
  private val context: Context? = null,
  private val weatherService: WeatherService = OpenMeteoWeatherService()
) {

  companion object {
    private const val TAG = "ActionExecutor"
  }

  private val recentTransactions = java.util.concurrent.ConcurrentHashMap<String, Long>()

  private fun isDuplicateTransaction(key: String, cooldownMs: Long = 1500L): Boolean {
    val now = System.currentTimeMillis()
    val last = recentTransactions[key] ?: 0L
    if (now - last < cooldownMs) {
      return true
    }
    recentTransactions[key] = now
    return false
  }

  suspend fun execute(actionRequest: ActionRequest): ActionResult = withContext(Dispatchers.IO) {
    val ctx = context ?: return@withContext ActionResult.Success(
      responseMessage = "Action planned for '${actionRequest.toolName}' with params: ${actionRequest.parameters}",
      data = actionRequest.parameters
    )

    try {
      when (actionRequest.toolName) {
        // 1. App Launching & System Utilities
        "open_app" -> handleOpenApp(ctx, actionRequest)
        "open_youtube" -> handleOpenYouTube(ctx, actionRequest)
        "open_camera" -> handleOpenCamera(ctx, actionRequest)
        "open_gallery" -> handleOpenGallery(ctx, actionRequest)
        "open_settings" -> handleOpenSettings(ctx, actionRequest)
        "open_whatsapp" -> handleOpenWhatsApp(ctx, actionRequest)

        // 2. Communication
        "make_call" -> handleMakeCall(ctx, actionRequest)
        "send_sms" -> handleSendSms(ctx, actionRequest)
        "send_whatsapp_message" -> handleSendWhatsAppMessage(ctx, actionRequest)

        // 3. Alarms, Timers & Productivity
        "set_alarm" -> handleSetAlarm(ctx, actionRequest)
        "set_timer" -> handleSetTimer(ctx, actionRequest)
        "set_reminder" -> handleSetReminder(ctx, actionRequest)
        "create_calendar_event" -> handleCreateCalendarEvent(ctx, actionRequest)

        // 4. Navigation & Weather
        "navigate_maps" -> handleNavigateMaps(ctx, actionRequest)
        "get_weather" -> handleGetWeather(ctx, actionRequest)

        // 5. Media & Sharing
        "play_music" -> handlePlayMusic(ctx, actionRequest)
        "media_control" -> handleMediaControl(ctx, actionRequest)
        "share_content" -> handleShareContent(ctx, actionRequest)

        else -> {
          ActionResult.Failure(
            reason = "असमर्थित कमांड: '${actionRequest.toolName}'।",
            errorType = "UNSUPPORTED_TOOL"
          )
        }
      }
    } catch (e: SecurityException) {
      Log.e(TAG, "SecurityException executing ${actionRequest.toolName}", e)
      ActionResult.Failure("सुरक्षा प्रतिबंध या अनुमति की कमी के कारण यह कार्य पूरा नहीं हो सका।", "SECURITY_EXCEPTION")
    } catch (e: Exception) {
      Log.e(TAG, "Exception executing ${actionRequest.toolName}", e)
      ActionResult.Failure("कार्य निष्पादित करते समय त्रुटि हुई: ${e.localizedMessage}", "EXECUTION_ERROR")
    }
  }

  // ==========================================
  // App Launching Handlers
  // ==========================================

  private fun handleOpenApp(context: Context, request: ActionRequest): ActionResult {
    val appName = request.parameters["app_name"]?.lowercase()?.trim() ?: "app"
    val pm = context.packageManager

    val directPackage = when {
      appName.contains("youtube") || appName.contains("यूट्यूब") -> "com.google.android.youtube"
      appName.contains("whatsapp") || appName.contains("व्हाट्सएप") -> "com.whatsapp"
      appName.contains("chrome") || appName.contains("क्रोम") -> "com.android.chrome"
      appName.contains("map") || appName.contains("नक्शा") -> "com.google.android.apps.maps"
      appName.contains("calculator") || appName.contains("कैलकुलेटर") -> "com.google.android.calculator"
      appName.contains("clock") || appName.contains("अलार्म") || appName.contains("घड़ी") -> "com.google.android.deskclock"
      appName.contains("calendar") || appName.contains("कैलेंडर") -> "com.google.android.calendar"
      appName.contains("photos") || appName.contains("gallery") || appName.contains("गैलरी") -> "com.google.android.apps.photos"
      else -> null
    }

    if (appName.contains("camera") || appName.contains("कैमरा")) {
      return handleOpenCamera(context, request)
    }

    if (appName.contains("settings") || appName.contains("सेटिंग्स") || appName.contains("सेटिङ्ग")) {
      return handleOpenSettings(context, request)
    }

    if (appName.contains("gallery") || appName.contains("गैलरी") || appName.contains("फोटो")) {
      return handleOpenGallery(context, request)
    }

    if (directPackage != null) {
      val launchIntent = pm.getLaunchIntentForPackage(directPackage)
      if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return ActionResult.Success(
          responseMessage = "$appName खोला जा रहा है...",
          data = mapOf("package" to directPackage)
        )
      }
    }

    // Try finding by installed applications label
    try {
      val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
      for (app in installedApps) {
        val label = pm.getApplicationLabel(app).toString().lowercase()
        if (label.contains(appName) || appName.contains(label)) {
          val intent = pm.getLaunchIntentForPackage(app.packageName)
          if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return ActionResult.Success(
              responseMessage = "${pm.getApplicationLabel(app)} खोला जा रहा है...",
              data = mapOf("package" to app.packageName)
            )
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed searching installed applications", e)
    }

    return ActionResult.Failure(
      reason = "यह app ($appName) आपके फोन में installed नहीं है।",
      errorType = "APP_NOT_INSTALLED"
    )
  }

  private fun handleOpenYouTube(context: Context, request: ActionRequest): ActionResult {
    val query = request.parameters["query"]?.trim()
    val pm = context.packageManager

    if (!query.isNullOrBlank()) {
      val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
        setPackage("com.google.android.youtube")
        putExtra("query", query)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      return if (searchIntent.resolveActivity(pm) != null) {
        context.startActivity(searchIntent)
        ActionResult.Success(
          responseMessage = "YouTube पर '$query' खोजा जा रहा है...",
          data = mapOf("query" to query)
        )
      } else {
        // Fallback to browser YouTube URL
        val webIntent = Intent(
          Intent.ACTION_VIEW,
          Uri.parse("https://www.youtube.com/results?search_query=${URLEncoder.encode(query, "UTF-8")}")
        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        context.startActivity(webIntent)
        ActionResult.Success(
          responseMessage = "ब्राउज़र में YouTube पर '$query' खोला जा रहा है...",
          data = mapOf("query" to query)
        )
      }
    }

    val launchIntent = pm.getLaunchIntentForPackage("com.google.android.youtube")
    return if (launchIntent != null) {
      launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(launchIntent)
      ActionResult.Success("YouTube खोला जा रहा है...")
    } else {
      val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(webIntent)
      ActionResult.Success("YouTube वेब पेज खोला जा रहा है...")
    }
  }

  private fun handleOpenCamera(context: Context, request: ActionRequest): ActionResult {
    val mode = request.parameters["mode"]?.lowercase() ?: "photo"
    val intent = if (mode == "video") {
      Intent(MediaStore.ACTION_VIDEO_CAPTURE)
    } else {
      Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
    }.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

    return if (intent.resolveActivity(context.packageManager) != null) {
      context.startActivity(intent)
      ActionResult.Success("Camera खोला जा रहा है...")
    } else {
      ActionResult.Failure("Camera app खोलने में असमर्थ।", "CAMERA_UNAVAILABLE")
    }
  }

  private fun handleOpenGallery(context: Context, request: ActionRequest): ActionResult {
    val intent = Intent(Intent.ACTION_VIEW).apply {
      type = "image/*"
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    return if (intent.resolveActivity(context.packageManager) != null) {
      context.startActivity(intent)
      ActionResult.Success("Gallery / Photos खोली जा रही है...")
    } else {
      val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      if (pickIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(pickIntent)
        ActionResult.Success("Gallery खोली जा रही है...")
      } else {
        ActionResult.Failure("Gallery app उपलब्ध नहीं है।", "GALLERY_UNAVAILABLE")
      }
    }
  }

  private fun handleOpenSettings(context: Context, request: ActionRequest): ActionResult {
    val settingType = request.parameters["setting_type"]?.lowercase() ?: "main"
    val intent = when (settingType) {
      "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
      "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
      "location" -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
      "sound", "volume" -> Intent(Settings.ACTION_SOUND_SETTINGS)
      "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
      "app" -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
      }
      else -> Intent(Settings.ACTION_SETTINGS)
    }.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

    context.startActivity(intent)
    return ActionResult.Success("$settingType settings खोली जा रही है...")
  }

  private fun handleOpenWhatsApp(context: Context, request: ActionRequest): ActionResult {
    val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
    return if (launchIntent != null) {
      launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(launchIntent)
      ActionResult.Success("WhatsApp खोला जा रहा है...")
    } else {
      ActionResult.Failure("यह app (WhatsApp) आपके फोन में installed नहीं है।", "APP_NOT_INSTALLED")
    }
  }

  // ==========================================
  // Communication Handlers
  // ==========================================

  private fun handleMakeCall(context: Context, request: ActionRequest): ActionResult {
    val contactName = request.parameters["contact_name"]?.trim()
    val directPhone = request.parameters["phone_number"]?.trim()

    if (contactName.isNullOrBlank() && directPhone.isNullOrBlank()) {
      return ActionResult.NeedsMoreInfo(
        question = "किसे कॉल करना चाहते हैं?",
        missingFields = listOf("contact_name")
      )
    }

    if (!directPhone.isNullOrBlank()) {
      return dispatchCallIntent(context, directPhone, contactName ?: directPhone)
    }

    // Resolve contact name from device contacts
    if (!PermissionManager.hasContactsPermission(context)) {
      return ActionResult.NeedsPermission(
        permission = PermissionManager.PERM_READ_CONTACTS,
        explanation = PermissionManager.getExplanation(PermissionManager.PERM_READ_CONTACTS),
        pendingAction = request
      )
    }

    val matches = ContactResolver.searchContacts(context, contactName!!)
    return when {
      matches.isEmpty() -> {
        // Offer dialer fallback for unresolved contact name
        ActionResult.Failure(
          reason = "'$contactName' नाम का कोई संपर्क आपकी Contact List में नहीं मिला।",
          errorType = "CONTACT_NOT_FOUND"
        )
      }
      matches.size == 1 -> {
        val contact = matches.first()
        dispatchCallIntent(context, contact.phoneNumber, contact.name)
      }
      else -> {
        // Multiple matches -> NeedsPicker
        val options = matches.map {
          PickerOption(
            id = it.id,
            title = it.name,
            subtitle = "${it.phoneNumber} (${it.type})",
            extra = mapOf("phone_number" to it.phoneNumber, "contact_name" to it.name)
          )
        }
        ActionResult.NeedsPicker(
          prompt = "कौन-से $contactName को call करूँ?",
          options = options,
          pendingAction = request
        )
      }
    }
  }

  private fun dispatchCallIntent(context: Context, phoneNumber: String, displayName: String): ActionResult {
    val cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
    val hasCallPerm = PermissionManager.hasCallPermission(context)

    val intent = if (hasCallPerm) {
      Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanPhone"))
    } else {
      Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
    }.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

    return try {
      context.startActivity(intent)
      val actionDesc = if (hasCallPerm) "कॉल लगाई जा रही है" else "डायलर खोला गया"
      ActionResult.Success(
        responseMessage = "$displayName ($cleanPhone) को $actionDesc...",
        data = mapOf("contact" to displayName, "phone" to cleanPhone)
      )
    } catch (e: Exception) {
      ActionResult.Failure("कॉल लगाने में त्रुटि: ${e.localizedMessage}", "CALL_FAILED")
    }
  }

  private fun handleSendSms(context: Context, request: ActionRequest): ActionResult {
    val recipient = request.parameters["recipient"]?.trim()
    val message = request.parameters["message"]?.trim()

    if (recipient.isNullOrBlank()) {
      return ActionResult.NeedsMoreInfo(
        question = "किसे SMS भेजना चाहते हैं?",
        missingFields = listOf("recipient")
      )
    }

    if (message.isNullOrBlank()) {
      return ActionResult.NeedsMoreInfo(
        question = "क्या संदेश भेजना है?",
        missingFields = listOf("message"),
        partialAction = request
      )
    }

    // Resolve recipient phone number
    var phoneNumber = recipient
    var displayName = recipient

    if (PermissionManager.hasContactsPermission(context) && !recipient.matches(Regex("""\+?\d{7,15}"""))) {
      val matches = ContactResolver.searchContacts(context, recipient)
      if (matches.size == 1) {
        phoneNumber = matches.first().phoneNumber
        displayName = matches.first().name
      } else if (matches.size > 1) {
        val options = matches.map {
          PickerOption(
            id = it.id,
            title = it.name,
            subtitle = "${it.phoneNumber} (${it.type})",
            extra = mapOf("recipient" to it.phoneNumber, "message" to message)
          )
        }
        return ActionResult.NeedsPicker(
          prompt = "किसे SMS भेजें?",
          options = options,
          pendingAction = request
        )
      }
    }

    val cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
      data = Uri.parse("smsto:$cleanPhone")
      putExtra("sms_body", message)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    return if (smsIntent.resolveActivity(context.packageManager) != null) {
      context.startActivity(smsIntent)
      ActionResult.Success(
        responseMessage = "$displayName को SMS भेजने के लिए स्क्रीन खोली गई: \"$message\"",
        data = mapOf("recipient" to displayName, "phone" to cleanPhone, "message" to message)
      )
    } else {
      ActionResult.Failure("SMS app उपलब्ध नहीं है।", "SMS_APP_UNAVAILABLE")
    }
  }

  private fun handleSendWhatsAppMessage(context: Context, request: ActionRequest): ActionResult {
    val recipient = request.parameters["recipient"]?.trim()
    val message = request.parameters["message"]?.trim()

    if (recipient.isNullOrBlank()) {
      return ActionResult.NeedsMoreInfo(
        question = "WhatsApp पर किसे मैसेज भेजना है?",
        missingFields = listOf("recipient")
      )
    }

    if (message.isNullOrBlank()) {
      return ActionResult.NeedsMoreInfo(
        question = "क्या message भेजना है?",
        missingFields = listOf("message"),
        partialAction = request
      )
    }

    // Check if WhatsApp is installed
    val isInstalled = context.packageManager.getLaunchIntentForPackage("com.whatsapp") != null
    if (!isInstalled) {
      return ActionResult.Failure(
        reason = "WhatsApp आपके फोन में installed नहीं है।",
        errorType = "APP_NOT_INSTALLED"
      )
    }

    // Resolve contact number if possible
    var phoneNumber = recipient
    var displayName = recipient
    if (PermissionManager.hasContactsPermission(context) && !recipient.matches(Regex("""\+?\d{7,15}"""))) {
      val matches = ContactResolver.searchContacts(context, recipient)
      if (matches.size == 1) {
        phoneNumber = matches.first().phoneNumber
        displayName = matches.first().name
      } else if (matches.size > 1) {
        val options = matches.map {
          PickerOption(
            id = it.id,
            title = it.name,
            subtitle = "${it.phoneNumber} (${it.type})",
            extra = mapOf("recipient" to it.phoneNumber, "message" to message)
          )
        }
        return ActionResult.NeedsPicker(
          prompt = "कौन-से $recipient को WhatsApp मैसेज भेजें?",
          options = options,
          pendingAction = request
        )
      }
    }

    val cleanDigits = phoneNumber.replace(Regex("[^0-9]"), "")
    val intent = if (cleanDigits.length >= 10) {
      // Direct chat compose URL
      val url = "https://api.whatsapp.com/send?phone=$cleanDigits&text=${URLEncoder.encode(message, "UTF-8")}"
      Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        setPackage("com.whatsapp")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    } else {
      // General share to WhatsApp flow
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        setPackage("com.whatsapp")
        putExtra(Intent.EXTRA_TEXT, message)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    }

    return try {
      context.startActivity(intent)
      ActionResult.Success(
        responseMessage = "WhatsApp पर $displayName को मैसेज तैयार है: \"$message\"",
        data = mapOf("recipient" to displayName, "message" to message)
      )
    } catch (e: Exception) {
      ActionResult.Failure("WhatsApp खोलने में त्रुटि: ${e.localizedMessage}", "WHATSAPP_ERROR")
    }
  }

  // ==========================================
  // Alarms, Timers & Productivity Handlers
  // ==========================================

  private fun handleSetAlarm(context: Context, request: ActionRequest): ActionResult {
    val timeStr = request.parameters["time"]?.trim()
    val label = request.parameters["label"]?.trim() ?: "Gamak Alarm"
    val relativeMinutes = request.parameters["relative_minutes"]?.toIntOrNull()

    var (hour, minute) = parseHourAndMinute(timeStr, relativeMinutes)

    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
      putExtra(AlarmClock.EXTRA_HOUR, hour)
      putExtra(AlarmClock.EXTRA_MINUTES, minute)
      putExtra(AlarmClock.EXTRA_MESSAGE, label)
      putExtra(AlarmClock.EXTRA_SKIP_UI, false)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    return if (intent.resolveActivity(context.packageManager) != null) {
      try {
        context.startActivity(intent)
        ActionResult.Success(
          responseMessage = "$formattedTime के लिए '$label' अलार्म सेट कर दिया गया है।",
          data = mapOf("time" to formattedTime, "label" to label)
        )
      } catch (e: Exception) {
        ActionResult.Failure("अलार्म लगाने में त्रुटि: ${e.localizedMessage}", "ALARM_ERROR")
      }
    } else {
      // Fallback: Schedule using native AlarmManager & ReminderBroadcastReceiver
      scheduleExactLocalNotification(
        context = context,
        title = "Alarm: $label",
        message = "Alarm scheduled for $formattedTime",
        hour = hour,
        minute = minute
      )
      ActionResult.Success(
        responseMessage = "$formattedTime के लिए अलार्म शेड्यूल कर दिया गया है।",
        data = mapOf("time" to formattedTime, "label" to label)
      )
    }
  }

  private fun handleSetTimer(context: Context, request: ActionRequest): ActionResult {
    val minutes = request.parameters["duration_minutes"]?.toIntOrNull()
      ?: (request.parameters["duration_seconds"]?.toIntOrNull()?.div(60) ?: 5)
    val seconds = (minutes * 60).coerceAtLeast(1)
    val label = request.parameters["label"]?.trim() ?: "Gamak Timer"

    val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
      putExtra(AlarmClock.EXTRA_LENGTH, seconds)
      putExtra(AlarmClock.EXTRA_MESSAGE, label)
      putExtra(AlarmClock.EXTRA_SKIP_UI, false)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    return if (intent.resolveActivity(context.packageManager) != null) {
      try {
        context.startActivity(intent)
        ActionResult.Success(
          responseMessage = "$minutes मिनट का टाइमर शुरू कर दिया गया है।",
          data = mapOf("minutes" to minutes.toString(), "label" to label)
        )
      } catch (e: Exception) {
        ActionResult.Failure("टाइमर शुरू करने में त्रुटि: ${e.localizedMessage}", "TIMER_ERROR")
      }
    } else {
      ActionResult.Failure("डिवाइस में Timer app उपलब्ध नहीं है।", "TIMER_UNAVAILABLE")
    }
  }

  private fun handleSetReminder(context: Context, request: ActionRequest): ActionResult {
    val title = request.parameters["title"]?.trim()
    val timeStr = request.parameters["time"]?.trim()
    val dateStr = request.parameters["date"]?.trim() ?: "today"

    if (title.isNullOrBlank()) {
      return ActionResult.NeedsMoreInfo(
        question = "किस बात का रिमाइंडर लगाना है?",
        missingFields = listOf("title")
      )
    }

    if (timeStr.isNullOrBlank()) {
      return ActionResult.NeedsMoreInfo(
        question = "किस समय याद दिलाऊँ?",
        missingFields = listOf("time"),
        partialAction = request
      )
    }

    if (!PermissionManager.hasNotificationPermission(context)) {
      return ActionResult.NeedsPermission(
        permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          android.Manifest.permission.POST_NOTIFICATIONS
        } else "",
        explanation = "रिमाइंडर का नोटिफिकेशन दिखाने के लिए अनुमति चाहिए।",
        pendingAction = request
      )
    }

    val (hour, minute) = parseHourAndMinute(timeStr, null)
    scheduleExactLocalNotification(
      context = context,
      title = "रिमाइंडर: $title",
      message = "$timeStr बजे: $title",
      hour = hour,
      minute = minute
    )

    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    return ActionResult.Success(
      responseMessage = "$formattedTime बजे के लिए रिमाइंडर सेट हो गया: \"$title\"",
      data = mapOf("title" to title, "time" to formattedTime, "date" to dateStr)
    )
  }

  private fun handleCreateCalendarEvent(context: Context, request: ActionRequest): ActionResult {
    val title = request.parameters["title"]?.trim()
    val timeStr = request.parameters["time"]?.trim()
    val dateStr = request.parameters["date"]?.trim() ?: "tomorrow"
    val durationMinutes = request.parameters["duration_minutes"]?.toLongOrNull() ?: 60L
    val location = request.parameters["location"]?.trim() ?: ""

    if (title.isNullOrBlank()) {
      return ActionResult.NeedsMoreInfo(
        question = "कैलेंडर इवेंट का क्या नाम रखना है?",
        missingFields = listOf("title")
      )
    }

    if (timeStr.isNullOrBlank()) {
      return ActionResult.NeedsMoreInfo(
        question = "इवेंट किस समय शुरू होना चाहिए?",
        missingFields = listOf("time"),
        partialAction = request
      )
    }

    val (hour, minute) = parseHourAndMinute(timeStr, null)
    val cal = Calendar.getInstance().apply {
      if (dateStr.contains("tomorrow") || dateStr.contains("कल") || dateStr.contains("भोलि")) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
    }

    val startMillis = cal.timeInMillis
    val endMillis = startMillis + (durationMinutes * 60 * 1000)

    val intent = Intent(Intent.ACTION_INSERT).apply {
      data = CalendarContract.Events.CONTENT_URI
      putExtra(CalendarContract.Events.TITLE, title)
      putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
      putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
      if (location.isNotBlank()) {
        putExtra(CalendarContract.Events.EVENT_LOCATION, location)
      }
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    return if (intent.resolveActivity(context.packageManager) != null) {
      try {
        context.startActivity(intent)
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        ActionResult.Success(
          responseMessage = "कैलेंडर में '$title' इवेंट तैयार कर दिया गया है ($formattedTime)।",
          data = mapOf("title" to title, "time" to formattedTime, "location" to location)
        )
      } catch (e: Exception) {
        ActionResult.Failure("कैलेंडर इवेंट बनाने में त्रुटि: ${e.localizedMessage}", "CALENDAR_ERROR")
      }
    } else {
      ActionResult.Failure("कैलेंडर app उपलब्ध नहीं है।", "CALENDAR_UNAVAILABLE")
    }
  }

  // ==========================================
  // Navigation & Weather Handlers
  // ==========================================

  private fun handleNavigateMaps(context: Context, request: ActionRequest): ActionResult {
    val destination = request.parameters["destination"]?.trim()
    if (destination.isNullOrBlank()) {
      return ActionResult.NeedsMoreInfo(
        question = "कहाँ जाने का रास्ता देखना चाहते हैं?",
        missingFields = listOf("destination")
      )
    }

    val encoded = URLEncoder.encode(destination, "UTF-8")
    val mode = request.parameters["mode"]?.lowercase() ?: "d" // driving default
    val modeCode = when (mode) {
      "walking", "walk", "पैदल" -> "w"
      "transit", "बस", "ट्रेन" -> "r"
      "bicycling", "साइकिल" -> "b"
      else -> "d"
    }

    val navUri = Uri.parse("google.navigation:q=$encoded&mode=$modeCode")
    val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
      setPackage("com.google.android.apps.maps")
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    return if (mapIntent.resolveActivity(context.packageManager) != null) {
      context.startActivity(mapIntent)
      ActionResult.Success(
        responseMessage = "$destination के लिए Google Maps नेविगेशन शुरू किया जा रहा है...",
        data = mapOf("destination" to destination)
      )
    } else {
      // Fallback to geo URI or browser maps
      val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded")).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      if (geoIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(geoIntent)
        ActionResult.Success("$destination के लिए नक्शा खोला जा रहा है...")
      } else {
        val webIntent = Intent(
          Intent.ACTION_VIEW,
          Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$encoded")
        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        context.startActivity(webIntent)
        ActionResult.Success("ब्राउज़र में $destination का रास्ता खोला जा रहा है...")
      }
    }
  }

  private suspend fun handleGetWeather(context: Context, request: ActionRequest): ActionResult {
    val locationQuery = request.parameters["location"]?.trim() ?: "Current Location"
    val result = weatherService.fetchWeather(locationQuery, language = "hi")

    return result.fold(
      onSuccess = { info ->
        val tempFormatted = String.format(Locale.getDefault(), "%.1f°C", info.temperature)
        val highLow = if (info.highTemp != null && info.lowTemp != null) {
          " (अधिकतम: ${info.highTemp.toInt()}°C, न्यूनतम: ${info.lowTemp.toInt()}°C)"
        } else ""

        val response = "${info.locationName} में वर्तमान तापमान $tempFormatted है। मौसम: ${info.conditionDescription}$highLow। नमी: ${info.humidity}%, हवा: ${info.windSpeed} km/h।"
        ActionResult.Success(
          responseMessage = response,
          data = mapOf(
            "location" to info.locationName,
            "temperature" to tempFormatted,
            "condition" to info.conditionDescription,
            "humidity" to "${info.humidity}%"
          )
        )
      },
      onFailure = { e ->
        ActionResult.Failure(
          reason = "मौसम की जानकारी प्राप्त नहीं हो सकी: ${e.localizedMessage}",
          errorType = "WEATHER_FETCH_ERROR"
        )
      }
    )
  }

  // ==========================================
  // Media & Sharing Handlers
  // ==========================================

  private fun handlePlayMusic(context: Context, request: ActionRequest): ActionResult {
    val query = request.parameters["query"]?.trim()

    if (!query.isNullOrBlank()) {
      val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
        putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
        putExtra(SearchManager.QUERY, query)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      return if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
        ActionResult.Success("'$query' चलाया जा रहा है...", mapOf("query" to query))
      } else {
        // Fallback to YouTube or general music
        handleOpenYouTube(context, ActionRequest("open_youtube", mapOf("query" to "$query song")))
      }
    }

    // Toggle media play button via AudioManager
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    if (audioManager != null) {
      val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
      val eventUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
      audioManager.dispatchMediaKeyEvent(eventDown)
      audioManager.dispatchMediaKeyEvent(eventUp)
      return ActionResult.Success("संगीत शुरू किया गया।")
    }

    return ActionResult.Failure("Media playback उपलब्ध नहीं है।", "MEDIA_UNAVAILABLE")
  }

  private fun handleMediaControl(context: Context, request: ActionRequest): ActionResult {
    val command = request.parameters["command"]?.lowercase()?.trim() ?: "toggle"
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
      ?: return ActionResult.Failure("Audio service उपलब्ध नहीं है।", "AUDIO_SERVICE_UNAVAILABLE")

    val keyCode = when (command) {
      "play", "resume", "शुरू" -> KeyEvent.KEYCODE_MEDIA_PLAY
      "pause", "stop", "रोको", "बन्द" -> KeyEvent.KEYCODE_MEDIA_PAUSE
      "next", "अगला", "अर्को" -> KeyEvent.KEYCODE_MEDIA_NEXT
      "previous", "prev", "पिछला" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
      else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
    }

    val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
    val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
    audioManager.dispatchMediaKeyEvent(eventDown)
    audioManager.dispatchMediaKeyEvent(eventUp)

    val desc = when (command) {
      "pause" -> "संगीत रोक दिया गया (Paused)"
      "next" -> "अगला गाना (Next Track)"
      "previous" -> "पिछला गाना (Previous Track)"
      else -> "Media प्लेबैक अपडेट किया गया"
    }

    return ActionResult.Success(desc, mapOf("command" to command))
  }

  private fun handleShareContent(context: Context, request: ActionRequest): ActionResult {
    val text = request.parameters["text"]?.trim() ?: "Gamak AI Assistant"
    val targetApp = request.parameters["target_app"]?.lowercase()?.trim()

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, text)
      if (targetApp == "whatsapp") {
        setPackage("com.whatsapp")
      }
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val chooser = Intent.createChooser(sendIntent, "Share via").apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    return try {
      context.startActivity(chooser)
      ActionResult.Success("शेयर शीट खोली गई।", mapOf("text" to text))
    } catch (e: Exception) {
      ActionResult.Failure("सामग्री साझा करने में असमर्थ।", "SHARE_ERROR")
    }
  }

  // ==========================================
  // Helper Functions
  // ==========================================

  private fun parseHourAndMinute(timeStr: String?, relativeMinutes: Int?): Pair<Int, Int> {
    if (relativeMinutes != null && relativeMinutes > 0) {
      val cal = Calendar.getInstance().apply {
        add(Calendar.MINUTE, relativeMinutes)
      }
      return Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    if (timeStr.isNullOrBlank()) {
      return Pair(7, 0) // Default 7:00 AM
    }

    val isPm = timeStr.contains("pm", ignoreCase = true) ||
      timeStr.contains("शाम", ignoreCase = true) ||
      timeStr.contains("रात", ignoreCase = true) ||
      timeStr.contains("beluka", ignoreCase = true)

    val isAm = timeStr.contains("am", ignoreCase = true) ||
      timeStr.contains("सुबह", ignoreCase = true) ||
      timeStr.contains("बिहान", ignoreCase = true)

    val match = Regex("""(\d{1,2})(?::(\d{2}))?""").find(timeStr)
    if (match != null) {
      var hour = match.groupValues[1].toIntOrNull() ?: 7
      val minute = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0

      if (isPm && hour < 12) hour += 12
      if (isAm && hour == 12) hour = 0
      return Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    return Pair(7, 0)
  }

  private fun scheduleExactLocalNotification(
    context: Context,
    title: String,
    message: String,
    hour: Int,
    minute: Int
  ) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
      if (before(Calendar.getInstance())) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }

    val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
      putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, title)
      putExtra(ReminderBroadcastReceiver.EXTRA_MESSAGE, message)
      putExtra(ReminderBroadcastReceiver.EXTRA_NOTIFICATION_ID, (System.currentTimeMillis() % 100000).toInt())
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      cal.timeInMillis.toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
      } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
      }
    } catch (e: SecurityException) {
      Log.w(TAG, "Exact alarm permission restricted, falling back to inexact alarm", e)
      alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
    }
  }
}
