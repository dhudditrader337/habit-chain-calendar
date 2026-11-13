package com.habitchain.calendar

import com.habitchain.calendar.data.local.SnapshotCodec
import com.habitchain.calendar.data.local.SnapshotStore
import com.habitchain.calendar.data.repository.HabitRepositoryImpl
import com.habitchain.calendar.domain.model.*
import com.habitchain.calendar.domain.time.LocalDateProvider
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.*

class LocalPersistenceTest {
    private val today = LocalDate.of(2026, 11, 13)
    private val habit = Habit("reading", "Read ten pages", HabitColor.Leaf, today.minusDays(5))
    private val clock = object : LocalDateProvider {
        override fun today() = today
        override fun millisUntilTomorrow() = 1000L
    }
    private class MemoryStore : SnapshotStore {
        var value: String? = null
        var fail = false
        override fun read() = value
        override fun write(value: String): Boolean { if (fail) return false; this.value = value; return true }
    }
    private fun repository(store: SnapshotStore) = HabitRepositoryImpl(store, clock, { Unit })
    private fun <T> await(block: suspend () -> T): T {
        val latch = CountDownLatch(1)
        var outcome: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) { outcome = result; latch.countDown() }
        })
        check(latch.await(5, TimeUnit.SECONDS))
        return requireNotNull(outcome).getOrThrow()
    }
    @Test fun newStorageContainsNoPersonalExamples() {
        val repository = repository(MemoryStore())
        await { repository.load() }
        assertTrue(repository.state.value.snapshot.habits.isEmpty())
    }
    @Test fun duplicateMarksAndReopeningKeepOneDay() {
        val store = MemoryStore()
        val first = repository(store)
        await { first.load(); first.saveHabit(habit); first.mark(habit.id, today, true); first.mark(habit.id, today, true) }
        val reopened = repository(store)
        await { reopened.load() }
        assertEquals(setOf(today), reopened.state.value.snapshot.history(habit.id).completed)
    }
    @Test fun failedCommitRetainsTheConfirmedSnapshot() {
        val store = MemoryStore()
        val repository = repository(store)
        await { repository.load(); repository.saveHabit(habit) }
        val confirmed = store.value
        store.fail = true
        assertThrows(IllegalStateException::class.java) { await { repository.mark(habit.id, today, true) } }
        assertEquals(confirmed, store.value)
        assertTrue(repository.state.value.snapshot.history(habit.id).completed.isEmpty())
    }
    @Test fun notesAreIndependentOfMarks() {
        val repository = repository(MemoryStore())
        await { repository.load(); repository.saveHabit(habit); repository.saveNote(habit.id, today, "A gentle start"); repository.mark(habit.id, today, true); repository.mark(habit.id, today, false) }
        assertEquals("A gentle start", repository.state.value.snapshot.history(habit.id).notes[today])
        assertTrue(repository.state.value.snapshot.history(habit.id).completed.isEmpty())
        await { repository.saveNote(habit.id, today, "") }
        assertTrue(repository.state.value.snapshot.history(habit.id).notes.isEmpty())
    }
    @Test fun futureDaysAndDaysBeforeCreationCannotBeMarked() {
        val repository = repository(MemoryStore())
        await { repository.load(); repository.saveHabit(habit) }
        assertThrows(IllegalArgumentException::class.java) { await { repository.mark(habit.id, today.plusDays(1), true) } }
        assertThrows(IllegalArgumentException::class.java) { await { repository.mark(habit.id, habit.createdOn.minusDays(1), true) } }
    }
    @Test fun deletionRemovesNotesAndHistoryTogether() {
        val repository = repository(MemoryStore())
        await { repository.load(); repository.saveHabit(habit); repository.saveNote(habit.id, today, "Quiet morning"); repository.mark(habit.id, today, true); repository.deleteHabit(habit.id) }
        assertTrue(repository.state.value.snapshot.habits.isEmpty())
        assertTrue(repository.state.value.snapshot.histories.isEmpty())
    }
    @Test fun settingsFeedbackAndResultsSurviveReopening() {
        val store = MemoryStore()
        val repository = repository(store)
        val result = ResultSnapshot("session", today, SessionMode.Real, 1, 1, 0, listOf(habit.name), emptyList(), 1, 1)
        await { repository.load(); repository.saveHabit(habit); repository.mark(habit.id, today, true); repository.saveSettings(AppSettings(1230, true)); repository.saveFeedback("I enjoy the calendar"); repository.saveResult(result) }
        val reopened = repository(store)
        await { reopened.load() }
        assertEquals(AppSettings(1230, true), reopened.state.value.snapshot.settings)
        assertEquals("I enjoy the calendar", reopened.state.value.snapshot.feedback)
        assertEquals(result, reopened.state.value.snapshot.lastResult)
    }
    @Test fun anUnknownSchemaIsNotOverwritten() {
        val store = MemoryStore().apply { value = "{\"schemaVersion\":99}" }
        val repository = repository(store)
        await { repository.load() }
        assertTrue(repository.state.value.error)
        assertThrows(IllegalStateException::class.java) { await { repository.saveHabit(habit) } }
        assertEquals("{\"schemaVersion\":99}", store.value)
    }
    @Test fun explicitHabitReminderOptInEnablesTheMasterSetting() {
        val repository = repository(MemoryStore())
        await { repository.load(); repository.saveHabit(habit.copy(reminderEnabled = true)) }
        assertTrue(repository.state.value.snapshot.settings.remindersEnabled)
    }
    @Test fun editingAHabitDoesNotReenableDisabledMasterReminders() {
        val repository = repository(MemoryStore())
        val reminded = habit.copy(reminderEnabled = true)
        await { repository.load(); repository.saveHabit(reminded); repository.saveSettings(AppSettings(remindersEnabled = false)); repository.saveHabit(reminded.copy(name = "Read before bed")) }
        assertFalse(repository.state.value.snapshot.settings.remindersEnabled)
    }
    @Test fun aFailedReloadKeepsTheLastGoodSnapshotVisible() {
        val store = MemoryStore()
        val repository = repository(store)
        await { repository.load(); repository.saveHabit(habit) }
        store.value = "broken snapshot"
        await { repository.load() }
        assertTrue(repository.state.value.error)
        assertEquals(listOf(habit), repository.state.value.snapshot.habits)
    }
    @Test fun deliberateResetClearsAllDataAndRepairsCorruptStorage() {
        val store = MemoryStore().apply { value = "broken snapshot" }
        val repository = repository(store)
        await { repository.load(); repository.reset() }
        assertFalse(repository.state.value.error)
        assertEquals(AppSnapshot(), SnapshotCodec.decode(store.value))
    }
}
