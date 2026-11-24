package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.ChecklistFrequency
import com.example.data.MaintenanceRepository
import com.example.data.PhotoStorageManager
import com.example.data.RunStatus
import com.example.pdf.PdfReportGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: MaintenanceRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MaintenanceRepository(
            schoolProfileDao = db.schoolProfileDao(),
            templateDao = db.checklistTemplateDao(),
            runDao = db.maintenanceRunDao(),
            photoStorage = PhotoStorageManager(context)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read app name from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Custodian Check", appName)
    }

    @Test
    fun `seeds default daily weekly and monthly checklists`() = runTest {
        repository.ensureInitialized()

        val dailyRun = repository.observeActiveRun(ChecklistFrequency.DAILY).first()
        val weeklyRun = repository.observeActiveRun(ChecklistFrequency.WEEKLY).first()
        val monthlyRun = repository.observeActiveRun(ChecklistFrequency.MONTHLY).first()

        assertNotNull(dailyRun)
        assertNotNull(weeklyRun)
        assertNotNull(monthlyRun)
        assertTrue(dailyRun!!.items.size >= 10)
        assertTrue(weeklyRun!!.items.isNotEmpty())
        assertTrue(monthlyRun!!.items.isNotEmpty())
    }

    @Test
    fun `finalize incomplete list saves archived report and spawns fresh cycle`() = runTest {
        repository.ensureInitialized()
        val activeBefore = repository.observeActiveRun(ChecklistFrequency.DAILY).first()!!
        val firstItem = activeBefore.items.first()

        repository.toggleItemTick(firstItem.id, true)
        repository.updateItemDetails(firstItem.id, "Hallway mopped", emptyList())

        val finalizedId = repository.finalizeRun(
            runId = activeBefore.run.id,
            finalizationNote = "Gym closed for assembly"
        )
        assertNotNull(finalizedId)

        val archived = repository.observeRunById(finalizedId!!).first()!!
        assertEquals(RunStatus.FINALIZED_INCOMPLETE, archived.statusEnum)
        assertEquals("Gym closed for assembly", archived.run.finalizationNote)
        assertEquals(1, archived.tickedCount)
        assertEquals("Hallway mopped", archived.items.first().note)

        val freshActive = repository.observeActiveRun(ChecklistFrequency.DAILY).first()!!
        assertTrue(freshActive.run.id != activeBefore.run.id)
        assertEquals(0, freshActive.tickedCount)
    }

    @Test
    fun `finalize complete list marks status completed and generates valid pdf`() = runTest {
        repository.ensureInitialized()
        val active = repository.observeActiveRun(ChecklistFrequency.WEEKLY).first()!!
        active.items.forEach { item ->
            repository.toggleItemTick(item.id, true)
        }

        val finalizedId = repository.finalizeRun(active.run.id, "")!!
        val archived = repository.observeRunById(finalizedId).first()!!
        assertEquals(RunStatus.COMPLETED, archived.statusEnum)

        val suggested = PdfReportGenerator.suggestedFileName(archived)
        assertTrue(suggested.startsWith("maintenance_weekly_"))
        assertTrue(suggested.endsWith(".pdf"))
    }

    @Test
    fun `setup template edits sync to active run without altering archived reports`() = runTest {
        repository.ensureInitialized()
        val initialActive = repository.observeActiveRun(ChecklistFrequency.MONTHLY).first()!!
        val archivedId = repository.finalizeRun(initialActive.run.id, "Baseline")!!

        repository.addTemplateItem(
            frequency = ChecklistFrequency.MONTHLY,
            zone = "Boiler Room",
            title = "Test pressure relief valve"
        )

        val updatedActive = repository.observeActiveRun(ChecklistFrequency.MONTHLY).first()!!
        assertTrue(updatedActive.items.any { it.title == "Test pressure relief valve" })

        val archived = repository.observeRunById(archivedId).first()!!
        assertTrue(archived.items.none { it.title == "Test pressure relief valve" })
    }
}
