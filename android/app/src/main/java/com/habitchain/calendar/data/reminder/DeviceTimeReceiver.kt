package com.habitchain.calendar.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.habitchain.calendar.data.local.PreferencesStore
import com.habitchain.calendar.data.local.SnapshotCodec

class DeviceTimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        ReceiverWorker.executor.execute {
            try {
                val snapshot = SnapshotCodec.decode(PreferencesStore(context.applicationContext).read())
                ReminderScheduler(context.applicationContext).reschedule(snapshot)
            } catch (_: Exception) {
                context.getSystemService(android.app.AlarmManager::class.java).cancel(android.app.PendingIntent.getBroadcast(context, 41, Intent(context, ReminderReceiver::class.java), android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
            } finally { pending.finish() }
        }
    }
}
