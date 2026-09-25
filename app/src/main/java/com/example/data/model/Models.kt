package com.example.data.model

data class Student(
    val id: String = "default_student",
    val name: String,
    val schoolName: String,
    val className: String,
    val login: String,
    val isDemo: Boolean = false,
    val lastSyncTime: Long = System.currentTimeMillis()
)

data class Grade(
    val id: String,
    val subject: String,
    val grade: String,          // e.g. "5", "4+", "1", "6-"
    val numericValue: Double,   // e.g. 5.0, 4.5, 1.0, 5.75
    val weight: Int,            // e.g. 1 to 5
    val category: String,       // e.g. "Sprawdzian", "Kartkówka", "Odpowiedź", "Aktywność"
    val date: String,           // e.g. "2026-09-18"
    val teacher: String,
    val semester: Int,          // 1 or 2
    val comment: String = ""
)

data class TimetableEntry(
    val id: String,
    val dayOfWeek: Int,         // 1 = Poniedziałek, 2 = Wtorek, ..., 5 = Piątek
    val period: Int,            // 1, 2, 3...
    val timeRange: String,      // e.g. "08:00 - 08:45"
    val subject: String,
    val classroom: String,
    val teacher: String,
    val statusNote: String = "Planowa",
    val isCancelled: Boolean = false,
    val isSubstitution: Boolean = false
)

data class Lesson(
    val id: String,
    val date: String,           // e.g. "2026-09-24"
    val period: Int,
    val subject: String,
    val topic: String,
    val teacher: String,
    val attendance: String = "Obecność" // "Obecność", "Spóźnienie", "Nieobecność", "Usprawiedliwiona"
)

data class Homework(
    val id: String,
    val subject: String,
    val topic: String,
    val content: String,
    val deadline: String,       // e.g. "2026-09-28"
    val creationDate: String,   // e.g. "2026-09-22"
    val teacher: String,
    val isCompleted: Boolean = false
)

sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : SyncState()
    data class Error(val message: String) : SyncState()
}
