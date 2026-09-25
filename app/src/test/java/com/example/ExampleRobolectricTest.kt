package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.GradeEntity
import com.example.data.local.entities.HomeworkEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.TimetableEntity
import com.example.data.repository.LibrusRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: LibrusRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LibrusRepository(database.librusDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Synergia Student", appName)
    }

    @Test
    fun `test offline cache persistence in Room database`() = runBlocking {
        val student = StudentEntity(
            id = "test_user_1",
            name = "Uczeń Testowy",
            schoolName = "I LO",
            className = "3A",
            login = "test_user_1",
            isDemo = false,
            lastSyncTime = System.currentTimeMillis()
        )
        database.librusDao().insertStudent(student)

        val cachedStudent = repository.student.first()
        assertNotNull(cachedStudent)
        assertEquals("Uczeń Testowy", cachedStudent?.name)
        assertEquals("test_user_1", cachedStudent?.login)

        // Test Terminarz / Calendar event persistence
        val hw = HomeworkEntity(
            id = "event_1",
            subject = "Matematyka",
            topic = "Sprawdzian",
            content = "Funkcje kwadratowe i wielomiany",
            deadline = "2026-09-28",
            creationDate = "Terminarz szkolny",
            teacher = "prof. Kowal",
            isCompleted = false
        )
        database.librusDao().insertHomework(listOf(hw))

        val cachedEvents = repository.calendarEvents.first()
        assertEquals(1, cachedEvents.size)
        assertEquals("Matematyka", cachedEvents[0].title)
        assertEquals("Sprawdzian", cachedEvents[0].category)

        repository.toggleCalendarEvent("event_1", true)
        val updatedEvents = repository.calendarEvents.first()
        assertTrue(updatedEvents[0].isCompleted)

        // Test Multi-week timetable
        val currentWeekLesson = TimetableEntity(
            id = "tt_curr_1",
            dayOfWeek = 1,
            period = 1,
            timeRange = "08:00 - 08:45",
            subject = "Informatyka",
            classroom = "Sala 101",
            teacher = "prof. Nowak",
            statusNote = "Planowa",
            isCancelled = false,
            isSubstitution = false,
            weekOffset = 0
        )
        val nextWeekLesson = TimetableEntity(
            id = "tt_next_1",
            dayOfWeek = 1,
            period = 1,
            timeRange = "08:00 - 08:45",
            subject = "Matematyka",
            classroom = "Sala 204",
            teacher = "prof. Kowal",
            statusNote = "Planowa",
            isCancelled = false,
            isSubstitution = false,
            weekOffset = 1
        )
        database.librusDao().insertTimetable(listOf(currentWeekLesson, nextWeekLesson))

        val currentWeekList = repository.getTimetableForWeek(0).first()
        assertEquals(1, currentWeekList.size)
        assertEquals("Informatyka", currentWeekList[0].subject)

        val nextWeekList = repository.getTimetableForWeek(1).first()
        assertEquals(1, nextWeekList.size)
        assertEquals("Matematyka", nextWeekList[0].subject)
    }
}
