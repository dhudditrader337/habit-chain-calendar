package com.habitchain.calendar.data.repository

import com.habitchain.calendar.data.local.SnapshotStore
import com.habitchain.calendar.data.local.SnapshotCodec
import com.habitchain.calendar.domain.model.*
import com.habitchain.calendar.domain.repository.HabitRepository
import com.habitchain.calendar.domain.time.LocalDateProvider
import com.habitchain.calendar.domain.usecase.ValidateHabit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate

class HabitRepositoryImpl(
    private val store: SnapshotStore,
    private val clock: LocalDateProvider,
    private val onSaved: (AppSnapshot) -> Unit,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : HabitRepository {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(StoreState(today = clock.today()))
    override val state = _state.asStateFlow()
    private var loaded = false

    override suspend fun load() = withContext(io) {
        mutex.withLock {
            _state.update { it.copy(loading = true) }
            try {
                val snapshot = SnapshotCodec.decode(store.read())
                loaded = true
                _state.update { it.copy(snapshot = snapshot, loading = false, error = false, today = clock.today()) }
                runCatching { onSaved(snapshot) }
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, error = true) }
            }
            Unit
        }
    }

    override suspend fun refreshDate() = withContext(io) {
        _state.update { it.copy(today = clock.today()) }
        if (loaded) runCatching { onSaved(_state.value.snapshot) }
        Unit
    }

    private suspend fun change(allowReset: Boolean = false, transform: (AppSnapshot) -> AppSnapshot) = withContext(io) {
        mutex.withLock {
            check(loaded || allowReset)
            check(!_state.value.error || allowReset)
            val next = transform(_state.value.snapshot)
            check(store.write(SnapshotCodec.encode(next)))
            loaded = true
            _state.update { it.copy(snapshot = next, error = false, loading = false, today = clock.today()) }
            runCatching { onSaved(next) }
            Unit
        }
    }

    override suspend fun saveHabit(habit: Habit) = change { snapshot ->
        require(ValidateHabit(habit.name, habit.id.takeIf { id -> snapshot.habits.any { it.id == id } }, snapshot.habits) == null)
        val previous = snapshot.habits.find { it.id == habit.id }
        val exists = previous != null
        snapshot.copy(
            habits = if (exists) snapshot.habits.map { if (it.id == habit.id) habit else it } else snapshot.habits + habit,
            histories = if (exists) snapshot.histories else snapshot.histories + (habit.id to HabitHistory()),
            settings = if (habit.reminderEnabled && previous?.reminderEnabled != true) snapshot.settings.copy(remindersEnabled = true) else snapshot.settings
        )
    }

    override suspend fun mark(id: String, date: LocalDate, completed: Boolean) = change { snapshot ->
        val habit = snapshot.habits.first { it.id == id }
        require(!date.isBefore(habit.createdOn) && !date.isAfter(clock.today()))
        val history = snapshot.history(id)
        snapshot.copy(histories = snapshot.histories + (id to history.copy(completed = if (completed) history.completed + date else history.completed - date)))
    }

    override suspend fun saveNote(id: String, date: LocalDate, note: String) = change { snapshot ->
        val habit = snapshot.habits.first { it.id == id }
        require(note.length <= 280 && !date.isBefore(habit.createdOn) && !date.isAfter(clock.today()))
        val history = snapshot.history(id)
        val notes = if (note.isBlank()) history.notes - date else history.notes + (date to note.trim())
        snapshot.copy(histories = snapshot.histories + (id to history.copy(notes = notes)))
    }

    override suspend fun deleteHabit(id: String) = change { it.copy(habits = it.habits.filterNot { habit -> habit.id == id }, histories = it.histories - id, lastResult = null) }
    override suspend fun saveSettings(settings: AppSettings) = change { require(settings.defaultReminderMinutes in 0..1439); it.copy(settings = settings) }
    override suspend fun saveResult(result: ResultSnapshot) = change { require(result.mode == SessionMode.Real); it.copy(lastResult = result) }
    override suspend fun saveFeedback(message: String) = change { require(message.length <= 500); it.copy(feedback = message) }
    override suspend fun reset() = change(allowReset = true) { AppSnapshot() }
}
