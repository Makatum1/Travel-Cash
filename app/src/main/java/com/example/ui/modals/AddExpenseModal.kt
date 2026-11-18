package com.example.ui.modals

import android.Manifest
import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.CategoryCatalog
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplit
import com.example.data.model.ParticipantEntity
import com.example.data.model.PaymentMethodCatalog
import com.example.data.model.TripEntity
import com.example.data.remote.NominatimPlaceSuggestion
import com.example.data.remote.NominatimService
import com.example.domain.DebtCalculator
import com.example.ui.theme.AppLanguage
import kotlinx.coroutines.delay
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.OceanNavy
import com.example.ui.theme.TravelBlue
import com.example.util.CurrencyCatalog
import com.example.util.FormatUtils
import com.example.util.UiTexts
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong

enum class SplitMode(val labelEs: String, val labelEn: String) {
    EQUAL("Partes iguales", "Equal shares"),
    EXACT("Importes exactos", "Exact amounts"),
    PERCENTAGE("Porcentajes (%)", "Percentages (%)");

    fun getLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ES -> labelEs
        AppLanguage.EN -> labelEn
        AppLanguage.FR -> when (this) {
            EQUAL -> "Parts égales"
            EXACT -> "Montants exacts"
            PERCENTAGE -> "Pourcentages (%)"
        }
        AppLanguage.IT -> when (this) {
            EQUAL -> "Parti uguali"
            EXACT -> "Importi esatti"
            PERCENTAGE -> "Percentuali (%)"
        }
        AppLanguage.ZH -> when (this) {
            EQUAL -> "均摊"
            EXACT -> "精确金额"
            PERCENTAGE -> "百分比 (%)"
        }
        AppLanguage.NL -> when (this) {
            EQUAL -> "Gelijke delen"
            EXACT -> "Exacte bedragen"
            PERCENTAGE -> "Percentages (%)"
        }
        AppLanguage.DE -> when (this) {
            EQUAL -> "Gleiche Teile"
            EXACT -> "Genaue Beträge"
            PERCENTAGE -> "Prozentual (%)"
        }
        AppLanguage.PL -> when (this) {
            EQUAL -> "Równe części"
            EXACT -> "Dokładne kwoty"
            PERCENTAGE -> "Procentowo (%)"
        }
        AppLanguage.PT -> when (this) {
            EQUAL -> "Partes iguais"
            EXACT -> "Montantes exatos"
            PERCENTAGE -> "Percentagens (%)"
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddExpenseModal(
    trip: TripEntity,
    participants: List<ParticipantEntity>,
    existingExpense: ExpenseEntity?,
    userLocation: Pair<Double, Double>?,
    placeSuggestions: List<NominatimPlaceSuggestion>,
    isSearchingPlaces: Boolean,
    onRequestLocationRefresh: () -> Unit,
    onPlaceQueryChanged: (String) -> Unit,
    onClearPlaceSuggestions: () -> Unit,
    onSaveExpense: (ExpenseEntity, Boolean) -> Unit,
    onDeleteExpense: (String) -> Unit,
    systemNavBarBottomPadding: Dp = 0.dp,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentLang = LocalAppLanguage.current
    val texts = UiTexts.get(currentLang)
    val isEdit = existingExpense != null

    val navBottomFromSystem = remember(context, systemNavBarBottomPadding) {
        if (systemNavBarBottomPadding > 0.dp) {
            systemNavBarBottomPadding
        } else {
            val activity = context as? androidx.activity.ComponentActivity
            val decorView = activity?.window?.decorView
            val insets = if (decorView != null) androidx.core.view.ViewCompat.getRootWindowInsets(decorView) else null
            val bottomPx = insets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
            if (bottomPx > 0) {
                (bottomPx / context.resources.displayMetrics.density).dp
            } else {
                0.dp
            }
        }
    }

    // Si los botones de navegación del sistema están activos (>= 36.dp), guarda espacio suficiente por encima (+ 36.dp).
    // Si no están en uso o están ocultos (< 36.dp), se suben a una altura cómoda de al menos 52.dp para que no queden bajos.
    val bottomSpacing = if (navBottomFromSystem >= 36.dp) {
        navBottomFromSystem + 36.dp
    } else {
        maxOf(52.dp, navBottomFromSystem + 32.dp)
    }

    var amountText by remember {
        mutableStateOf(existingExpense?.amount?.let { String.format(Locale.US, "%.2f", it) } ?: "")
    }
    var selectedCurrency by remember {
        mutableStateOf(existingExpense?.currency ?: trip.baseCurrency)
    }
    var exchangeRateText by remember {
        mutableStateOf(
            existingExpense?.exchangeRate?.let { String.format(Locale.US, "%.4f", it) }
                ?: String.format(
                    Locale.US,
                    "%.4f",
                    CurrencyCatalog.getDefaultExchangeRate(selectedCurrency, trip.baseCurrency)
                )
        )
    }
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    val standardNonGeneralNames = remember {
        CategoryCatalog.categories.dropLast(1).map { it.name.lowercase(Locale.ROOT) }.toSet()
    }
    var customGeneralButtonLabel by remember {
        val existingCat = existingExpense?.category
        val initialCustom = if (
            existingCat != null &&
            !standardNonGeneralNames.contains(existingCat.lowercase(Locale.ROOT)) &&
            !existingCat.equals("General", ignoreCase = true) &&
            !existingCat.equals("General & Otros", ignoreCase = true)
        ) {
            existingCat
        } else {
            "General"
        }
        mutableStateOf(initialCustom)
    }
    var showRenameGeneralDialog by remember { mutableStateOf(false) }
    var tempGeneralNameInput by remember { mutableStateOf("") }

    var selectedCategory by remember {
        val existingCat = existingExpense?.category
        val initialCat = if (existingCat.equals("General & Otros", ignoreCase = true)) {
            "General"
        } else {
            existingCat ?: CategoryCatalog.categories.first().name
        }
        mutableStateOf(initialCat)
    }
    var selectedDate by remember {
        mutableStateOf(existingExpense?.date ?: LocalDate.now().toString())
    }
    var prorationDaysText by remember {
        mutableStateOf(existingExpense?.prorationDays?.toString() ?: "")
    }

    var placeName by remember { mutableStateOf(existingExpense?.placeName ?: "") }
    // Dirección (para gastos generales) u Origen y Destino (para Transporte, Taxi y Vuelos)
    var addressText by remember { mutableStateOf(existingExpense?.country ?: "") }
    var latitudeValue by remember { mutableStateOf(existingExpense?.latitude) }
    var longitudeValue by remember { mutableStateOf(existingExpense?.longitude) }

    // Campos de Origen y Destino con buscador estilo Google Maps para Transporte, Taxi y Vuelos
    var originText by remember {
        val initialOrigin = when {
            !existingExpense?.originName.isNullOrBlank() -> existingExpense?.originName ?: ""
            existingExpense?.country?.contains("→") == true ->
                existingExpense.country.substringBefore("→").trim()
            else -> ""
        }
        mutableStateOf(initialOrigin)
    }
    var originLatitudeValue by remember { mutableStateOf(existingExpense?.originLatitude) }
    var originLongitudeValue by remember { mutableStateOf(existingExpense?.originLongitude) }
    var originSuggestions by remember { mutableStateOf<List<NominatimPlaceSuggestion>>(emptyList()) }
    var isSearchingOrigin by remember { mutableStateOf(false) }

    var destinationText by remember {
        val initialDest = when {
            existingExpense?.country?.contains("→") == true ->
                existingExpense.country.substringAfter("→").trim()
            existingExpense != null && CategoryCatalog.isRouteCategory(existingExpense.category) ->
                existingExpense.country.ifBlank { existingExpense.placeName }
            else -> ""
        }
        mutableStateOf(initialDest)
    }
    var destinationSuggestions by remember { mutableStateOf<List<NominatimPlaceSuggestion>>(emptyList()) }
    var isSearchingDestination by remember { mutableStateOf(false) }

    var activeRouteSearchField by remember { mutableStateOf<String?>(null) } // "origin" | "destination" | null

    LaunchedEffect(originText, activeRouteSearchField) {
        if (activeRouteSearchField != "origin") return@LaunchedEffect
        val q = originText.trim()
        if (q.length < 2) {
            originSuggestions = emptyList()
            isSearchingOrigin = false
            return@LaunchedEffect
        }
        isSearchingOrigin = true
        delay(350)
        originSuggestions = NominatimService.searchPlacesByProximity(
            query = q,
            userLat = userLocation?.first,
            userLon = userLocation?.second,
            context = context
        )
        isSearchingOrigin = false
    }

    LaunchedEffect(destinationText, activeRouteSearchField) {
        if (activeRouteSearchField != "destination") return@LaunchedEffect
        val q = destinationText.trim()
        if (q.length < 2) {
            destinationSuggestions = emptyList()
            isSearchingDestination = false
            return@LaunchedEffect
        }
        isSearchingDestination = true
        delay(350)
        destinationSuggestions = NominatimService.searchPlacesByProximity(
            query = q,
            userLat = userLocation?.first,
            userLon = userLocation?.second,
            context = context
        )
        isSearchingDestination = false
    }

    var notes by remember { mutableStateOf(existingExpense?.notes ?: "") }
    var paidById by remember {
        mutableStateOf(existingExpense?.paidById ?: participants.firstOrNull()?.id ?: DebtCalculator.BOTE_COMUN_ID)
    }
    var paymentMethod by remember {
        mutableStateOf(existingExpense?.paymentMethod ?: PaymentMethodCatalog.methods.first())
    }
    var photos by remember {
        mutableStateOf(existingExpense?.photos ?: emptyList())
    }
    var previewReceiptUri by remember { mutableStateOf<String?>(null) }

    // Estado de división (Splits)
    var splitMode by remember { mutableStateOf(SplitMode.EQUAL) }
    val includedParticipantIds = remember {
        val map = mutableStateMapOf<String, Boolean>()
        if (existingExpense != null && existingExpense.splits.isNotEmpty()) {
            participants.forEach { p ->
                map[p.id] = existingExpense.splits.any { it.participantId == p.id && it.amount > 0.0 }
            }
        } else {
            participants.forEach { p -> map[p.id] = true }
        }
        map
    }
    val exactSplitInputs = remember {
        val map = mutableStateMapOf<String, String>()
        participants.forEach { p ->
            val existingAmt = existingExpense?.splits?.find { it.participantId == p.id }?.amount
            map[p.id] = existingAmt?.let { String.format(Locale.US, "%.2f", it) } ?: ""
        }
        map
    }
    val pctSplitInputs = remember {
        val map = mutableStateMapOf<String, String>()
        val defaultPct = if (participants.isNotEmpty()) 100.0 / participants.size else 100.0
        participants.forEach { p ->
            val existingPct = existingExpense?.splits?.find { it.participantId == p.id }?.percentage
            map[p.id] = String.format(Locale.US, "%.1f", existingPct ?: defaultPct)
        }
        map
    }

    var validationError by remember { mutableStateOf<String?>(null) }

    // Permiso de ubicación GPS al abrir el modal o pulsar botón GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onRequestLocationRefresh()
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val initialFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        delay(150)
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Photo Picker múltiple sin permisos (permite elegir tantos recibos o fotos como se desee)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            photos = photos + uris.map { it.toString() }
        }
    }

