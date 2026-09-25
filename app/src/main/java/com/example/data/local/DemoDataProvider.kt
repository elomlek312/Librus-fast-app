package com.example.data.local

import com.example.data.model.Grade
import com.example.data.model.Homework
import com.example.data.model.Lesson
import com.example.data.model.Student
import com.example.data.model.TimetableEntry

object DemoDataProvider {

    fun getDemoStudent(): Student = Student(
        id = "student_demo_1",
        name = "Jan Kowalski",
        schoolName = "I Liceum Ogólnokształcące im. M. Kopernika w Warszawie",
        className = "3B (Profil Matematyczno-Fizyczny)",
        login = "7294819u",
        isDemo = true,
        lastSyncTime = System.currentTimeMillis()
    )

    fun getDemoGrades(): List<Grade> = listOf(
        // Matematyka (Rozszerzona)
        Grade("g1", "Matematyka", "5", 5.0, 3, "Sprawdzian", "2026-09-22", "prof. M. Wiśniewski", 1, "Rachunek różniczkowy i pochodne"),
        Grade("g2", "Matematyka", "4+", 4.5, 2, "Kartkówka", "2026-09-15", "prof. M. Wiśniewski", 1, "Granice ciągów liczbowych"),
        Grade("g3", "Matematyka", "6", 6.0, 1, "Aktywność", "2026-09-10", "prof. M. Wiśniewski", 1, "Zadanie z gwiazdką na tablicy"),
        Grade("g4", "Matematyka", "5-", 4.75, 3, "Praca klasowa", "2026-09-04", "prof. M. Wiśniewski", 1, "Powtórzenie materiału z klasy 2"),

        // Język polski
        Grade("g5", "Język polski", "4", 4.0, 3, "Sprawdzian", "2026-09-20", "mgr A. Dąbrowska", 1, "Młoda Polska - motywy w literaturze"),
        Grade("g6", "Język polski", "5", 5.0, 2, "Wypracowanie", "2026-09-12", "mgr A. Dąbrowska", 1, "Tragizm postaci w 'Weselu' Stanisława Wyspiańskiego"),
        Grade("g7", "Język polski", "4+", 4.5, 1, "Odpowiedź ustna", "2026-09-08", "mgr A. Dąbrowska", 1, "Znajomość lektury"),

        // Język angielski
        Grade("g8", "Język angielski", "6", 6.0, 3, "Sprawdzian", "2026-09-23", "mgr K. Adams", 1, "Advanced Grammar & Inversion"),
        Grade("g9", "Język angielski", "5+", 5.5, 2, "Kartkówka", "2026-09-16", "mgr K. Adams", 1, "Vocabulary: Technology & Society"),
        Grade("g10", "Język angielski", "5", 5.0, 1, "Projekt", "2026-09-09", "mgr K. Adams", 1, "Presentation on AI impact"),

        // Fizyka
        Grade("g11", "Fizyka", "5", 5.0, 3, "Sprawdzian", "2026-09-21", "dr P. Lewandowski", 1, "Pole elektromagnetyczne i indukcja"),
        Grade("g12", "Fizyka", "4", 4.0, 2, "Kartkówka", "2026-09-14", "dr P. Lewandowski", 1, "Prawa Kirchhoffa"),
        Grade("g13", "Fizyka", "5-", 4.75, 2, "Ćwiczenie laboratoryjne", "2026-09-07", "dr P. Lewandowski", 1, "Wyznaczanie ogniskowej soczewki"),

        // Informatyka
        Grade("g14", "Informatyka", "6", 6.0, 3, "Projekt", "2026-09-24", "inż. T. Kamiński", 1, "Implementacja algorytmów grafowych w Kotlin"),
        Grade("g15", "Informatyka", "5", 5.0, 2, "Kartkówka", "2026-09-11", "inż. T. Kamiński", 1, "Złożoność obliczeniowa"),

        // Chemia
        Grade("g16", "Chemia", "4+", 4.5, 3, "Sprawdzian", "2026-09-19", "mgr E. Wójcik", 1, "Związki organiczne i estryfikacja"),
        Grade("g17", "Chemia", "4", 4.0, 2, "Kartkówka", "2026-09-05", "mgr E. Wójcik", 1, "Węglowodory nasycone i nienasycone"),

        // Biologia
        Grade("g18", "Biologia", "5-", 4.75, 3, "Sprawdzian", "2026-09-18", "dr B. Kaczmarek", 1, "Genetyka molekularna i replikacja DNA"),
        Grade("g19", "Biologia", "5", 5.0, 1, "Aktywność", "2026-09-10", "dr B. Kaczmarek", 1, "Analiza drzewa rodowodowego"),

        // Historia
        Grade("g20", "Historia", "4", 4.0, 2, "Sprawdzian", "2026-09-17", "mgr S. Zieliński", 1, "Dwudziestolecie międzywojenne w Polsce"),
        Grade("g21", "Historia", "5", 5.0, 1, "Odpowiedź", "2026-09-03", "mgr S. Zieliński", 1, "Konferencja paryska i traktat wersalski"),

        // Wychowanie fizyczne
        Grade("g22", "Wychowanie fizyczne", "6", 6.0, 1, "Sprawność fizyczna", "2026-09-23", "mgr R. Mazur", 1, "Bieg na 1000m"),
        Grade("g23", "Wychowanie fizyczne", "5", 5.0, 1, "Aktywność", "2026-09-16", "mgr R. Mazur", 1, "Testy siłowe")
    )

