package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entities.GradeEntity
import com.example.data.local.entities.HomeworkEntity
import com.example.data.local.entities.LessonEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.TimetableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibrusDao {

    // Student
    @Query("SELECT * FROM students LIMIT 1")
    fun getStudent(): Flow<StudentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Query("DELETE FROM students")
    suspend fun clearStudent()

    // Grades
    @Query("SELECT * FROM grades ORDER BY date DESC, id DESC")
    fun getGrades(): Flow<List<GradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<GradeEntity>)

    @Query("DELETE FROM grades")
    suspend fun clearGrades()

    // Timetable
    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek ASC, period ASC")
    fun getTimetable(): Flow<List<TimetableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetable(entries: List<TimetableEntity>)

    @Query("DELETE FROM timetable_entries")
    suspend fun clearTimetable()

    // Lessons
    @Query("SELECT * FROM lessons ORDER BY date DESC, period DESC")
    fun getLessons(): Flow<List<LessonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Query("DELETE FROM lessons")
    suspend fun clearLessons()

    // Homework
    @Query("SELECT * FROM homework ORDER BY deadline ASC")
    fun getHomework(): Flow<List<HomeworkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHomework(homeworkList: List<HomeworkEntity>)

    @Query("UPDATE homework SET isCompleted = :completed WHERE id = :id")
    suspend fun updateHomeworkCompletion(id: String, completed: Boolean)

    @Query("DELETE FROM homework")
    suspend fun clearHomework()

    @Transaction
    suspend fun clearAllData() {
        clearStudent()
        clearGrades()
        clearTimetable()
        clearLessons()
        clearHomework()
    }
}
