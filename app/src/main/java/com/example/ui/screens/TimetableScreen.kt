package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimetableEntry
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TimetableScreen(
    timetable: List<TimetableEntry>,
    selectedDay: Int,
    selectedWeekOffset: Int,
    onDaySelected: (Int) -> Unit,
    onWeekOffsetSelected: (Int) -> Unit
) {
    val dayNames = listOf(
        1 to "Poniedziałek",
        2 to "Wtorek",
        3 to "Środa",
        4 to "Czwartek",
        5 to "Piątek"
    )

    // Current time timer that ticks every 30 seconds
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val currentCalendar = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    val todayDayOfWeek = when (currentCalendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        else -> 1
    }

    // Filter by week offset and day
    val dayEntries = remember(timetable, selectedDay, selectedWeekOffset) {
        timetable.filter { it.dayOfWeek == selectedDay && it.weekOffset == selectedWeekOffset }
            .sortedBy { it.period }
    }

    // Calculate dates for current and next week
    val weekDateRange = remember(selectedWeekOffset) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.add(Calendar.WEEK_OF_YEAR, selectedWeekOffset)
        val daysSinceMonday = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
        val startFormat = SimpleDateFormat("d MMM", Locale("pl"))
        val startDate = startFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, 4) // Friday
        val endFormat = SimpleDateFormat("d MMM yyyy", Locale("pl"))
        val endDate = endFormat.format(cal.time)
        "$startDate – $endDate"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("timetable_screen")
    ) {
        // Week Selector Header (Bieżący tydzień / Następny tydzień)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onWeekOffsetSelected(selectedWeekOffset - 1) },
                    enabled = selectedWeekOffset > 0,
                    modifier = Modifier.testTag("prev_week_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Poprzedni tydzień"
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (selectedWeekOffset) {
                            0 -> "Bieżący tydzień"
                            1 -> "Następny tydzień"
                            else -> "Tydzień (+$selectedWeekOffset)"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = weekDateRange,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onWeekOffsetSelected(selectedWeekOffset + 1) },
                    modifier = Modifier.testTag("next_week_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Następny tydzień"
                    )
                }
            }
        }

        // Day Switcher Tabs
        ScrollableTabRow(
            selectedTabIndex = (selectedDay - 1).coerceIn(0, 4),
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            dayNames.forEach { (dayIndex, name) ->
                val isSelected = selectedDay == dayIndex
                val isToday = todayDayOfWeek == dayIndex && selectedWeekOffset == 0

                Tab(
                    selected = isSelected,
                    onClick = { onDaySelected(dayIndex) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isToday) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    },
                    modifier = Modifier.testTag("day_tab_$dayIndex")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (dayEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedWeekOffset == 1) "Brak zajęć w następnym tygodniu" else "Brak zajęć w tym dniu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            val isViewingToday = (selectedDay == todayDayOfWeek && selectedWeekOffset == 0)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dayEntries, key = { it.id }) { entry ->
                    val progressRatio = if (isViewingToday) {
                        calculateLessonProgress(entry.timeRange, currentTimeMillis)
                    } else {
                        null
                    }

                    TimetableEntryCard(
                        entry = entry,
                        progressRatio = progressRatio
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * Calculates current progress in the lesson:
 * returns 0.0f .. 1.0f if currently active during lesson time, null otherwise
 */
private fun calculateLessonProgress(timeRange: String, currentTimeMillis: Long): Float? {
    try {
        val parts = timeRange.split("-", "–").map { it.trim() }
        if (parts.size < 2) return null

        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val startParts = parts[0].split(":", ".").mapNotNull { it.trim().toIntOrNull() }
        val endParts = parts[1].split(":", ".").mapNotNull { it.trim().toIntOrNull() }

        if (startParts.size >= 2 && endParts.size >= 2) {
            val startMinutes = startParts[0] * 60 + startParts[1]
            val endMinutes = endParts[0] * 60 + endParts[1]

            if (currentMinutes in startMinutes..endMinutes) {
                val duration = (endMinutes - startMinutes).coerceAtLeast(1)
                val elapsed = currentMinutes - startMinutes
                return (elapsed.toFloat() / duration).coerceIn(0.05f, 0.95f)
            }
        }
    } catch (_: Exception) {}
    return null
}

@Composable
fun TimetableEntryCard(
    entry: TimetableEntry,
    progressRatio: Float? = null // e.g. 0.5f if half-way through lesson
) {
    val isOngoing = progressRatio != null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timetable_entry_${entry.id}")
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isOngoing -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    entry.isCancelled -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    entry.isSubstitution -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
            border = if (isOngoing) {
                androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            } else null,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Period Number Box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isOngoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${entry.period}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOngoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.subject,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isOngoing) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(10.dp),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "TERAZ",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Classroom badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MeetingRoom,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = entry.classroom,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = entry.timeRange,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = entry.teacher,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (entry.isSubstitution || entry.isCancelled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (entry.isCancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = entry.statusNote,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // DIAGONAL PROGRESS LINE OVER CURRENT LESSON (User Request)
        // If lesson is ongoing at 9.10 and started at 8.55, the diagonal line crosses about in the middle over that lesson
        if (progressRatio != null) {
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                val cardWidth = size.width
                val cardHeight = size.height

                // The diagonal crossing point X advances across the card proportional to elapsed time
                val currentX = cardWidth * progressRatio

                // Draw vivid diagonal progress cut line through current lesson card
                val deltaX = 35.dp.toPx()
                val startX = (currentX - deltaX).coerceAtLeast(0f)
                val endX = (currentX + deltaX).coerceAtMost(cardWidth)

                // Top-to-bottom diagonal cut
                drawLine(
                    color = primaryColor.copy(alpha = 0.85f),
                    start = Offset(startX, 0f),
                    end = Offset(endX, cardHeight),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Small indicator dot at current cross point
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = Offset(currentX, cardHeight / 2f)
                )
            }
        }
    }
}
