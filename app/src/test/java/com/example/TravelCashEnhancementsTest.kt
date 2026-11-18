package com.example

import com.example.data.model.CategoryCatalog
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplit
import com.example.data.model.ParticipantEntity
import com.example.domain.DebtCalculator
import com.example.domain.MapPinInput
import com.example.domain.MapWaypointOffsetCalculator
import com.example.domain.ParticipantNetSpendCalculator
import com.example.util.DailyReminderScheduler
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import kotlin.math.hypot

class TravelCashEnhancementsTest {

    @Test
    fun mapWaypointOffsetCalculator_separatesOverlappingPinsWithoutMutatingSource() {
        val originalLat = 41.9028
        val originalLon = 12.4964

        val exp1 = ExpenseEntity(
            id = "e1",
            tripId = "t1",
            amount = 20.0,
            currency = "EUR",
            amountInBaseCurrency = 20.0,
            exchangeRate = 1.0,
            category = "Restaurantes y Comida",
            date = "2026-07-10",
            paymentMethod = "Tarjeta",
            notes = "Almuerzo",
            latitude = originalLat,
            longitude = originalLon,
            placeName = "Plaza Mayor",
            paidById = "p1"
        )
        val exp2 = exp1.copy(id = "e2", notes = "Café")
        val exp3 = exp1.copy(id = "e3", notes = "Helado")
        val distantExp = exp1.copy(id = "e4", latitude = 41.8902, longitude = 12.4922)

        val pins = listOf(exp1, exp2, exp3, distantExp).map {
            MapPinInput("dest-${it.id}", it.latitude!!, it.longitude!!)
        }

        val displayCoords = MapWaypointOffsetCalculator.computeDisplayCoordinates(pins)

        val c1 = displayCoords["dest-e1"]!!
        val c2 = displayCoords["dest-e2"]!!
        val c3 = displayCoords["dest-e3"]!!
        val c4 = displayCoords["dest-e4"]!!

        // Todos los pines solapados obtienen posiciones distintas
        assertTrue(hypot(c1.first - c2.first, c1.second - c2.second) > 0.0001)
        assertTrue(hypot(c1.first - c3.first, c1.second - c3.second) > 0.0001)
        assertTrue(hypot(c2.first - c3.first, c2.second - c3.second) > 0.0001)

        // El pin aislado mantiene sus coordenadas exactas
        assertEquals(41.8902, c4.first, 1e-9)
        assertEquals(12.4922, c4.second, 1e-9)

        // Las coordenadas reales de las entidades jamás se alteran
        assertEquals(originalLat, exp1.latitude!!, 1e-9)
        assertEquals(originalLon, exp1.longitude!!, 1e-9)
        assertEquals(originalLat, exp2.latitude!!, 1e-9)
        assertEquals(originalLon, exp2.longitude!!, 1e-9)
    }

    @Test
    fun debtSettlements_areAlwaysExcludedFromMapFilter() {
        val settlementWithCoords = ExpenseEntity(
            id = "settle-1234",
            tripId = "t1",
            amount = 50.0,
            currency = "EUR",
            amountInBaseCurrency = 50.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-07-12",
            paymentMethod = "Bizum",
            notes = "Liquidación",
            latitude = 40.4168,
            longitude = -3.7038,
            paidById = "p1"
        )
        val regularExpense = settlementWithCoords.copy(
            id = "exp-5678",
            category = "Alojamiento"
        )

        val mapEligible = listOf(settlementWithCoords, regularExpense).filter {
            !CategoryCatalog.isDebtSettlementExpense(it) &&
                it.latitude != null &&
                it.longitude != null
        }

        assertEquals(1, mapEligible.size)
        assertEquals("exp-5678", mapEligible.first().id)
        assertTrue(CategoryCatalog.isDebtSettlementExpense(settlementWithCoords))
        assertFalse(CategoryCatalog.isDebtSettlementExpense(regularExpense))
    }

