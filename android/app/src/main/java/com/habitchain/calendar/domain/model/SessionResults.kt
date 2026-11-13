package com.habitchain.calendar.domain.model

class SessionResults {
    private val results = linkedMapOf<String, ResultSnapshot>()
    fun put(result: ResultSnapshot) {
        results[result.sessionId] = result
        if (results.size > 8) results.remove(results.keys.first())
    }
    fun get(id: String): ResultSnapshot? = results[id]
    fun clear() = results.clear()
}
