package com.example.domain

import com.example.data.model.CategoryCatalog
import com.example.data.model.ExpenseEntity
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

data class ParticipantBalance(
    val participantId: String,
    val participantName: String,
    val avatarColor: String,
    val participantPhotoUri: String? = null,
    val totalExpensesPaid: Double,            // Pagado de su bolsillo (gastos reales + liquidaciones enviadas)
    val totalExpensesOwed: Double,            // Parte de gastos que le correspondía asumir (SÓLO gastos reales del viaje)
    val totalBoteContributed: Double,         // Dinero en efectivo que ha metido en el bote común
    val totalDebtDiscountCompensated: Double, // Descuentos aplicados al poner bote porque se le debía dinero
    val totalDebtSettlementsSent: Double = 0.0,     // Total enviado en liquidaciones a otros
    val totalDebtSettlementsReceived: Double = 0.0, // Total recibido que saldó deudas reales adelantadas
    val netBalance: Double                    // Balance neto
)

data class DebtTransaction(
    val fromParticipantId: String, // ID de participante o "bote-comun"
    val fromName: String,
    val toParticipantId: String,
    val toName: String,
    val amount: Double,
    val isFromBote: Boolean
)

data class DebtCalculationResult(
    val participantBalances: List<ParticipantBalance>,
    val transactions: List<DebtTransaction>,
    val totalBoteCollected: Double,
    val totalSpentFromBoteDirectly: Double,
    val availableBote: Double
)

object DebtCalculator {

    const val BOTE_COMUN_ID = "bote-comun"
    const val BOTE_COMUN_NAME = "Bote Común"

    private fun round2(value: Double): Double = (value * 100.0).roundToLong() / 100.0