    @Test
    fun dailyReminderScheduler_defaultsTo2100AndComputesNextTriggerCorrectly() {
        assertEquals(21, DailyReminderScheduler.DEFAULT_HOUR)
        assertEquals(0, DailyReminderScheduler.DEFAULT_MINUTE)
        assertEquals("21:00", DailyReminderScheduler.formatTime(21, 0))
        assertEquals("08:05", DailyReminderScheduler.formatTime(8, 5))

        // Si son las 18:30 y el recordatorio es a las 21:00 -> dispara hoy mismo a las 21:00
        val beforeCal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 10, 18, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerSameDay = DailyReminderScheduler.computeNextTriggerMillis(21, 0, beforeCal.timeInMillis)
        val resultSameDay = Calendar.getInstance().apply { timeInMillis = triggerSameDay }
        assertEquals(10, resultSameDay.get(Calendar.DAY_OF_MONTH))
        assertEquals(21, resultSameDay.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultSameDay.get(Calendar.MINUTE))

        // Si son las 21:30 y el recordatorio es a las 21:00 -> dispara mañana a las 21:00
        val afterCal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 10, 21, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerNextDay = DailyReminderScheduler.computeNextTriggerMillis(21, 0, afterCal.timeInMillis)
        val resultNextDay = Calendar.getInstance().apply { timeInMillis = triggerNextDay }
        assertEquals(11, resultNextDay.get(Calendar.DAY_OF_MONTH))
        assertEquals(21, resultNextDay.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultNextDay.get(Calendar.MINUTE))
    }

    @Test
    fun dateSynchronization_editingSettlementDateReflectsAcrossReportsWithoutAlteringBalance() {
        val ana = ParticipantEntity("p1", "t1", "Ana", "#0066FF")
        val luis = ParticipantEntity("p2", "t1", "Luis", "#10B981")
        val participants = listOf(ana, luis)

        val dinner = ExpenseEntity(
            id = "e1",
            tripId = "t1",
            amount = 100.0,
            currency = "EUR",
            amountInBaseCurrency = 100.0,
            exchangeRate = 1.0,
            category = "Restaurantes y Comida",
            date = "2026-06-01",
            paymentMethod = "Tarjeta",
            notes = "Cena",
            paidById = "p1",
            splits = listOf(
                ExpenseSplit("s1", "e1", "p1", 50.0, 50.0),
                ExpenseSplit("s2", "e1", "p2", 50.0, 50.0)
            )
        )
        val settlement = ExpenseEntity(
            id = "settle-1",
            tripId = "t1",
            amount = 50.0,
            currency = "EUR",
            amountInBaseCurrency = 50.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-06-02",
            paymentMethod = "Bizum",
            notes = "Liquidación Luis -> Ana",
            paidById = "p2",
            splits = listOf(ExpenseSplit("ss1", "settle-1", "p1", 50.0, 100.0))
        )

        val balanceBefore = DebtCalculator.calculateDebtBalances(participants, listOf(dinner, settlement), emptyList())

        // Modificamos la fecha de la liquidación a 15/06/2026 normalizada a ISO
        val newIsoDate = FormatUtils.parseFlexibleDateToIso("15/06/2026")
        assertEquals("2026-06-15", newIsoDate)
        val editedSettlement = settlement.copy(date = newIsoDate)

        val updatedExpenses = listOf(dinner, editedSettlement)
        val balanceAfter = DebtCalculator.calculateDebtBalances(participants, updatedExpenses, emptyList())
        val netReport = ParticipantNetSpendCalculator.calculate(participants, updatedExpenses, emptyList())

        // El balance matemático no cambia al cambiar la fecha
        assertEquals(
            balanceBefore.participantBalances.first { it.participantId == "p1" }.netBalance,
            balanceAfter.participantBalances.first { it.participantId == "p1" }.netBalance,
            0.001
        )
        assertEquals(0.0, balanceAfter.participantBalances.first { it.participantId == "p1" }.netBalance, 0.001)

        // La fecha modificada se refleja exactamente igual en todos los movimientos del informe
        val anaTransferEntry = netReport.participantSummaries
            .first { it.participant.id == "p1" }
            .entries
            .first { it.sourceExpense?.id == "settle-1" }
        val luisTransferEntry = netReport.participantSummaries
            .first { it.participant.id == "p2" }
            .entries
            .first { it.sourceExpense?.id == "settle-1" }

        assertEquals("2026-06-15", anaTransferEntry.date)
        assertEquals("2026-06-15", luisTransferEntry.date)
        assertNotEquals("2026-06-02", anaTransferEntry.date)
    }

