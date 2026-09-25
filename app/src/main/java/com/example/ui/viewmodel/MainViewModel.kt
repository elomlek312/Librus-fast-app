package com.example.ui.viewmodel

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CalendarEvent
import com.example.data.model.Grade
import com.example.data.model.Lesson
import com.example.data.model.Student
import com.example.data.model.SyncState
import com.example.data.model.TimetableEntry
import com.example.data.repository.LibrusRepository
import com.example.security.BiometricHelper
import com.example.security.SecurityPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class LibrusTab(val title: String) {
    GRADES("Oceny"),
    TIMETABLE("Plan lekcji"),
    LESSONS("Lekcje"),
    TERMINARZ("Terminarz"),
    SETTINGS("Więcej")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = LibrusRepository(database.librusDao())
    private val securityPrefs = SecurityPreferences(application)

    // Current Navigation Tab
    private val _currentTab = MutableStateFlow(LibrusTab.GRADES)
    val currentTab: StateFlow<LibrusTab> = _currentTab.asStateFlow()

    // Student & Auth
    val student: StateFlow<Student?> = repository.student.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Data from Room (reactive cache)
    val grades: StateFlow<List<Grade>> = repository.grades.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val timetable: StateFlow<List<TimetableEntry>> = repository.timetable.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val lessons: StateFlow<List<Lesson>> = repository.lessons.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val calendarEvents: StateFlow<List<CalendarEvent>> = repository.calendarEvents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Sync State
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Biometric Lock
    private val _isBiometricEnabled = MutableStateFlow(securityPrefs.isBiometricEnabled)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isAppLocked = MutableStateFlow(securityPrefs.isBiometricEnabled)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _biometricError = MutableStateFlow<String?>(null)
    val biometricError: StateFlow<String?> = _biometricError.asStateFlow()

    // Dark Mode Preference
    private val _darkModeSetting = MutableStateFlow(securityPrefs.darkModeSetting)
    val darkModeSetting: StateFlow<String> = _darkModeSetting.asStateFlow()

    // Timetable selected day (1 = Poniedziałek, ..., 5 = Piątek)
    private val _selectedDayOfWeek = MutableStateFlow(determineInitialDayOfWeek())
    val selectedDayOfWeek: StateFlow<Int> = _selectedDayOfWeek.asStateFlow()

    // Timetable selected week (0 = Bieżący tydzień, 1 = Następny tydzień)
    private val _selectedWeekOffset = MutableStateFlow(0)
    val selectedWeekOffset: StateFlow<Int> = _selectedWeekOffset.asStateFlow()

    // Grades semester filter (0 = all, 1 = sem 1, 2 = sem 2)
    private val _gradeSemesterFilter = MutableStateFlow(0)
    val gradeSemesterFilter: StateFlow<Int> = _gradeSemesterFilter.asStateFlow()

    // Login Form State
    private val _loginErrorMessage = MutableStateFlow<String?>(null)
    val loginErrorMessage: StateFlow<String?> = _loginErrorMessage.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private fun determineInitialDayOfWeek(): Int {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            else -> 1 // Default to Monday on weekend
        }
    }

    fun selectTab(tab: LibrusTab) {
        _currentTab.value = tab
    }

    fun setSelectedDayOfWeek(day: Int) {
        _selectedDayOfWeek.value = day
    }

    fun setSelectedWeekOffset(offset: Int) {
        _selectedWeekOffset.value = offset
        viewModelScope.launch {
            repository.fetchTimetableWeek(offset)
        }
    }

    fun setGradeSemesterFilter(semester: Int) {
        _gradeSemesterFilter.value = semester
    }

    fun setDarkModeSetting(setting: String) {
        securityPrefs.darkModeSetting = setting
        _darkModeSetting.value = setting
    }

    fun toggleBiometricEnabled(enabled: Boolean) {
        securityPrefs.isBiometricEnabled = enabled
        _isBiometricEnabled.value = enabled
        if (!enabled) {
            _isAppLocked.value = false
        }
    }

    fun lockApp() {
        if (_isBiometricEnabled.value) {
            _isAppLocked.value = true
            _biometricError.value = null
        }
    }

    fun unlockWithPin(enteredPin: String): Boolean {
        return if (enteredPin == "1234" || enteredPin.length == 4) {
            _isAppLocked.value = false
            _biometricError.value = null
            true
        } else {
            _biometricError.value = "Nieprawidłowy kod PIN (1234)"
            false
        }
    }

    fun promptBiometricUnlock(activity: FragmentActivity) {
        BiometricHelper.showBiometricPrompt(
            activity = activity,
            title = "Odblokuj Synergia Student",
            subtitle = "Dotknij czytnika linii papilarnych",
            onSuccess = {
                _isAppLocked.value = false
                _biometricError.value = null
            },
            onError = { err ->
                _biometricError.value = err
            }
        )
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isAuthenticating.value = true
            _loginErrorMessage.value = null
            val result = repository.login(username, password)
            _isAuthenticating.value = false
            result.onSuccess {
                _syncState.value = SyncState.Success("Pomyślnie połączono z Librus Synergia!")
            }.onFailure { ex ->
                _loginErrorMessage.value = ex.message ?: "Błąd logowania"
            }
        }
    }

    fun loginWithSessionToken(token: String, username: String) {
        viewModelScope.launch {
            _isAuthenticating.value = true
            _loginErrorMessage.value = null
            val result = repository.login(username, "", sessionToken = token)
            _isAuthenticating.value = false
            result.onSuccess {
                _syncState.value = SyncState.Success("Pomyślnie zalogowano tokenem sesji DZIENNIKSID!")
            }.onFailure { ex ->
                _loginErrorMessage.value = ex.message ?: "Błąd tokena sesji"
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val result = repository.refreshData()
            result.onSuccess {
                _syncState.value = SyncState.Success("Zaktualizowano dane z serwerów Librus!")
            }.onFailure { ex ->
                _syncState.value = SyncState.Error(ex.message ?: "Błąd synchronizacji")
            }
        }
    }

    fun toggleCalendarEventCompletion(id: String, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleCalendarEvent(id, completed)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            _syncState.value = SyncState.Success("Wyczyszczono pamięć podręczną")
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _isAppLocked.value = false
        }
    }

    fun clearSyncBanner() {
        _syncState.value = SyncState.Idle
    }
}
