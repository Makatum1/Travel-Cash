package com.example

import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplit
import com.example.data.model.IncomeContribution
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.TripEntity
import com.example.util.ExportImportManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExportImportManagerTest {

    private fun createSampleTripData(): Triple<TripEntity, List<ParticipantEntity>, Pair<List<ExpenseEntity>, List<IncomeEntity>>> {
        val trip = TripEntity(
            id = "trip-test-1",
            title = "Viaje a Roma",
            startDate = "2026-06-01",
            endDate = "2026-06-07",
            budget = 1200.0,
            baseCurrency = "EUR",
            headerPhotoUri = null,
            createdAt = 1000L
        )

        val p1 = ParticipantEntity("part-1", trip.id, "Alejandro", "#0066FF")
        val p2 = ParticipantEntity("part-2", trip.id, "María", "#10B981")
        val participants = listOf(p1, p2)

        val split1 = ExpenseSplit("s1", "exp-1", p1.id, 25.0, 50.0)
        val split2 = ExpenseSplit("s2", "exp-1", p2.id, 25.0, 50.0)
        val exp = ExpenseEntity(
            id = "exp-1",
            tripId = trip.id,
            amount = 50.0,
            currency = "EUR",
            amountInBaseCurrency = 50.0,
            exchangeRate = 1.0,
            category = "Restaurantes",
            date = "2026-06-02",
            paymentMethod = "Tarjeta",
            notes = "Cena trattoria",
            latitude = 41.89,
            longitude = 12.49,
            placeName = "Colosseo Ristorante",
            country = "Italia",
            originName = "",
            originLatitude = null,
            originLongitude = null,
            paidById = p1.id,
            photos = emptyList(),
            splits = listOf(split1, split2),
            prorationDays = null,
            createdAt = 2000L
        )
        val expenses = listOf(exp)

        val contrib1 = IncomeContribution(p1.id, 30.0)
        val contrib2 = IncomeContribution(p2.id, 30.0)
        val income = IncomeEntity(
            id = "inc-1",
            tripId = trip.id,
            amount = 60.0,
            date = "2026-06-01",
            description = "Fondo común",
            basePerPerson = 30.0,
            calculationMethod = "perPerson",
            contributions = listOf(contrib1, contrib2)
        )
        val incomes = listOf(income)

        return Triple(trip, participants, Pair(expenses, incomes))
    }

    @Test
    fun `test single trip json export and parse`() {
        val (trip, parts, expsAndIncs) = createSampleTripData()
        val (exps, incs) = expsAndIncs

        val jsonString = ExportImportManager.exportFullBackupJson(
            trips = listOf(trip),
            participants = parts,
            expenses = exps,
            incomes = incs
        )

        // Parse through parseFullBackupJson
        val payload = ExportImportManager.parseFullBackupJson(jsonString)
        assertEquals(1, payload.trips.size)
        assertEquals("Viaje a Roma", payload.trips.first().title)
        assertEquals(2, payload.participants.size)
        assertEquals(1, payload.expenses.size)
        assertEquals(1, payload.incomes.size)

        // Parse through readTripsFromBytes as direct JSON bytes
        val packages = ExportImportManager.readTripsFromBytes(jsonString.toByteArray(Charsets.UTF_8))
        assertEquals(1, packages.size)
        val pkg = packages.first()
        assertEquals("Viaje a Roma", pkg.trip.title)
        assertEquals(2, pkg.participants.size)
        assertEquals(1, pkg.expenses.size)
        assertEquals(1, pkg.incomes.size)
    }

    @Test
    fun `test single trip zip export and reimport`() {
        val (trip, parts, expsAndIncs) = createSampleTripData()
        val (exps, incs) = expsAndIncs

        val baos = ByteArrayOutputStream()
        ExportImportManager.writeTripsToZip(
            outputStream = baos,
            tripsToExport = listOf(trip),
            allParticipants = parts,
            allExpenses = exps,
            allIncomes = incs
        )

        val zipBytes = baos.toByteArray()
        assertTrue(zipBytes.size > 20)

        val packages = ExportImportManager.readTripsFromBytes(zipBytes)
        assertEquals(1, packages.size)
        val pkg = packages.first()
        assertEquals(trip.id, pkg.trip.id)
        assertEquals("Viaje a Roma", pkg.trip.title)
        assertEquals(2, pkg.participants.size)
        assertEquals(1, pkg.expenses.size)
    }

    @Test
    fun `test multiple trips zip export with duplicate titles`() {
        val (trip1, parts1, expsAndIncs1) = createSampleTripData()
        val trip2 = trip1.copy(id = "trip-test-2", title = "Viaje a Roma") // Duplicate title

        val allTrips = listOf(trip1, trip2)
        val allParts = parts1 + parts1.map { it.copy(id = it.id + "-t2", tripId = trip2.id) }
        val allExps = expsAndIncs1.first + expsAndIncs1.first.map { it.copy(id = it.id + "-t2", tripId = trip2.id) }
        val allIncs = expsAndIncs1.second + expsAndIncs1.second.map { it.copy(id = it.id + "-t2", tripId = trip2.id) }

        val baos = ByteArrayOutputStream()
        ExportImportManager.writeTripsToZip(
            outputStream = baos,
            tripsToExport = allTrips,
            allParticipants = allParts,
            allExpenses = allExps,
            allIncomes = allIncs
        )

        val packages = ExportImportManager.readTripsFromBytes(baos.toByteArray())
        assertEquals(2, packages.size)
        assertEquals(trip1.id, packages[0].trip.id)
        assertEquals(trip2.id, packages[1].trip.id)
    }

    @Test
    fun `test safe handling of UTF-8 BOM and empty entries`() {
        val (trip, parts, expsAndIncs) = createSampleTripData()
        val rawJson = ExportImportManager.exportFullBackupJson(
            trips = listOf(trip),
            participants = parts,
            expenses = expsAndIncs.first,
            incomes = expsAndIncs.second
        )

        // String with UTF-8 BOM prefix \uFEFF
        val jsonWithBom = "\uFEFF$rawJson"
        val payload = ExportImportManager.parseFullBackupJson(jsonWithBom)
        assertEquals(1, payload.trips.size)
        assertEquals("Viaje a Roma", payload.trips.first().title)

        // Empty bytes
        val emptyPackages = ExportImportManager.readTripsFromBytes(byteArrayOf())
        assertTrue(emptyPackages.isEmpty())

        // Empty string
        val emptyPayload = ExportImportManager.parseFullBackupJson("")
        assertTrue(emptyPayload.trips.isEmpty())
    }

    @Test
    fun `test name normalization and capitalization`() {
        assertEquals("paris", ExportImportManager.normalizeTripName("  París  "))
        assertEquals("paris", ExportImportManager.normalizeTripName("PARIS"))
        assertEquals("paris", ExportImportManager.normalizeTripName("paris"))

        assertEquals("Paris", ExportImportManager.capitalizeTripName("paris"))
        assertEquals("Tokio", ExportImportManager.capitalizeTripName("tokio"))
    }
}