    @Test
    fun personByCategoryBreakdown_groupsRealExpensesByParticipantAndCategoryExcludingSettlements() {
        val ana = ParticipantEntity("p1", "t1", "Ana", "#0066FF")
        val luis = ParticipantEntity("p2", "t1", "Luis", "#10B981")
        val participants = listOf(ana, luis)

        val hotel = ExpenseEntity(
            id = "e1",
            tripId = "t1",
            amount = 200.0,
            currency = "EUR",
            amountInBaseCurrency = 200.0,
            exchangeRate = 1.0,
            category = "Alojamiento",
            date = "2026-06-01",
            paymentMethod = "Tarjeta",
            notes = "Hotel Centro",
            paidById = "p1",
            splits = listOf(
                ExpenseSplit("s1", "e1", "p1", 120.0, 60.0),
                ExpenseSplit("s2", "e1", "p2", 80.0, 40.0)
            )
        )
        val food = ExpenseEntity(
            id = "e2",
            tripId = "t1",
            amount = 60.0,
            currency = "EUR",
            amountInBaseCurrency = 60.0,
            exchangeRate = 1.0,
            category = "Restaurantes y Comida",
            date = "2026-06-02",
            paymentMethod = "Bote Común",
            notes = "Tapas",
            paidById = DebtCalculator.BOTE_COMUN_ID,
            splits = listOf(
                ExpenseSplit("s3", "e2", "p1", 30.0, 50.0),
                ExpenseSplit("s4", "e2", "p2", 30.0, 50.0)
            )
        )
        val settlement = ExpenseEntity(
            id = "settle-99",
            tripId = "t1",
            amount = 80.0,
            currency = "EUR",
            amountInBaseCurrency = 80.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-06-03",
            paymentMethod = "Bizum",
            notes = "Liquidación",
            paidById = "p2",
            splits = listOf(ExpenseSplit("s5", "settle-99", "p1", 80.0, 100.0))
        )

        val breakdown = ParticipantNetSpendCalculator.calculateByParticipantAndCategory(
            participants = participants,
            expenses = listOf(hotel, food, settlement)
        )

        val anaBreakdown = breakdown.first { it.participant.id == "p1" }
        val luisBreakdown = breakdown.first { it.participant.id == "p2" }

        assertEquals(150.0, anaBreakdown.totalAmount, 0.01)
        assertEquals(2, anaBreakdown.categories.size)
        assertEquals("Alojamiento", anaBreakdown.categories[0].categoryName)
        assertEquals(120.0, anaBreakdown.categories[0].totalAmount, 0.01)
        assertEquals("Restaurantes y Comida", anaBreakdown.categories[1].categoryName)
        assertEquals(30.0, anaBreakdown.categories[1].totalAmount, 0.01)

        assertEquals(110.0, luisBreakdown.totalAmount, 0.01)
        assertEquals(80.0, luisBreakdown.categories.first { it.categoryName == "Alojamiento" }.totalAmount, 0.01)
        assertEquals(30.0, luisBreakdown.categories.first { it.categoryName == "Restaurantes y Comida" }.totalAmount, 0.01)

        // Ninguna liquidación debe aparecer en el desglose de categorías por persona
        assertTrue(anaBreakdown.categories.none { CategoryCatalog.isDebtSettlementCategory(it.categoryName) })
        assertTrue(luisBreakdown.categories.none { CategoryCatalog.isDebtSettlementCategory(it.categoryName) })
    }

    @Test
    fun expensesList_ordersMostRecentDateFirstDescending() {
        val older = ExpenseEntity(
            id = "e-old",
            tripId = "t1",
            amount = 15.0,
            currency = "EUR",
            amountInBaseCurrency = 15.0,
            exchangeRate = 1.0,
            category = "General",
            date = "2026-07-01",
            paymentMethod = "Tarjeta",
            notes = "Día 1",
            paidById = "p1",
            createdAt = 1000L
        )
        val middle = older.copy(id = "e-mid", date = "2026-07-05", createdAt = 2000L)
        val newest = older.copy(id = "e-new", date = "2026-07-10", createdAt = 3000L)

        val sortedDescending = listOf(older, newest, middle).sortedWith(
            compareByDescending<ExpenseEntity> { FormatUtils.parseFlexibleDateToIso(it.date) }
                .thenByDescending { it.createdAt }
        )

        assertEquals(listOf("e-new", "e-mid", "e-old"), sortedDescending.map { it.id })
    }

