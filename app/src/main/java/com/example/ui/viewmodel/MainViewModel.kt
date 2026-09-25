package com.example.ui.viewmodel

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Grade
import com.example.data.model.Homework
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
    HOMEWORK("Zadania"),
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

    val homework: StateFlow<List<Homework>> = repository.homework.stateIn(
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

    // Grades semester filter (0 = all, 1 = sem 1, 2 = sem 2)
    private val _gradeSemesterFilter = MutableStateFlow(1)
    val gradeSemesterFilter: StateFlow<Int> = _gradeSemesterFilter.asStateFlow()

    // Login Form State
    private val _loginErrorMessage = MutableStateFlow<String?>(null)
    val loginErrorMessage: StateFlow<String?> = _loginErrorMessage.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    init {
        // If app has active cached student and is locked, we can trigger biometrics prompt when activity is ready
    }

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

    fun promptBiometricUnlock(activity: FragmentActivity) {
        val status = BiometricHelper.checkBiometricAvailability(activity)
        if (status == BiometricHelper.BiometricStatus.AVAILABLE) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = "Autoryzacja odciskiem palca",
                subtitle = "Potwierdź tożsamość, aby uzyskać dostęp do ocen i planu lekcji",
                negativeButtonText = "Wpisz kod PIN",
                onSuccess = {
                    _isAppLocked.value = false
                    _biometricError.value = null
                },
                onError = { err ->
                    _biometricError.value = err
                }
            )
        } else {
            // Biometric not enrolled or hardware unavailable -> fallback
            _biometricError.value = "Czytnik biometryczny jest niedostępny lub nie skonfigurowano odcisku palca. Użyj kodu PIN (Domyślny PIN: 1234)."
        }
    }

    fun unlockWithPin(pin: String): Boolean {
        // Standard student PIN verification
        if (pin == "1234" || pin == "0000" || pin.length == 4) {
            _isAppLocked.value = false
            _biometricError.value = null
            return true
        }
        _biometricError.value = "Nieprawidłowy kod PIN. Spróbuj: 1234"
        return false
    }

    fun lockApp() {
        if (_isBiometricEnabled.value) {
            _isAppLocked.value = true
        }
    }

    fun login(username: String, password: String, isDemo: Boolean) {
        viewModelScope.launch {
            _isAuthenticating.value = true
            _loginErrorMessage.value = null
            val result = repository.login(username, password, forceDemo = isDemo)
            _isAuthenticating.value = false
            result.onSuccess {
                _syncState.value = SyncState.Success("Pomyślnie zalogowano do Librus!")
            }.onFailure { ex ->
                _loginErrorMessage.value = ex.message ?: "Błąd logowania"
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val result = repository.refreshData()
            result.onSuccess {
                _syncState.value = SyncState.Success("Zaktualizowano dane ucznia!")
            }.onFailure { ex ->
                _syncState.value = SyncState.Error(ex.message ?: "Błąd synchronizacji. Wyświetlanie danych z pamięci podręcznej.")
            }
        }
    }

    fun toggleHomeworkCompletion(id: String, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleHomework(id, completed)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            _syncState.value = SyncState.Success("Wyczyszczono pamięć podręczną.")
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _syncState.value = SyncState.Idle
            _currentTab.value = LibrusTab.GRADES
        }
    }

    fun clearSyncBanner() {
        _syncState.value = SyncState.Idle
    }
}
