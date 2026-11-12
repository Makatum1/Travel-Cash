package com.example

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplit
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.TripEntity
import com.example.domain.DebtCalculator
import com.example.util.FormatUtils
import com.example.ui.components.ActiveTripHeaderAndBudgetCard
import com.example.ui.components.EditableParticipantAvatar
import com.example.ui.components.EditableTripRouteIcon
import com.example.ui.components.TopFeedbackToast
import com.example.ui.components.TravelCashTopBar
import com.example.ui.components.TravelLocationPinIcon
import com.example.ui.modals.AddExpenseModal
import com.example.ui.modals.CreateTripModal
import com.example.ui.modals.CsvColumnMappingModal
import com.example.ui.modals.IncomeModal
import com.example.ui.modals.SuggestionsModal
import com.example.ui.screens.ExpensesTabScreen
import com.example.ui.screens.MapTabScreen
import com.example.ui.screens.SettingsTabScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SplitsTabScreen
import com.example.ui.screens.StatsTabScreen
import com.example.ui.screens.TripsTabScreen
import com.example.ui.theme.isEnglish
import com.example.ui.theme.CoralRed
import com.example.ui.theme.TravelBlue
import com.example.ui.theme.TravelBlueLight
import com.example.ui.theme.TravelCashTheme
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.TravelCashViewModel
import com.example.util.ExportImportManager
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: TravelCashViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
            val language by vm.language.collectAsStateWithLifecycle()

            TravelCashTheme(
                themeMode = themeMode,
                language = language
            ) {
                var isSplashActive by remember { mutableStateOf(true) }

                if (isSplashActive) {
                    SplashScreen(
                        onSplashFinished = { isSplashActive = false }
                    )
                } else {
                    TravelCashApp(viewModel = vm)
                }
            }
        }
    }
}

