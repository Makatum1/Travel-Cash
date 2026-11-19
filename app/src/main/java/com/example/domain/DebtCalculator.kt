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
    val netBalance: Double,                   // Balance neto
    val personalPotExcess: Double = 0.0,      // Exceso generado por liquidaciones (bote personal por pago de más)
    val otherUsersExcess: Double = 0.0,       // Saldo acumulado de otros usuarios (suma de excesos de liquidaciones de los demás)
    val cashFlowBalance: Double = 0.0         // Flujo de caja: cobrosLiquidaciones - pagosGastos
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
    val availableBote: Double,
    val totalSettlementExcess: Double = 0.0
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

        val isFromBote: (ExpenseEntity) -> Boolean = { exp ->
            exp.paidById == BOTE_COMUN_ID || exp.paymentMethod.equals("Bote Común", ignoreCase = true)
        }
        val realExpenses = expenses.filter { !CategoryCatalog.isDebtSettlementExpense(it) && !isFromBote(it) }
        val settlementExpenses = expenses.filter { CategoryCatalog.isDebtSettlementExpense(it) }

        // Bote Común
        val totalBoteCollected = incomes.sumOf { it.amount }
        val totalSpentFromBoteDirectly = expenses.filter {
            !CategoryCatalog.isDebtSettlementExpense(it) && isFromBote(it)
        }.sumOf { it.amountInBaseCurrency }
        val availableBote = max(0.0, totalBoteCollected - totalSpentFromBoteDirectly)

        // Análisis bilateral de liquidaciones vs gastos reales entre cada par de participantes
        val realCostPaidByAforB = mutableMapOf<Pair<String, String>, Double>()
        for (exp in realExpenses) {
            val payerId = exp.paidById
            if (payerId.isNotBlank() && !isFromBote(exp)) {
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
        val personalPotExcessByParticipant = mutableMapOf<String, Double>()

        for (pA in participants) {
            for (pB in participants) {
                if (pA.id == pB.id) continue
                val costAforB = realCostPaidByAforB[Pair(pA.id, pB.id)] ?: 0.0
                val costBforA = realCostPaidByAforB[Pair(pB.id, pA.id)] ?: 0.0
                val realDebtBtoA = max(0.0, costAforB - costBforA)

                val sentBtoA = settlementsSentFromBtoA[Pair(pB.id, pA.id)] ?: 0.0
                val settled = min(sentBtoA, realDebtBtoA)
                val excess = max(0.0, sentBtoA - realDebtBtoA)

                debtReimbursedToHolder[Pair(pB.id, pA.id)] = round2(settled)
                if (excess > 0.009) {
                    personalPotExcessByParticipant[pB.id] = (personalPotExcessByParticipant[pB.id] ?: 0.0) + excess
                }
            }
        }

        val totalSettlementExcess = round2(personalPotExcessByParticipant.values.sum())

        val balances = participants.map { participant ->
            // 1. Gastos reales que pagó de su propio bolsillo (excluyendo Bote Común)
            val realPaidOutOfPocket = realExpenses.filter {
                it.paidById == participant.id && !isFromBote(it)
            }.sumOf { it.amountInBaseCurrency }

            // 2. Liquidaciones enviadas directamente a otros participantes
            val settlementsSent = settlementExpenses.filter {
                it.paidById == participant.id && !isFromBote(it)
            }.sumOf { it.amountInBaseCurrency }

            // 3. Liquidaciones recibidas que saldaron gastos adelantados para otros (reembolsos)
            val debtReimbursementsReceived = participants.filter { it.id != participant.id }.sumOf { other ->
                debtReimbursedToHolder[Pair(other.id, participant.id)] ?: 0.0
            }

            // Total recaudado en liquidaciones recibidas (flujo de caja entrante)
            val settlementsReceivedTotal = settlementExpenses.filter {
                !isFromBote(it) && it.splits.any { s -> s.participantId == participant.id }
            }.sumOf { exp ->
                exp.splits.filter { it.participantId == participant.id }.sumOf { it.amount }
            }

            // 4. Lo que le tocaba asumir a cada uno: SÓLO gastos reales del viaje pagados por personas
            val realOwed = realExpenses.sumOf { expense ->
                val splitForPerson = expense.splits.find { it.participantId == participant.id }
                splitForPerson?.amount ?: if (expense.splits.isEmpty() && participants.isNotEmpty()) (expense.amountInBaseCurrency / participants.size) else 0.0
            }

            // 5. Bote Común aportado y compensación de deuda por ingresos especiales
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
            val debtDiscountCoveredByMe = incomes.sumOf { income ->
                val totalDiscount = income.contributions.sumOf { max(0.0, it.debtDiscount ?: 0.0) }
                if (totalDiscount > 0.009) {
                    val myCash = income.contributions.find { it.participantId == participant.id }?.amount ?: 0.0
                    val totalCash = income.contributions.sumOf { max(0.0, it.amount) }
                    if (totalCash > 0.009) {
                        (myCash / totalCash) * totalDiscount
                    } else 0.0
                } else {
                    0.0
                }
            }

            // Total desembolsado físicamente de su bolsillo en gastos reales y liquidaciones
            val totalPaid = realPaidOutOfPocket + settlementsSent

            // Flujo de caja: cobrosLiquidaciones - pagosGastos
            val cashFlow = settlementsReceivedTotal - totalPaid

            // Exceso propio en liquidaciones (bote personal por pago de más)
            val personalExcess = round2(personalPotExcessByParticipant[participant.id] ?: 0.0)

            // Saldo de otros usuarios: suma de botes personales por exceso de los demás participantes
            val otherExcess = round2(participants.filter { it.id != participant.id }.sumOf { personalPotExcessByParticipant[it.id] ?: 0.0 })

            // Balance neto
            val net = (totalPaid - debtReimbursementsReceived - debtDiscountCompensated + debtDiscountCoveredByMe) - realOwed

            runCatching {
                android.util.Log.d("DebtCalculator", "User: ${participant.name}, Pagado: -$totalPaid, Cobrado: +$settlementsReceivedTotal, Flujo: $cashFlow, Net: $net, PersonalExcess: $personalExcess, OtherUsersExcess: $otherExcess")
            }

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
                netBalance = round2(net),
                personalPotExcess = personalExcess,
                otherUsersExcess = otherExcess,
                cashFlowBalance = round2(cashFlow)
            )
        }

        val transactions = mutableListOf<DebtTransaction>()

        // Deudas directas entre participantes cruzando deudores (netBalance < 0) con acreedores reales por gastos adelantados
        val creditorRemaining = mutableMapOf<String, Double>()
        balances.forEach { b ->
            val realPaid = realExpenses.filter {
                it.paidById == b.participantId && !isFromBote(it)
            }.sumOf { it.amountInBaseCurrency }
            val creditoPorGastos = max(0.0, (realPaid - b.totalDebtSettlementsReceived - b.totalDebtDiscountCompensated) - b.totalExpensesOwed)
            val eligible = min(max(0.0, b.netBalance), creditoPorGastos)
            if (eligible > 0.009) {
                creditorRemaining[b.participantId] = eligible
            }
        }

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
            availableBote = round2(max(0.0, totalBoteCollected - totalSpentFromBoteDirectly)),
            totalSettlementExcess = totalSettlementExcess
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
