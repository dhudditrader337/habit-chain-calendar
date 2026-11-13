package com.habitchain.calendar.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.habitchain.calendar.data.local.PreferencesStore
import com.habitchain.calendar.data.local.SnapshotCodec
import java.util.concurrent.Executors

internal object ReceiverWorker {
    val executor = Executors.newSingleThreadExecutor()
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        ReceiverWorker.executor.execute {
            try {
                val snapshot = SnapshotCodec.decode(PreferencesStore(context.applicationContext).read())
                ReminderScheduler(context.applicationContext).deliver(snapshot, intent.getLongExtra("due", 0L))
            } catch (_: Exception) {
                context.getSystemService(android.app.NotificationManager::class.java).cancel(41)
            } finally { pending.finish() }
        }
    }
}
