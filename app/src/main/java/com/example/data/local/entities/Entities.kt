package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CalendarEvent
import com.example.data.model.Grade
import com.example.data.model.Lesson
import com.example.data.model.Student
import com.example.data.model.TimetableEntry

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val schoolName: String,
    val className: String,
    val login: String,
    val isDemo: Boolean,
    val lastSyncTime: Long
) {
    fun toDomain(): Student = Student(
        id = id,
        name = name,
        schoolName = schoolName,
        className = className,
        login = login,
        isDemo = isDemo,
        lastSyncTime = lastSyncTime
    )

    companion object {
        fun fromDomain(student: Student): StudentEntity = StudentEntity(
            id = student.id,
            name = student.name,
            schoolName = student.schoolName,
            className = student.className,
            login = student.login,
            isDemo = student.isDemo,
            lastSyncTime = student.lastSyncTime
        )
    }
}

@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey val id: String,
    val subject: String,
    val grade: String,
    val numericValue: Double,
    val weight: Int,
    val category: String,
    val date: String,
    val teacher: String,
    val semester: Int,
    val comment: String
) {
    fun toDomain(): Grade = Grade(
        id = id,
        subject = subject,
        grade = grade,
        numericValue = numericValue,
        weight = weight,
        category = category,
        date = date,
        teacher = teacher,
        semester = semester,
        comment = comment
    )

    companion object {
        fun fromDomain(grade: Grade): GradeEntity = GradeEntity(
            id = grade.id,
            subject = grade.subject,
            grade = grade.grade,
            numericValue = grade.numericValue,
            weight = grade.weight,
            category = grade.category,
            date = grade.date,
            teacher = grade.teacher,
            semester = grade.semester,
            comment = grade.comment
        )
    }
}

@Entity(tableName = "timetable_entries")
data class TimetableEntity(
    @PrimaryKey val id: String,
    val dayOfWeek: Int,
    val period: Int,
    val timeRange: String,
    val subject: String,
    val classroom: String,
    val teacher: String,
    val statusNote: String,
    val isCancelled: Boolean,
    val isSubstitution: Boolean,
    val weekOffset: Int = 0
) {
    fun toDomain(): TimetableEntry = TimetableEntry(
        id = id,
        dayOfWeek = dayOfWeek,
        period = period,
        timeRange = timeRange,
        subject = subject,
        classroom = classroom,
        teacher = teacher,
        statusNote = statusNote,
        isCancelled = isCancelled,
        isSubstitution = isSubstitution,
        weekOffset = weekOffset
    )

    companion object {
        fun fromDomain(entry: TimetableEntry): TimetableEntity = TimetableEntity(
            id = entry.id,
            dayOfWeek = entry.dayOfWeek,
            period = entry.period,
            timeRange = entry.timeRange,
            subject = entry.subject,
            classroom = entry.classroom,
            teacher = entry.teacher,
            statusNote = entry.statusNote,
            isCancelled = entry.isCancelled,
            isSubstitution = entry.isSubstitution,
            weekOffset = entry.weekOffset
        )
    }
}

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String,
    val date: String,
    val period: Int,
    val subject: String,
    val topic: String,
    val teacher: String,
    val attendance: String
) {
    fun toDomain(): Lesson = Lesson(
        id = id,
        date = date,
        period = period,
        subject = subject,
        topic = topic,
        teacher = teacher,
        attendance = attendance
    )

    companion object {
        fun fromDomain(lesson: Lesson): LessonEntity = LessonEntity(
            id = lesson.id,
            date = lesson.date,
            period = lesson.period,
            subject = lesson.subject,
            topic = lesson.topic,
            teacher = lesson.teacher,
            attendance = lesson.attendance
        )
    }
}

@Entity(tableName = "homework")
data class HomeworkEntity(
    @PrimaryKey val id: String,
    val subject: String,
    val topic: String,
    val content: String,
    val deadline: String,
    val creationDate: String,
    val teacher: String,
    val isCompleted: Boolean
) {
    fun toDomain(): CalendarEvent = CalendarEvent(
        id = id,
        title = subject,
        category = topic.ifBlank { "Wydarzenie" },
        description = content,
        date = deadline,
        teacher = teacher,
        timeRange = creationDate,
        isCompleted = isCompleted
    )

    companion object {
        fun fromDomain(event: CalendarEvent): HomeworkEntity = HomeworkEntity(
            id = event.id,
            subject = event.title,
            topic = event.category,
            content = event.description,
            deadline = event.date,
            creationDate = event.timeRange,
            teacher = event.teacher,
            isCompleted = event.isCompleted
        )
    }
}
