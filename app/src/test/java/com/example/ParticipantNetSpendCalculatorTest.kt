package com.example

import com.example.data.model.CategoryCatalog
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplit
import com.example.data.model.IncomeContribution
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import com.example.domain.CommonPotMovement
import com.example.domain.CommonPotMovementType
import com.example.domain.DebtCalculator
import com.example.domain.MovementStatus
import com.example.domain.ParticipantLedgerEntryType
import com.example.domain.ParticipantNetSpendCalculator
import com.example.domain.ParticipantTransfer
import com.example.ui.theme.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParticipantNetSpendCalculatorTest {

    private val pepe = ParticipantEntity("pepe", "t1", "Pepe", "#0066FF")
    private val juan = ParticipantEntity("juan", "t1", "Juan", "#10B981")
    private val ana = ParticipantEntity("ana", "t1", "Ana", "#F59E0B")

    @Test
    fun transferBetweenParticipants_adjustsBothNetSpendsAndDoesNotInflateRealTotal() {
        val hotel = ExpenseEntity(
            id = "e1",
            tripId = "t1",
            amount = 100.0,
            currency = "EUR",
            amountInBaseCurrency = 100.0,
            exchangeRate = 1.0,
            category = "Alojamiento",
            date = "2026-06-10",
            paymentMethod = "Tarjeta",
            notes = "Hotel",
            placeName = "Hotel Central",
            paidById = juan.id,
            splits = listOf(
                ExpenseSplit("s1", "e1", pepe.id, 50.0, 50.0),
                ExpenseSplit("s2", "e1", juan.id, 50.0, 50.0)
            )
        )

        val settlement = ExpenseEntity(
            id = "settle-001",
            tripId = "t1",
            amount = 50.0,
            currency = "EUR",
            amountInBaseCurrency = 50.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-06-11",
            paymentMethod = "Bizum",
            notes = "Saldado de cuenta",
            placeName = "Liquidación de deuda: Pepe → Juan",
            paidById = pepe.id,
            splits = listOf(
                ExpenseSplit("s3", "settle-001", juan.id, 50.0, 100.0)
            )
        )

        val report = ParticipantNetSpendCalculator.calculate(
            participants = listOf(pepe, juan),
            expenses = listOf(hotel, settlement),
            incomes = emptyList()
        )

        // El gasto real del viaje sigue siendo 100 €, sin sumarle la liquidación de 50 €
        assertEquals(100.0, report.realTotalSpent, 0.01)
        assertEquals(1, report.realExpenseCount)
        assertEquals(50.0, report.equalShareAverage, 0.01)
        assertTrue(report.allEqual)
        assertTrue(report.participantsWithDifference.isEmpty())

        val pepeSummary = report.participantSummaries.first { it.participant.id == pepe.id }
        val juanSummary = report.participantSummaries.first { it.participant.id == juan.id }

        // Pepe: 0 pagado + 50 enviado = 50 € neto; etiqueta "Pago a Juan"
        assertEquals(0.0, pepeSummary.realExpensesPaid, 0.01)
        assertEquals(50.0, pepeSummary.transfersSent, 0.01)
        assertEquals(0.0, pepeSummary.transfersReceived, 0.01)
        assertEquals(50.0, pepeSummary.netSpend, 0.01)
        assertEquals(0.0, pepeSummary.differenceFromAverage, 0.01)
        val pepeTransferEntry = pepeSummary.entries.first { it.type == ParticipantLedgerEntryType.TRANSFER_SENT }
        assertEquals("Pago a Juan", pepeTransferEntry.displayLabel(AppLanguage.ES))
        assertEquals(50.0, pepeTransferEntry.signedAmount, 0.01)

        // Juan: 100 pagado - 50 recibido = 50 € neto; etiqueta "Recibido de Pepe"
        assertEquals(100.0, juanSummary.realExpensesPaid, 0.01)
        assertEquals(0.0, juanSummary.transfersSent, 0.01)
        assertEquals(50.0, juanSummary.transfersReceived, 0.01)
        assertEquals(50.0, juanSummary.netSpend, 0.01)
        assertEquals(0.0, juanSummary.differenceFromAverage, 0.01)
        val juanTransferEntry = juanSummary.entries.first { it.type == ParticipantLedgerEntryType.TRANSFER_RECEIVED }
        assertEquals("Recibido de Pepe", juanTransferEntry.displayLabel(AppLanguage.ES))
        assertEquals(-50.0, juanTransferEntry.signedAmount, 0.01)
    }

    @Test
    fun commonPotContributionsReceiptsAndDirectSpend_produceExactEqualNetSpendWithoutDuplication() {
        // Pepe y Juan aportan 40 € cada uno al bote común (80 € total en bote)
        val potIncome = IncomeEntity(
            id = "inc-1",
            tripId = "t1",
            amount = 80.0,
            date = "2026-06-09",
            description = "Bote inicial",
            basePerPerson = 40.0,
            contributions = listOf(
                IncomeContribution(pepe.id, 40.0, 0.0),
                IncomeContribution(juan.id, 40.0, 0.0)
            )
        )

        // El bote paga directamente un gasto real de 50 € (quedan 30 € en bote)
        val potDinner = ExpenseEntity(
            id = "e-bote",
            tripId = "t1",
            amount = 50.0,
            currency = "EUR",
            amountInBaseCurrency = 50.0,
            exchangeRate = 1.0,
            category = "Restaurantes y Comida",
            date = "2026-06-10",
            paymentMethod = "Bote Común",
            notes = "Cena",
            placeName = "Restaurante",
            paidById = DebtCalculator.BOTE_COMUN_ID,
            splits = listOf(
                ExpenseSplit("sb1", "e-bote", pepe.id, 25.0, 50.0),
                ExpenseSplit("sb2", "e-bote", juan.id, 25.0, 50.0)
            )
        )

        // Juan paga de su bolsillo un gasto real de 60 €
        val juanTaxi = ExpenseEntity(
            id = "e-juan",
            tripId = "t1",
            amount = 60.0,
            currency = "EUR",
            amountInBaseCurrency = 60.0,
            exchangeRate = 1.0,
            category = "Taxi",
            date = "2026-06-10",
            paymentMethod = "Efectivo",
            notes = "Taxi aeropuerto",
            placeName = "Aeropuerto",
            paidById = juan.id,
            splits = listOf(
                ExpenseSplit("sj1", "e-juan", pepe.id, 30.0, 50.0),
                ExpenseSplit("sj2", "e-juan", juan.id, 30.0, 50.0)
            )
        )

        // Juan recibe los 30 € restantes del bote común
        val potToJuan = ExpenseEntity(
            id = "settle-bote-1",
            tripId = "t1",
            amount = 30.0,
            currency = "EUR",
            amountInBaseCurrency = 30.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-06-11",
            paymentMethod = "Bote Común",
            notes = "Reembolso desde bote",
            placeName = "Liquidación desde Bote Común a Juan",
            paidById = DebtCalculator.BOTE_COMUN_ID,
            splits = listOf(
                ExpenseSplit("sp1", "settle-bote-1", juan.id, 30.0, 100.0)
            )
        )

        // Pepe paga 15 € a Juan para dejar las cuentas iguales (55 € cada uno)
        val pepeToJuan = ExpenseEntity(
            id = "settle-p2j",
            tripId = "t1",
            amount = 15.0,
            currency = "EUR",
            amountInBaseCurrency = 15.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-06-11",
            paymentMethod = "Bizum",
            notes = "Ajuste final",
            placeName = "Liquidación de deuda: Pepe → Juan",
            paidById = pepe.id,
            splits = listOf(
                ExpenseSplit("sp2", "settle-p2j", juan.id, 15.0, 100.0)
            )
        )

        val report = ParticipantNetSpendCalculator.calculate(
            participants = listOf(pepe, juan),
            expenses = listOf(potDinner, juanTaxi, potToJuan, pepeToJuan),
            incomes = listOf(potIncome)
        )

        // Gasto total real = 50 (bote) + 60 (Juan) = 110 €; media = 55 €; saldo del bote = 0 €
        assertEquals(110.0, report.realTotalSpent, 0.01)
        assertEquals(2, report.realExpenseCount)
        assertEquals(55.0, report.equalShareAverage, 0.01)
        assertEquals(0.0, report.commonPotBalance, 0.01)
        assertTrue(report.allEqual)

        val pepeSummary = report.participantSummaries.first { it.participant.id == pepe.id }
        val juanSummary = report.participantSummaries.first { it.participant.id == juan.id }

        // Pepe: 0 + 40 (Aportado al bote) + 15 (Pago a Juan) = 55 €
        assertEquals(55.0, pepeSummary.netSpend, 0.01)
        assertTrue(pepeSummary.entries.any { it.type == ParticipantLedgerEntryType.POT_CONTRIBUTION && it.displayLabel(AppLanguage.ES) == "Aportado al bote" && it.signedAmount == 40.0 })
        assertTrue(pepeSummary.entries.any { it.type == ParticipantLedgerEntryType.TRANSFER_SENT && it.displayLabel(AppLanguage.ES) == "Pago a Juan" && it.signedAmount == 15.0 })

        // Juan: 60 (Taxi) + 40 (Aportado al bote) - 30 (Recibido del bote) - 15 (Recibido de Pepe) = 55 €
        assertEquals(55.0, juanSummary.netSpend, 0.01)
        assertTrue(juanSummary.entries.any { it.type == ParticipantLedgerEntryType.POT_CONTRIBUTION && it.displayLabel(AppLanguage.ES) == "Aportado al bote" && it.signedAmount == 40.0 })
        assertTrue(juanSummary.entries.any { it.type == ParticipantLedgerEntryType.POT_RECEIVED && it.displayLabel(AppLanguage.ES) == "Recibido del bote" && it.signedAmount == -30.0 })
        assertTrue(juanSummary.entries.any { it.type == ParticipantLedgerEntryType.TRANSFER_RECEIVED && it.displayLabel(AppLanguage.ES) == "Recibido de Pepe" && it.signedAmount == -15.0 })
    }

    @Test
    fun unequalSpends_reportsVisibleDifferencesAgainstAverage() {
        val hotel = ExpenseEntity(
            id = "e1",
            tripId = "t1",
            amount = 90.0,
            currency = "EUR",
            amountInBaseCurrency = 90.0,
            exchangeRate = 1.0,
            category = "Alojamiento",
            date = "2026-06-10",
            paymentMethod = "Tarjeta",
            notes = "Hotel",
            paidById = juan.id,
            splits = listOf(
                ExpenseSplit("s1", "e1", pepe.id, 30.0, 33.33),
                ExpenseSplit("s2", "e1", juan.id, 30.0, 33.33),
                ExpenseSplit("s3", "e1", ana.id, 30.0, 33.34)
            )
        )

        val report = ParticipantNetSpendCalculator.calculate(
            participants = listOf(pepe, juan, ana),
            expenses = listOf(hotel),
            incomes = emptyList()
        )

        assertEquals(90.0, report.realTotalSpent, 0.01)
        assertEquals(30.0, report.equalShareAverage, 0.01)
        assertFalse(report.allEqual)
        assertEquals(3, report.participantsWithDifference.size)

        val juanSummary = report.participantSummaries.first { it.participant.id == juan.id }
        val pepeSummary = report.participantSummaries.first { it.participant.id == pepe.id }
        assertEquals(60.0, juanSummary.differenceFromAverage, 0.01)
        assertEquals(-30.0, pepeSummary.differenceFromAverage, 0.01)
    }

    @Test
    fun legacyGeneralSettlementAndPendingExtraMovements_handledAccurately() {
        // Liquidación heredada guardada anteriormente con categoría "General" pero id "settle-..."
        val legacySettlement = ExpenseEntity(
            id = "settle-legacy",
            tripId = "t1",
            amount = 25.0,
            currency = "EUR",
            amountInBaseCurrency = 25.0,
            exchangeRate = 1.0,
            category = "General",
            date = "2026-06-11",
            paymentMethod = "Efectivo",
            notes = "Liquidación",
            paidById = pepe.id,
            splits = listOf(ExpenseSplit("s-leg", "settle-legacy", juan.id, 25.0, 100.0))
        )

        // Movimiento explícito pendiente (no debe alterar todavía el gasto neto)
        val pendingTransfer = ParticipantTransfer(
            id = "tx-pending",
            fromParticipantId = pepe.id,
            toParticipantId = juan.id,
            amount = 100.0,
            date = "2026-06-12",
            description = "Pendiente de confirmar",
            status = MovementStatus.PENDING
        )

        // Movimiento explícito completado con el bote
        val potContrib = CommonPotMovement(
            id = "pot-extra-1",
            participantId = pepe.id,
            type = CommonPotMovementType.CONTRIBUTION,
            amount = 40.0,
            date = "2026-06-12",
            description = "Aportación extra",
            status = MovementStatus.COMPLETED
        )

        val report = ParticipantNetSpendCalculator.calculate(
            participants = listOf(pepe, juan),
            expenses = listOf(legacySettlement),
            incomes = emptyList(),
            extraTransfers = listOf(pendingTransfer),
            extraPotMovements = listOf(potContrib)
        )

        assertEquals(0.0, report.realTotalSpent, 0.01)
        assertEquals(40.0, report.commonPotBalance, 0.01)

        val pepeSummary = report.participantSummaries.first { it.participant.id == pepe.id }
        val juanSummary = report.participantSummaries.first { it.participant.id == juan.id }

        // Pepe: +25 (Pago a Juan) + 40 (Aportado al bote) = 65 € (el pendiente de 100 € no suma)
        assertEquals(65.0, pepeSummary.netSpend, 0.01)
        // Juan: -25 (Recibido de Pepe) = -25 €
        assertEquals(-25.0, juanSummary.netSpend, 0.01)
    }

    @Test
    fun allNineLanguages_translateDebtSettlementAndLedgerLabelsCorrectly() {
        val unnamedHotel = ExpenseEntity(
            id = "e-unnamed",
            tripId = "t1",
            amount = 80.0,
            currency = "EUR",
            amountInBaseCurrency = 80.0,
            exchangeRate = 1.0,
            category = "Alojamiento",
            date = "2026-06-10",
            paymentMethod = "Tarjeta",
            notes = "",
            placeName = "",
            paidById = juan.id,
            splits = listOf(
                ExpenseSplit("su1", "e-unnamed", pepe.id, 40.0, 50.0),
                ExpenseSplit("su2", "e-unnamed", juan.id, 40.0, 50.0)
            )
        )
        val transfer = ParticipantTransfer("tx-1", pepe.id, juan.id, 40.0, "2026-06-11")
        val potIn = CommonPotMovement("pm-1", pepe.id, CommonPotMovementType.CONTRIBUTION, 20.0, "2026-06-11")
        val potOut = CommonPotMovement("pm-2", juan.id, CommonPotMovementType.RECEIVED_FROM_POT, 20.0, "2026-06-11")

        val report = ParticipantNetSpendCalculator.calculate(
            participants = listOf(pepe, juan),
            expenses = listOf(unnamedHotel),
            incomes = emptyList(),
            extraTransfers = listOf(transfer),
            extraPotMovements = listOf(potIn, potOut)
        )

        val pepeEntries = report.participantSummaries.first { it.participant.id == pepe.id }.entries
        val juanEntries = report.participantSummaries.first { it.participant.id == juan.id }.entries

        val sentEntry = pepeEntries.first { it.type == ParticipantLedgerEntryType.TRANSFER_SENT }
        val recvEntry = juanEntries.first { it.type == ParticipantLedgerEntryType.TRANSFER_RECEIVED }
        val potInEntry = pepeEntries.first { it.type == ParticipantLedgerEntryType.POT_CONTRIBUTION }
        val potOutEntry = juanEntries.first { it.type == ParticipantLedgerEntryType.POT_RECEIVED }
        val unnamedExpenseEntry = juanEntries.first { it.type == ParticipantLedgerEntryType.REAL_EXPENSE_PAID }

        AppLanguage.entries.forEach { lang ->
            val translatedDebtCat = CategoryCatalog.displayName(CategoryCatalog.DEBT_SETTLEMENT_CATEGORY, lang)
            assertTrue("Debt settlement category must be non-blank for $lang", translatedDebtCat.isNotBlank())
            assertTrue("isDebtSettlementCategory must recognize $translatedDebtCat ($lang)", CategoryCatalog.isDebtSettlementCategory(translatedDebtCat))

            val sentLabel = sentEntry.displayLabel(lang)
            val recvLabel = recvEntry.displayLabel(lang)
            assertTrue("Sent label must include Juan in $lang", sentLabel.contains("Juan"))
            assertTrue("Recv label must include Pepe in $lang", recvLabel.contains("Pepe"))
            assertTrue("Pot contribution label non-blank in $lang", potInEntry.displayLabel(lang).isNotBlank())
            assertTrue("Pot received label non-blank in $lang", potOutEntry.displayLabel(lang).isNotBlank())

            // Gasto sin lugar ni notas debe traducir la categoría de respaldo al idioma activo
            val expectedHotelCat = CategoryCatalog.displayName("Alojamiento", lang)
            assertEquals(expectedHotelCat, unnamedExpenseEntry.displayLabel(lang))
        }
    }

    @Test
    fun paidAssumedAndReserveBalance_respectsCustomSplitsInsteadOfSimpleAverage() {
        // Ejemplo real del usuario:
        // Total real del viaje = 700 € (media simple sería 350 €, pero el reparto es desigual: Pepe asume 296 €, Juan asume 404 €)
        // Pepe entrega 500 € al administrador (Juan) / bote -> Pagado = 500 €, Asumido = 296 €, Reserva/Balance = +204 € a favor
        val tripExpense = ExpenseEntity(
            id = "e-custom-split",
            tripId = "t1",
            amount = 700.0,
            currency = "EUR",
            amountInBaseCurrency = 700.0,
            exchangeRate = 1.0,
            category = "Alojamiento",
            date = "2026-06-15",
            paymentMethod = "Tarjeta",
            notes = "Paquete viaje",
            placeName = "Agencia",
            paidById = juan.id,
            splits = listOf(
                ExpenseSplit("sc1", "e-custom-split", pepe.id, 296.0, 42.29),
                ExpenseSplit("sc2", "e-custom-split", juan.id, 404.0, 57.71)
            )
        )

        val pepePaymentToAdmin = ExpenseEntity(
            id = "settle-500",
            tripId = "t1",
            amount = 500.0,
            currency = "EUR",
            amountInBaseCurrency = 500.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-06-16",
            paymentMethod = "Transferencia",
            notes = "Entrega al administrador",
            placeName = "Liquidación de deuda: Pepe → Juan",
            paidById = pepe.id,
            splits = listOf(
                ExpenseSplit("sc3", "settle-500", juan.id, 500.0, 100.0)
            )
        )

        val report = ParticipantNetSpendCalculator.calculate(
            participants = listOf(pepe, juan),
            expenses = listOf(tripExpense, pepePaymentToAdmin),
            incomes = emptyList()
        )

        assertEquals(700.0, report.realTotalSpent, 0.01)

        val pepeSummary = report.participantSummaries.first { it.participant.id == pepe.id }
        val juanSummary = report.participantSummaries.first { it.participant.id == juan.id }

        // Pepe: Pagado = 500 €, Asumido = 296 € (respetando el split exacto, NO 350 €), Reserva = +204 €
        assertEquals(500.0, pepeSummary.netSpend, 0.01)
        assertEquals(296.0, pepeSummary.assumedSpend, 0.01)
        assertEquals(204.0, pepeSummary.balance, 0.01)
        assertEquals(pepeSummary.netSpend - pepeSummary.assumedSpend, pepeSummary.balance, 0.01)

        // Juan: Pagado = 700 - 296 = 404 € (recupera los 296 € de Pepe, los 204 € de exceso son bote personal de Pepe sin crear gasto a Juan), Asumido = 404 €, Balance = 0 € (en paz)
        assertEquals(404.0, juanSummary.netSpend, 0.01)
        assertEquals(404.0, juanSummary.assumedSpend, 0.01)
        assertEquals(0.0, juanSummary.balance, 0.01)
        assertEquals(juanSummary.netSpend - juanSummary.assumedSpend, juanSummary.balance, 0.01)
    }

    @Test
    fun multipleTransfersFromSameSender_groupedCorrectlyAndCappedAtRealDebt() {
        val tripExpense = ExpenseEntity(
            id = "e-hotel",
            tripId = "t1",
            amount = 200.0,
            currency = "EUR",
            amountInBaseCurrency = 200.0,
            exchangeRate = 1.0,
            category = "Alojamiento",
            date = "2026-06-15",
            paymentMethod = "Tarjeta",
            notes = "Hotel",
            placeName = "Hotel",
            paidById = juan.id,
            splits = listOf(
                ExpenseSplit("s1", "e-hotel", pepe.id, 100.0, 50.0),
                ExpenseSplit("s2", "e-hotel", juan.id, 100.0, 50.0)
            )
        )

        // Pepe le hace dos pagos a Juan: uno de 60€ y otro de 70€ (total 130€, debía 100€ -> exceso 30€)
        val transfer1 = ExpenseEntity(
            id = "tx1",
            tripId = "t1",
            amount = 60.0,
            currency = "EUR",
            amountInBaseCurrency = 60.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-06-16",
            paymentMethod = "Bizum",
            notes = "Pago 1",
            placeName = "Liquidación",
            paidById = pepe.id,
            splits = listOf(ExpenseSplit("stx1", "tx1", juan.id, 60.0, 100.0))
        )
        val transfer2 = ExpenseEntity(
            id = "tx2",
            tripId = "t1",
            amount = 70.0,
            currency = "EUR",
            amountInBaseCurrency = 70.0,
            exchangeRate = 1.0,
            category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
            date = "2026-06-17",
            paymentMethod = "Bizum",
            notes = "Pago 2",
            placeName = "Liquidación",
            paidById = pepe.id,
            splits = listOf(ExpenseSplit("stx2", "tx2", juan.id, 70.0, 100.0))
        )

        val report = ParticipantNetSpendCalculator.calculate(
            participants = listOf(pepe, juan),
            expenses = listOf(tripExpense, transfer1, transfer2),
            incomes = emptyList()
        )

        val pepeSummary = report.participantSummaries.first { it.participant.id == pepe.id }
        val juanSummary = report.participantSummaries.first { it.participant.id == juan.id }

        // Pepe: Pagó 130€ en liquidaciones, Asumido = 100€ -> Saldo extra = +30€
        assertEquals(130.0, pepeSummary.netSpend, 0.01)
        assertEquals(100.0, pepeSummary.assumedSpend, 0.01)
        assertEquals(30.0, pepeSummary.balance, 0.01)

        // Juan: Pagó 200€ - 100€ (deuda real reembolsada agrupada) = 100€ netSpend, Asumido = 100€ -> Balance = 0.00€
        assertEquals(100.0, juanSummary.netSpend, 0.01)
        assertEquals(100.0, juanSummary.assumedSpend, 0.01)
        assertEquals(0.0, juanSummary.balance, 0.01)
    }

    @Test
    fun directTripExpensePayment_yieldsExtraBalanceForPayerAndDebtForOthers() {
        // Juan paga una cena de 300€ para Pepe, Juan y Ana (100€ cada uno)
        val dinner = ExpenseEntity(
            id = "e-dinner",
            tripId = "t1",
            amount = 300.0,
            currency = "EUR",
            amountInBaseCurrency = 300.0,
            exchangeRate = 1.0,
            category = "Restaurantes",
            date = "2026-06-15",
            paymentMethod = "Tarjeta",
            notes = "Cena grupal",
            placeName = "Restaurante",
            paidById = juan.id,
            splits = listOf(
                ExpenseSplit("d1", "e-dinner", pepe.id, 100.0, 33.33),
                ExpenseSplit("d2", "e-dinner", juan.id, 100.0, 33.34),
                ExpenseSplit("d3", "e-dinner", ana.id, 100.0, 33.33)
            )
        )

        val report = ParticipantNetSpendCalculator.calculate(
            participants = listOf(pepe, juan, ana),
            expenses = listOf(dinner),
            incomes = emptyList()
        )

        val juanSummary = report.participantSummaries.first { it.participant.id == juan.id }
        val pepeSummary = report.participantSummaries.first { it.participant.id == pepe.id }
        val anaSummary = report.participantSummaries.first { it.participant.id == ana.id }

        // Juan: Pagado = 300€, Asumido = 100€, Saldo extra = +200€
        assertEquals(300.0, juanSummary.netSpend, 0.01)
        assertEquals(100.0, juanSummary.assumedSpend, 0.01)
        assertEquals(200.0, juanSummary.balance, 0.01)

        // Pepe: Pagado = 0€, Asumido = 100€, Debe = -100€
        assertEquals(0.0, pepeSummary.netSpend, 0.01)
        assertEquals(100.0, pepeSummary.assumedSpend, 0.01)
        assertEquals(-100.0, pepeSummary.balance, 0.01)

        // Ana: Pagado = 0€, Asumido = 100€, Debe = -100€
        assertEquals(0.0, anaSummary.netSpend, 0.01)
        assertEquals(100.0, anaSummary.assumedSpend, 0.01)
        assertEquals(-100.0, anaSummary.balance, 0.01)
    }
}
