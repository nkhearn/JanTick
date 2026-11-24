package com.example.pdf

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.MaintenanceRunItemEntity
import com.example.data.RunStatus
import com.example.data.RunWithItems
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PdfReportGenerator {

    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792
    private const val MARGIN = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)
    private const val FOOTER_HEIGHT = 36f
    private const val MAX_Y = PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT

    private val dateTimeFormatter = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    private val fileDateFormatter = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())

    fun suggestedFileName(runWithItems: RunWithItems): String {
        val freq = runWithItems.frequencyEnum.displayName.lowercase(Locale.getDefault())
        val stamp = fileDateFormatter.format(
            Date(runWithItems.run.finalizedAtMillis ?: runWithItems.run.createdAtMillis)
        )
        return "maintenance_${freq}_${stamp}.pdf"
    }

    suspend fun generateTempShareFile(context: Context, runWithItems: RunWithItems): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
            val file = File(dir, suggestedFileName(runWithItems))
            FileOutputStream(file).use { out ->
                buildPdfDocument(runWithItems, out)
            }
            file
        }

    suspend fun writePdfToUri(
        context: Context,
        runWithItems: RunWithItems,
        destinationUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                buildPdfDocument(runWithItems, out)
            } != null
        }.getOrDefault(false)
    }

    fun sharePdf(context: Context, pdfFile: File, runWithItems: RunWithItems) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val subject = "${runWithItems.run.schoolNameSnapshot} - ${runWithItems.frequencyEnum.displayName} Maintenance Report"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Maintenance Report PDF"))
    }

    fun printReport(activity: Activity, runWithItems: RunWithItems) {
        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "${runWithItems.run.schoolNameSnapshot} ${runWithItems.frequencyEnum.displayName} Report"
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(suggestedFileName(runWithItems))
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        buildPdfDocument(runWithItems, out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    private fun buildPdfDocument(runWithItems: RunWithItems, outputStream: OutputStream) {
        val document = PdfDocument()
        try {
        val run = runWithItems.run
        val items = runWithItems.items

        val navyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 58, 95) }
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(244, 246, 249) }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val emeraldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(21, 128, 61) }
        val amberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(180, 83, 9) }
        val zoneBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(226, 232, 240) }
        val noteBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(254, 249, 235) }

        val headerTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(214, 227, 245)
            textSize = 10f
        }
        val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val zoneTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 95)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val itemTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 24, 39)
            textSize = 10.5f
        }
        val itemNotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(120, 53, 15)
            textSize = 9.5f
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8.5f
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN

        fun drawFooter(currentCanvas: Canvas, pNum: Int) {
            currentCanvas.drawLine(MARGIN, PAGE_HEIGHT - MARGIN - 20f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - MARGIN - 20f, borderPaint)
            currentCanvas.drawText(
                "Custodian Check • School Maintenance Record #CC-${run.id.toString().padStart(4, '0')}",
                MARGIN,
                PAGE_HEIGHT - MARGIN - 6f,
                footerPaint
            )
            val pageText = "Page $pNum"
            val w = footerPaint.measureText(pageText)
            currentCanvas.drawText(pageText, PAGE_WIDTH - MARGIN - w, PAGE_HEIGHT - MARGIN - 6f, footerPaint)
        }

        fun startNextPage(continuationZone: String? = null) {
            drawFooter(canvas, pageNumber)
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
            canvas.drawText(
                "${run.schoolNameSnapshot} — ${runWithItems.frequencyEnum.displayName} Maintenance Report (cont.)",
                MARGIN,
                y + 10f,
                labelPaint
            )
            y += 22f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, borderPaint)
            y += 12f
            if (!continuationZone.isNullOrBlank()) {
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, zoneBgPaint)
                canvas.drawText("$continuationZone (cont.)", MARGIN + 8f, y + 13.5f, zoneTitlePaint)
                y += 26f
            }
        }

        fun ensureSpace(neededHeight: Float, continuationZone: String? = null) {
            if (y + neededHeight > MAX_Y) {
                startNextPage(continuationZone)
            }
        }

        // 1. Header Banner
        val bannerRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 66f)
        canvas.drawRoundRect(bannerRect, 8f, 8f, navyPaint)
        canvas.drawText("SCHOOL BUILDING MAINTENANCE REPORT", MARGIN + 14f, y + 26f, headerTitlePaint)
        canvas.drawText(
            "${runWithItems.frequencyEnum.displayName.uppercase(Locale.getDefault())} INSPECTION  •  RECORD #CC-${run.id.toString().padStart(4, '0')}",
            MARGIN + 14f,
            y + 46f,
            headerSubPaint
        )

        val statusLabel = when (runWithItems.statusEnum) {
            RunStatus.COMPLETED -> "COMPLETED"
            RunStatus.FINALIZED_INCOMPLETE -> "FINALIZED INCOMPLETE"
            RunStatus.IN_PROGRESS -> "IN PROGRESS"
        }
        val statusPaint = when (runWithItems.statusEnum) {
            RunStatus.COMPLETED -> emeraldPaint
            RunStatus.FINALIZED_INCOMPLETE -> amberPaint
            RunStatus.IN_PROGRESS -> navyPaint
        }
        val badgeW = badgeTextPaint.measureText(statusLabel) + 20f
        val badgeRect = RectF(PAGE_WIDTH - MARGIN - 14f - badgeW, y + 18f, PAGE_WIDTH - MARGIN - 14f, y + 40f)
        canvas.drawRoundRect(badgeRect, 11f, 11f, statusPaint)
        canvas.drawText(statusLabel, badgeRect.left + 10f, badgeRect.top + 14.5f, badgeTextPaint)
        y += 78f

        // 2. School Metadata Grid
        val infoRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 76f)
        canvas.drawRoundRect(infoRect, 6f, 6f, cardBgPaint)
        canvas.drawRoundRect(infoRect, 6f, 6f, borderPaint)

        val col2X = MARGIN + (CONTENT_WIDTH / 2f) + 8f
        canvas.drawText("SCHOOL", MARGIN + 12f, y + 16f, labelPaint)
        canvas.drawText(run.schoolNameSnapshot.ifBlank { "School Building" }, MARGIN + 12f, y + 31f, valuePaint)

        canvas.drawText("BUILDING / CAMPUS", col2X, y + 16f, labelPaint)
        canvas.drawText(run.campusSnapshot.ifBlank { "Main Building" }, col2X, y + 31f, valuePaint)

        canvas.drawText("CUSTODIAN", MARGIN + 12f, y + 50f, labelPaint)
        canvas.drawText(run.custodianNameSnapshot.ifBlank { "Custodian" }, MARGIN + 12f, y + 65f, valuePaint)

        val dateLabel = if (run.finalizedAtMillis != null) "FINALIZED" else "STARTED"
        val dateText = dateTimeFormatter.format(Date(run.finalizedAtMillis ?: run.createdAtMillis))
        canvas.drawText(dateLabel, col2X, y + 50f, labelPaint)
        canvas.drawText(dateText, col2X, y + 65f, valuePaint)
        y += 88f

        // 3. Summary Strip
        val total = items.size
        val ticked = items.count { it.isTicked }
        val unchecked = total - ticked
        val pct = if (total > 0) (ticked * 100) / total else 0

        val summaryRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 46f)
        canvas.drawRoundRect(summaryRect, 6f, 6f, cardBgPaint)
        canvas.drawRoundRect(summaryRect, 6f, 6f, borderPaint)

        val third = CONTENT_WIDTH / 3f
        canvas.drawText("VERIFIED", MARGIN + 12f, y + 16f, labelPaint)
        canvas.drawText("$ticked of $total ($pct%)", MARGIN + 12f, y + 34f, valuePaint)

        canvas.drawText("UNCHECKED", MARGIN + third + 12f, y + 16f, labelPaint)
        canvas.drawText("$unchecked items", MARGIN + third + 12f, y + 34f, valuePaint)

        val notesCount = items.count { it.note.isNotBlank() }
        val photosCount = items.sumOf { it.photoPaths().size }
        canvas.drawText("DOCUMENTATION", MARGIN + (third * 2f) + 12f, y + 16f, labelPaint)
        canvas.drawText("$notesCount notes • $photosCount photos", MARGIN + (third * 2f) + 12f, y + 34f, valuePaint)
        y += 56f

        if (run.finalizationNote.isNotBlank()) {
            val noteLayout = createStaticLayout(
                "Finalization Note: ${run.finalizationNote}",
                itemNotePaint,
                (CONTENT_WIDTH - 20f).toInt()
            )
            val boxH = noteLayout.height + 16f
            ensureSpace(boxH + 8f)
            val fRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + boxH)
            canvas.drawRoundRect(fRect, 6f, 6f, noteBgPaint)
            canvas.drawRoundRect(fRect, 6f, 6f, borderPaint)
            canvas.save()
            canvas.translate(MARGIN + 10f, y + 8f)
            noteLayout.draw(canvas)
            canvas.restore()
            y += boxH + 12f
        }

        // 4. Checklist Items Grouped by Zone
        val grouped = items.groupBy { it.zone }
        grouped.forEach { (zone, zoneItems) ->
            ensureSpace(48f)
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 22f, zoneBgPaint)
            canvas.drawText(zone.uppercase(Locale.getDefault()), MARGIN + 8f, y + 14.5f, zoneTitlePaint)
            val zoneProgress = "${zoneItems.count { it.isTicked }}/${zoneItems.size}"
            val zpW = labelPaint.measureText(zoneProgress)
            canvas.drawText(zoneProgress, PAGE_WIDTH - MARGIN - 8f - zpW, y + 14.5f, labelPaint)
            y += 28f

            zoneItems.forEach { item ->
                drawChecklistItem(
                    canvasProvider = { canvas },
                    ensureSpace = { needed -> ensureSpace(needed, zone) },
                    getY = { y },
                    setY = { newY -> y = newY },
                    item = item,
                    itemTitlePaint = itemTitlePaint,
                    itemNotePaint = itemNotePaint,
                    emeraldPaint = emeraldPaint,
                    borderPaint = borderPaint,
                    noteBgPaint = noteBgPaint
                )
            }
            y += 6f
        }

        // 5. Sign-Off Block
        ensureSpace(78f)
        y += 12f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, borderPaint)
        y += 16f
        canvas.drawText("SIGN-OFF & VERIFICATION", MARGIN, y, labelPaint)
        y += 28f

        val sigWidth = (CONTENT_WIDTH - 32f) / 2f
        canvas.drawLine(MARGIN, y, MARGIN + sigWidth, y, borderPaint)
        canvas.drawLine(MARGIN + sigWidth + 32f, y, PAGE_WIDTH - MARGIN, y, borderPaint)
        y += 12f
        canvas.drawText("Custodian Signature & Date", MARGIN, y, footerPaint)
        canvas.drawText("Principal / Facilities Review & Date", MARGIN + sigWidth + 32f, y, footerPaint)

        drawFooter(canvas, pageNumber)
        document.finishPage(page)
        document.writeTo(outputStream)
        } finally {
            runCatching { document.close() }
        }
    }

    private inline fun drawChecklistItem(
        canvasProvider: () -> Canvas,
        ensureSpace: (Float) -> Unit,
        getY: () -> Float,
        setY: (Float) -> Unit,
        item: MaintenanceRunItemEntity,
        itemTitlePaint: TextPaint,
        itemNotePaint: TextPaint,
        emeraldPaint: Paint,
        borderPaint: Paint,
        noteBgPaint: Paint
    ) {
        val textWidth = (CONTENT_WIDTH - 34f).toInt()
        val titleLayout = createStaticLayout(item.title, itemTitlePaint, textWidth)
        val noteLayout = if (item.note.isNotBlank()) {
            createStaticLayout("Note: ${item.note}", itemNotePaint, (CONTENT_WIDTH - 46f).toInt())
        } else {
            null
        }
        val photos = item.photoPaths().filter { File(it).exists() }.take(3)
        val photoRowHeight = if (photos.isNotEmpty()) 68f else 0f
        val noteHeight = noteLayout?.let { it.height + 12f } ?: 0f
        val rowHeight = maxOf(20f, titleLayout.height.toFloat()) + noteHeight + photoRowHeight + 10f

        ensureSpace(rowHeight)
        val currentCanvas = canvasProvider()
        var cy = getY()

        // Checkbox symbol
        val boxRect = RectF(MARGIN + 4f, cy + 2f, MARGIN + 16f, cy + 14f)
        if (item.isTicked) {
            currentCanvas.drawRoundRect(boxRect, 2f, 2f, emeraldPaint)
            val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                strokeWidth = 1.8f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            currentCanvas.drawLine(boxRect.left + 2.5f, boxRect.centerY(), boxRect.centerX() - 0.5f, boxRect.bottom - 3f, checkPaint)
            currentCanvas.drawLine(boxRect.centerX() - 0.5f, boxRect.bottom - 3f, boxRect.right - 2.5f, boxRect.top + 3f, checkPaint)
        } else {
            currentCanvas.drawRoundRect(boxRect, 2f, 2f, borderPaint)
        }

        currentCanvas.save()
        currentCanvas.translate(MARGIN + 26f, cy)
        titleLayout.draw(currentCanvas)
        currentCanvas.restore()
        cy += maxOf(18f, titleLayout.height.toFloat()) + 4f

        if (noteLayout != null) {
            val nRect = RectF(MARGIN + 26f, cy, PAGE_WIDTH - MARGIN, cy + noteLayout.height + 8f)
            currentCanvas.drawRoundRect(nRect, 4f, 4f, noteBgPaint)
            currentCanvas.save()
            currentCanvas.translate(MARGIN + 32f, cy + 4f)
            noteLayout.draw(currentCanvas)
            currentCanvas.restore()
            cy += noteLayout.height + 12f
        }

        if (photos.isNotEmpty()) {
            var px = MARGIN + 26f
            photos.forEach { path ->
                val bmp = decodeThumbnail(path, 160, 120)
                if (bmp != null) {
                    val dest = RectF(px, cy, px + 76f, cy + 58f)
                    val src = Rect(0, 0, bmp.width, bmp.height)
                    currentCanvas.drawBitmap(bmp, src, dest, null)
                    currentCanvas.drawRect(dest, borderPaint)
                    bmp.recycle()
                    px += 84f
                }
            }
            cy += 64f
        }

        currentCanvas.drawLine(MARGIN + 26f, cy + 2f, PAGE_WIDTH - MARGIN, cy + 2f, borderPaint)
        setY(cy + 8f)
    }

    private fun createStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .build()
    }

    private fun decodeThumbnail(path: String, reqW: Int, reqH: Int): Bitmap? {
        return runCatching {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, opts)
            var sample = 1
            while (opts.outWidth / sample > reqW * 2 && opts.outHeight / sample > reqH * 2) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, decodeOpts)
        }.getOrNull()
    }
}