@Composable
fun TravelCashApp(
    viewModel: TravelCashViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val placeSuggestions by viewModel.placeSuggestions.collectAsStateWithLifecycle()
    val isSearchingPlaces by viewModel.isSearchingPlaces.collectAsStateWithLifecycle()
    val pendingCsvManualImport by viewModel.pendingCsvManualImport.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val reminderNotificationsEnabled by viewModel.reminderNotificationsEnabled.collectAsStateWithLifecycle()
    val isEn = language.isEnglish()

    fun sanitizeTripFileName(title: String, extension: String): String {
        val clean = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        val finalName = if (clean.isEmpty()) "viaje" else clean
        return "$finalName.$extension"
    }

    // Estados de modales
    var showAddOrEditExpenseModal by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    var showIncomeModal by remember { mutableStateOf(false) }
    var editingIncome by remember { mutableStateOf<IncomeEntity?>(null) }

    var showCreateTripModal by remember { mutableStateOf(false) }
    var editingTrip by remember { mutableStateOf<TripEntity?>(null) }
    var showSuggestionsModal by remember { mutableStateOf(false) }

    // Apertura automática del modal de primer viaje si la base de datos está limpia/vacía
    var hasAutoPromptedFirstTrip by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(uiState.isInitialized, uiState.trips.isEmpty()) {
        if (uiState.isInitialized && uiState.trips.isEmpty() && !hasAutoPromptedFirstTrip) {
            hasAutoPromptedFirstTrip = true
            editingTrip = null
            showCreateTripModal = true
        }
    }

    // 1. Estado para cambiar la foto del icono de una ruta/viaje (o dejar el de carretera por defecto)
    var tripForIconPhotoDialog by remember { mutableStateOf<TripEntity?>(null) }

    val tripPhotoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val targetTrip = tripForIconPhotoDialog ?: uiState.activeTrip
        if (uri != null && targetTrip != null) {
            try {
                val destFile = File(context.filesDir, "trip_icon_${targetTrip.id}_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.updateTrip(targetTrip.copy(headerPhotoUri = Uri.fromFile(destFile).toString()))
                tripForIconPhotoDialog = null
            } catch (e: Exception) {
                viewModel.showToast(
                    if (isEn) "Error saving route photo" else "Error al guardar la foto de la ruta",
                    isError = true
                )
            }
        }
    }

    val tripPhotoCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        val targetTrip = tripForIconPhotoDialog ?: uiState.activeTrip
        if (bitmap != null && targetTrip != null) {
            try {
                val destFile = File(context.filesDir, "trip_icon_${targetTrip.id}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                viewModel.updateTrip(targetTrip.copy(headerPhotoUri = Uri.fromFile(destFile).toString()))
                tripForIconPhotoDialog = null
            } catch (e: Exception) {
                viewModel.showToast(
                    if (isEn) "Error saving camera photo" else "Error al guardar la foto de la cámara",
                    isError = true
                )
            }
        }
    }

    // 2. Estado para añadir/cambiar la foto de un participante (o dejar inicial + color por defecto)
    var participantForPhotoDialog by remember { mutableStateOf<ParticipantEntity?>(null) }

    val participantPhotoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val targetParticipant = participantForPhotoDialog
        if (uri != null && targetParticipant != null) {
            try {
                val destFile = File(context.filesDir, "part_avatar_${targetParticipant.id}_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.updateParticipantPhoto(targetParticipant, Uri.fromFile(destFile).toString())
                participantForPhotoDialog = null
            } catch (e: Exception) {
                viewModel.showToast(
                    if (isEn) "Error saving participant photo" else "Error al guardar la foto del participante",
                    isError = true
                )
            }
        }
    }

    val participantPhotoCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        val targetParticipant = participantForPhotoDialog
        if (bitmap != null && targetParticipant != null) {
            try {
                val destFile = File(context.filesDir, "part_avatar_${targetParticipant.id}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                viewModel.updateParticipantPhoto(targetParticipant, Uri.fromFile(destFile).toString())
                participantForPhotoDialog = null
            } catch (e: Exception) {
                viewModel.showToast(
                    if (isEn) "Error saving camera photo" else "Error al guardar la foto de la cámara",
                    isError = true
                )
            }
        }
    }

    // Estado auxiliar para exportaciones / importaciones con SAF
    var tripForPendingExport by remember { mutableStateOf<TripEntity?>(null) }
    var csvImportCreatesNewTrip by remember { mutableStateOf(false) }

    // Launcher para exportar CSV de un viaje
    val exportTripCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val targetTrip = tripForPendingExport ?: uiState.activeTrip
        if (uri != null && targetTrip != null) {
            try {
                val parts = uiState.allParticipants.filter { it.tripId == targetTrip.id }
                val exps = uiState.allExpenses.filter { it.tripId == targetTrip.id }
                val csvStr = ExportImportManager.generateTripExpensesCsv(targetTrip, parts, exps)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csvStr.toByteArray(Charsets.UTF_8))
                }
                viewModel.showToast(
                    if (isEn) "CSV file for \"${targetTrip.title}\" exported" else "Archivo CSV de \"${targetTrip.title}\" exportado"
                )
            } catch (e: Exception) {
                viewModel.showToast(
                    if (isEn) "Error exporting CSV" else "Error al exportar CSV",
                    isError = true
                )
            }
        }
    }

    // Launcher para exportar Reporte PDF multipágina
    val exportTripPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        val targetTrip = tripForPendingExport ?: uiState.activeTrip
        if (uri != null && targetTrip != null) {
            try {
                val parts = uiState.allParticipants.filter { it.tripId == targetTrip.id }
                val exps = uiState.allExpenses.filter { it.tripId == targetTrip.id }
                val incs = uiState.allIncomes.filter { it.tripId == targetTrip.id }
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    ExportImportManager.writeTripPdfReport(out, targetTrip, parts, exps, incs)
                }
                viewModel.showToast(
                    if (isEn) "PDF report for \"${targetTrip.title}\" generated" else "Reporte PDF de \"${targetTrip.title}\" generado"
                )
            } catch (e: Exception) {
                viewModel.showToast(
                    if (isEn) "Error generating PDF report" else "Error al generar el reporte PDF",
                    isError = true
                )
            }
        }
    }

    // Launcher para importar archivo CSV
    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
                } ?: ""
                viewModel.processCsvImportContent(
                    csvContent = content,
                    createNewTrip = csvImportCreatesNewTrip,
                    suggestedFileName = if (isEn) "Imported CSV Trip" else "Viaje Importado CSV"
                )
            } catch (e: Exception) {
                viewModel.showToast(
                    if (isEn) "Could not read selected CSV file" else "No se pudo leer el archivo CSV seleccionado",
                    isError = true
                )
            }
        }
    }

    var tripIdForPendingZipExport by rememberSaveable { mutableStateOf<String?>(null) }
    var isExportingAllTripsZip by rememberSaveable { mutableStateOf(false) }
    var tripIdForPendingJsonExport by rememberSaveable { mutableStateOf<String?>(null) }

    // Launcher para exportar archivo ZIP (todos los viajes o un viaje individual)
    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            val targetId = if (isExportingAllTripsZip) null else tripIdForPendingZipExport
            viewModel.exportTripsToZipUri(uri, targetTripId = targetId)
        }
    }

    // Launcher para importar archivo ZIP o JSON de viaje
    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importTripsFromUri(uri)
        }
    }

    // Launcher para exportar un viaje individual como JSON
    val exportSingleTripJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val targetId = tripIdForPendingJsonExport ?: tripForPendingExport?.id ?: uiState.activeTrip?.id
        if (uri != null && targetId != null) {
            viewModel.exportSingleTripJsonToUri(uri, targetTripId = targetId)
        }
    }

    // BackHandler en pestañas secundarias para volver a la pestaña principal Gastos
    if (selectedTab != MainTab.EXPENSES) {
        BackHandler {
            viewModel.selectTab(MainTab.EXPENSES)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TravelCashTopBar(
                trips = uiState.trips,
                activeTrip = uiState.activeTrip,
                onSelectTrip = { viewModel.selectActiveTrip(it) },
                onCreateNewTripClick = {
                    editingTrip = null
                    showCreateTripModal = true
                },
                onImportNewTripCsvClick = {
                    csvImportCreatesNewTrip = true
                    importCsvLauncher.launch(arrayOf("text/*", "application/csv", "*/*"))
                },
                onSuggestionsClick = { showSuggestionsModal = true }
            )
        },
        floatingActionButton = {
            if (selectedTab == MainTab.EXPENSES && uiState.activeTrip != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingExpense = null
                        showAddOrEditExpenseModal = true
                    },
                    containerColor = TravelBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("fab_add_expense")
                ) {
                    Icon(Icons.Default.Add, contentDescription = if (isEn) "Add Expense" else "Añadir Gasto")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "Add Expense" else "Añadir Gasto",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Barra de navegación: Gastos, Cuentas, Estadística, Mapa, Viajes, Ajustes
                val navItems = listOf(
                    MainTab.EXPENSES to Icons.Default.ReceiptLong,
                    MainTab.SPLITS to Icons.Default.Group,
                    MainTab.STATS to Icons.Default.BarChart,
                    MainTab.MAP to Icons.Default.Map,
                    MainTab.TRIPS to Icons.Default.FlightTakeoff,
                    MainTab.SETTINGS to Icons.Default.Settings
                )

                navItems.forEach { (tab, icon) ->
                    val selected = selectedTab == tab
                    val label = if (isEn) tab.titleEn else tab.titleEs
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            viewModel.selectTab(tab)
                        },
                        icon = {
                            if (tab == MainTab.MAP) {
                                TravelLocationPinIcon(
                                    tint = if (selected) TravelBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                    accentColor = if (selected) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 9.5.sp
                                ),
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TravelBlue,
                            selectedTextColor = TravelBlue,
                            indicatorColor = TravelBlueLight,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("bottom_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val activeTrip = uiState.activeTrip
            val activeTripHeaderContent: @Composable () -> Unit = {
                if (activeTrip != null) {
                    ActiveTripHeaderAndBudgetCard(
                        trip = activeTrip,
                        totalSpent = uiState.totalSpent,
                        totalIncome = uiState.totalIncome,
                        extraBalance = uiState.extraBalance,
                        onTripIconClick = {
                            tripForIconPhotoDialog = activeTrip
                        },
                        onExportCsvClick = {
                            tripForPendingExport = activeTrip
                            exportTripCsvLauncher.launch(sanitizeTripFileName(activeTrip.title, "csv"))
                        },
                        onExportPdfClick = {
                            tripForPendingExport = activeTrip
                            exportTripPdfLauncher.launch(sanitizeTripFileName(activeTrip.title, "pdf"))
                        },
                        onImportCsvToTripClick = {
                            csvImportCreatesNewTrip = false
                            importCsvLauncher.launch(arrayOf("text/*", "application/csv", "*/*"))
                        },
                        onAddBoteIncomeClick = {
                            editingIncome = null
                            showIncomeModal = true
                        },
                        isSimplified = true
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val currentTab = selectedTab
                if (currentTab == MainTab.SETTINGS) {
                    SettingsTabScreen(
                        themeMode = themeMode,
                        language = language,
                        reminderNotificationsEnabled = reminderNotificationsEnabled,
                        trips = uiState.trips,
                        onThemeModeChange = viewModel::setThemeMode,
                        onLanguageChange = viewModel::setLanguage,
                        onReminderNotificationsChange = viewModel::setReminderNotificationsEnabled,
                        onExportZipClick = { trip ->
                            if (trip == null) {
                                if (uiState.trips.isEmpty()) {
                                    viewModel.showToast(
                                        if (isEn) "No trips to export" else "No hay viajes para exportar",
                                        isError = true
                                    )
                                } else {
                                    isExportingAllTripsZip = true
                                    tripIdForPendingZipExport = null
                                    exportZipLauncher.launch("travel_cash_todos_los_viajes.zip")
                                }
                            } else {
                                isExportingAllTripsZip = false
                                tripIdForPendingZipExport = trip.id
                                val cleanTitle = trip.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
                                val safeName = if (cleanTitle.isEmpty()) "viaje" else cleanTitle
                                exportZipLauncher.launch("viaje_${safeName}.zip")
                            }
                        },
                        onImportZipClick = {
                            importZipLauncher.launch(arrayOf("*/*"))
                        }
                    )
                } else if (activeTrip != null) {
                    when (currentTab) {
                        MainTab.EXPENSES -> {
                            ExpensesTabScreen(
                                trip = activeTrip,
                                participants = uiState.activeParticipants,
                                expenses = uiState.activeExpenses,
                                searchQuery = searchQuery,
                                selectedDateFilter = selectedDateFilter,
                                selectedCategoryFilter = selectedCategoryFilter,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onDateFilterChange = viewModel::updateDateFilter,
                                onCategoryFilterChange = viewModel::updateCategoryFilter,
                                onClearAllFilters = viewModel::clearExpenseFilters,
                                onAddExpenseClick = {
                                    editingExpense = null
                                    showAddOrEditExpenseModal = true
                                },
                                onExpenseClick = { exp ->
                                    editingExpense = exp
                                    showAddOrEditExpenseModal = true
                                },
                                topHeader = activeTripHeaderContent
                            )
                        }

                        MainTab.SPLITS -> {
                            SplitsTabScreen(
                                trip = activeTrip,
                                participants = uiState.activeParticipants,
                                expenses = uiState.activeExpenses,
                                incomes = uiState.activeIncomes,
                                debtResult = uiState.debtResult,
                                onAddParticipant = viewModel::addParticipant,
                                onUpdateParticipantName = viewModel::updateParticipantName,
                                onParticipantAvatarClick = { participant ->
                                    participantForPhotoDialog = participant
                                },
                                onDeleteParticipant = viewModel::deleteParticipant,
                                onAddIncomeClick = {
                                    editingIncome = null
                                    showIncomeModal = true
                                },
                                onEditIncomeClick = { inc ->
                                    editingIncome = inc
                                    showIncomeModal = true
                                },
                                onDeleteIncomeClick = viewModel::deleteIncome,
                                onSettleDebt = { tx, settledAmount, paymentMethod ->
                                    val expenseId = "settle-${java.util.UUID.randomUUID().toString().take(8)}"
                                    val fromName = if (tx.isFromBote) (if (isEn) "Common Pot" else "Bote Común") else tx.fromName
                                    val place = if (tx.isFromBote) {
                                        if (isEn) "Settlement from Common Pot to ${tx.toName}" else "Liquidación desde Bote Común a ${tx.toName}"
                                    } else {
                                        if (isEn) "Debt settlement: ${tx.fromName} → ${tx.toName}" else "Liquidación de deuda: ${tx.fromName} → ${tx.toName}"
                                    }
                                    val payerId = if (tx.isFromBote) DebtCalculator.BOTE_COMUN_ID else tx.fromParticipantId
                                    val method = if (tx.isFromBote) "Bote Común" else paymentMethod
                                    val expense = ExpenseEntity(
                                        id = expenseId,
                                        tripId = activeTrip.id,
                                        amount = settledAmount,
                                        currency = activeTrip.baseCurrency,
                                        amountInBaseCurrency = settledAmount,
                                        exchangeRate = 1.0,
                                        category = "General",
                                        date = java.time.LocalDate.now().toString(),
                                        paymentMethod = method,
                                        notes = if (isEn) "Settlement: $fromName pays ${tx.toName}" else "Saldado de cuenta ($fromName paga a ${tx.toName})",
                                        latitude = null,
                                        longitude = null,
                                        placeName = place,
                                        country = "",
                                        originName = "",
                                        originLatitude = null,
                                        originLongitude = null,
                                        paidById = payerId,
                                        splits = listOf(
                                            ExpenseSplit(
                                                id = "split-${java.util.UUID.randomUUID().toString().take(8)}",
                                                expenseId = expenseId,
                                                participantId = tx.toParticipantId,
                                                amount = settledAmount,
                                                percentage = 100.0
                                            )
                                        ),
                                        createdAt = System.currentTimeMillis()
                                    )
                                    viewModel.saveExpense(expense, isEdit = false)
                                    viewModel.showToast(
                                        if (isEn) "Debt settled: ${FormatUtils.formatCurrency(settledAmount, activeTrip.baseCurrency)}"
                                        else "Deuda saldada: ${FormatUtils.formatCurrency(settledAmount, activeTrip.baseCurrency)}"
                                    )
                                },
                                topHeader = activeTripHeaderContent
                            )
                        }

                        MainTab.STATS -> {
                            StatsTabScreen(
                                trip = activeTrip,
                                participants = uiState.activeParticipants,
                                expenses = uiState.activeExpenses,
                                onParticipantAvatarClick = { participant ->
                                    participantForPhotoDialog = participant
                                },
                                onExpenseClick = { exp ->
                                    editingExpense = exp
                                    showAddOrEditExpenseModal = true
                                },
                                topHeader = activeTripHeaderContent
                            )
                        }

                        MainTab.MAP -> {
                            MapTabScreen(
                                trip = activeTrip,
                                participants = uiState.activeParticipants,
                                expenses = uiState.activeExpenses,
                                userLocation = userLocation,
                                onRequestLocationRefresh = viewModel::refreshDeviceLocation,
                                onEditExpenseClick = { exp ->
                                    editingExpense = exp
                                    showAddOrEditExpenseModal = true
                                }
                            )
                        }

                        MainTab.TRIPS -> {
                            TripsTabScreen(
                                trips = uiState.trips,
                                activeTripId = uiState.activeTripId,
                                allParticipants = uiState.allParticipants,
                                allExpenses = uiState.allExpenses,
                                allIncomes = uiState.allIncomes,
                                onSelectTrip = viewModel::selectActiveTrip,
                                onTripIconClick = { trip ->
                                    tripForIconPhotoDialog = trip
                                },
                                onCreateNewTripClick = {
                                    editingTrip = null
                                    showCreateTripModal = true
                                },
                                onAddSampleTripClick = viewModel::createSampleTrip,
                                onImportZipClick = {
                                    importZipLauncher.launch(arrayOf("*/*"))
                                },
                                onEditTripClick = { trip ->
                                    editingTrip = trip
                                    showCreateTripModal = true
                                },
                                onExportSingleTripJsonClick = { trip ->
                                    tripForPendingExport = trip
                                    tripIdForPendingJsonExport = trip.id
                                    val clean = trip.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
                                    val safeName = if (clean.isEmpty()) "Viaje" else clean
                                    exportSingleTripJsonLauncher.launch("Viaje_${safeName}.json")
                                },
                                onExportSingleTripPdfClick = { trip ->
                                    tripForPendingExport = trip
                                    exportTripPdfLauncher.launch(sanitizeTripFileName(trip.title, "pdf"))
                                },
                                onDeleteTripConfirmed = viewModel::deleteTrip,
                                topHeader = activeTripHeaderContent
                            )
                        }

                        MainTab.SETTINGS -> {}
                    }
                } else {
                    TripsTabScreen(
                        trips = uiState.trips,
                        activeTripId = null,
                        allParticipants = uiState.allParticipants,
                        allExpenses = uiState.allExpenses,
                        allIncomes = uiState.allIncomes,
                        onSelectTrip = viewModel::selectActiveTrip,
                        onTripIconClick = { trip ->
                            tripForIconPhotoDialog = trip
                        },
                        onCreateNewTripClick = {
                            editingTrip = null
                            showCreateTripModal = true
                        },
                        onAddSampleTripClick = viewModel::createSampleTrip,
                        onImportZipClick = {
                            importZipLauncher.launch(arrayOf("*/*"))
                        },
                        onEditTripClick = { trip ->
                            editingTrip = trip
                            showCreateTripModal = true
                        },
                        onExportSingleTripJsonClick = { trip ->
                            tripForPendingExport = trip
                            tripIdForPendingJsonExport = trip.id
                            val clean = trip.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
                            val safeName = if (clean.isEmpty()) "Viaje" else clean
                            exportSingleTripJsonLauncher.launch("Viaje_${safeName}.json")
                        },
                        onExportSingleTripPdfClick = { trip ->
                            tripForPendingExport = trip
                            exportTripPdfLauncher.launch(sanitizeTripFileName(trip.title, "pdf"))
                        },
                        onDeleteTripConfirmed = viewModel::deleteTrip
                    )
                }
            }

            // Notificación superior flotante (Feedback visual)
            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                TopFeedbackToast(
                    message = toastMessage?.text,
                    isError = toastMessage?.isError == true,
                    onDismiss = viewModel::dismissToast
                )
            }
        }
    }

    // Diálogo para elegir foto personalizada al pinchar el icono de una ruta (o dejar el de carretera por defecto)
    if (tripForIconPhotoDialog != null) {
        val targetTrip = tripForIconPhotoDialog!!
        AlertDialog(
            onDismissRequest = { tripForIconPhotoDialog = null },
            title = {
                Text(
                    text = if (isEn) "Route icon for \"${targetTrip.title}\"" else "Icono de la ruta \"${targetTrip.title}\"",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EditableTripRouteIcon(
                        photoUri = targetTrip.headerPhotoUri,
                        onClick = {},
                        size = 72.dp
                    )
                    Text(
                        text = if (isEn) {
                            "You can set a custom photo for this route or keep the default road icon:"
                        } else {
                            "Puedes poner una foto personalizada para esta ruta o dejar el icono de la carretera por defecto:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                tripPhotoGalleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TravelBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Gallery" else "Galería")
                        }
                        OutlinedButton(
                            onClick = {
                                tripPhotoCameraLauncher.launch(null)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Camera" else "Cámara")
                        }
                    }
                    if (!targetTrip.headerPhotoUri.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateTrip(targetTrip.copy(headerPhotoUri = null))
                                tripForIconPhotoDialog = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Keep default road icon" else "Dejar icono de carretera por defecto")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { tripForIconPhotoDialog = null }) {
                    Text(if (isEn) "Close" else "Cerrar", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Diálogo para añadir foto al icono de un participante (o dejar inicial + color aleatorio por defecto)
    if (participantForPhotoDialog != null) {
        val targetPart = participantForPhotoDialog!!
        AlertDialog(
            onDismissRequest = { participantForPhotoDialog = null },
            title = {
                Text(
                    text = if (isEn) "Icon for ${targetPart.name}" else "Icono de ${targetPart.name}",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EditableParticipantAvatar(
                        name = targetPart.name,
                        avatarColorHex = targetPart.avatarColor,
                        photoUri = targetPart.photoUri,
                        onClick = {},
                        size = 72.dp
                    )
                    Text(
                        text = if (isEn) {
                            "Choose a photo for ${targetPart.name}. If none is set, their initial and color are shown by default:"
                        } else {
                            "Elige una foto para ${targetPart.name}. Si no pones ninguna, se mostrará por defecto su inicial con su color:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                participantPhotoGalleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TravelBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Gallery" else "Galería")
                        }
                        OutlinedButton(
                            onClick = {
                                participantPhotoCameraLauncher.launch(null)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Camera" else "Cámara")
                        }
                    }
                    if (!targetPart.photoUri.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateParticipantPhoto(targetPart, null)
                                participantForPhotoDialog = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Keep default (Initial + Color)" else "Dejar por defecto (Inicial + Color)")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { participantForPhotoDialog = null }) {
                    Text(if (isEn) "Close" else "Cerrar", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // =========================================================================
    // MODALES EMERGENTES
    // =========================================================================
    val activeTrip = uiState.activeTrip

    if (showAddOrEditExpenseModal && activeTrip != null) {
        AddExpenseModal(
            trip = activeTrip,
            participants = uiState.activeParticipants,
            existingExpense = editingExpense,
            userLocation = userLocation,
            placeSuggestions = placeSuggestions,
            isSearchingPlaces = isSearchingPlaces,
            onRequestLocationRefresh = viewModel::refreshDeviceLocation,
            onPlaceQueryChanged = viewModel::onPlaceQueryChanged,
            onClearPlaceSuggestions = viewModel::clearPlaceSuggestions,
            onSaveExpense = viewModel::saveExpense,
            onDeleteExpense = viewModel::deleteExpense,
            onDismiss = {
                showAddOrEditExpenseModal = false
                editingExpense = null
            }
        )
    }

    if (showIncomeModal && activeTrip != null) {
        IncomeModal(
            trip = activeTrip,
            participants = uiState.activeParticipants,
            expenses = uiState.activeExpenses,
            incomes = uiState.activeIncomes,
            existingIncome = editingIncome,
            onSaveIncome = viewModel::saveIncome,
            onDismiss = {
                showIncomeModal = false
                editingIncome = null
            }
        )
    }

    if (showCreateTripModal) {
        val targetTripToEdit = editingTrip
        val targetParticipants = if (targetTripToEdit != null) {
            uiState.allParticipants.filter { it.tripId == targetTripToEdit.id }
        } else {
            emptyList()
        }
        CreateTripModal(
            existingTrip = targetTripToEdit,
            existingParticipants = targetParticipants,
            isFirstTrip = uiState.trips.isEmpty() && targetTripToEdit == null,
            onCreateTrip = { title, startDate, endDate, budget, baseCurrency, participantNames ->
                if (targetTripToEdit != null) {
                    viewModel.updateTripDetails(
                        trip = targetTripToEdit,
                        title = title,
                        startDate = startDate,
                        endDate = endDate,
                        budget = budget,
                        baseCurrency = baseCurrency,
                        participantNames = participantNames
                    )
                } else {
                    viewModel.createTrip(
                        title = title,
                        startDate = startDate,
                        endDate = endDate,
                        budget = budget,
                        baseCurrency = baseCurrency,
                        participantNames = participantNames
                    )
                }
            },
            onDismiss = {
                showCreateTripModal = false
                editingTrip = null
            }
        )
    }

    if (pendingCsvManualImport != null) {
        val pending = pendingCsvManualImport!!
        CsvColumnMappingModal(
            sheet = pending.parsedSheet,
            createNewTrip = pending.createNewTrip,
            initialTripTitle = pending.suggestedTripTitle,
            onConfirmMapping = viewModel::confirmManualCsvMapping,
            onDismiss = viewModel::dismissManualCsvMapping
        )
    }

    val pendingDuplicateCandidate by viewModel.pendingDuplicate.collectAsStateWithLifecycle()
    if (pendingDuplicateCandidate != null) {
        val candidate = pendingDuplicateCandidate!!
        AlertDialog(
            onDismissRequest = {
                viewModel.resolveDuplicateTrip(candidate, overwrite = false)
            },
            title = {
                Text(
                    text = if (isEn) "Trip Already Exists" else "Viaje ya existente",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isEn) {
                        "The trip \"${candidate.existing.title}\" already exists. Do you want to overwrite it?"
                    } else {
                        "El viaje \"${candidate.existing.title}\" ya existe. ¿Quieres sobreescribirlo?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resolveDuplicateTrip(candidate, overwrite = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TravelBlue)
                ) {
                    Text(if (isEn) "Overwrite" else "Sobreescribir")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.resolveDuplicateTrip(candidate, overwrite = false)
                    }
                ) {
                    Text(if (isEn) "Skip" else "Saltar")
                }
            }
        )
    }

    if (showSuggestionsModal) {
        SuggestionsModal(onDismiss = { showSuggestionsModal = false })
    }
}
