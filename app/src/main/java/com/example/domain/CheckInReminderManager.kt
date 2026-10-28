package com.example.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.WeeklyCheckInPreferences
import java.util.Calendar

object CheckInReminderManager {

    private const val CHANNEL_ID = "weekly_check_in_channel"
    private const val CHANNEL_NAME = "Weekend Weight Check-In Reminders"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for weekly weight check-in and goal progress review"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showCheckInNotification(context: Context, isMissedReminder: Boolean = false) {
        ensureNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_NAVIGATE_TO", "weekly_check_in")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isMissedReminder) {
            "Weight Check-In Reminder ⚖️"
        } else {
            "Time for Your Weekend Check-In! 🌟"
        }

        val body = if (isMissedReminder) {
            "Whenever you're ready, log your current weight to review your weekly progress and update your 7-day plan."
        } else {
            "Log your weight today to celebrate your weekly progress, track healthy habits, and get your AI coach plan!"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(if (isMissedReminder) 1002 else 1001, builder.build())
    }

    fun calculateNextCheckInMillis(
        preferredDay: String, // "Saturday" or "Sunday"
        hour: Int,
        minute: Int
    ): Long {
        val calendar = Calendar.getInstance()
        val targetDayOfWeek = if (preferredDay.equals("Saturday", ignoreCase = true)) {
            Calendar.SATURDAY
        } else {
            Calendar.SUNDAY
        }

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        var daysDifference = targetDayOfWeek - currentDayOfWeek

        if (daysDifference < 0 || (daysDifference == 0 && System.currentTimeMillis() >= calendar.timeInMillis)) {
            daysDifference += 7
        }

        calendar.add(Calendar.DAY_OF_YEAR, daysDifference)
        return calendar.timeInMillis
    }
}