    @Test
    fun excessSettlementPayment_becomesPersonalPotAndConsumesSubsequentExpensesBeforeRegeneratingDebt() {
        val user1 = ParticipantEntity("u1", "t1", "Usuario 1 (Admin)", "#0066FF")
        val user2 = ParticipantEntity("u2", "t1", "Usuario 2", "#10B981")
        val participants = listOf(user1, user2)

        // Paso 1: Usuario 1 paga un gasto inicial de 296€ que le correspondía asumir al Usuario 2
        val initialExpense = ExpenseEntity(
            id = "exp-init",
            tripId = "t1",
            amount = 296.0,
            currency = "EUR",
            amountInBaseCurrency = 296.0,
            exchangeRate = 1.0,
            category = "Alojamiento",
            date = "2026-07-01",
            paymentMethod = "Tarjeta",
            notes = "Hotel",
            paidById = "u1",
            splits = listOf(ExpenseSplit("s1", "exp-init", "u2", 296.0, 100.0))
        )

        val res1 = DebtCalculator.calculateDebtBalances(participants, listOf(initialExpense), emptyList())
        assertEquals(1, res1.transactions.size)
        assertEquals("u2", res1.transactions.first().fromParticipantId)
        assertEquals("u1", res1.transactions.first().toParticipantId)
        assertEquals(296.0, res1.transactions.first().amount, 0.01)

        // Paso 2: Usuario 2 le da 500€ al Usuario 1 (liquidación manual con pago en exceso)
        val excessSettlement = ExpenseEntity(
            id = "settle-excess",
            tripId = "t1",
            amount = 500.0,
            currency = "EUR",
            amountInBaseCurrency = 500.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-07-02",
            paymentMethod = "Bizum",
            notes = "Liquidación con exceso: 500€",
            paidById = "u2",
            splits = listOf(ExpenseSplit("s2", "settle-excess", "u1", 500.0, 100.0))
        )

        val expensesStep2 = listOf(initialExpense, excessSettlement)
        val res2 = DebtCalculator.calculateDebtBalances(participants, expensesStep2, emptyList())
        val netReport2 = ParticipantNetSpendCalculator.calculate(participants, expensesStep2, emptyList())

        val u1Balance2 = res2.participantBalances.first { it.participantId == "u1" }
        val u2Balance2 = res2.participantBalances.first { it.participantId == "u2" }
        val u2Summary2 = netReport2.participantSummaries.first { it.participant.id == "u2" }

        // El desembolso de Usuario 2 es 500€, su gasto asumido es 296€ y su balance a favor es +204€
        assertEquals(500.0, u2Balance2.totalExpensesPaid, 0.01)
        assertEquals(296.0, u2Balance2.totalExpensesOwed, 0.01)
        assertEquals(204.0, u2Balance2.netBalance, 0.01)

        assertEquals(500.0, u2Summary2.netSpend, 0.01)
        assertEquals(296.0, u2Summary2.assumedSpend, 0.01)
        assertEquals(204.0, u2Summary2.balance, 0.01)

        // No hay deudas pendientes entre ellos: el excedente de 204€ es bote personal de Usuario 2
        assertTrue(res2.transactions.isEmpty())

        // Paso 3: Usuario 1 paga un nuevo gasto de 100€ imputable a Usuario 2
        // Se debe ir consumiendo del bote personal (quedan 104€ a favor) y NO generar deuda
        val expense3 = ExpenseEntity(
            id = "exp-3",
            tripId = "t1",
            amount = 100.0,
            currency = "EUR",
            amountInBaseCurrency = 100.0,
            exchangeRate = 1.0,
            category = "Restaurantes y Comida",
            date = "2026-07-03",
            paymentMethod = "Tarjeta",
            notes = "Cena",
            paidById = "u1",
            splits = listOf(ExpenseSplit("s3", "exp-3", "u2", 100.0, 100.0))
        )

        val expensesStep3 = listOf(initialExpense, excessSettlement, expense3)
        val res3 = DebtCalculator.calculateDebtBalances(participants, expensesStep3, emptyList())
        val u2Balance3 = res3.participantBalances.first { it.participantId == "u2" }

        assertEquals(500.0, u2Balance3.totalExpensesPaid, 0.01)
        assertEquals(396.0, u2Balance3.totalExpensesOwed, 0.01)
        assertEquals(104.0, u2Balance3.netBalance, 0.01)
        // Sigue sin haber deuda porque aún le quedan 104€ en su bote personal
        assertTrue(res3.transactions.isEmpty())

        // Paso 4: Usuario 1 paga otro gasto de 150€ imputable a Usuario 2
        // Ahora el consumo total de Usuario 2 es 296 + 100 + 150 = 546€
        // El bote de 500€ se ha agotado por completo y ahora Usuario 2 vuelve a deber la diferencia (46€) a Usuario 1
        val expense4 = ExpenseEntity(
            id = "exp-4",
            tripId = "t1",
            amount = 150.0,
            currency = "EUR",
            amountInBaseCurrency = 150.0,
            exchangeRate = 1.0,
            category = "Excursiones",
            date = "2026-07-04",
            paymentMethod = "Tarjeta",
            notes = "Tour guiado",
            paidById = "u1",
            splits = listOf(ExpenseSplit("s4", "exp-4", "u2", 150.0, 100.0))
        )

        val expensesStep4 = listOf(initialExpense, excessSettlement, expense3, expense4)
        val res4 = DebtCalculator.calculateDebtBalances(participants, expensesStep4, emptyList())
        val u2Balance4 = res4.participantBalances.first { it.participantId == "u2" }

        assertEquals(500.0, u2Balance4.totalExpensesPaid, 0.01)
        assertEquals(546.0, u2Balance4.totalExpensesOwed, 0.01)
        assertEquals(-46.0, u2Balance4.netBalance, 0.01)

        // Ahora que el bote se agotó, se regenera la deuda pendiente de 46€ desde Usuario 2 a Usuario 1
        assertEquals(1, res4.transactions.size)
        val txRegenerated = res4.transactions.first()
        assertEquals("u2", txRegenerated.fromParticipantId)
        assertEquals("u1", txRegenerated.toParticipantId)
        assertEquals(46.0, txRegenerated.amount, 0.01)
    }

