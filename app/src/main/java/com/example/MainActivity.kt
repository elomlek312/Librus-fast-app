package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.SyncState
import com.example.ui.components.SyncStatusBar
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.GradesScreen
import com.example.ui.screens.HomeworkScreen
import com.example.ui.screens.LessonsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TimetableScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LibrusTab
import com.example.ui.viewmodel.MainViewModel

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()

            val darkModeSetting by viewModel.darkModeSetting.collectAsStateWithLifecycle()
            val useDarkTheme = when (darkModeSetting) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                MainAppContent(
                    activity = this,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    activity: FragmentActivity,
    viewModel: MainViewModel
) {
    val student by viewModel.student.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val biometricError by viewModel.biometricError.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val grades by viewModel.grades.collectAsStateWithLifecycle()
    val timetable by viewModel.timetable.collectAsStateWithLifecycle()
    val lessons by viewModel.lessons.collectAsStateWithLifecycle()
    val homework by viewModel.homework.collectAsStateWithLifecycle()

    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val selectedDayOfWeek by viewModel.selectedDayOfWeek.collectAsStateWithLifecycle()
    val gradeSemesterFilter by viewModel.gradeSemesterFilter.collectAsStateWithLifecycle()
    val darkModeSetting by viewModel.darkModeSetting.collectAsStateWithLifecycle()

    val loginErrorMessage by viewModel.loginErrorMessage.collectAsStateWithLifecycle()
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()

    // Handle Android system Back button when navigating secondary tabs
    BackHandler(enabled = currentTab != LibrusTab.GRADES && student != null && !isAppLocked) {
        viewModel.selectTab(LibrusTab.GRADES)
    }

    // Auto prompt biometric when app is locked and student is logged in
    LaunchedEffect(isAppLocked, student) {
        if (isAppLocked && student != null && isBiometricEnabled) {
            viewModel.promptBiometricUnlock(activity)
        }
    }

    when {
        student == null -> {
            LoginScreen(
                isAuthenticating = isAuthenticating,
                errorMessage = loginErrorMessage,
                onLogin = { user, pass, isDemo ->
                    viewModel.login(user, pass, isDemo)
                }
            )
        }
        isAppLocked -> {
            BiometricLockScreen(
                studentName = student?.name ?: "Uczeń",
                onPromptBiometrics = { viewModel.promptBiometricUnlock(activity) },
                onUnlockWithPin = { pin -> viewModel.unlockWithPin(pin) },
                errorMessage = biometricError
            )
        }
        else -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                Text(
                                    text = currentTab.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${student?.name} • ${student?.className}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        actions = {
                            // Quick lock button if biometrics enabled
                            if (isBiometricEnabled) {
                                IconButton(
                                    onClick = { viewModel.lockApp() },
                                    modifier = Modifier.testTag("appbar_lock_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Zablokuj aplikację",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Sync button
                            val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 1000, easing = LinearEasing)
                                ),
                                label = "sync_rotation_value"
                            )

                            IconButton(
                                onClick = { viewModel.refreshData() },
                                modifier = Modifier.testTag("appbar_sync_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Synchronizuj z Librus",
                                    modifier = if (syncState is SyncState.Syncing) Modifier.rotate(rotation) else Modifier,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        LibrusTab.values().forEach { tab ->
                            val isSelected = currentTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = when (tab) {
                                            LibrusTab.GRADES -> if (isSelected) Icons.Filled.Grade else Icons.Outlined.Grade
                                            LibrusTab.TIMETABLE -> if (isSelected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth
                                            LibrusTab.LESSONS -> if (isSelected) Icons.Filled.Book else Icons.Outlined.Book
                                            LibrusTab.HOMEWORK -> if (isSelected) Icons.Filled.Assignment else Icons.Outlined.Assignment
                                            LibrusTab.SETTINGS -> if (isSelected) Icons.Filled.Settings else Icons.Outlined.Settings
                                        },
                                        contentDescription = tab.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Sync / Offline banner
                    SyncStatusBar(
                        syncState = syncState,
                        isOfflineData = (student?.isDemo == false && syncState is SyncState.Idle),
                        onDismiss = { viewModel.clearSyncBanner() },
                        onRetry = { viewModel.refreshData() }
                    )

                    // Tab Content Switcher with smooth fade animation
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                        label = "tab_content_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { tab ->
                        when (tab) {
                            LibrusTab.GRADES -> {
                                GradesScreen(
                                    grades = grades,
                                    selectedSemester = gradeSemesterFilter,
                                    onSemesterSelected = { viewModel.setGradeSemesterFilter(it) }
                                )
                            }
                            LibrusTab.TIMETABLE -> {
                                TimetableScreen(
                                    timetable = timetable,
                                    selectedDay = selectedDayOfWeek,
                                    onDaySelected = { viewModel.setSelectedDayOfWeek(it) }
                                )
                            }
                            LibrusTab.LESSONS -> {
                                LessonsScreen(lessons = lessons)
                            }
                            LibrusTab.HOMEWORK -> {
                                HomeworkScreen(
                                    homeworkList = homework,
                                    onToggleCompletion = { id, comp ->
                                        viewModel.toggleHomeworkCompletion(id, comp)
                                    }
                                )
                            }
                            LibrusTab.SETTINGS -> {
                                SettingsScreen(
                                    student = student,
                                    isBiometricEnabled = isBiometricEnabled,
                                    onToggleBiometrics = { viewModel.toggleBiometricEnabled(it) },
                                    onLockAppNow = { viewModel.lockApp() },
                                    darkModeSetting = darkModeSetting,
                                    onSelectDarkMode = { viewModel.setDarkModeSetting(it) },
                                    gradesCount = grades.size,
                                    timetableCount = timetable.size,
                                    homeworkCount = homework.size,
                                    onSyncNow = { viewModel.refreshData() },
                                    onClearCache = { viewModel.clearCache() },
                                    onLogout = { viewModel.logout() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
