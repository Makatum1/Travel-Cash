package com.example.ui.modals

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ParticipantEntity
import com.example.data.model.TripEntity
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.isEnglish
import com.example.ui.theme.OceanNavy
import com.example.ui.theme.TravelBlue
import com.example.util.CsvColumnMapping
import com.example.util.CurrencyCatalog
import com.example.util.ExportImportManager
import com.example.util.FormatUtils
import com.example.util.ParsedCsvSheet
import java.time.LocalDate
import java.util.Locale

@Composable
private fun ModalLightSurfaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            surface = Color.White,
            onSurface = Color(0xFF475569),
            onSurfaceVariant = Color(0xFF64748B),
            outline = Color(0xFFCBD5E1)
        ),
        content = content
    )
}

// =========================================================================
// 1. MODAL PARA CREAR O EDITAR VIAJE
// =========================================================================
@Composable
fun CreateTripModal(
    existingTrip: TripEntity? = null,
    existingParticipants: List<ParticipantEntity> = emptyList(),
    isFirstTrip: Boolean = false,
    onCreateTrip: (
        title: String,
        startDate: String,
        endDate: String,
        budget: Double?,
        baseCurrency: String,
        participantNames: List<String>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEn = LocalAppLanguage.current.isEnglish()
    val isEdit = existingTrip != null

    var title by remember { mutableStateOf(existingTrip?.title ?: "") }
    var startDate by remember { mutableStateOf(existingTrip?.startDate ?: LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf(existingTrip?.endDate ?: LocalDate.now().plusDays(7).toString()) }
    var budgetText by remember {
        mutableStateOf(
            existingTrip?.budget?.takeIf { it > 0.0 }?.let { String.format(Locale.US, "%.2f", it) } ?: ""
        )
    }
    var baseCurrency by remember { mutableStateOf(existingTrip?.baseCurrency ?: "EUR") }
    var currencyExpanded by remember { mutableStateOf(false) }

    val participantNames = remember {
        val initialNames = if (existingParticipants.isNotEmpty()) {
            existingParticipants.map { it.name }
        } else {
            emptyList<String>()
        }
        mutableStateListOf(*initialNames.toTypedArray())
    }
    var newPersonInput by remember { mutableStateOf("") }

    val titleFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        try {
            titleFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        ModalLightSurfaceTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(top = 16.dp, bottom = 16.dp)
                .imePadding(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OceanNavy)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEdit) {
                            if (isEn) "✏️ Edit Trip" else "✏️ Editar Viaje"
                        } else {
                            if (isEn) "✈️ Create New Trip" else "✈️ Crear Nuevo Viaje"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = if (isEn) "Close" else "Cerrar",
                            tint = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isFirstTrip) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.5.dp, TravelBlue.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = TravelBlue,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("✈️", fontSize = 22.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEn) "Welcome to Travel Cash!" else "¡Bienvenido a Travel Cash!",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = OceanNavy
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isEn)
                                            "Create your trip to start using the application."
                                        else
                                            "Bienvenido a travel cash crea tu viaje para empezar a utilizar la aplicación.",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = {
                            Text(
                                if (isEn) "Trip Name (e.g. Route through Iceland)" else "Nombre del Viaje (ej. Ruta por Islandia)"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(titleFocusRequester)
                            .testTag("trip_title_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                val d = LocalDate.parse(startDate)
                                DatePickerDialog(
                                    context,
                                    { _, y, m, day ->
                                        startDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, day)
                                    },
                                    d.year,
                                    d.monthValue - 1,
                                    d.dayOfMonth
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isEn) "Start date" else "Fecha inicio",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                                Text(FormatUtils.formatShortDate(startDate), fontWeight = FontWeight.Bold, color = OceanNavy)
                            }
                        }

                        Surface(
                            onClick = {
                                val d = LocalDate.parse(endDate)
                                DatePickerDialog(
                                    context,
                                    { _, y, m, day ->
                                        endDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, day)
                                    },
                                    d.year,
                                    d.monthValue - 1,
                                    d.dayOfMonth
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isEn) "End date" else "Fecha fin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                                Text(FormatUtils.formatShortDate(endDate), fontWeight = FontWeight.Bold, color = OceanNavy)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = budgetText,
                            onValueChange = { budgetText = it },
                            label = { Text(if (isEn) "Optional budget" else "Presupuesto opcional") },
                            placeholder = { Text(if (isEn) "No limit if empty" else "Sin límite si vacío") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { currencyExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (isEn) {
                                        "Currency: $baseCurrency (${CurrencyCatalog.getSymbol(baseCurrency)})"
                                    } else {
                                        "Moneda: $baseCurrency (${CurrencyCatalog.getSymbol(baseCurrency)})"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            DropdownMenu(
                                expanded = currencyExpanded,
                                onDismissRequest = { currencyExpanded = false }
                            ) {
                                CurrencyCatalog.currencies.forEach { curr ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${curr.code} - ${CurrencyCatalog.getDisplayName(curr, isEn)} (${curr.symbol})")
                                        },
                                        onClick = {
                                            baseCurrency = curr.code
                                            currencyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isEdit) {
                            if (isEn) "Trip participants" else "Participantes del viaje"
                        } else {
                            if (isEn) "Initial trip participants" else "Participantes iniciales del viaje"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPersonInput,
                            onValueChange = { newPersonInput = it },
                            placeholder = { Text(if (isEn) "Add traveler..." else "Añadir viajero...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newPersonInput.trim().isNotEmpty()) {
                                    participantNames.add(newPersonInput.trim())
                                    newPersonInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TravelBlue)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        participantNames.forEachIndexed { idx, name ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "👤 $name",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = OceanNavy
                                    )
                                    IconButton(
                                        onClick = { participantNames.removeAt(idx) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = if (isEn) "Remove" else "Quitar",
                                            tint = CoralRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Espacio generoso para que cualquier campo o botón se pueda scrollear holgadamente por encima del teclado
                    Spacer(modifier = Modifier.height(180.dp))
                }

                // Footer con botones Cancelar y Crear/Guardar fijado por encima del teclado
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(if (isEn) "Cancel" else "Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val budgetVal = budgetText.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0.0 }
                            val finalParticipants = participantNames.toMutableList()
                            if (newPersonInput.trim().isNotEmpty() && !finalParticipants.contains(newPersonInput.trim())) {
                                finalParticipants.add(newPersonInput.trim())
                            }
                            if (finalParticipants.isEmpty()) {
                                finalParticipants.add(if (isEn) "Traveler 1" else "Viajero 1")
                            }
                            onCreateTrip(
                                title.ifBlank { "Viaje ${LocalDate.now().year}" },
                                startDate,
                                endDate,
                                budgetVal,
                                baseCurrency,
                                finalParticipants.toList()
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TravelBlue),
                        modifier = Modifier.testTag("confirm_create_trip_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEdit) {
                                if (isEn) "Save Changes" else "Guardar Cambios"
                            } else {
                                if (isEn) "Create Trip" else "Crear Viaje"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        }
    }
}

// =========================================================================
// 2. MODAL DE MAPEO MANUAL DE COLUMNAS CSV CON VISTA PREVIA DE FILAS
// =========================================================================
@Composable
fun CsvColumnMappingModal(
    sheet: ParsedCsvSheet,
    createNewTrip: Boolean,
    initialTripTitle: String,
    onConfirmMapping: (CsvColumnMapping, String) -> Unit,
    onDismiss: () -> Unit
) {
    val isEn = LocalAppLanguage.current.isEnglish()
    var tripTitle by remember { mutableStateOf(initialTripTitle) }

    var dateCol by remember { mutableIntStateOf(sheet.suggestedMapping.dateColIndex) }
    var amountCol by remember { mutableIntStateOf(sheet.suggestedMapping.amountColIndex.coerceAtLeast(0)) }
    var currencyCol by remember { mutableIntStateOf(sheet.suggestedMapping.currencyColIndex) }
    var categoryCol by remember { mutableIntStateOf(sheet.suggestedMapping.categoryColIndex) }
    var placeCol by remember { mutableIntStateOf(sheet.suggestedMapping.placeColIndex) }
    var paidByCol by remember { mutableIntStateOf(sheet.suggestedMapping.paidByColIndex) }
    var notesCol by remember { mutableIntStateOf(sheet.suggestedMapping.notesColIndex) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ModalLightSurfaceTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .padding(top = 16.dp, bottom = 64.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OceanNavy)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEn) "📥 Manual CSV Column Mapping" else "📥 Mapeo Manual de Columnas CSV",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = if (isEn) "Close" else "Cerrar",
                            tint = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isEn) {
                            "Custom columns detected in your CSV file. Assign each field to its corresponding column:"
                        } else {
                            "Hemos detectado columnas personalizadas en tu archivo CSV. Asigna a qué campo corresponde cada columna:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )

                    if (createNewTrip) {
                        OutlinedTextField(
                            value = tripTitle,
                            onValueChange = { tripTitle = it },
                            label = {
                                Text(
                                    if (isEn) "Title for the new imported trip" else "Título para el nuevo viaje importado"
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ColumnSelectorRow(
                        label = if (isEn) "Amount Column (Required)" else "Columna de Importe (Obligatoria)",
                        headers = sheet.headers,
                        selectedIndex = amountCol,
                        allowNone = false,
                        isEn = isEn
                    ) { amountCol = it }
                    ColumnSelectorRow(
                        label = if (isEn) "Date Column" else "Columna de Fecha",
                        headers = sheet.headers,
                        selectedIndex = dateCol,
                        allowNone = true,
                        isEn = isEn
                    ) { dateCol = it }
                    ColumnSelectorRow(
                        label = if (isEn) "Place / Venue Column" else "Columna de Lugar / Establecimiento",
                        headers = sheet.headers,
                        selectedIndex = placeCol,
                        allowNone = true,
                        isEn = isEn
                    ) { placeCol = it }
                    ColumnSelectorRow(
                        label = if (isEn) "Category Column" else "Columna de Categoría",
                        headers = sheet.headers,
                        selectedIndex = categoryCol,
                        allowNone = true,
                        isEn = isEn
                    ) { categoryCol = it }
                    ColumnSelectorRow(
                        label = if (isEn) "Payer Column (Who paid)" else "Columna de Pagador (Quién pagó)",
                        headers = sheet.headers,
                        selectedIndex = paidByCol,
                        allowNone = true,
                        isEn = isEn
                    ) { paidByCol = it }
                    ColumnSelectorRow(
                        label = if (isEn) "Currency Column (ISO)" else "Columna de Moneda (ISO)",
                        headers = sheet.headers,
                        selectedIndex = currencyCol,
                        allowNone = true,
                        isEn = isEn
                    ) { currencyCol = it }
                    ColumnSelectorRow(
                        label = if (isEn) "Notes Column" else "Columna de Notas",
                        headers = sheet.headers,
                        selectedIndex = notesCol,
                        allowNone = true,
                        isEn = isEn
                    ) { notesCol = it }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isEn) {
                            "Preview of first rows (${sheet.rows.size} total rows):"
                        } else {
                            "Vista previa de las primeras filas (${sheet.rows.size} filas en total):"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy
                    )

                    sheet.rows.take(4).forEachIndexed { idx, row ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${if (isEn) "Row" else "Fila"} ${idx + 1}: ${row.joinToString(" | ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OceanNavy,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(if (isEn) "Cancel" else "Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirmMapping(
                                CsvColumnMapping(
                                    dateColIndex = dateCol,
                                    amountColIndex = amountCol,
                                    currencyColIndex = currencyCol,
                                    categoryColIndex = categoryCol,
                                    placeColIndex = placeCol,
                                    paidByColIndex = paidByCol,
                                    notesColIndex = notesCol
                                ),
                                tripTitle
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenDark)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEn) "Import ${sheet.rows.size} rows" else "Importar ${sheet.rows.size} filas",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ColumnSelectorRow(
    label: String,
    headers: List<String>,
    selectedIndex: Int,
    allowNone: Boolean,
    isEn: Boolean,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val noneText = if (isEn) "(Do not assign)" else "(No asignar)"
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = OceanNavy,
            modifier = Modifier.weight(1f)
        )
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                val text = if (selectedIndex in headers.indices) {
                    headers[selectedIndex]
                } else {
                    noneText
                }
                Text(text)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (allowNone) {
                    DropdownMenuItem(
                        text = { Text(noneText) },
                        onClick = {
                            onSelect(-1)
                            expanded = false
                        }
                    )
                }
                headers.forEachIndexed { idx, h ->
                    DropdownMenuItem(
                        text = { Text("Col ${idx + 1}: $h") },
                        onClick = {
                            onSelect(idx)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// =========================================================================
// 3. MODAL DE COPIA DE SEGURIDAD TOTAL (BACKUP & RESTORE JSON COMPLETO)
// =========================================================================
@Composable
fun BackupRestoreModal(
    tripCount: Int,
    participantCount: Int,
    expenseCount: Int,
    incomeCount: Int,
    onGenerateBackupJson: () -> String,
    onExportBackupFileClick: () -> Unit,
    onRestoreBackupFileClick: (replaceAll: Boolean) -> Unit,
    onRestoreBackupRawJson: (jsonContent: String, replaceAll: Boolean) -> Unit,
    onShowToast: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEn = LocalAppLanguage.current.isEnglish()

    var replaceAllMode by remember { mutableStateOf(false) }
    var showRawJsonEditor by remember { mutableStateOf(false) }
    var rawJsonText by remember { mutableStateOf("") }

    // Previsualización en vivo del JSON pegado o cargado en el área de texto
    val parsedPreview = remember(rawJsonText) {
        if (rawJsonText.isBlank()) null else {
            runCatching { ExportImportManager.parseFullBackupJson(rawJsonText) }.getOrNull()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ModalLightSurfaceTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(top = 16.dp, bottom = 64.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = TravelBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEn) "JSON Backup & Restore" else "Copia de Seguridad JSON",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = OceanNavy
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = if (isEn) "Close" else "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isEn) {
                        "Current device data: $tripCount trips, $participantCount participants, $expenseCount expenses & $incomeCount pot contributions."
                    } else {
                        "Datos actuales en dispositivo: $tripCount viajes, $participantCount participantes, $expenseCount gastos y $incomeCount aportaciones al bote."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Exportar Estado Completo (.json)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, TravelBlue.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isEn) "1. Export Full App State (.json)" else "1. Exportar Estado Completo (.json)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = OceanNavy
                        )
                        Text(
                            text = if (isEn) {
                                "Saves all trips, participants, expenses, photos, pot incomes and preferences into a JSON backup."
                            } else {
                                "Guarda todos tus viajes, participantes, gastos, fotos, ingresos del bote y ajustes en un archivo JSON."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onExportBackupFileClick()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TravelBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_json_backup_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEn) "Export Backup File (.json)" else "Exportar Backup (.json)",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val json = onGenerateBackupJson()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("travel_cash_backup", json))
                                    onShowToast(
                                        if (isEn) "JSON backup copied to clipboard" else "JSON de copia de seguridad copiado al portapapeles"
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isEn) "Copy JSON" else "Copiar JSON",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    rawJsonText = onGenerateBackupJson()
                                    showRawJsonEditor = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isEn) "View / Edit JSON" else "Ver / Pegar JSON",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Importar / Restaurar Backup (.json) fusionando o reemplazando datos
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, EmeraldGreenDark.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isEn) "2. Import / Restore Backup (.json)" else "2. Importar / Restaurar Backup (.json)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = OceanNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = !replaceAllMode,
                                onClick = { replaceAllMode = false }
                            )
                            Column {
                                Text(
                                    text = if (isEn) "Merge with current data" else "Fusionar con datos actuales",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = OceanNavy
                                )
                                Text(
                                    text = if (isEn) {
                                        "Keeps current trips and adds or updates those from the backup."
                                    } else {
                                        "Conserva tus viajes actuales y añade o actualiza los del archivo."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = replaceAllMode,
                                onClick = { replaceAllMode = true }
                            )
                            Column {
                                Text(
                                    text = if (isEn) "Replace all data" else "Reemplazar todo",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = CoralRed
                                )
                                Text(
                                    text = if (isEn) {
                                        "Deletes current data before restoring the backup."
                                    } else {
                                        "Borra los datos actuales antes de restaurar la copia."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onRestoreBackupFileClick(replaceAllMode)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("restore_json_backup_button")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEn) {
                                    "Select .json File & Restore"
                                } else {
                                    "Seleccionar archivo .json y Restaurar"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        AnimatedVisibility(visible = showRawJsonEditor) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEn) "JSON Content:" else "Contenido JSON:",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = OceanNavy
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            val pasted = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                            if (pasted.isNotBlank()) {
                                                rawJsonText = pasted
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isEn) "Paste" else "Pegar",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = rawJsonText,
                                    onValueChange = { rawJsonText = it },
                                    placeholder = {
                                        Text(
                                            if (isEn) "Paste your backup JSON here..." else "Pega aquí el contenido JSON de tu copia..."
                                        )
                                    },
                                    maxLines = 6,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("raw_json_backup_input")
                                )

                                if (parsedPreview != null && parsedPreview.trips.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, EmeraldGreenDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isEn) {
                                                "✓ Valid JSON detected: ${parsedPreview.trips.size} trips, ${parsedPreview.participants.size} participants, ${parsedPreview.expenses.size} expenses, ${parsedPreview.incomes.size} pot incomes."
                                            } else {
                                                "✓ JSON válido detectado: ${parsedPreview.trips.size} viajes, ${parsedPreview.participants.size} participantes, ${parsedPreview.expenses.size} gastos y ${parsedPreview.incomes.size} ingresos."
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = EmeraldGreenDark,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        onRestoreBackupRawJson(rawJsonText, replaceAllMode)
                                        onDismiss()
                                    },
                                    enabled = rawJsonText.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = OceanNavy),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("restore_raw_json_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEn) "Restore from Pasted JSON" else "Restaurar desde texto JSON",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

// =========================================================================
// 4. MODAL DE SUGERENCIAS Y GUÍA RÁPIDA DE TRAVEL CASH
// =========================================================================
@Composable
fun SuggestionsModal(onDismiss: () -> Unit) {
    val isEn = LocalAppLanguage.current.isEnglish()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        ModalLightSurfaceTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 64.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEn) "Travel Cash Tips" else "Sugerencias Travel Cash",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = OceanNavy
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = if (isEn) "Close" else "Cerrar")
                    }
                }

                // Correo electrónico de contacto/sugerencias arriba del todo
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:sugerenciastravelcash@gmail.com")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, if (isEn) "Travel Cash Suggestions" else "Sugerencias Travel Cash")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Email", "sugerenciastravelcash@gmail.com")
                            clipboard.setPrimaryClip(clip)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.5.dp, TravelBlue.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("suggestions_email_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TravelBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEn) "Questions & suggestions email:" else "Correo para dudas y sugerencias:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "sugerenciastravelcash@gmail.com",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                color = TravelBlue
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = if (isEn) "Send email" else "Enviar correo",
                            tint = TravelBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuggestionItem(
                        title = if (isEn) "🏺 Common Pot Golden Rule" else "🏺 Regla de Oro del Bote Común",
                        body = if (isEn) {
                            "When adding a Pot Income in 'Amount per person' mode, if any traveler paid expenses out of pocket, the system automatically deducts what is owed to them from their pot share."
                        } else {
                            "Al pulsar '+ Ingreso (Bote)' en modo 'Cuantía por persona', si algún viajero adelantó pagos de su bolsillo, el sistema descuenta automáticamente lo que se le debe de su cuota de bote."
                        }
                    )
                    SuggestionItem(
                        title = if (isEn) "📍 GPS Proximity with OpenStreetMap" else "📍 Proximidad GPS con OpenStreetMap",
                        body = if (isEn) {
                            "When adding an expense, type the restaurant or shop name: the app queries Nominatim with your current GPS and sorts the closest venues first, filling in the address automatically."
                        } else {
                            "Al añadir un gasto, escribe el nombre del restaurante o comercio: la app consulta Nominatim con tu GPS actual, ordena primero los más cercanos y rellena la dirección."
                        }
                    )
                    SuggestionItem(
                        title = if (isEn) "📊 Interactive Data & Splits" else "📊 Datos y Cuentas Interactivas",
                        body = if (isEn) {
                            "In the 'Data' and 'Splits' tabs, tap on any category, person or settlement payment to expand its full mathematical breakdown."
                        } else {
                            "En las pestañas 'Datos' y 'Cuentas', pulsa sobre cualquier categoría, persona o pago de liquidación para desplegar su desglose matemático completo."
                        }
                    )
                    SuggestionItem(
                        title = if (isEn) "⚙️ Export and Import in Settings" else "⚙️ Exportar e Importar en Ajustes",
                        body = if (isEn) {
                            "In the Settings tab you can export and import your trips in compressed .zip format with JSON files."
                        } else {
                            "En la pestaña Ajustes puedes exportar e importar tus viajes en formato comprimido .zip con archivos JSON."
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = OceanNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isEn) "Got it" else "Entendido", fontWeight = FontWeight.Bold)
                }
            }
        }
        }
    }
}

@Composable
private fun SuggestionItem(title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = OceanNavy
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569)
            )
        }
    }
}
