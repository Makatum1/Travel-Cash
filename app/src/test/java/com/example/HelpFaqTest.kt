package com.example

import com.example.ui.screens.TRAVEL_CASH_FAQS
import com.example.ui.screens.normalizeFaqSearchText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpFaqTest {

    @Test
    fun normalizeFaqSearchText_removesDiacriticsAndConvertsToLowercase() {
        val input = "¿CÓMO FUNCIONA EL BOTE COMÚN? ¡DIVISIÓN & EXPORTACIÓN!"
        val normalized = normalizeFaqSearchText(input)
        assertEquals("como funciona el bote comun division & exportacion", normalized)
    }

    @Test
    fun faqSearch_findsBoteWithoutAccents() {
        val query = normalizeFaqSearchText("bote")
        val matches = TRAVEL_CASH_FAQS.filter {
            normalizeFaqSearchText(it.questionEs).contains(query) || normalizeFaqSearchText(it.answerEs).contains(query)
        }
        assertTrue(matches.any { it.id == "faq_common_pot" })
    }

    @Test
    fun faqSearch_findsDivisionIgnoringAccents() {
        val query = normalizeFaqSearchText("division")
        val matches = TRAVEL_CASH_FAQS.filter {
            normalizeFaqSearchText(it.questionEs).contains(query) || normalizeFaqSearchText(it.answerEs).contains(query)
        }
        assertTrue(matches.any { it.id == "faq_split_expenses" })
    }

    @Test
    fun faqSearch_findsExport() {
        val query = normalizeFaqSearchText("export")
        val matches = TRAVEL_CASH_FAQS.filter {
            normalizeFaqSearchText(it.questionEs).contains(query) || normalizeFaqSearchText(it.answerEs).contains(query)
        }
        assertTrue(matches.any { it.id == "faq_export_zip" })
        assertTrue(matches.any { it.id == "faq_export_pdf" })
    }

    @Test
    fun faqSearch_worksInEnglish() {
        val query = normalizeFaqSearchText("pot")
        val matches = TRAVEL_CASH_FAQS.filter {
            normalizeFaqSearchText(it.questionEn).contains(query) || normalizeFaqSearchText(it.answerEn).contains(query)
        }
        assertTrue(matches.any { it.id == "faq_common_pot" })
    }

    @Test
    fun faqs_doNotContainForeignTermsLikeCaminoOrEtapa() {
        TRAVEL_CASH_FAQS.forEach { item ->
            val allText = "${item.categoryEs} ${item.categoryEn} ${item.questionEs} ${item.questionEn} ${item.answerEs} ${item.answerEn}".lowercase()
            assertFalse("FAQ ${item.id} should not mention 'camino'", allText.contains("camino"))
            assertFalse("FAQ ${item.id} should not mention 'etapa'", allText.contains("etapa"))
        }
    }

    @Test
    fun faqs_isComprehensiveAndCoversAllKeyAreas() {
        assertTrue("Debe contener al menos 20 preguntas frecuentes", TRAVEL_CASH_FAQS.size >= 20)
    }

    @Test
    fun faqs_provideTranslationsForAllSupportedLanguages() {
        val languages = com.example.ui.theme.AppLanguage.entries
        assertEquals(9, languages.size)
        TRAVEL_CASH_FAQS.forEach { item ->
            languages.forEach { lang ->
                val q = item.getQuestion(lang)
                val a = item.getAnswer(lang)
                val cat = item.getCategory(lang)
                assertTrue("Question must not be blank for ${item.id} in $lang", q.isNotBlank())
                assertTrue("Answer must not be blank for ${item.id} in $lang", a.isNotBlank())
                assertTrue("Category must not be blank for ${item.id} in $lang", cat.isNotBlank())

                val allText = "$q $a $cat".lowercase()
                assertFalse("FAQ ${item.id} in $lang should not mention 'camino'", allText.contains("camino"))
                assertFalse("FAQ ${item.id} in $lang should not mention 'etapa'", allText.contains("etapa"))
            }
        }
    }
}