    fun getDemoTimetable(): List<TimetableEntry> = listOf(
        // Poniedziałek (1)
        TimetableEntry("t_m1", 1, 1, "08:00 - 08:45", "Matematyka", "Sala 204", "prof. M. Wiśniewski"),
        TimetableEntry("t_m2", 1, 2, "08:55 - 09:40", "Matematyka", "Sala 204", "prof. M. Wiśniewski"),
        TimetableEntry("t_m3", 1, 3, "09:50 - 10:35", "Język polski", "Sala 112", "mgr A. Dąbrowska"),
        TimetableEntry("t_m4", 1, 4, "10:50 - 11:35", "Fizyka", "Sala 301 (Fizyczna)", "dr P. Lewandowski"),
        TimetableEntry("t_m5", 1, 5, "11:50 - 12:35", "Język angielski", "Sala 105", "mgr K. Adams"),
        TimetableEntry("t_m6", 1, 6, "12:45 - 13:30", "Informatyka", "Pracownia Komp. 2", "inż. T. Kamiński"),
        TimetableEntry("t_m7", 1, 7, "13:40 - 14:25", "Wychowanie fizyczne", "Hala sportowa", "mgr R. Mazur"),

        // Wtorek (2)
        TimetableEntry("t_tu1", 2, 1, "08:00 - 08:45", "Język angielski", "Sala 105", "mgr K. Adams"),
        TimetableEntry("t_tu2", 2, 2, "08:55 - 09:40", "Historia", "Sala 201", "mgr S. Zieliński"),
        TimetableEntry("t_tu3", 2, 3, "09:50 - 10:35", "Matematyka", "Sala 204", "prof. M. Wiśniewski"),
        TimetableEntry("t_tu4", 2, 4, "10:50 - 11:35", "Chemia", "Sala 305 (Chemiczna)", "mgr E. Wójcik"),
        TimetableEntry("t_tu5", 2, 5, "11:50 - 12:35", "Biologia", "Sala 308 (Biologiczna)", "dr B. Kaczmarek"),
        TimetableEntry("t_tu6", 2, 6, "12:45 - 13:30", "Godzina wychowawcza", "Sala 204", "prof. M. Wiśniewski"),

        // Środa (3)
        TimetableEntry("t_w1", 3, 1, "08:00 - 08:45", "Fizyka", "Sala 301", "dr P. Lewandowski", statusNote = "Zastępstwo - prof. Zieliński", isSubstitution = true),
        TimetableEntry("t_w2", 3, 2, "08:55 - 09:40", "Fizyka", "Sala 301", "dr P. Lewandowski"),
        TimetableEntry("t_w3", 3, 3, "09:50 - 10:35", "Język polski", "Sala 112", "mgr A. Dąbrowska"),
        TimetableEntry("t_w4", 3, 4, "10:50 - 11:35", "Język polski", "Sala 112", "mgr A. Dąbrowska"),
        TimetableEntry("t_w5", 3, 5, "11:50 - 12:35", "Informatyka", "Pracownia Komp. 2", "inż. T. Kamiński"),
        TimetableEntry("t_w6", 3, 6, "12:45 - 13:30", "Geografia", "Sala 215", "mgr M. Kozłowska"),

        // Czwartek (4)
        TimetableEntry("t_th1", 4, 1, "08:00 - 08:45", "Matematyka", "Sala 204", "prof. M. Wiśniewski"),
        TimetableEntry("t_th2", 4, 2, "08:55 - 09:40", "Język niemiecki", "Sala 108", "mgr H. Schmidt"),
        TimetableEntry("t_th3", 4, 3, "09:50 - 10:35", "Chemia", "Sala 305", "mgr E. Wójcik"),
        TimetableEntry("t_th4", 4, 4, "10:50 - 11:35", "Wychowanie fizyczne", "Boisko / Basen", "mgr R. Mazur"),
        TimetableEntry("t_th5", 4, 5, "11:50 - 12:35", "Wychowanie fizyczne", "Boisko / Basen", "mgr R. Mazur"),
        TimetableEntry("t_th6", 4, 6, "12:45 - 13:30", "Filozofia", "Sala 114", "dr T. Barański"),

        // Piątek (5)
        TimetableEntry("t_f1", 5, 1, "08:00 - 08:45", "Biologia", "Sala 308", "dr B. Kaczmarek"),
        TimetableEntry("t_f2", 5, 2, "08:55 - 09:40", "Język angielski", "Sala 105", "mgr K. Adams"),
        TimetableEntry("t_f3", 5, 3, "09:50 - 10:35", "Matematyka", "Sala 204", "prof. M. Wiśniewski"),
        TimetableEntry("t_f4", 5, 4, "10:50 - 11:35", "Fizyka", "Sala 301", "dr P. Lewandowski"),
        TimetableEntry("t_f5", 5, 5, "11:50 - 12:35", "Historia", "Sala 201", "mgr S. Zieliński"),
        TimetableEntry("t_f6", 5, 6, "12:45 - 13:30", "Kółko matematyczne (fakultet)", "Sala 204", "prof. M. Wiśniewski")
    )

