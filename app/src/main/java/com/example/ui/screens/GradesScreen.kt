package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Grade
import com.example.ui.components.GradeBadge
import com.example.ui.components.getGradeColor
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GradesScreen(
    grades: List<Grade>,
    selectedSemester: Int,
    onSemesterSelected: (Int) -> Unit
) {
    var selectedGradeForDetails by remember { mutableStateOf<Grade?>(null) }
    var showSimulator by remember { mutableStateOf(false) }

    val filteredGrades = remember(grades, selectedSemester) {
        if (selectedSemester == 0) grades else grades.filter { it.semester == selectedSemester }
    }

    // Group by Subject
    val groupedGrades = remember(filteredGrades) {
        filteredGrades.groupBy { it.subject }
    }

    // Overall weighted average
    val overallAverage = remember(filteredGrades) {
        calculateWeightedAverage(filteredGrades)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("grades_list")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Overall Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("grades_summary_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Średnia ważona ocen",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (overallAverage > 0) String.format(Locale.US, "%.2f", overallAverage) else "–",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = "Liczba wystawionych ocen: ${filteredGrades.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    // Simulator button
                    Button(
                        onClick = { showSimulator = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("open_grade_simulator_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Symulator")
                    }
                }
            }
        }

        // Semester Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "Wszystkie",
                    1 to "Semestr 1",
                    2 to "Semestr 2"
                ).forEach { (sem, label) ->
                    FilterChip(
                        selected = selectedSemester == sem,
                        onClick = { onSemesterSelected(sem) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("semester_chip_$sem")
                    )
                }
            }
        }

        if (groupedGrades.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Grade,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Brak ocen w wybranym semestrze",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Subject Grade Cards
        items(groupedGrades.entries.toList(), key = { it.key }) { (subject, subjectGrades) ->
            val subjectAvg = calculateWeightedAverage(subjectGrades)
            val avgColor = getGradeColor(subjectAvg)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subject_card_${subject}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = avgColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Śr: ${String.format(Locale.US, "%.2f", subjectAvg)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = avgColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        subjectGrades.forEach { grade ->
                            GradeBadge(
                                grade = grade,
                                onClick = { selectedGradeForDetails = grade }
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Grade Details Dialog
    selectedGradeForDetails?.let { grade ->
        val gradeColor = getGradeColor(grade.numericValue)

        AlertDialog(
            onDismissRequest = { selectedGradeForDetails = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(gradeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = grade.grade,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = gradeColor
                    )
                }
            },
            title = {
                Text(
                    text = grade.subject,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow(label = "Kategoria:", value = grade.category)
                    DetailRow(label = "Waga oceny:", value = "${grade.weight}")
                    DetailRow(label = "Wartość numeryczna:", value = "${grade.numericValue}")
                    DetailRow(label = "Data wpisu:", value = grade.date)
                    DetailRow(label = "Nauczyciel:", value = grade.teacher)
                    DetailRow(label = "Semestr:", value = "Semestr ${grade.semester}")
                    if (grade.comment.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Opis / komentarz:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = grade.comment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { selectedGradeForDetails = null },
                    modifier = Modifier.testTag("close_grade_details_button")
                ) {
                    Text("Zamknij")
                }
            }
        )
    }

    // What-If Grade Simulator Dialog
    if (showSimulator) {
        val subjects = remember(grades) { grades.map { it.subject }.distinct() }
        var selectedSubject by remember { mutableStateOf(subjects.firstOrNull() ?: "Matematyka") }
        var simGradeValue by remember { mutableDoubleStateOf(5.0) }
        var simWeight by remember { mutableIntStateOf(3) }

        val subjectCurrentGrades = remember(grades, selectedSubject) {
            grades.filter { it.subject == selectedSubject }
        }
        val currentSubjectAvg = remember(subjectCurrentGrades) {
            calculateWeightedAverage(subjectCurrentGrades)
        }
        val simulatedSubjectAvg = remember(subjectCurrentGrades, simGradeValue, simWeight) {
            val totalWeight = subjectCurrentGrades.sumOf { it.weight } + simWeight
            val totalScore = subjectCurrentGrades.sumOf { it.numericValue * it.weight } + (simGradeValue * simWeight)
            if (totalWeight > 0) totalScore / totalWeight else 0.0
        }

        AlertDialog(
            onDismissRequest = { showSimulator = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Symulator Ocen (Co jeśli?)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Sprawdź, jak potencjalna ocena wpłynie na Twoją średnią z wybranego przedmiotu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Subject Chips Flow
                    Text(
                        text = "Wybierz przedmiot:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjects.take(6).forEach { subj ->
                            FilterChip(
                                selected = selectedSubject == subj,
                                onClick = { selectedSubject = subj },
                                label = { Text(subj, fontSize = 12.sp) }
                            )
                        }
                    }

                    // Proposed Grade Selector
                    Text(
                        text = "Symulowana ocena: ${simGradeValue.toInt()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).forEach { gVal ->
                            FilterChip(
                                selected = simGradeValue == gVal,
                                onClick = { simGradeValue = gVal },
                                label = { Text("${gVal.toInt()}") }
                            )
                        }
                    }

                    // Weight Selector
                    Text(
                        text = "Waga oceny: $simWeight",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(1, 2, 3, 4, 5).forEach { w ->
                            FilterChip(
                                selected = simWeight == w,
                                onClick = { simWeight = w },
                                label = { Text("Waga $w") }
                            )
                        }
                    }

                    // Simulation Result Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Aktualna średnia", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = String.format(Locale.US, "%.2f", currentSubjectAvg),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Text("➔", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Nowa średnia", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = String.format(Locale.US, "%.2f", simulatedSubjectAvg),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = if (simulatedSubjectAvg >= currentSubjectAvg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSimulator = false },
                    modifier = Modifier.testTag("close_simulator_dialog_button")
                ) {
                    Text("Zamknij")
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun calculateWeightedAverage(grades: List<Grade>): Double {
    val totalWeight = grades.sumOf { it.weight }
    if (totalWeight == 0) return 0.0
    val totalScore = grades.sumOf { it.numericValue * it.weight }
    return totalScore / totalWeight
}
