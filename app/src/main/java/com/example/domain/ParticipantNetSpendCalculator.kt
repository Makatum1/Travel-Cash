package com.example.domain

import com.example.data.model.CategoryCatalog
import com.example.data.model.ExpenseEntity
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import com.example.ui.theme.AppLanguage
import com.example.util.FormatUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

enum class MovementStatus {
    COMPLETED,
    PENDING
}

/**
 * Movimiento diferenciado de pago / transferencia / liquidación entre dos participantes.
 * No cuenta como gasto real del viaje, sino como ajuste neto (+ para quien paga, - para quien recibe).
 */
data class ParticipantTransfer(
    val id: String,
    val fromParticipantId: String,
    val toParticipantId: String,
    val amount: Double,
    val date: String,
    val description: String = "",
    val status: MovementStatus = MovementStatus.COMPLETED,
    val sourceExpense: ExpenseEntity? = null
)

enum class CommonPotMovementType {
    CONTRIBUTION,       // Aportación al bote (+ en gasto neto del participante)
    RECEIVED_FROM_POT   // Recibido del bote (- en gasto neto del participante)
}

/**
 * Movimiento diferenciado entre un participante y el Bote Común.
 * El bote común no es un participante normal; estos movimientos son ajustes de flujo de caja
 * y no alteran el gasto total real del viaje.
 */
data class CommonPotMovement(
    val id: String,
    val participantId: String,
    val type: CommonPotMovementType,
    val amount: Double,
    val date: String,
    val description: String = "",
    val status: MovementStatus = MovementStatus.COMPLETED,
    val sourceExpense: ExpenseEntity? = null,
    val sourceIncome: IncomeEntity? = null
)

enum class ParticipantLedgerEntryType {
    REAL_EXPENSE_PAID,  // Gasto real del viaje pagado directamente (+amount)
    TRANSFER_SENT,      // Pago a XXXX (+amount)
    TRANSFER_RECEIVED,  // Recibido de XXX (-amount)
    POT_CONTRIBUTION,   // Aportado al bote (+amount)
    POT_RECEIVED        // Recibido del bote (-amount)
}

data class ParticipantLedgerEntry(
    val id: String,
    val type: ParticipantLedgerEntryType,
    val signedAmount: Double,
    val absoluteAmount: Double,
    val date: String,
    val title: String,
    val counterpartyName: String? = null,
    val categoryName: String? = null,
    val description: String = "",
    val status: MovementStatus = MovementStatus.COMPLETED,
    val sourceExpense: ExpenseEntity? = null,
    val sourceIncome: IncomeEntity? = null
) {
    fun displayLabel(lang: AppLanguage): String {
        val other = counterpartyName.orEmpty().ifBlank { "?" }
        return when (type) {
            ParticipantLedgerEntryType.REAL_EXPENSE_PAID ->
                title.ifBlank { CategoryCatalog.displayName(categoryName.orEmpty(), lang) }
            ParticipantLedgerEntryType.TRANSFER_SENT -> when (lang) {
                AppLanguage.ES -> "Pago a $other"
                AppLanguage.EN -> "Paid to $other"
                AppLanguage.FR -> "Paiement à $other"
                AppLanguage.IT -> "Pagamento a $other"
                AppLanguage.ZH -> "支付给 $other"
                AppLanguage.NL -> "Betaald aan $other"
                AppLanguage.DE -> "Zahlung an $other"
                AppLanguage.PL -> "Płatność dla $other"
                AppLanguage.PT -> "Pagamento a $other"
            }
            ParticipantLedgerEntryType.TRANSFER_RECEIVED -> when (lang) {
                AppLanguage.ES -> "Recibido de $other"
                AppLanguage.EN -> "Received from $other"
                AppLanguage.FR -> "Reçu de $other"
                AppLanguage.IT -> "Ricevuto da $other"
                AppLanguage.ZH -> "收到来自 $other"
                AppLanguage.NL -> "Ontvangen van $other"
                AppLanguage.DE -> "Erhalten von $other"
                AppLanguage.PL -> "Otrzymano od $other"
                AppLanguage.PT -> "Recebido de $other"
            }
            ParticipantLedgerEntryType.POT_CONTRIBUTION -> when (lang) {
                AppLanguage.ES -> "Aportado al bote"
                AppLanguage.EN -> "Contributed to pot"
                AppLanguage.FR -> "Versé à la cagnotte"
                AppLanguage.IT -> "Versato nella cassa"
                AppLanguage.ZH -> "存入公账"
                AppLanguage.NL -> "Bijgedragen aan pot"
                AppLanguage.DE -> "In Kasse eingezahlt"
                AppLanguage.PL -> "Wpłacono do kasy"
                AppLanguage.PT -> "Contribuído para o fundo"
            }
            ParticipantLedgerEntryType.POT_RECEIVED -> when (lang) {
                AppLanguage.ES -> "Recibido del bote"
                AppLanguage.EN -> "Received from pot"
                AppLanguage.FR -> "Reçu de la cagnotte"
                AppLanguage.IT -> "Ricevuto dalla cassa"
                AppLanguage.ZH -> "从公账收到"
                AppLanguage.NL -> "Ontvangen uit pot"
                AppLanguage.DE -> "Aus Kasse erhalten"
                AppLanguage.PL -> "Otrzymano z kasy"
                AppLanguage.PT -> "Recebido do fundo"
            }
        }
    }
}

