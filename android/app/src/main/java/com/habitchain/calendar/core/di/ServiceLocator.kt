package com.habitchain.calendar.core.di

import android.content.Context
import com.habitchain.calendar.R
import com.habitchain.calendar.data.local.PreferencesStore
import com.habitchain.calendar.data.reminder.ReminderScheduler
import com.habitchain.calendar.data.repository.HabitRepositoryImpl
import com.habitchain.calendar.data.sample.SampleData
import com.habitchain.calendar.data.time.SystemLocalDateProvider
import com.habitchain.calendar.domain.model.SessionResults

object ServiceLocator {
    private var configured: AppViewModelFactory? = null
    val factory: AppViewModelFactory get() = requireNotNull(configured)

    @Synchronized
    fun initialize(context: Context) {
        if (configured != null) return
        val app = context.applicationContext
        val clock = SystemLocalDateProvider()
        val reminders = ReminderScheduler(app)
        val repository = HabitRepositoryImpl(PreferencesStore(app), clock, { reminders.reschedule(it) })
        val names = listOf(R.string.sample_read, R.string.sample_spanish, R.string.sample_stretch).map { app.getString(it) }
        val notes = listOf(R.string.sample_note_read, R.string.sample_note_spanish, R.string.sample_note_stretch).map { app.getString(it) }
        val version = app.packageManager.getPackageInfo(app.packageName, 0).versionName.orEmpty()
        configured = AppViewModelFactory(repository, clock, { date -> SampleData.snapshot(date, names, notes) }, SessionResults(), reminders::allowed, version)
    }
}
