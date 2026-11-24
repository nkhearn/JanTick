package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChecklistFrequency
import com.example.data.ChecklistTemplateEntity
import com.example.data.MaintenanceRepository
import com.example.data.SchoolProfileEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SetupUiState(
    val frequency: ChecklistFrequency = ChecklistFrequency.DAILY,
    val profile: SchoolProfileEntity = SchoolProfileEntity(),
    val templates: List<ChecklistTemplateEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModel(
    private val repository: MaintenanceRepository
) : ViewModel() {

    private val selectedFrequency = MutableStateFlow(ChecklistFrequency.DAILY)

    val uiState: StateFlow<SetupUiState> = combine(
        selectedFrequency,
        repository.schoolProfile,
        selectedFrequency.flatMapLatest { repository.observeTemplates(it) }
    ) { freq, profile, templates ->
        SetupUiState(
            frequency = freq,
            profile = profile ?: SchoolProfileEntity(),
            templates = templates
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SetupUiState()
    )

    fun selectFrequency(frequency: ChecklistFrequency) {
        selectedFrequency.value = frequency
    }

    fun saveProfile(schoolName: String, custodianName: String, campusLabel: String) {
        viewModelScope.launch {
            repository.updateSchoolProfile(schoolName, custodianName, campusLabel)
        }
    }

    fun addTask(zone: String, title: String) {
        val freq = selectedFrequency.value
        viewModelScope.launch {
            repository.addTemplateItem(freq, zone, title)
        }
    }

    fun updateTask(template: ChecklistTemplateEntity, newZone: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateTemplateItem(template.copy(zone = newZone, title = newTitle))
        }
    }

    fun moveTask(templateId: Long, moveUp: Boolean) {
        val freq = selectedFrequency.value
        viewModelScope.launch {
            repository.moveTemplateItem(freq, templateId, moveUp)
        }
    }

    fun deleteTask(templateId: Long) {
        val freq = selectedFrequency.value
        viewModelScope.launch {
            repository.deleteTemplateItem(freq, templateId)
        }
    }

    fun restoreDefaults() {
        val freq = selectedFrequency.value
        viewModelScope.launch {
            repository.resetFrequencyToDefaults(freq)
        }
    }
}