    /**
     * Calcula los balances individuales y las transacciones de liquidación de cuentas
     * siguiendo las reglas exactas solicitadas:
     *
     * 1. Lo que le toca asumir a cada uno (totalExpensesOwed) es SÓLO la suma de sus partes proporcionales
     *    en los gastos reales del viaje (sin contar si un usuario le ha dado dinero a otro en liquidaciones).
     * 2. Si un usuario paga más de lo que debe (ej. debía 296€ y da 500€):
     *    - El receptor coge los 296€ para saldar la deuda por los gastos que adelantó para el pagador.
     *    - Al receptor NO se le crea ningún gasto por los 204€ restantes ni entra en saldo negativo.
     *    - El pagador tiene +204€ en positivo como su bote personal / saldo a favor para futuros gastos.
     * 3. A medida que el receptor adelante nuevos gastos para el pagador, se van consumiendo de ese bote personal
     *    de 204€ sin generar deuda hasta que se agote.
     */
    fun calculateDebtBalances(
        participants: List<ParticipantEntity>,
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>
    ): DebtCalculationResult {
        if (participants.isEmpty()) {
            return DebtCalculationResult(
                participantBalances = emptyList(),
                transactions = emptyList(),
                totalBoteCollected = 0.0,
                totalSpentFromBoteDirectly = 0.0,
                availableBote = 0.0
            )
        }

        val realExpenses = expenses.filter { !CategoryCatalog.isDebtSettlementExpense(it) }
        val settlementExpenses = expenses.filter { CategoryCatalog.isDebtSettlementExpense(it) }

        // Bote Común
        val totalBoteCollected = incomes.sumOf { it.amount }
        val totalSpentFromBoteDirectly = expenses.filter {
            it.paidById == BOTE_COMUN_ID || it.paymentMethod.equals("Bote Común", ignoreCase = true)
        }.sumOf { it.amountInBaseCurrency }
        var availableBote = max(0.0, totalBoteCollected - totalSpentFromBoteDirectly)

        // Análisis bilateral de liquidaciones vs gastos reales entre cada par de participantes
        val realCostPaidByAforB = mutableMapOf<Pair<String, String>, Double>()
        for (exp in realExpenses) {
            val payerId = exp.paidById
            if (payerId.isNotBlank() && !exp.paymentMethod.equals("Bote Común", ignoreCase = true)) {
                if (exp.splits.isNotEmpty()) {
                    for (split in exp.splits) {
                        if (split.participantId != payerId) {
                            val key = Pair(payerId, split.participantId)
                            realCostPaidByAforB[key] = (realCostPaidByAforB[key] ?: 0.0) + split.amount
                        }
                    }
                } else if (participants.isNotEmpty()) {
                    val equalShare = exp.amountInBaseCurrency / participants.size
                    for (p in participants) {
                        if (p.id != payerId) {
                            val key = Pair(payerId, p.id)
                            realCostPaidByAforB[key] = (realCostPaidByAforB[key] ?: 0.0) + equalShare
                        }
                    }
                }
            }
        }

        val settlementsSentFromBtoA = mutableMapOf<Pair<String, String>, Double>()
        for (settle in settlementExpenses) {
            val fromId = settle.paidById
            for (split in settle.splits) {
                val toId = split.participantId
                if (fromId.isNotBlank() && toId.isNotBlank() && fromId != toId) {
                    val key = Pair(fromId, toId)
                    settlementsSentFromBtoA[key] = (settlementsSentFromBtoA[key] ?: 0.0) + split.amount
                }
            }
        }

        val debtReimbursedToHolder = mutableMapOf<Pair<String, String>, Double>()

        for (pA in participants) {
            for (pB in participants) {
                if (pA.id == pB.id) continue
                val costAforB = realCostPaidByAforB[Pair(pA.id, pB.id)] ?: 0.0
                val costBforA = realCostPaidByAforB[Pair(pB.id, pA.id)] ?: 0.0
                val realDebtBtoA = max(0.0, costAforB - costBforA)

                val sentBtoA = settlementsSentFromBtoA[Pair(pB.id, pA.id)] ?: 0.0
                val settled = min(sentBtoA, realDebtBtoA)

                debtReimbursedToHolder[Pair(pB.id, pA.id)] = round2(settled)
            }
        }

        val balances = participants.map { participant ->
            // 1. Gastos reales que pagó de su propio bolsillo (excluyendo Bote Común)
            val realPaidOutOfPocket = realExpenses.filter {
                it.paidById == participant.id && !it.paymentMethod.equals("Bote Común", ignoreCase = true)
            }.sumOf { it.amountInBaseCurrency }

            // 2. Liquidaciones enviadas directamente a otros participantes
            val settlementsSent = settlementExpenses.filter {
                it.paidById == participant.id && !it.paymentMethod.equals("Bote Común", ignoreCase = true)
            }.sumOf { it.amountInBaseCurrency }

            // 3. Liquidaciones recibidas que saldaron gastos adelantados para otros (reembolsos)
            val debtReimbursementsReceived = participants.filter { it.id != participant.id }.sumOf { other ->
                debtReimbursedToHolder[Pair(other.id, participant.id)] ?: 0.0
            }

            // 4. Lo que le tocaba asumir a cada uno: SÓLO gastos reales del viaje
            val realOwed = realExpenses.sumOf { expense ->
                val splitForPerson = expense.splits.find { it.participantId == participant.id }
                splitForPerson?.amount ?: if (expense.splits.isEmpty() && participants.isNotEmpty()) (expense.amountInBaseCurrency / participants.size) else 0.0
            }

            // 5. Bote Común aportado y compensado
            val boteContributed = incomes.sumOf { income ->
                income.contributions
                    .filter { it.participantId == participant.id }
                    .sumOf { max(0.0, it.amount) }
            }
            val debtDiscountCompensated = incomes.sumOf { income ->
                income.contributions
                    .filter { it.participantId == participant.id }
                    .sumOf { max(0.0, it.debtDiscount ?: 0.0) }
            }

            // Total desembolsado físicamente de su bolsillo
            val totalPaid = realPaidOutOfPocket + settlementsSent

            // Balance neto
            val net = (totalPaid - debtReimbursementsReceived - debtDiscountCompensated + boteContributed) - realOwed

            ParticipantBalance(
                participantId = participant.id,
                participantName = participant.name,
                avatarColor = participant.avatarColor,
                participantPhotoUri = participant.photoUri,
                totalExpensesPaid = round2(totalPaid),
                totalExpensesOwed = round2(realOwed),
                totalBoteContributed = round2(boteContributed),
                totalDebtDiscountCompensated = round2(debtDiscountCompensated),
                totalDebtSettlementsSent = round2(settlementsSent),
                totalDebtSettlementsReceived = round2(debtReimbursementsReceived),
                netBalance = round2(net)
            )
        }

        val transactions = mutableListOf<DebtTransaction>()

        // PASO 1: Compensación desde el Bote Común a participantes acreedores
        val creditorRemaining = mutableMapOf<String, Double>()
        balances.forEach { b ->
            val realPaid = realExpenses.filter {
                it.paidById == b.participantId && !it.paymentMethod.equals("Bote Común", ignoreCase = true)
            }.sumOf { it.amountInBaseCurrency }
            val creditoPorGastos = max(0.0, (realPaid - b.totalDebtSettlementsReceived - b.totalDebtDiscountCompensated) - b.totalExpensesOwed)
            val eligible = min(max(0.0, b.netBalance), creditoPorGastos)
            if (eligible > 0.009) {
                creditorRemaining[b.participantId] = eligible
            }
        }

        for (b in balances) {
            if (availableBote <= 0.009) break
            val currentCreditorNeed = creditorRemaining[b.participantId] ?: 0.0
            if (currentCreditorNeed > 0.009) {
                val transferAmount = round2(min(availableBote, currentCreditorNeed))
                if (transferAmount > 0.009) {
                    transactions.add(
                        DebtTransaction(
                            fromParticipantId = BOTE_COMUN_ID,
                            fromName = BOTE_COMUN_NAME,
                            toParticipantId = b.participantId,
                            toName = b.participantName,
                            amount = transferAmount,
                            isFromBote = true
                        )
                    )
                    availableBote = max(0.0, availableBote - transferAmount)
                    creditorRemaining[b.participantId] = max(0.0, currentCreditorNeed - transferAmount)
                }
            }
        }

        // PASO 2: Deudas entre participantes cruzando deudores (netBalance < 0) con acreedores
        val debtors = balances
            .filter { it.netBalance < -0.009 }
            .map { it.participantId to abs(it.netBalance) }
            .toMutableList()

        val creditors = balances
            .filter { (creditorRemaining[it.participantId] ?: 0.0) > 0.009 }
            .map { it.participantId to (creditorRemaining[it.participantId] ?: 0.0) }
            .toMutableList()

        var dIdx = 0
        var cIdx = 0
        while (dIdx < debtors.size && cIdx < creditors.size) {
            val (debtorId, debtAmt) = debtors[dIdx]
            val (creditorId, credAmt) = creditors[cIdx]

            val settled = round2(min(debtAmt, credAmt))
            if (settled > 0.009) {
                val debtorName = balances.find { it.participantId == debtorId }?.participantName ?: "Participante"
                val creditorName = balances.find { it.participantId == creditorId }?.participantName ?: "Participante"
                transactions.add(
                    DebtTransaction(
                        fromParticipantId = debtorId,
                        fromName = debtorName,
                        toParticipantId = creditorId,
                        toName = creditorName,
                        amount = settled,
                        isFromBote = false
                    )
                )
            }

            val newDebt = debtAmt - settled
            val newCred = credAmt - settled
            debtors[dIdx] = debtorId to newDebt
            creditors[cIdx] = creditorId to newCred

            if (newDebt <= 0.009) dIdx++
            if (newCred <= 0.009) cIdx++
        }

        return DebtCalculationResult(
            participantBalances = balances,
            transactions = transactions,
            totalBoteCollected = round2(totalBoteCollected),
            totalSpentFromBoteDirectly = round2(totalSpentFromBoteDirectly),
            availableBote = round2(max(0.0, totalBoteCollected - totalSpentFromBoteDirectly))
        )
    }

    /**
     * Calcula cuánto se le debe actualmente a cada participante ANTES de registrar un ingreso.
     */
    fun calculateCurrentDebtInFavorMap(
        participants: List<ParticipantEntity>,
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>,
        excludeIncomeId: String? = null
    ): Map<String, Double> {
        val filteredIncomes = if (excludeIncomeId != null) {
            incomes.filter { it.id != excludeIncomeId }
        } else {
            incomes
        }
        val result = calculateDebtBalances(participants, expenses, filteredIncomes)
        return result.participantBalances.associate { balance ->
            val deudaAFavor = max(0.0, balance.netBalance)
            balance.participantId to round2(deudaAFavor)
        }
    }
}
