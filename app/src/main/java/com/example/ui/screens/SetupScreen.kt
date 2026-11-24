package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ChecklistFrequency
import com.example.data.ChecklistTemplateEntity
import com.example.data.DefaultSchoolTemplates
import com.example.ui.SetupUiState
import com.example.ui.components.FrequencySegmentedSelector
import com.example.ui.theme.LocalCustodianColors

@Composable
fun SetupScreen(
    uiState: SetupUiState,
    onSelectFrequency: (ChecklistFrequency) -> Unit,
    onSaveProfile: (String, String, String) -> Unit,
    onAddTask: (String, String) -> Unit,
    onUpdateTask: (ChecklistTemplateEntity, String, String) -> Unit,
    onMoveTask: (Long, Boolean) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onRestoreDefaults: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var schoolName by remember { mutableStateOf(uiState.profile.schoolName) }
    var custodianName by remember { mutableStateOf(uiState.profile.custodianName) }
    var campusLabel by remember { mutableStateOf(uiState.profile.campusLabel) }

    LaunchedEffect(uiState.profile) {
        schoolName = uiState.profile.schoolName
        custodianName = uiState.profile.custodianName
        campusLabel = uiState.profile.campusLabel
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<ChecklistTemplateEntity?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.setup_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // School & Custodian Profile Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, LocalCustodianColors.current.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.setup_profile_section),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = schoolName,
                            onValueChange = { schoolName = it },
                            label = { Text(stringResource(R.string.label_school_name)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_school_name_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = custodianName,
                                onValueChange = { custodianName = it },
                                label = { Text(stringResource(R.string.label_custodian_name)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("setup_custodian_name_input")
                            )
                            OutlinedTextField(
                                value = campusLabel,
                                onValueChange = { campusLabel = it },
                                label = { Text(stringResource(R.string.label_campus_name)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("setup_campus_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    onSaveProfile(schoolName, custodianName, campusLabel)
                                    onShowMessage("School & custodian profile saved")
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("setup_save_profile_button")
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_save_profile))
                            }
                        }
                    }
                }
            }

            // Tick List Templates Section Header & Selector
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.setup_templates_section),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                FrequencySegmentedSelector(
                    selected = uiState.frequency,
                    onSelect = onSelectFrequency,
                    testTagPrefix = "setup_freq"
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("setup_add_task_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_add_task), maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { showRestoreDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("setup_restore_defaults_button")
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_restore_defaults), maxLines = 1)
                    }
                }
            }

            if (uiState.templates.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, LocalCustodianColors.current.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "No ${uiState.frequency.displayName.lowercase()} tasks configured",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap Add Task or Restore Defaults to set up this tick list.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = uiState.templates,
                    key = { _, item -> item.id }
                ) { index, template ->
                    TemplateTaskRow(
                        template = template,
                        canMoveUp = index > 0,
                        canMoveDown = index < uiState.templates.lastIndex,
                        onMoveUp = { onMoveTask(template.id, true) },
                        onMoveDown = { onMoveTask(template.id, false) },
                        onEdit = { editingTemplate = template },
                        onDelete = {
                            onDeleteTask(template.id)
                            onShowMessage("Task removed from ${uiState.frequency.displayName} template")
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showAddDialog) {
        AddEditTaskDialog(
            initialZone = DefaultSchoolTemplates.commonZones.first(),
            initialTitle = "",
            isEdit = false,
            onDismiss = { showAddDialog = false },
            onSave = { zone, title ->
                onAddTask(zone, title)
                showAddDialog = false
                onShowMessage("Added task to ${uiState.frequency.displayName} list")
            }
        )
    }

    editingTemplate?.let { current ->
        AddEditTaskDialog(
            initialZone = current.zone,
            initialTitle = current.title,
            isEdit = true,
            onDismiss = { editingTemplate = null },
            onSave = { zone, title ->
                onUpdateTask(current, zone, title)
                editingTemplate = null
                onShowMessage("Updated task")
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text(stringResource(R.string.dialog_restore_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dialog_restore_body,
                        uiState.frequency.displayName
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDialog = false
                        onRestoreDefaults()
                        onShowMessage("Restored ${uiState.frequency.displayName} default tasks")
                    },
                    modifier = Modifier.testTag("confirm_restore_defaults_button")
                ) {
                    Text(stringResource(R.string.btn_restore_defaults))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun TemplateTaskRow(
    template: ChecklistTemplateEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, LocalCustodianColors.current.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("template_row_${template.id}")
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = template.zone,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = template.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 6.dp)
            )

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.testTag("template_move_up_${template.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = stringResource(R.string.cd_move_up)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.testTag("template_move_down_${template.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = stringResource(R.string.cd_move_down)
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("template_edit_${template.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.cd_edit_task)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("template_delete_${template.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_delete_task),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddEditTaskDialog(
    initialZone: String,
    initialTitle: String,
    isEdit: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var zone by remember { mutableStateOf(initialZone) }
    var title by remember { mutableStateOf(initialTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (isEdit) R.string.dialog_edit_task_title else R.string.dialog_add_task_title
                )
            )
        },
        text = {
            Column {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DefaultSchoolTemplates.commonZones.forEach { presetZone ->
                        FilterChip(
                            selected = zone.equals(presetZone, ignoreCase = true),
                            onClick = { zone = presetZone },
                            label = { Text(presetZone, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = zone,
                    onValueChange = { zone = it },
                    label = { Text(stringResource(R.string.label_task_zone)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_task_zone_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.label_task_title)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_task_title_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(zone, title) },
                enabled = title.trim().isNotEmpty(),
                modifier = Modifier.testTag("dialog_save_task_button")
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
