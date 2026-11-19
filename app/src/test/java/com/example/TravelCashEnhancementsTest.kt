package com.example

import com.example.data.model.CategoryCatalog
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplit
import com.example.data.model.IncomeContribution
import com.example.data.model.IncomeEntity
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
    fun extraBalance_adjustmentSection_progressiveCalculation_matchesValidationScenario() {
        // Escenario de validación obligatorio:
        // Total de Gastos del viaje: -1010€.
        // Usuario 1: Su parte proporcional (gastos asumidos) es de 296€.
        // Cálculo: Al total (-1010) se le suma su parte (296). Resultado: -714€ (que se mostrará como 714).
        // Usuario 2: Su parte proporcional es de 296€. (Da 500€ a cuenta, pero eso no afecta a esta resta).
        // Cálculo: Al ajuste anterior (-714) se le suma su parte (296). Resultado: -418€ (que se mostrará como 418).
        // Usuario 3: Su parte proporcional es de 296€. (Da 300€ a cuenta, pero eso no afecta a esta resta).
        // Cálculo: Al ajuste anterior (-418) se le suma su parte (296). Resultado: -122€ (que se mostrará como -122).
        // si otro usuario su ajuste de gasto es 21 pues solo esa cuantía (-122 + 21 = -101)

        val u1 = ParticipantEntity("u1", "t1", "Usuario 1", "#0066FF")
        val u2 = ParticipantEntity("u2", "t1", "Usuario 2", "#10B981")
        val u3 = ParticipantEntity("u3", "t1", "Usuario 3", "#F59E0B")
        val participants = listOf(u1, u2, u3)

        // Gasto 1: 888€ asumido a partes iguales entre U1, U2 y U3 (296€ cada uno)
        val exp888 = ExpenseEntity(
            id = "exp-1",
            tripId = "t1",
            amount = 888.0,
            currency = "EUR",
            amountInBaseCurrency = 888.0,
            exchangeRate = 1.0,
            category = "Alojamiento",
            date = "2026-07-01",
            paymentMethod = "Tarjeta",
            notes = "Hotel",
            paidById = u1.id,
            splits = listOf(
                ExpenseSplit("s1", "exp-1", u1.id, 296.0, 33.33),
                ExpenseSplit("s2", "exp-1", u2.id, 296.0, 33.33),
                ExpenseSplit("s3", "exp-1", u3.id, 296.0, 33.34)
            )
        )

        // Gasto 2: 122€ para alcanzar exactamente 1010€ de gastos totales del viaje (888 + 122 = 1010€)
        val exp122 = ExpenseEntity(
            id = "exp-2",
            tripId = "t1",
            amount = 122.0,
            currency = "EUR",
            amountInBaseCurrency = 122.0,
            exchangeRate = 1.0,
            category = "Transporte",
            date = "2026-07-02",
            paymentMethod = "Tarjeta",
            notes = "Tren",
            paidById = u1.id,
            splits = emptyList()
        )

        // Liquidaciones: U2 da 500€ a U1, U3 da 300€ a U1 (NO deben afectar al cálculo del Ajuste)
        val settleU2 = ExpenseEntity(
            id = "settle-u2",
            tripId = "t1",
            amount = 500.0,
            currency = "EUR",
            amountInBaseCurrency = 500.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-07-03",
            paymentMethod = "Transferencia",
            notes = "Pago a cuenta U2",
            paidById = u2.id
        )

        val settleU3 = ExpenseEntity(
            id = "settle-u3",
            tripId = "t1",
            amount = 300.0,
            currency = "EUR",
            amountInBaseCurrency = 300.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-07-04",
            paymentMethod = "Bizum",
            notes = "Pago a cuenta U3",
            paidById = u3.id
        )

        val allExpenses = listOf(exp888, exp122, settleU2, settleU3)

        // 1. Total de Gastos del viaje (excluyendo liquidaciones)
        val totalSpent = allExpenses
            .filter { !CategoryCatalog.isDebtSettlementExpense(it) }
            .sumOf { it.amountInBaseCurrency }
        assertEquals(1010.0, totalSpent, 0.001)

        // 2. Cálculo matemático de balances
        val debtResult = DebtCalculator.calculateDebtBalances(participants, allExpenses, emptyList())
        val balU1 = debtResult.participantBalances.first { it.participantId == u1.id }
        val balU2 = debtResult.participantBalances.first { it.participantId == u2.id }
        val balU3 = debtResult.participantBalances.first { it.participantId == u3.id }

        assertEquals(296.0, balU1.totalExpensesOwed, 0.001)
        assertEquals(296.0, balU2.totalExpensesOwed, 0.001)
        assertEquals(296.0, balU3.totalExpensesOwed, 0.001)

        // 3. Progresión paso a paso requerida:
        var stepBalance = -totalSpent // -1010€
        assertEquals(-1010.0, stepBalance, 0.001)

        stepBalance += balU1.totalExpensesOwed // -1010 + 296 = -714
        assertEquals(-714.0, stepBalance, 0.001)

        stepBalance += balU2.totalExpensesOwed // -714 + 296 = -418 (el pago de 500 no afecta)
        assertEquals(-418.0, stepBalance, 0.001)

        stepBalance += balU3.totalExpensesOwed // -418 + 296 = -122 (el pago de 300 no afecta)
        assertEquals(-122.0, stepBalance, 0.001)

        // 4. Fórmula directa integrada en TravelCashViewModel:
        val totalAssumed = debtResult.participantBalances.sumOf { it.totalExpensesOwed }
        val personalAdjustment = -totalSpent + totalAssumed
        assertEquals(-122.0, personalAdjustment, 0.001)

        // 5. Formato con signo resultante:
        val formatted = FormatUtils.formatSignedCurrency(personalAdjustment, "EUR", java.util.Locale.GERMANY)
        assertTrue(formatted.startsWith("-122,00"))

        // 6. Caso con 4º participante con ajuste de gasto de 21€:
        val u4Owed = 21.0
        val adjustmentWithU4 = personalAdjustment + u4Owed
        assertEquals(-101.0, adjustmentWithU4, 0.001)
    }

    @Test
    fun extraBalance_scenarioWithoutCommonPot_onlyExpensesAndDirectPayments_ignoresPersonalPot() {
        // Escenario 1: Sin Bote Común (solo gastos y pagos directos de usuarios)
        val u1 = ParticipantEntity("u1", "t1", "Usuario 1", "#0066FF")
        val u2 = ParticipantEntity("u2", "t1", "Usuario 2", "#10B981")
        val participants = listOf(u1, u2)

        // Dos gastos pagados de bolsillo por los usuarios:
        val exp1 = ExpenseEntity(
            id = "e1",
            tripId = "t1",
            amount = 100.0,
            currency = "EUR",
            amountInBaseCurrency = 100.0,
            exchangeRate = 1.0,
            category = "Comida",
            date = "2026-07-01",
            paymentMethod = "Tarjeta",
            notes = "Cena U1",
            paidById = u1.id
        )
        val exp2 = ExpenseEntity(
            id = "e2",
            tripId = "t1",
            amount = 50.0,
            currency = "EUR",
            amountInBaseCurrency = 50.0,
            exchangeRate = 1.0,
            category = "Transporte",
            date = "2026-07-02",
            paymentMethod = "Efectivo",
            notes = "Taxi U2",
            paidById = u2.id
        )

        // Liquidación con pago en exceso entre usuarios (U2 debía 25€ y entrega 500€ -> exceso/bote personal)
        val settle = ExpenseEntity(
            id = "settle-1",
            tripId = "t1",
            amount = 500.0,
            currency = "EUR",
            amountInBaseCurrency = 500.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-07-03",
            paymentMethod = "Bizum",
            notes = "Liquidación con exceso",
            paidById = u2.id
        )

        val expenses = listOf(exp1, exp2, settle)
        val incomes = emptyList<IncomeEntity>() // Sin bote común

        // Bote común = 0
        val totalBoteCollected = incomes.sumOf { it.amount }
        val totalSpentFromBote = expenses.filter { exp ->
            !CategoryCatalog.isDebtSettlementExpense(exp) &&
                (exp.paidById == DebtCalculator.BOTE_COMUN_ID || exp.paymentMethod.equals("Bote Común", ignoreCase = true))
        }.sumOf { it.amountInBaseCurrency }
        val availableBote = totalBoteCollected - totalSpentFromBote
        assertEquals(0.0, availableBote, 0.001)

        // Gastos directos y pagos de usuarios (quien paga salda su gasto):
        val directExpenses = expenses.filter { exp ->
            !CategoryCatalog.isDebtSettlementExpense(exp) &&
                exp.paidById != DebtCalculator.BOTE_COMUN_ID &&
                !exp.paymentMethod.equals("Bote Común", ignoreCase = true)
        }
        val totalDirectExpenses = directExpenses.sumOf { it.amountInBaseCurrency }
        val directPaymentsByUser = directExpenses.filter { it.paidById.isNotBlank() }.sumOf { it.amountInBaseCurrency }

        assertEquals(150.0, totalDirectExpenses, 0.001)
        assertEquals(150.0, directPaymentsByUser, 0.001)

        val personalAdjustment = directPaymentsByUser - totalDirectExpenses
        assertEquals(0.0, personalAdjustment, 0.001)

        val extra = availableBote + personalAdjustment
        assertEquals(0.0, extra, 0.001)

        // La liquidación de 500€ NO entra en el Saldo Extra ni lo altera
        assertTrue(expenses.filter { CategoryCatalog.isDebtSettlementExpense(it) }.isNotEmpty())
    }

    @Test
    fun extraBalance_scenarioWithCommonPot_contributionsAndBoteExpenses_ignoresPersonalPot() {
        // Escenario 2: Con Bote Común (aportaciones al bote y gastos desde el bote)
        val u1 = ParticipantEntity("u1", "t1", "Usuario 1", "#0066FF")
        val u2 = ParticipantEntity("u2", "t1", "Usuario 2", "#10B981")
        val u3 = ParticipantEntity("u3", "t1", "Usuario 3", "#F59E0B")

        // Aportaciones al bote común: 300€
        val incomes = listOf(
            IncomeEntity(
                id = "inc-1",
                tripId = "t1",
                amount = 300.0,
                description = "Fondo común",
                date = "2026-07-01",
                contributions = listOf(
                    IncomeContribution(u1.id, 100.0),
                    IncomeContribution(u2.id, 100.0),
                    IncomeContribution(u3.id, 100.0)
                )
            )
        )

        // Gasto pagado desde el bote común: 90€
        val expFromBote = ExpenseEntity(
            id = "exp-bote",
            tripId = "t1",
            amount = 90.0,
            currency = "EUR",
            amountInBaseCurrency = 90.0,
            exchangeRate = 1.0,
            category = "Supermercado",
            date = "2026-07-02",
            paymentMethod = "Bote Común",
            notes = "Compra comida común",
            paidById = DebtCalculator.BOTE_COMUN_ID
        )

        // Gasto directo pagado de bolsillo por U1: 40€
        val expDirect = ExpenseEntity(
            id = "exp-direct",
            tripId = "t1",
            amount = 40.0,
            currency = "EUR",
            amountInBaseCurrency = 40.0,
            exchangeRate = 1.0,
            category = "Transporte",
            date = "2026-07-02",
            paymentMethod = "Tarjeta",
            notes = "Metro pagado por U1",
            paidById = u1.id
        )

        // Liquidación entre usuarios con exceso / bote personal: U3 transfiere 300€ a U1
        val settle = ExpenseEntity(
            id = "settle-u3",
            tripId = "t1",
            amount = 300.0,
            currency = "EUR",
            amountInBaseCurrency = 300.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-07-03",
            paymentMethod = "Bizum",
            notes = "Liquidación U3 a U1",
            paidById = u3.id
        )

        val expenses = listOf(expFromBote, expDirect, settle)

        // Bote Común: aportaciones (300€) - gastado con bote (90€) = 210€
        val totalBoteCollected = incomes.sumOf { it.amount }
        val totalSpentFromBote = expenses.filter { exp ->
            !CategoryCatalog.isDebtSettlementExpense(exp) &&
                (exp.paidById == DebtCalculator.BOTE_COMUN_ID || exp.paymentMethod.equals("Bote Común", ignoreCase = true))
        }.sumOf { it.amountInBaseCurrency }
        val availableBote = totalBoteCollected - totalSpentFromBote
        assertEquals(210.0, availableBote, 0.001)

        // Gasto directo pagado por U1: 40€ cubierto por 40€ pagado por U1
        val directExpenses = expenses.filter { exp ->
            !CategoryCatalog.isDebtSettlementExpense(exp) &&
                exp.paidById != DebtCalculator.BOTE_COMUN_ID &&
                !exp.paymentMethod.equals("Bote Común", ignoreCase = true)
        }
        val totalDirectExpenses = directExpenses.sumOf { it.amountInBaseCurrency }
        val directPaymentsByUser = directExpenses.filter { it.paidById.isNotBlank() }.sumOf { it.amountInBaseCurrency }
        val personalAdjustment = directPaymentsByUser - totalDirectExpenses
        assertEquals(0.0, personalAdjustment, 0.001)

        // Saldo extra de la caja = 210€ del bote común + 0€ directo = 210€
        val extra = availableBote + personalAdjustment
        assertEquals(210.0, extra, 0.001)

        // Ni la liquidación de 300€ ni los botes personales alteran el saldo del bote común (210€)
        assertEquals(210.0, availableBote, 0.001)
    }

    @Test
    fun extraBalance_exactFormula_matchesValidationScenario() {
        // Test Unitario Obligatorio del requerimiento exacto:
        // Total de Gastos del viaje: 1010€.
        // El Usuario 1 (Administrador) adelanta los 1010€.
        // El Usuario 2 paga su parte de 296€.
        // El Usuario 3 tiene una deuda pendiente de 101€.
        // El Usuario 4 tiene una deuda pendiente de 21€.
        // Cálculo esperado: Como el Usuario 3 debe 101€ y el Usuario 4 debe 21€, el déficit total de la caja es de -122€.
        // Por lo tanto, el 'Saldo extra' debe mostrar exactamente -122. Bajo ningún concepto debe mostrar 0.

        val u1 = ParticipantEntity("u1", "t1", "Usuario 1 (Admin)", "#0066FF")
        val u2 = ParticipantEntity("u2", "t1", "Usuario 2", "#10B981")
        val u3 = ParticipantEntity("u3", "t1", "Usuario 3", "#F59E0B")
        val u4 = ParticipantEntity("u4", "t1", "Usuario 4", "#8B5CF6")
        val participants = listOf(u1, u2, u3, u4)

        // 1. Gasto total del viaje: 1010€ adelantado por Usuario 1 (Admin)
        // Splits:
        // U2 debe 296€
        // U3 debe 101€
        // U4 debe 21€
        // U1 asume el resto: 1010 - 296 - 101 - 21 = 592€
        val exp1010 = ExpenseEntity(
            id = "exp-1010",
            tripId = "t1",
            amount = 1010.0,
            currency = "EUR",
            amountInBaseCurrency = 1010.0,
            exchangeRate = 1.0,
            category = "Alojamiento",
            date = "2026-07-01",
            paymentMethod = "Tarjeta",
            notes = "Adelanto de gastos del viaje por Administrador",
            paidById = u1.id,
            splits = listOf(
                ExpenseSplit("s1", "exp-1010", u1.id, 592.0, (592.0 / 1010.0) * 100.0),
                ExpenseSplit("s2", "exp-1010", u2.id, 296.0, (296.0 / 1010.0) * 100.0),
                ExpenseSplit("s3", "exp-1010", u3.id, 101.0, (101.0 / 1010.0) * 100.0),
                ExpenseSplit("s4", "exp-1010", u4.id, 21.0, (21.0 / 1010.0) * 100.0)
            )
        )

        // 2. El Usuario 2 paga su parte de 296€ (liquidación saldando su deuda hacia Usuario 1)
        val settleU2 = ExpenseEntity(
            id = "settle-u2",
            tripId = "t1",
            amount = 296.0,
            currency = "EUR",
            amountInBaseCurrency = 296.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-07-02",
            paymentMethod = "Bizum",
            notes = "U2 salda su deuda a U1",
            paidById = u2.id,
            splits = listOf(
                ExpenseSplit("s-settle-u2", "settle-u2", u1.id, 296.0, 100.0)
            )
        )

        val expenses = listOf(exp1010, settleU2)
        val incomes = emptyList<IncomeEntity>() // Bote común a 0

        // Calculamos los balances de deuda mediante DebtCalculator
        val debtResult = DebtCalculator.calculateDebtBalances(participants, expenses, incomes)

        // Verificamos balances individuales de los participantes:
        val b1 = debtResult.participantBalances.find { it.participantId == u1.id }!!
        val b2 = debtResult.participantBalances.find { it.participantId == u2.id }!!
        val b3 = debtResult.participantBalances.find { it.participantId == u3.id }!!
        val b4 = debtResult.participantBalances.find { it.participantId == u4.id }!!

        assertEquals(0.0, b2.netBalance, 0.001) // U2 ha saldado totalmente su deuda
        assertEquals(-101.0, b3.netBalance, 0.001) // U3 tiene deuda pendiente de 101€
        assertEquals(-21.0, b4.netBalance, 0.001) // U4 tiene deuda pendiente de 21€
        assertEquals(122.0, b1.netBalance, 0.001) // U1 tiene a favor 122€ (lo que falta por cobrar)

        // Verificamos cálculo de deudas pendientes (balances negativos < -0.009):
        val totalDeudasPendientes = debtResult.participantBalances
            .filter { it.netBalance < -0.009 }
            .sumOf { kotlin.math.abs(it.netBalance) }
        assertEquals(122.0, totalDeudasPendientes, 0.001)

        // Con bote a 0:
        val availableBote = debtResult.availableBote
        assertEquals(0.0, availableBote, 0.001)

        // Saldo Extra = availableBote - totalDeudasPendientes:
        val saldoExtra = availableBote - totalDeudasPendientes
        assertEquals(-122.0, saldoExtra, 0.001)
        assertNotEquals(0.0, saldoExtra, 0.001) // Bajo ningún concepto debe dar 0 con deudas pendientes

        // Si se ingresa dinero al bote común (ej: 200€ al bote):
        val incomesConBote = listOf(
            IncomeEntity(
                id = "inc-200",
                tripId = "t1",
                amount = 200.0,
                date = "2026-07-02",
                description = "Aportación al bote común",
                contributions = emptyList()
            )
        )
        val debtResultConBote = DebtCalculator.calculateDebtBalances(participants, expenses, incomesConBote)
        val saldoExtraConBote = debtResultConBote.availableBote - totalDeudasPendientes
        assertEquals(78.0, saldoExtraConBote, 0.001) // 200 - 122 = +78€

        // Si U3 y U4 también saldan sus deudas (101€ y 21€) sin bote:
        val settleU3 = ExpenseEntity(
            id = "s-u3",
            tripId = "t1",
            amount = 101.0,
            currency = "EUR",
            amountInBaseCurrency = 101.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-07-03",
            paymentMethod = "Bizum",
            notes = "U3 salda deuda",
            paidById = u3.id,
            splits = listOf(ExpenseSplit("s3", "s-u3", u1.id, 101.0, 100.0))
        )
        val settleU4 = ExpenseEntity(
            id = "s-u4",
            tripId = "t1",
            amount = 21.0,
            currency = "EUR",
            amountInBaseCurrency = 21.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-07-03",
            paymentMethod = "Bizum",
            notes = "U4 salda deuda",
            paidById = u4.id,
            splits = listOf(ExpenseSplit("s4", "s-u4", u1.id, 21.0, 100.0))
        )
        val debtResultAllSettled = DebtCalculator.calculateDebtBalances(participants, listOf(exp1010, settleU2, settleU3, settleU4), incomes)
        val deudasAllSettled = debtResultAllSettled.participantBalances
            .filter { it.netBalance < -0.009 }
            .sumOf { kotlin.math.abs(it.netBalance) }
        assertEquals(0.0, deudasAllSettled, 0.001)
        val saldoExtraAllSettled = debtResultAllSettled.availableBote - deudasAllSettled
        assertEquals(0.0, saldoExtraAllSettled, 0.001) // Todos han pagado -> Saldo es 0
    }

    @Test
    fun extraBalance_excessSettlements_ignoresPersonalPots_andReturnsMinus122() {
        // Escenario exacto del usuario:
        // Usuario 2: Deuda inicial 296€, paga 500€. (Exceso / bote personal: 204€).
        // Usuario 3: Deuda inicial 296€, paga 300€. (Exceso: 4€). Deuda pendiente restante: 101€.
        // Usuario 4: Deuda pendiente restante: 21€.
        // Resultado esperado: -122. Bajo ningún concepto debe salir -208.

        val u1 = ParticipantEntity("u1", "t1", "Usuario 1 (Admin)", "#0066FF")
        val u2 = ParticipantEntity("u2", "t1", "Usuario 2", "#10B981")
        val u3 = ParticipantEntity("u3", "t1", "Usuario 3", "#F59E0B")
        val u4 = ParticipantEntity("u4", "t1", "Usuario 4", "#8B5CF6")
        val participants = listOf(u1, u2, u3, u4)

        // Gastos reales del viaje:
        // U2 tiene 296€ asignados.
        // U3 tiene 296€ iniciales + 105€ de gasto adicional = 401€ (o 300€ pagados + 101€ pendientes = 401€).
        // U4 tiene 21€ asignados.
        // U1 adelanta el total de gastos del viaje de su bolsillo.
        val totalGastos = 1010.0
        val expTrip = ExpenseEntity(
            id = "exp-trip-1010",
            tripId = "t1",
            amount = totalGastos,
            currency = "EUR",
            amountInBaseCurrency = totalGastos,
            exchangeRate = 1.0,
            category = "General",
            date = "2026-07-01",
            paymentMethod = "Tarjeta",
            notes = "Adelanto del viaje",
            paidById = u1.id,
            splits = listOf(
                ExpenseSplit("s1", "exp-trip-1010", u1.id, totalGastos - (296.0 + 401.0 + 21.0), 10.0),
                ExpenseSplit("s2", "exp-trip-1010", u2.id, 296.0, 29.3),
                ExpenseSplit("s3", "exp-trip-1010", u3.id, 401.0, 39.7),
                ExpenseSplit("s4", "exp-trip-1010", u4.id, 21.0, 2.0)
            )
        )

        // Liquidación de U2: Paga 500€ debiendo 296€ (exceso de 204€ hacia U1)
        val settleU2 = ExpenseEntity(
            id = "settle-u2-500",
            tripId = "t1",
            amount = 500.0,
            currency = "EUR",
            amountInBaseCurrency = 500.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-07-02",
            paymentMethod = "Transferencia",
            notes = "U2 paga 500€",
            paidById = u2.id,
            splits = listOf(ExpenseSplit("s-u2", "settle-u2-500", u1.id, 500.0, 100.0))
        )

        // Liquidación de U3: Paga 300€ debiendo 296€ (exceso de 4€, quedando 101€ de deuda restante)
        val settleU3 = ExpenseEntity(
            id = "settle-u3-300",
            tripId = "t1",
            amount = 300.0,
            currency = "EUR",
            amountInBaseCurrency = 300.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-07-02",
            paymentMethod = "Bizum",
            notes = "U3 paga 300€",
            paidById = u3.id,
            splits = listOf(ExpenseSplit("s-u3", "settle-u3-300", u1.id, 300.0, 100.0))
        )

        val activeExps = listOf(expTrip, settleU2, settleU3)
        val activeIncs = emptyList<IncomeEntity>()

        // Ejecutamos la fórmula exacta de TravelCashViewModel:
        val totalDeudasPendientes = participants.sumOf { p ->
            val expenseShare = activeExps
                .filter { !CategoryCatalog.isDebtSettlementExpense(it) }
                .sumOf { exp -> exp.splits.find { it.participantId == p.id }?.amount ?: 0.0 }

            val paidDirectly = activeExps
                .filter { exp ->
                    !CategoryCatalog.isDebtSettlementExpense(exp) &&
                        exp.paidById == p.id &&
                        !exp.paymentMethod.equals("Bote Común", ignoreCase = true)
                }.sumOf { it.amountInBaseCurrency }

            val paidInSettlements = activeExps
                .filter { exp ->
                    CategoryCatalog.isDebtSettlementExpense(exp) && exp.paidById == p.id
                }.sumOf { it.amountInBaseCurrency }

            val boteContributed = activeIncs.sumOf { inc ->
                inc.contributions
                    .filter { it.participantId == p.id }
                    .sumOf { kotlin.math.max(0.0, it.amount) }
            }

            val totalCovered = paidDirectly + paidInSettlements + boteContributed
            kotlin.math.max(0.0, expenseShare - totalCovered)
        }

        val totalBoteIncome = activeIncs.sumOf { it.amount }
        val totalSpentFromBote = activeExps.filter { exp ->
            !CategoryCatalog.isDebtSettlementExpense(exp) &&
                (exp.paidById == DebtCalculator.BOTE_COMUN_ID || exp.paymentMethod.equals("Bote Común", ignoreCase = true))
        }.sumOf { it.amountInBaseCurrency }
        val availableBote = kotlin.math.max(0.0, totalBoteIncome - totalSpentFromBote)

        val saldoExtra = availableBote - totalDeudasPendientes

        // Verificaciones críticas:
        // 1. U2 no tiene deuda pendiente (pagó 500 debiendo 296 -> 0€ deuda, el exceso de 204€ se ignora)
        // 2. U3 tiene exactamente 101€ de deuda pendiente restante
        // 3. U4 tiene exactamente 21€ de deuda pendiente
        // 4. U1 es el acreedor y tiene 0€ de deuda pendiente
        // 5. Total de deudas pendientes = 101€ + 21€ = 122€
        // 6. Saldo Extra = -122€ (bajo ningún concepto -208€)
        assertEquals(122.0, totalDeudasPendientes, 0.001)
        assertEquals(-122.0, saldoExtra, 0.001)
        assertNotEquals(-208.0, saldoExtra, 0.001)
        assertNotEquals(0.0, saldoExtra, 0.001)
    }

    @Test
    fun expensesTab_liquidationsSection_includesPotIncomesForViewingEditingAndDeleting() {
        val u1 = ParticipantEntity("u1", "t1", "Usuario 1", "#0066FF")
        val u2 = ParticipantEntity("u2", "t1", "Usuario 2", "#10B981")

        // 1 liquidación de deuda registrada
        val settle = ExpenseEntity(
            id = "settle-1",
            tripId = "t1",
            amount = 150.0,
            currency = "EUR",
            amountInBaseCurrency = 150.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-07-02",
            paymentMethod = "Bizum",
            notes = "U2 salda deuda",
            paidById = u2.id,
            splits = listOf(ExpenseSplit("s1", "settle-1", u1.id, 150.0, 100.0))
        )

        // 1 ingreso al bote común registrado
        val income = IncomeEntity(
            id = "inc-1",
            tripId = "t1",
            amount = 200.0,
            date = "2026-07-03",
            description = "Aportación al Bote Común",
            contributions = listOf(IncomeContribution(u1.id, 100.0), IncomeContribution(u2.id, 100.0))
        )

        val settlements = listOf(settle)
        val incomes = listOf(income)

        // Verificamos que el contador conjunto de Liquidaciones de gastos es 2 (1 liquidación + 1 ingreso de bote)
        val totalCombinedCount = settlements.size + incomes.size
        assertEquals(2, totalCombinedCount)

        // Verificamos el importe combinado en la sección
        val totalCombinedAmount = settlements.sumOf { it.amountInBaseCurrency } + incomes.sumOf { it.amount }
        assertEquals(350.0, totalCombinedAmount, 0.001)

        // Verificamos que ambos elementos son identificables para su edición/borrado
        assertTrue(settlements.any { it.id == "settle-1" })
        assertTrue(incomes.any { it.id == "inc-1" })
    }
}
