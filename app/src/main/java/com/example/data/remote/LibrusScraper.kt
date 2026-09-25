package com.example.data.remote

import android.util.Log
import com.example.data.model.CalendarEvent
import com.example.data.model.Grade
import com.example.data.model.Lesson
import com.example.data.model.Student
import com.example.data.model.TimetableEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

object LibrusScraper {
    private const val TAG = "LibrusScraper"
    private const val BASE_URL = "https://synergia.librus.pl"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    private fun fetchDocument(client: OkHttpClient, path: String, postBody: FormBody? = null): Result<Document> {
        return try {
            val url = if (path.startsWith("http")) path else "$BASE_URL/$path"
            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "$BASE_URL/uczen_index")

            if (postBody != null) {
                requestBuilder.post(postBody)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("Błąd HTTP ${response.code} przy pobieraniu $path"))
                }
                val html = response.body?.string() ?: ""
                if (html.contains("loguj") && (html.contains("hasło") || html.contains("login"))) {
                    return Result.failure(Exception("Sesja Librus wygasła. Wymagane ponowne zalogowanie."))
                }
                Result.success(Jsoup.parse(html, BASE_URL))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching $path", e)
            Result.failure(e)
        }
    }

    /**
     * Scrapes real student information from https://synergia.librus.pl/informacja
     */
    suspend fun scrapeAccountInfo(client: OkHttpClient, fallbackLogin: String): Result<Student> = withContext(Dispatchers.IO) {
        val docResult = fetchDocument(client, "informacja")
        if (docResult.isFailure) return@withContext Result.failure(docResult.exceptionOrNull() ?: Exception("Błąd profilu"))

        val doc = docResult.getOrNull()!!
        try {
            val rows = doc.select("#body table tr, table.decorated tr")
            var studentName = ""
            var className = ""
            var educator = ""
            var login = fallbackLogin

            for (row in rows) {
                val text = row.text().trim()
                val td = row.select("td").text().trim()
                val th = row.select("th").text().trim()

                when {
                    th.contains("Imię i nazwisko", ignoreCase = true) || text.startsWith("Imię i nazwisko") -> {
                        if (studentName.isEmpty()) studentName = td.ifEmpty { text.substringAfter(":").trim() }
                    }
                    th.contains("Klasa", ignoreCase = true) || text.startsWith("Klasa") -> {
                        if (className.isEmpty()) className = td.ifEmpty { text.substringAfter(":").trim() }
                    }
                    th.contains("Wychowawca", ignoreCase = true) || text.startsWith("Wychowawca") -> {
                        if (educator.isEmpty()) educator = td.ifEmpty { text.substringAfter(":").trim() }
                    }
                    th.contains("Login", ignoreCase = true) || text.startsWith("Login") -> {
                        if (td.isNotEmpty()) login = td
                    }
                }
            }

            if (studentName.isEmpty()) {
                val userHeader = doc.select(".user-name, #user-section .name, .logged-as").text().trim()
                if (userHeader.isNotEmpty()) {
                    studentName = userHeader
                }
            }

            val student = Student(
                id = login.ifBlank { UUID.randomUUID().toString() },
                name = studentName.ifBlank { "Uczeń ($login)" },
                schoolName = if (educator.isNotEmpty()) "Wychowawca: $educator" else "Librus Synergia",
                className = className.ifBlank { "Klasa Szkolna" },
                login = login,
                isDemo = false,
                lastSyncTime = System.currentTimeMillis()
            )
            Result.success(student)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing account info", e)
            Result.failure(e)
        }
    }

    /**
     * Scrapes real grades from https://synergia.librus.pl/przegladaj_oceny/uczen
     * Handles both table selectors and all grade variants (standard, points, descriptive)
     */
    suspend fun scrapeGrades(client: OkHttpClient): Result<List<Grade>> = withContext(Dispatchers.IO) {
        val docResult = fetchDocument(client, "przegladaj_oceny/uczen")
        if (docResult.isFailure) return@withContext Result.failure(docResult.exceptionOrNull() ?: Exception("Błąd ocen"))

        val doc = docResult.getOrNull()!!
        val gradesList = mutableListOf<Grade>()

        try {
            // Mati365 selector: table.decorated.stretch:eq(1) > tbody > tr[class^='line']:not([name]), or any line in grades table
            var rows = doc.select("table.decorated.stretch tr[class^='line']:not([name])")
            if (rows.isEmpty()) {
                rows = doc.select("table.decorated tr[class^='line']:not([name])")
            }
            if (rows.isEmpty()) {
                rows = doc.select("tr.line0, tr.line1")
            }

            for (row in rows) {
                val cells = row.select("td")
                if (cells.size < 3) continue

                // Subject name is in column 1 (index 1)
                val subjectName = cells[1].text().trim()
                if (subjectName.isBlank() || subjectName.equals("Zachowanie", ignoreCase = true) || subjectName.contains("Średnia")) {
                    continue
                }

                // Check all cells for span.grade-box or a[href*='szczegoly']
                for (cellIndex in 2 until cells.size) {
                    val gradeBoxes = cells[cellIndex].select("span.grade-box a, a[href*='szczegoly'], a[href*='oceny']")
                    val semester = if (cellIndex in 2..4) 1 else 2

                    for (box in gradeBoxes) {
                        val grade = parseGradeElement(box, subjectName, semester)
                        if (grade != null) {
                            gradesList.add(grade)
                        }
                    }
                }
            }

            // Fallback for single or simplified table view
            if (gradesList.isEmpty()) {
                val allGradeBoxes = doc.select("span.grade-box a, a[href*='szczegoly']")
                for (box in allGradeBoxes) {
                    val row = box.closest("tr")
                    val subject = row?.select("td")?.getOrNull(1)?.text()?.trim() ?: "Przedmiot"
                    val grade = parseGradeElement(box, subject, 1)
                    if (grade != null) gradesList.add(grade)
                }
            }

            Result.success(gradesList)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing grades", e)
            Result.failure(e)
        }
    }

    private fun parseGradeElement(box: org.jsoup.nodes.Element, subjectName: String, semester: Int): Grade? {
        val gradeText = box.text().trim().replace("*", "").trim()
        if (gradeText.isEmpty()) return null

        val tooltip = (box.attr("title").ifBlank { box.attr("alt") })
            .replace("<br/>", "\n")
            .replace("<br>", "\n")
        val href = box.attr("href")
        val id = href.substringAfterLast("/").ifBlank { UUID.randomUUID().toString() }

        var category = "Ocena bieżąca"
        var teacher = "Nauczyciel"
        var date = ""
        var weight = 1

        for (line in tooltip.lines()) {
            val clean = line.trim()
            when {
                clean.startsWith("Kategoria:", ignoreCase = true) -> category = clean.substringAfter(":").trim()
                clean.startsWith("Data:", ignoreCase = true) -> date = clean.substringAfter(":").trim()
                clean.startsWith("Nauczyciel:", ignoreCase = true) -> teacher = clean.substringAfter(":").trim()
                clean.startsWith("Waga:", ignoreCase = true) -> {
                    val wStr = clean.substringAfter(":").trim()
                    weight = wStr.toIntOrNull() ?: 1
                }
            }
        }

        val numericVal = when {
            gradeText.startsWith("6") && gradeText.endsWith("-") -> 5.75
            gradeText.startsWith("6") -> 6.0
            gradeText.startsWith("5") && gradeText.endsWith("+") -> 5.5
            gradeText.startsWith("5") && gradeText.endsWith("-") -> 4.75
            gradeText.startsWith("5") -> 5.0
            gradeText.startsWith("4") && gradeText.endsWith("+") -> 4.5
            gradeText.startsWith("4") && gradeText.endsWith("-") -> 3.75
            gradeText.startsWith("4") -> 4.0
            gradeText.startsWith("3") && gradeText.endsWith("+") -> 3.5
            gradeText.startsWith("3") && gradeText.endsWith("-") -> 2.75
            gradeText.startsWith("3") -> 3.0
            gradeText.startsWith("2") && gradeText.endsWith("+") -> 2.5
            gradeText.startsWith("2") && gradeText.endsWith("-") -> 1.75
            gradeText.startsWith("2") -> 2.0
            gradeText.startsWith("1") && gradeText.endsWith("+") -> 1.5
            gradeText.startsWith("1") -> 1.0
            else -> gradeText.filter { it.isDigit() }.toDoubleOrNull() ?: 3.0
        }

        return Grade(
            id = id,
            subject = subjectName,
            grade = gradeText,
            numericValue = numericVal,
            weight = weight,
            category = category,
            date = date.ifBlank { "Librus" },
            teacher = teacher,
            semester = semester,
            comment = tooltip
        )
    }

    /**
     * Scrapes timetable for a given week offset:
     * weekOffset = 0: current week
     * weekOffset = 1: next week
     * weekOffset = -1: previous week
     */
    suspend fun scrapeTimetable(client: OkHttpClient, weekOffset: Int = 0): Result<List<TimetableEntry>> = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)

        // Set to Monday of target week
        val daysSinceMonday = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
        val fromDate = dateFormat.format(cal.time)

        // Set to Sunday of target week
        cal.add(Calendar.DAY_OF_MONTH, 6)
        val toDate = dateFormat.format(cal.time)

        val formBody = FormBody.Builder()
            .add("tydzien", "${fromDate}_${toDate}")
            .build()

        val docResult = fetchDocument(client, "przegladaj_plan_lekcji", formBody)
        if (docResult.isFailure) return@withContext Result.failure(docResult.exceptionOrNull() ?: Exception("Błąd planu lekcji"))

        val doc = docResult.getOrNull()!!
        val entries = mutableListOf<TimetableEntry>()

        try {
            var rows = doc.select("table.decorated.plan-lekcji tr.line1")
            if (rows.isEmpty()) {
                rows = doc.select("tr.line1")
            }

            for (row in rows) {
                val hourTh = row.select("th").text().trim()
                val cells = row.select("td")
                if (cells.size < 6) continue

                val periodNum = row.select("td:first-child").text().trim().toIntOrNull() ?: 1

                for (dayIndex in 1..5) {
                    if (dayIndex >= cells.size) break
                    val cell = cells[dayIndex]
                    val textDivs = cell.select(".text")
                    if (textDivs.isEmpty()) continue

                    for (textDiv in textDivs) {
                        val subject = textDiv.select("b").text().trim()
                        if (subject.isBlank()) continue

                        val rawHtml = textDiv.html()
                        var teacher = ""
                        var room = ""

                        if (rawHtml.contains("<br")) {
                            val afterBr = rawHtml.substringAfter("<br").substringAfter(">").replace(Regex("<[^>]*>"), "").trim()
                            val clean = afterBr.replace("&nbsp;", " ").replace("&amp;", "&").removePrefix("-").trim()
                            if (clean.contains(" s. ")) {
                                teacher = clean.substringBefore(" s. ").trim()
                                room = "Sala " + clean.substringAfter(" s. ").trim()
                            } else {
                                teacher = clean
                            }
                        }

                        val isCancelled = cell.select("s").isNotEmpty() || cell.text().contains("odwołan", ignoreCase = true)
                        val isSubstitution = cell.text().contains("zastępstw", ignoreCase = true)

                        entries.add(
                            TimetableEntry(
                                id = "${weekOffset}_${dayIndex}_${periodNum}_${subject}_${UUID.randomUUID()}",
                                dayOfWeek = dayIndex,
                                period = periodNum,
                                timeRange = hourTh.ifBlank { "Lekcja $periodNum" },
                                subject = subject,
                                classroom = room.ifBlank { "Sala" },
                                teacher = teacher.ifBlank { "Nauczyciel" },
                                isSubstitution = isSubstitution,
                                isCancelled = isCancelled,
                                statusNote = if (isCancelled) "Odwołana" else if (isSubstitution) "Zastępstwo" else "Planowa",
                                weekOffset = weekOffset
                            )
                        )
                    }
                }
            }
            Result.success(entries)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing timetable", e)
            Result.failure(e)
        }
    }

    /**
     * Scrapes real lessons from https://synergia.librus.pl/zrealizowane_lekcje
     */
    suspend fun scrapeLessons(client: OkHttpClient): Result<List<Lesson>> = withContext(Dispatchers.IO) {
        val docResult = fetchDocument(client, "zrealizowane_lekcje")
        if (docResult.isSuccess) {
            val doc = docResult.getOrNull()!!
            val lessons = mutableListOf<Lesson>()
            val rows = doc.select("table.decorated tbody tr, table tr.line0, table tr.line1")

            for (row in rows) {
                val cells = row.select("td")
                if (cells.size < 4) continue

                val date = cells[0].text().trim()
                val period = cells.getOrNull(1)?.text()?.trim()?.toIntOrNull() ?: 1
                val subject = cells.getOrNull(2)?.text()?.trim() ?: ""
                val topic = cells.getOrNull(3)?.text()?.trim() ?: ""
                val teacher = cells.getOrNull(4)?.text()?.trim() ?: "Nauczyciel"
                val attendance = cells.getOrNull(5)?.text()?.trim() ?: "Obecność"

                if (subject.isNotBlank() && topic.isNotBlank()) {
                    lessons.add(
                        Lesson(
                            id = "lesson_${date}_${period}_${UUID.randomUUID()}",
                            date = date,
                            period = period,
                            subject = subject,
                            topic = topic,
                            teacher = teacher,
                            attendance = attendance
                        )
                    )
                }
            }

            if (lessons.isNotEmpty()) {
                return@withContext Result.success(lessons)
            }
        }

        // Fallback: derive lessons from the real scraped timetable
        val ttResult = scrapeTimetable(client, 0)
        if (ttResult.isSuccess) {
            val tt = ttResult.getOrNull() ?: emptyList()
            val derived = tt.mapIndexed { idx, entry ->
                Lesson(
                    id = "real_${entry.dayOfWeek}_${entry.period}_$idx",
                    date = when (entry.dayOfWeek) {
                        1 -> "Poniedziałek"
                        2 -> "Wtorek"
                        3 -> "Środa"
                        4 -> "Czwartek"
                        5 -> "Piątek"
                        else -> "Dzień powszedni"
                    },
                    period = entry.period,
                    subject = entry.subject,
                    topic = "Zajęcia edukacyjne (${entry.classroom})",
                    teacher = entry.teacher,
                    attendance = "Obecność"
                )
            }
            return@withContext Result.success(derived)
        }

        Result.success(emptyList())
    }

    /**
     * Scrapes Terminarz from https://synergia.librus.pl/terminarz (sprawdziany, kartkówki, zadania domowe, wydarzenia)
     * Matches Mati365/librus-api calendar.js resource.
     */
    suspend fun scrapeTerminarz(client: OkHttpClient): Result<List<CalendarEvent>> = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)

        val formBody = FormBody.Builder()
            .add("miesiac", currentMonth.toString())
            .add("rok", currentYear.toString())
            .build()

        val docResult = fetchDocument(client, "terminarz", formBody)
        if (docResult.isFailure) {
            // Try fetching moje_zadania as fallback if terminarz fails
            return@withContext scrapeHomeworkFallback(client)
        }

        val doc = docResult.getOrNull()!!
        val events = mutableListOf<CalendarEvent>()

        try {
            // Mati365 selector: table.kalendarz.decorated.center tbody td .kalendarz-dzien
            val dayCells = doc.select("table.kalendarz td, .kalendarz-dzien, td.dzien")

            for (cell in dayCells) {
                val dayNum = cell.select(".kalendarz-numer-dnia, .numer-dnia, b").first()?.text()?.trim() ?: continue
                val dayInt = dayNum.toIntOrNull() ?: continue
                val dateString = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, dayInt)

                // Event details in this day cell
                val eventEntries = cell.select("td, .kalendarz-wpis, div[onclick*='terminarz/szczegoly'], a[onclick*='terminarz/szczegoly']")
                for (entry in eventEntries) {
                    val onclick = entry.attr("onclick")
                    val id = Regex("/(\\d*)'").find(onclick)?.groupValues?.getOrNull(1) ?: UUID.randomUUID().toString()
                    val title = entry.text().trim()
                    if (title.isBlank()) continue

                    val category = when {
                        title.contains("sprawdzian", ignoreCase = true) -> "Sprawdzian"
                        title.contains("kartkówk", ignoreCase = true) -> "Kartkówka"
                        title.contains("zadanie", ignoreCase = true) -> "Zadanie domowe"
                        title.contains("odpowiedź", ignoreCase = true) -> "Odpowiedź"
                        title.contains("wycieczka", ignoreCase = true) -> "Wycieczka"
                        title.contains("uroczystość", ignoreCase = true) -> "Uroczystość"
                        else -> "Wydarzenie"
                    }

                    events.add(
                        CalendarEvent(
                            id = "event_${id}_$dateString",
                            date = dateString,
                            title = title,
                            category = category,
                            description = "Wpis w terminarzu Librus na dzień $dateString.",
                            teacher = "Librus Synergia",
                            timeRange = "Terminarz szkolny",
                            isCompleted = false
                        )
                    )
                }
            }

            // Also merge with moje_zadania if any homework is listed
            val hwFallback = scrapeHomeworkFallback(client).getOrNull() ?: emptyList()
            events.addAll(hwFallback)

            Result.success(events.distinctBy { it.id })
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing terminarz", e)
            scrapeHomeworkFallback(client)
        }
    }

    private suspend fun scrapeHomeworkFallback(client: OkHttpClient): Result<List<CalendarEvent>> = withContext(Dispatchers.IO) {
        val docResult = fetchDocument(client, "moje_zadania")
        if (docResult.isFailure) return@withContext Result.success(emptyList<CalendarEvent>())

        val doc = docResult.getOrNull()!!
        val events = mutableListOf<CalendarEvent>()

        try {
            val rows = doc.select("table.myHomeworkTable tbody tr, table.decorated tbody tr")
            for (row in rows) {
                val cells = row.select("td")
                if (cells.size < 6) continue

                val subject = cells[0].text().trim()
                val teacher = cells[1].text().trim()
                val title = cells[2].text().trim()
                val fromDate = cells[4].text().trim()
                val toDate = cells[6].text().trim()

                if (subject.isNotBlank()) {
                    events.add(
                        CalendarEvent(
                            id = "hw_${UUID.randomUUID()}",
                            date = toDate.ifBlank { fromDate },
                            title = subject,
                            category = "Zadanie domowe",
                            description = "$title. Zadane przez $teacher.",
                            teacher = teacher,
                            timeRange = if (fromDate.isNotBlank()) "Zadano: $fromDate" else "",
                            isCompleted = false
                        )
                    )
                }
            }
            Result.success(events)
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }
}
