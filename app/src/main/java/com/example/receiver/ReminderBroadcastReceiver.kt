package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.platform.PermissionManager

class ReminderBroadcastReceiver : BroadcastReceiver() {

  companion object {
    const val CHANNEL_ID = "gamak_reminders_channel"
    const val EXTRA_TITLE = "extra_reminder_title"
    const val EXTRA_MESSAGE = "extra_reminder_message"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
  }

  override fun onReceive(context: Context, intent: Intent?) {
    val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Gamak AI Reminder"
    val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: "You have a scheduled reminder."
    val notificationId = intent?.getIntExtra(EXTRA_NOTIFICATION_ID, 1001) ?: 1001

    createNotificationChannel(context)

    if (!PermissionManager.hasNotificationPermission(context)) {
      return
    }

    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(message))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setDefaults(NotificationCompat.DEFAULT_ALL)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    try {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    } catch (e: SecurityException) {
      // Notification permission not granted
    }
  }

  private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "Gamak Reminders & Alarms"
      val descriptionText = "Notifications for Gamak AI alarms and reminders"
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
        description = descriptionText
        enableVibration(true)
      }
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }
}