data class ParticipantNetSpendSummary(
    val participant: ParticipantEntity,
    val realExpensesPaid: Double,
    val transfersSent: Double,
    val transfersReceived: Double,
    val potContributions: Double,
    val potReceived: Double,
    val netSpend: Double,                  // Pagado (desembolso neto real de su bolsillo)
    val assumedSpend: Double,              // Asumido (consumo real según reglas de reparto de los gastos)
    val balance: Double,                   // Balance / Reserva = Pagado - Asumido (+ a favor/reserva, - debe)
    val differenceFromAverage: Double,
    val entries: List<ParticipantLedgerEntry>
) {
    val isBalancedWithAverage: Boolean
        get() = abs(differenceFromAverage) <= 0.015

    val isBalancedWithAssumed: Boolean
        get() = abs(balance) <= 0.015
}

data class TripNetSpendReport(
    val realTotalSpent: Double,
    val realExpenseCount: Int,
    val participantCount: Int,
    val equalShareAverage: Double,
    val totalPotCollected: Double,
    val totalRealSpentFromPot: Double,
    val totalReceivedFromPot: Double,
    val commonPotBalance: Double,
    val participantSummaries: List<ParticipantNetSpendSummary>,
    val participantsWithDifference: List<ParticipantNetSpendSummary>,
    val allEqual: Boolean,
    val transfers: List<ParticipantTransfer>,
    val potMovements: List<CommonPotMovement>
)

data class ParticipantCategoryExpenseDetail(
    val expense: ExpenseEntity,
    val participantAmount: Double
)

data class ParticipantCategorySpendItem(
    val categoryName: String,
    val totalAmount: Double,
    val expenseCount: Int,
    val details: List<ParticipantCategoryExpenseDetail>
)

data class ParticipantCategoryBreakdown(
    val participant: ParticipantEntity,
    val totalAmount: Double,
    val totalExpenseCount: Int,
    val categories: List<ParticipantCategorySpendItem>
)

object ParticipantNetSpendCalculator {

    private fun round2(value: Double): Double = (value * 100.0).roundToLong() / 100.0

    private fun isPaidFromBote(exp: ExpenseEntity): Boolean =
        exp.paidById == DebtCalculator.BOTE_COMUN_ID ||
                exp.paymentMethod.equals("Bote Común", ignoreCase = true)

