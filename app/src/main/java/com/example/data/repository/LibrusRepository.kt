package com.example.data.repository

import android.util.Log
import com.example.data.local.LibrusDao
import com.example.data.local.entities.GradeEntity
import com.example.data.local.entities.HomeworkEntity
import com.example.data.local.entities.LessonEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.TimetableEntity
import com.example.data.model.CalendarEvent
import com.example.data.model.Grade
import com.example.data.model.Lesson
import com.example.data.model.Student
import com.example.data.model.TimetableEntry
import com.example.data.remote.LibrusAuthManager
import com.example.data.remote.LibrusScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LibrusRepository(
    private val dao: LibrusDao
) {
    private val TAG = "LibrusRepository"

    val student: Flow<Student?> = dao.getStudent().map { it?.toDomain() }

    val grades: Flow<List<Grade>> = dao.getGrades().map { list ->
        list.map { it.toDomain() }
    }

    val timetable: Flow<List<TimetableEntry>> = dao.getTimetable().map { list ->
        list.map { it.toDomain() }
    }

    fun getTimetableForWeek(weekOffset: Int): Flow<List<TimetableEntry>> {
        return dao.getTimetableForWeek(weekOffset).map { list ->
            list.map { it.toDomain() }
        }
    }

    val lessons: Flow<List<Lesson>> = dao.getLessons().map { list ->
        list.map { it.toDomain() }
    }

    val calendarEvents: Flow<List<CalendarEvent>> = dao.getHomework().map { list ->
        list.map { it.toDomain() }
    }

    /**
     * Authenticates with real Librus servers and fetches real data
     */
    suspend fun login(
        username: String,
        password: String,
        sessionToken: String? = null
    ): Result<Student> = withContext(Dispatchers.IO) {
        try {
            val client = LibrusAuthManager.getClient()

            // 1. Authenticate with credentials or session token
            if (!sessionToken.isNullOrBlank()) {
                val tokenResult = LibrusAuthManager.authorizeWithSessionCookie(sessionToken)
                if (tokenResult.isFailure) {
                    return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("Błąd tokena sesji"))
                }
            } else {
                val authResult = LibrusAuthManager.authorize(username, password)
                if (authResult.isFailure) {
                    return@withContext Result.failure(authResult.exceptionOrNull() ?: Exception("Błąd logowania w Librus"))
                }
            }

            // 2. Fetch real student profile from Librus
            val studentResult = LibrusScraper.scrapeAccountInfo(client, username)
            val student = studentResult.getOrElse {
                Student(
                    id = username.ifBlank { "librus_user" },
                    name = "Uczeń ($username)",
                    schoolName = "Librus Synergia",
                    className = "Klasa",
                    login = username,
                    isDemo = false,
                    lastSyncTime = System.currentTimeMillis()
                )
            }

            dao.insertStudent(StudentEntity.fromDomain(student))

            // 3. Fetch real lessons, multi-week timetable, grades, and terminarz from Librus servers
            fetchAndSaveRealData(student)

            Result.success(student)
        } catch (e: Exception) {
            Log.e(TAG, "Login and fetch failed", e)
            Result.failure(e)
        }
    }

    suspend fun refreshData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentStudent = dao.getStudent().firstOrNull()?.toDomain()
                ?: return@withContext Result.failure(Exception("Brak aktywnego profilu ucznia. Zaloguj się."))

            val updatedStudent = currentStudent.copy(lastSyncTime = System.currentTimeMillis())
            dao.insertStudent(StudentEntity.fromDomain(updatedStudent))

            fetchAndSaveRealData(updatedStudent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Refresh failed", e)
            Result.failure(e)
        }
    }

    suspend fun fetchTimetableWeek(weekOffset: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = LibrusAuthManager.getClient()
            val ttResult = LibrusScraper.scrapeTimetable(client, weekOffset)
            if (ttResult.isSuccess) {
                val list = ttResult.getOrNull() ?: emptyList()
                if (list.isNotEmpty()) {
                    dao.clearTimetableForWeek(weekOffset)
                    dao.insertTimetable(list.map { TimetableEntity.fromDomain(it) })
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Connects to Librus Synergia to fetch and persist real student data
     */
    private suspend fun fetchAndSaveRealData(student: Student) {
        val client = LibrusAuthManager.getClient()

        // 1. Real Grades (with comprehensive selectors)
        val gradesResult = LibrusScraper.scrapeGrades(client)
        if (gradesResult.isSuccess) {
            val gradesList = gradesResult.getOrNull() ?: emptyList()
            if (gradesList.isNotEmpty()) {
                dao.clearGrades()
                dao.insertGrades(gradesList.map { GradeEntity.fromDomain(it) })
            }
        }

        // 2. Real Timetable: Fetch BOTH current week (0) AND next week (1)
        val currentWeekResult = LibrusScraper.scrapeTimetable(client, 0)
        val nextWeekResult = LibrusScraper.scrapeTimetable(client, 1)

        val allTimetableEntries = mutableListOf<TimetableEntity>()
        if (currentWeekResult.isSuccess) {
            allTimetableEntries.addAll(currentWeekResult.getOrNull().orEmpty().map { TimetableEntity.fromDomain(it) })
        }
        if (nextWeekResult.isSuccess) {
            allTimetableEntries.addAll(nextWeekResult.getOrNull().orEmpty().map { TimetableEntity.fromDomain(it) })
        }
        if (allTimetableEntries.isNotEmpty()) {
            dao.clearTimetable()
            dao.insertTimetable(allTimetableEntries)
        }

        // 3. Real Lessons
        val lessonsResult = LibrusScraper.scrapeLessons(client)
        if (lessonsResult.isSuccess) {
            val lessonsList = lessonsResult.getOrNull() ?: emptyList()
            if (lessonsList.isNotEmpty()) {
                dao.clearLessons()
                dao.insertLessons(lessonsList.map { LessonEntity.fromDomain(it) })
            }
        }

        // 4. Terminarz (replaces Zadania with full Librus Terminarz)
        val calendarResult = LibrusScraper.scrapeTerminarz(client)
        if (calendarResult.isSuccess) {
            val eventsList = calendarResult.getOrNull() ?: emptyList()
            if (eventsList.isNotEmpty()) {
                dao.clearHomework()
                dao.insertHomework(eventsList.map { HomeworkEntity.fromDomain(it) })
            }
        }
    }

    suspend fun toggleCalendarEvent(id: String, completed: Boolean) = withContext(Dispatchers.IO) {
        dao.updateHomeworkCompletion(id, completed)
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        dao.clearGrades()
        dao.clearTimetable()
        dao.clearLessons()
        dao.clearHomework()
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        dao.clearAllData()
        LibrusAuthManager.cookieJar.clear()
    }
}
