package com.example

import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplit
import com.example.data.model.IncomeContribution
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.TripEntity
import com.example.domain.DebtCalculator
import com.example.util.ExportImportManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExportImportManagerTest {

    private fun sampleData(): Triple<List<TripEntity>, List<ParticipantEntity>, Pair<List<ExpenseEntity>, List<IncomeEntity>>> {
        val trip1 = TripEntity(
            id = "trip-1",
            title = "París",
            startDate = "2026-05-01",
            endDate = "2026-05-05",
            budget = 1000.0,
            baseCurrency = "EUR"
        )
        val trip2 = TripEntity(
            id = "trip-2",
            title = "París",
            startDate = "2026-06-01",
            endDate = "2026-06-03",
            budget = null,
            baseCurrency = "EUR"
        )
        val p1 = ParticipantEntity(id = "p1", tripId = "trip-1", name = "Alejandro", avatarColor = "#0066FF")
        val p2 = ParticipantEntity(id = "p2", tripId = "trip-1", name = "María", avatarColor = "#10B981")
        val p3 = ParticipantEntity(id = "p3", tripId = "trip-2", name = "Carlos", avatarColor = "#F59E0B")

        val exp1 = ExpenseEntity(
            id = "e1",
            tripId = "trip-1",
            amount = 50.0,
            currency = "EUR",
            amountInBaseCurrency = 50.0,
            exchangeRate = 1.0,
            category = "Restaurantes y Comida",
            date = "2026-05-02",
            paymentMethod = "Tarjeta",
            notes = "Cena",
            placeName = "Bistro",
            paidById = "p1",
            splits = listOf(
                ExpenseSplit("e1-s1", "e1", "p1", 25.0, 50.0),
                ExpenseSplit("e1-s2", "e1", "p2", 25.0, 50.0)
            )
        )
        val exp2 = ExpenseEntity(
            id = "e2",
            tripId = "trip-1",
            amount = 20.0,
            currency = "EUR",
            amountInBaseCurrency = 20.0,
            exchangeRate = 1.0,
            category = "Transporte",
            date = "2026-05-02",
            paymentMethod = "Bote Común",
            notes = "Metro",
            paidById = DebtCalculator.BOTE_COMUN_ID,
            splits = listOf(
                ExpenseSplit("e2-s1", "e2", "p1", 10.0, 50.0),
                ExpenseSplit("e2-s2", "e2", "p2", 10.0, 50.0)
            )
        )
        val inc1 = IncomeEntity(
            id = "i1",
            tripId = "trip-1",
            amount = 60.0,
            date = "2026-05-01",
            description = "Bote inicial",
            basePerPerson = 30.0,
            contributions = listOf(
                IncomeContribution("p1", 30.0, 0.0),
                IncomeContribution("p2", 30.0, 0.0)
            )
        )
        return Triple(listOf(trip1, trip2), listOf(p1, p2, p3), Pair(listOf(exp1, exp2), listOf(inc1)))
    }

    @Test
    fun `export single trip as JSON and reimport succeeds`() {
        val (trips, parts, expsAndIncs) = sampleData()
        val trip1 = trips.first()
        val json = ExportImportManager.exportFullBackupJson(
            trips = listOf(trip1),
            participants = parts.filter { it.tripId == trip1.id },
            expenses = expsAndIncs.first.filter { it.tripId == trip1.id },
            incomes = expsAndIncs.second.filter { it.tripId == trip1.id }
        )

        val imported = ExportImportManager.readTripsFromBytes(json.toByteArray(Charsets.UTF_8))
        assertEquals(1, imported.size)
        assertEquals("París", imported[0].trip.title)
        assertEquals(2, imported[0].participants.size)
        assertEquals(2, imported[0].expenses.size)
        assertEquals(1, imported[0].incomes.size)
    }

    @Test
    fun `export single trip as ZIP and reimport succeeds`() {
        val (trips, parts, expsAndIncs) = sampleData()
        val trip1 = trips.first()
        val baos = ByteArrayOutputStream()
        ExportImportManager.writeTripsToZip(
            outputStream = baos,
            tripsToExport = listOf(trip1),
            allParticipants = parts,
            allExpenses = expsAndIncs.first,
            allIncomes = expsAndIncs.second
        )

        val imported = ExportImportManager.readTripsFromZip(ByteArrayInputStream(baos.toByteArray()))
        assertEquals(1, imported.size)
        assertEquals("París", imported[0].trip.title)
        assertEquals(2, imported[0].participants.size)
        assertEquals(2, imported[0].expenses.size)
        assertEquals(1, imported[0].incomes.size)
    }

    @Test
    fun `export all trips including duplicate titles as ZIP and reimport succeeds`() {
        val (trips, parts, expsAndIncs) = sampleData()
        val baos = ByteArrayOutputStream()
        ExportImportManager.writeTripsToZip(
            outputStream = baos,
            tripsToExport = trips,
            allParticipants = parts,
            allExpenses = expsAndIncs.first,
            allIncomes = expsAndIncs.second
        )

        val imported = ExportImportManager.readTripsFromZip(ByteArrayInputStream(baos.toByteArray()))
        assertEquals(2, imported.size)
        assertEquals(2, imported[0].participants.size)
        assertEquals(1, imported[1].participants.size)
    }

    @Test
    fun `import handles UTF8 BOM and empty input safely`() {
        val (trips, parts, expsAndIncs) = sampleData()
        val trip1 = trips.first()
        val jsonWithBom = "\uFEFF" + ExportImportManager.exportFullBackupJson(
            trips = listOf(trip1),
            participants = parts.filter { it.tripId == trip1.id },
            expenses = expsAndIncs.first.filter { it.tripId == trip1.id },
            incomes = expsAndIncs.second.filter { it.tripId == trip1.id }
        )

        val imported = ExportImportManager.readTripsFromBytes(jsonWithBom.toByteArray(Charsets.UTF_8))
        assertEquals(1, imported.size)
        assertTrue(ExportImportManager.readTripsFromBytes(ByteArray(0)).isEmpty())
    }

    @Test
    fun `normalizeTripName ignores case, accents and surrounding spaces`() {
        assertEquals("paris", ExportImportManager.normalizeTripName("París"))
        assertEquals("paris", ExportImportManager.normalizeTripName("  paris  "))
        assertEquals("paris", ExportImportManager.normalizeTripName("PARÍS"))
        assertEquals("malaga", ExportImportManager.normalizeTripName("Málaga"))
        assertEquals("Viaje a roma", ExportImportManager.capitalizeTripName("  viaje a roma "))
    }
}
