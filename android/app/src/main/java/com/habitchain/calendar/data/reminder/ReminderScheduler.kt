package com.habitchain.calendar.data.reminder

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.habitchain.calendar.MainActivity
import com.habitchain.calendar.R
import com.habitchain.calendar.domain.model.AppSnapshot
import java.time.*

class ReminderScheduler(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val alarm = context.getSystemService(AlarmManager::class.java)
    private fun pending(time: Long = 0L) = PendingIntent.getBroadcast(context, 41,
        Intent(context, ReminderReceiver::class.java).putExtra("due", time), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun allowed(): Boolean = manager.areNotificationsEnabled() && (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    @Synchronized
    fun reschedule(snapshot: AppSnapshot, clearNotification: Boolean = true) {
        alarm.cancel(pending())
        if (clearNotification) manager.cancel(41)
        if (!snapshot.settings.remindersEnabled || !allowed()) { manager.cancel(41); return }
        manager.createNotificationChannel(NotificationChannel("habit_evening", context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val now = ZonedDateTime.now()
        val today = now.toLocalDate()
        val next = snapshot.habits.filter { it.reminderEnabled }.map { habit ->
            val minutes = habit.customReminderMinutes ?: snapshot.settings.defaultReminderMinutes
            var candidate = today.atTime(minutes / 60, minutes % 60).atZone(now.zone)
            if (!candidate.isAfter(now) || today in snapshot.history(habit.id).completed) candidate = candidate.plusDays(1)
            candidate.toInstant().toEpochMilli()
        }.minOrNull()
        if (next != null) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending(next))
        if (snapshot.habits.none { it.reminderEnabled && today !in snapshot.history(it.id).completed }) manager.cancel(41)
    }

    fun deliver(snapshot: AppSnapshot, due: Long) {
        val now = ZonedDateTime.now()
        val scheduled = Instant.ofEpochMilli(due).atZone(now.zone)
        if (allowed() && snapshot.settings.remindersEnabled && scheduled.toLocalDate() == now.toLocalDate() && due <= System.currentTimeMillis()) {
            val minute = scheduled.hour * 60 + scheduled.minute
            val habits = snapshot.habits.filter { it.reminderEnabled && (it.customReminderMinutes ?: snapshot.settings.defaultReminderMinutes) == minute && now.toLocalDate() !in snapshot.history(it.id).completed }
            if (habits.isNotEmpty()) {
                val intent = PendingIntent.getActivity(context, 42, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val publicVersion = Notification.Builder(context, "habit_evening").setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(context.getString(R.string.reminder_title)).setContentText(context.getString(R.string.reminder_public)).build()
                val text = if (habits.size == 1) context.getString(R.string.reminder_single, habits.first().name) else context.getString(R.string.reminder_multiple, habits.size)
                val notification = Notification.Builder(context, "habit_evening").setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(context.getString(R.string.reminder_title)).setContentText(text).setStyle(Notification.BigTextStyle().bigText(text))
                    .setContentIntent(intent).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(publicVersion).build()
                manager.notify(41, notification)
            }
        }
        reschedule(snapshot, clearNotification = false)
    }
}
