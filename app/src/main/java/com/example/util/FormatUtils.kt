package com.example.util

import com.example.ui.theme.AppLanguage
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

object FormatUtils {
    @Volatile
    var currentAppLocale: Locale = Locale.forLanguageTag("es")

    private val spanishLocale = Locale.forLanguageTag("es-ES")
    private val englishLocale = Locale.US

    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", spanishLocale)
    private val fullDayFormatterEs = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", spanishLocale)
    private val fullDayFormatterEn = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", englishLocale)

    fun formatCurrency(amount: Double, currencyCode: String = "EUR", locale: Locale = currentAppLocale): String {
        val symbol = CurrencyCatalog.getSymbol(currencyCode)
        val formattedNumber = String.format(locale, "%,.2f", amount)
        return "$formattedNumber $symbol"
    }

    fun formatSignedCurrency(amount: Double, currencyCode: String = "EUR", locale: Locale = currentAppLocale): String {
        val symbol = CurrencyCatalog.getSymbol(currencyCode)
        val sign = if (amount > 0.004) "+" else if (amount < -0.004) "-" else ""
        val formattedNumber = String.format(locale, "%,.2f", abs(amount))
        return "$sign$formattedNumber $symbol"
    }

    fun formatShortDate(isoDate: String, locale: Locale = currentAppLocale): String {
        return try {
            val parsed = LocalDate.parse(isoDate.take(10))
            val lang = locale.language.lowercase(Locale.ROOT)
            val pattern = when (lang) {
                "zh" -> "yyyy/MM/dd"
                "de", "pl" -> "dd.MM.yyyy"
                "nl" -> "dd-MM-yyyy"
                else -> "dd/MM/yyyy"
            }
            val formatter = DateTimeFormatter.ofPattern(pattern, locale)
            parsed.format(formatter)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatLongDate(isoDate: String, locale: Locale = currentAppLocale): String {
        return try {
            val parsed = LocalDate.parse(isoDate.take(10))
            val lang = locale.language.lowercase(Locale.ROOT)
            val pattern = when (lang) {
                "zh" -> "yyyy年M月d日 EEEE"
                "de" -> "EEEE, d. MMMM yyyy"
                "pl" -> "EEEE, d MMMM yyyy"
                "nl", "fr", "it" -> "EEEE d MMMM yyyy"
                "en" -> "EEEE, MMMM d, yyyy"
                else -> "EEEE, d 'de' MMMM 'de' yyyy" // es, pt
            }
            val formatter = DateTimeFormatter.ofPattern(pattern, locale)
            val text = parsed.format(formatter)
            text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatLongSpanishDate(isoDate: String, isEn: Boolean = false): String {
        val locale = if (isEn) englishLocale else spanishLocale
        return formatLongDate(isoDate, locale)
    }

    fun formatLongSpanishDate(isoDate: String, lang: AppLanguage): String {
        return formatLongDate(isoDate, lang.locale)
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
        if (trimmed.matches(Regex("\\d{1,2}\\.\\d{1,2}\\.\\d{4}"))) {
            val parts = trimmed.split(".")
            return "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}"
        }
        if (trimmed.matches(Regex("\\d{1,2}-\\d{1,2}-\\d{4}"))) {
            val parts = trimmed.split("-")
            return "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}"
        }
        if (trimmed.matches(Regex("\\d{4}/\\d{1,2}/\\d{1,2}"))) {
            val parts = trimmed.split("/")
            return "${parts[0]}-${parts[1].padStart(2, '0')}-${parts[2].padStart(2, '0')}"
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

    fun getDisplayName(curr: CurrencyInfo, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.ES -> curr.name
            AppLanguage.EN -> curr.nameEn
            AppLanguage.FR -> when (curr.code) {
                "EUR" -> "Euro"
                "USD" -> "Dollar américain"
                "JPY" -> "Yen japonais"
                "GBP" -> "Livre sterling"
                "CHF" -> "Franc suisse"
                "MXN" -> "Peso mexicain"
                "COP" -> "Peso colombien"
                "ARS" -> "Peso argentin"
                "CLP" -> "Peso chilien"
                "PEN" -> "Sol péruvien"
                "BRL" -> "Real brésilien"
                "CAD" -> "Dollar canadien"
                "AUD" -> "Dollar australien"
                "MAD" -> "Dirham marocain"
                "THB" -> "Baht thaïlandais"
                else -> curr.nameEn
            }
            AppLanguage.IT -> when (curr.code) {
                "EUR" -> "Euro"
                "USD" -> "Dollaro statunitense"
                "JPY" -> "Yen giapponese"
                "GBP" -> "Sterlina britannica"
                "CHF" -> "Franco svizzero"
                "MXN" -> "Peso messicano"
                "COP" -> "Peso colombiano"
                "ARS" -> "Peso argentino"
                "CLP" -> "Peso cileno"
                "PEN" -> "Sol peruviano"
                "BRL" -> "Real brasiliano"
                "CAD" -> "Dollaro canadese"
                "AUD" -> "Dollaro australiano"
                "MAD" -> "Dirham marocchino"
                "THB" -> "Baht thailandese"
                else -> curr.nameEn
            }
            AppLanguage.ZH -> when (curr.code) {
                "EUR" -> "欧元"
                "USD" -> "美元"
                "JPY" -> "日元"
                "GBP" -> "英镑"
                "CHF" -> "瑞士法郎"
                "MXN" -> "墨西哥比索"
                "COP" -> "哥伦比亚比索"
                "ARS" -> "阿根廷比索"
                "CLP" -> "智利比索"
                "PEN" -> "秘鲁索尔"
                "BRL" -> "巴西雷亚尔"
                "CAD" -> "加元"
                "AUD" -> "澳元"
                "MAD" -> "摩洛哥迪拉姆"
                "THB" -> "泰铢"
                else -> curr.nameEn
            }
            AppLanguage.NL -> when (curr.code) {
                "EUR" -> "Euro"
                "USD" -> "Amerikaanse dollar"
                "JPY" -> "Japanse yen"
                "GBP" -> "Britse pond"
                "CHF" -> "Zwitserse frank"
                "MXN" -> "Mexicaanse peso"
                "COP" -> "Colombiaanse peso"
                "ARS" -> "Argentijnse peso"
                "CLP" -> "Chileense peso"
                "PEN" -> "Peruaanse sol"
                "BRL" -> "Braziliaanse real"
                "CAD" -> "Canadese dollar"
                "AUD" -> "Australische dollar"
                "MAD" -> "Marokkaanse dirham"
                "THB" -> "Thaise baht"
                else -> curr.nameEn
            }
            AppLanguage.DE -> when (curr.code) {
                "EUR" -> "Euro"
                "USD" -> "US-Dollar"
                "JPY" -> "Japanischer Yen"
                "GBP" -> "Britisches Pfund"
                "CHF" -> "Schweizer Franken"
                "MXN" -> "Mexikanischer Peso"
                "COP" -> "Kolumbianischer Peso"
                "ARS" -> "Argentinischer Peso"
                "CLP" -> "Chilenischer Peso"
                "PEN" -> "Peruanischer Sol"
                "BRL" -> "Brasilianischer Real"
                "CAD" -> "Kanadischer Dollar"
                "AUD" -> "Australischer Dollar"
                "MAD" -> "Marokkanischer Dirham"
                "THB" -> "Thailändischer Baht"
                else -> curr.nameEn
            }
            AppLanguage.PL -> when (curr.code) {
                "EUR" -> "Euro"
                "USD" -> "Dolar amerykański"
                "JPY" -> "Jen japoński"
                "GBP" -> "Funt brytyjski"
                "CHF" -> "Frank szwajcarski"
                "MXN" -> "Peso meksykańskie"
                "COP" -> "Peso kolumbijskie"
                "ARS" -> "Peso argentyńskie"
                "CLP" -> "Peso chilijskie"
                "PEN" -> "Sol peruwiański"
                "BRL" -> "Real brazylijski"
                "CAD" -> "Dolar kanadyjski"
                "AUD" -> "Dolar australijski"
                "MAD" -> "Dirham marokański"
                "THB" -> "Bat tajlandzki"
                else -> curr.nameEn
            }
            AppLanguage.PT -> when (curr.code) {
                "EUR" -> "Euro"
                "USD" -> "Dólar americano"
                "JPY" -> "Iene japonês"
                "GBP" -> "Libra esterlina"
                "CHF" -> "Franco suíço"
                "MXN" -> "Peso mexicano"
                "COP" -> "Peso colombiano"
                "ARS" -> "Peso argentino"
                "CLP" -> "Peso chileno"
                "PEN" -> "Sol peruano"
                "BRL" -> "Real brasileiro"
                "CAD" -> "Dólar canadiano"
                "AUD" -> "Dólar australiano"
                "MAD" -> "Dirham marroquino"
                "THB" -> "Baht tailandês"
                else -> curr.nameEn
            }
        }
    }

    fun getDisplayName(curr: CurrencyInfo, isEn: Boolean): String {
        return getDisplayName(curr, if (isEn) AppLanguage.EN else AppLanguage.ES)
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
