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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.OceanNavy
import com.example.ui.theme.TravelBlue
import com.example.util.CsvColumnMapping
import com.example.util.CurrencyCatalog
import com.example.util.ExportImportManager
import com.example.util.FormatUtils
import com.example.util.ParsedCsvSheet
import com.example.util.UiTexts
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
    val currentLang = LocalAppLanguage.current
    val texts = UiTexts.get(currentLang)
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
                        text = if (isEdit) "✏️ ${texts.editTripTitle}" else "✈️ ${texts.newTripTitle}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = texts.closeBtn,
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
                                    val welcomeTitle = when (currentLang) {
                                        AppLanguage.ES -> "¡Bienvenido a Travel Cash!"
                                        AppLanguage.EN -> "Welcome to Travel Cash!"
                                        AppLanguage.FR -> "Bienvenue sur Travel Cash !"
                                        AppLanguage.IT -> "Benvenuto su Travel Cash!"
                                        AppLanguage.ZH -> "欢迎使用 Travel Cash！"
                                        AppLanguage.NL -> "Welkom bij Travel Cash!"
                                        AppLanguage.DE -> "Willkommen bei Travel Cash!"
                                        AppLanguage.PL -> "Witamy w Travel Cash!"
                                    }
                                    val welcomeSubtitle = when (currentLang) {
                                        AppLanguage.ES -> "Bienvenido a travel cash crea tu viaje para empezar a utilizar la aplicación."
                                        AppLanguage.EN -> "Create your trip to start using the application."
                                        AppLanguage.FR -> "Créez votre voyage pour commencer à utiliser l'application."
                                        AppLanguage.IT -> "Crea il tuo viaggio per iniziare a usare l'applicazione."
                                        AppLanguage.ZH -> "创建您的旅程以开始使用该应用程序。"
                                        AppLanguage.NL -> "Maak je reis aan om de applicatie te gaan gebruiken."
                                        AppLanguage.DE -> "Erstellen Sie Ihre Reise, um die Anwendung zu nutzen."
                                        AppLanguage.PL -> "Utwórz podróż, aby zacząć korzystać z aplikacji."
                                    }
                                    Text(
                                        text = welcomeTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = OceanNavy
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = welcomeSubtitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }

                    val tripNameLabel = when (currentLang) {
                        AppLanguage.ES -> "Nombre del Viaje (ej. Ruta por Islandia)"
                        AppLanguage.EN -> "Trip Name (e.g. Route through Iceland)"
                        AppLanguage.FR -> "Nom du voyage (ex. Route en Islande)"
                        AppLanguage.IT -> "Nome del viaggio (es. Itinerario in Islanda)"
                        AppLanguage.ZH -> "旅程名称（如：冰岛公路之旅）"
                        AppLanguage.NL -> "Reisnaam (bijv. Rondreis door IJsland)"
                        AppLanguage.DE -> "Reisename (z.B. Rundreise durch Island)"
                        AppLanguage.PL -> "Nazwa podróży (np. Trasa po Islandii)"
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(tripNameLabel) },
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
                                val startDateLabel = when (currentLang) {
                                    AppLanguage.ES -> "Fecha inicio"
                                    AppLanguage.EN -> "Start date"
                                    AppLanguage.FR -> "Date de début"
                                    AppLanguage.IT -> "Data inizio"
                                    AppLanguage.ZH -> "开始日期"
                                    AppLanguage.NL -> "Startdatum"
                                    AppLanguage.DE -> "Startdatum"
                                    AppLanguage.PL -> "Data rozpoczęcia"
                                }
                                Text(
                                    text = startDateLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                                Text(FormatUtils.formatShortDate(startDate, currentLang.locale), fontWeight = FontWeight.Bold, color = OceanNavy)
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
                                val endDateLabel = when (currentLang) {
                                    AppLanguage.ES -> "Fecha fin"
                                    AppLanguage.EN -> "End date"
                                    AppLanguage.FR -> "Date de fin"
                                    AppLanguage.IT -> "Data fine"
                                    AppLanguage.ZH -> "结束日期"
                                    AppLanguage.NL -> "Einddatum"
                                    AppLanguage.DE -> "Enddatum"
                                    AppLanguage.PL -> "Data zakończenia"
                                }
                                Text(
                                    text = endDateLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                                Text(FormatUtils.formatShortDate(endDate, currentLang.locale), fontWeight = FontWeight.Bold, color = OceanNavy)
                            }
                        }
                    }

                    val optBudgetLabel = when (currentLang) {
                        AppLanguage.ES -> "Presupuesto opcional"
                        AppLanguage.EN -> "Optional budget"
                        AppLanguage.FR -> "Budget optionnel"
                        AppLanguage.IT -> "Budget facoltativo"
                        AppLanguage.ZH -> "可选预算"
                        AppLanguage.NL -> "Optioneel budget"
                        AppLanguage.DE -> "Optionales Budget"
                        AppLanguage.PL -> "Opcjonalny budżet"
                    }
                    val noLimitPlaceholder = when (currentLang) {
                        AppLanguage.ES -> "Sin límite si vacío"
                        AppLanguage.EN -> "No limit if empty"
                        AppLanguage.FR -> "Sans limite si vide"
                        AppLanguage.IT -> "Senza limite se vuoto"
                        AppLanguage.ZH -> "留空表示不设限"
                        AppLanguage.NL -> "Geen limiet indien leeg"
                        AppLanguage.DE -> "Keine Grenze falls leer"
                        AppLanguage.PL -> "Bez limitu, jeśli puste"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = budgetText,
                            onValueChange = { budgetText = it },
                            label = { Text(optBudgetLabel) },
                            placeholder = { Text(noLimitPlaceholder) },
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
                                val currencyBtnLabel = when (currentLang) {
                                    AppLanguage.ES -> "Moneda"
                                    AppLanguage.EN -> "Currency"
                                    AppLanguage.FR -> "Devise"
                                    AppLanguage.IT -> "Valuta"
                                    AppLanguage.ZH -> "货币"
                                    AppLanguage.NL -> "Valuta"
                                    AppLanguage.DE -> "Währung"
                                    AppLanguage.PL -> "Waluta"
                                }
                                Text(
                                    text = "$currencyBtnLabel: $baseCurrency (${CurrencyCatalog.getSymbol(baseCurrency)})",
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
                                            Text("${curr.code} - ${CurrencyCatalog.getDisplayName(curr, currentLang)} (${curr.symbol})")
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

                    val participantsTitle = if (isEdit) {
                        when (currentLang) {
                            AppLanguage.ES -> "Participantes del viaje"
                            AppLanguage.EN -> "Trip participants"
                            AppLanguage.FR -> "Participants au voyage"
                            AppLanguage.IT -> "Partecipanti al viaggio"
                            AppLanguage.ZH -> "旅程参与者"
                            AppLanguage.NL -> "Deelnemers aan de reis"
                            AppLanguage.DE -> "Reiseteilnehmer"
                            AppLanguage.PL -> "Uczestnicy podróży"
                        }
                    } else {
                        when (currentLang) {
                            AppLanguage.ES -> "Participantes iniciales del viaje"
                            AppLanguage.EN -> "Initial trip participants"
                            AppLanguage.FR -> "Participants initiaux au voyage"
                            AppLanguage.IT -> "Partecipanti initiali al viaggio"
                            AppLanguage.ZH -> "旅程初始参与者"
                            AppLanguage.NL -> "Initiële reisdeelnemers"
                            AppLanguage.DE -> "Erste Reiseteilnehmer"
                            AppLanguage.PL -> "Początkowi uczestnicy podróży"
                        }
                    }
                    Text(
                        text = participantsTitle,
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
                            placeholder = { Text(texts.addParticipantPlaceholder) },
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
                                            contentDescription = texts.deleteBtn,
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
                        Text(texts.cancel)
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
                                finalParticipants.add(texts.defaultTravelerName)
                            }
                            val defaultTripTitle = when (currentLang) {
                                AppLanguage.ES -> "Viaje ${LocalDate.now().year}"
                                AppLanguage.EN -> "Trip ${LocalDate.now().year}"
                                AppLanguage.FR -> "Voyage ${LocalDate.now().year}"
                                AppLanguage.IT -> "Viaggio ${LocalDate.now().year}"
                                AppLanguage.ZH -> "${LocalDate.now().year}年旅程"
                                AppLanguage.NL -> "Reis ${LocalDate.now().year}"
                                AppLanguage.DE -> "Reise ${LocalDate.now().year}"
                                AppLanguage.PL -> "Podróż ${LocalDate.now().year}"
                            }
                            onCreateTrip(
                                title.ifBlank { defaultTripTitle },
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
                        val saveTripLabel = if (isEdit) {
                            when (currentLang) {
                                AppLanguage.ES -> "Guardar Cambios"
                                AppLanguage.EN -> "Save Changes"
                                AppLanguage.FR -> "Enregistrer"
                                AppLanguage.IT -> "Salva modifiche"
                                AppLanguage.ZH -> "保存修改"
                                AppLanguage.NL -> "Wijzigingen opslaan"
                                AppLanguage.DE -> "Änderungen speichern"
                                AppLanguage.PL -> "Zapisz zmiany"
                            }
                        } else texts.saveTripBtn
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = saveTripLabel,
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
    val currentLang = LocalAppLanguage.current
    val texts = UiTexts.get(currentLang)
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
                val modalTitle = when (currentLang) {
                    AppLanguage.ES -> "📥 Mapeo Manual de Columnas CSV"
                    AppLanguage.EN -> "📥 Manual CSV Column Mapping"
                    AppLanguage.FR -> "📥 Mappage manuel des colonnes CSV"
                    AppLanguage.IT -> "📥 Mappatura manuale colonne CSV"
                    AppLanguage.ZH -> "📥 CSV 列手动映射"
                    AppLanguage.NL -> "📥 Handmatige CSV-kolomtoewijzing"
                    AppLanguage.DE -> "📥 Manuelle CSV-Spaltenzuordnung"
                    AppLanguage.PL -> "📥 Ręczne mapowanie kolumn CSV"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OceanNavy)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = modalTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = texts.closeBtn,
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
                    val subtitleText = when (currentLang) {
                        AppLanguage.ES -> "Hemos detectado columnas personalizadas en tu archivo CSV. Asigna a qué campo corresponde cada columna:"
                        AppLanguage.EN -> "Custom columns detected in your CSV file. Assign each field to its corresponding column:"
                        AppLanguage.FR -> "Colonnes personnalisées détectées dans votre fichier CSV. Attribuez chaque champ à sa colonne :"
                        AppLanguage.IT -> "Colonne personalizzate rilevate nel tuo file CSV. Assegna ciascun campo alla colonna corrispondente:"
                        AppLanguage.ZH -> "在您的 CSV 文件中检测到自定义列。请将每个字段分配给其对应列："
                        AppLanguage.NL -> "Aangepaste kolommen gedetecteerd in je CSV-bestand. Wijs elk veld toe aan de corresponderende kolom:"
                        AppLanguage.DE -> "Benutzerdefinierte Spalten in Ihrer CSV-Datei erkannt. Weisen Sie jedes Feld der entsprechenden Spalte zu:"
                        AppLanguage.PL -> "Wykryto niestandardowe kolumny w pliku CSV. Przypisz każde pole do odpowiedniej kolumny:"
                    }
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )

                    if (createNewTrip) {
                        val newTripTitleLabel = when (currentLang) {
                            AppLanguage.ES -> "Título para el nuevo viaje importado"
                            AppLanguage.EN -> "Title for the new imported trip"
                            AppLanguage.FR -> "Titre du nouveau voyage importé"
                            AppLanguage.IT -> "Titolo del nuovo viaggio importato"
                            AppLanguage.ZH -> "新导入旅程的标题"
                            AppLanguage.NL -> "Titel voor de nieuw geïmporteerde reis"
                            AppLanguage.DE -> "Titel für die neu importierte Reise"
                            AppLanguage.PL -> "Tytuł nowej zaimportowanej podróży"
                        }
                        OutlinedTextField(
                            value = tripTitle,
                            onValueChange = { tripTitle = it },
                            label = { Text(newTripTitleLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    val amountColLabel = when (currentLang) {
                        AppLanguage.ES -> "Columna de Importe (Obligatoria)"
                        AppLanguage.EN -> "Amount Column (Required)"
                        AppLanguage.FR -> "Colonne de montant (obligatoire)"
                        AppLanguage.IT -> "Colonna importo (obbligatoria)"
                        AppLanguage.ZH -> "金额列（必填）"
                        AppLanguage.NL -> "Bedragkolom (verplicht)"
                        AppLanguage.DE -> "Betragsspalte (erforderlich)"
                        AppLanguage.PL -> "Kolumna kwoty (wymagana)"
                    }
                    val dateColLabel = when (currentLang) {
                        AppLanguage.ES -> "Columna de Fecha"
                        AppLanguage.EN -> "Date Column"
                        AppLanguage.FR -> "Colonne de date"
                        AppLanguage.IT -> "Colonna data"
                        AppLanguage.ZH -> "日期列"
                        AppLanguage.NL -> "Datumkolom"
                        AppLanguage.DE -> "Datumspalte"
                        AppLanguage.PL -> "Kolumna daty"
                    }
                    val placeColLabel = when (currentLang) {
                        AppLanguage.ES -> "Columna de Lugar / Establecimiento"
                        AppLanguage.EN -> "Place / Venue Column"
                        AppLanguage.FR -> "Colonne de lieu / établissement"
                        AppLanguage.IT -> "Colonna luogo / esercizio"
                        AppLanguage.ZH -> "地点 / 场所列"
                        AppLanguage.NL -> "Locatiekolom"
                        AppLanguage.DE -> "Ort / Betriebsspalte"
                        AppLanguage.PL -> "Kolumna miejsca / lokalu"
                    }
                    val categoryColLabel = when (currentLang) {
                        AppLanguage.ES -> "Columna de Categoría"
                        AppLanguage.EN -> "Category Column"
                        AppLanguage.FR -> "Colonne de catégorie"
                        AppLanguage.IT -> "Colonna categoria"
                        AppLanguage.ZH -> "类别列"
                        AppLanguage.NL -> "Categoriekolom"
                        AppLanguage.DE -> "Kategoriespalte"
                        AppLanguage.PL -> "Kolumna kategorii"
                    }
                    val paidByColLabel = when (currentLang) {
                        AppLanguage.ES -> "Columna de Pagador (Quién pagó)"
                        AppLanguage.EN -> "Payer Column (Who paid)"
                        AppLanguage.FR -> "Colonne du payeur (qui a payé)"
                        AppLanguage.IT -> "Colonna pagatore (chi ha pagato)"
                        AppLanguage.ZH -> "付款人列（由谁支付）"
                        AppLanguage.NL -> "Betalerkolom (wie heeft betaald)"
                        AppLanguage.DE -> "Zahlerspalte (wer bezahlt hat)"
                        AppLanguage.PL -> "Kolumna płatnika (kto zapłacił)"
                    }
                    val currencyColLabel = when (currentLang) {
                        AppLanguage.ES -> "Columna de Moneda (ISO)"
                        AppLanguage.EN -> "Currency Column (ISO)"
                        AppLanguage.FR -> "Colonne de devise (ISO)"
                        AppLanguage.IT -> "Colonna valuta (ISO)"
                        AppLanguage.ZH -> "货币列 (ISO)"
                        AppLanguage.NL -> "Valutakolom (ISO)"
                        AppLanguage.DE -> "Währungsspalte (ISO)"
                        AppLanguage.PL -> "Kolumna waluty (ISO)"
                    }
                    val notesColLabel = when (currentLang) {
                        AppLanguage.ES -> "Columna de Notas"
                        AppLanguage.EN -> "Notes Column"
                        AppLanguage.FR -> "Colonne de notes"
                        AppLanguage.IT -> "Colonna note"
                        AppLanguage.ZH -> "备注列"
                        AppLanguage.NL -> "Notitieskolom"
                        AppLanguage.DE -> "Notizenspalte"
                        AppLanguage.PL -> "Kolumna notatek"
                    }

                    ColumnSelectorRow(
                        label = amountColLabel,
                        headers = sheet.headers,
                        selectedIndex = amountCol,
                        allowNone = false,
                        lang = currentLang
                    ) { amountCol = it }
                    ColumnSelectorRow(
                        label = dateColLabel,
                        headers = sheet.headers,
                        selectedIndex = dateCol,
                        allowNone = true,
                        lang = currentLang
                    ) { dateCol = it }
                    ColumnSelectorRow(
                        label = placeColLabel,
                        headers = sheet.headers,
                        selectedIndex = placeCol,
                        allowNone = true,
                        lang = currentLang
                    ) { placeCol = it }
                    ColumnSelectorRow(
                        label = categoryColLabel,
                        headers = sheet.headers,
                        selectedIndex = categoryCol,
                        allowNone = true,
                        lang = currentLang
                    ) { categoryCol = it }
                    ColumnSelectorRow(
                        label = paidByColLabel,
                        headers = sheet.headers,
                        selectedIndex = paidByCol,
                        allowNone = true,
                        lang = currentLang
                    ) { paidByCol = it }
                    ColumnSelectorRow(
                        label = currencyColLabel,
                        headers = sheet.headers,
                        selectedIndex = currencyCol,
                        allowNone = true,
                        lang = currentLang
                    ) { currencyCol = it }
                    ColumnSelectorRow(
                        label = notesColLabel,
                        headers = sheet.headers,
                        selectedIndex = notesCol,
                        allowNone = true,
                        lang = currentLang
                    ) { notesCol = it }

                    Spacer(modifier = Modifier.height(6.dp))
                    val previewRowsLabel = when (currentLang) {
                        AppLanguage.ES -> "Vista previa de las primeras filas (${sheet.rows.size} filas en total):"
                        AppLanguage.EN -> "Preview of first rows (${sheet.rows.size} total rows):"
                        AppLanguage.FR -> "Aperçu des premières lignes (${sheet.rows.size} lignes au total) :"
                        AppLanguage.IT -> "Anteprima delle prime righe (${sheet.rows.size} righe in totale):"
                        AppLanguage.ZH -> "前几行预览（共 ${sheet.rows.size} 行）："
                        AppLanguage.NL -> "Voorbeeld van eerste rijen (${sheet.rows.size} rijen in totaal):"
                        AppLanguage.DE -> "Vorschau der ersten Zeilen (${sheet.rows.size} Zeilen insgesamt):"
                        AppLanguage.PL -> "Podgląd pierwszych wierszy (łącznie ${sheet.rows.size} wierszy):"
                    }
                    Text(
                        text = previewRowsLabel,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy
                    )

                    val rowWord = when (currentLang) {
                        AppLanguage.ES -> "Fila"
                        AppLanguage.EN -> "Row"
                        AppLanguage.FR -> "Ligne"
                        AppLanguage.IT -> "Riga"
                        AppLanguage.ZH -> "行"
                        AppLanguage.NL -> "Rij"
                        AppLanguage.DE -> "Zeile"
                        AppLanguage.PL -> "Wiersz"
                    }
                    sheet.rows.take(4).forEachIndexed { idx, row ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$rowWord ${idx + 1}: ${row.joinToString(" | ")}",
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
                        Text(texts.cancel)
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
                        val importRowsLabel = when (currentLang) {
                            AppLanguage.ES -> "Importar ${sheet.rows.size} filas"
                            AppLanguage.EN -> "Import ${sheet.rows.size} rows"
                            AppLanguage.FR -> "Importer ${sheet.rows.size} lignes"
                            AppLanguage.IT -> "Importa ${sheet.rows.size} righe"
                            AppLanguage.ZH -> "导入 ${sheet.rows.size} 行"
                            AppLanguage.NL -> "${sheet.rows.size} rijen importeren"
                            AppLanguage.DE -> "${sheet.rows.size} Zeilen importieren"
                            AppLanguage.PL -> "Zaimportuj ${sheet.rows.size} wierszy"
                        }
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = importRowsLabel,
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
    lang: AppLanguage,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val noneText = when (lang) {
        AppLanguage.ES -> "(No asignar)"
        AppLanguage.EN -> "(Do not assign)"
        AppLanguage.FR -> "(Ne pas attribuer)"
        AppLanguage.IT -> "(Non assegnare)"
        AppLanguage.ZH -> "（不分配）"
        AppLanguage.NL -> "(Niet toewijzen)"
        AppLanguage.DE -> "(Nicht zuweisen)"
        AppLanguage.PL -> "(Nie przypisuj)"
    }
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
    val currentLang = LocalAppLanguage.current
    val texts = UiTexts.get(currentLang)

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
                    val backupTitle = when (currentLang) {
                        AppLanguage.ES -> "Copia de Seguridad JSON"
                        AppLanguage.EN -> "JSON Backup & Restore"
                        AppLanguage.FR -> "Sauvegarde et Restauration JSON"
                        AppLanguage.IT -> "Backup e Ripristino JSON"
                        AppLanguage.ZH -> "JSON 备份与恢复"
                        AppLanguage.NL -> "JSON-back-up en herstel"
                        AppLanguage.DE -> "JSON-Sicherung & Wiederherstellung"
                        AppLanguage.PL -> "Kopia zapasowa i Przywracanie JSON"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = TravelBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = backupTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = OceanNavy
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = texts.closeBtn)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                val deviceDataText = when (currentLang) {
                    AppLanguage.ES -> "Datos actuales en dispositivo: $tripCount viajes, $participantCount participantes, $expenseCount gastos y $incomeCount aportaciones al bote."
                    AppLanguage.EN -> "Current device data: $tripCount trips, $participantCount participants, $expenseCount expenses & $incomeCount pot contributions."
                    AppLanguage.FR -> "Données actuelles sur l'appareil : $tripCount voyages, $participantCount participants, $expenseCount dépenses et $incomeCount apports cagnotte."
                    AppLanguage.IT -> "Dati correnti sul dispositivo: $tripCount viaggi, $participantCount partecipanti, $expenseCount spese e $incomeCount contributi cassa."
                    AppLanguage.ZH -> "设备当前数据：$tripCount 个旅程、$participantCount 位参与者、$expenseCount 项支出及 $incomeCount 笔公账注入。"
                    AppLanguage.NL -> "Huidige apparaatgegevens: $tripCount reizen, $participantCount deelnemers, $expenseCount uitgaven en $incomeCount potbijdragen."
                    AppLanguage.DE -> "Aktuelle Gerätedaten: $tripCount Reisen, $participantCount Teilnehmer, $expenseCount Ausgaben & $incomeCount Kassenbeiträge."
                    AppLanguage.PL -> "Aktualne dane w urządzeniu: $tripCount podróży, $participantCount uczestników, $expenseCount wydatków i $incomeCount wpłat do budżetu."
                }
                Text(
                    text = deviceDataText,
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
                        val section1Title = when (currentLang) {
                            AppLanguage.ES -> "1. Exportar Estado Completo (.json)"
                            AppLanguage.EN -> "1. Export Full App State (.json)"
                            AppLanguage.FR -> "1. Exporter l'état complet (.json)"
                            AppLanguage.IT -> "1. Esporta stato completo (.json)"
                            AppLanguage.ZH -> "1. 导出完整应用状态 (.json)"
                            AppLanguage.NL -> "1. Volledige app-status exporteren (.json)"
                            AppLanguage.DE -> "1. Gesamten App-Zustand exportieren (.json)"
                            AppLanguage.PL -> "1. Eksportuj pełny stan aplikacji (.json)"
                        }
                        val section1Desc = when (currentLang) {
                            AppLanguage.ES -> "Guarda todos tus viajes, participantes, gastos, fotos, ingresos del bote y ajustes en un archivo JSON."
                            AppLanguage.EN -> "Saves all trips, participants, expenses, photos, pot incomes and preferences into a JSON backup."
                            AppLanguage.FR -> "Enregistre tous vos voyages, participants, dépenses, photos, revenus de cagnotte et préférences dans une sauvegarde JSON."
                            AppLanguage.IT -> "Salva tutti i viaggi, partecipanti, spese, foto, entrate cassa e impostazioni in un backup JSON."
                            AppLanguage.ZH -> "将所有旅程、参与者、支出、照片、公账收入及偏好设置保存到 JSON 备份中。"
                            AppLanguage.NL -> "Slaat alle reizen, deelnemers, uitgaven, foto's, potinkomsten en voorkeuren op in een JSON-back-up."
                            AppLanguage.DE -> "Speichert alle Reisen, Teilnehmer, Ausgaben, Fotos, Kasseneinnahmen und Einstellungen in einer JSON-Sicherung."
                            AppLanguage.PL -> "Zapisuje wszystkie podróże, uczestników, wydatki, zdjęcia, wpłaty do budżetu i ustawienia w kopii zapasowej JSON."
                        }
                        Text(
                            text = section1Title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = OceanNavy
                        )
                        Text(
                            text = section1Desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val exportBtnText = when (currentLang) {
                            AppLanguage.ES -> "Exportar Backup (.json)"
                            AppLanguage.EN -> "Export Backup File (.json)"
                            AppLanguage.FR -> "Exporter le fichier de sauvegarde (.json)"
                            AppLanguage.IT -> "Esporta file di backup (.json)"
                            AppLanguage.ZH -> "导出备份文件 (.json)"
                            AppLanguage.NL -> "Back-upbestand exporteren (.json)"
                            AppLanguage.DE -> "Sicherungsdatei exportieren (.json)"
                            AppLanguage.PL -> "Eksportuj plik kopii (.json)"
                        }
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
                                text = exportBtnText,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val copiedToast = when (currentLang) {
                            AppLanguage.ES -> "JSON de copia de seguridad copiado al portapapeles"
                            AppLanguage.EN -> "JSON backup copied to clipboard"
                            AppLanguage.FR -> "Sauvegarde JSON copiée dans le presse-papiers"
                            AppLanguage.IT -> "Backup JSON copiato negli appunti"
                            AppLanguage.ZH -> "JSON 备份已复制到剪贴板"
                            AppLanguage.NL -> "JSON-back-up naar klembord gekopieerd"
                            AppLanguage.DE -> "JSON-Sicherung in die Zwischenablage kopiert"
                            AppLanguage.PL -> "Kopia JSON skopiowana do schowka"
                        }
                        val copyJsonBtn = when (currentLang) {
                            AppLanguage.ES -> "Copiar JSON"
                            AppLanguage.EN -> "Copy JSON"
                            AppLanguage.FR -> "Copier JSON"
                            AppLanguage.IT -> "Copia JSON"
                            AppLanguage.ZH -> "复制 JSON"
                            AppLanguage.NL -> "JSON kopiëren"
                            AppLanguage.DE -> "JSON kopieren"
                            AppLanguage.PL -> "Kopiuj JSON"
                        }
                        val viewEditJsonBtn = when (currentLang) {
                            AppLanguage.ES -> "Ver / Pegar JSON"
                            AppLanguage.EN -> "View / Edit JSON"
                            AppLanguage.FR -> "Voir / Coller JSON"
                            AppLanguage.IT -> "Visualizza / Incolla JSON"
                            AppLanguage.ZH -> "查看 / 粘贴 JSON"
                            AppLanguage.NL -> "JSON bekijken / plakken"
                            AppLanguage.DE -> "JSON anzeigen / einfügen"
                            AppLanguage.PL -> "Pokaż / Wklej JSON"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val json = onGenerateBackupJson()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("travel_cash_backup", json))
                                    onShowToast(copiedToast)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = copyJsonBtn,
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
                                    text = viewEditJsonBtn,
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
                        val section2Title = when (currentLang) {
                            AppLanguage.ES -> "2. Importar / Restaurar Backup (.json)"
                            AppLanguage.EN -> "2. Import / Restore Backup (.json)"
                            AppLanguage.FR -> "2. Importer / Restaurer sauvegarde (.json)"
                            AppLanguage.IT -> "2. Importa / Ripristina backup (.json)"
                            AppLanguage.ZH -> "2. 导入 / 恢复备份 (.json)"
                            AppLanguage.NL -> "2. Back-up importeren / herstellen (.json)"
                            AppLanguage.DE -> "2. Sicherung importieren / wiederherstellen (.json)"
                            AppLanguage.PL -> "2. Importuj / Przywróć kopię (.json)"
                        }
                        Text(
                            text = section2Title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = OceanNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val mergeTitle = when (currentLang) {
                            AppLanguage.ES -> "Fusionar con datos actuales"
                            AppLanguage.EN -> "Merge with current data"
                            AppLanguage.FR -> "Fusionner avec les données actuelles"
                            AppLanguage.IT -> "Unisci ai dati attuali"
                            AppLanguage.ZH -> "与当前数据合并"
                            AppLanguage.NL -> "Samenvoegen met huidige gegevens"
                            AppLanguage.DE -> "Mit aktuellen Daten zusammenführen"
                            AppLanguage.PL -> "Połącz z aktualnymi danymi"
                        }
                        val mergeDesc = when (currentLang) {
                            AppLanguage.ES -> "Conserva tus viajes actuales y añade o actualiza los del archivo."
                            AppLanguage.EN -> "Keeps current trips and adds or updates those from the backup."
                            AppLanguage.FR -> "Conserve vos voyages actuels et ajoute ou met à jour ceux de la sauvegarde."
                            AppLanguage.IT -> "Mantiene i viaggi correnti e aggiunge o aggiorna quelli del backup."
                            AppLanguage.ZH -> "保留现有旅程并添加或更新备份中的旅程。"
                            AppLanguage.NL -> "Behoudt huidige reizen en voegt die uit de back-up toe of werkt ze bij."
                            AppLanguage.DE -> "Behält aktuelle Reisen bei und fügt die aus der Sicherung hinzu oder aktualisiert sie."
                            AppLanguage.PL -> "Zachowuje bieżące podróże i dodaje lub aktualizuje te z kopii zapasowej."
                        }
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
                                    text = mergeTitle,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = OceanNavy
                                )
                                Text(
                                    text = mergeDesc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        val replaceTitle = when (currentLang) {
                            AppLanguage.ES -> "Reemplazar todo"
                            AppLanguage.EN -> "Replace all data"
                            AppLanguage.FR -> "Remplacer toutes les données"
                            AppLanguage.IT -> "Sostituisci tutti i dati"
                            AppLanguage.ZH -> "替换所有数据"
                            AppLanguage.NL -> "Alles vervangen"
                            AppLanguage.DE -> "Alle Daten ersetzen"
                            AppLanguage.PL -> "Zastąp wszystkie dane"
                        }
                        val replaceDesc = when (currentLang) {
                            AppLanguage.ES -> "Borra los datos actuales antes de restaurar la copia."
                            AppLanguage.EN -> "Deletes current data before restoring the backup."
                            AppLanguage.FR -> "Supprime les données actuelles avant de restaurer la sauvegarde."
                            AppLanguage.IT -> "Cancella i dati attuali prima di ripristinare il backup."
                            AppLanguage.ZH -> "在恢复备份前删除当前的所有数据。"
                            AppLanguage.NL -> "Wist huidige gegevens alvorens de back-up te herstellen."
                            AppLanguage.DE -> "Löscht aktuelle Daten vor der Wiederherstellung der Sicherung."
                            AppLanguage.PL -> "Usuwa bieżące dane przed przywróceniem kopii zapasowej."
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
                                    text = replaceTitle,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = CoralRed
                                )
                                Text(
                                    text = replaceDesc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val selectJsonBtn = when (currentLang) {
                            AppLanguage.ES -> "Seleccionar archivo .json y Restaurar"
                            AppLanguage.EN -> "Select .json File & Restore"
                            AppLanguage.FR -> "Sélectionner fichier .json et restaurer"
                            AppLanguage.IT -> "Seleziona file .json e ripristina"
                            AppLanguage.ZH -> "选择 .json 文件并恢复"
                            AppLanguage.NL -> ".json-bestand selecteren en herstellen"
                            AppLanguage.DE -> ".json-Datei auswählen & wiederherstellen"
                            AppLanguage.PL -> "Wybierz plik .json i przywróć"
                        }
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
                                text = selectJsonBtn,
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
                                val jsonContentLabel = when (currentLang) {
                                    AppLanguage.ES -> "Contenido JSON:"
                                    AppLanguage.EN -> "JSON Content:"
                                    AppLanguage.FR -> "Contenu JSON :"
                                    AppLanguage.IT -> "Contenuto JSON:"
                                    AppLanguage.ZH -> "JSON 内容："
                                    AppLanguage.NL -> "JSON-inhoud:"
                                    AppLanguage.DE -> "JSON-Inhalt:"
                                    AppLanguage.PL -> "Zawartość JSON:"
                                }
                                val pasteBtnLabel = when (currentLang) {
                                    AppLanguage.ES -> "Pegar"
                                    AppLanguage.EN -> "Paste"
                                    AppLanguage.FR -> "Coller"
                                    AppLanguage.IT -> "Incolla"
                                    AppLanguage.ZH -> "粘贴"
                                    AppLanguage.NL -> "Plakken"
                                    AppLanguage.DE -> "Einfügen"
                                    AppLanguage.PL -> "Wklej"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = jsonContentLabel,
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
                                            text = pasteBtnLabel,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                val pastePlaceholder = when (currentLang) {
                                    AppLanguage.ES -> "Pega aquí el contenido JSON de tu copia..."
                                    AppLanguage.EN -> "Paste your backup JSON here..."
                                    AppLanguage.FR -> "Collez ici le contenu JSON de votre sauvegarde..."
                                    AppLanguage.IT -> "Incolla qui il contenuto JSON del tuo backup..."
                                    AppLanguage.ZH -> "在此处粘贴您的备份 JSON 内容..."
                                    AppLanguage.NL -> "Plak hier de JSON-inhoud van je back-up..."
                                    AppLanguage.DE -> "Fügen Sie hier den JSON-Inhalt Ihrer Sicherung ein..."
                                    AppLanguage.PL -> "Wklej tutaj zawartość JSON swojej kopii..."
                                }
                                OutlinedTextField(
                                    value = rawJsonText,
                                    onValueChange = { rawJsonText = it },
                                    placeholder = { Text(pastePlaceholder) },
                                    maxLines = 6,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("raw_json_backup_input")
                                )

                                if (parsedPreview != null && parsedPreview.trips.isNotEmpty()) {
                                    val validJsonText = when (currentLang) {
                                        AppLanguage.ES -> "✓ JSON válido detectado: ${parsedPreview.trips.size} viajes, ${parsedPreview.participants.size} participantes, ${parsedPreview.expenses.size} gastos y ${parsedPreview.incomes.size} ingresos."
                                        AppLanguage.EN -> "✓ Valid JSON detected: ${parsedPreview.trips.size} trips, ${parsedPreview.participants.size} participants, ${parsedPreview.expenses.size} expenses, ${parsedPreview.incomes.size} pot incomes."
                                        AppLanguage.FR -> "✓ JSON valide détecté : ${parsedPreview.trips.size} voyages, ${parsedPreview.participants.size} participants, ${parsedPreview.expenses.size} dépenses, ${parsedPreview.incomes.size} apports."
                                        AppLanguage.IT -> "✓ JSON valido rilevato: ${parsedPreview.trips.size} viaggi, ${parsedPreview.participants.size} partecipanti, ${parsedPreview.expenses.size} spese, ${parsedPreview.incomes.size} entrate cassa."
                                        AppLanguage.ZH -> "✓ 检测到有效的 JSON：${parsedPreview.trips.size} 个旅程、${parsedPreview.participants.size} 位参与者、${parsedPreview.expenses.size} 项支出、${parsedPreview.incomes.size} 笔公账注入。"
                                        AppLanguage.NL -> "✓ Geldige JSON gedetecteerd: ${parsedPreview.trips.size} reizen, ${parsedPreview.participants.size} deelnemers, ${parsedPreview.expenses.size} uitgaven, ${parsedPreview.incomes.size} potbijdragen."
                                        AppLanguage.DE -> "✓ Gültiges JSON erkannt: ${parsedPreview.trips.size} Reisen, ${parsedPreview.participants.size} Teilnehmer, ${parsedPreview.expenses.size} Ausgaben, ${parsedPreview.incomes.size} Kasseneinnahmen."
                                        AppLanguage.PL -> "✓ Wykryto prawidłowy JSON: ${parsedPreview.trips.size} podróży, ${parsedPreview.participants.size} uczestników, ${parsedPreview.expenses.size} wydatków, ${parsedPreview.incomes.size} wpłat."
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, EmeraldGreenDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = validJsonText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = EmeraldGreenDark,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                val restoreFromPastedBtn = when (currentLang) {
                                    AppLanguage.ES -> "Restaurar desde texto JSON"
                                    AppLanguage.EN -> "Restore from Pasted JSON"
                                    AppLanguage.FR -> "Restaurer depuis le texte JSON"
                                    AppLanguage.IT -> "Ripristina da testo JSON"
                                    AppLanguage.ZH -> "从粘贴的 JSON 恢复"
                                    AppLanguage.NL -> "Herstellen vanuit geplakte JSON"
                                    AppLanguage.DE -> "Aus eingefügtem JSON wiederherstellen"
                                    AppLanguage.PL -> "Przywróć z wklejonego JSON"
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
                                        text = restoreFromPastedBtn,
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
fun SuggestionsModal(
    onDismiss: () -> Unit,
    onOpenHelpFaq: () -> Unit = {}
) {
    val currentLang = LocalAppLanguage.current
    val context = LocalContext.current

    val modalTitle = when (currentLang) {
        AppLanguage.ES -> "Ayuda Travel Cash"
        AppLanguage.EN -> "Travel Cash Help"
        AppLanguage.FR -> "Aide Travel Cash"
        AppLanguage.IT -> "Aiuto Travel Cash"
        AppLanguage.ZH -> "Travel Cash 帮助"
        AppLanguage.NL -> "Travel Cash Help"
        AppLanguage.DE -> "Travel Cash Hilfe"
        AppLanguage.PL -> "Pomoc Travel Cash"
    }

    val closeDesc = when (currentLang) {
        AppLanguage.ES -> "Cerrar"
        AppLanguage.EN -> "Close"
        AppLanguage.FR -> "Fermer"
        AppLanguage.IT -> "Chiudi"
        AppLanguage.ZH -> "关闭"
        AppLanguage.NL -> "Sluiten"
        AppLanguage.DE -> "Schließen"
        AppLanguage.PL -> "Zamknij"
    }

    val sendEmailDesc = when (currentLang) {
        AppLanguage.ES -> "Enviar correo"
        AppLanguage.EN -> "Send email"
        AppLanguage.FR -> "Envoyer un e-mail"
        AppLanguage.IT -> "Invia email"
        AppLanguage.ZH -> "发送邮件"
        AppLanguage.NL -> "E-mail verzenden"
        AppLanguage.DE -> "E-Mail senden"
        AppLanguage.PL -> "Wyślij e-mail"
    }

    val gotItBtn = when (currentLang) {
        AppLanguage.ES -> "Entendido"
        AppLanguage.EN -> "Got it"
        AppLanguage.FR -> "Compris"
        AppLanguage.IT -> "Ho capito"
        AppLanguage.ZH -> "知道了"
        AppLanguage.NL -> "Begrepen"
        AppLanguage.DE -> "Verstanden"
        AppLanguage.PL -> "Rozumiem"
    }

    val potRuleTitle = when (currentLang) {
        AppLanguage.ES -> "🏺 Regla de Oro del Bote Común"
        AppLanguage.EN -> "🏺 Common Pot Golden Rule"
        AppLanguage.FR -> "🏺 Règle d'or du pot commun"
        AppLanguage.IT -> "🏺 Regola d'oro della cassa comune"
        AppLanguage.ZH -> "🏺 公账黄金法则"
        AppLanguage.NL -> "🏺 Gouden regel van de gemeenschappelijke pot"
        AppLanguage.DE -> "🏺 Goldene Regel der Gemeinschaftskasse"
        AppLanguage.PL -> "🏺 Złota zasada wspólnej kasy"
    }
    val potRuleBody = when (currentLang) {
        AppLanguage.ES -> "Al pulsar '+ Ingreso (Bote)' en modo 'Cuantía por persona', si algún viajero adelantó pagos de su bolsillo, el sistema descuenta automáticamente lo que se le debe de su cuota de bote."
        AppLanguage.EN -> "When adding a Pot Income in 'Amount per person' mode, if any traveler paid expenses out of pocket, the system automatically deducts what is owed to them from their pot share."
        AppLanguage.FR -> "En ajoutant un apport au pot en mode « Montant par personne », si un voyageur a avancé des frais de sa poche, le système déduit automatiquement ce qui lui est dû de sa part."
        AppLanguage.IT -> "Quando aggiungi un'entrata cassa in modalità 'Importo per persona', se un viaggiatore ha anticipato spese di tasca propria, il sistema deduce automaticamente quanto gli spetta dalla sua quota."
        AppLanguage.ZH -> "在“每人金额”模式下添加公账注入时，如果有旅行者自掏腰包垫付了费用，系统会自动从其应缴份额中扣除欠款。"
        AppLanguage.NL -> "Bij het toevoegen van een potbijdrage in de modus 'Bedrag per persoon': als een reiziger uit eigen zak heeft betaald, verrekent het systeem automatisch wat hij tegoed heeft."
        AppLanguage.DE -> "Beim Hinzufügen einer Kasseneinzahlung im Modus 'Betrag pro Person': Wenn ein Reisender Auslagen vorgelegt hat, zieht das System den geschuldeten Betrag automatisch von seinem Kassenanteil ab."
        AppLanguage.PL -> "Dodając wpłatę do kasy w trybie „Kwota na osobę”, jeśli któryś z podróżnych wyłożył z własnej kieszeni, system automatycznie odlicza należność od jego składki."
    }

    val gpsTitle = when (currentLang) {
        AppLanguage.ES -> "📍 Proximidad GPS con OpenStreetMap"
        AppLanguage.EN -> "📍 GPS Proximity with OpenStreetMap"
        AppLanguage.FR -> "📍 Proximité GPS avec OpenStreetMap"
        AppLanguage.IT -> "📍 Prossimità GPS con OpenStreetMap"
        AppLanguage.ZH -> "📍 OpenStreetMap GPS 邻近定位"
        AppLanguage.NL -> "📍 GPS-nabijheid met OpenStreetMap"
        AppLanguage.DE -> "📍 GPS-Nähe mit OpenStreetMap"
        AppLanguage.PL -> "📍 Bliskość GPS z OpenStreetMap"
    }
    val gpsBody = when (currentLang) {
        AppLanguage.ES -> "Al añadir un gasto, escribe el nombre del restaurante o comercio: la app consulta Nominatim con tu GPS actual, ordena primero los más cercanos y rellena la dirección."
        AppLanguage.EN -> "When adding an expense, type the restaurant or shop name: the app queries Nominatim with your current GPS and sorts the closest venues first, filling in the address automatically."
        AppLanguage.FR -> "Lors de l'ajout d'une dépense, saisissez le nom du commerce ou restaurant : l'appli consulte Nominatim avec votre GPS actuel, trie les plus proches en premier et renseigne l'adresse."
        AppLanguage.IT -> "Quando aggiungi una spesa, digita il nome del ristorante o negozio: l'app consulta Nominatim con il tuo GPS attuale, ordina per primi i più vicini e inserisce l'indirizzo."
        AppLanguage.ZH -> "添加支出时输入餐厅或商店名称：应用会结合您当前的 GPS 查询 Nominatim，优先排列最近的地点并自动填充地址。"
        AppLanguage.NL -> "Typ bij het toevoegen van een uitgave de naam van het restaurant of de winkel: de app raadpleegt Nominatim met je huidige GPS, toont de dichtstbijzijnde eerst en vult het adres in."
        AppLanguage.DE -> "Geben Sie beim Hinzufügen einer Ausgabe den Namen des Restaurants oder Geschäfts ein: Die App fragt Nominatim mit Ihrem aktuellen GPS ab, sortiert die nächstgelegenen zuerst und füllt die Adresse aus."
        AppLanguage.PL -> "Dodając wydatek, wpisz nazwę lokalu lub sklepu: aplikacja sprawdza Nominatim z Twoim GPS, sortuje najbliższe miejsca i automatycznie uzupełnia adres."
    }

    val statsTitle = when (currentLang) {
        AppLanguage.ES -> "📊 Datos y Cuentas Interactivas"
        AppLanguage.EN -> "📊 Interactive Data & Splits"
        AppLanguage.FR -> "📊 Données et comptes interactifs"
        AppLanguage.IT -> "📊 Dati e conti interattivi"
        AppLanguage.ZH -> "📊 交互式数据与结算"
        AppLanguage.NL -> "📊 Interactieve gegevens & afrekeningen"
        AppLanguage.DE -> "📊 Interaktive Daten & Abrechnungen"
        AppLanguage.PL -> "📊 Interaktywne dane i rozliczenia"
    }
    val statsBody = when (currentLang) {
        AppLanguage.ES -> "En las pestañas 'Datos' y 'Cuentas', pulsa sobre cualquier categoría, persona o pago de liquidación para desplegar su desglose matemático completo."
        AppLanguage.EN -> "In the 'Data' and 'Splits' tabs, tap on any category, person or settlement payment to expand its full mathematical breakdown."
        AppLanguage.FR -> "Dans les onglets « Données » et « Comptes », appuyez sur n'importe quelle catégorie, personne ou remboursement pour afficher le détail mathématique complet."
        AppLanguage.IT -> "Nelle schede 'Dati' e 'Conti', tocca qualsiasi categoria, persona o pagamento di saldo per visualizzarne il riepilogo matematico completo."
        AppLanguage.ZH -> "在“数据”和“账目”标签页中，点击任意类别、成员或结算还款，即可展开完整的数学明细。"
        AppLanguage.NL -> "Tik in de tabbladen 'Gegevens' en 'Afrekeningen' op een categorie, persoon of vereffening om de volledige wiskundige uitsplitsing te zien."
        AppLanguage.DE -> "Tippen Sie in den Reitern 'Daten' und 'Abrechnung' auf eine Kategorie, Person oder Ausgleichszahlung, um die vollständige Aufschlüsselung anzuzeigen."
        AppLanguage.PL -> "W zakładkach „Dane” i „Rozliczenia” dotknij dowolnej kategorii, osoby lub płatności wyrównawczej, aby rozwinąć pełne zestawienie."
    }

    val exportTitle = when (currentLang) {
        AppLanguage.ES -> "⚙️ Exportación e Importación en Ajustes"
        AppLanguage.EN -> "⚙️ Export and Import in Settings"
        AppLanguage.FR -> "⚙️ Exportation et importation dans les paramètres"
        AppLanguage.IT -> "⚙️ Esportazione e importazione in Impostazioni"
        AppLanguage.ZH -> "⚙️ 设置中的导出与导入"
        AppLanguage.NL -> "⚙️ Exporteren en importeren in Instellingen"
        AppLanguage.DE -> "⚙️ Export und Import in den Einstellungen"
        AppLanguage.PL -> "⚙️ Eksport i import w Ustawieniach"
    }
    val exportBody = when (currentLang) {
        AppLanguage.ES -> "En la pestaña Ajustes puedes exportar e importar tus viajes en formato comprimido .zip con archivos JSON."
        AppLanguage.EN -> "In the Settings tab you can export and import your trips in compressed .zip format with JSON files."
        AppLanguage.FR -> "Dans l'onglet Paramètres, vous pouvez exporter et importer vos voyages au format compressé .zip contenant des fichiers JSON."
        AppLanguage.IT -> "Nella scheda Impostazioni puoi esportare e importare i tuoi viaggi in formato compresso .zip con file JSON."
        AppLanguage.ZH -> "在“设置”标签页中，您可以将旅程以包含 JSON 文件的压缩 .zip 格式进行导出和导入。"
        AppLanguage.NL -> "In het tabblad Instellingen kun je je reizen exporteren en importeren in gecomprimeerd .zip-formaat met JSON-bestanden."
        AppLanguage.DE -> "Im Reiter Einstellungen können Sie Ihre Reisen im komprimierten .zip-Format mit JSON-Dateien exportieren und importieren."
        AppLanguage.PL -> "W zakładce Ustawienia możesz eksportować i importować swoje podróże w skompresowanym formacie .zip z plikami JSON."
    }

    val faqButtonText = when (currentLang) {
        AppLanguage.ES -> "Ayuda y Preguntas Frecuentes"
        AppLanguage.EN -> "Help & FAQ"
        AppLanguage.FR -> "Aide et FAQ"
        AppLanguage.IT -> "Aiuto e FAQ"
        AppLanguage.ZH -> "帮助与常见问题"
        AppLanguage.NL -> "Help & FAQ"
        AppLanguage.DE -> "Hilfe & FAQ"
        AppLanguage.PL -> "Pomoc i FAQ"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ModalLightSurfaceTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = modalTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = OceanNavy
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = closeDesc)
                    }
                }

                // Correo electrónico de contacto/sugerencias arriba del todo
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:sugerenciastravelcash@gmail.com")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, modalTitle)
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(TravelBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (currentLang) {
                                    AppLanguage.ES -> "Correo para dudas y sugerencias:"
                                    AppLanguage.EN -> "Questions & suggestions email:"
                                    AppLanguage.FR -> "Courriel pour questions et suggestions :"
                                    AppLanguage.IT -> "Email per dubbi e suggerimenti:"
                                    AppLanguage.ZH -> "疑问与建议反馈邮箱："
                                    AppLanguage.NL -> "E-mail voor vragen en suggesties:"
                                    AppLanguage.DE -> "E-Mail für Fragen und Anregungen:"
                                    AppLanguage.PL -> "E-mail do pytań i sugestii:"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "sugerenciastravelcash@gmail.com",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = (-0.3).sp
                                ),
                                color = TravelBlue,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = sendEmailDesc,
                            tint = TravelBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Botón de Ayuda y Preguntas Frecuentes justo debajo del correo de contacto
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    onClick = onOpenHelpFaq,
                    shape = RoundedCornerShape(14.dp),
                    color = OceanNavy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("suggestions_help_faq_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = faqButtonText,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SuggestionItem(
                        title = potRuleTitle,
                        body = potRuleBody
                    )
                    SuggestionItem(
                        title = gpsTitle,
                        body = gpsBody
                    )
                    SuggestionItem(
                        title = statsTitle,
                        body = statsBody
                    )
                    SuggestionItem(
                        title = exportTitle,
                        body = exportBody
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = OceanNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(gotItBtn, fontWeight = FontWeight.Bold)
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
