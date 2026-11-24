package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import org.json.JSONArray

@Entity(tableName = "school_profile")
data class SchoolProfileEntity(
    @PrimaryKey val id: Int = 1,
    val schoolName: String = "Oakridge Elementary School",
    val custodianName: String = "Head Custodian",
    val campusLabel: String = "Main Building"
)

@Entity(
    tableName = "checklist_templates",
    indices = [Index(value = ["frequency", "sortOrder"])]
)
data class ChecklistTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val frequency: String,
    val zone: String,
    val title: String,
    val sortOrder: Int
)

@Entity(
    tableName = "maintenance_runs",
    indices = [Index(value = ["frequency", "status"])]
)
data class MaintenanceRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val frequency: String,
    val status: String,
    val createdAtMillis: Long,
    val finalizedAtMillis: Long? = null,
    val finalizationNote: String = "",
    val schoolNameSnapshot: String = "",
    val custodianNameSnapshot: String = "",
    val campusSnapshot: String = ""
)

@Entity(
    tableName = "maintenance_run_items",
    foreignKeys = [
        ForeignKey(
            entity = MaintenanceRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["runId", "sortOrder"])]
)
data class MaintenanceRunItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,
    val templateId: Long?,
    val zone: String,
    val title: String,
    val sortOrder: Int,
    val isTicked: Boolean = false,
    val note: String = "",
    val photoPathsJson: String = "[]"
) {
    fun photoPaths(): List<String> = decodePhotoPaths(photoPathsJson)
}

data class RunWithItems(
    @Embedded val run: MaintenanceRunEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "runId"
    )
    val rawItems: List<MaintenanceRunItemEntity>
) {
    val items: List<MaintenanceRunItemEntity>
        get() = rawItems.sortedBy { it.sortOrder }

    val frequencyEnum: ChecklistFrequency
        get() = ChecklistFrequency.fromString(run.frequency)

    val statusEnum: RunStatus
        get() = RunStatus.fromString(run.status)

    val tickedCount: Int
        get() = rawItems.count { it.isTicked }

    val totalCount: Int
        get() = rawItems.size
}

fun encodePhotoPaths(paths: List<String>): String {
    val array = JSONArray()
    paths.forEach { array.put(it) }
    return array.toString()
}

fun decodePhotoPaths(json: String): List<String> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optString(i)
                if (item.isNotBlank()) add(item)
            }
        }
    }.getOrDefault(emptyList())
}
