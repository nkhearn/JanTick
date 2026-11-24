package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolProfileDao {
    @Query("SELECT * FROM school_profile WHERE id = 1")
    fun observeProfile(): Flow<SchoolProfileEntity?>

    @Query("SELECT * FROM school_profile WHERE id = 1")
    suspend fun getProfileOnce(): SchoolProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: SchoolProfileEntity)
}

@Dao
interface ChecklistTemplateDao {
    @Query("SELECT * FROM checklist_templates WHERE frequency = :frequency ORDER BY sortOrder ASC, id ASC")
    fun observeTemplatesByFrequency(frequency: String): Flow<List<ChecklistTemplateEntity>>

    @Query("SELECT * FROM checklist_templates WHERE frequency = :frequency ORDER BY sortOrder ASC, id ASC")
    suspend fun getTemplatesByFrequencyOnce(frequency: String): List<ChecklistTemplateEntity>

    @Query("SELECT COUNT(*) FROM checklist_templates")
    suspend fun countTemplates(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ChecklistTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<ChecklistTemplateEntity>): List<Long>

    @Update
    suspend fun updateTemplate(template: ChecklistTemplateEntity)

    @Update
    suspend fun updateTemplates(templates: List<ChecklistTemplateEntity>)

    @Query("DELETE FROM checklist_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Long)

    @Query("DELETE FROM checklist_templates WHERE frequency = :frequency")
    suspend fun deleteTemplatesByFrequency(frequency: String)
}

@Dao
interface MaintenanceRunDao {
    @Transaction
    @Query(
        "SELECT * FROM maintenance_runs WHERE frequency = :frequency AND status = 'IN_PROGRESS' " +
            "ORDER BY createdAtMillis DESC LIMIT 1"
    )
    fun observeActiveRunWithItems(frequency: String): Flow<RunWithItems?>

    @Transaction
    @Query(
        "SELECT * FROM maintenance_runs WHERE status != 'IN_PROGRESS' " +
            "ORDER BY COALESCE(finalizedAtMillis, createdAtMillis) DESC"
    )
    fun observeArchivedRunsWithItems(): Flow<List<RunWithItems>>

    @Transaction
    @Query("SELECT * FROM maintenance_runs WHERE id = :runId LIMIT 1")
    fun observeRunWithItemsById(runId: Long): Flow<RunWithItems?>

    @Transaction
    @Query("SELECT * FROM maintenance_runs WHERE id = :runId LIMIT 1")
    suspend fun getRunWithItemsOnce(runId: Long): RunWithItems?

    @Query(
        "SELECT * FROM maintenance_runs WHERE frequency = :frequency AND status = 'IN_PROGRESS' " +
            "ORDER BY createdAtMillis DESC LIMIT 1"
    )
    suspend fun getActiveRunOnce(frequency: String): MaintenanceRunEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: MaintenanceRunEntity): Long

    @Update
    suspend fun updateRun(run: MaintenanceRunEntity)

    @Query("DELETE FROM maintenance_runs WHERE id = :runId")
    suspend fun deleteRunById(runId: Long)

    @Query("SELECT * FROM maintenance_run_items WHERE runId = :runId ORDER BY sortOrder ASC, id ASC")
    suspend fun getRunItemsOnce(runId: Long): List<MaintenanceRunItemEntity>

    @Query("SELECT * FROM maintenance_run_items WHERE id = :itemId LIMIT 1")
    suspend fun getRunItemByIdOnce(itemId: Long): MaintenanceRunItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRunItem(item: MaintenanceRunItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRunItems(items: List<MaintenanceRunItemEntity>)

    @Update
    suspend fun updateRunItem(item: MaintenanceRunItemEntity)

    @Update
    suspend fun updateRunItems(items: List<MaintenanceRunItemEntity>)

    @Query("UPDATE maintenance_run_items SET isTicked = :isTicked WHERE id = :itemId")
    suspend fun updateItemTick(itemId: Long, isTicked: Boolean)

    @Query("UPDATE maintenance_run_items SET note = :note, photoPathsJson = :photoPathsJson WHERE id = :itemId")
    suspend fun updateItemDetails(itemId: Long, note: String, photoPathsJson: String)

    @Query("DELETE FROM maintenance_run_items WHERE runId = :runId")
    suspend fun deleteItemsByRunId(runId: Long)

    @Query("DELETE FROM maintenance_run_items WHERE runId = :runId AND templateId = :templateId")
    suspend fun deleteActiveRunItemsByTemplateId(runId: Long, templateId: Long)
}
