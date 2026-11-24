package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChecklistFrequency
import com.example.data.MaintenanceRepository
import com.example.data.RunWithItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReportsUiState(
    val selectedFilter: ChecklistFrequency? = null,
    val allRuns: List<RunWithItems> = emptyList(),
    val filteredRuns: List<RunWithItems> = emptyList()
)

class ReportsViewModel(
    private val repository: MaintenanceRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<ChecklistFrequency?>(null)

    val uiState: StateFlow<ReportsUiState> = combine(
        selectedFilter,
        repository.archivedRuns
    ) { filter, runs ->
        val filtered = if (filter == null) {
            runs
        } else {
            runs.filter { it.frequencyEnum == filter }
        }
        ReportsUiState(
            selectedFilter = filter,
            allRuns = runs,
            filteredRuns = filtered
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState()
    )

    fun setFilter(frequency: ChecklistFrequency?) {
        selectedFilter.value = frequency
    }

    fun observeRunDetail(runId: Long): Flow<RunWithItems?> {
        return repository.observeRunById(runId)
    }

    fun deleteRun(runId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteArchivedRun(runId)
            onDeleted()
        }
    }
}