    @Test
    fun standardMultiParticipantTrip_originalDebtCalculationAndNormalSettlement_worksCleanly() {
        val u1 = ParticipantEntity("u1", "t1", "Ana", "#0066FF")
        val u2 = ParticipantEntity("u2", "t1", "Carlos", "#10B981")
        val u3 = ParticipantEntity("u3", "t1", "Beatriz", "#F59E0B")
        val participants = listOf(u1, u2, u3)

        // Gasto 1: Ana paga 300€ repartido entre los 3 (100€ cada uno)
        val expense1 = ExpenseEntity(
            id = "exp-shared",
            tripId = "t1",
            amount = 300.0,
            currency = "EUR",
            amountInBaseCurrency = 300.0,
            exchangeRate = 1.0,
            category = "Restaurantes y Comida",
            date = "2026-07-01",
            paymentMethod = "Tarjeta",
            notes = "Cena grupo",
            paidById = "u1",
            splits = listOf(
                ExpenseSplit("s1", "exp-shared", "u1", 100.0, 33.33),
                ExpenseSplit("s2", "exp-shared", "u2", 100.0, 33.33),
                ExpenseSplit("s3", "exp-shared", "u3", 100.0, 33.33)
            )
        )

        val res1 = DebtCalculator.calculateDebtBalances(participants, listOf(expense1), emptyList())
        assertEquals(2, res1.transactions.size)
        assertTrue(res1.transactions.any { it.fromParticipantId == "u2" && it.toParticipantId == "u1" && it.amount == 100.0 })
        assertTrue(res1.transactions.any { it.fromParticipantId == "u3" && it.toParticipantId == "u1" && it.amount == 100.0 })

        // Liquidación normal: Carlos liquida sus 100€ a Ana
        val settleCarlos = ExpenseEntity(
            id = "settle-carlos",
            tripId = "t1",
            amount = 100.0,
            currency = "EUR",
            amountInBaseCurrency = 100.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-07-02",
            paymentMethod = "Bizum",
            notes = "Liquidación Carlos a Ana",
            paidById = "u2",
            splits = listOf(ExpenseSplit("sc", "settle-carlos", "u1", 100.0, 100.0))
        )

        val res2 = DebtCalculator.calculateDebtBalances(participants, listOf(expense1, settleCarlos), emptyList())
        // Carlos ya no debe nada; solo Beatriz debe 100€ a Ana
        assertEquals(1, res2.transactions.size)
        assertEquals("u3", res2.transactions.first().fromParticipantId)
        assertEquals("u1", res2.transactions.first().toParticipantId)
        assertEquals(100.0, res2.transactions.first().amount, 0.01)

        val carlosBalance = res2.participantBalances.first { it.participantId == "u2" }
        assertEquals(100.0, carlosBalance.totalExpensesPaid, 0.01)
        assertEquals(100.0, carlosBalance.totalExpensesOwed, 0.01)
        assertEquals(0.0, carlosBalance.netBalance, 0.01)
    }
}
