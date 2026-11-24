package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ChecklistFrequency
import com.example.data.RunStatus
import com.example.ui.theme.LocalCustodianColors
import com.example.ui.theme.MonoMetricStyle
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencySegmentedSelector(
    selected: ChecklistFrequency,
    onSelect: (ChecklistFrequency) -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    val options = ChecklistFrequency.entries
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, frequency ->
            val label = when (frequency) {
                ChecklistFrequency.DAILY -> stringResource(R.string.freq_daily)
                ChecklistFrequency.WEEKLY -> stringResource(R.string.freq_weekly)
                ChecklistFrequency.MONTHLY -> stringResource(R.string.freq_monthly)
            }
            SegmentedButton(
                selected = selected == frequency,
                onClick = { onSelect(frequency) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("${testTagPrefix}_${frequency.name.lowercase()}")
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: RunStatus,
    modifier: Modifier = Modifier
) {
    val semantic = LocalCustodianColors.current
    val (bg, fg, icon, text) = when (status) {
        RunStatus.COMPLETED -> Quad(
            semantic.verifiedContainer,
            semantic.onVerifiedContainer,
            Icons.Filled.CheckCircle,
            stringResource(R.string.status_completed)
        )
        RunStatus.FINALIZED_INCOMPLETE -> Quad(
            semantic.warningContainer,
            semantic.onWarningContainer,
            Icons.Filled.WarningAmber,
            stringResource(R.string.status_finalized_incomplete)
        )
        RunStatus.IN_PROGRESS -> Quad(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Filled.Schedule,
            "In Progress"
        )
    }

    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FrequencyPill(
    frequency: ChecklistFrequency,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = frequency.displayName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ZoneSectionHeader(
    zone: String,
    tickedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val semantic = LocalCustodianColors.current
    val allDone = totalCount > 0 && tickedCount == totalCount

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(
            text = zone.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            color = if (allDone) semantic.verifiedContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (allDone) semantic.onVerifiedContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = "$tickedCount/$totalCount",
                style = MonoMetricStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun PhotoThumbnailRow(
    paths: List<String>,
    onRemove: ((String) -> Unit)? = null,
    onClickPhoto: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val existing = paths.filter { File(it).exists() }
    if (existing.isEmpty()) return

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier = modifier
    ) {
        items(existing, key = { it }) { path ->
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        BorderStroke(1.dp, LocalCustodianColors.current.cardBorder),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = onClickPhoto != null) {
                        onClickPhoto?.invoke(path)
                    }
            ) {
                AsyncImage(
                    model = File(path),
                    contentDescription = stringResource(R.string.cd_attached_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (onRemove != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f),
                        contentColor = MaterialTheme.colorScheme.surface,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(22.dp)
                            .clickable { onRemove(path) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cd_remove_photo),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoPreviewDialog(
    photoPath: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f))
                .clickable(onClick = onDismiss)
                .padding(16.dp)
        ) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = stringResource(R.string.cd_attached_photo),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp)
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
