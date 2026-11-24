package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MaintenanceRepository(
    private val schoolProfileDao: SchoolProfileDao,
    private val templateDao: ChecklistTemplateDao,
    private val runDao: MaintenanceRunDao,
    private val photoStorage: PhotoStorageManager
) {
    private val initMutex = Mutex()

    val schoolProfile: Flow<SchoolProfileEntity?> = schoolProfileDao.observeProfile()

    val archivedRuns: Flow<List<RunWithItems>> = runDao.observeArchivedRunsWithItems()

    fun observeActiveRun(frequency: ChecklistFrequency): Flow<RunWithItems?> {
        return runDao.observeActiveRunWithItems(frequency.name)
    }

    fun observeRunById(runId: Long): Flow<RunWithItems?> {
        return runDao.observeRunWithItemsById(runId)
    }

    fun observeTemplates(frequency: ChecklistFrequency): Flow<List<ChecklistTemplateEntity>> {
        return templateDao.observeTemplatesByFrequency(frequency.name)
    }

    suspend fun ensureInitialized() {
        initMutex.withLock {
            if (schoolProfileDao.getProfileOnce() == null) {
                schoolProfileDao.upsertProfile(SchoolProfileEntity())
            }

            if (templateDao.countTemplates() == 0) {
                ChecklistFrequency.entries.forEach { freq ->
                    val entities = DefaultSchoolTemplates.tasksFor(freq).mapIndexed { index, task ->
                        ChecklistTemplateEntity(
                            frequency = freq.name,
                            zone = task.zone,
                            title = task.title,
                            sortOrder = index
                        )
                    }
                    templateDao.insertTemplates(entities)
                }
            }

            ChecklistFrequency.entries.forEach { freq ->
                if (runDao.getActiveRunOnce(freq.name) == null) {
                    createFreshActiveRun(freq)
                }
            }
        }
    }

    private suspend fun createFreshActiveRun(frequency: ChecklistFrequency): Long {
        val profile = schoolProfileDao.getProfileOnce() ?: SchoolProfileEntity()
        val templates = templateDao.getTemplatesByFrequencyOnce(frequency.name)
        val runId = runDao.insertRun(
            MaintenanceRunEntity(
                frequency = frequency.name,
                status = RunStatus.IN_PROGRESS.name,
                createdAtMillis = System.currentTimeMillis(),
                schoolNameSnapshot = profile.schoolName,
                custodianNameSnapshot = profile.custodianName,
                campusSnapshot = profile.campusLabel
            )
        )
        val runItems = templates.mapIndexed { index, template ->
            MaintenanceRunItemEntity(
                runId = runId,
                templateId = template.id,
                zone = template.zone,
                title = template.title,
                sortOrder = index
            )
        }
        if (runItems.isNotEmpty()) {
            runDao.insertRunItems(runItems)
        }
        return runId
    }

    suspend fun toggleItemTick(itemId: Long, isTicked: Boolean) {
        runDao.updateItemTick(itemId, isTicked)
    }

    suspend fun updateItemDetails(itemId: Long, note: String, photoPaths: List<String>) {
        runDao.updateItemDetails(
            itemId = itemId,
            note = note.trim(),
            photoPathsJson = encodePhotoPaths(photoPaths)
        )
    }

    suspend fun finalizeRun(runId: Long, finalizationNote: String): Long? {
        val runWithItems = runDao.getRunWithItemsOnce(runId) ?: return null
        val profile = schoolProfileDao.getProfileOnce() ?: SchoolProfileEntity()
        val items = runWithItems.items
        val allTicked = items.isNotEmpty() && items.all { it.isTicked }
        val newStatus = if (allTicked) RunStatus.COMPLETED else RunStatus.FINALIZED_INCOMPLETE

        val updatedRun = runWithItems.run.copy(
            status = newStatus.name,
            finalizedAtMillis = System.currentTimeMillis(),
            finalizationNote = finalizationNote.trim(),
            schoolNameSnapshot = profile.schoolName,
            custodianNameSnapshot = profile.custodianName,
            campusSnapshot = profile.campusLabel
        )
        runDao.updateRun(updatedRun)

        val freq = ChecklistFrequency.fromString(runWithItems.run.frequency)
        createFreshActiveRun(freq)
        return runId
    }

    suspend fun updateSchoolProfile(
        schoolName: String,
        custodianName: String,
        campusLabel: String
    ) {
        schoolProfileDao.upsertProfile(
            SchoolProfileEntity(
                id = 1,
                schoolName = schoolName.trim().ifEmpty { "School Building" },
                custodianName = custodianName.trim().ifEmpty { "Custodian" },
                campusLabel = campusLabel.trim().ifEmpty { "Main Campus" }
            )
        )
    }

    suspend fun addTemplateItem(
        frequency: ChecklistFrequency,
        zone: String,
        title: String
    ) {
        val cleanTitle = title.trim()
        val cleanZone = zone.trim().ifEmpty { "General" }
        if (cleanTitle.isEmpty()) return

        val existing = templateDao.getTemplatesByFrequencyOnce(frequency.name)
        val nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val newTemplateId = templateDao.insertTemplate(
            ChecklistTemplateEntity(
                frequency = frequency.name,
                zone = cleanZone,
                title = cleanTitle,
                sortOrder = nextOrder
            )
        )

        val activeRun = runDao.getActiveRunOnce(frequency.name)
        if (activeRun != null) {
            val activeItems = runDao.getRunItemsOnce(activeRun.id)
            val nextRunOrder = (activeItems.maxOfOrNull { it.sortOrder } ?: -1) + 1
            runDao.insertRunItem(
                MaintenanceRunItemEntity(
                    runId = activeRun.id,
                    templateId = newTemplateId,
                    zone = cleanZone,
                    title = cleanTitle,
                    sortOrder = nextRunOrder
                )
            )
        }
    }

    suspend fun updateTemplateItem(template: ChecklistTemplateEntity) {
        val cleanTitle = template.title.trim()
        val cleanZone = template.zone.trim().ifEmpty { "General" }
        if (cleanTitle.isEmpty()) return

        val updated = template.copy(title = cleanTitle, zone = cleanZone)
        templateDao.updateTemplate(updated)

        val activeRun = runDao.getActiveRunOnce(updated.frequency)
        if (activeRun != null) {
            val matching = runDao.getRunItemsOnce(activeRun.id).filter { it.templateId == updated.id }
            matching.forEach { item ->
                runDao.updateRunItem(item.copy(zone = cleanZone, title = cleanTitle))
            }
        }
    }

    suspend fun moveTemplateItem(
        frequency: ChecklistFrequency,
        templateId: Long,
        moveUp: Boolean
    ) {
        val list = templateDao.getTemplatesByFrequencyOnce(frequency.name).toMutableList()
        val index = list.indexOfFirst { it.id == templateId }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in list.indices) return

        val item = list.removeAt(index)
        list.add(targetIndex, item)
        val reindexed = list.mapIndexed { idx, t -> t.copy(sortOrder = idx) }
        templateDao.updateTemplates(reindexed)

        val orderByTemplateId = reindexed.associate { it.id to it.sortOrder }
        val activeRun = runDao.getActiveRunOnce(frequency.name)
        if (activeRun != null) {
            val activeItems = runDao.getRunItemsOnce(activeRun.id)
            val updatedItems = activeItems.mapIndexed { idx, runItem ->
                val mappedOrder = runItem.templateId?.let { orderByTemplateId[it] } ?: (reindexed.size + idx)
                runItem.copy(sortOrder = mappedOrder)
            }
            runDao.updateRunItems(updatedItems)
        }
    }

    suspend fun deleteTemplateItem(frequency: ChecklistFrequency, templateId: Long) {
        templateDao.deleteTemplateById(templateId)
        val activeRun = runDao.getActiveRunOnce(frequency.name)
        if (activeRun != null) {
            runDao.deleteActiveRunItemsByTemplateId(activeRun.id, templateId)
        }
    }

    suspend fun resetFrequencyToDefaults(frequency: ChecklistFrequency) {
        templateDao.deleteTemplatesByFrequency(frequency.name)
        val defaults = DefaultSchoolTemplates.tasksFor(frequency).mapIndexed { idx, task ->
            ChecklistTemplateEntity(
                frequency = frequency.name,
                zone = task.zone,
                title = task.title,
                sortOrder = idx
            )
        }
        val insertedIds = templateDao.insertTemplates(defaults)

        val activeRun = runDao.getActiveRunOnce(frequency.name)
        if (activeRun != null) {
            runDao.deleteItemsByRunId(activeRun.id)
            val freshItems = defaults.mapIndexed { idx, tmpl ->
                MaintenanceRunItemEntity(
                    runId = activeRun.id,
                    templateId = insertedIds.getOrNull(idx),
                    zone = tmpl.zone,
                    title = tmpl.title,
                    sortOrder = idx
                )
            }
            runDao.insertRunItems(freshItems)
        }
    }

    suspend fun deleteArchivedRun(runId: Long) {
        val runWithItems = runDao.getRunWithItemsOnce(runId)
        runWithItems?.items?.forEach { item ->
            item.photoPaths().forEach { path ->
                photoStorage.deletePhotoFile(path)
            }
        }
        runDao.deleteRunById(runId)
    }
}
