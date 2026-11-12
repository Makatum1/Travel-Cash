package com.example.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

object FormatUtils {
    private val spanishLocale = Locale("es", "ES")
    private val englishLocale = Locale.US

    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", spanishLocale)
    private val fullDayFormatterEs = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", spanishLocale)
    private val fullDayFormatterEn = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", englishLocale)

    fun formatCurrency(amount: Double, currencyCode: String = "EUR"): String {
        val symbol = CurrencyCatalog.getSymbol(currencyCode)
        val formattedNumber = String.format(spanishLocale, "%,.2f", amount)
        return "$formattedNumber $symbol"
    }

    fun formatSignedCurrency(amount: Double, currencyCode: String = "EUR"): String {
        val symbol = CurrencyCatalog.getSymbol(currencyCode)
        val sign = if (amount > 0.004) "+" else if (amount < -0.004) "-" else ""
        val formattedNumber = String.format(spanishLocale, "%,.2f", abs(amount))
        return "$sign$formattedNumber $symbol"
    }

    fun formatShortDate(isoDate: String): String {
        return try {
            val parsed = LocalDate.parse(isoDate.take(10))
            parsed.format(shortDateFormatter)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatLongSpanishDate(isoDate: String, isEn: Boolean = false): String {
        return try {
            val parsed = LocalDate.parse(isoDate.take(10))
            val locale = if (isEn) englishLocale else spanishLocale
            val formatter = if (isEn) fullDayFormatterEn else fullDayFormatterEs
            val text = parsed.format(formatter)
            text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        } catch (e: Exception) {
            isoDate
        }
    }

    fun parseFlexibleDateToIso(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return trimmed
        if (trimmed.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) {
            val parts = trimmed.split("/")
            return "${parts[2]}-${parts[1]}-${parts[0]}"
        }
        if (trimmed.matches(Regex("\\d{1,2}/\\d{1,2}/\\d{4}"))) {
            val parts = trimmed.split("/")
            return "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}"
        }
        return try {
            LocalDate.parse(trimmed.take(10)).toString()
        } catch (e: Exception) {
            LocalDate.now().toString()
        }
    }

    fun calculateTripDays(startDate: String, endDate: String): Int {
        return try {
            val s = LocalDate.parse(startDate.take(10))
            val e = LocalDate.parse(endDate.take(10))
            val diff = ChronoUnit.DAYS.between(s, e).toInt() + 1
            diff.coerceAtLeast(1)
        } catch (e: Exception) {
            1
        }
    }
}

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val rateToEur: Double, // 1 unit of currency = rateToEur EUR (approximate offline default)
    val nameEn: String = name
)

object CurrencyCatalog {
    val currencies = listOf(
        CurrencyInfo("EUR", "Euro", "€", 1.0, "Euro"),
        CurrencyInfo("USD", "Dólar estadounidense", "$", 0.92, "US Dollar"),
        CurrencyInfo("JPY", "Yen japonés", "¥", 0.0061, "Japanese Yen"),
        CurrencyInfo("GBP", "Libra esterlina", "£", 1.17, "British Pound"),
        CurrencyInfo("CHF", "Franco suizo", "CHF", 1.05, "Swiss Franc"),
        CurrencyInfo("MXN", "Peso mexicano", "MX$", 0.049, "Mexican Peso"),
        CurrencyInfo("COP", "Peso colombiano", "COL$", 0.00022, "Colombian Peso"),
        CurrencyInfo("ARS", "Peso argentino", "AR$", 0.00095, "Argentine Peso"),
        CurrencyInfo("CLP", "Peso chileno", "CLP$", 0.00097, "Chilean Peso"),
        CurrencyInfo("PEN", "Sol peruano", "S/", 0.25, "Peruvian Sol"),
        CurrencyInfo("BRL", "Real brasileño", "R$", 0.16, "Brazilian Real"),
        CurrencyInfo("CAD", "Dólar canadiense", "C$", 0.67, "Canadian Dollar"),
        CurrencyInfo("AUD", "Dólar australiano", "A$", 0.61, "Australian Dollar"),
        CurrencyInfo("MAD", "Dírham marroquí", "DH", 0.093, "Moroccan Dirham"),
        CurrencyInfo("THB", "Baht tailandés", "฿", 0.027, "Thai Baht")
    )

    fun getSymbol(code: String): String {
        return currencies.find { it.code.equals(code, ignoreCase = true) }?.symbol ?: code
    }

    fun getDisplayName(curr: CurrencyInfo, isEn: Boolean): String {
        return if (isEn) curr.nameEn else curr.name
    }

    /**
     * Returns default exchange rate so that:
     * amountInBaseCurrency = amount * exchangeRate
     */
    fun getDefaultExchangeRate(fromCurrency: String, toBaseCurrency: String): Double {
        if (fromCurrency.equals(toBaseCurrency, ignoreCase = true)) return 1.0
        val from = currencies.find { it.code.equals(fromCurrency, ignoreCase = true) }?.rateToEur ?: 1.0
        val to = currencies.find { it.code.equals(toBaseCurrency, ignoreCase = true) }?.rateToEur ?: 1.0
        if (to == 0.0) return 1.0
        return (from / to)
    }
}
