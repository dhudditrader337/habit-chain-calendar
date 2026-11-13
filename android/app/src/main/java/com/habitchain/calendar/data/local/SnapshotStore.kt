package com.habitchain.calendar.data.local

interface SnapshotStore {
    fun read(): String?
    fun write(value: String): Boolean
}
