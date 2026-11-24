package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ChecklistFrequency
import com.example.data.MaintenanceRunItemEntity
import com.example.ui.ChecklistUiState
import com.example.ui.components.FrequencySegmentedSelector
import com.example.ui.components.PhotoPreviewDialog
import com.example.ui.components.PhotoThumbnailRow
import com.example.ui.components.ZoneSectionHeader
import com.example.ui.theme.LocalCustodianColors
import com.example.ui.theme.MonoMetricStyle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChecklistsScreen(
    uiState: ChecklistUiState,
    onSelectFrequency: (ChecklistFrequency) -> Unit,
    onToggleTick: (MaintenanceRunItemEntity) -> Unit,
    onSaveItemDetails: (Long, String, List<String>) -> Unit,
    onCreateCameraTarget: () -> Pair<File, Uri>,
    onImportPickedPhoto: (Uri, (String?) -> Unit) -> Unit,
    onImportCapturedPhoto: (File, (String?) -> Unit) -> Unit,
    onDeletePhotoFile: (String) -> Unit,
    onFinalizeRun: (String, (Long) -> Unit) -> Unit,
    onReportSaved: (Long) -> Unit,
    onNavigateToSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingItem by remember { mutableStateOf<MaintenanceRunItemEntity?>(null) }
    var showFinalizeDialog by remember { mutableStateOf(false) }
    var previewPhotoPath by remember { mutableStateOf<String?>(null) }

    val todayText = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Top School & Date Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.profile.schoolName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${uiState.profile.campusLabel} • $todayText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Daily / Weekly / Monthly Switcher
            FrequencySegmentedSelector(
                selected = uiState.frequency,
                onSelect = onSelectFrequency,
                testTagPrefix = "checklist_freq"
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Progress Summary & Finalize Bar
                ProgressSummaryCard(
                    uiState = uiState,
                    onFinalizeClick = { showFinalizeDialog = true }
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.items.isEmpty()) {
                    EmptyChecklistCard(
                        frequency = uiState.frequency,
                        onNavigateToSetup = onNavigateToSetup
                    )
                } else {
                    val groupedItems = remember(uiState.items) {
                        uiState.items.groupBy { it.zone }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        groupedItems.forEach { (zone, zoneItems) ->
                            item(key = "header_${uiState.frequency.name}_$zone") {
                                ZoneSectionHeader(
                                    zone = zone,
                                    tickedCount = zoneItems.count { it.isTicked },
                                    totalCount = zoneItems.size
                                )
                            }
                            items(
                                items = zoneItems,
                                key = { it.id }
                            ) { item ->
                                ChecklistTaskCard(
                                    item = item,
                                    onToggleTick = { onToggleTick(item) },
                                    onOpenDetails = { editingItem = item },
                                    onPreviewPhoto = { previewPhotoPath = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingItem?.let { currentItem ->
        ItemEvidenceBottomSheet(
            item = currentItem,
            onDismiss = { editingItem = null },
            onSave = { note, photos ->
                onSaveItemDetails(currentItem.id, note, photos)
                editingItem = null
            },
            onCreateCameraTarget = onCreateCameraTarget,
            onImportPickedPhoto = onImportPickedPhoto,
            onImportCapturedPhoto = onImportCapturedPhoto,
            onDeletePhotoFile = onDeletePhotoFile,
            onPreviewPhoto = { previewPhotoPath = it }
        )
    }

    if (showFinalizeDialog) {
        FinalizeChecklistDialog(
            uiState = uiState,
            onDismiss = { showFinalizeDialog = false },
            onConfirm = { finalNote ->
                showFinalizeDialog = false
                onFinalizeRun(finalNote) { savedRunId ->
                    onReportSaved(savedRunId)
                }
            }
        )
    }

    previewPhotoPath?.let { path ->
        PhotoPreviewDialog(
            photoPath = path,
            onDismiss = { previewPhotoPath = null }
        )
    }
}

@Composable
private fun ProgressSummaryCard(
    uiState: ChecklistUiState,
    onFinalizeClick: () -> Unit
) {
    val semantic = LocalCustodianColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = spring(),
        label = "checklist_progress"
    )
    val accentColor by animateColorAsState(
        targetValue = if (uiState.allTicked) semantic.verified else MaterialTheme.colorScheme.primary,
        label = "progress_accent"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${uiState.frequency.displayName} ${stringResource(R.string.checklist_progress_label)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.tickedCount} / ${uiState.totalCount} completed",
                        style = MonoMetricStyle,
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onFinalizeClick,
                    enabled = uiState.totalCount > 0 && !uiState.isFinalizing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.allTicked) semantic.verified else MaterialTheme.colorScheme.primary,
                        contentColor = if (uiState.allTicked) semantic.onVerified else MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("finalize_checklist_button")
                ) {
                    Icon(
                        imageVector = if (uiState.allTicked) Icons.Filled.DoneAll else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.allTicked) {
                            stringResource(R.string.btn_complete_save)
                        } else {
                            stringResource(R.string.btn_finalize_list)
                        },
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun ChecklistTaskCard(
    item: MaintenanceRunItemEntity,
    onToggleTick: () -> Unit,
    onOpenDetails: () -> Unit,
    onPreviewPhoto: (String) -> Unit
) {
    val semantic = LocalCustodianColors.current
    val photos = remember(item.photoPathsJson) { item.photoPaths().filter { File(it).exists() } }
    val hasNote = item.note.isNotBlank()

    val containerColor by animateColorAsState(
        targetValue = if (item.isTicked) {
            semantic.verifiedContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "task_card_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (item.isTicked) semantic.verified.copy(alpha = 0.45f) else semantic.cardBorder,
        label = "task_card_border"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${item.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleTick)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("task_toggle_${item.id}")
            ) {
                // Custom high-contrast checkbox with 48dp touch zone feel
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (item.isTicked) semantic.verified else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (item.isTicked) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (item.isTicked) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Checked",
                                tint = semantic.onVerified,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.isTicked) FontWeight.Normal else FontWeight.Medium,
                    color = if (item.isTicked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (item.isTicked) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = onOpenDetails,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("task_details_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = if (hasNote || photos.isNotEmpty()) {
                            Icons.Filled.EditNote
                        } else {
                            Icons.AutoMirrored.Filled.NoteAdd
                        },
                        contentDescription = stringResource(R.string.btn_add_note_photo),
                        modifier = Modifier.size(18.dp)
                    )
                    if (photos.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${photos.size}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            AnimatedVisibility(visible = hasNote || photos.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 54.dp, end = 12.dp, bottom = 10.dp)
                ) {
                    if (hasNote) {
                        Surface(
                            color = semantic.warningContainer.copy(alpha = 0.65f),
                            contentColor = semantic.onWarningContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.note,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    if (photos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        PhotoThumbnailRow(
                            paths = photos,
                            onClickPhoto = onPreviewPhoto
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChecklistCard(
    frequency: ChecklistFrequency,
    onNavigateToSetup: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, LocalCustodianColors.current.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.empty_checklist_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${frequency.displayName}: ${stringResource(R.string.empty_checklist_body)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onNavigateToSetup,
                modifier = Modifier.testTag("empty_open_setup_button")
            ) {
                Text(stringResource(R.string.action_go_to_setup))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemEvidenceBottomSheet(
    item: MaintenanceRunItemEntity,
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit,
    onCreateCameraTarget: () -> Pair<File, Uri>,
    onImportPickedPhoto: (Uri, (String?) -> Unit) -> Unit,
    onImportCapturedPhoto: (File, (String?) -> Unit) -> Unit,
    onDeletePhotoFile: (String) -> Unit,
    onPreviewPhoto: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var noteText by remember(item.id) { mutableStateOf(item.note) }
    val photoPaths = remember(item.id) {
        mutableStateListOf<String>().apply { addAll(item.photoPaths()) }
    }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImportPickedPhoto(uri) { savedPath ->
                if (savedPath != null) {
                    photoPaths.add(savedPath)
                }
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (success && file != null) {
            onImportCapturedPhoto(file) { savedPath ->
                if (savedPath != null) {
                    photoPaths.add(savedPath)
                }
            }
        } else {
            file?.delete()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = stringResource(R.string.sheet_item_details_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.zone} • ${item.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(stringResource(R.string.label_maintenance_note)) },
                placeholder = { Text(stringResource(R.string.hint_maintenance_note)) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("item_note_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        val (file, uri) = onCreateCameraTarget()
                        pendingCameraFile = file
                        takePictureLauncher.launch(uri)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("btn_take_photo")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_take_photo), maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("btn_pick_photo")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_pick_photo), maxLines = 1)
                }
            }

            if (photoPaths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                PhotoThumbnailRow(
                    paths = photoPaths,
                    onRemove = { removedPath ->
                        photoPaths.remove(removedPath)
                        if (!item.photoPaths().contains(removedPath)) {
                            onDeletePhotoFile(removedPath)
                        }
                    },
                    onClickPhoto = onPreviewPhoto
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        // Clean up photos removed from the saved list
                        val removedOld = item.photoPaths().filterNot { photoPaths.contains(it) }
                        removedOld.forEach { onDeletePhotoFile(it) }
                        onSave(noteText, photoPaths.toList())
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("save_item_details_button")
                ) {
                    Text(stringResource(R.string.btn_save_details))
                }
            }
        }
    }
}

@Composable
private fun FinalizeChecklistDialog(
    uiState: ChecklistUiState,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    val semantic = LocalCustodianColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (uiState.allTicked) Icons.Filled.DoneAll else Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = if (uiState.allTicked) semantic.verified else semantic.warning
            )
        },
        title = {
            Text(
                text = if (uiState.allTicked) {
                    stringResource(R.string.dialog_finalize_complete_title)
                } else {
                    stringResource(R.string.dialog_finalize_incomplete_title)
                }
            )
        },
        text = {
            Column {
                Text(
                    text = if (uiState.allTicked) {
                        "All ${uiState.totalCount} ${uiState.frequency.displayName.lowercase()} items are ticked. Save this inspection locally and reset the checklist for the next cycle?"
                    } else {
                        "${uiState.tickedCount} of ${uiState.totalCount} items are ticked (${uiState.uncheckedCount} unchecked). You can record a reason before finalizing and saving locally."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.label_finalization_note)) },
                    placeholder = { Text(stringResource(R.string.hint_finalization_note)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("finalization_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(note) },
                modifier = Modifier.testTag("confirm_finalize_button")
            ) {
                Text(stringResource(R.string.btn_confirm_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_finalize_button")
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
