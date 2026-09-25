package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DemoDataProvider
import com.example.data.local.entities.GradeEntity
import com.example.data.local.entities.HomeworkEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.repository.LibrusRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val demoStudent = DemoDataProvider.getDemoStudent()
        val loginResult = repository.login("demo", "demo", forceDemo = true)

        assertTrue(loginResult.isSuccess)

        val cachedStudent = repository.student.first()
        assertNotNull(cachedStudent)
        assertEquals("Jan Kowalski", cachedStudent?.name)
        assertEquals("7294819u", cachedStudent?.login)

        val cachedGrades = repository.grades.first()
        assertTrue(cachedGrades.isNotEmpty())

        val cachedTimetable = repository.timetable.first()
        assertTrue(cachedTimetable.isNotEmpty())

        val cachedHomework = repository.homework.first()
        assertTrue(cachedHomework.isNotEmpty())
    }

    @Test
    fun `test homework toggle persistence in Room database`() = runBlocking {
        repository.login("demo", "demo", forceDemo = true)
        val initialHw = repository.homework.first()
        val firstItem = initialHw.first()

        repository.toggleHomework(firstItem.id, !firstItem.isCompleted)
        val updatedHw = repository.homework.first()
        val updatedItem = updatedHw.first { it.id == firstItem.id }

        assertEquals(!firstItem.isCompleted, updatedItem.isCompleted)
    }
}
