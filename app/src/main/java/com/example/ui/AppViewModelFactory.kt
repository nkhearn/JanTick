package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.MaintenanceRepository
import com.example.data.PhotoStorageManager

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }
    val photoStorage: PhotoStorageManager by lazy { PhotoStorageManager(appContext) }
    val repository: MaintenanceRepository by lazy {
        MaintenanceRepository(
            schoolProfileDao = database.schoolProfileDao(),
            templateDao = database.checklistTemplateDao(),
            runDao = database.maintenanceRunDao(),
            photoStorage = photoStorage
        )
    }
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ChecklistViewModel::class.java) -> {
                ChecklistViewModel(container.repository, container.photoStorage) as T
            }
            modelClass.isAssignableFrom(ReportsViewModel::class.java) -> {
                ReportsViewModel(container.repository) as T
            }
            modelClass.isAssignableFrom(SetupViewModel::class.java) -> {
                SetupViewModel(container.repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