    // Cámara rápida para ticket o recuerdo
    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val file = File(context.cacheDir, "recuerdo_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                photos = photos + Uri.fromFile(file).toString()
            } catch (_: Exception) {
            }
        }
    }

    val numericAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val numericRate = exchangeRateText.replace(",", ".").toDoubleOrNull() ?: 1.0
    val amountInBase = remember(numericAmount, numericRate, selectedCurrency, trip.baseCurrency) {
        if (selectedCurrency.equals(trip.baseCurrency, ignoreCase = true)) {
            ((numericAmount * 100.0).roundToLong()) / 100.0
        } else {
            ((numericAmount * numericRate * 100.0).roundToLong()) / 100.0
        }
    }

    Dialog(
        onDismissRequest = {
            onClearPlaceSuggestions()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                surface = Color.White,
                onSurface = Color(0xFF475569),
                onSurfaceVariant = Color(0xFF64748B),
                outline = Color(0xFFCBD5E1)
            )
        ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 10.dp)
                .padding(top = 10.dp, bottom = bottomSpacing)
                .imePadding(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header del Modal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OceanNavy)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEdit) texts.editExpenseTitle else texts.addExpenseTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    IconButton(
                        onClick = {
                            onClearPlaceSuggestions()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. IMPORTE Y MULTIDIVISA
                    val step1Title = when (currentLang) {
                        AppLanguage.ES -> "1. Importe y Moneda"
                        AppLanguage.EN -> "1. Amount & Currency"
                        AppLanguage.FR -> "1. Montant et Devise"
                        AppLanguage.IT -> "1. Importo e Valuta"
                        AppLanguage.ZH -> "1. 金额与货币"
                        AppLanguage.NL -> "1. Bedrag en Valuta"
                        AppLanguage.DE -> "1. Betrag & Währung"
                        AppLanguage.PL -> "1. Kwota i Waluta"
                        AppLanguage.PT -> "1. Montante e Moeda"
                    }
                    Text(
                        text = step1Title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = {
                                Text("${texts.amountLabel} (${CurrencyCatalog.getSymbol(selectedCurrency)})")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.4f)
                                .focusRequester(initialFocusRequester)
                                .testTag("expense_amount_input")
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { currencyMenuExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "$selectedCurrency (${CurrencyCatalog.getSymbol(selectedCurrency)})",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            DropdownMenu(
                                expanded = currencyMenuExpanded,
                                onDismissRequest = { currencyMenuExpanded = false }
                            ) {
                                CurrencyCatalog.currencies.forEach { curr ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${curr.code} - ${CurrencyCatalog.getDisplayName(curr, currentLang)} (${curr.symbol})")
                                        },
                                        onClick = {
                                            selectedCurrency = curr.code
                                            val newDefaultRate = CurrencyCatalog.getDefaultExchangeRate(
                                                curr.code,
                                                trip.baseCurrency
                                            )
                                            exchangeRateText = String.format(Locale.US, "%.4f", newDefaultRate)
                                            currencyMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Si la divisa elegida difiere de la moneda base del viaje, mostrar tasa editable y conversión
                    if (!selectedCurrency.equals(trip.baseCurrency, ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, TravelBlue.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val rateLabel = when (currentLang) {
                                    AppLanguage.ES -> "Tasa (1 $selectedCurrency = X ${trip.baseCurrency})"
                                    AppLanguage.EN -> "Rate (1 $selectedCurrency = X ${trip.baseCurrency})"
                                    AppLanguage.FR -> "Taux (1 $selectedCurrency = X ${trip.baseCurrency})"
                                    AppLanguage.IT -> "Tasso (1 $selectedCurrency = X ${trip.baseCurrency})"
                                    AppLanguage.ZH -> "汇率 (1 $selectedCurrency = X ${trip.baseCurrency})"
                                    AppLanguage.NL -> "Wisselkoers (1 $selectedCurrency = X ${trip.baseCurrency})"
                                    AppLanguage.DE -> "Wechselkurs (1 $selectedCurrency = X ${trip.baseCurrency})"
                                    AppLanguage.PL -> "Kurs (1 $selectedCurrency = X ${trip.baseCurrency})"
                                    AppLanguage.PT -> "Taxa (1 $selectedCurrency = X ${trip.baseCurrency})"
                                }
                                val inBaseLabel = when (currentLang) {
                                    AppLanguage.ES -> "En moneda base:"
                                    AppLanguage.EN -> "In base currency:"
                                    AppLanguage.FR -> "En devise de base :"
                                    AppLanguage.IT -> "In valuta base:"
                                    AppLanguage.ZH -> "折合基础货币："
                                    AppLanguage.NL -> "In basisvaluta:"
                                    AppLanguage.DE -> "In Basiswährung:"
                                    AppLanguage.PL -> "W walucie bazowej:"
                                    AppLanguage.PT -> "Na moeda base:"
                                }
                                OutlinedTextField(
                                    value = exchangeRateText,
                                    onValueChange = { exchangeRateText = it },
                                    label = { Text(rateLabel) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = inBaseLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        text = FormatUtils.formatCurrency(amountInBase, trip.baseCurrency),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = TravelBlue
                                    )
                                }
                            }
                        }
                    }

                    // 2. LUGAR / ESTABLECIMIENTO u ORIGEN Y DESTINO
                    val isRouteExpense = CategoryCatalog.isRouteCategory(selectedCategory)
                    val step2Title = if (isRouteExpense) {
                        when (currentLang) {
                            AppLanguage.ES -> "2. Origen y Destino"
                            AppLanguage.EN -> "2. Origin & Destination"
                            AppLanguage.FR -> "2. Origine et Destination"
                            AppLanguage.IT -> "2. Origine e Destinazione"
                            AppLanguage.ZH -> "2. 出发地与目的地"
                            AppLanguage.NL -> "2. Herkomst en Bestemming"
                            AppLanguage.DE -> "2. Abfahrt & Ziel"
                            AppLanguage.PL -> "2. Miejsce wyjazdu i docelowe"
                            AppLanguage.PT -> "2. Origem e Destino"
                        }
                    } else {
                        when (currentLang) {
                            AppLanguage.ES -> "2. Lugar / Establecimiento"
                            AppLanguage.EN -> "2. Place / Venue"
                            AppLanguage.FR -> "2. Lieu / Établissement"
                            AppLanguage.IT -> "2. Luogo / Esercizio"
                            AppLanguage.ZH -> "2. 地点 / 场所"
                            AppLanguage.NL -> "2. Locatie / Gelegenheid"
                            AppLanguage.DE -> "2. Ort / Betrieb"
                            AppLanguage.PL -> "2. Miejsce / Lokal"
                            AppLanguage.PT -> "2. Local / Estabelecimento"
                        }
                    }
                    Text(
                        text = step2Title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy
                    )

                    if (!isRouteExpense) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = placeName,
                                onValueChange = {
                                    placeName = it
                                    activeRouteSearchField = null
                                    onPlaceQueryChanged(it)
                                },
                                label = { Text(texts.placeOrMerchantLabel) },
                                leadingIcon = {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = TravelBlue)
                                },
                                trailingIcon = {
                                    if (isSearchingPlaces) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("expense_place_input")
                            )

                            Surface(
                                onClick = {
                                    coroutineScope.launch {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                        onRequestLocationRefresh()
                                        val coords = userLocation ?: NominatimService.fetchCurrentDeviceLocation(context)
                                        if (coords != null) {
                                            latitudeValue = coords.first
                                            longitudeValue = coords.second
                                            val geoResult = NominatimService.reverseGeocodeLocation(context, coords.first, coords.second)
                                            if (geoResult != null) {
                                                placeName = geoResult.first
                                                if (geoResult.second.isNotBlank()) {
                                                    addressText = geoResult.second
                                                }
                                            } else if (placeName.isBlank()) {
                                                placeName = "${coords.first}, ${coords.second}"
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE0EDFF),
                                border = BorderStroke(1.dp, TravelBlue),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "GPS",
                                        tint = TravelBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "GPS",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TravelBlue
                                    )
                                }
                            }
                        }

                        if (userLocation != null) {
                            val gpsActiveHint = when (currentLang) {
                                AppLanguage.ES -> "📍 GPS activo — Ordenando lugares más cercanos primero"
                                AppLanguage.EN -> "📍 GPS active — Sorting closest places first"
                                AppLanguage.FR -> "📍 GPS actif — Lieux les plus proches en premier"
                                AppLanguage.IT -> "📍 GPS attivo — Ordina prima i luoghi più vicini"
                                AppLanguage.ZH -> "📍 GPS 已启用 — 优先排列最近的地点"
                                AppLanguage.NL -> "📍 GPS actief — Dichtstbijzijnde locaties eerst"
                                AppLanguage.DE -> "📍 GPS aktiv — Nächste Orte zuerst"
                                AppLanguage.PL -> "📍 GPS aktywny — Najbliższe miejsca na początku"
                                AppLanguage.PT -> "📍 GPS ativo — A ordenar locais mais próximos primeiro"
                            }
                            Text(
                                text = gpsActiveHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldGreenDark
                            )
                        }

                        // Lista desplegable de sugerencias de Lugar / Establecimiento
                        if (placeSuggestions.isNotEmpty() && activeRouteSearchField == null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, TravelBlue.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    placeSuggestions.take(6).forEach { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    placeName = suggestion.shortPlaceName
                                                    addressText = suggestion.displayName
                                                    latitudeValue = suggestion.latitude
                                                    longitudeValue = suggestion.longitude
                                                    onClearPlaceSuggestions()
                                                }
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = suggestion.shortPlaceName,
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = OceanNavy,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = suggestion.displayName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF64748B),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (suggestion.distanceKm != null) {
                                                val distText = if (suggestion.distanceKm < 1.0) {
                                                    "${(suggestion.distanceKm * 1000).toInt()} m"
                                                } else {
                                                    String.format(Locale.US, "%.1f km", suggestion.distanceKm)
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFE0EDFF)
                                                ) {
                                                    Text(
                                                        text = distText,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = TravelBlue,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = Color(0xFFE2E8F0))
                                    }
                                }
                            }
                        }

                        // Campo único de Dirección para el resto de categorías
                        val addressLabel = when (currentLang) {
                            AppLanguage.ES -> "Dirección"
                            AppLanguage.EN -> "Address"
                            AppLanguage.FR -> "Adresse"
                            AppLanguage.IT -> "Indirizzo"
                            AppLanguage.ZH -> "地址"
                            AppLanguage.NL -> "Adres"
                            AppLanguage.DE -> "Adresse"
                            AppLanguage.PL -> "Adres"
                            AppLanguage.PT -> "Morada"
                        }
                        val addressPlaceholder = when (currentLang) {
                            AppLanguage.ES -> "Calle, ciudad o dirección del lugar..."
                            AppLanguage.EN -> "Street, city or address of the place..."
                            AppLanguage.FR -> "Rue, ville ou adresse du lieu..."
                            AppLanguage.IT -> "Via, città o indirizzo del luogo..."
                            AppLanguage.ZH -> "街道、城市或地点地址..."
                            AppLanguage.NL -> "Straat, stad of adres van de locatie..."
                            AppLanguage.DE -> "Straße, Stadt oder Adresse des Ortes..."
                            AppLanguage.PL -> "Ulica, miasto lub adres miejsca..."
                            AppLanguage.PT -> "Rua, cidade ou morada do local..."
                        }
                        OutlinedTextField(
                            value = addressText,
                            onValueChange = { addressText = it },
                            label = { Text(addressLabel) },
                            placeholder = { Text(addressPlaceholder) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_address_input")
                        )
                    } else {
                        // En Transporte, Taxi y Vuelos: mostrar Origen y Destino con buscador de calles, lugares, ciudades, etc.
                        val routeSearchPlaceholder = when (currentLang) {
                            AppLanguage.ES -> "Buscar calle, lugar, ciudad, estación o aeropuerto..."
                            AppLanguage.EN -> "Search street, place, city, station or airport..."
                            AppLanguage.FR -> "Rechercher rue, lieu, ville, gare ou aéroport..."
                            AppLanguage.IT -> "Cerca via, luogo, città, stazione o aeroporto..."
                            AppLanguage.ZH -> "搜索街道、地点、城市、车站或机场..."
                            AppLanguage.NL -> "Zoek straat, plaats, stad, station of luchthaven..."
                            AppLanguage.DE -> "Straße, Ort, Stadt, Bahnhof oder Flughafen suchen..."
                            AppLanguage.PL -> "Szukaj ulicy, miejsca, miasta, stacji lub lotniska..."
                            AppLanguage.PT -> "Pesquisar rua, local, cidade, estação ou aeroporto..."
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, TravelBlue.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Campo 1: ORIGEN
                                OutlinedTextField(
                                    value = originText,
                                    onValueChange = {
                                        originText = it
                                        activeRouteSearchField = "origin"
                                        onClearPlaceSuggestions()
                                    },
                                    label = { Text(texts.originLabel) },
                                    placeholder = { Text(routeSearchPlaceholder) },
                                    leadingIcon = {
                                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = EmeraldGreenDark)
                                    },
                                    trailingIcon = {
                                        if (isSearchingOrigin) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else if (originText.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    originText = ""
                                                    originLatitudeValue = null
                                                    originLongitudeValue = null
                                                    originSuggestions = emptyList()
                                                }
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("expense_origin_input")
                                 )

                                if (originSuggestions.isNotEmpty() && activeRouteSearchField == "origin") {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, EmeraldGreenDark.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            originSuggestions.take(5).forEach { suggestion ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            originText = suggestion.shortPlaceName
                                                            originLatitudeValue = suggestion.latitude
                                                            originLongitudeValue = suggestion.longitude
                                                            originSuggestions = emptyList()
                                                            activeRouteSearchField = null
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "🛫 ${suggestion.shortPlaceName}",
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                            color = OceanNavy,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = suggestion.displayName,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color(0xFF64748B),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                HorizontalDivider(color = Color(0xFFE2E8F0))
                                            }
                                        }
                                    }
                                }

                                // Campo 2: DESTINO
                                OutlinedTextField(
                                    value = destinationText,
                                    onValueChange = {
                                        destinationText = it
                                        activeRouteSearchField = "destination"
                                        onClearPlaceSuggestions()
                                    },
                                    label = { Text(texts.destinationLabel) },
                                    placeholder = { Text(routeSearchPlaceholder) },
                                    leadingIcon = {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = TravelBlue)
                                    },
                                    trailingIcon = {
                                        if (isSearchingDestination) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else if (destinationText.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    destinationText = ""
                                                    destinationSuggestions = emptyList()
                                                }
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("expense_destination_input")
                                )

                                if (destinationSuggestions.isNotEmpty() && activeRouteSearchField == "destination") {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, TravelBlue.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            destinationSuggestions.take(5).forEach { suggestion ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            destinationText = suggestion.shortPlaceName
                                                            latitudeValue = suggestion.latitude
                                                            longitudeValue = suggestion.longitude
                                                            destinationSuggestions = emptyList()
                                                            activeRouteSearchField = null
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "🏁 ${suggestion.shortPlaceName}",
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                            color = OceanNavy,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = suggestion.displayName,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color(0xFF64748B),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                HorizontalDivider(color = Color(0xFFE2E8F0))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. CATEGORÍA
                    val step3Title = when (currentLang) {
                        AppLanguage.ES -> "3. Categoría"
                        AppLanguage.EN -> "3. Category"
                        AppLanguage.FR -> "3. Catégorie"
                        AppLanguage.IT -> "3. Categoria"
                        AppLanguage.ZH -> "3. 类别"
                        AppLanguage.NL -> "3. Categorie"
                        AppLanguage.DE -> "3. Kategorie"
                        AppLanguage.PL -> "3. Kategoria"
                        AppLanguage.PT -> "3. Categoria"
                    }
                    Text(
                        text = step3Title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryCatalog.categories.forEach { cat ->
                            val isGeneralButton = cat.name.equals("General", ignoreCase = true)
                            val displayLabel = if (isGeneralButton) {
                                if (customGeneralButtonLabel.equals("General", ignoreCase = true)) {
                                    CategoryCatalog.displayName("General", currentLang)
                                } else {
                                    customGeneralButtonLabel // Nombre personalizado puesto por el usuario: no se traduce
                                }
                            } else {
                                CategoryCatalog.displayName(cat.name, currentLang)
                            }
                            val isSelected = if (isGeneralButton) {
                                selectedCategory.equals(customGeneralButtonLabel, ignoreCase = true) ||
                                        selectedCategory.equals("General", ignoreCase = true)
                            } else {
                                selectedCategory.equals(cat.name, ignoreCase = true)
                            }

                            Surface(
                                onClick = {
                                    if (isGeneralButton) {
                                        selectedCategory = customGeneralButtonLabel
                                        tempGeneralNameInput = if (customGeneralButtonLabel.equals("General", ignoreCase = true)) {
                                            ""
                                        } else {
                                            customGeneralButtonLabel
                                        }
                                        showRenameGeneralDialog = true
                                    } else {
                                        selectedCategory = cat.name
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) cat.color else cat.color.copy(alpha = 0.12f),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isSelected) cat.color else cat.color.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else cat.color,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = displayLabel,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else OceanNavy
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                val d = try {
                                    LocalDate.parse(selectedDate)
                                } catch (e: Exception) {
                                    LocalDate.now()
                                }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        selectedDate = String.format(
                                            Locale.US,
                                            "%04d-%02d-%02d",
                                            year,
                                            month + 1,
                                            dayOfMonth
                                        )
                                    },
                                    d.year,
                                    d.monthValue - 1,
                                    d.dayOfMonth
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TravelBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = when (currentLang) {
                                            AppLanguage.ES -> "Fecha del gasto"
                                            AppLanguage.EN -> "Expense date"
                                            AppLanguage.FR -> "Date de la dépense"
                                            AppLanguage.IT -> "Data della spesa"
                                            AppLanguage.ZH -> "支出日期"
                                            AppLanguage.NL -> "Uitgavedatum"
                                            AppLanguage.DE -> "Ausgabendatum"
                                            AppLanguage.PL -> "Data wydatku"
                                            AppLanguage.PT -> "Data da despesa"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = FormatUtils.formatShortDate(selectedDate, currentLang.locale),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = OceanNavy
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = prorationDaysText,
                            onValueChange = { prorationDaysText = it.filter { ch -> ch.isDigit() } },
                            label = { Text(texts.prorateDaysLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    val pDays = prorationDaysText.toIntOrNull()
                    if (pDays != null && pDays > 1 && amountInBase > 0) {
                        val dailyCost = amountInBase / pDays
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEDE9FE),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val proratedText = when (currentLang) {
                                AppLanguage.ES -> "🗓️ Gasto prorrateado en $pDays días → Supone ${FormatUtils.formatCurrency(dailyCost, trip.baseCurrency)} por día"
                                AppLanguage.EN -> "🗓️ Expense prorated over $pDays days → ${FormatUtils.formatCurrency(dailyCost, trip.baseCurrency)} per day"
                                AppLanguage.FR -> "🗓️ Dépense étalée sur $pDays jours → ${FormatUtils.formatCurrency(dailyCost, trip.baseCurrency)} par jour"
                                AppLanguage.IT -> "🗓️ Spesa ripartita su $pDays giorni → ${FormatUtils.formatCurrency(dailyCost, trip.baseCurrency)} al giorno"
                                AppLanguage.ZH -> "🗓️ 费用按 $pDays 天分摊 → 每天 ${FormatUtils.formatCurrency(dailyCost, trip.baseCurrency)}"
                                AppLanguage.NL -> "🗓️ Uitgave gespreid over $pDays dagen → ${FormatUtils.formatCurrency(dailyCost, trip.baseCurrency)} per dag"
                                AppLanguage.DE -> "🗓️ Ausgabe auf $pDays Tage umgelegt → ${FormatUtils.formatCurrency(dailyCost, trip.baseCurrency)} pro Tag"
                                AppLanguage.PL -> "🗓️ Wydatek rozłożony na $pDays dni → ${FormatUtils.formatCurrency(dailyCost, trip.baseCurrency)} dziennie"
                                AppLanguage.PT -> "🗓️ Despesa rateada em $pDays dias → Supõe ${FormatUtils.formatCurrency(dailyCost, trip.baseCurrency)} por dia"
                            }
                            Text(
                                text = proratedText,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF6D28D9),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(texts.notesLabel) },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 4. PARTICIPANTES
                    val step4Title = when (currentLang) {
                        AppLanguage.ES -> "4. Participantes"
                        AppLanguage.EN -> "4. Participants"
                        AppLanguage.FR -> "4. Participants"
                        AppLanguage.IT -> "4. Partecipanti"
                        AppLanguage.ZH -> "4. 参与者"
                        AppLanguage.NL -> "4. Deelnemers"
                        AppLanguage.DE -> "4. Teilnehmer"
                        AppLanguage.PL -> "4. Uczestnicy"
                        AppLanguage.PT -> "4. Participantes"
                    }
                    val step4Subtitle = when (currentLang) {
                        AppLanguage.ES -> "Selecciona quién paga este gasto (el Bote común o un participante):"
                        AppLanguage.EN -> "Select who pays for this expense (Common Pot or a participant):"
                        AppLanguage.FR -> "Sélectionnez qui paie cette dépense (la cagnotte commune ou un participant) :"
                        AppLanguage.IT -> "Seleziona chi paga questa spesa (la cassa comune o un partecipante):"
                        AppLanguage.ZH -> "选择由谁支付此费用（公账或某位参与者）："
                        AppLanguage.NL -> "Selecteer wie deze uitgave betaalt (de gemeenschappelijke pot of een deelnemer):"
                        AppLanguage.DE -> "Wählen Sie, wer diese Ausgabe bezahlt (Gemeinschaftskasse oder ein Teilnehmer):"
                        AppLanguage.PL -> "Wybierz, kto płaci za ten wydatek (wspólny budżet lub uczestnik):"
                        AppLanguage.PT -> "Selecione quem paga esta despesa (o Fundo comum ou um participante):"
                    }
                    Text(
                        text = step4Title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy
                    )
                    Text(
                        text = step4Subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // El Bote siempre en primera posición delante de los participantes
                        val isBoteSelected = paidById == DebtCalculator.BOTE_COMUN_ID
                        Surface(
                            onClick = {
                                paidById = DebtCalculator.BOTE_COMUN_ID
                                paymentMethod = "Bote Común"
                            },
                            shape = CircleShape,
                            color = if (isBoteSelected) EmeraldGreen else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isBoteSelected) EmeraldGreen else Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = "🐷 ${texts.commonPotName}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (isBoteSelected) Color.White else OceanNavy,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }

                        participants.forEach { p ->
                            val isSelected = paidById == p.id
                            Surface(
                                onClick = { paidById = p.id },
                                shape = CircleShape,
                                color = if (isSelected) TravelBlue else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSelected) TravelBlue else Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = "👤 ${p.name}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else OceanNavy,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentMethodCatalog.methods.forEach { method ->
                            val isSelected = paymentMethod.equals(method, ignoreCase = true)
                            val methodPrefix = if (method.equals("Bote Común", ignoreCase = true)) "🐷 " else ""
                            Surface(
                                onClick = {
                                    paymentMethod = method
                                    if (method == "Bote Común") {
                                        paidById = DebtCalculator.BOTE_COMUN_ID
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) OceanNavy else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSelected) OceanNavy else Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = "$methodPrefix${PaymentMethodCatalog.displayName(method, currentLang)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // 5. DIVISIÓN DEL GASTO
                    val step5Title = when (currentLang) {
                        AppLanguage.ES -> "5. División del Gasto"
                        AppLanguage.EN -> "5. Expense Split"
                        AppLanguage.FR -> "5. Répartition de la dépense"
                        AppLanguage.IT -> "5. Divisione della spesa"
                        AppLanguage.ZH -> "5. 费用分摊"
                        AppLanguage.NL -> "5. Kostenverdeling"
                        AppLanguage.DE -> "5. Ausgabenaufteilung"
                        AppLanguage.PL -> "5. Podział wydatku"
                        AppLanguage.PT -> "5. Divisão da Despesa"
                    }
                    Text(
                        text = step5Title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SplitMode.entries.forEach { mode ->
                            val isSelected = splitMode == mode
                            Surface(
                                onClick = { splitMode = mode },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) TravelBlue else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = mode.getLabel(currentLang),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else OceanNavy,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            when (splitMode) {
                                SplitMode.EQUAL -> {
                                    val activeCount = participants.count { includedParticipantIds[it.id] == true }.coerceAtLeast(1)
                                    val sharePerPerson = if (amountInBase > 0) amountInBase / activeCount else 0.0
                                    participants.forEach { p ->
                                        val checked = includedParticipantIds[p.id] == true
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = checked,
                                                    onCheckedChange = { includedParticipantIds[p.id] = it }
                                                )
                                                Text(
                                                    text = p.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = OceanNavy
                                                )
                                            }
                                            Text(
                                                text = if (checked) FormatUtils.formatCurrency(sharePerPerson, trip.baseCurrency) else "0,00 ${CurrencyCatalog.getSymbol(trip.baseCurrency)}",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = if (checked) TravelBlue else Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }

                                SplitMode.EXACT -> {
                                    val sumExact = participants.sumOf {
                                        exactSplitInputs[it.id]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                                    }
                                    val exactStatusText = when (currentLang) {
                                        AppLanguage.ES -> "Suma asignada: ${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)} / Total: ${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)}"
                                        AppLanguage.EN -> "Assigned sum: ${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)} / Total: ${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)}"
                                        AppLanguage.FR -> "Somme assignée : ${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)} / Total : ${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)}"
                                        AppLanguage.IT -> "Somma assegnata: ${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)} / Totale: ${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)}"
                                        AppLanguage.ZH -> "分配总额：${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)} / 总计：${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)}"
                                        AppLanguage.NL -> "Toegewezen som: ${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)} / Totaal: ${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)}"
                                        AppLanguage.DE -> "Zugewiesene Summe: ${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)} / Gesamt: ${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)}"
                                        AppLanguage.PL -> "Przydzielona suma: ${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)} / Łącznie: ${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)}"
                                        AppLanguage.PT -> "Soma atribuída: ${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)} / Total: ${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)}"
                                    }
                                    Text(
                                        text = exactStatusText,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (abs(sumExact - amountInBase) <= 0.05) EmeraldGreenDark else CoralRed
                                    )
                                    participants.forEach { p ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = p.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = exactSplitInputs[p.id] ?: "",
                                                onValueChange = { exactSplitInputs[p.id] = it },
                                                label = { Text(CurrencyCatalog.getSymbol(trip.baseCurrency)) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier.width(130.dp)
                                            )
                                        }
                                    }
                                }

                                SplitMode.PERCENTAGE -> {
                                    val sumPct = participants.sumOf {
                                        pctSplitInputs[it.id]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                                    }
                                    val pctStatusText = when (currentLang) {
                                        AppLanguage.ES -> "Suma de porcentajes: ${String.format(Locale.US, "%.1f", sumPct)}% (debe sumar 100%)"
                                        AppLanguage.EN -> "Percentage sum: ${String.format(Locale.US, "%.1f", sumPct)}% (must equal 100%)"
                                        AppLanguage.FR -> "Somme des pourcentages : ${String.format(Locale.US, "%.1f", sumPct)}% (doit égaler 100%)"
                                        AppLanguage.IT -> "Somma percentuali: ${String.format(Locale.US, "%.1f", sumPct)}% (deve essere 100%)"
                                        AppLanguage.ZH -> "百分比总和：${String.format(Locale.US, "%.1f", sumPct)}%（必须为 100%）"
                                        AppLanguage.NL -> "Som percentages: ${String.format(Locale.US, "%.1f", sumPct)}% (moet 100% zijn)"
                                        AppLanguage.DE -> "Prozentsumme: ${String.format(Locale.US, "%.1f", sumPct)}% (muss 100% ergeben)"
                                        AppLanguage.PL -> "Suma procentów: ${String.format(Locale.US, "%.1f", sumPct)}% (musi wynosić 100%)"
                                        AppLanguage.PT -> "Soma de percentagens: ${String.format(Locale.US, "%.1f", sumPct)}% (deve somar 100%)"
                                    }
                                    Text(
                                        text = pctStatusText,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (abs(sumPct - 100.0) <= 0.2) EmeraldGreenDark else CoralRed
                                    )
                                    participants.forEach { p ->
                                        val pctVal = pctSplitInputs[p.id]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                                        val calcAmt = (amountInBase * pctVal) / 100.0
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = p.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                                Text(
                                                    text = FormatUtils.formatCurrency(calcAmt, trip.baseCurrency),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TravelBlue
                                                )
                                            }
                                            OutlinedTextField(
                                                value = pctSplitInputs[p.id] ?: "",
                                                onValueChange = { pctSplitInputs[p.id] = it },
                                                label = { Text("%") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier.width(110.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. RECIBOS Y RECUERDOS
                    val step6Title = when (currentLang) {
                        AppLanguage.ES -> "6. Recibos y Recuerdos"
                        AppLanguage.EN -> "6. Receipts & Memories"
                        AppLanguage.FR -> "6. Reçus et Souvenirs"
                        AppLanguage.IT -> "6. Ricevute e Ricordi"
                        AppLanguage.ZH -> "6. 收据与回忆"
                        AppLanguage.NL -> "6. Bonnen en Herinneringen"
                        AppLanguage.DE -> "6. Belege & Erinnerungen"
                        AppLanguage.PL -> "6. Rachunki i Wspomnienia"
                        AppLanguage.PT -> "6. Recibos e Lembranças"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = step6Title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = OceanNavy
                        )
                        if (photos.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, TravelBlue.copy(alpha = 0.4f))
                            ) {
                                val receiptWord = if (photos.size == 1) {
                                    when (currentLang) {
                                        AppLanguage.ES -> "recibo"
                                        AppLanguage.EN -> "receipt"
                                        AppLanguage.FR -> "reçu"
                                        AppLanguage.IT -> "ricevuta"
                                        AppLanguage.ZH -> "张收据"
                                        AppLanguage.NL -> "bon"
                                        AppLanguage.DE -> "Beleg"
                                        AppLanguage.PL -> "rachunek"
                                        AppLanguage.PT -> "recibo"
                                    }
                                } else {
                                    when (currentLang) {
                                        AppLanguage.ES -> "recibos"
                                        AppLanguage.EN -> "receipts"
                                        AppLanguage.FR -> "reçus"
                                        AppLanguage.IT -> "ricevute"
                                        AppLanguage.ZH -> "张收据"
                                        AppLanguage.NL -> "bonnen"
                                        AppLanguage.DE -> "Belege"
                                        AppLanguage.PL -> "rachunki"
                                        AppLanguage.PT -> "recibos"
                                    }
                                }
                                Text(
                                    text = "${photos.size} $receiptWord",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TravelBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    val receiptsInfoText = when (currentLang) {
                        AppLanguage.ES -> "Añade todos los tickets, facturas, recibos o recuerdos que necesites sin límite."
                        AppLanguage.EN -> "Add as many tickets, invoices, receipts or photos as you need without limits."
                        AppLanguage.FR -> "Ajoutez autant de tickets, factures, reçus ou photos que vous le souhaitez sans limite."
                        AppLanguage.IT -> "Aggiungi tutti gli scontrini, fatture, ricevute o foto di cui hai bisogno senza limiti."
                        AppLanguage.ZH -> "无限制添加所需的所有票据、发票、收据或照片。"
                        AppLanguage.NL -> "Voeg onbeperkt alle benodigde tickets, facturen, bonnen of foto's toe."
                        AppLanguage.DE -> "Fügen Sie unbegrenzt Tickets, Rechnungen, Belege oder Fotos hinzu."
                        AppLanguage.PL -> "Dodawaj wszystkie potrzebne bilety, faktury, rachunki lub zdjęcia bez ograniczeń."
                        AppLanguage.PT -> "Adicione todos os bilhetes, faturas, recibos ou lembranças de que necessitar sem limite."
                    }
                    Text(
                        text = receiptsInfoText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(texts.galleryOption)
                        }
                        OutlinedButton(
                            onClick = { cameraPreviewLauncher.launch(null) }
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(texts.cameraOption)
                        }
                    }

                    if (photos.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            photos.forEachIndexed { idx, photoUri ->
                                Box(
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFE2E8F0))
                                        .clickable { previewReceiptUri = photoUri }
                                ) {
                                    val photoContentDesc = when (currentLang) {
                                        AppLanguage.ES -> "Recibo o recuerdo ${idx + 1}"
                                        AppLanguage.EN -> "Receipt or memory ${idx + 1}"
                                        AppLanguage.FR -> "Reçu ou souvenir ${idx + 1}"
                                        AppLanguage.IT -> "Ricevuta o ricordo ${idx + 1}"
                                        AppLanguage.ZH -> "收据或回忆 ${idx + 1}"
                                        AppLanguage.NL -> "Bon of herinnering ${idx + 1}"
                                        AppLanguage.DE -> "Beleg oder Erinnerung ${idx + 1}"
                                        AppLanguage.PL -> "Rachunek lub wspomnienie ${idx + 1}"
                                        AppLanguage.PT -> "Recibo ou lembrança ${idx + 1}"
                                    }
                                    AsyncImage(
                                        model = photoUri,
                                        contentDescription = photoContentDesc,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        onClick = {
                                            photos = photos.filterIndexed { i, _ -> i != idx }
                                        },
                                        shape = CircleShape,
                                        color = CoralRed,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = texts.deleteBtn,
                                            tint = Color.White,
                                            modifier = Modifier.padding(3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (validationError != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = validationError!!,
                                color = Color(0xFF991B1B),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Espacio inferior generoso para que cualquier botón (Bizum, Efectivo, GPS, Categorías) o campo se pueda scrollear por encima del teclado
                    Spacer(modifier = Modifier.height(200.dp))
                }

                // Footer con botones Eliminar (si es edición), Cancelar y Guardar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (existingExpense != null) {
                        OutlinedButton(
                            onClick = {
                                onDeleteExpense(existingExpense.id)
                                onDismiss()
                            },
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                            border = BorderStroke(1.dp, CoralRed),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(texts.deleteBtn, maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(texts.cancel, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            if (numericAmount <= 0.0) {
                                validationError = when (currentLang) {
                                    AppLanguage.ES -> "Introduce un importe mayor que 0."
                                    AppLanguage.EN -> "Enter an amount greater than 0."
                                    AppLanguage.FR -> "Saisissez un montant supérieur à 0."
                                    AppLanguage.IT -> "Inserisci un importo maggiore di 0."
                                    AppLanguage.ZH -> "请输入大于 0 的金额。"
                                    AppLanguage.NL -> "Voer een bedrag in groter dan 0."
                                    AppLanguage.DE -> "Geben Sie einen Betrag größer als 0 ein."
                                    AppLanguage.PL -> "Wprowadź kwotę większą niż 0."
                                    AppLanguage.PT -> "Introduza um montante superior a 0."
                                }
                                return@Button
                            }
                            val expId = existingExpense?.id ?: "exp-${UUID.randomUUID().toString().take(8)}"

                            val finalSplits: List<ExpenseSplit> = when (splitMode) {
                                SplitMode.EQUAL -> {
                                    val selectedParts = participants.filter { includedParticipantIds[it.id] == true }
                                    if (selectedParts.isEmpty()) {
                                        validationError = when (currentLang) {
                                            AppLanguage.ES -> "Selecciona al menos un participante para dividir el gasto."
                                            AppLanguage.EN -> "Select at least one participant to split the expense."
                                            AppLanguage.FR -> "Sélectionnez au moins un participant pour diviser la dépense."
                                            AppLanguage.IT -> "Seleziona almeno un partecipante per dividere la spesa."
                                            AppLanguage.ZH -> "请至少选择一位参与者以分摊费用。"
                                            AppLanguage.NL -> "Selecteer ten minste één deelnemer om de uitgave te verdelen."
                                            AppLanguage.DE -> "Wählen Sie mindestens einen Teilnehmer zur Ausgabenaufteilung aus."
                                            AppLanguage.PL -> "Wybierz przynajmniej jednego uczestnika, aby podzielić wydatek."
                                            AppLanguage.PT -> "Selecione pelo menos um participante para dividir a despesa."
                                        }
                                        return@Button
                                    }
                                    val count = selectedParts.size
                                    val perP = ((amountInBase / count) * 100.0).roundToLong() / 100.0
                                    val pct = ((100.0 / count) * 100.0).roundToLong() / 100.0
                                    selectedParts.mapIndexed { idx, p ->
                                        val amt = if (idx == count - 1) {
                                            ((amountInBase - perP * (count - 1)) * 100.0).roundToLong() / 100.0
                                        } else perP
                                        ExpenseSplit(
                                            id = "$expId-s-$idx",
                                            expenseId = expId,
                                            participantId = p.id,
                                            amount = amt,
                                            percentage = pct
                                        )
                                    }
                                }

                                SplitMode.EXACT -> {
                                    val sumExact = participants.sumOf {
                                        exactSplitInputs[it.id]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                                    }
                                    if (abs(sumExact - amountInBase) > 0.08) {
                                        validationError = when (currentLang) {
                                            AppLanguage.ES -> "La suma de importes exactos (${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)}) debe coincidir con el total (${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)})."
                                            AppLanguage.EN -> "The sum of exact amounts (${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)}) must match the total (${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)})."
                                            AppLanguage.FR -> "La somme des montants exacts (${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)}) doit correspondre au total (${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)})."
                                            AppLanguage.IT -> "La somma degli importi esatti (${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)}) deve coincidere con il totale (${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)})."
                                            AppLanguage.ZH -> "精确金额总和 (${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)}) 必须等于总计 (${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)})。"
                                            AppLanguage.NL -> "De som van exacte bedragen (${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)}) moet overeenkomen met het totaal (${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)})."
                                            AppLanguage.DE -> "Die Summe der genauen Beträge (${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)}) muss mit dem Gesamtbetrag übereinstimmen (${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)})."
                                            AppLanguage.PL -> "Suma dokładnych kwot (${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)}) musi zgadzać się z sumą całkowitą (${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)})."
                                            AppLanguage.PT -> "A soma dos montantes exatos (${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)}) deve coincidir com o total (${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)})."
                                        }
                                        return@Button
                                    }
                                    participants.mapIndexedNotNull { idx, p ->
                                        val amt = exactSplitInputs[p.id]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                                        if (amt <= 0.0) null else {
                                            val pct = if (amountInBase > 0) (amt / amountInBase) * 100.0 else 0.0
                                            ExpenseSplit(
                                                id = "$expId-s-$idx",
                                                expenseId = expId,
                                                participantId = p.id,
                                                amount = amt,
                                                percentage = pct
                                            )
                                        }
                                    }
                                }

                                SplitMode.PERCENTAGE -> {
                                    val sumPct = participants.sumOf {
                                        pctSplitInputs[it.id]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                                    }
                                    if (abs(sumPct - 100.0) > 0.5) {
                                        validationError = when (currentLang) {
                                            AppLanguage.ES -> "Los porcentajes deben sumar 100% (actualmente suman ${String.format(Locale.US, "%.1f", sumPct)}%)."
                                            AppLanguage.EN -> "Percentages must add up to 100% (currently ${String.format(Locale.US, "%.1f", sumPct)}%)."
                                            AppLanguage.FR -> "Les pourcentages doivent totaliser 100% (actuellement ${String.format(Locale.US, "%.1f", sumPct)}%)."
                                            AppLanguage.IT -> "Le percentuali devono sommare a 100% (attualmente ${String.format(Locale.US, "%.1f", sumPct)}%)."
                                            AppLanguage.ZH -> "百分比总和必须为 100%（当前为 ${String.format(Locale.US, "%.1f", sumPct)}%）。"
                                            AppLanguage.NL -> "Percentages moeten optellen tot 100% (momenteel ${String.format(Locale.US, "%.1f", sumPct)}%)."
                                            AppLanguage.DE -> "Die Prozentsätze müssen 100% ergeben (derzeit ${String.format(Locale.US, "%.1f", sumPct)}%)."
                                            AppLanguage.PL -> "Procenty muszą sumować się do 100% (obecnie ${String.format(Locale.US, "%.1f", sumPct)}%)."
                                            AppLanguage.PT -> "As percentagens devem somar 100% (atualmente somam ${String.format(Locale.US, "%.1f", sumPct)}%)."
                                        }
                                        return@Button
                                    }
                                    participants.mapIndexedNotNull { idx, p ->
                                        val pct = pctSplitInputs[p.id]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                                        if (pct <= 0.0) null else {
                                            val amt = ((amountInBase * pct / 100.0) * 100.0).roundToLong() / 100.0
                                            ExpenseSplit(
                                                id = "$expId-s-$idx",
                                                expenseId = expId,
                                                participantId = p.id,
                                                amount = amt,
                                                percentage = pct
                                            )
                                        }
                                    }
                                }
                            }

                            val isRoute = CategoryCatalog.isRouteCategory(selectedCategory)
                            val cleanOrigin = originText.trim()
                            val cleanDest = destinationText.trim()
                            val resolvedAddress = if (isRoute) {
                                when {
                                    cleanOrigin.isNotEmpty() && cleanDest.isNotEmpty() -> "$cleanOrigin → $cleanDest"
                                    cleanDest.isNotEmpty() -> cleanDest
                                    cleanOrigin.isNotEmpty() -> cleanOrigin
                                    else -> addressText.trim()
                                }
                            } else {
                                addressText.trim()
                            }
                            val resolvedPlaceName = placeName.trim().ifEmpty {
                                if (isRoute && resolvedAddress.isNotEmpty()) resolvedAddress else selectedCategory
                            }

                            val newExpense = ExpenseEntity(
                                id = expId,
                                tripId = trip.id,
                                amount = numericAmount,
                                currency = selectedCurrency,
                                amountInBaseCurrency = amountInBase,
                                exchangeRate = if (selectedCurrency.equals(trip.baseCurrency, ignoreCase = true)) 1.0 else numericRate,
                                category = selectedCategory,
                                date = selectedDate,
                                paymentMethod = paymentMethod,
                                notes = notes.trim(),
                                latitude = latitudeValue,
                                longitude = longitudeValue,
                                placeName = resolvedPlaceName,
                                country = resolvedAddress,
                                originName = if (isRoute) cleanOrigin else "",
                                originLatitude = if (isRoute) originLatitudeValue else null,
                                originLongitude = if (isRoute) originLongitudeValue else null,
                                paidById = paidById,
                                photos = photos,
                                splits = finalSplits,
                                prorationDays = prorationDaysText.toIntOrNull()?.takeIf { it > 1 },
                                createdAt = existingExpense?.createdAt ?: System.currentTimeMillis()
                            )
                            onClearPlaceSuggestions()
                            onSaveExpense(newExpense, isEdit)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TravelBlue),
                        modifier = Modifier
                            .weight(if (existingExpense != null) 1.35f else 1.5f)
                            .height(48.dp)
                            .testTag("save_expense_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = texts.saveExpenseBtn,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
        }
    }

    // Diálogo para ponerle un nombre personalizado al botón "General" solo para este gasto (volviendo a "General" en otros gastos)
    if (showRenameGeneralDialog) {
        val renameDialogTitle = when (currentLang) {
            AppLanguage.ES -> "Personalizar botón General"
            AppLanguage.EN -> "Customize General Button"
            AppLanguage.FR -> "Personnaliser le bouton Général"
            AppLanguage.IT -> "Personalizza pulsante Generale"
            AppLanguage.ZH -> "自定义常规按钮"
            AppLanguage.NL -> "Knop Algemeen aanpassen"
            AppLanguage.DE -> "Allgemein-Schaltfläche anpassen"
            AppLanguage.PL -> "Dostosuj przycisk Ogólne"
            AppLanguage.PT -> "Personalizar botão Geral"
        }
        val renameDialogDesc = when (currentLang) {
            AppLanguage.ES -> "Escribe un nombre para este botón en este gasto (para futuros gastos volverá a mostrarse como \"General\"):"
            AppLanguage.EN -> "Enter a custom name for this button on this expense only (it will reset to \"General\" for future expenses):"
            AppLanguage.FR -> "Entrez un nom personnalisé pour ce bouton uniquement pour cette dépense (il sera réinitialisé à « Général » pour les prochaines dépenses) :"
            AppLanguage.IT -> "Inserisci un nome personalizzato per questo pulsante solo per questa spesa (tornerà a \"Generale\" per le spese future):"
            AppLanguage.ZH -> "仅为此项支出输入此按钮的自定义名称（后续支出将重置为“常规”）："
            AppLanguage.NL -> "Voer alleen voor deze uitgave een aangepaste naam in voor deze knop (wordt bij volgende uitgaven weer 'Algemeen'):"
            AppLanguage.DE -> "Geben Sie nur für diese Ausgabe einen benutzerdefinierten Namen ein (wird bei künftigen Ausgaben wieder auf „Allgemein“ zurückgesetzt):"
            AppLanguage.PL -> "Wpisz własną nazwę tego przycisku tylko dla tego wydatku (dla kolejnych wydatków powróci do „Ogólne”):"
            AppLanguage.PT -> "Escreva um nome para este botão nesta despesa (para futuras despesas voltará a mostrar-se como \"Geral\"):"
        }
        val renameFieldLabel = when (currentLang) {
            AppLanguage.ES -> "Nombre (ej. Souvenirs, Visado, Propina...)"
            AppLanguage.EN -> "Name (e.g. Souvenirs, Visa, Tip...)"
            AppLanguage.FR -> "Nom (ex. Souvenirs, Visa, Pourboire...)"
            AppLanguage.IT -> "Nome (es. Souvenir, Visto, Mancia...)"
            AppLanguage.ZH -> "名称（如：纪念品、签证、小费...）"
            AppLanguage.NL -> "Naam (bijv. Souvenirs, Visum, Fooi...)"
            AppLanguage.DE -> "Name (z.B. Souvenirs, Visum, Trinkgeld...)"
            AppLanguage.PL -> "Nazwa (np. Pamiątki, Wiza, Napiwek...)"
            AppLanguage.PT -> "Nome (ex. Lembranças, Visto, Gorjeta...)"
        }
        val applyBtnLabel = when (currentLang) {
            AppLanguage.ES -> "Aplicar a este gasto"
            AppLanguage.EN -> "Apply to this expense"
            AppLanguage.FR -> "Appliquer à cette dépense"
            AppLanguage.IT -> "Applica a questa spesa"
            AppLanguage.ZH -> "应用到此费用"
            AppLanguage.NL -> "Toepassen op deze uitgave"
            AppLanguage.DE -> "Auf diese Ausgabe anwenden"
            AppLanguage.PL -> "Zastosuj do tego wydatku"
            AppLanguage.PT -> "Aplicar a esta despesa"
        }
        val keepGeneralLabel = when (currentLang) {
            AppLanguage.ES -> "Dejar en General"
            AppLanguage.EN -> "Keep as General"
            AppLanguage.FR -> "Garder Général"
            AppLanguage.IT -> "Mantieni Generale"
            AppLanguage.ZH -> "保留为常规"
            AppLanguage.NL -> "Als Algemeen behouden"
            AppLanguage.DE -> "Als Allgemein belassen"
            AppLanguage.PL -> "Pozostaw jako Ogólne"
            AppLanguage.PT -> "Manter como Geral"
        }
        val applyCustomGeneralName = {
            val customName = tempGeneralNameInput.trim().ifEmpty { "General" }
            customGeneralButtonLabel = customName
            selectedCategory = customName
            showRenameGeneralDialog = false
        }
        AlertDialog(
            onDismissRequest = { showRenameGeneralDialog = false },
            containerColor = Color.White,
            titleContentColor = OceanNavy,
            textContentColor = Color(0xFF334155),
            title = {
                Text(
                    text = renameDialogTitle,
                    fontWeight = FontWeight.ExtraBold,
                    color = OceanNavy
                )
            },
            text = {
                val renameFocusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current
                val dialogWindowInfo = LocalWindowInfo.current
                val dialogView = LocalView.current
                var hasRequestedKeyboard by remember { mutableStateOf(false) }

                LaunchedEffect(dialogWindowInfo.isWindowFocused) {
                    if (dialogWindowInfo.isWindowFocused && !hasRequestedKeyboard) {
                        hasRequestedKeyboard = true
                        delay(80)
                        try {
                            renameFocusRequester.requestFocus()
                            keyboardController?.show()
                            val imm = dialogView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                            imm?.showSoftInput(dialogView, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                        } catch (_: Exception) {}
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = renameDialogDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    OutlinedTextField(
                        value = tempGeneralNameInput,
                        onValueChange = { tempGeneralNameInput = it },
                        placeholder = {
                            Text(
                                text = renameFieldLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = TravelBlue,
                            unfocusedBorderColor = Color(0xFF94A3B8),
                            focusedTextColor = OceanNavy,
                            unfocusedTextColor = OceanNavy,
                            cursorColor = TravelBlue,
                            focusedPlaceholderColor = Color(0xFF94A3B8),
                            unfocusedPlaceholderColor = Color(0xFF94A3B8)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { applyCustomGeneralName() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(renameFocusRequester)
                            .testTag("rename_general_category_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { applyCustomGeneralName() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TravelBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(applyBtnLabel, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        customGeneralButtonLabel = "General"
                        selectedCategory = "General"
                        showRenameGeneralDialog = false
                    },
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OceanNavy)
                ) {
                    Text(keepGeneralLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Modal de previsualización a gran tamaño para el recibo o foto seleccionado
    if (previewReceiptUri != null) {
        val previewTitle = when (currentLang) {
            AppLanguage.ES -> "Recibo / Recuerdo"
            AppLanguage.EN -> "Receipt / Memory"
            AppLanguage.FR -> "Reçu / Souvenir"
            AppLanguage.IT -> "Ricevuta / Ricordo"
            AppLanguage.ZH -> "收据 / 回忆"
            AppLanguage.NL -> "Bon / Herinnering"
            AppLanguage.DE -> "Beleg / Erinnerung"
            AppLanguage.PL -> "Rachunek / Wspomnienie"
            AppLanguage.PT -> "Recibo / Lembrança"
        }
        Dialog(onDismissRequest = { previewReceiptUri = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 20.dp, bottom = 64.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = previewTitle,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { previewReceiptUri = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = texts.closeBtn,
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = previewReceiptUri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}
