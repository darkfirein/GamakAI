package com.example.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionManager {

  const val PERM_RECORD_AUDIO = Manifest.permission.RECORD_AUDIO
  const val PERM_READ_CONTACTS = Manifest.permission.READ_CONTACTS
  const val PERM_CALL_PHONE = Manifest.permission.CALL_PHONE
  const val PERM_SEND_SMS = Manifest.permission.SEND_SMS
  const val PERM_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
  const val PERM_READ_CALENDAR = Manifest.permission.READ_CALENDAR
  const val PERM_WRITE_CALENDAR = Manifest.permission.WRITE_CALENDAR

  fun hasPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
  }

  fun hasAudioPermission(context: Context): Boolean = hasPermission(context, PERM_RECORD_AUDIO)

  fun hasContactsPermission(context: Context): Boolean = hasPermission(context, PERM_READ_CONTACTS)

  fun hasCallPermission(context: Context): Boolean = hasPermission(context, PERM_CALL_PHONE)

  fun hasSmsPermission(context: Context): Boolean = hasPermission(context, PERM_SEND_SMS)

  fun hasLocationPermission(context: Context): Boolean {
    return hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
      hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    } else {
      true
    }
  }

  fun hasCalendarPermission(context: Context): Boolean {
    return hasPermission(context, PERM_WRITE_CALENDAR) && hasPermission(context, PERM_READ_CALENDAR)
  }

  fun getExplanation(permission: String): String {
    return when (permission) {
      PERM_RECORD_AUDIO -> "आवाज सुनने और कमांड प्रोसेस करने के लिए माइक्रोफोन अनुमति आवश्यक है।"
      PERM_READ_CONTACTS -> "संपर्क (Contacts) ढूँढने और सही व्यक्ति को कॉल या मैसेज करने के लिए Contacts अनुमति चाहिए।"
      PERM_CALL_PHONE -> "सीधे फोन कॉल लगाने के लिए Phone अनुमति आवश्यक है।"
      PERM_SEND_SMS -> "SMS संदेश भेजने के लिए SMS अनुमति आवश्यक है।"
      PERM_LOCATION -> "सटीक मौसम और नेविगेशन दिशाओं के लिए Location अनुमति आवश्यक है।"
      PERM_WRITE_CALENDAR, PERM_READ_CALENDAR -> "कैलेंडर में इवेंट और मीटिंग्स जोड़ने के लिए Calendar अनुमति आवश्यक है।"
      Manifest.permission.POST_NOTIFICATIONS -> "अलार्म और रिमाइंडर सूचनाएं (Notifications) दिखाने के लिए अनुमति आवश्यक है।"
      else -> "इस कार्य को सुरक्षित रूप से पूरा करने के लिए अनुमति आवश्यक है।"
    }
  }
}
