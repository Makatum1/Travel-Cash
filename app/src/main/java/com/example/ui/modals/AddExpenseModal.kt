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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
    PERCENTAGE("Porcentajes (%)", "Percentages (%)")
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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEn = LocalAppLanguage.current == AppLanguage.EN
    val isEdit = existingExpense != null

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
            userLon = userLocation?.second
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
            userLon = userLocation?.second
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
                .padding(horizontal = 10.dp)
                .padding(top = 16.dp, bottom = 16.dp)
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
                        text = if (isEdit) {
                            if (isEn) "Edit Expense" else "Editar Gasto"
                        } else {
                            if (isEn) "New Trip Expense" else "Nuevo Gasto del Viaje"
                        },
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. IMPORTE Y MULTIDIVISA
                    Text(
                        text = if (isEn) "1. Amount & Currency" else "1. Importe y Moneda",
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
                                Text(
                                    if (isEn) {
                                        "Amount (${CurrencyCatalog.getSymbol(selectedCurrency)})"
                                    } else {
                                        "Importe (${CurrencyCatalog.getSymbol(selectedCurrency)})"
                                    }
                                )
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
                                            Text("${curr.code} - ${CurrencyCatalog.getDisplayName(curr, isEn)} (${curr.symbol})")
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
                                OutlinedTextField(
                                    value = exchangeRateText,
                                    onValueChange = { exchangeRateText = it },
                                    label = {
                                        Text(
                                            if (isEn) {
                                                "Rate (1 $selectedCurrency = X ${trip.baseCurrency})"
                                            } else {
                                                "Tasa (1 $selectedCurrency = X ${trip.baseCurrency})"
                                            }
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isEn) "In base currency:" else "En moneda base:",
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
                    Text(
                        text = if (isRouteExpense) {
                            if (isEn) "2. Origin & Destination" else "2. Origen y Destino"
                        } else {
                            if (isEn) "2. Place / Venue" else "2. Lugar / Establecimiento"
                        },
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
                                label = {
                                    Text(
                                        if (isEn) {
                                            "Place / Venue"
                                        } else {
                                            "Lugar / Establecimiento"
                                        }
                                    )
                                },
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
                                        contentDescription = if (isEn) "Get GPS" else "Obtener GPS",
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
                            Text(
                                text = if (isEn) {
                                    "📍 GPS active — Sorting closest places first"
                                } else {
                                    "📍 GPS activo — Ordenando lugares más cercanos primero"
                                },
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
                        OutlinedTextField(
                            value = addressText,
                            onValueChange = { addressText = it },
                            label = { Text(if (isEn) "Address" else "Dirección") },
                            placeholder = {
                                Text(
                                    if (isEn) {
                                        "Street, city or address of the place..."
                                    } else {
                                        "Calle, ciudad o dirección del lugar..."
                                    }
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_address_input")
                        )
                    } else {
                        // En Transporte, Taxi y Vuelos: mostrar Origen y Destino con buscador de calles, lugares, ciudades, etc.
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
                                    label = {
                                        Text(if (isEn) "Origin" else "Origen")
                                    },
                                    placeholder = {
                                        Text(
                                            if (isEn) {
                                                "Search street, place, city, station or airport..."
                                            } else {
                                                "Buscar calle, lugar, ciudad, estación o aeropuerto..."
                                            }
                                        )
                                    },
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
                                    label = {
                                        Text(if (isEn) "Destination" else "Destino")
                                    },
                                    placeholder = {
                                        Text(
                                            if (isEn) {
                                                "Search street, place, city, station or airport..."
                                            } else {
                                                "Buscar calle, lugar, ciudad, estación o aeropuerto..."
                                            }
                                        )
                                    },
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
                    Text(
                        text = if (isEn) "3. Category" else "3. Categoría",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryCatalog.categories.forEach { cat ->
                            val isGeneralButton = cat.name.equals("General", ignoreCase = true)
                            val displayLabel = if (isGeneralButton) {
                                if (customGeneralButtonLabel.equals("General", ignoreCase = true)) {
                                    CategoryCatalog.displayName("General", isEn)
                                } else {
                                    customGeneralButtonLabel // Nombre personalizado puesto por el usuario: no se traduce
                                }
                            } else {
                                CategoryCatalog.displayName(cat.name, isEn)
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
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) cat.color else cat.color.copy(alpha = 0.12f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) cat.color else cat.color.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else cat.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = displayLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
                                        text = if (isEn) "Expense date" else "Fecha del gasto",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = FormatUtils.formatShortDate(selectedDate),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = OceanNavy
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = prorationDaysText,
                            onValueChange = { prorationDaysText = it.filter { ch -> ch.isDigit() } },
                            label = { Text(if (isEn) "Prorate (N days)" else "Prorratear (N días)") },
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
                            Text(
                                text = if (isEn) {
                                    "🗓️ Expense prorated over $pDays days → ${FormatUtils.formatCurrency(dailyCost, trip.baseCurrency)} per day"
                                } else {
                                    "🗓️ Gasto prorrateado en $pDays días → Supone ${FormatUtils.formatCurrency(dailyCost, trip.baseCurrency)} por día"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF6D28D9),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(if (isEn) "Notes / Additional description" else "Notas / Descripción adicional") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 4. PARTICIPANTES
                    Text(
                        text = if (isEn) "4. Participants" else "4. Participantes",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy
                    )
                    Text(
                        text = if (isEn) {
                            "Select who pays for this expense (Common Pot or a participant):"
                        } else {
                            "Selecciona quién paga este gasto (el Bote común o un participante):"
                        },
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
                                text = if (isEn) "🐷 Common Pot" else "🐷 Bote Común",
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
                                    text = "$methodPrefix${PaymentMethodCatalog.displayName(method, isEn)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // 5. DIVISIÓN DEL GASTO
                    Text(
                        text = if (isEn) "5. Expense Split" else "5. División del Gasto",
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
                                    text = if (isEn) mode.labelEn else mode.labelEs,
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
                                    Text(
                                        text = if (isEn) {
                                            "Assigned sum: ${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)} / Total: ${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)}"
                                        } else {
                                            "Suma asignada: ${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)} / Total: ${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)}"
                                        },
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
                                    Text(
                                        text = if (isEn) {
                                            "Percentage sum: ${String.format(Locale.US, "%.1f", sumPct)}% (must equal 100%)"
                                        } else {
                                            "Suma de porcentajes: ${String.format(Locale.US, "%.1f", sumPct)}% (debe sumar 100%)"
                                        },
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEn) "6. Receipts & Memories" else "6. Recibos y Recuerdos",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = OceanNavy
                        )
                        if (photos.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, TravelBlue.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${photos.size} ${if (photos.size == 1) (if (isEn) "receipt" else "recibo") else (if (isEn) "recibos" else "recibos")}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TravelBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (isEn) {
                            "Add as many tickets, invoices, receipts or photos as you need without limits."
                        } else {
                            "Añade todos los tickets, facturas, recibos o recuerdos que necesites sin límite."
                        },
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
                            Text(if (isEn) "Gallery" else "Galería")
                        }
                        OutlinedButton(
                            onClick = { cameraPreviewLauncher.launch(null) }
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Camera" else "Cámara")
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
                                    AsyncImage(
                                        model = photoUri,
                                        contentDescription = if (isEn) "Receipt or memory ${idx + 1}" else "Recibo o recuerdo ${idx + 1}",
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
                                            contentDescription = if (isEn) "Remove" else "Eliminar",
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
                    if (isEdit && existingExpense != null) {
                        OutlinedButton(
                            onClick = {
                                onDeleteExpense(existingExpense.id)
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                            border = BorderStroke(1.dp, CoralRed),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEn) "Delete" else "Borrar", maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(if (isEn) "Cancel" else "Cancelar", maxLines = 1)
                    }

                    Button(
                        onClick = {
                            if (numericAmount <= 0.0) {
                                validationError = if (isEn) "Enter an amount greater than 0." else "Introduce un importe mayor que 0."
                                return@Button
                            }
                            val expId = existingExpense?.id ?: "exp-${UUID.randomUUID().toString().take(8)}"

                            val finalSplits: List<ExpenseSplit> = when (splitMode) {
                                SplitMode.EQUAL -> {
                                    val selectedParts = participants.filter { includedParticipantIds[it.id] == true }
                                    if (selectedParts.isEmpty()) {
                                        validationError = if (isEn) {
                                            "Select at least one participant to split the expense."
                                        } else {
                                            "Selecciona al menos un participante para dividir el gasto."
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
                                        validationError = if (isEn) {
                                            "The sum of exact amounts (${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)}) must match the total (${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)})."
                                        } else {
                                            "La suma de importes exactos (${FormatUtils.formatCurrency(sumExact, trip.baseCurrency)}) debe coincidir con el total (${FormatUtils.formatCurrency(amountInBase, trip.baseCurrency)})."
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
                                        validationError = if (isEn) {
                                            "Percentages must add up to 100% (currently ${String.format(Locale.US, "%.1f", sumPct)}%)."
                                        } else {
                                            "Los porcentajes deben sumar 100% (actualmente suman ${String.format(Locale.US, "%.1f", sumPct)}%)."
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
                            .weight(if (isEdit && existingExpense != null) 1.35f else 1.5f)
                            .testTag("save_expense_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEn) "Save" else "Guardar",
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
        AlertDialog(
            onDismissRequest = { showRenameGeneralDialog = false },
            title = {
                Text(
                    text = if (isEn) "Customize General Button" else "Personalizar botón General",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isEn) {
                            "Enter a custom name for this button on this expense only (it will reset to \"General\" for future expenses):"
                        } else {
                            "Escribe un nombre para este botón en este gasto (para futuros gastos volverá a mostrarse como \"General\"):"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    OutlinedTextField(
                        value = tempGeneralNameInput,
                        onValueChange = { tempGeneralNameInput = it },
                        label = {
                            Text(
                                if (isEn) "Name (e.g. Souvenirs, Visa, Tip...)" else "Nombre (ej. Souvenirs, Visado, Propina...)"
                            )
                        },
                        placeholder = { Text("General") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val customName = tempGeneralNameInput.trim().ifEmpty { "General" }
                        customGeneralButtonLabel = customName
                        selectedCategory = customName
                        showRenameGeneralDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TravelBlue)
                ) {
                    Text(if (isEn) "Apply to this expense" else "Aplicar a este gasto", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        customGeneralButtonLabel = "General"
                        selectedCategory = "General"
                        showRenameGeneralDialog = false
                    }
                ) {
                    Text(if (isEn) "Keep as General" else "Dejar en General")
                }
            }
        )
    }

    // Modal de previsualización a gran tamaño para el recibo o foto seleccionado
    if (previewReceiptUri != null) {
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
                            text = if (isEn) "Receipt / Memory" else "Recibo / Recuerdo",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { previewReceiptUri = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isEn) "Close" else "Cerrar",
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
