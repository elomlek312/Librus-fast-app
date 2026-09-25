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
                            text = "Liczba ocen: ${filteredGrades.size}",
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

        // Semester Filter Chips (Default is 0 = Wszystkie, so user immediately sees all grades)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "Wszystkie (${grades.size})",
                    1 to "Semestr 1 (${grades.count { it.semester == 1 }})",
                    2 to "Semestr 2 (${grades.count { it.semester == 2 }})"
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
                            text = "Brak zarejestrowanych ocen w wybranym semestrze",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(groupedGrades.keys.toList(), key = { it }) { subjectName ->
                val subjectGrades = groupedGrades[subjectName] ?: emptyList()
                val subjectAverage = calculateWeightedAverage(subjectGrades)

                SubjectGradesCard(
                    subjectName = subjectName,
                    grades = subjectGrades,
                    average = subjectAverage,
                    onGradeClick = { selectedGradeForDetails = it }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Grade Details Modal
    selectedGradeForDetails?.let { grade ->
        GradeDetailDialog(
            grade = grade,
            onDismiss = { selectedGradeForDetails = null }
        )
    }

    // Grade Simulator Dialog
    if (showSimulator) {
        GradeSimulatorDialog(
            currentGrades = filteredGrades,
            currentAverage = overallAverage,
            onDismiss = { showSimulator = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectGradesCard(
    subjectName: String,
    grades: List<Grade>,
    average: Double,
    onGradeClick: (Grade) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_card_${subjectName.replace(" ", "_")}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    text = subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Subject Average Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = getGradeColor(average).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (average > 0) String.format(Locale.US, "%.2f", average) else "–",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = getGradeColor(average),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Grades list flow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                grades.forEach { grade ->
                    GradeBadge(
                        grade = grade,
                        onClick = { onGradeClick(grade) }
                    )
                }
            }
        }
    }
}

@Composable
fun GradeDetailDialog(
    grade: Grade,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = grade.subject,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = CircleShape,
                    color = getGradeColor(grade.numericValue).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = grade.grade,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = getGradeColor(grade.numericValue),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider()
                DetailRow(label = "Kategoria:", value = grade.category)
                DetailRow(label = "Waga oceny:", value = "${grade.weight}")
                DetailRow(label = "Wartość numeryczna:", value = String.format(Locale.US, "%.2f", grade.numericValue))
                DetailRow(label = "Data wpisu:", value = grade.date)
                DetailRow(label = "Nauczyciel:", value = grade.teacher)
                DetailRow(label = "Semestr:", value = "Semestr ${grade.semester}")
                if (grade.comment.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Komentarz / Opis:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = grade.comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_grade_details_button")
            ) {
                Text("Zamknij")
            }
        }
    )
}

@Composable
fun GradeSimulatorDialog(
    currentGrades: List<Grade>,
    currentAverage: Double,
    onDismiss: () -> Unit
) {
    var hypotheticalGrade by remember { mutableDoubleStateOf(5.0) }
    var hypotheticalWeight by remember { mutableIntStateOf(2) }

    val simulatedAverage = remember(currentGrades, hypotheticalGrade, hypotheticalWeight) {
        val totalCurrentWeight = currentGrades.sumOf { it.weight }
        val currentSum = currentGrades.sumOf { it.numericValue * it.weight }
        val newSum = currentSum + (hypotheticalGrade * hypotheticalWeight)
        val newWeight = totalCurrentWeight + hypotheticalWeight
        if (newWeight > 0) newSum / newWeight else currentAverage
    }

    val diff = simulatedAverage - currentAverage

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Symulator średniej",
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
                    text = "Sprawdź jak przyszła ocena wpłynie na Twoją średnią ważoną:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Current vs Projected
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Aktualna", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = if (currentAverage > 0) String.format(Locale.US, "%.2f", currentAverage) else "–",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text("➔", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Po dodaniu", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = String.format(Locale.US, "%.2f", simulatedAverage),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (diff >= 0) Color(0xFF1E8E3E) else Color(0xFFD93025)
                            )
                        }
                    }
                }

                // Grade Selector
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Planowana ocena:", style = MaterialTheme.typography.labelMedium)
                        Text("${hypotheticalGrade.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = hypotheticalGrade.toFloat(),
                        onValueChange = { hypotheticalGrade = it.toInt().toDouble() },
                        valueRange = 1f..6f,
                        steps = 4,
                        modifier = Modifier.testTag("simulator_grade_slider")
                    )
                }

                // Weight Selector
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Waga oceny:", style = MaterialTheme.typography.labelMedium)
                        Text("$hypotheticalWeight", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = hypotheticalWeight.toFloat(),
                        onValueChange = { hypotheticalWeight = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.testTag("simulator_weight_slider")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_simulator_button")
            ) {
                Text("Gotowe")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun calculateWeightedAverage(grades: List<Grade>): Double {
    val totalWeight = grades.sumOf { it.weight }
    if (totalWeight == 0) return 0.0
    val totalSum = grades.sumOf { it.numericValue * it.weight }
    return totalSum / totalWeight
}