    /**
     * Calcula el informe de gasto neto por participante para "Datos → Gastos por participante":
     *
     * Gasto neto = Gastos pagados + Pagos/liquidaciones enviadas + Aportaciones al bote
     *              - Pagos/liquidaciones recibidas - Dinero recibido del bote
     *
     * Reglas garantizadas:
     * - Las liquidaciones entre participantes y los movimientos con el bote NO se suman al gasto total real del viaje.
     * - Si el bote paga directamente un gasto real del viaje, cuenta una sola vez en realTotalSpent y no se duplica
     *   en ningún participante (su aportación al bote ya contabilizó su desembolso).
     */
    fun calculate(
        participants: List<ParticipantEntity>,
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>,
        extraTransfers: List<ParticipantTransfer> = emptyList(),
        extraPotMovements: List<CommonPotMovement> = emptyList()
    ): TripNetSpendReport {
        val participantMap = participants.associateBy { it.id }

        val realExpenses = mutableListOf<ExpenseEntity>()
        val derivedTransfers = mutableListOf<ParticipantTransfer>()
        val derivedPotMovements = mutableListOf<CommonPotMovement>()

        var realTotalSpent = 0.0
        var totalRealSpentFromPot = 0.0

        // 1. Clasificar gastos reales vs liquidaciones entre participantes y entregas del bote
        for (exp in expenses) {
            val isSettlement = CategoryCatalog.isDebtSettlementExpense(exp)
            val fromBote = isPaidFromBote(exp)

            if (isSettlement) {
                // Ajuste de liquidación: nunca altera el gasto total real del viaje
                if (fromBote) {
                    if (exp.splits.isNotEmpty()) {
                        exp.splits.filter { it.amount > 0.004 }.forEachIndexed { idx, split ->
                            derivedPotMovements.add(
                                CommonPotMovement(
                                    id = "${exp.id}-pot-recv-$idx",
                                    participantId = split.participantId,
                                    type = CommonPotMovementType.RECEIVED_FROM_POT,
                                    amount = round2(split.amount),
                                    date = exp.date,
                                    description = exp.notes.ifBlank { exp.placeName },
                                    status = MovementStatus.COMPLETED,
                                    sourceExpense = exp
                                )
                            )
                        }
                    }
                } else {
                    if (exp.splits.isNotEmpty()) {
                        exp.splits.filter { it.amount > 0.004 }.forEachIndexed { idx, split ->
                            derivedTransfers.add(
                                ParticipantTransfer(
                                    id = "${exp.id}-xfer-$idx",
                                    fromParticipantId = exp.paidById,
                                    toParticipantId = split.participantId,
                                    amount = round2(split.amount),
                                    date = exp.date,
                                    description = exp.notes.ifBlank { exp.placeName },
                                    status = MovementStatus.COMPLETED,
                                    sourceExpense = exp
                                )
                            )
                        }
                    }
                }
            } else {
                // Gasto real del viaje
                realExpenses.add(exp)
                realTotalSpent += exp.amountInBaseCurrency
                if (fromBote) {
                    totalRealSpentFromPot += exp.amountInBaseCurrency
                }
            }
        }

        // 2. Extraer aportaciones al bote desde IncomeEntity
        var totalPotCollected = 0.0
        for (income in incomes) {
            totalPotCollected += max(0.0, income.amount)
            income.contributions
                .filter { it.amount > 0.004 }
                .forEachIndexed { idx, contrib ->
                    derivedPotMovements.add(
                        CommonPotMovement(
                            id = "${income.id}-pot-contrib-$idx-${contrib.participantId}",
                            participantId = contrib.participantId,
                            type = CommonPotMovementType.CONTRIBUTION,
                            amount = round2(contrib.amount),
                            date = income.date,
                            description = income.description,
                            status = MovementStatus.COMPLETED,
                            sourceIncome = income
                        )
                    )
                }
        }

        // Incorporar posibles transferencias y movimientos de bote explícitos adicionales
        val allTransfers = derivedTransfers + extraTransfers
        val allPotMovements = derivedPotMovements + extraPotMovements

        val completedTransfers = allTransfers.filter { it.status == MovementStatus.COMPLETED }
        val completedPotMovements = allPotMovements.filter { it.status == MovementStatus.COMPLETED }

        // Sumar aportaciones adicionales explícitas al total recaudado del bote (las de incomes ya se contaron arriba)
        val extraPotContributions = extraPotMovements
            .filter { it.status == MovementStatus.COMPLETED && it.type == CommonPotMovementType.CONTRIBUTION }
            .sumOf { max(0.0, it.amount) }
        totalPotCollected += extraPotContributions

        val totalReceivedFromPot = completedPotMovements
            .filter { it.type == CommonPotMovementType.RECEIVED_FROM_POT }
            .sumOf { max(0.0, it.amount) }

        val commonPotBalance = round2(totalPotCollected - totalRealSpentFromPot - totalReceivedFromPot)

        val roundedRealTotal = round2(realTotalSpent)
        val equalShareAverage = if (participants.isNotEmpty()) {
            round2(roundedRealTotal / participants.size)
        } else {
            0.0
        }

        // 3. Construir el resumen neto y desglose por participante
        val summaries = participants.map { participant ->
            val entries = mutableListOf<ParticipantLedgerEntry>()

            // A) Gastos reales pagados directamente por este participante (excluyendo los pagados con Bote Común)
            var paidDirectly = 0.0
            realExpenses
                .filter { it.paidById == participant.id && !isPaidFromBote(it) }
                .forEach { exp ->
                    val amt = round2(exp.amountInBaseCurrency)
                    paidDirectly += amt
                    entries.add(
                        ParticipantLedgerEntry(
                            id = "exp-${exp.id}",
                            type = ParticipantLedgerEntryType.REAL_EXPENSE_PAID,
                            signedAmount = amt,
                            absoluteAmount = amt,
                            date = exp.date,
                            title = exp.placeName.ifBlank { exp.notes },
                            categoryName = exp.category,
                            description = exp.notes,
                            status = MovementStatus.COMPLETED,
                            sourceExpense = exp
                        )
                    )
                }

            // B) Pagos/liquidaciones enviadas a otros participantes ("Pago a XXXX") -> +importe
            var sentTotal = 0.0
            completedTransfers
                .filter { it.fromParticipantId == participant.id && it.toParticipantId != participant.id }
                .forEach { tx ->
                    val amt = round2(tx.amount)
                    sentTotal += amt
                    val receiverName = participantMap[tx.toParticipantId]?.name ?: "?"
                    entries.add(
                        ParticipantLedgerEntry(
                            id = "sent-${tx.id}",
                            type = ParticipantLedgerEntryType.TRANSFER_SENT,
                            signedAmount = amt,
                            absoluteAmount = amt,
                            date = tx.date,
                            title = "Pago a $receiverName",
                            counterpartyName = receiverName,
                            description = tx.description,
                            status = tx.status,
                            sourceExpense = tx.sourceExpense
                        )
                    )
                }

            // C) Pagos/liquidaciones recibidas de otros participantes ("Recibido de XXX")
            // Sólo se descuenta del desembolso la parte que salda deudas reales que este participante adelantó.
            // El exceso es saldo extra custodiado para el emisor y no reduce el gasto del receptor.
            val otherParticipants = participants.filter { it.id != participant.id }
            val debtReimbursedReceivedTotal = if (realExpenses.isEmpty()) {
                completedTransfers.filter { it.toParticipantId == participant.id && it.fromParticipantId != participant.id }.sumOf { it.amount }
            } else {
                otherParticipants.sumOf { sender ->
                    val totalSentBySenderToMe = completedTransfers
                        .filter { it.fromParticipantId == sender.id && it.toParticipantId == participant.id }
                        .sumOf { it.amount }
                    val realCostPaidByMeForSender = realExpenses.filter { it.paidById == participant.id }.sumOf { exp ->
                        val sp = exp.splits.find { it.participantId == sender.id }
                        sp?.amount ?: if (exp.splits.isEmpty() && participants.isNotEmpty()) (exp.amountInBaseCurrency / participants.size) else 0.0
                    }
                    val realCostPaidBySenderForMe = realExpenses.filter { it.paidById == sender.id }.sumOf { exp ->
                        val sp = exp.splits.find { it.participantId == participant.id }
                        sp?.amount ?: if (exp.splits.isEmpty() && participants.isNotEmpty()) (exp.amountInBaseCurrency / participants.size) else 0.0
                    }
                    val realDebtSenderToMe = max(0.0, realCostPaidByMeForSender - realCostPaidBySenderForMe)
                    min(totalSentBySenderToMe, realDebtSenderToMe)
                }
            }

            completedTransfers
                .filter { it.toParticipantId == participant.id && it.fromParticipantId != participant.id }
                .forEach { tx ->
                    val senderId = tx.fromParticipantId
                    val amt = round2(tx.amount)
                    val senderName = participantMap[senderId]?.name ?: "?"
                    entries.add(
                        ParticipantLedgerEntry(
                            id = "recv-${tx.id}",
                            type = ParticipantLedgerEntryType.TRANSFER_RECEIVED,
                            signedAmount = -amt,
                            absoluteAmount = amt,
                            date = tx.date,
                            title = "Recibido de $senderName",
                            counterpartyName = senderName,
                            description = tx.description,
                            status = tx.status,
                            sourceExpense = tx.sourceExpense
                        )
                    )
                }

            // D) Aportaciones al bote común ("Aportado al bote") -> +importe
            var potContribTotal = 0.0
            completedPotMovements
                .filter { it.participantId == participant.id && it.type == CommonPotMovementType.CONTRIBUTION }
                .forEach { mv ->
                    val amt = round2(mv.amount)
                    potContribTotal += amt
                    entries.add(
                        ParticipantLedgerEntry(
                            id = "pot-in-${mv.id}",
                            type = ParticipantLedgerEntryType.POT_CONTRIBUTION,
                            signedAmount = amt,
                            absoluteAmount = amt,
                            date = mv.date,
                            title = "Aportado al bote",
                            description = mv.description,
                            status = mv.status,
                            sourceIncome = mv.sourceIncome
                        )
                    )
                }

            // E) Dinero recibido del bote común ("Recibido del bote") -> -importe
            var potRecvTotal = 0.0
            completedPotMovements
                .filter { it.participantId == participant.id && it.type == CommonPotMovementType.RECEIVED_FROM_POT }
                .forEach { mv ->
                    val amt = round2(mv.amount)
                    potRecvTotal += amt
                    entries.add(
                        ParticipantLedgerEntry(
                            id = "pot-out-${mv.id}",
                            type = ParticipantLedgerEntryType.POT_RECEIVED,
                            signedAmount = -amt,
                            absoluteAmount = amt,
                            date = mv.date,
                            title = "Recibido del bote",
                            description = mv.description,
                            status = mv.status,
                            sourceExpense = mv.sourceExpense
                        )
                    )
                }

            // F) Gasto asumido según las reglas exactas de reparto (splits) de cada gasto real del viaje
            val assumedRaw = realExpenses.sumOf { exp ->
                if (exp.splits.isNotEmpty()) {
                    exp.splits.filter { it.participantId == participant.id }.sumOf { it.amount }
                } else if (participants.isNotEmpty()) {
                    exp.amountInBaseCurrency / participants.size
                } else {
                    0.0
                }
            }
            val assumedSpend = round2(assumedRaw)

            // Pagado (desembolso neto real de su bolsillo) = Gastos pagados + Liquidaciones enviadas + Aportaciones al bote - Reembolsos recibidos de deudas - Recibido del bote
            val netSpend = round2(paidDirectly + sentTotal + potContribTotal - debtReimbursedReceivedTotal - potRecvTotal)
            // Balance / Reserva = Pagado - Asumido (+ saldo a favor / reserva para futuras deudas, - deuda pendiente)
            val balance = round2(netSpend - assumedSpend)
            val diffFromAvg = round2(netSpend - equalShareAverage)

            val sortedEntries = entries.sortedByDescending { FormatUtils.parseFlexibleDateToIso(it.date) }

            ParticipantNetSpendSummary(
                participant = participant,
                realExpensesPaid = round2(paidDirectly),
                transfersSent = round2(sentTotal),
                transfersReceived = round2(debtReimbursedReceivedTotal),
                potContributions = round2(potContribTotal),
                potReceived = round2(potRecvTotal),
                netSpend = netSpend,
                assumedSpend = assumedSpend,
                balance = balance,
                differenceFromAverage = diffFromAvg,
                entries = sortedEntries
            )
        }.sortedByDescending { it.netSpend }

        val withDiff = summaries.filter { !it.isBalancedWithAverage }

        return TripNetSpendReport(
            realTotalSpent = roundedRealTotal,
            realExpenseCount = realExpenses.size,
            participantCount = participants.size,
            equalShareAverage = equalShareAverage,
            totalPotCollected = round2(totalPotCollected),
            totalRealSpentFromPot = round2(totalRealSpentFromPot),
            totalReceivedFromPot = round2(totalReceivedFromPot),
            commonPotBalance = commonPotBalance,
            participantSummaries = summaries,
            participantsWithDifference = withDiff,
            allEqual = withDiff.isEmpty(),
            transfers = allTransfers,
            potMovements = allPotMovements
        )
    }

