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

    @Test
    fun formatUtils_formatsShortDateForSupportedLocales() {
        val testDate = "2026-11-16"
        assertEquals("16/11/2026", com.example.util.FormatUtils.formatShortDate(testDate, java.util.Locale.forLanguageTag("es")))
        assertEquals("16/11/2026", com.example.util.FormatUtils.formatShortDate(testDate, java.util.Locale.forLanguageTag("en")))
        assertEquals("16/11/2026", com.example.util.FormatUtils.formatShortDate(testDate, java.util.Locale.forLanguageTag("fr")))
        assertEquals("16/11/2026", com.example.util.FormatUtils.formatShortDate(testDate, java.util.Locale.forLanguageTag("it")))
        assertEquals("2026/11/16", com.example.util.FormatUtils.formatShortDate(testDate, java.util.Locale.SIMPLIFIED_CHINESE))
        assertEquals("16-11-2026", com.example.util.FormatUtils.formatShortDate(testDate, java.util.Locale.forLanguageTag("nl")))
        assertEquals("16.11.2026", com.example.util.FormatUtils.formatShortDate(testDate, java.util.Locale.forLanguageTag("de")))
        assertEquals("16.11.2026", com.example.util.FormatUtils.formatShortDate(testDate, java.util.Locale.forLanguageTag("pl")))
    }

    @Test
    fun formatUtils_formatsLongDateForSupportedLocales() {
        val testDate = "2026-11-16" // Monday
        val es = com.example.util.FormatUtils.formatLongDate(testDate, java.util.Locale.forLanguageTag("es"))
        val en = com.example.util.FormatUtils.formatLongDate(testDate, java.util.Locale.ENGLISH)
        val fr = com.example.util.FormatUtils.formatLongDate(testDate, java.util.Locale.FRENCH)
        val it = com.example.util.FormatUtils.formatLongDate(testDate, java.util.Locale.ITALIAN)
        val de = com.example.util.FormatUtils.formatLongDate(testDate, java.util.Locale.GERMAN)
        val zh = com.example.util.FormatUtils.formatLongDate(testDate, java.util.Locale.SIMPLIFIED_CHINESE)

        assertTrue(es.contains("noviembre", ignoreCase = true))
        assertTrue(en.contains("November", ignoreCase = true))
        assertTrue(fr.contains("novembre", ignoreCase = true))
        assertTrue(it.contains("novembre", ignoreCase = true))
        assertTrue(de.contains("November", ignoreCase = true))
        assertTrue(zh.contains("2026年11月16日"))
    }

    @Test
    fun expenses_sortedInChronologicalOrderWhenDateIsModified() {
        val exp1 = ExpenseEntity(
            id = "e1",
            tripId = "t1",
            amount = 10.0,
            currency = "EUR",
            amountInBaseCurrency = 10.0,
            exchangeRate = 1.0,
            category = "General",
            date = "2026-11-10",
            paymentMethod = "Tarjeta",
            notes = "First",
            paidById = "p1",
            createdAt = 1000L
        )
        val exp2 = ExpenseEntity(
            id = "e2",
            tripId = "t1",
            amount = 20.0,
            currency = "EUR",
            amountInBaseCurrency = 20.0,
            exchangeRate = 1.0,
            category = "General",
            date = "2026-11-15",
            paymentMethod = "Tarjeta",
            notes = "Second",
            paidById = "p1",
            createdAt = 2000L
        )
        val exp3 = ExpenseEntity(
            id = "e3",
            tripId = "t1",
            amount = 30.0,
            currency = "EUR",
            amountInBaseCurrency = 30.0,
            exchangeRate = 1.0,
            category = "General",
            date = "2026-11-20",
            paymentMethod = "Tarjeta",
            notes = "Third",
            paidById = "p1",
            createdAt = 3000L
        )

        val list = listOf(exp1, exp2, exp3)

        // Case A: User modifies exp3 (originally 2026-11-20) and changes date to 2026-11-12
        val modifiedExp3 = exp3.copy(date = "2026-11-12")
        val updatedList = listOf(exp1, exp2, modifiedExp3).sortedWith(
            compareBy<ExpenseEntity> { com.example.util.FormatUtils.parseFlexibleDateToIso(it.date) }
                .thenBy { it.createdAt }
        )

        // It should be placed chronologically between exp1 (Nov 10) and exp2 (Nov 15), NOT at the end!
        assertEquals("e1", updatedList[0].id)
        assertEquals("e3", updatedList[1].id)
        assertEquals("e2", updatedList[2].id)

        // Case B: User modifies exp2 (originally 2026-11-15) and changes date to 2026-11-05 (earliest date)
        val modifiedExp2 = exp2.copy(date = "2026-11-05")
        val updatedListB = listOf(exp1, modifiedExp2, exp3).sortedWith(
            compareBy<ExpenseEntity> { com.example.util.FormatUtils.parseFlexibleDateToIso(it.date) }
                .thenBy { it.createdAt }
        )

        // It should be at the beginning (Nov 05), NOT at the end!
        assertEquals("e2", updatedListB[0].id)
        assertEquals("e1", updatedListB[1].id)
        assertEquals("e3", updatedListB[2].id)
    }
}
