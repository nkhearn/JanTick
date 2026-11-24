package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MaintenanceRunItemEntity
import com.example.data.RunWithItems
import com.example.pdf.PdfReportGenerator
import com.example.ui.components.FrequencyPill
import com.example.ui.components.PhotoPreviewDialog
import com.example.ui.components.PhotoThumbnailRow
import com.example.ui.components.StatusBadge
import com.example.ui.components.ZoneSectionHeader
import com.example.ui.theme.LocalCustodianColors
import com.example.ui.theme.MonoMetricStyle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    runWithItems: RunWithItems?,
    onBack: () -> Unit,
    onDelete: (Long) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var previewPhotoPath by remember { mutableStateOf<String?>(null) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val report = runWithItems ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            scope.launch {
                isExporting = true
                val ok = PdfReportGenerator.writePdfToUri(context, report, uri)
                isExporting = false
                onShowMessage(if (ok) "PDF report saved" else "Failed to save PDF")
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = runWithItems?.let {
                            "${it.frequencyEnum.displayName} Report #CC-${it.run.id.toString().padStart(4, '0')}"
                        } ?: "Report Detail",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("report_detail_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (runWithItems != null) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag("report_detail_delete")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = stringResource(R.string.cd_delete_report),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (runWithItems != null) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()
                        ) {
                            OutlinedButton(
                                enabled = !isExporting,
                                onClick = {
                                    savePdfLauncher.launch(PdfReportGenerator.suggestedFileName(runWithItems))
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_save_pdf")
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_save_pdf), maxLines = 1)
                            }

                            FilledTonalButton(
                                enabled = !isExporting,
                                onClick = {
                                    val activity = context.findActivity()
                                    if (activity != null) {
                                        PdfReportGenerator.printReport(activity, runWithItems)
                                    } else {
                                        onShowMessage("Unable to launch print dialog")
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_print_pdf")
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_print_pdf), maxLines = 1)
                            }

                            Button(
                                enabled = !isExporting,
                                onClick = {
                                    scope.launch {
                                        isExporting = true
                                        val file = PdfReportGenerator.generateTempShareFile(context, runWithItems)
                                        isExporting = false
                                        PdfReportGenerator.sharePdf(context, file, runWithItems)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("btn_share_pdf")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_share_pdf), maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (runWithItems == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                CircularProgressIndicator()
            }
        } else {
            val grouped = remember(runWithItems.items) {
                runWithItems.items.groupBy { it.zone }
            }
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)
                ) {
                    item {
                        ReportHeaderSummaryCard(runWithItems = runWithItems)
                    }

                    grouped.forEach { (zone, zoneItems) ->
                        item(key = "detail_zone_$zone") {
                            ZoneSectionHeader(
                                zone = zone,
                                tickedCount = zoneItems.count { it.isTicked },
                                totalCount = zoneItems.size
                            )
                        }
                        items(zoneItems, key = { it.id }) { item ->
                            ArchivedItemRow(
                                item = item,
                                onPreviewPhoto = { previewPhotoPath = it }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && runWithItems != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_report_title)) },
            text = { Text(stringResource(R.string.dialog_delete_report_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(runWithItems.run.id)
                    }
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
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
private fun ReportHeaderSummaryCard(runWithItems: RunWithItems) {
    val semantic = LocalCustodianColors.current
    val dateFormat = remember {
        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    }
    val finalizedText = dateFormat.format(
        Date(runWithItems.run.finalizedAtMillis ?: runWithItems.run.createdAtMillis)
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrequencyPill(frequency = runWithItems.frequencyEnum)
                StatusBadge(status = runWithItems.statusEnum)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = runWithItems.run.schoolNameSnapshot,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "${runWithItems.run.campusSnapshot} • Custodian: ${runWithItems.run.custodianNameSnapshot}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Finalized: $finalizedText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("VERIFIED", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${runWithItems.tickedCount} / ${runWithItems.totalCount}",
                            style = MonoMetricStyle,
                            color = semantic.verified
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("UNCHECKED", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${runWithItems.totalCount - runWithItems.tickedCount}",
                            style = MonoMetricStyle,
                            color = if (runWithItems.tickedCount < runWithItems.totalCount) {
                                semantic.warning
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }

            if (runWithItems.run.finalizationNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = semantic.warningContainer,
                    contentColor = semantic.onWarningContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Finalization Note: ${runWithItems.run.finalizationNote}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchivedItemRow(
    item: MaintenanceRunItemEntity,
    onPreviewPhoto: (String) -> Unit
) {
    val semantic = LocalCustodianColors.current
    val photos = remember(item.photoPathsJson) { item.photoPaths().filter { File(it).exists() } }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = if (item.isTicked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (item.isTicked) "Verified" else "Unchecked",
                    tint = if (item.isTicked) semantic.verified else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (item.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = semantic.warningContainer.copy(alpha = 0.65f),
                    contentColor = semantic.onWarningContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 32.dp)
                ) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (photos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                PhotoThumbnailRow(
                    paths = photos,
                    onClickPhoto = onPreviewPhoto,
                    modifier = Modifier.padding(start = 32.dp)
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
