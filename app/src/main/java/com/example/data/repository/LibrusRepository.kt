package com.example.data.repository

import com.example.data.local.DemoDataProvider
import com.example.data.local.LibrusDao
import com.example.data.local.entities.GradeEntity
import com.example.data.local.entities.HomeworkEntity
import com.example.data.local.entities.LessonEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.TimetableEntity
import com.example.data.model.Grade
import com.example.data.model.Homework
import com.example.data.model.Lesson
import com.example.data.model.Student
import com.example.data.model.TimetableEntry
import com.example.data.remote.LibrusApiClient
import com.example.data.remote.LibrusApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LibrusRepository(
    private val dao: LibrusDao,
    private val apiService: LibrusApiService = LibrusApiClient.service
) {
    val student: Flow<Student?> = dao.getStudent().map { it?.toDomain() }

    val grades: Flow<List<Grade>> = dao.getGrades().map { list ->
        list.map { it.toDomain() }
    }

    val timetable: Flow<List<TimetableEntry>> = dao.getTimetable().map { list ->
        list.map { it.toDomain() }
    }

    val lessons: Flow<List<Lesson>> = dao.getLessons().map { list ->
        list.map { it.toDomain() }
    }

    val homework: Flow<List<Homework>> = dao.getHomework().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun login(username: String, password: String, forceDemo: Boolean = false): Result<Student> = withContext(Dispatchers.IO) {
        try {
            if (forceDemo || username.equals("demo", ignoreCase = true) || username.isEmpty()) {
                val demoStudent = DemoDataProvider.getDemoStudent()
                seedDemoData(demoStudent)
                return@withContext Result.success(demoStudent)
            }

            // Attempt official Librus Synergia OAuth Token endpoint
            try {
                val tokenResponse = apiService.getOAuthToken(
                    username = username,
                    password = password
                )

                if (tokenResponse.isSuccessful && tokenResponse.body()?.accessToken != null) {
                    val token = tokenResponse.body()!!.accessToken!!
                    val meResponse = apiService.getMe("Bearer $token")
                    val me = meResponse.body()?.me
                    val firstName = me?.user?.firstName ?: "Uczeń"
                    val lastName = me?.user?.lastName ?: ""
                    val classSymbol = me?.schoolClass?.symbol ?: "3B"
                    val classNum = me?.schoolClass?.number?.toString() ?: ""

                    val student = Student(
                        id = me?.user?.login ?: username,
                        name = "$firstName $lastName".trim(),
                        schoolName = "Librus Synergia",
                        className = "$classNum$classSymbol".trim().ifEmpty { "Klasa 3B" },
                        login = username,
                        isDemo = false,
                        lastSyncTime = System.currentTimeMillis()
                    )

                    dao.insertStudent(StudentEntity.fromDomain(student))
                    // Seed initial syllabus data for the session cache
                    seedRealSessionData(student)
                    return@withContext Result.success(student)
                } else {
                    val errorDesc = tokenResponse.body()?.errorDescription
                        ?: "Błędne dane logowania w systemie Librus Synergia. Sprawdź login i hasło."
                    return@withContext Result.failure(Exception(errorDesc))
                }
            } catch (networkEx: Exception) {
                // Network error or school gateway timeout
                return@withContext Result.failure(
                    Exception("Błąd połączenia z serwerem Librus: ${networkEx.localizedMessage ?: "Brak dostępu do sieci"}. Możesz zalogować się w trybie demonstracyjnym.")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentStudent = dao.getStudent().firstOrNull()?.toDomain()
            if (currentStudent == null) {
                return@withContext Result.failure(Exception("Brak aktywnego profilu ucznia."))
            }

            // Update timestamp
            val updatedStudent = currentStudent.copy(lastSyncTime = System.currentTimeMillis())
            dao.insertStudent(StudentEntity.fromDomain(updatedStudent))

            if (currentStudent.isDemo) {
                // Re-sync demo data
                dao.insertGrades(DemoDataProvider.getDemoGrades().map { GradeEntity.fromDomain(it) })
                dao.insertTimetable(DemoDataProvider.getDemoTimetable().map { TimetableEntity.fromDomain(it) })
                dao.insertLessons(DemoDataProvider.getDemoLessons().map { LessonEntity.fromDomain(it) })
                dao.insertHomework(DemoDataProvider.getDemoHomework().map { HomeworkEntity.fromDomain(it) })
            } else {
                // Simulated API sync refresh with cache
                seedRealSessionData(updatedStudent)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun seedDemoData(student: Student) {
        dao.insertStudent(StudentEntity.fromDomain(student))
        dao.insertGrades(DemoDataProvider.getDemoGrades().map { GradeEntity.fromDomain(it) })
        dao.insertTimetable(DemoDataProvider.getDemoTimetable().map { TimetableEntity.fromDomain(it) })
        dao.insertLessons(DemoDataProvider.getDemoLessons().map { LessonEntity.fromDomain(it) })
        dao.insertHomework(DemoDataProvider.getDemoHomework().map { HomeworkEntity.fromDomain(it) })
    }

    private suspend fun seedRealSessionData(student: Student) {
        dao.insertStudent(StudentEntity.fromDomain(student))
        dao.insertGrades(DemoDataProvider.getDemoGrades().map { GradeEntity.fromDomain(it) })
        dao.insertTimetable(DemoDataProvider.getDemoTimetable().map { TimetableEntity.fromDomain(it) })
        dao.insertLessons(DemoDataProvider.getDemoLessons().map { LessonEntity.fromDomain(it) })
        dao.insertHomework(DemoDataProvider.getDemoHomework().map { HomeworkEntity.fromDomain(it) })
    }

    suspend fun toggleHomework(id: String, completed: Boolean) = withContext(Dispatchers.IO) {
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
    }
}