    /**
     * Agrupa los gastos reales del viaje por participante y categoría (excluyendo liquidaciones de deuda),
     * usando la parte asumida de cada participante en cada gasto para reflejar su gasto real por categoría
     * (por ejemplo, cuánto ha gastado una persona en Alojamiento, Restaurantes y Comida, Transporte, etc.).
     */
    fun calculateByParticipantAndCategory(
        participants: List<ParticipantEntity>,
        expenses: List<ExpenseEntity>
    ): List<ParticipantCategoryBreakdown> {
        val realExpenses = expenses.filter { !CategoryCatalog.isDebtSettlementExpense(it) }

        return participants.map { participant ->
            val categoryBuckets = LinkedHashMap<String, MutableList<ParticipantCategoryExpenseDetail>>()

            for (exp in realExpenses) {
                val share = if (exp.splits.isNotEmpty()) {
                    exp.splits
                        .filter { it.participantId == participant.id }
                        .sumOf { it.amount }
                } else if (participants.isNotEmpty()) {
                    exp.amountInBaseCurrency / participants.size
                } else {
                    0.0
                }

                if (share > 0.004) {
                    val canonicalCat = CategoryCatalog.getCategory(exp.category).name
                    categoryBuckets
                        .getOrPut(canonicalCat) { mutableListOf() }
                        .add(
                            ParticipantCategoryExpenseDetail(
                                expense = exp,
                                participantAmount = round2(share)
                            )
                        )
                }
            }

            val categories = categoryBuckets.map { (catName, details) ->
                val sortedDetails = details.sortedByDescending {
                    FormatUtils.parseFlexibleDateToIso(it.expense.date)
                }
                val catTotal = round2(sortedDetails.sumOf { it.participantAmount })
                ParticipantCategorySpendItem(
                    categoryName = catName,
                    totalAmount = catTotal,
                    expenseCount = sortedDetails.size,
                    details = sortedDetails
                )
            }.sortedByDescending { it.totalAmount }

            val totalForPerson = round2(categories.sumOf { it.totalAmount })
            val totalCountForPerson = categories.sumOf { it.expenseCount }

            ParticipantCategoryBreakdown(
                participant = participant,
                totalAmount = totalForPerson,
                totalExpenseCount = totalCountForPerson,
                categories = categories
            )
        }.sortedByDescending { it.totalAmount }
    }
}
