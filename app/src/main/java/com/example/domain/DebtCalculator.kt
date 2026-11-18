package com.example.domain

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
    val totalExpensesPaid: Double,            // Pagado de su bolsillo
    val totalExpensesOwed: Double,            // Parte de gastos que le correspondía asumir
    val totalBoteContributed: Double,         // Dinero en efectivo que ha metido en el bote común
    val totalDebtDiscountCompensated: Double, // Descuentos aplicados al poner bote porque se le debía dinero
    val netBalance: Double                    // (totalExpensesPaid - totalDebtDiscountCompensated + totalBoteContributed) - totalExpensesOwed
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
     * siguiendo con exactitud matemática las reglas del modelo contable de Travel Cash:
     *
     * netBalance = (totalExpensesPaid - totalDebtDiscountCompensated + totalBoteContributed) - totalExpensesOwed
     *
     * Paso 1: Transacción desde Bote Común ('bote-comun') hacia el Acreedor por min(availableBote, creditoPorGastos)
     * Paso 2: Deudas de participantes restantes (netBalance < 0) hacia los acreedores restantes.
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

        // Gastos pagados directamente con el método "Bote Común" o paidById == "bote-comun"
        val totalBoteCollected = incomes.sumOf { it.amount }
        val totalSpentFromBoteDirectly = expenses.filter {
            it.paidById == BOTE_COMUN_ID || it.paymentMethod.equals("Bote Común", ignoreCase = true)
        }.sumOf { it.amountInBaseCurrency }

        var availableBote = max(0.0, totalBoteCollected - totalSpentFromBoteDirectly)

        // Separar gastos reales del viaje de las liquidaciones de deuda entre participantes
        val realExpenses = expenses.filter { !com.example.data.model.CategoryCatalog.isDebtSettlementExpense(it) }
        val settlementExpenses = expenses.filter { com.example.data.model.CategoryCatalog.isDebtSettlementExpense(it) }

        val balances = participants.map { participant ->
            // Gastos reales que esta persona pagó de su bolsillo (excluyendo los que se pagaron con el Bote Común directamente)
            val realPaidOutOfPocket = realExpenses.filter {
                it.paidById == participant.id && !it.paymentMethod.equals("Bote Común", ignoreCase = true)
            }.sumOf { it.amountInBaseCurrency }

            // Gastos reales que a esta persona le correspondía asumir según los splits
            val realOwed = realExpenses.sumOf { expense ->
                val splitForPerson = expense.splits.find { it.participantId == participant.id }
                if (splitForPerson != null) {
                    splitForPerson.amount
                } else if (expense.splits.isEmpty() && participants.isNotEmpty()) {
                    expense.amountInBaseCurrency / participants.size
                } else {
                    0.0
                }
            }

            // Liquidaciones/pagos enviados directamente a otros participantes
            val settlementsSent = settlementExpenses.filter {
                it.paidById == participant.id && !it.paymentMethod.equals("Bote Común", ignoreCase = true)
            }.sumOf { it.amountInBaseCurrency }

            // Liquidaciones/pagos recibidos de otros participantes
            val settlementsReceived = settlementExpenses.filter {
                it.paidById != participant.id && !it.paymentMethod.equals("Bote Común", ignoreCase = true)
            }.sumOf { expense ->
                expense.splits.filter { it.participantId == participant.id }.sumOf { it.amount }
            }

            // Suma del dinero en efectivo que esta persona ha metido en el bote común
            val boteContributed = incomes.sumOf { income ->
                income.contributions
                    .filter { it.participantId == participant.id }
                    .sumOf { max(0.0, it.amount) }
            }

            // Suma de los descuentos (debtDiscount) que ya se le restaron al poner bote porque se le debía dinero
            val debtDiscountCompensated = incomes.sumOf { income ->
                income.contributions
                    .filter { it.participantId == participant.id }
                    .sumOf { max(0.0, it.debtDiscount ?: 0.0) }
            }

            val totalPaid = realPaidOutOfPocket + settlementsSent
            val net = (totalPaid - settlementsReceived - debtDiscountCompensated + boteContributed) - realOwed

            ParticipantBalance(
                participantId = participant.id,
                participantName = participant.name,
                avatarColor = participant.avatarColor,
                participantPhotoUri = participant.photoUri,
                totalExpensesPaid = round2(totalPaid),
                totalExpensesOwed = round2(realOwed),
                totalBoteContributed = round2(boteContributed),
                totalDebtDiscountCompensated = round2(debtDiscountCompensated),
                netBalance = round2(net)
            )
        }

        // Cálculo de excedentes de liquidación (Bote Personal / Saldo a favor gestionado por el administrador)
        // Si p1 envía más dinero en liquidaciones a p2 de lo que le debía a p2 por gastos reales,
        // el exceso se convierte en un bote personal de p1 custodiado por p2 para futuros gastos.
        val personalPotsHeld = mutableMapOf<Pair<String, String>, Double>()
        for (p1 in participants) {
            for (p2 in participants) {
                if (p1.id == p2.id) continue
                val sentP1toP2 = settlementExpenses.filter {
                    it.paidById == p1.id && it.splits.any { s -> s.participantId == p2.id }
                }.sumOf { it.amountInBaseCurrency }
                val sentP2toP1 = settlementExpenses.filter {
                    it.paidById == p2.id && it.splits.any { s -> s.participantId == p1.id }
                }.sumOf { it.amountInBaseCurrency }
                val netSettlementP1toP2 = sentP1toP2 - sentP2toP1

                val realCostP2forP1 = realExpenses.filter { it.paidById == p2.id }.sumOf { exp ->
                    exp.splits.find { s -> s.participantId == p1.id }?.amount ?: 0.0
                }
                val realCostP1forP2 = realExpenses.filter { it.paidById == p1.id }.sumOf { exp ->
                    exp.splits.find { s -> s.participantId == p2.id }?.amount ?: 0.0
                }
                val netRealDebtP1toP2 = max(0.0, realCostP2forP1 - realCostP1forP2)

                if (netSettlementP1toP2 > netRealDebtP1toP2 + 0.009) {
                    val excess = round2(netSettlementP1toP2 - netRealDebtP1toP2)
                    personalPotsHeld[Pair(p1.id, p2.id)] = excess
                }
            }
        }

        val potHeldForOthersMap = participants.associate { p ->
            p.id to personalPotsHeld.filter { it.key.second == p.id }.values.sum()
        }
        val potOwnedHeldByOthersMap = participants.associate { p ->
            p.id to personalPotsHeld.filter { it.key.first == p.id }.values.sum()
        }

        val transactions = mutableListOf<DebtTransaction>()

        // Estado mutable de cuánto les falta por cobrar a los acreedores
        val creditorRemaining = mutableMapOf<String, Double>()
        balances.forEach { b ->
            if (b.netBalance > 0.009) {
                creditorRemaining[b.participantId] = b.netBalance
            }
        }

        // PASO 1: Transacción del Bote Común al Acreedor
        // Si hay dinero recaudado en el bote (availableBote > 0) y hay personas que adelantaron gastos de su bolsillo
        // (creditoPorGastos = (totalExpensesPaid - totalDebtDiscountCompensated) - totalExpensesOwed > 0)
        for (b in balances) {
            if (availableBote <= 0.009) break
            val creditoPorGastos = max(0.0, (b.totalExpensesPaid - b.totalDebtDiscountCompensated) - b.totalExpensesOwed)
            val currentCreditorNeed = creditorRemaining[b.participantId] ?: 0.0
            val eligibleFromBote = min(creditoPorGastos, currentCreditorNeed)
            if (eligibleFromBote > 0.009) {
                val transferAmount = round2(min(availableBote, eligibleFromBote))
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

        // Si aún queda bote disponible y hay acreedores con saldo positivo pendiente, cubrirlo también desde el bote
        for (b in balances) {
            if (availableBote <= 0.009) break
            val currentCreditorNeed = creditorRemaining[b.participantId] ?: 0.0
            if (currentCreditorNeed > 0.009) {
                val transferAmount = round2(min(availableBote, currentCreditorNeed))
                if (transferAmount > 0.009) {
                    val existingIdx = transactions.indexOfFirst {
                        it.isFromBote && it.toParticipantId == b.participantId
                    }
                    if (existingIdx >= 0) {
                        val prev = transactions[existingIdx]
                        transactions[existingIdx] = prev.copy(amount = round2(prev.amount + transferAmount))
                    } else {
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
                    }
                    availableBote = max(0.0, availableBote - transferAmount)
                    creditorRemaining[b.participantId] = max(0.0, currentCreditorNeed - transferAmount)
                }
            }
        }

        // PASO 2: Deudas de Participantes Restantes
        // Se calculan las posiciones netas ajustadas excluyendo los botes personales custodiados
        // (el custodio que recibió un exceso no debe ese dinero como deuda en efectivo, sino que lo administra
        // como saldo a favor del pagador para futuros gastos; y el pagador no es un acreedor exigiendo cobro en mano).
        val adjustedNetMap = balances.associate { b ->
            val potHeld = potHeldForOthersMap[b.participantId] ?: 0.0
            val potOwned = potOwnedHeldByOthersMap[b.participantId] ?: 0.0
            b.participantId to round2(b.netBalance + potHeld - potOwned)
        }

        val debtors = balances
            .filter { (adjustedNetMap[it.participantId] ?: 0.0) < -0.009 }
            .map { it.participantId to abs(adjustedNetMap[it.participantId] ?: 0.0) }
            .toMutableList()

        val creditors = balances
            .filter { (creditorRemaining[it.participantId] ?: 0.0) - (potOwnedHeldByOthersMap[it.participantId] ?: 0.0) > 0.009 }
            .map { it.participantId to round2((creditorRemaining[it.participantId] ?: 0.0) - (potOwnedHeldByOthersMap[it.participantId] ?: 0.0)) }
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
     * Calcula cuánto se le debe actualmente a cada participante ANTES de registrar (o excluyendo) un ingreso concreto
     * para aplicar la Regla de Oro de "Cuantía por persona" en IncomeModal:
     * aportacionEfectivo = max(0, cuantiaPorPersona - deudaAFavor)
     * descuentoAplicado = min(cuantiaPorPersona, deudaAFavor)
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
            // A una persona se le debe dinero si su netBalance > 0 (o adelantó gastos > lo que debía)
            val creditoPorGastos = (balance.totalExpensesPaid - balance.totalDebtDiscountCompensated) - balance.totalExpensesOwed
            val deudaAFavor = max(0.0, max(balance.netBalance, creditoPorGastos))
            balance.participantId to round2(deudaAFavor)
        }
    }
}