    fun getDemoLessons(): List<Lesson> = listOf(
        Lesson("l1", "2026-09-24", 6, "Geografia", "Procesy urbanizacji na świecie i megamiasta.", "mgr M. Kozłowska", "Obecność"),
        Lesson("l2", "2026-09-24", 5, "Informatyka", "Złożoność obliczeniowa i algorytmy sortowania szybkiego.", "inż. T. Kamiński", "Obecność"),
        Lesson("l3", "2026-09-24", 4, "Język polski", "Konteksty filozoficzne w poezji młodopolskiej.", "mgr A. Dąbrowska", "Obecność"),
        Lesson("l4", "2026-09-24", 3, "Język polski", "Symbolizm i synestezja w liryce Kazimierza Przerwy-Tetmajera.", "mgr A. Dąbrowska", "Obecność"),
        Lesson("l5", "2026-09-24", 2, "Fizyka", "Zjawisko indukcji elektromagnetycznej i prawo Faradaya.", "dr P. Lewandowski", "Obecność"),
        Lesson("l6", "2026-09-24", 1, "Fizyka", "Obliczanie siły elektromotorycznej w polu magnetycznym.", "dr P. Lewandowski", "Obecność"),
        Lesson("l7", "2026-09-23", 5, "Biologia", "Synteza białek - transkrypcja i translacja kodu genetycznego.", "dr B. Kaczmarek", "Obecność"),
        Lesson("l8", "2026-09-23", 4, "Chemia", "Reakcje addycji i polimeryzacji węglowodorów.", "mgr E. Wójcik", "Spóźnienie"),
        Lesson("l9", "2026-09-23", 3, "Matematyka", "Twierdzenie Lagrange'a o wartości średniej i ekstrema funkcji.", "prof. M. Wiśniewski", "Obecność"),
        Lesson("l10", "2026-09-23", 2, "Historia", "Polityka zagraniczna II Rzeczypospolitej w latach 30.", "mgr S. Zieliński", "Obecność"),
        Lesson("l11", "2026-09-23", 1, "Język angielski", "Conditionals type 0, 1, 2, 3 and mixed conditionals in academic context.", "mgr K. Adams", "Obecność")
    )

    fun getDemoHomework(): List<Homework> = listOf(
        Homework(
            id = "hw1",
            subject = "Matematyka",
            topic = "Zadania z pochodnych i ekstremów lokalnych",
            content = "Podręcznik str. 84, zadania 4.12 - 4.18 (podpunkty a, c, d). Przygotować dowód monotoniczności funkcji w zeszycie.",
            deadline = "2026-09-28",
            creationDate = "2026-09-24",
            teacher = "prof. M. Wiśniewski",
            isCompleted = false
        ),
        Homework(
            id = "hw2",
            subject = "Język polski",
            topic = "Rozprawka: Wesele Wyspiańskiego jako diagnoza społeczeństwa",
            content = "Napisz rozprawkę na minimum 400 słów. Odwołaj się do 'Wesela' oraz innego tekstu kultury o tematyce narodowej.",
            deadline = "2026-09-29",
            creationDate = "2026-09-22",
            teacher = "mgr A. Dąbrowska",
            isCompleted = false
        ),
        Homework(
            id = "hw3",
            subject = "Fizyka",
            topic = "Sprawozdanie z doświadczenia: Reguła Lenza",
            content = "Uzupełnij arkusz laboratoryjny nr 3. Sporządź wykres zależności indukowanego prądu od prędkości magnesu.",
            deadline = "2026-09-26",
            creationDate = "2026-09-23",
            teacher = "dr P. Lewandowski",
            isCompleted = true
        ),
        Homework(
            id = "hw4",
            subject = "Informatyka",
            topic = "Projekt: Algorytm Dijkstry",
            content = "Zaimplementować znajdowanie najkrótszej ścieżki w grafie ważonym. Wysłać link do repozytorium GitHub na platformę szkolną.",
            deadline = "2026-10-02",
            creationDate = "2026-09-24",
            teacher = "inż. T. Kamiński",
            isCompleted = false
        ),
        Homework(
            id = "hw5",
            subject = "Język angielski",
            topic = "Essay: The Future of Renewable Energy",
            content = "Write an argumentative essay (200-250 words) discussing whether solar and wind can completely replace fossil fuels by 2040.",
            deadline = "2026-09-27",
            creationDate = "2026-09-21",
            teacher = "mgr K. Adams",
            isCompleted = true
        )
    )
}
