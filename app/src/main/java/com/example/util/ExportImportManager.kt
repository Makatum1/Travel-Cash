package com.example.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.AvatarColorPalette
import com.example.data.model.CategoryCatalog
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplit
import com.example.data.model.IncomeContribution
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.TripEntity
import com.example.domain.DebtCalculator
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.roundToLong

data class CsvColumnMapping(
    val dateColIndex: Int = -1,
    val amountColIndex: Int = -1,
    val currencyColIndex: Int = -1,
    val categoryColIndex: Int = -1,
    val placeColIndex: Int = -1,
    val notesColIndex: Int = -1,
    val paidByColIndex: Int = -1,
    val paymentMethodColIndex: Int = -1,
    val countryColIndex: Int = -1,
    val latColIndex: Int = -1,
    val lonColIndex: Int = -1
)

data class ParsedCsvSheet(
    val headers: List<String>,
    val rows: List<List<String>>,
    val suggestedMapping: CsvColumnMapping,
    val isStandardAutoDetected: Boolean
)

data class BackupPayload(
    val trips: List<TripEntity>,
    val participants: List<ParticipantEntity>,
    val expenses: List<ExpenseEntity>,
    val incomes: List<IncomeEntity>,
    val activeTripId: String? = null,
    val themeMode: String? = null,
    val language: String? = null
)

data class TripPackage(
    val trip: TripEntity,
    val participants: List<ParticipantEntity>,
    val expenses: List<ExpenseEntity>,
    val incomes: List<IncomeEntity>
)

object ExportImportManager {

    private fun round2(v: Double): Double = (v * 100.0).roundToLong() / 100.0

    /**
     * Normaliza el nombre de un viaje eliminando tildes/acentos, espacios sobrantes
     * y convirtiendo a minúsculas para comparar duplicados estrictamente sin importar
     * mayúsculas, minúsculas ni acentos (ej. "Paris", "paris" y "París" resultan en "paris").
     */
    fun normalizeTripName(name: String): String {
        return Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .trim()
            .lowercase(Locale.ROOT)
    }

    /**
     * Asegura que la primera letra del nombre del viaje se guarde siempre en mayúscula.
     */
    fun capitalizeTripName(name: String): String {
        val trimmed = name.trim()
        return trimmed.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
    }

    /**
     * Exporta uno o varios viajes dentro de un archivo .zip.
     * Si se eligen todos, agrupa todos los .json dentro de ese ZIP.
     * Si se elige uno, el ZIP contendrá solo ese JSON.
     */
    fun writeTripsToZip(
        outputStream: OutputStream,
        tripsToExport: List<TripEntity>,
        allParticipants: List<ParticipantEntity>,
        allExpenses: List<ExpenseEntity>,
        allIncomes: List<IncomeEntity>
    ) {
        if (tripsToExport.isEmpty()) return
        val baos = ByteArrayOutputStream()
        val usedNames = mutableSetOf<String>()
        ZipOutputStream(baos).use { zipOut ->
            tripsToExport.forEach { trip ->
                val parts = allParticipants.filter { it.tripId == trip.id }
                val exps = allExpenses.filter { it.tripId == trip.id }
                val incs = allIncomes.filter { it.tripId == trip.id }
                val jsonContent = exportFullBackupJson(
                    trips = listOf(trip),
                    participants = parts,
                    expenses = exps,
                    incomes = incs
                )
                val baseName = trip.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "viaje" }
                var candidate = "$baseName.json"
                var suffix = 2
                while (!usedNames.add(candidate.lowercase(Locale.ROOT))) {
                    candidate = "${baseName}_$suffix.json"
                    suffix++
                }
                val zipEntry = ZipEntry(candidate)
                zipOut.putNextEntry(zipEntry)
                zipOut.write(jsonContent.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }
        }
        outputStream.write(baos.toByteArray())
        outputStream.flush()
    }

    /**
     * Lee un archivo .zip o .json desde un InputStream y extrae todos los paquetes de viajes.
     */
    fun readTripsFromZip(inputStream: InputStream): List<TripPackage> {
        val bytes = inputStream.readBytes()
        return readTripsFromBytes(bytes)
    }

