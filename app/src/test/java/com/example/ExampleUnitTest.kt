package com.example

import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplit
import com.example.data.model.IncomeContribution
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import com.example.domain.DebtCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun debtCalculator_calculatesNetBalanceAndBoteSettlementCorrectly() {
        val p1 = ParticipantEntity("p1", "t1", "Carlos", "#0066FF")
        val p2 = ParticipantEntity("p2", "t1", "Elena", "#10B981")
        val participants = listOf(p1, p2)

        // Carlos paga un gasto de 100 EUR (50 EUR corresponde a cada uno)
        val exp = ExpenseEntity(
            id = "e1",
            tripId = "t1",
            amount = 100.0,
            currency = "EUR",
            amountInBaseCurrency = 100.0,
            exchangeRate = 1.0,
            category = "Alojamiento",
            date = "2026-05-15",
            paymentMethod = "Tarjeta",
            notes = "Hotel",
            paidById = "p1",
            splits = listOf(
                ExpenseSplit("s1", "e1", "p1", 50.0, 50.0),
                ExpenseSplit("s2", "e1", "p2", 50.0, 50.0)
            )
        )

        // Elena pone 30 EUR en el bote común y Carlos pone 0 EUR (compensando 30 EUR de lo que se le debía)
        val income = IncomeEntity(
            id = "inc1",
            tripId = "t1",
            amount = 30.0,
            date = "2026-05-16",
            description = "Bote",
            basePerPerson = 30.0,
            calculationMethod = "perPerson",
            contributions = listOf(
                IncomeContribution("p1", 0.0, debtDiscount = 30.0),
                IncomeContribution("p2", 30.0, debtDiscount = 0.0)
            )
        )

        val result = DebtCalculator.calculateDebtBalances(participants, listOf(exp), listOf(income))
        val b1 = result.participantBalances.first { it.participantId == "p1" }
        val b2 = result.participantBalances.first { it.participantId == "p2" }

        // Carlos: (100 - 30 + 0) - 50 = +20.0 (más los 30 que coge del bote o ya compensó)
        assertEquals(20.0, b1.netBalance, 0.01)
        // Elena: (0 - 0 + 30) - 50 = -20.0
        assertEquals(-20.0, b2.netBalance, 0.01)
        assertTrue(result.transactions.isNotEmpty())
    }
}
