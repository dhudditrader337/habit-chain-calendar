package com.habitchain.calendar.data.local

import android.content.Context

class PreferencesStore(private val context: Context) : SnapshotStore {
    private val preferences by lazy { context.getSharedPreferences("habit_chain_calendar", Context.MODE_PRIVATE) }
    override fun read(): String? = preferences.getString("app_state_v1", null)
    override fun write(value: String): Boolean = preferences.edit().putString("app_state_v1", value).commit()
}