    /**
     * Detecta automáticamente si los bytes corresponden a un archivo .zip (con uno o varios .json dentro)
     * o a un archivo .json individual exportado directamente desde un viaje, y extrae todos los viajes válidos.
     */
    fun readTripsFromBytes(bytes: ByteArray): List<TripPackage> {
        if (bytes.isEmpty()) return emptyList()
        val results = mutableListOf<TripPackage>()
        val isZipHeader = bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

        if (isZipHeader) {
            runCatching {
                ZipInputStream(ByteArrayInputStream(bytes)).use { zipIn ->
                    val buffer = ByteArray(8192)
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val entryName = entry.name
                        if (!entry.isDirectory && !entryName.startsWith("__MACOSX/", ignoreCase = true)) {
                            val entryBaos = ByteArrayOutputStream()
                            var read: Int
                            while (zipIn.read(buffer).also { read = it } != -1) {
                                entryBaos.write(buffer, 0, read)
                            }
                            val rawText = entryBaos.toString(Charsets.UTF_8.name())
                                .trimStart('\uFEFF')
                                .trim()
                            val looksLikeJson = entryName.endsWith(".json", ignoreCase = true) ||
                                entryName.endsWith(".txt", ignoreCase = true) ||
                                rawText.startsWith("{") ||
                                rawText.startsWith("[")
                            if (looksLikeJson && rawText.isNotEmpty()) {
                                results.addAll(extractPackagesFromJson(rawText))
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }
        }

        if (results.isEmpty()) {
            val rawText = String(bytes, Charsets.UTF_8)
                .trimStart('\uFEFF')
                .trim()
            if (rawText.startsWith("{") || rawText.startsWith("[")) {
                results.addAll(extractPackagesFromJson(rawText))
            }
        }

        return results
    }

    private fun extractPackagesFromJson(rawJson: String): List<TripPackage> {
        val extracted = mutableListOf<TripPackage>()
        val trimmed = rawJson.trimStart('\uFEFF').trim()
        if (trimmed.isEmpty()) return emptyList()

        // Soporte para raíz JSONArray (lista directa de viajes o paquetes)
        if (trimmed.startsWith("[")) {
            runCatching {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { obj ->
                        extracted.addAll(extractPackagesFromJson(obj.toString()))
                    }
                }
            }
            return extracted
        }

        runCatching {
            val payload = parseFullBackupJson(trimmed)
            if (payload.trips.size == 1) {
                val trip = payload.trips.first()
                val parts = payload.participants.filter { it.tripId == trip.id || it.tripId.isBlank() }
                    .map { if (it.tripId.isBlank()) it.copy(tripId = trip.id) else it }
                val exps = payload.expenses.filter { it.tripId == trip.id || it.tripId.isBlank() }
                    .map { if (it.tripId.isBlank()) it.copy(tripId = trip.id) else it }
                val incs = payload.incomes.filter { it.tripId == trip.id || it.tripId.isBlank() }
                    .map { if (it.tripId.isBlank()) it.copy(tripId = trip.id) else it }
                extracted.add(TripPackage(trip, parts, exps, incs))
            } else {
                payload.trips.forEach { trip ->
                    val parts = payload.participants.filter { it.tripId == trip.id }
                    val exps = payload.expenses.filter { it.tripId == trip.id }
                    val incs = payload.incomes.filter { it.tripId == trip.id }
                    extracted.add(TripPackage(trip, parts, exps, incs))
                }
            }
        }
        return extracted
    }

    // =========================================================================
    // 1. EXPORTACIÓN CSV (Viaje individual y Listado general de viajes)
    // =========================================================================
    fun generateTripExpensesCsv(
        trip: TripEntity,
        participants: List<ParticipantEntity>,
        expenses: List<ExpenseEntity>
    ): String {
        val partMap = participants.associateBy { it.id }
        val sb = StringBuilder()
        sb.appendLine("Fecha,Lugar,Categoria,Notas,Importe,Moneda,ImporteMonedaBase,MonedaBase,TipoCambio,PagadoPor,MetodoPago,Pais,Latitud,Longitud,DiasProrrateo")
        expenses.sortedBy { it.date }.forEach { exp ->
            val payerName = if (exp.paidById == DebtCalculator.BOTE_COMUN_ID) {
                "Bote Común"
            } else {
                partMap[exp.paidById]?.name ?: "Viajero"
            }
            val row = listOf(
                escapeCsv(exp.date),
                escapeCsv(exp.placeName),
                escapeCsv(exp.category),
                escapeCsv(exp.notes),
                String.format(Locale.US, "%.2f", exp.amount),
                escapeCsv(exp.currency),
                String.format(Locale.US, "%.2f", exp.amountInBaseCurrency),
                escapeCsv(trip.baseCurrency),
                String.format(Locale.US, "%.4f", exp.exchangeRate),
                escapeCsv(payerName),
                escapeCsv(exp.paymentMethod),
                escapeCsv(exp.country),
                exp.latitude?.toString() ?: "",
                exp.longitude?.toString() ?: "",
                exp.prorationDays?.toString() ?: ""
            )
            sb.appendLine(row.joinToString(","))
        }
        return sb.toString()
    }

    fun generateAllTripsSummaryCsv(
        trips: List<TripEntity>,
        participants: List<ParticipantEntity>,
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("TituloViaje,FechaInicio,FechaFin,MonedaBase,Presupuesto,TotalGastado,TotalIngresosBote,SaldoExtra,NumParticipantes,NumGastos")
        trips.forEach { trip ->
            val tParts = participants.filter { it.tripId == trip.id }
            val tExps = expenses.filter { it.tripId == trip.id }
            val tIncs = incomes.filter { it.tripId == trip.id }
            val spent = tExps.sumOf { it.amountInBaseCurrency }
            val inc = tIncs.sumOf { it.amount }
            val extra = if (trip.budget != null && trip.budget > 0) {
                trip.budget - spent + inc
            } else {
                inc - spent
            }
            val row = listOf(
                escapeCsv(trip.title),
                escapeCsv(trip.startDate),
                escapeCsv(trip.endDate),
                escapeCsv(trip.baseCurrency),
                trip.budget?.let { String.format(Locale.US, "%.2f", it) } ?: "Sin limite",
                String.format(Locale.US, "%.2f", spent),
                String.format(Locale.US, "%.2f", inc),
                String.format(Locale.US, "%.2f", extra),
                tParts.size.toString(),
                tExps.size.toString()
            )
            sb.appendLine(row.joinToString(","))
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\r", " ").replace("\n", " ")
        return if (clean.contains(",") || clean.contains("\"") || clean.contains(";")) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }

    // =========================================================================
    // 2. IMPORTACIÓN INTELIGENTE DE CSV (TravelSpend + Mapeo Manual)
    // =========================================================================
    fun parseCsvContent(rawCsv: String): ParsedCsvSheet {
        val lines = rawCsv.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return ParsedCsvSheet(emptyList(), emptyList(), CsvColumnMapping(), false)
        }

        val firstLine = lines.first()
        val delimiter = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ';' else ','

        val parsedLines = lines.map { splitCsvLine(it, delimiter) }
        val headers = parsedLines.first()
        val dataRows = parsedLines.drop(1).filter { row -> row.any { it.isNotBlank() } }

        var dateIdx = -1
        var amountIdx = -1
        var currencyIdx = -1
        var categoryIdx = -1
        var placeIdx = -1
        var notesIdx = -1
        var paidByIdx = -1
        var methodIdx = -1
        var countryIdx = -1
        var latIdx = -1
        var lonIdx = -1

        headers.forEachIndexed { idx, h ->
            val norm = h.lowercase(Locale.ROOT).trim()
            when {
                dateIdx == -1 && (norm.contains("date") || norm.contains("fecha") || norm == "dia" || norm == "día") -> dateIdx = idx
                amountIdx == -1 && (norm == "amount" || norm == "importe" || norm == "valor" || norm == "total" || norm == "gasto" || norm == "price" || norm == "cost") -> amountIdx = idx
                currencyIdx == -1 && (norm.contains("curr") || norm == "moneda" || norm == "divisa" || norm == "iso") -> currencyIdx = idx
                categoryIdx == -1 && (norm.contains("cat") || norm == "tipo" || norm == "type") -> categoryIdx = idx
                placeIdx == -1 && (norm.contains("place") || norm.contains("lugar") || norm.contains("estab") || norm.contains("merchant") || norm.contains("title") || norm.contains("concepto") || norm.contains("comercio")) -> placeIdx = idx
                notesIdx == -1 && (norm.contains("note") || norm.contains("nota") || norm.contains("desc") || norm.contains("coment")) -> notesIdx = idx
                paidByIdx == -1 && (norm.contains("paid") || norm.contains("pagad") || norm.contains("pagó") || norm.contains("pago por") || norm.contains("payer") || norm.contains("quien") || norm.contains("autor")) -> paidByIdx = idx
                methodIdx == -1 && (norm.contains("method") || norm.contains("metodo") || norm.contains("método") || norm.contains("forma")) -> methodIdx = idx
                countryIdx == -1 && (norm.contains("country") || norm.contains("pais") || norm.contains("país")) -> countryIdx = idx
                latIdx == -1 && (norm == "lat" || norm.contains("latitud") || norm.contains("latitude")) -> latIdx = idx
                lonIdx == -1 && (norm == "lon" || norm == "lng" || norm.contains("longitud") || norm.contains("longitude")) -> lonIdx = idx
            }
        }

        // Reconoce automáticamente si tiene al menos Fecha + Importe y alguna columna descriptiva (Categoría o Lugar o Pagador)
        val isAutoRecognized = dateIdx >= 0 && amountIdx >= 0 && (categoryIdx >= 0 || placeIdx >= 0 || notesIdx >= 0)

        val mapping = CsvColumnMapping(
            dateColIndex = dateIdx,
            amountColIndex = amountIdx,
            currencyColIndex = currencyIdx,
            categoryColIndex = categoryIdx,
            placeColIndex = placeIdx,
            notesColIndex = notesIdx,
            paidByColIndex = paidByIdx,
            paymentMethodColIndex = methodIdx,
            countryColIndex = countryIdx,
            latColIndex = latIdx,
            lonColIndex = lonIdx
        )

        return ParsedCsvSheet(
            headers = headers,
            rows = dataRows,
            suggestedMapping = mapping,
            isStandardAutoDetected = isAutoRecognized
        )
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == delimiter && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    fun convertRowsToEntities(
        tripId: String,
        baseCurrency: String,
        existingParticipants: List<ParticipantEntity>,
        rows: List<List<String>>,
        mapping: CsvColumnMapping
    ): Pair<List<ParticipantEntity>, List<ExpenseEntity>> {
        val allParticipants = existingParticipants.toMutableList()
        val createdParticipants = mutableListOf<ParticipantEntity>()
        val createdExpenses = mutableListOf<ExpenseEntity>()

        fun getOrCreateParticipant(rawName: String): String {
            val clean = rawName.trim().ifEmpty { "Viajero 1" }
            if (clean.equals("Bote Común", ignoreCase = true) || clean.equals("Bote", ignoreCase = true)) {
                return DebtCalculator.BOTE_COMUN_ID
            }
            val found = allParticipants.find { it.name.equals(clean, ignoreCase = true) }
            if (found != null) return found.id
            val color = AvatarColorPalette.colors[allParticipants.size % AvatarColorPalette.colors.size]
            val newPart = ParticipantEntity(
                id = "part-${UUID.randomUUID().toString().take(8)}",
                tripId = tripId,
                name = clean,
                avatarColor = color
            )
            allParticipants.add(newPart)
            createdParticipants.add(newPart)
            return newPart.id
        }

        if (allParticipants.isEmpty()) {
            getOrCreateParticipant("Viajero principal")
        }

        rows.forEachIndexed { index, row ->
            fun col(idx: Int): String = if (idx in row.indices) row[idx].trim() else ""

            val rawAmountStr = col(mapping.amountColIndex)
                .replace("€", "")
                .replace("$", "")
                .replace(" ", "")
                .let { s ->
                    if (s.contains(",") && s.contains(".")) {
                        if (s.lastIndexOf(',') > s.lastIndexOf('.')) {
                            s.replace(".", "").replace(",", ".")
                        } else {
                            s.replace(",", "")
                        }
                    } else {
                        s.replace(",", ".")
                    }
                }

            val amount = rawAmountStr.toDoubleOrNull() ?: return@forEachIndexed
            if (amount <= 0.0) return@forEachIndexed

            val rawDate = col(mapping.dateColIndex)
            val dateIso = if (rawDate.isNotBlank()) {
                FormatUtils.parseFlexibleDateToIso(rawDate)
            } else {
                LocalDate.now().toString()
            }

            val currency = col(mapping.currencyColIndex).uppercase(Locale.ROOT).takeIf { it.length == 3 } ?: baseCurrency
            val rate = CurrencyCatalog.getDefaultExchangeRate(currency, baseCurrency)
            val amountBase = round2(amount * rate)

            val rawCat = col(mapping.categoryColIndex)
            val matchedCat = CategoryCatalog.categories.find {
                it.name.equals(rawCat, ignoreCase = true) ||
                        (rawCat.isNotBlank() && it.name.lowercase(Locale.ROOT).contains(rawCat.lowercase(Locale.ROOT)))
            }?.name ?: "General"

            val place = col(mapping.placeColIndex).ifEmpty {
                col(mapping.notesColIndex).take(35).ifEmpty { matchedCat }
            }
            val notes = col(mapping.notesColIndex)
            val country = col(mapping.countryColIndex)
            val method = col(mapping.paymentMethodColIndex).ifEmpty { "Tarjeta" }
            val payerName = col(mapping.paidByColIndex)
            val paidById = if (payerName.isNotBlank()) {
                getOrCreateParticipant(payerName)
            } else {
                allParticipants.first().id
            }

            val lat = col(mapping.latColIndex).replace(",", ".").toDoubleOrNull()
            val lon = col(mapping.lonColIndex).replace(",", ".").toDoubleOrNull()

            val expId = "exp-csv-${System.currentTimeMillis()}-$index"
            val currentPartIds = allParticipants.map { it.id }
            val count = currentPartIds.size.coerceAtLeast(1)
            val perPerson = round2(amountBase / count)
            val pct = round2(100.0 / count)
            val splits = currentPartIds.mapIndexed { sIdx, pid ->
                val splitAmt = if (sIdx == count - 1) round2(amountBase - perPerson * (count - 1)) else perPerson
                ExpenseSplit(
                    id = "$expId-s-$sIdx",
                    expenseId = expId,
                    participantId = pid,
                    amount = splitAmt,
                    percentage = pct
                )
            }

            createdExpenses.add(
                ExpenseEntity(
                    id = expId,
                    tripId = tripId,
                    amount = round2(amount),
                    currency = currency,
                    amountInBaseCurrency = amountBase,
                    exchangeRate = rate,
                    category = matchedCat,
                    date = dateIso,
                    paymentMethod = method,
                    notes = notes,
                    latitude = lat,
                    longitude = lon,
                    placeName = place,
                    country = country,
                    paidById = paidById,
                    photos = emptyList(),
                    splits = splits,
                    prorationDays = null,
                    createdAt = System.currentTimeMillis() + index
                )
            )
        }

        // Re-ajustar splits para que todos los gastos importados incluyan a todos los participantes detectados en el CSV
        val finalPartIds = allParticipants.map { it.id }
        val finalCount = finalPartIds.size.coerceAtLeast(1)
        val normalizedExpenses = createdExpenses.map { exp ->
            val perPerson = round2(exp.amountInBaseCurrency / finalCount)
            val pct = round2(100.0 / finalCount)
            val updatedSplits = finalPartIds.mapIndexed { sIdx, pid ->
                val splitAmt = if (sIdx == finalCount - 1) {
                    round2(exp.amountInBaseCurrency - perPerson * (finalCount - 1))
                } else perPerson
                ExpenseSplit(
                    id = "${exp.id}-s-$sIdx",
                    expenseId = exp.id,
                    participantId = pid,
                    amount = splitAmt,
                    percentage = pct
                )
            }
            exp.copy(splits = updatedSplits)
        }

        return createdParticipants to normalizedExpenses
    }

    // =========================================================================
    // 3. GENERACIÓN DE REPORTE PDF PROFESIONAL MULTIPÁGINA
    // =========================================================================
    fun writeTripPdfReport(
        outputStream: OutputStream,
        trip: TripEntity,
        participants: List<ParticipantEntity>,
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>
    ) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 standard points
        val pageHeight = 842
        val margin = 36f

        val debtResult = DebtCalculator.calculateDebtBalances(participants, expenses, incomes)
        val totalSpent = expenses.sumOf { it.amountInBaseCurrency }
        val totalIncome = incomes.sumOf { it.amount }
        val boteExpenses = expenses.filter { it.paidById == DebtCalculator.BOTE_COMUN_ID }.sumOf { it.amountInBaseCurrency }
        val commonPotBalance = totalIncome - boteExpenses
        val extraBalance = if (trip.budget != null && trip.budget > 0) {
            trip.budget - totalSpent + totalIncome
        } else {
            totalIncome - totalSpent
        }
        val genTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

        var pageNumber = 1
        var currentPage = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        var canvas: Canvas = currentPage.canvas
        var y = margin

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 17f
            color = Color.WHITE
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 9.5f
            color = Color.parseColor("#E2E8F0")
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12.5f
            color = Color.parseColor("#0F172A")
        }
        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 9f
            color = Color.parseColor("#1E293B")
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 8.5f
            color = Color.parseColor("#334155")
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun ensureSpace(requiredHeight: Float) {
            if (y + requiredHeight > pageHeight - margin) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                currentPage = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                canvas = currentPage.canvas
                y = margin
            }
        }

        // Encabezado principal del informe con fecha y hora de generación
        fillPaint.color = Color.parseColor("#0F172A")
        canvas.drawRoundRect(
            RectF(margin, y, pageWidth - margin, y + 74f),
            12f,
            12f,
            fillPaint
        )
        canvas.drawText("TRAVEL CASH — Reporte Financiero Completo", margin + 16f, y + 24f, titlePaint)
        canvas.drawText(
            "Viaje: ${trip.title}  |  Fechas: ${FormatUtils.formatShortDate(trip.startDate)} - ${FormatUtils.formatShortDate(trip.endDate)}  |  Moneda Base: ${trip.baseCurrency}",
            margin + 16f,
            y + 45f,
            subtitlePaint
        )
        canvas.drawText(
            "Generado: $genTime  |  Viajeros: ${participants.size}  |  Gastos registrados: ${expenses.size}",
            margin + 16f,
            y + 62f,
            subtitlePaint
        )
        y += 88f

        // 1. Resumen Financiero y Estadísticas (2 filas x 3 cajas)
        canvas.drawText("1. Resumen Financiero y Estadísticas", margin, y, sectionPaint)
        y += 12f
        val boxW = (pageWidth - margin * 2 - 16f) / 3f

        // Fila 1: Presupuesto, Gastado Total, Saldo Extra
        val row1Items = listOf(
            Triple("Presupuesto", trip.budget?.let { FormatUtils.formatCurrency(it, trip.baseCurrency) } ?: "Flexible", "#EFF6FF"),
            Triple("Gastado Total", FormatUtils.formatCurrency(totalSpent, trip.baseCurrency), "#F8FAFC"),
            Triple("SALDO EXTRA", FormatUtils.formatSignedCurrency(extraBalance, trip.baseCurrency), if (extraBalance >= 0) "#DCFCE7" else "#FEE2E2")
        )
        row1Items.forEachIndexed { idx, (label, valStr, bgHex) ->
            val left = margin + idx * (boxW + 8f)
            fillPaint.color = Color.parseColor(bgHex)
            canvas.drawRoundRect(RectF(left, y, left + boxW, y + 44f), 8f, 8f, fillPaint)
            canvas.drawText(label, left + 10f, y + 16f, bodyPaint)
            canvas.drawText(valStr, left + 10f, y + 34f, bodyBoldPaint)
        }
        y += 50f

        // Fila 2: Aportaciones Bote, Gastos con Bote, Saldo Actual del Bote
        val row2Items = listOf(
            Triple("Aportaciones Bote", "+${FormatUtils.formatCurrency(totalIncome, trip.baseCurrency)}", "#ECFDF5"),
            Triple("Gastado desde Bote", FormatUtils.formatCurrency(boteExpenses, trip.baseCurrency), "#F8FAFC"),
            Triple("SALDO ACTUAL BOTE", FormatUtils.formatSignedCurrency(commonPotBalance, trip.baseCurrency), if (commonPotBalance >= 0) "#D1FAE5" else "#FEE2E2")
        )
        row2Items.forEachIndexed { idx, (label, valStr, bgHex) ->
            val left = margin + idx * (boxW + 8f)
            fillPaint.color = Color.parseColor(bgHex)
            canvas.drawRoundRect(RectF(left, y, left + boxW, y + 44f), 8f, 8f, fillPaint)
            canvas.drawText(label, left + 10f, y + 16f, bodyPaint)
            canvas.drawText(valStr, left + 10f, y + 34f, bodyBoldPaint)
        }
        y += 56f

        // 2. Desglose por Categoría con Porcentajes
        ensureSpace(70f)
        canvas.drawText("2. Desglose por Categoría y Porcentajes", margin, y, sectionPaint)
        y += 12f

        val catStats = expenses.groupBy { it.category }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amountInBaseCurrency }
                val count = list.size
                val pct = if (totalSpent > 0) (sum / totalSpent) * 100.0 else 0.0
                Triple(cat, count, Pair(sum, pct))
            }
            .sortedByDescending { it.third.first }

        if (catStats.isEmpty()) {
            canvas.drawText("No hay gastos registrados para categorizar.", margin, y + 12f, bodyPaint)
            y += 24f
        } else {
            fillPaint.color = Color.parseColor("#E2E8F0")
            canvas.drawRect(margin, y, pageWidth - margin, y + 18f, fillPaint)
            canvas.drawText("Categoría", margin + 8f, y + 13f, bodyBoldPaint)
            canvas.drawText("Nº Gastos", margin + 200f, y + 13f, bodyBoldPaint)
            canvas.drawText("Importe Total", margin + 310f, y + 13f, bodyBoldPaint)
            canvas.drawText("% del Total", margin + 430f, y + 13f, bodyBoldPaint)
            y += 20f

            catStats.forEach { (cat, count, sumAndPct) ->
                ensureSpace(18f)
                val (sum, pct) = sumAndPct
                canvas.drawText(cat.take(28), margin + 8f, y + 12f, bodyBoldPaint)
                canvas.drawText("$count gastos", margin + 200f, y + 12f, bodyPaint)
                canvas.drawText(FormatUtils.formatCurrency(sum, trip.baseCurrency), margin + 310f, y + 12f, bodyBoldPaint)
                canvas.drawText(String.format(Locale.US, "%.1f%%", pct), margin + 430f, y + 12f, bodyBoldPaint)
                y += 17f
            }
            y += 10f
        }

        // 3. Liquidación de Cuentas (Deudas Pendientes)
        ensureSpace(70f)
        canvas.drawText("3. Liquidación de Cuentas (Deudas Pendientes)", margin, y, sectionPaint)
        y += 12f
        if (debtResult.transactions.isEmpty()) {
            canvas.drawText("No hay deudas pendientes. Todas las cuentas están saldadas al 100%.", margin, y + 12f, bodyPaint)
            y += 26f
        } else {
            debtResult.transactions.forEach { tx ->
                ensureSpace(22f)
                fillPaint.color = Color.parseColor("#F1F5F9")
                canvas.drawRoundRect(RectF(margin, y, pageWidth - margin, y + 20f), 4f, 4f, fillPaint)
                canvas.drawText(
                    "${tx.fromName}  →  paga a  →  ${tx.toName}",
                    margin + 10f,
                    y + 14f,
                    bodyBoldPaint
                )
                canvas.drawText(
                    FormatUtils.formatCurrency(tx.amount, trip.baseCurrency),
                    pageWidth - margin - 110f,
                    y + 14f,
                    bodyBoldPaint
                )
                y += 24f
            }
            y += 8f
        }

        // 4. Balance Detallado por Participante
        ensureSpace(80f)
        canvas.drawText("4. Balance Detallado por Participante", margin, y, sectionPaint)
        y += 14f
        fillPaint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(margin, y, pageWidth - margin, y + 20f, fillPaint)
        canvas.drawText("Participante", margin + 8f, y + 14f, bodyBoldPaint)
        canvas.drawText("Pagó Bolsillo", margin + 140f, y + 14f, bodyBoldPaint)
        canvas.drawText("Puso Bote", margin + 240f, y + 14f, bodyBoldPaint)
        canvas.drawText("Le Correspondía", margin + 330f, y + 14f, bodyBoldPaint)
        canvas.drawText("Balance Neto", margin + 435f, y + 14f, bodyBoldPaint)
        y += 22f

        debtResult.participantBalances.forEach { b ->
            ensureSpace(20f)
            canvas.drawText(b.participantName, margin + 8f, y + 13f, bodyBoldPaint)
            canvas.drawText(FormatUtils.formatCurrency(b.totalExpensesPaid, trip.baseCurrency), margin + 140f, y + 13f, bodyPaint)
            canvas.drawText(FormatUtils.formatCurrency(b.totalBoteContributed, trip.baseCurrency), margin + 240f, y + 13f, bodyPaint)
            canvas.drawText(FormatUtils.formatCurrency(b.totalExpensesOwed, trip.baseCurrency), margin + 330f, y + 13f, bodyPaint)
            canvas.drawText(FormatUtils.formatSignedCurrency(b.netBalance, trip.baseCurrency), margin + 435f, y + 13f, bodyBoldPaint)
            y += 19f
        }
        y += 14f

        // 5. Listado de Aportaciones al Bote Común
        if (incomes.isNotEmpty()) {
            ensureSpace(60f)
            canvas.drawText("5. Aportaciones al Bote Común (${incomes.size})", margin, y, sectionPaint)
            y += 14f
            incomes.forEach { inc ->
                ensureSpace(20f)
                canvas.drawText(
                    "${FormatUtils.formatShortDate(inc.date)} — ${inc.description.ifEmpty { "Aportación al bote" }}",
                    margin + 8f,
                    y + 13f,
                    bodyPaint
                )
                canvas.drawText(
                    "+${FormatUtils.formatCurrency(inc.amount, trip.baseCurrency)}",
                    pageWidth - margin - 100f,
                    y + 13f,
                    bodyBoldPaint
                )
                y += 18f
            }
            y += 14f
        }

        // 6. Listado Completo de Gastos
        ensureSpace(60f)
        val sectionIndex = if (incomes.isNotEmpty()) 6 else 5
        canvas.drawText("$sectionIndex. Desglose Completo de Gastos (${expenses.size})", margin, y, sectionPaint)
        y += 14f
        fillPaint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(margin, y, pageWidth - margin, y + 20f, fillPaint)
        canvas.drawText("Fecha", margin + 6f, y + 14f, bodyBoldPaint)
        canvas.drawText("Lugar / Concepto", margin + 70f, y + 14f, bodyBoldPaint)
        canvas.drawText("Categoría", margin + 245f, y + 14f, bodyBoldPaint)
        canvas.drawText("Pagó", margin + 370f, y + 14f, bodyBoldPaint)
        canvas.drawText("Importe", margin + 445f, y + 14f, bodyBoldPaint)
        y += 22f

        val partNames = participants.associate { it.id to it.name }
        expenses.sortedByDescending { it.date }.forEach { exp ->
            ensureSpace(20f)
            val payer = if (exp.paidById == DebtCalculator.BOTE_COMUN_ID) "Bote Común" else partNames[exp.paidById] ?: "-"
            val placeText = exp.placeName.ifEmpty { exp.notes.ifEmpty { exp.category } }.take(28)
            canvas.drawText(FormatUtils.formatShortDate(exp.date), margin + 6f, y + 13f, bodyPaint)
            canvas.drawText(placeText, margin + 70f, y + 13f, bodyPaint)
            canvas.drawText(exp.category.take(20), margin + 245f, y + 13f, bodyPaint)
            canvas.drawText(payer.take(14), margin + 370f, y + 13f, bodyPaint)
            canvas.drawText(FormatUtils.formatCurrency(exp.amountInBaseCurrency, trip.baseCurrency), margin + 445f, y + 13f, bodyBoldPaint)
            y += 18f
        }

        pdfDocument.finishPage(currentPage)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    /**
     * Genera el Reporte PDF del Listado General de todos los Viajes.
     */
    fun writeAllTripsSummaryPdfReport(
        outputStream: OutputStream,
        trips: List<TripEntity>,
        participants: List<ParticipantEntity>,
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>
    ) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4
        val pageHeight = 842
        val margin = 36f

        var pageNumber = 1
        var currentPage = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        var canvas: Canvas = currentPage.canvas
        var y = margin

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = Color.WHITE
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
            color = Color.parseColor("#E2E8F0")
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 13f
            color = Color.parseColor("#0F172A")
        }
        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 9.5f
            color = Color.parseColor("#1E293B")
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 9f
            color = Color.parseColor("#334155")
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun ensureSpace(requiredHeight: Float) {
            if (y + requiredHeight > pageHeight - margin) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                currentPage = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                canvas = currentPage.canvas
                y = margin
            }
        }

        // Encabezado
        fillPaint.color = Color.parseColor("#0F172A")
        canvas.drawRoundRect(RectF(margin, y, pageWidth - margin, y + 62f), 12f, 12f, fillPaint)
        canvas.drawText("TRAVEL CASH — Listado General de Viajes", margin + 16f, y + 26f, titlePaint)
        canvas.drawText(
            "Total de viajes: ${trips.size}  |  Total gastos registrados: ${expenses.size}  |  Generado: ${FormatUtils.formatShortDate(LocalDate.now().toString())}",
            margin + 16f,
            y + 46f,
            subtitlePaint
        )
        y += 78f

        val today = LocalDate.now()

        trips.forEachIndexed { idx, trip ->
            ensureSpace(105f)
            val tParts = participants.filter { it.tripId == trip.id }
            val tExps = expenses.filter { it.tripId == trip.id }
            val tIncs = incomes.filter { it.tripId == trip.id }
            val spent = tExps.sumOf { it.amountInBaseCurrency }
            val inc = tIncs.sumOf { it.amount }
            val extra = if (trip.budget != null && trip.budget > 0.0) {
                trip.budget - spent + inc
            } else {
                inc - spent
            }

            val startLocal = runCatching { LocalDate.parse(trip.startDate.take(10)) }.getOrNull()
            val endLocal = runCatching { LocalDate.parse(trip.endDate.take(10)) }.getOrNull()
            val statusStr = when {
                startLocal != null && today.isBefore(startLocal) -> "Por iniciar"
                endLocal != null && today.isAfter(endLocal) -> "Finalizado"
                else -> "En curso"
            }

            fillPaint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(RectF(margin, y, pageWidth - margin, y + 88f), 10f, 10f, fillPaint)

            canvas.drawText(
                "${idx + 1}. ${trip.title}  [$statusStr]",
                margin + 12f,
                y + 20f,
                sectionPaint
            )
            canvas.drawText(
                "Fechas: ${FormatUtils.formatShortDate(trip.startDate)} - ${FormatUtils.formatShortDate(trip.endDate)}   |   Moneda: ${trip.baseCurrency}   |   Viajeros: ${tParts.size}   |   Gastos: ${tExps.size}",
                margin + 12f,
                y + 36f,
                bodyPaint
            )

            val boxW = (pageWidth - margin * 2 - 48f) / 4f
            val metrics = listOf(
                Triple("Presupuesto", trip.budget?.let { FormatUtils.formatCurrency(it, trip.baseCurrency) } ?: "Flexible", "#EFF6FF"),
                Triple("Gastado", FormatUtils.formatCurrency(spent, trip.baseCurrency), "#F1F5F9"),
                Triple("Bote Común", "+${FormatUtils.formatCurrency(inc, trip.baseCurrency)}", "#ECFDF5"),
                Triple("Saldo Extra", FormatUtils.formatSignedCurrency(extra, trip.baseCurrency), if (extra >= 0) "#D1FAE5" else "#FEE2E2")
            )
            metrics.forEachIndexed { mIdx, (mLabel, mVal, bgHex) ->
                val left = margin + 12f + mIdx * (boxW + 8f)
                fillPaint.color = Color.parseColor(bgHex)
                canvas.drawRoundRect(RectF(left, y + 46f, left + boxW, y + 80f), 6f, 6f, fillPaint)
                canvas.drawText(mLabel, left + 8f, y + 60f, bodyPaint)
                canvas.drawText(mVal, left + 8f, y + 74f, bodyBoldPaint)
            }

            y += 100f
        }

        pdfDocument.finishPage(currentPage)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    // =========================================================================
    // 4. COPIA DE SEGURIDAD TOTAL JSON (BACKUP & RESTORE DEL ESTADO COMPLETO)
    // =========================================================================
    fun exportFullBackupJson(
        trips: List<TripEntity>,
        participants: List<ParticipantEntity>,
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>,
        activeTripId: String? = null,
        themeMode: String? = null,
        language: String? = null
    ): String {
        val root = JSONObject()
        root.put("app", "Travel Cash")
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())
        if (activeTripId != null) root.put("activeTripId", activeTripId)
        if (themeMode != null) root.put("themeMode", themeMode)
        if (language != null) root.put("language", language)

        val tripsArr = JSONArray()
        trips.forEach { t ->
            tripsArr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("startDate", t.startDate)
                    put("endDate", t.endDate)
                    if (t.budget != null) put("budget", t.budget)
                    put("baseCurrency", t.baseCurrency)
                    if (t.headerPhotoUri != null) put("headerPhotoUri", t.headerPhotoUri)
                    put("createdAt", t.createdAt)
                }
            )
        }
        root.put("trips", tripsArr)

        val partsArr = JSONArray()
        participants.forEach { p ->
            partsArr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("tripId", p.tripId)
                    put("name", p.name)
                    put("avatarColor", p.avatarColor)
                    if (p.photoUri != null) put("photoUri", p.photoUri)
                }
            )
        }
        root.put("participants", partsArr)

        val expsArr = JSONArray()
        expenses.forEach { e ->
            val splitsArr = JSONArray()
            e.splits.forEach { s ->
                splitsArr.put(
                    JSONObject().apply {
                        put("id", s.id)
                        put("expenseId", s.expenseId)
                        put("participantId", s.participantId)
                        put("amount", s.amount)
                        put("percentage", s.percentage)
                    }
                )
            }
            val photosArr = JSONArray()
            e.photos.forEach { photosArr.put(it) }

            expsArr.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("tripId", e.tripId)
                    put("amount", e.amount)
                    put("currency", e.currency)
                    put("amountInBaseCurrency", e.amountInBaseCurrency)
                    put("exchangeRate", e.exchangeRate)
                    put("category", e.category)
                    put("date", e.date)
                    put("paymentMethod", e.paymentMethod)
                    put("notes", e.notes)
                    if (e.latitude != null) put("latitude", e.latitude)
                    if (e.longitude != null) put("longitude", e.longitude)
                    put("placeName", e.placeName)
                    put("country", e.country)
                    put("originName", e.originName)
                    if (e.originLatitude != null) put("originLatitude", e.originLatitude)
                    if (e.originLongitude != null) put("originLongitude", e.originLongitude)
                    put("paidById", e.paidById)
                    put("photos", photosArr)
                    put("splits", splitsArr)
                    if (e.prorationDays != null) put("prorationDays", e.prorationDays)
                    put("createdAt", e.createdAt)
                }
            )
        }
        root.put("expenses", expsArr)

        val incsArr = JSONArray()
        incomes.forEach { inc ->
            val contribsArr = JSONArray()
            inc.contributions.forEach { c ->
                contribsArr.put(
                    JSONObject().apply {
                        put("participantId", c.participantId)
                        put("amount", c.amount)
                        if (c.debtDiscount != null) put("debtDiscount", c.debtDiscount)
                    }
                )
            }
            incsArr.put(
                JSONObject().apply {
                    put("id", inc.id)
                    put("tripId", inc.tripId)
                    put("amount", inc.amount)
                    put("date", inc.date)
                    put("description", inc.description)
                    if (inc.basePerPerson != null) put("basePerPerson", inc.basePerPerson)
                    put("calculationMethod", inc.calculationMethod)
                    put("contributions", contribsArr)
                }
            )
        }
        root.put("incomes", incsArr)

        return root.toString(2)
    }

    fun parseFullBackupJson(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString.trimStart('\uFEFF').trim())
        val dataObj = root.optJSONObject("data") ?: root

        val activeTripId = dataObj.optString("activeTripId", "").takeIf { it.isNotBlank() }
        val themeMode = dataObj.optString("themeMode", "").takeIf { it.isNotBlank() }
        val language = dataObj.optString("language", "").takeIf { it.isNotBlank() }

        val trips = mutableListOf<TripEntity>()
        val participants = mutableListOf<ParticipantEntity>()
        val expenses = mutableListOf<ExpenseEntity>()
        val incomes = mutableListOf<IncomeEntity>()

        fun parseParticipantObj(o: JSONObject, fallbackTripId: String = ""): ParticipantEntity {
            return ParticipantEntity(
                id = o.optString("id").ifBlank { "part-${UUID.randomUUID().toString().take(8)}" },
                tripId = o.optString("tripId").ifBlank { fallbackTripId },
                name = o.optString("name", "Viajero"),
                avatarColor = o.optString("avatarColor", o.optString("color", "#0066FF")),
                photoUri = if (o.has("photoUri") && !o.isNull("photoUri")) o.optString("photoUri") else null
            )
        }

        fun parseExpenseObj(o: JSONObject, fallbackTripId: String = ""): ExpenseEntity {
            val expId = o.optString("id").ifBlank { "exp-${UUID.randomUUID().toString().take(8)}" }
            val splits = mutableListOf<ExpenseSplit>()
            val sArr = o.optJSONArray("splits") ?: JSONArray()
            for (j in 0 until sArr.length()) {
                val so = sArr.optJSONObject(j) ?: continue
                splits.add(
                    ExpenseSplit(
                        id = so.optString("id").ifBlank { "$expId-s-$j" },
                        expenseId = so.optString("expenseId").ifBlank { expId },
                        participantId = so.optString("participantId"),
                        amount = so.optDouble("amount", 0.0),
                        percentage = so.optDouble("percentage", 0.0)
                    )
                )
            }
            val photos = mutableListOf<String>()
            val pArr = o.optJSONArray("photos") ?: JSONArray()
            for (j in 0 until pArr.length()) {
                photos.add(pArr.optString(j))
            }

            val amt = o.optDouble("amount", 0.0)
            val rate = o.optDouble("exchangeRate", 1.0)
            val amtBase = if (o.has("amountInBaseCurrency")) {
                o.optDouble("amountInBaseCurrency", amt)
            } else {
                round2(amt * rate)
            }

            return ExpenseEntity(
                id = expId,
                tripId = o.optString("tripId").ifBlank { fallbackTripId },
                amount = amt,
                currency = o.optString("currency", "EUR"),
                amountInBaseCurrency = amtBase,
                exchangeRate = rate,
                category = o.optString("category", "General"),
                date = o.optString("date", LocalDate.now().toString()),
                paymentMethod = o.optString("paymentMethod", "Tarjeta"),
                notes = o.optString("notes", ""),
                latitude = if (o.has("latitude") && !o.isNull("latitude")) o.optDouble("latitude") else null,
                longitude = if (o.has("longitude") && !o.isNull("longitude")) o.optDouble("longitude") else null,
                placeName = o.optString("placeName", ""),
                country = o.optString("country", ""),
                originName = o.optString("originName", ""),
                originLatitude = if (o.has("originLatitude") && !o.isNull("originLatitude")) o.optDouble("originLatitude") else null,
                originLongitude = if (o.has("originLongitude") && !o.isNull("originLongitude")) o.optDouble("originLongitude") else null,
                paidById = o.optString("paidById", ""),
                photos = photos,
                splits = splits,
                prorationDays = if (o.has("prorationDays") && !o.isNull("prorationDays")) o.optInt("prorationDays") else null,
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        }

        fun parseIncomeObj(o: JSONObject, fallbackTripId: String = ""): IncomeEntity {
            val contribs = mutableListOf<IncomeContribution>()
            val cArr = o.optJSONArray("contributions") ?: JSONArray()
            for (j in 0 until cArr.length()) {
                val co = cArr.optJSONObject(j) ?: continue
                contribs.add(
                    IncomeContribution(
                        participantId = co.optString("participantId"),
                        amount = co.optDouble("amount", 0.0),
                        debtDiscount = if (co.has("debtDiscount") && !co.isNull("debtDiscount")) co.optDouble("debtDiscount") else null
                    )
                )
            }
            return IncomeEntity(
                id = o.optString("id").ifBlank { "inc-${UUID.randomUUID().toString().take(8)}" },
                tripId = o.optString("tripId").ifBlank { fallbackTripId },
                amount = o.optDouble("amount", 0.0),
                date = o.optString("date", LocalDate.now().toString()),
                description = o.optString("description", ""),
                basePerPerson = if (o.has("basePerPerson") && !o.isNull("basePerPerson")) o.optDouble("basePerPerson") else null,
                calculationMethod = o.optString("calculationMethod", "perPerson"),
                contributions = contribs
            )
        }

        val tripsArr = dataObj.optJSONArray("trips") ?: JSONArray().apply {
            val singleTrip = dataObj.optJSONObject("trip")
            if (singleTrip != null) {
                put(singleTrip)
            } else if (dataObj.has("title") || dataObj.has("name")) {
                put(dataObj)
            }
        }
        for (i in 0 until tripsArr.length()) {
            val o = tripsArr.optJSONObject(i) ?: continue
            val tId = o.optString("id").ifBlank { "trip-${UUID.randomUUID().toString().take(8)}" }
            trips.add(
                TripEntity(
                    id = tId,
                    title = o.optString("title", o.optString("name", "Viaje")),
                    startDate = o.optString("startDate", LocalDate.now().toString()),
                    endDate = o.optString("endDate", LocalDate.now().toString()),
                    budget = if (o.has("budget") && !o.isNull("budget")) o.optDouble("budget") else null,
                    baseCurrency = o.optString("baseCurrency", o.optString("currency", "EUR")),
                    headerPhotoUri = if (o.has("headerPhotoUri") && !o.isNull("headerPhotoUri")) o.optString("headerPhotoUri") else null,
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
            // Soporte adicional por si el JSON trae participantes, gastos o ingresos anidados dentro de cada viaje
            val nestedParts = o.optJSONArray("participants")
            if (nestedParts != null) {
                for (k in 0 until nestedParts.length()) {
                    nestedParts.optJSONObject(k)?.let { participants.add(parseParticipantObj(it, tId)) }
                }
            }
            val nestedExps = o.optJSONArray("expenses")
            if (nestedExps != null) {
                for (k in 0 until nestedExps.length()) {
                    nestedExps.optJSONObject(k)?.let { expenses.add(parseExpenseObj(it, tId)) }
                }
            }
            val nestedIncs = o.optJSONArray("incomes")
            if (nestedIncs != null) {
                for (k in 0 until nestedIncs.length()) {
                    nestedIncs.optJSONObject(k)?.let { incomes.add(parseIncomeObj(it, tId)) }
                }
            }
        }

        val partsArr = dataObj.optJSONArray("participants") ?: JSONArray()
        for (i in 0 until partsArr.length()) {
            partsArr.optJSONObject(i)?.let { participants.add(parseParticipantObj(it)) }
        }

        val expsArr = dataObj.optJSONArray("expenses") ?: JSONArray()
        for (i in 0 until expsArr.length()) {
            expsArr.optJSONObject(i)?.let { expenses.add(parseExpenseObj(it)) }
        }

        val incsArr = dataObj.optJSONArray("incomes") ?: JSONArray()
        for (i in 0 until incsArr.length()) {
            incsArr.optJSONObject(i)?.let { incomes.add(parseIncomeObj(it)) }
        }

        return BackupPayload(
            trips = trips,
            participants = participants,
            expenses = expenses,
            incomes = incomes,
            activeTripId = activeTripId,
            themeMode = themeMode,
            language = language
        )
    }

    /**
     * Extrae gastos desde un archivo PDF (por ejemplo reportes PDF de Travel Cash o extractos con fecha e importe)
     * o adjunta el documento PDF importado como gasto/registro en el viaje activo.
     */
    fun parseExpensesFromPdfBytes(
        pdfBytes: ByteArray,
        tripId: String,
        baseCurrency: String,
        existingParticipants: List<ParticipantEntity>
    ): List<ExpenseEntity> {
        val rawText = String(pdfBytes, Charsets.ISO_8859_1)
        val dateAmountRegex = Regex("(\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2}).*?(\\d+[.,]\\d{2})")
        val matches = dateAmountRegex.findAll(rawText).toList()

        val payerId = existingParticipants.firstOrNull()?.id ?: DebtCalculator.BOTE_COMUN_ID
        val partIds = existingParticipants.map { it.id }
        val count = partIds.size.coerceAtLeast(1)

        val extracted = mutableListOf<ExpenseEntity>()
        matches.take(40).forEachIndexed { idx, match ->
            val rawDate = match.groupValues[1]
            val rawAmt = match.groupValues[2].replace(",", ".")
            val amount = rawAmt.toDoubleOrNull() ?: return@forEachIndexed
            if (amount <= 0.0) return@forEachIndexed

            val expId = "exp-pdf-${System.currentTimeMillis()}-$idx"
            val perPerson = round2(amount / count)
            val pct = round2(100.0 / count)
            val splits = partIds.mapIndexed { sIdx, pid ->
                val splitAmt = if (sIdx == count - 1) round2(amount - perPerson * (count - 1)) else perPerson
                ExpenseSplit(
                    id = "$expId-s-$sIdx",
                    expenseId = expId,
                    participantId = pid,
                    amount = splitAmt,
                    percentage = pct
                )
            }
            extracted.add(
                ExpenseEntity(
                    id = expId,
                    tripId = tripId,
                    amount = amount,
                    currency = baseCurrency,
                    amountInBaseCurrency = amount,
                    exchangeRate = 1.0,
                    category = "General",
                    date = FormatUtils.parseFlexibleDateToIso(rawDate),
                    paymentMethod = "Tarjeta",
                    notes = "Importado desde documento PDF",
                    placeName = "Gasto PDF #${idx + 1}",
                    paidById = payerId,
                    splits = splits,
                    createdAt = System.currentTimeMillis() + idx
                )
            )
        }
        return extracted
    }
}
