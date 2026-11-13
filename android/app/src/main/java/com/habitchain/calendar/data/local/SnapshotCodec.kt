package com.habitchain.calendar.data.local

import com.habitchain.calendar.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object SnapshotCodec {
    fun encode(snapshot: AppSnapshot): String = JSONObject().apply {
        put("schemaVersion", 1)
        put("habits", JSONArray().apply {
            snapshot.habits.forEach { habit -> put(JSONObject().apply {
                put("id", habit.id)
                put("name", habit.name)
                put("color", habit.color.name)
                put("createdOn", habit.createdOn.toString())
                put("reminder", habit.reminderEnabled)
                put("custom", habit.customReminderMinutes ?: JSONObject.NULL)
                val history = snapshot.history(habit.id)
                put("completed", JSONArray(history.completed.sorted().map { it.toString() }))
                put("notes", JSONObject().apply { history.notes.forEach { (date, note) -> put(date.toString(), note) } })
            }) }
        })
        put("settings", JSONObject().apply {
            put("minutes", snapshot.settings.defaultReminderMinutes)
            put("enabled", snapshot.settings.remindersEnabled)
        })
        put("feedback", snapshot.feedback)
        snapshot.lastResult?.let { put("result", encodeResult(it)) }
    }.toString()

    fun decode(value: String?): AppSnapshot {
        if (value == null) return AppSnapshot()
        val root = JSONObject(value)
        require(root.getInt("schemaVersion") == 1)
        val habits = mutableListOf<Habit>()
        val histories = mutableMapOf<String, HabitHistory>()
        val array = root.getJSONArray("habits")
        require(array.length() <= 30)
        repeat(array.length()) { index ->
            val item = array.getJSONObject(index)
            val id = item.getString("id")
            val name = item.getString("name")
            val custom = if (item.isNull("custom")) null else item.getInt("custom")
            require(id.isNotBlank() && id.length <= 100 && habits.none { it.id == id })
            require(name.isNotBlank() && name.length <= 40 && habits.none { it.name.equals(name, true) })
            require(custom == null || custom in 0..1439)
            val habit = Habit(id, name, HabitColor.valueOf(item.getString("color")), LocalDate.parse(item.getString("createdOn")), item.optBoolean("reminder"), custom)
            val completed = item.getJSONArray("completed")
            val dates = (0 until completed.length()).map { LocalDate.parse(completed.getString(it)) }.toSet()
            require(dates.all { !it.isBefore(habit.createdOn) })
            val notesObject = item.getJSONObject("notes")
            val notes = notesObject.keys().asSequence().associate { key ->
                val date = LocalDate.parse(key)
                val note = notesObject.getString(key)
                require(note.length <= 280 && !date.isBefore(habit.createdOn))
                date to note
            }
            habits.add(habit)
            histories[id] = HabitHistory(dates, notes)
        }
        val settings = root.getJSONObject("settings")
        val minutes = settings.getInt("minutes")
        require(minutes in 0..1439)
        val feedback = root.optString("feedback")
        require(feedback.length <= 500)
        return AppSnapshot(habits, histories, AppSettings(minutes, settings.getBoolean("enabled")), root.optJSONObject("result")?.let { decodeResult(it) }, feedback)
    }

    private fun encodeResult(result: ResultSnapshot) = JSONObject().apply {
        put("id", result.sessionId)
        put("date", result.date.toString())
        put("marked", result.markedCount)
        put("total", result.totalCount)
        put("already", result.alreadyCount)
        put("changed", JSONArray(result.changedNames))
        put("pending", JSONArray(result.pendingNames))
        put("current", result.currentChain)
        put("longest", result.longestChain)
    }

    private fun decodeResult(item: JSONObject): ResultSnapshot {
        fun names(key: String): List<String> {
            val array = item.getJSONArray(key)
            return (0 until array.length()).map { array.getString(it) }
        }
        val marked = item.getInt("marked")
        val total = item.getInt("total")
        val already = item.getInt("already")
        require(total in 0..30 && marked in 0..total && already in 0..marked)
        return ResultSnapshot(item.getString("id"), LocalDate.parse(item.getString("date")), SessionMode.Real, marked, total, already, names("changed"), names("pending"), item.getInt("current").coerceAtLeast(0), item.getInt("longest").coerceAtLeast(0))
    }
}
