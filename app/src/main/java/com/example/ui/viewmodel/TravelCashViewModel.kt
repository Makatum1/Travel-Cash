package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TravelCashDatabase
import com.example.data.model.AvatarColorPalette
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplit
import com.example.data.model.IncomeContribution
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.TripEntity
import com.example.data.remote.NominatimPlaceSuggestion
import com.example.data.remote.NominatimService
import com.example.data.repository.TravelCashRepository
import com.example.domain.DebtCalculationResult
import com.example.domain.DebtCalculator
import android.net.Uri
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.AppThemeMode
import com.example.util.CsvColumnMapping
import com.example.util.ExportImportManager
import com.example.util.ParsedCsvSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.util.TripPackage
import java.time.LocalDate
import java.util.UUID

enum class MainTab(val titleEs: String, val titleEn: String) {
    EXPENSES("Gastos", "Expenses"),
    SPLITS("Cuentas", "Splits"),
    STATS("Datos", "Data"),
    MAP("Mapa", "Map"),
    TRIPS("Viajes", "Trips"),
    SETTINGS("Ajustes", "Settings")
}

data class ToastMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isError: Boolean = false
)

data class CsvPendingManualImport(
    val parsedSheet: ParsedCsvSheet,
    val createNewTrip: Boolean,
    val suggestedTripTitle: String
)

data class TravelCashUiState(
    val trips: List<TripEntity> = emptyList(),
    val activeTripId: String? = null,
    val activeTrip: TripEntity? = null,
    val activeParticipants: List<ParticipantEntity> = emptyList(),
    val activeExpenses: List<ExpenseEntity> = emptyList(),
    val activeIncomes: List<IncomeEntity> = emptyList(),
    val allParticipants: List<ParticipantEntity> = emptyList(),
    val allExpenses: List<ExpenseEntity> = emptyList(),
    val allIncomes: List<IncomeEntity> = emptyList(),
    val debtResult: DebtCalculationResult = DebtCalculationResult(
        emptyList(), emptyList(), 0.0, 0.0, 0.0
    ),
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val extraBalance: Double = 0.0,
    val isInitialized: Boolean = false
)

class TravelCashViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = TravelCashDatabase.getInstance(application).travelCashDao()
    private val repository = TravelCashRepository(dao)

    private val _selectedTab = MutableStateFlow(MainTab.EXPENSES)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _activeTripId = MutableStateFlow<String?>(null)

    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
    val toastMessage: StateFlow<ToastMessage?> = _toastMessage.asStateFlow()

    // Filtros en Pestaña Gastos
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDateFilter = MutableStateFlow<String?>(null)
    val selectedDateFilter: StateFlow<String?> = _selectedDateFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    // Ubicación GPS y sugerencias Nominatim
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    private val _placeSuggestions = MutableStateFlow<List<NominatimPlaceSuggestion>>(emptyList())
    val placeSuggestions: StateFlow<List<NominatimPlaceSuggestion>> = _placeSuggestions.asStateFlow()

    private val _isSearchingPlaces = MutableStateFlow(false)
    val isSearchingPlaces: StateFlow<Boolean> = _isSearchingPlaces.asStateFlow()

    private var placeSearchJob: Job? = null

    // Estado para mapeo manual de CSV
    private val _pendingCsvManualImport = MutableStateFlow<CsvPendingManualImport?>(null)
    val pendingCsvManualImport: StateFlow<CsvPendingManualImport?> = _pendingCsvManualImport.asStateFlow()

    val uiState: StateFlow<TravelCashUiState> = combine(
        repository.allTripsFlow,
        repository.allParticipantsFlow,
        repository.allExpensesFlow,
        repository.allIncomesFlow,
        _activeTripId
    ) { trips, participants, expenses, incomes, currentActiveId ->
        val resolvedActiveTrip = trips.find { it.id == currentActiveId } ?: trips.firstOrNull()
        val resolvedId = resolvedActiveTrip?.id

        val activeParts = if (resolvedId != null) {
            participants.filter { it.tripId == resolvedId }
        } else emptyList()

        val activeExps = if (resolvedId != null) {
            expenses.filter { it.tripId == resolvedId }
        } else emptyList()

        val activeIncs = if (resolvedId != null) {
            incomes.filter { it.tripId == resolvedId }
        } else emptyList()

        val debtResult = DebtCalculator.calculateDebtBalances(activeParts, activeExps, activeIncs)
        val spent = activeExps.sumOf { it.amountInBaseCurrency }
        val inc = activeIncs.sumOf { it.amount }
        // Saldo Extra: Calculado como Ingresos Totales - Gastado Total (o Presupuesto - Gastado + Ingresos si hay presupuesto fijo)
        val extra = if (resolvedActiveTrip?.budget != null && resolvedActiveTrip.budget > 0.0) {
            resolvedActiveTrip.budget - spent + inc
        } else {
            inc - spent
        }

        TravelCashUiState(
            trips = trips,
            activeTripId = resolvedId,
            activeTrip = resolvedActiveTrip,
            activeParticipants = activeParts,
            activeExpenses = activeExps,
            activeIncomes = activeIncs,
            allParticipants = participants,
            allExpenses = expenses,
            allIncomes = incomes,
            debtResult = debtResult,
            totalSpent = spent,
            totalIncome = inc,
            extraBalance = extra,
            isInitialized = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TravelCashUiState()
    )

    init {
        viewModelScope.launch {
            val seedTripId = repository.ensureSeedDataIfEmpty()
            if (_activeTripId.value == null && seedTripId != null) {
                _activeTripId.value = seedTripId
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun selectActiveTrip(tripId: String) {
        _activeTripId.value = tripId
        clearExpenseFilters()
    }

    fun showToast(message: String, isError: Boolean = false) {
        _toastMessage.value = ToastMessage(text = message, isError = isError)
        viewModelScope.launch {
            val currentId = _toastMessage.value?.id
            delay(3200)
            if (_toastMessage.value?.id == currentId) {
                _toastMessage.value = null
            }
        }
    }

    fun dismissToast() {
        _toastMessage.value = null
    }

    // =========================================================================
    // FILTROS DE GASTOS
    // =========================================================================
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateDateFilter(dateIso: String?) {
        _selectedDateFilter.value = dateIso
    }

    fun updateCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun clearExpenseFilters() {
        _searchQuery.value = ""
        _selectedDateFilter.value = null
        _selectedCategoryFilter.value = null
    }

    // =========================================================================
    // BÚSQUEDA DE LUGARES POR PROXIMIDAD GPS (NOMINATIM + DEBOUNCE 350ms)
    // =========================================================================
    fun refreshDeviceLocation() {
        viewModelScope.launch {
            val coords = NominatimService.fetchCurrentDeviceLocation(getApplication())
            if (coords != null) {
                _userLocation.value = coords
            }
        }
    }

    fun setManualUserLocation(lat: Double, lon: Double) {
        _userLocation.value = lat to lon
    }

    fun onPlaceQueryChanged(query: String) {
        placeSearchJob?.cancel()
        if (query.trim().length < 2) {
            _placeSuggestions.value = emptyList()
            _isSearchingPlaces.value = false
            return
        }
        placeSearchJob = viewModelScope.launch {
            _isSearchingPlaces.value = true
            delay(350) // Debounce de 350ms
            val coords = _userLocation.value
            val results = NominatimService.searchPlacesByProximity(
                query = query,
                userLat = coords?.first,
                userLon = coords?.second
            )
            _placeSuggestions.value = results
            _isSearchingPlaces.value = false
        }
    }

    fun clearPlaceSuggestions() {
        _placeSuggestions.value = emptyList()
        _isSearchingPlaces.value = false
    }

    // =========================================================================
    // CRUD DE VIAJES
    // =========================================================================
    fun createTrip(
        title: String,
        startDate: String,
        endDate: String,
        budget: Double?,
        baseCurrency: String,
        participantNames: List<String>
    ) {
        viewModelScope.launch {
            val tripId = "trip-${UUID.randomUUID().toString().take(8)}"
            val newTrip = TripEntity(
                id = tripId,
                title = title.trim().ifEmpty { "Nuevo Viaje" },
                startDate = startDate,
                endDate = endDate,
                budget = budget?.takeIf { it > 0.0 },
                baseCurrency = baseCurrency,
                headerPhotoUri = null,
                createdAt = System.currentTimeMillis()
            )
            val cleanNames = participantNames.map { it.trim() }.filter { it.isNotEmpty() }
                .ifEmpty { listOf("Viajero 1", "Viajero 2") }

            val parts = cleanNames.mapIndexed { idx, name ->
                ParticipantEntity(
                    id = "part-${UUID.randomUUID().toString().take(8)}",
                    tripId = tripId,
                    name = name,
                    avatarColor = AvatarColorPalette.colors[idx % AvatarColorPalette.colors.size]
                )
            }

            repository.insertTrip(newTrip)
            repository.insertParticipants(parts)
            _activeTripId.value = tripId
            showToast("Viaje \"${newTrip.title}\" creado correctamente")
        }
    }

    fun createSampleTrip() {
        viewModelScope.launch {
            val isEn = _language.value == AppLanguage.EN
            val tripId = "trip-${UUID.randomUUID().toString().take(8)}"
            val now = LocalDate.now()
            val sampleTrip = TripEntity(
                id = tripId,
                title = if (isEn) "Trip to Rome" else "Viaje a Roma",
                startDate = now.minusDays(2).toString(),
                endDate = now.plusDays(5).toString(),
                budget = 1200.0,
                baseCurrency = "EUR",
                headerPhotoUri = null,
                createdAt = System.currentTimeMillis()
            )

            val p1Id = "part-${UUID.randomUUID().toString().take(8)}"
            val p2Id = "part-${UUID.randomUUID().toString().take(8)}"
            val p3Id = "part-${UUID.randomUUID().toString().take(8)}"

            val p1 = ParticipantEntity(id = p1Id, tripId = tripId, name = if (isEn) "Alex" else "Alejandro", avatarColor = AvatarColorPalette.colors[0])
            val p2 = ParticipantEntity(id = p2Id, tripId = tripId, name = if (isEn) "Maria" else "María", avatarColor = AvatarColorPalette.colors[1])
            val p3 = ParticipantEntity(id = p3Id, tripId = tripId, name = if (isEn) "Carlos" else "Carlos", avatarColor = AvatarColorPalette.colors[2])
            val participants = listOf(p1, p2, p3)

            val exp1Id = "exp-${UUID.randomUUID().toString().take(8)}"
            val exp1 = ExpenseEntity(
                id = exp1Id,
                tripId = tripId,
                amount = 64.50,
                currency = "EUR",
                amountInBaseCurrency = 64.50,
                exchangeRate = 1.0,
                category = "Restauración",
                date = now.minusDays(1).toString(),
                paymentMethod = "Tarjeta",
                notes = if (isEn) "Dinner at Trattoria Romana" else "Cena en Trattoria Romana",
                latitude = 41.8902,
                longitude = 12.4922,
                placeName = "Coliseo, Roma",
                country = "Italia",
                originName = "",
                originLatitude = null,
                originLongitude = null,
                paidById = p1Id,
                splits = listOf(
                    ExpenseSplit("$exp1Id-1", exp1Id, p1Id, 21.50, 33.33),
                    ExpenseSplit("$exp1Id-2", exp1Id, p2Id, 21.50, 33.33),
                    ExpenseSplit("$exp1Id-3", exp1Id, p3Id, 21.50, 33.34)
                ),
                createdAt = System.currentTimeMillis() - 86400000L
            )

            val exp2Id = "exp-${UUID.randomUUID().toString().take(8)}"
            val exp2 = ExpenseEntity(
                id = exp2Id,
                tripId = tripId,
                amount = 48.00,
                currency = "EUR",
                amountInBaseCurrency = 48.00,
                exchangeRate = 1.0,
                category = "Transporte",
                date = now.toString(),
                paymentMethod = "Bizum",
                notes = if (isEn) "Taxi from airport to hotel" else "Taxi aeropuerto al hotel",
                latitude = 41.9028,
                longitude = 12.4964,
                placeName = "Roma Termini",
                country = "Italia",
                originName = if (isEn) "Fiumicino Airport" else "Aeropuerto Fiumicino",
                originLatitude = 41.8003,
                originLongitude = 12.2389,
                paidById = p2Id,
                splits = listOf(
                    ExpenseSplit("$exp2Id-1", exp2Id, p1Id, 16.00, 33.33),
                    ExpenseSplit("$exp2Id-2", exp2Id, p2Id, 16.00, 33.33),
                    ExpenseSplit("$exp2Id-3", exp2Id, p3Id, 16.00, 33.34)
                ),
                createdAt = System.currentTimeMillis() - 3600000L
            )

            val exp3Id = "exp-${UUID.randomUUID().toString().take(8)}"
            val exp3 = ExpenseEntity(
                id = exp3Id,
                tripId = tripId,
                amount = 36.00,
                currency = "EUR",
                amountInBaseCurrency = 36.00,
                exchangeRate = 1.0,
                category = "Ocio",
                date = now.toString(),
                paymentMethod = "Bote Común",
                notes = if (isEn) "Museum entrance tickets" else "Entradas museo y galería",
                latitude = 41.9038,
                longitude = 12.4534,
                placeName = "Museos Vaticanos",
                country = "Italia",
                originName = "",
                originLatitude = null,
                originLongitude = null,
                paidById = DebtCalculator.BOTE_COMUN_ID,
                splits = listOf(
                    ExpenseSplit("$exp3Id-1", exp3Id, p1Id, 12.00, 33.33),
                    ExpenseSplit("$exp3Id-2", exp3Id, p2Id, 12.00, 33.33),
                    ExpenseSplit("$exp3Id-3", exp3Id, p3Id, 12.00, 33.34)
                ),
                createdAt = System.currentTimeMillis()
            )

            val incId = "inc-${UUID.randomUUID().toString().take(8)}"
            val sampleIncome = IncomeEntity(
                id = incId,
                tripId = tripId,
                amount = 90.00,
                date = now.minusDays(1).toString(),
                description = if (isEn) "Initial Common Pot fund" else "Fondo inicial Bote Común",
                basePerPerson = 30.0,
                calculationMethod = "perPerson",
                contributions = listOf(
                    IncomeContribution(p1Id, 30.0, 0.0),
                    IncomeContribution(p2Id, 30.0, 0.0),
                    IncomeContribution(p3Id, 30.0, 0.0)
                )
            )

            repository.insertTrip(sampleTrip)
            repository.insertParticipants(participants)
            repository.insertExpenses(listOf(exp1, exp2, exp3))
            repository.insertIncome(sampleIncome)

            _activeTripId.value = tripId
            showToast(
                if (isEn) "Sample trip \"${sampleTrip.title}\" added successfully!"
                else "¡Viaje de prueba \"${sampleTrip.title}\" añadido con éxito!"
            )
        }
    }

    fun updateTrip(trip: TripEntity) {
        viewModelScope.launch {
            repository.updateTrip(trip)
            showToast("Viaje actualizado")
        }
    }

    fun updateTripDetails(
        trip: TripEntity,
        title: String,
        startDate: String,
        endDate: String,
        budget: Double?,
        baseCurrency: String,
        participantNames: List<String>
    ) {
        viewModelScope.launch {
            val updatedTrip = trip.copy(
                title = title.trim().ifEmpty { trip.title },
                startDate = startDate,
                endDate = endDate,
                budget = budget?.takeIf { it > 0.0 },
                baseCurrency = baseCurrency
            )
            repository.updateTrip(updatedTrip)

            val cleanNames = participantNames.map { it.trim() }.filter { it.isNotEmpty() }
            if (cleanNames.isNotEmpty()) {
                val existingParts = uiState.value.allParticipants.filter { it.tripId == trip.id }
                val usedExistingIds = mutableSetOf<String>()

                cleanNames.forEachIndexed { idx, name ->
                    val exactMatch = existingParts.find {
                        it.id !in usedExistingIds && it.name.equals(name, ignoreCase = true)
                    }
                    if (exactMatch != null) {
                        usedExistingIds.add(exactMatch.id)
                        if (exactMatch.name != name) {
                            repository.updateParticipant(exactMatch.copy(name = name))
                        }
                    } else {
                        val newPart = ParticipantEntity(
                            id = "part-${UUID.randomUUID().toString().take(8)}",
                            tripId = trip.id,
                            name = name,
                            avatarColor = AvatarColorPalette.colors[(existingParts.size + idx) % AvatarColorPalette.colors.size]
                        )
                        repository.insertParticipant(newPart)
                    }
                }

                // Eliminar participantes que el usuario haya quitado explícitamente de la lista al editar
                existingParts.filter { it.id !in usedExistingIds }.forEach { removed ->
                    repository.deleteParticipant(removed.id)
                }
            }

            val isEn = _language.value == AppLanguage.EN
            showToast(
                if (isEn) "Trip \"${updatedTrip.title}\" updated" else "Viaje \"${updatedTrip.title}\" actualizado"
            )
        }
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            repository.deleteCompleteTrip(tripId)
            val remaining = repository.getAllTripsOnce()
            _activeTripId.value = remaining.firstOrNull()?.id
            showToast("Viaje eliminado")
        }
    }

    // =========================================================================
    // CRUD DE PARTICIPANTES
    // =========================================================================
    fun addParticipant(name: String) {
        val tripId = uiState.value.activeTripId ?: return
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val idx = uiState.value.activeParticipants.size
            val newPart = ParticipantEntity(
                id = "part-${UUID.randomUUID().toString().take(8)}",
                tripId = tripId,
                name = clean,
                avatarColor = AvatarColorPalette.colors[idx % AvatarColorPalette.colors.size]
            )
            repository.insertParticipant(newPart)
            showToast("Participante \"$clean\" añadido")
        }
    }

    private val prefs = application.getSharedPreferences("travel_cash_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        runCatching {
            AppThemeMode.valueOf(prefs.getString("theme_mode_v2", AppThemeMode.LIGHT.name) ?: AppThemeMode.LIGHT.name)
        }.getOrDefault(AppThemeMode.LIGHT)
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _language = MutableStateFlow(
        runCatching {
            val saved = prefs.getString("app_language", null)
            if (saved != null && saved != "SYSTEM") {
                AppLanguage.valueOf(saved)
            } else {
                AppLanguage.ES
            }
        }.getOrDefault(AppLanguage.ES)
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _reminderNotificationsEnabled = MutableStateFlow(
        prefs.getBoolean("reminder_notifications_enabled", false)
    )
    val reminderNotificationsEnabled: StateFlow<Boolean> = _reminderNotificationsEnabled.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode_v2", mode.name).apply()
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        prefs.edit().putString("app_language", lang.name).apply()
        val msg = when (lang) {
            AppLanguage.EN -> "Language changed to English"
            AppLanguage.ES -> "Idioma cambiado a Español"
        }
        showToast(msg)
    }

    fun setReminderNotificationsEnabled(enabled: Boolean) {
        _reminderNotificationsEnabled.value = enabled
        prefs.edit().putBoolean("reminder_notifications_enabled", enabled).apply()
        val isEn = _language.value == AppLanguage.EN
        val app = getApplication<Application>()
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        val channelId = "travel_cash_reminders"

        if (enabled) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    if (isEn) "Trip Expense Reminders" else "Recordatorios de Gastos de Viaje",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = if (isEn) {
                        "Reminders to log daily trip expenses and common pot contributions"
                    } else {
                        "Recordatorios para anotar los gastos diarios del viaje y el bote común"
                    }
                }
                nm?.createNotificationChannel(channel)
            }
            try {
                val activeTitle = uiState.value.activeTrip?.title ?: "Travel Cash"
                val notification = androidx.core.app.NotificationCompat.Builder(app, channelId)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(
                        if (isEn) "Travel Cash · Expense Reminder Active" else "Travel Cash · Recordatorio Activado"
                    )
                    .setContentText(
                        if (isEn) {
                            "Don't forget to log today's expenses for \"$activeTitle\"."
                        } else {
                            "No olvides registrar los gastos de hoy en \"$activeTitle\"."
                        }
                    )
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
                nm?.notify(1001, notification)
            } catch (_: SecurityException) {
            }
            showToast(
                if (isEn) "🔔 Reminder notifications enabled" else "🔔 Notificaciones de recordatorio activadas"
            )
        } else {
            nm?.cancel(1001)
            showToast(
                if (isEn) "🔕 Reminder notifications disabled" else "🔕 Notificaciones de recordatorio desactivadas"
            )
        }
    }

    fun updateParticipantPhoto(participant: ParticipantEntity, newPhotoUri: String?) {
        viewModelScope.launch {
            repository.updateParticipant(participant.copy(photoUri = newPhotoUri))
            val msg = if (newPhotoUri != null) {
                "Foto de ${participant.name} actualizada"
            } else {
                "Icono por defecto restaurado para ${participant.name}"
            }
            showToast(msg)
        }
    }

    fun processPdfImport(pdfBytes: ByteArray) {
        val active = uiState.value.activeTrip ?: return
        viewModelScope.launch {
            val extracted = ExportImportManager.parseExpensesFromPdfBytes(
                pdfBytes = pdfBytes,
                tripId = active.id,
                baseCurrency = active.baseCurrency,
                existingParticipants = uiState.value.activeParticipants
            )
            if (extracted.isNotEmpty()) {
                repository.insertExpenses(extracted)
                showToast("PDF importado: ${extracted.size} gastos añadidos al viaje")
            } else {
                showToast("No se detectaron líneas de gasto con importe en el PDF seleccionado", isError = true)
            }
        }
    }

    fun updateParticipantName(participant: ParticipantEntity, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.updateParticipant(participant.copy(name = clean))
            showToast("Nombre actualizado a \"$clean\"")
        }
    }

    fun deleteParticipant(participant: ParticipantEntity) {
        viewModelScope.launch {
            repository.deleteParticipant(participant.id)
            showToast("Participante \"${participant.name}\" eliminado")
        }
    }

    // =========================================================================
    // CRUD DE GASTOS
    // =========================================================================
    fun saveExpense(expense: ExpenseEntity, isEdit: Boolean) {
        viewModelScope.launch {
            if (isEdit) {
                repository.updateExpense(expense)
                showToast("Gasto actualizado correctamente")
            } else {
                repository.insertExpense(expense)
                showToast("Gasto guardado correctamente")
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            showToast("Gasto eliminado")
        }
    }

    // =========================================================================
    // CRUD DE INGRESOS (BOTE COMÚN)
    // =========================================================================
    fun saveIncome(income: IncomeEntity, isEdit: Boolean) {
        viewModelScope.launch {
            if (isEdit) {
                repository.updateIncome(income)
                showToast("Aportación al bote actualizada")
            } else {
                repository.insertIncome(income)
                showToast("Aportación al bote registrada")
            }
        }
    }

    fun deleteIncome(incomeId: String) {
        viewModelScope.launch {
            repository.deleteIncome(incomeId)
            showToast("Aportación al bote eliminada")
        }
    }

    // =========================================================================
    // IMPORTACIÓN CSV Y MAPEO MANUAL DE COLUMNAS
    // =========================================================================
    fun processCsvImportContent(
        csvContent: String,
        createNewTrip: Boolean,
        suggestedFileName: String = "Viaje Importado CSV"
    ) {
        val sheet = ExportImportManager.parseCsvContent(csvContent)
        if (sheet.rows.isEmpty()) {
            showToast("El archivo CSV está vacío o no tiene filas válidas", isError = true)
            return
        }

        if (sheet.isStandardAutoDetected) {
            finalizeCsvImport(
                sheet = sheet,
                mapping = sheet.suggestedMapping,
                createNewTrip = createNewTrip,
                tripTitle = suggestedFileName.removeSuffix(".csv")
            )
        } else {
            // Abrir Modal de Mapeo Manual de Columnas CSV
            _pendingCsvManualImport.value = CsvPendingManualImport(
                parsedSheet = sheet,
                createNewTrip = createNewTrip,
                suggestedTripTitle = suggestedFileName.removeSuffix(".csv")
            )
        }
    }

    fun confirmManualCsvMapping(mapping: CsvColumnMapping, customTripTitle: String) {
        val pending = _pendingCsvManualImport.value ?: return
        _pendingCsvManualImport.value = null
        finalizeCsvImport(
            sheet = pending.parsedSheet,
            mapping = mapping,
            createNewTrip = pending.createNewTrip,
            tripTitle = customTripTitle.ifBlank { pending.suggestedTripTitle }
        )
    }

    fun dismissManualCsvMapping() {
        _pendingCsvManualImport.value = null
    }

    private fun finalizeCsvImport(
        sheet: ParsedCsvSheet,
        mapping: CsvColumnMapping,
        createNewTrip: Boolean,
        tripTitle: String
    ) {
        viewModelScope.launch {
            val targetTripId: String
            val baseCurrency: String
            val existingParts: List<ParticipantEntity>

            if (createNewTrip || uiState.value.activeTrip == null) {
                targetTripId = "trip-csv-${UUID.randomUUID().toString().take(8)}"
                baseCurrency = "EUR"
                existingParts = emptyList()
            } else {
                val active = uiState.value.activeTrip!!
                targetTripId = active.id
                baseCurrency = active.baseCurrency
                existingParts = uiState.value.activeParticipants
            }

            val (newParticipants, newExpenses) = ExportImportManager.convertRowsToEntities(
                tripId = targetTripId,
                baseCurrency = baseCurrency,
                existingParticipants = existingParts,
                rows = sheet.rows,
                mapping = mapping
            )

            if (newExpenses.isEmpty()) {
                showToast("No se encontraron importes válidos en las filas seleccionadas", isError = true)
                return@launch
            }

            if (createNewTrip || uiState.value.activeTrip == null) {
                val minDate = newExpenses.minOfOrNull { it.date } ?: LocalDate.now().toString()
                val maxDate = newExpenses.maxOfOrNull { it.date } ?: LocalDate.now().toString()
                val newTrip = TripEntity(
                    id = targetTripId,
                    title = tripTitle.trim().ifEmpty { "Viaje Importado CSV" },
                    startDate = minDate,
                    endDate = maxDate,
                    budget = null,
                    baseCurrency = baseCurrency,
                    headerPhotoUri = null,
                    createdAt = System.currentTimeMillis()
                )
                repository.insertTrip(newTrip)
            }

            if (newParticipants.isNotEmpty()) {
                repository.insertParticipants(newParticipants)
            }
            repository.insertExpenses(newExpenses)
            _activeTripId.value = targetTripId
            showToast("CSV importado: ${newExpenses.size} gastos registrados")
        }
    }

    // =========================================================================
    // BACKUP Y RESTAURACIÓN JSON (ESTADO COMPLETO DE LA APP)
    // =========================================================================
    fun generateFullBackupJson(): String {
        val state = uiState.value
        return ExportImportManager.exportFullBackupJson(
            trips = state.trips,
            participants = state.allParticipants,
            expenses = state.allExpenses,
            incomes = state.allIncomes,
            activeTripId = state.activeTripId,
            themeMode = _themeMode.value.name,
            language = _language.value.name
        )
    }

    fun restoreJsonBackup(jsonContent: String, replaceAll: Boolean) {
        viewModelScope.launch {
            try {
                val payload = ExportImportManager.parseFullBackupJson(jsonContent)
                if (payload.trips.isEmpty()) {
                    showToast("El archivo JSON no contiene viajes válidos para restaurar", isError = true)
                    return@launch
                }
                repository.restoreBackup(
                    trips = payload.trips,
                    participants = payload.participants,
                    expenses = payload.expenses,
                    incomes = payload.incomes,
                    replaceAll = replaceAll
                )

                // Restaurar ajustes de tema e idioma si vienen incluidos en el backup
                payload.themeMode?.let { modeStr ->
                    runCatching { AppThemeMode.valueOf(modeStr) }.getOrNull()?.let { mode ->
                        _themeMode.value = mode
                        prefs.edit().putString("theme_mode", mode.name).apply()
                    }
                }
                payload.language?.let { langStr ->
                    runCatching { AppLanguage.valueOf(langStr) }.getOrNull()?.let { lang ->
                        _language.value = lang
                        prefs.edit().putString("app_language", lang.name).apply()
                    }
                }

                // Seleccionar el viaje activo exportado si existe, o el primer viaje restaurado
                val preferredTripId = payload.activeTripId?.takeIf { id ->
                    payload.trips.any { it.id == id }
                } ?: payload.trips.first().id
                _activeTripId.value = preferredTripId

                val modeText = if (replaceAll) "reemplazando datos" else "fusionando datos"
                showToast(
                    "Backup restaurado ($modeText): ${payload.trips.size} viajes, ${payload.expenses.size} gastos y ${payload.incomes.size} ingresos"
                )
            } catch (e: Exception) {
                showToast("Error al procesar el JSON de copia de seguridad", isError = true)
            }
        }
    }

    fun remapTripPackage(pkg: TripPackage, targetTripId: String): TripPackage {
        val capitalizedTitle = ExportImportManager.capitalizeTripName(pkg.trip.title)
        val partIdMap = mutableMapOf<String, String>()
        val newParticipants = pkg.participants.map { part ->
            val newId = "part-${UUID.randomUUID().toString().take(8)}"
            partIdMap[part.id] = newId
            part.copy(id = newId, tripId = targetTripId)
        }

        val newExpenses = pkg.expenses.mapIndexed { idx, exp ->
            val newExpId = "exp-${UUID.randomUUID().toString().take(8)}-$idx"
            val newPayerId = if (exp.paidById == DebtCalculator.BOTE_COMUN_ID) {
                DebtCalculator.BOTE_COMUN_ID
            } else {
                partIdMap[exp.paidById] ?: exp.paidById
            }
            val newSplits = exp.splits.mapIndexed { sIdx, split ->
                val mappedPartId = partIdMap[split.participantId] ?: split.participantId
                split.copy(
                    id = "$newExpId-s-$sIdx",
                    expenseId = newExpId,
                    participantId = mappedPartId
                )
            }
            exp.copy(
                id = newExpId,
                tripId = targetTripId,
                paidById = newPayerId,
                splits = newSplits
            )
        }

        val newIncomes = pkg.incomes.mapIndexed { idx, inc ->
            val newIncId = "inc-${UUID.randomUUID().toString().take(8)}-$idx"
            val newContribs = inc.contributions.map { c ->
                val mappedPartId = partIdMap[c.participantId] ?: c.participantId
                c.copy(participantId = mappedPartId)
            }
            inc.copy(
                id = newIncId,
                tripId = targetTripId,
                contributions = newContribs
            )
        }

        return TripPackage(
            trip = pkg.trip.copy(id = targetTripId, title = capitalizedTitle),
            participants = newParticipants,
            expenses = newExpenses,
            incomes = newIncomes
        )
    }

    fun exportTripsToZipUri(uri: Uri, targetTripId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val state = uiState.value
                val tripsToExport = if (targetTripId == null) {
                    state.trips
                } else {
                    state.trips.filter { it.id == targetTripId }
                }
                if (tripsToExport.isEmpty()) {
                    val isEn = _language.value == AppLanguage.EN
                    showToast(if (isEn) "No trips to export" else "No hay viajes para exportar", isError = true)
                    return@launch
                }
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    ExportImportManager.writeTripsToZip(
                        outputStream = out,
                        tripsToExport = tripsToExport,
                        allParticipants = state.allParticipants,
                        allExpenses = state.allExpenses,
                        allIncomes = state.allIncomes
                    )
                }
                val isEn = _language.value == AppLanguage.EN
                val msg = if (targetTripId == null) {
                    if (isEn) "ZIP file with all trips exported" else "Archivo ZIP con todos los viajes exportado con éxito"
                } else {
                    val title = tripsToExport.first().title
                    if (isEn) "Trip \"$title\" exported in ZIP" else "Viaje \"$title\" exportado en ZIP"
                }
                showToast(msg)
            } catch (e: Exception) {
                val isEn = _language.value == AppLanguage.EN
                showToast(if (isEn) "Error exporting ZIP file" else "Error al exportar el archivo ZIP", isError = true)
            }
        }
    }

    fun exportSingleTripJsonToUri(uri: Uri, targetTripId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val state = uiState.value
                val trip = state.trips.find { it.id == targetTripId } ?: return@launch
                val parts = state.allParticipants.filter { it.tripId == targetTripId }
                val exps = state.allExpenses.filter { it.tripId == targetTripId }
                val incs = state.allIncomes.filter { it.tripId == targetTripId }
                val jsonContent = ExportImportManager.exportFullBackupJson(
                    trips = listOf(trip),
                    participants = parts,
                    expenses = exps,
                    incomes = incs
                )
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonContent.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                val isEn = _language.value == AppLanguage.EN
                showToast(if (isEn) "Trip \"${trip.title}\" exported" else "Viaje \"${trip.title}\" exportado con éxito")
            } catch (e: Exception) {
                val isEn = _language.value == AppLanguage.EN
                showToast(if (isEn) "Error exporting trip JSON" else "Error al exportar el viaje en JSON", isError = true)
            }
        }
    }

    fun importTripsFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes()
                } ?: byteArrayOf()
                if (bytes.isEmpty()) {
                    val isEn = _language.value == AppLanguage.EN
                    showToast(if (isEn) "Empty file selected" else "El archivo seleccionado está vacío", isError = true)
                    return@launch
                }
                val packages = ExportImportManager.readTripsFromBytes(bytes)
                if (packages.isEmpty()) {
                    val isEn = _language.value == AppLanguage.EN
                    showToast(
                        if (isEn) "No valid trips found in file (.zip / .json)" else "No se encontraron viajes válidos en el archivo (.zip / .json)",
                        isError = true
                    )
                } else {
                    processZipImport(packages)
                }
            } catch (e: Exception) {
                val isEn = _language.value == AppLanguage.EN
                showToast(if (isEn) "Error reading import file" else "Error al leer el archivo de importación", isError = true)
            }
        }
    }

    private val _pendingDuplicates = MutableStateFlow<List<DuplicateTripCandidate>>(emptyList())
    val pendingDuplicate: StateFlow<DuplicateTripCandidate?> = _pendingDuplicates
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun processZipImport(packages: List<TripPackage>) {
        viewModelScope.launch {
            val existingTrips = repository.getAllTripsOnce()
            val duplicates = mutableListOf<DuplicateTripCandidate>()
            var newlyImportedCount = 0

            packages.forEach { pkg ->
                val normalizedTitle = ExportImportManager.normalizeTripName(pkg.trip.title)
                val existingMatch = existingTrips.find {
                    ExportImportManager.normalizeTripName(it.title) == normalizedTitle
                }
                if (existingMatch != null) {
                    duplicates.add(DuplicateTripCandidate(imported = pkg, existing = existingMatch))
                } else {
                    val newTripId = "trip-${UUID.randomUUID().toString().take(8)}"
                    val remapped = remapTripPackage(pkg, newTripId)

                    repository.insertTrip(remapped.trip)
                    repository.insertParticipants(remapped.participants)
                    repository.insertExpenses(remapped.expenses)
                    repository.insertIncomes(remapped.incomes)
                    newlyImportedCount++

                    if (_activeTripId.value == null) {
                        _activeTripId.value = newTripId
                    }
                }
            }

            if (duplicates.isNotEmpty()) {
                _pendingDuplicates.value = duplicates
            } else if (newlyImportedCount > 0) {
                val isEn = _language.value == AppLanguage.EN
                showToast(
                    if (isEn) "Imported $newlyImportedCount trip(s) successfully"
                    else "Se importaron $newlyImportedCount viaje(s) con éxito"
                )
            } else if (packages.isEmpty()) {
                val isEn = _language.value == AppLanguage.EN
                showToast(
                    if (isEn) "No valid trip JSON files found in ZIP"
                    else "No se encontraron archivos JSON de viaje válidos en el ZIP",
                    isError = true
                )
            }
        }
    }

    fun resolveDuplicateTrip(candidate: DuplicateTripCandidate, overwrite: Boolean) {
        viewModelScope.launch {
            val targetTripId = candidate.existing.id
            if (overwrite) {
                val remapped = remapTripPackage(candidate.imported, targetTripId)

                repository.deleteCompleteTrip(targetTripId)
                repository.insertTrip(remapped.trip)
                repository.insertParticipants(remapped.participants)
                repository.insertExpenses(remapped.expenses)
                repository.insertIncomes(remapped.incomes)

                val isEn = _language.value == AppLanguage.EN
                showToast(
                    if (isEn) "Trip \"${remapped.trip.title}\" overwritten"
                    else "Viaje \"${remapped.trip.title}\" sobreescrito"
                )
            }

            _pendingDuplicates.value = _pendingDuplicates.value.drop(1)
            if (_pendingDuplicates.value.isEmpty()) {
                val isEn = _language.value == AppLanguage.EN
                showToast(
                    if (isEn) "Import completed successfully"
                    else "Importación completada con éxito"
                )
            }
        }
    }
}

data class DuplicateTripCandidate(
    val imported: TripPackage,
    val existing: TripEntity
)
