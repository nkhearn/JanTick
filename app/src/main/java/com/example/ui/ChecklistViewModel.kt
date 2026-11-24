package com.example.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChecklistFrequency
import com.example.data.MaintenanceRepository
import com.example.data.MaintenanceRunItemEntity
import com.example.data.PhotoStorageManager
import com.example.data.RunWithItems
import com.example.data.SchoolProfileEntity
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChecklistUiState(
    val isLoading: Boolean = true,
    val frequency: ChecklistFrequency = ChecklistFrequency.DAILY,
    val profile: SchoolProfileEntity = SchoolProfileEntity(),
    val activeRun: RunWithItems? = null,
    val isFinalizing: Boolean = false
) {
    val items: List<MaintenanceRunItemEntity>
        get() = activeRun?.items.orEmpty()

    val tickedCount: Int
        get() = activeRun?.tickedCount ?: 0

    val totalCount: Int
        get() = activeRun?.totalCount ?: 0

    val progress: Float
        get() = if (totalCount > 0) tickedCount.toFloat() / totalCount.toFloat() else 0f

    val allTicked: Boolean
        get() = totalCount > 0 && tickedCount == totalCount

    val uncheckedCount: Int
        get() = (totalCount - tickedCount).coerceAtLeast(0)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChecklistViewModel(
    private val repository: MaintenanceRepository,
    private val photoStorage: PhotoStorageManager
) : ViewModel() {

    private val selectedFrequency = MutableStateFlow(ChecklistFrequency.DAILY)
    private val isFinalizing = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    val uiState: StateFlow<ChecklistUiState> = combine(
        selectedFrequency,
        repository.schoolProfile,
        selectedFrequency.flatMapLatest { repository.observeActiveRun(it) },
        isFinalizing
    ) { freq, profile, activeRun, finalizing ->
        ChecklistUiState(
            isLoading = activeRun == null,
            frequency = freq,
            profile = profile ?: SchoolProfileEntity(),
            activeRun = activeRun,
            isFinalizing = finalizing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChecklistUiState()
    )

    fun selectFrequency(frequency: ChecklistFrequency) {
        selectedFrequency.value = frequency
    }

    fun toggleTick(item: MaintenanceRunItemEntity) {
        viewModelScope.launch {
            repository.toggleItemTick(item.id, !item.isTicked)
        }
    }

    fun saveItemDetails(itemId: Long, note: String, photoPaths: List<String>) {
        viewModelScope.launch {
            repository.updateItemDetails(itemId, note, photoPaths)
        }
    }

    fun createCameraCaptureTarget(): Pair<File, Uri> {
        return photoStorage.createCameraCaptureFileAndUri()
    }

    fun importPickedPhoto(uri: Uri, onImported: (String?) -> Unit) {
        viewModelScope.launch {
            val path = photoStorage.importPhotoFromUri(uri)
            onImported(path)
        }
    }

    fun importCapturedPhoto(file: File, onImported: (String?) -> Unit) {
        viewModelScope.launch {
            val path = photoStorage.importCapturedFile(file)
            onImported(path)
        }
    }

    fun deletePhotoFile(path: String) {
        viewModelScope.launch {
            photoStorage.deletePhotoFile(path)
        }
    }

    fun finalizeCurrentRun(finalizationNote: String, onSaved: (Long) -> Unit) {
        val runId = uiState.value.activeRun?.run?.id ?: return
        if (isFinalizing.value) return
        viewModelScope.launch {
            isFinalizing.value = true
            try {
                val savedId = repository.finalizeRun(runId, finalizationNote)
                if (savedId != null) {
                    onSaved(savedId)
                }
            } finally {
                isFinalizing.value = false
            }
        }
    }
}
