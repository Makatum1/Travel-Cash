package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TravelCashDatabase
import com.example.data.model.AvatarColorPalette
import com.example.data.model.CategoryCatalog
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
import kotlinx.coroutines.withContext
import com.example.util.TripPackage
import com.example.util.UiTexts
import java.time.LocalDate
import java.util.UUID

enum class MainTab(val titleEs: String, val titleEn: String) {
    EXPENSES("Gastos", "Expenses"),
    SPLITS("Cuentas", "Splits"),
    STATS("Datos", "Data"),
    MAP("Mapa", "Map"),
    TRIPS("Viajes", "Trips"),
    SETTINGS("Ajustes", "Settings");

    fun getTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ES -> titleEs
        AppLanguage.EN -> titleEn
        AppLanguage.FR -> when (this) {
            EXPENSES -> "Dépenses"
            SPLITS -> "Comptes"
            STATS -> "Données"
            MAP -> "Carte"
            TRIPS -> "Voyages"
            SETTINGS -> "Paramètres"
        }
        AppLanguage.IT -> when (this) {
            EXPENSES -> "Spese"
            SPLITS -> "Saldi"
            STATS -> "Dati"
            MAP -> "Mappa"
            TRIPS -> "Viaggi"
            SETTINGS -> "Impostazioni"
        }
        AppLanguage.ZH -> when (this) {
            EXPENSES -> "支出"
            SPLITS -> "结算"
            STATS -> "统计"
            MAP -> "地图"
            TRIPS -> "行程"
            SETTINGS -> "设置"
        }
        AppLanguage.NL -> when (this) {
            EXPENSES -> "Uitgaven"
            SPLITS -> "Splitsen"
            STATS -> "Gegevens"
            MAP -> "Kaart"
            TRIPS -> "Reizen"
            SETTINGS -> "Instellingen"
        }
        AppLanguage.DE -> when (this) {
            EXPENSES -> "Ausgaben"
            SPLITS -> "Abrechnung"
            STATS -> "Daten"
            MAP -> "Karte"
            TRIPS -> "Reisen"
            SETTINGS -> "Einstellungen"
        }
        AppLanguage.PL -> when (this) {
            EXPENSES -> "Wydatki"
            SPLITS -> "Rozliczenia"
            STATS -> "Dane"
            MAP -> "Mapa"
            TRIPS -> "Podróże"
            SETTINGS -> "Ustawienia"
        }
        AppLanguage.PT -> when (this) {
            EXPENSES -> "Despesas"
            SPLITS -> "Contas"
            STATS -> "Dados"
            MAP -> "Mapa"
            TRIPS -> "Viagens"
            SETTINGS -> "Definições"
        }
    }
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

    private val prefs = application.getSharedPreferences("travel_cash_settings", Context.MODE_PRIVATE)
    private val keyLastActiveTripId = "last_active_trip_id"

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
                AppLanguage.detectSystemLanguage()
            }
        }.getOrElse { AppLanguage.detectSystemLanguage() }
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _selectedTab = MutableStateFlow(MainTab.EXPENSES)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _activeTripId = MutableStateFlow<String?>(
        prefs.getString(keyLastActiveTripId, null)?.takeIf { it.isNotBlank() }
    )

    private fun setActiveTripId(tripId: String?) {
        _activeTripId.value = tripId
        prefs.edit().apply {
            if (tripId.isNullOrBlank()) {
                remove(keyLastActiveTripId)
            } else {
                putString(keyLastActiveTripId, tripId)
            }
        }.apply()
    }

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
        val spent = activeExps
            .filter { !CategoryCatalog.isDebtSettlementExpense(it) }
            .sumOf { it.amountInBaseCurrency }
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
        val initialLang = _language.value
        java.util.Locale.setDefault(initialLang.locale)
        com.example.util.FormatUtils.currentAppLocale = initialLang.locale

        viewModelScope.launch {
            val seedTripId = repository.ensureSeedDataIfEmpty()
            val allTrips = dao.getAllTripsOnce()
            val savedId = _activeTripId.value
            val validSavedTrip = if (savedId != null) allTrips.find { it.id == savedId } else null
            if (validSavedTrip == null) {
                val fallbackId = seedTripId ?: allTrips.firstOrNull()?.id
                setActiveTripId(fallbackId)
            }
            // Asegurar que si existe el viaje de prueba (Viaje a Roma), el gasto del restaurante tenga la imagen de ejemplo
            try {
                val romaTrip = allTrips.find { it.title.contains("Roma", ignoreCase = true) }
                if (romaTrip != null) {
                    val romaExps = dao.getAllExpensesOnce().filter { it.tripId == romaTrip.id }
                    val trattoriaExp = romaExps.find { (it.placeName.contains("Coliseo", ignoreCase = true) || it.notes.contains("Trattoria", ignoreCase = true)) && it.photos.isEmpty() }
                    if (trattoriaExp != null) {
                        val receiptUri = com.example.util.SampleReceiptGenerator.getOrCreateSampleReceipt(getApplication())
                        repository.updateExpense(trattoriaExp.copy(photos = listOf(receiptUri)))
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun selectActiveTrip(tripId: String) {
        setActiveTripId(tripId)
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
                userLon = coords?.second,
                context = getApplication()
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
            setActiveTripId(tripId)
            val lang = _language.value
            val msg = when (lang) {
                AppLanguage.ES -> "Viaje \"${newTrip.title}\" creado correctamente"
                AppLanguage.EN -> "Trip \"${newTrip.title}\" created successfully"
                AppLanguage.FR -> "Voyage « ${newTrip.title} » créé avec succès"
                AppLanguage.IT -> "Viaggio \"${newTrip.title}\" creato con successo"
                AppLanguage.ZH -> "行程 \"${newTrip.title}\" 创建成功"
                AppLanguage.NL -> "Reis \"${newTrip.title}\" succesvol aangemaakt"
                AppLanguage.DE -> "Reise „${newTrip.title}“ erfolgreich erstellt"
                AppLanguage.PL -> "Podróż „${newTrip.title}” utworzona pomyślnie"
                AppLanguage.PT -> "Viagem \"${newTrip.title}\" criada com sucesso"
            }
            showToast(msg)
        }
    }

    fun createSampleTrip() {
        viewModelScope.launch {
            val lang = _language.value
            val tripTitle = when (lang) {
                AppLanguage.ES -> "Viaje a Roma"
                AppLanguage.EN -> "Trip to Rome"
                AppLanguage.FR -> "Voyage à Rome"
                AppLanguage.IT -> "Viaggio a Roma"
                AppLanguage.ZH -> "罗马之旅"
                AppLanguage.NL -> "Reis naar Rome"
                AppLanguage.DE -> "Reise nach Rom"
                AppLanguage.PL -> "Wycieczka do Rzymu"
                AppLanguage.PT -> "Viagem a Roma"
            }
            val tripId = "trip-${UUID.randomUUID().toString().take(8)}"
            val now = LocalDate.now()
            val sampleTrip = TripEntity(
                id = tripId,
                title = tripTitle,
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

            val p1Name = when (lang) {
                AppLanguage.ES -> "Alejandro"
                AppLanguage.EN -> "Alex"
                AppLanguage.FR -> "Alexandre"
                AppLanguage.IT -> "Alessandro"
                AppLanguage.ZH -> "亚历克斯"
                AppLanguage.NL -> "Alex"
                AppLanguage.DE -> "Alexander"
                AppLanguage.PL -> "Aleksander"
                AppLanguage.PT -> "Alexandre"
            }
            val p2Name = when (lang) {
                AppLanguage.ES -> "María"
                AppLanguage.EN -> "Maria"
                AppLanguage.FR -> "Marie"
                AppLanguage.IT -> "Maria"
                AppLanguage.ZH -> "玛丽亚"
                AppLanguage.NL -> "Maria"
                AppLanguage.DE -> "Maria"
                AppLanguage.PL -> "Maria"
                AppLanguage.PT -> "Maria"
            }
            val p3Name = when (lang) {
                AppLanguage.ES, AppLanguage.EN -> "Carlos"
                AppLanguage.FR -> "Charles"
                AppLanguage.IT -> "Carlo"
                AppLanguage.ZH -> "卡洛斯"
                AppLanguage.NL -> "Karel"
                AppLanguage.DE -> "Karl"
                AppLanguage.PL -> "Karol"
                AppLanguage.PT -> "Carlos"
            }

            val p1 = ParticipantEntity(id = p1Id, tripId = tripId, name = p1Name, avatarColor = AvatarColorPalette.colors[0])
            val p2 = ParticipantEntity(id = p2Id, tripId = tripId, name = p2Name, avatarColor = AvatarColorPalette.colors[1])
            val p3 = ParticipantEntity(id = p3Id, tripId = tripId, name = p3Name, avatarColor = AvatarColorPalette.colors[2])
            val participants = listOf(p1, p2, p3)

            val exp1Notes = when (lang) {
                AppLanguage.ES -> "Cena en Trattoria Romana"
                AppLanguage.EN -> "Dinner at Trattoria Romana"
                AppLanguage.FR -> "Dîner à la Trattoria Romana"
                AppLanguage.IT -> "Cena alla Trattoria Romana"
                AppLanguage.ZH -> "罗马餐厅晚餐"
                AppLanguage.NL -> "Diner in Trattoria Romana"
                AppLanguage.DE -> "Abendessen in der Trattoria Romana"
                AppLanguage.PL -> "Kolacja w Trattoria Romana"
                AppLanguage.PT -> "Jantar na Trattoria Romana"
            }
            val exp1Place = when (lang) {
                AppLanguage.ES -> "Coliseo, Roma"
                AppLanguage.EN -> "Colosseum, Rome"
                AppLanguage.FR -> "Colisée, Rome"
                AppLanguage.IT -> "Colosseo, Roma"
                AppLanguage.ZH -> "罗马斗兽场"
                AppLanguage.NL -> "Colosseum, Rome"
                AppLanguage.DE -> "Kolosseum, Rom"
                AppLanguage.PL -> "Koloseum, Rzym"
                AppLanguage.PT -> "Coliseu, Roma"
            }
            val exp1Country = when (lang) {
                AppLanguage.ES, AppLanguage.IT -> "Italia"
                AppLanguage.EN -> "Italy"
                AppLanguage.FR -> "Italie"
                AppLanguage.ZH -> "意大利"
                AppLanguage.NL -> "Italië"
                AppLanguage.DE -> "Italien"
                AppLanguage.PL -> "Włochy"
                AppLanguage.PT -> "Itália"
            }

            val exp1Id = "exp-${UUID.randomUUID().toString().take(8)}"
            val receiptUri = com.example.util.SampleReceiptGenerator.getOrCreateSampleReceipt(getApplication())
            val exp1 = ExpenseEntity(
                id = exp1Id,
                tripId = tripId,
                amount = 64.50,
                currency = "EUR",
                amountInBaseCurrency = 64.50,
                exchangeRate = 1.0,
                category = "Restaurantes y Comida",
                date = now.minusDays(1).toString(),
                paymentMethod = "Tarjeta",
                notes = exp1Notes,
                latitude = 41.8902,
                longitude = 12.4922,
                placeName = exp1Place,
                country = exp1Country,
                originName = "",
                originLatitude = null,
                originLongitude = null,
                paidById = p1Id,
                photos = listOf(receiptUri),
                splits = listOf(
                    ExpenseSplit("$exp1Id-1", exp1Id, p1Id, 21.50, 33.33),
                    ExpenseSplit("$exp1Id-2", exp1Id, p2Id, 21.50, 33.33),
                    ExpenseSplit("$exp1Id-3", exp1Id, p3Id, 21.50, 33.34)
                ),
                createdAt = System.currentTimeMillis() - 86400000L
            )

            val exp2Notes = when (lang) {
                AppLanguage.ES -> "Taxi aeropuerto al hotel"
                AppLanguage.EN -> "Taxi from airport to hotel"
                AppLanguage.FR -> "Taxi de l'aéroport à l'hôtel"
                AppLanguage.IT -> "Taxi dall'aeroporto all'hotel"
                AppLanguage.ZH -> "机场到酒店的出租车"
                AppLanguage.NL -> "Taxi van vliegveld naar hotel"
                AppLanguage.DE -> "Taxi vom Flughafen zum Hotel"
                AppLanguage.PL -> "Taksówka z lotniska do hotelu"
                AppLanguage.PT -> "Táxi do aeroporto para o hotel"
            }
            val exp2Origin = when (lang) {
                AppLanguage.ES -> "Aeropuerto Fiumicino"
                AppLanguage.EN -> "Fiumicino Airport"
                AppLanguage.FR -> "Aéroport de Fiumicino"
                AppLanguage.IT -> "Aeroporto di Fiumicino"
                AppLanguage.ZH -> "菲乌米奇诺机场"
                AppLanguage.NL -> "Luchthaven Fiumicino"
                AppLanguage.DE -> "Flughafen Fiumicino"
                AppLanguage.PL -> "Lotnisko Fiumicino"
                AppLanguage.PT -> "Aeroporto Fiumicino"
            }

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
                notes = exp2Notes,
                latitude = 41.9028,
                longitude = 12.4964,
                placeName = "Roma Termini",
                country = exp1Country,
                originName = exp2Origin,
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

            val exp3Notes = when (lang) {
                AppLanguage.ES -> "Entradas museo y galería"
                AppLanguage.EN -> "Museum and gallery tickets"
                AppLanguage.FR -> "Billets musée et galerie"
                AppLanguage.IT -> "Biglietti museo e galleria"
                AppLanguage.ZH -> "博物馆与画廊门票"
                AppLanguage.NL -> "Museum- en galerietickets"
                AppLanguage.DE -> "Museums- und Galerietickets"
                AppLanguage.PL -> "Bilety do muzeum i galerii"
                AppLanguage.PT -> "Bilhetes para museu e galeria"
            }
            val exp3Place = when (lang) {
                AppLanguage.ES -> "Museos Vaticanos"
                AppLanguage.EN -> "Vatican Museums"
                AppLanguage.FR -> "Musées du Vatican"
                AppLanguage.IT -> "Musei Vaticani"
                AppLanguage.ZH -> "梵蒂冈博物馆"
                AppLanguage.NL -> "Vaticaanse Musea"
                AppLanguage.DE -> "Vatikanische Museen"
                AppLanguage.PL -> "Muzea Watykańskie"
                AppLanguage.PT -> "Museus do Vaticano"
            }

            val exp3Id = "exp-${UUID.randomUUID().toString().take(8)}"
            val exp3 = ExpenseEntity(
                id = exp3Id,
                tripId = tripId,
                amount = 36.00,
                currency = "EUR",
                amountInBaseCurrency = 36.00,
                exchangeRate = 1.0,
                category = "Entradas y Ocio",
                date = now.toString(),
                paymentMethod = "Bote Común",
                notes = exp3Notes,
                latitude = 41.9038,
                longitude = 12.4534,
                placeName = exp3Place,
                country = exp1Country,
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

            val incDesc = when (lang) {
                AppLanguage.ES -> "Fondo inicial Bote Común"
                AppLanguage.EN -> "Initial Common Pot fund"
                AppLanguage.FR -> "Fonds initial cagnotte commune"
                AppLanguage.IT -> "Fondo iniziale cassa comune"
                AppLanguage.ZH -> "公账初始资金"
                AppLanguage.NL -> "Startkapitaal gezamenlijke pot"
                AppLanguage.DE -> "Anfangskapital Gemeinschaftskasse"
                AppLanguage.PL -> "Początkowy fundusz wspólnej kasy"
                AppLanguage.PT -> "Fundo inicial do Fundo Comum"
            }

            val incId = "inc-${UUID.randomUUID().toString().take(8)}"
            val sampleIncome = IncomeEntity(
                id = incId,
                tripId = tripId,
                amount = 90.00,
                date = now.minusDays(1).toString(),
                description = incDesc,
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

            setActiveTripId(tripId)
            val sampleMsg = when (lang) {
                AppLanguage.ES -> "¡Viaje de prueba \"${sampleTrip.title}\" añadido con éxito!"
                AppLanguage.EN -> "Sample trip \"${sampleTrip.title}\" added successfully!"
                AppLanguage.FR -> "Voyage d'essai « ${sampleTrip.title} » ajouté avec succès !"
                AppLanguage.IT -> "Viaggio di prova \"${sampleTrip.title}\" aggiunto con successo!"
                AppLanguage.ZH -> "测试行程 \"${sampleTrip.title}\" 添加成功！"
                AppLanguage.NL -> "Proefreis \"${sampleTrip.title}\" succesvol toegevoegd!"
                AppLanguage.DE -> "Testreise „${sampleTrip.title}“ erfolgreich hinzugefügt!"
                AppLanguage.PL -> "Podróż próbna „${sampleTrip.title}” dodana pomyślnie!"
                AppLanguage.PT -> "Viagem de teste \"${sampleTrip.title}\" adicionada com sucesso!"
            }
            showToast(sampleMsg)
        }
    }

    fun updateTrip(trip: TripEntity) {
        viewModelScope.launch {
            repository.updateTrip(trip)
            showToast(UiTexts.get(_language.value).toastTripUpdated)
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

            val lang = _language.value
            val msg = when (lang) {
                AppLanguage.ES -> "Viaje \"${updatedTrip.title}\" actualizado"
                AppLanguage.EN -> "Trip \"${updatedTrip.title}\" updated"
                AppLanguage.FR -> "Voyage « ${updatedTrip.title} » mis à jour"
                AppLanguage.IT -> "Viaggio \"${updatedTrip.title}\" aggiornato"
                AppLanguage.ZH -> "行程 \"${updatedTrip.title}\" 已更新"
                AppLanguage.NL -> "Reis \"${updatedTrip.title}\" bijgewerkt"
                AppLanguage.DE -> "Reise „${updatedTrip.title}“ aktualisiert"
                AppLanguage.PL -> "Podróż „${updatedTrip.title}” zaktualizowana"
                AppLanguage.PT -> "Viagem \"${updatedTrip.title}\" atualizada"
            }
            showToast(msg)
        }
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            repository.deleteCompleteTrip(tripId)
            val remaining = repository.getAllTripsOnce()
            if (_activeTripId.value == tripId || remaining.none { it.id == _activeTripId.value }) {
                setActiveTripId(remaining.firstOrNull()?.id)
            }
            showToast(UiTexts.get(_language.value).toastTripDeleted)
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
            val lang = _language.value
            val msg = when (lang) {
                AppLanguage.ES -> "Participante \"$clean\" añadido"
                AppLanguage.EN -> "Participant \"$clean\" added"
                AppLanguage.FR -> "Participant « $clean » ajouté"
                AppLanguage.IT -> "Partecipante \"$clean\" aggiunto"
                AppLanguage.ZH -> "参与者 \"$clean\" 已添加"
                AppLanguage.NL -> "Deelnemer \"$clean\" toegevoegd"
                AppLanguage.DE -> "Teilnehmer „$clean“ hinzugefügt"
                AppLanguage.PL -> "Uczestnik „$clean” dodany"
                AppLanguage.PT -> "Participante \"$clean\" adicionado"
            }
            showToast(msg)
        }
    }



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
        java.util.Locale.setDefault(lang.locale)
        com.example.util.FormatUtils.currentAppLocale = lang.locale
        val msg = when (lang) {
            AppLanguage.ES -> "Idioma cambiado a Español"
            AppLanguage.EN -> "Language changed to English"
            AppLanguage.FR -> "Langue changée en Français"
            AppLanguage.IT -> "Lingua cambiata in Italiano"
            AppLanguage.ZH -> "语言已切换为中文"
            AppLanguage.NL -> "Taal gewijzigd in Nederlands"
            AppLanguage.DE -> "Sprache auf Deutsch geändert"
            AppLanguage.PL -> "Język zmieniony na Polski"
            AppLanguage.PT -> "Idioma alterado para Português"
        }
        showToast(msg)
    }

    fun setReminderNotificationsEnabled(enabled: Boolean) {
        _reminderNotificationsEnabled.value = enabled
        prefs.edit().putBoolean("reminder_notifications_enabled", enabled).apply()
        val lang = _language.value
        val app = getApplication<Application>()
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        val channelId = "travel_cash_reminders"

        val channelName = when (lang) {
            AppLanguage.ES -> "Recordatorios de Gastos de Viaje"
            AppLanguage.EN -> "Trip Expense Reminders"
            AppLanguage.FR -> "Rappels de dépenses de voyage"
            AppLanguage.IT -> "Promemoria spese di viaggio"
            AppLanguage.ZH -> "旅行支出提醒"
            AppLanguage.NL -> "Reiskostenherinneringen"
            AppLanguage.DE -> "Reisekosten-Erinnerungen"
            AppLanguage.PL -> "Przypomnienia o wydatkach w podróży"
            AppLanguage.PT -> "Lembretes de Despesas de Viagem"
        }

        val channelDesc = when (lang) {
            AppLanguage.ES -> "Recordatorios para anotar los gastos diarios del viaje y el bote común"
            AppLanguage.EN -> "Reminders to log daily trip expenses and common pot contributions"
            AppLanguage.FR -> "Rappels pour enregistrer les dépenses quotidiennes du voyage et la cagnotte"
            AppLanguage.IT -> "Promemoria per registrare le spese giornaliere del viaggio e la cassa comune"
            AppLanguage.ZH -> "提醒记录每日旅行支出及公账存入"
            AppLanguage.NL -> "Herinneringen om dagelijkse reiskosten en inleg in de pot vast te leggen"
            AppLanguage.DE -> "Erinnerungen zur Erfassung täglicher Reiseausgaben und Kassenbeiträge"
            AppLanguage.PL -> "Przypomnienia o zapisywaniu codziennych wydatków z podróży i wspólnej kasy"
            AppLanguage.PT -> "Lembretes para registar as despesas diárias da viagem e o fundo comum"
        }

        val notifTitle = when (lang) {
            AppLanguage.ES -> "Travel Cash · Recordatorio Activado"
            AppLanguage.EN -> "Travel Cash · Expense Reminder Active"
            AppLanguage.FR -> "Travel Cash · Rappel activé"
            AppLanguage.IT -> "Travel Cash · Promemoria attivato"
            AppLanguage.ZH -> "Travel Cash · 支出提醒已激活"
            AppLanguage.NL -> "Travel Cash · Herinnering actief"
            AppLanguage.DE -> "Travel Cash · Erinnerung aktiviert"
            AppLanguage.PL -> "Travel Cash · Przypomnienie aktywne"
            AppLanguage.PT -> "Travel Cash · Lembrete Ativado"
        }

        if (enabled) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    channelName,
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = channelDesc
                }
                nm?.createNotificationChannel(channel)
            }
            try {
                val activeTitle = uiState.value.activeTrip?.title ?: "Travel Cash"
                val notifText = when (lang) {
                    AppLanguage.ES -> "No olvides registrar los gastos de hoy en \"$activeTitle\"."
                    AppLanguage.EN -> "Don't forget to log today's expenses for \"$activeTitle\"."
                    AppLanguage.FR -> "N'oubliez pas d'enregistrer les dépenses du jour pour « $activeTitle »."
                    AppLanguage.IT -> "Non dimenticare di registrare le spese di oggi in \"$activeTitle\"."
                    AppLanguage.ZH -> "别忘了记录今天在 \"$activeTitle\" 中的支出。"
                    AppLanguage.NL -> "Vergeet niet de uitgaven van vandaag voor \"$activeTitle\" te registreren."
                    AppLanguage.DE -> "Vergessen Sie nicht, die heutigen Ausgaben für „$activeTitle“ zu erfassen."
                    AppLanguage.PL -> "Nie zapomnij zarejestrować dzisiejszych wydatków w „$activeTitle”."
                    AppLanguage.PT -> "Não se esqueça de registar as despesas de hoje em \"$activeTitle\"."
                }
                val notification = androidx.core.app.NotificationCompat.Builder(app, channelId)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(notifTitle)
                    .setContentText(notifText)
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
                nm?.notify(1001, notification)
            } catch (_: SecurityException) {
            }
            val enabledToast = when (lang) {
                AppLanguage.ES -> "🔔 Notificaciones de recordatorio activadas"
                AppLanguage.EN -> "🔔 Reminder notifications enabled"
                AppLanguage.FR -> "🔔 Notifications de rappel activées"
                AppLanguage.IT -> "🔔 Notifiche di promemoria attivate"
                AppLanguage.ZH -> "🔔 提醒通知已开启"
                AppLanguage.NL -> "🔔 Herinneringsmeldingen ingeschakeld"
                AppLanguage.DE -> "🔔 Erinnerungsbenachrichtigungen aktiviert"
                AppLanguage.PL -> "🔔 Powiadomienia z przypomnieniami włączone"
                AppLanguage.PT -> "🔔 Notificações de lembrete ativadas"
            }
            showToast(enabledToast)
        } else {
            nm?.cancel(1001)
            val disabledToast = when (lang) {
                AppLanguage.ES -> "🔕 Notificaciones de recordatorio desactivadas"
                AppLanguage.EN -> "🔕 Reminder notifications disabled"
                AppLanguage.FR -> "🔕 Notifications de rappel désactivées"
                AppLanguage.IT -> "🔕 Notifiche di promemoria disattivate"
                AppLanguage.ZH -> "🔕 提醒通知已关闭"
                AppLanguage.NL -> "🔕 Herinneringsmeldingen uitgeschakeld"
                AppLanguage.DE -> "🔕 Erinnerungsbenachrichtigungen deaktiviert"
                AppLanguage.PL -> "🔕 Powiadomienia z przypomnieniami wyłączone"
                AppLanguage.PT -> "🔕 Notificações de lembrete desativadas"
            }
            showToast(disabledToast)
        }
    }

    fun updateParticipantPhoto(participant: ParticipantEntity, newPhotoUri: String?) {
        val lang = _language.value
        viewModelScope.launch {
            repository.updateParticipant(participant.copy(photoUri = newPhotoUri))
            val msg = if (newPhotoUri != null) {
                when (lang) {
                    AppLanguage.ES -> "Foto de ${participant.name} actualizada"
                    AppLanguage.EN -> "Photo for ${participant.name} updated"
                    AppLanguage.FR -> "Photo de ${participant.name} mise à jour"
                    AppLanguage.IT -> "Foto di ${participant.name} aggiornata"
                    AppLanguage.ZH -> "${participant.name} 的照片已更新"
                    AppLanguage.NL -> "Foto van ${participant.name} bijgewerkt"
                    AppLanguage.DE -> "Foto von ${participant.name} aktualisiert"
                    AppLanguage.PL -> "Zdjęcie użytkownika ${participant.name} zaktualizowane"
                    AppLanguage.PT -> "Foto de ${participant.name} atualizada"
                }
            } else {
                when (lang) {
                    AppLanguage.ES -> "Icono por defecto restaurado para ${participant.name}"
                    AppLanguage.EN -> "Default icon restored for ${participant.name}"
                    AppLanguage.FR -> "Icône par défaut restaurée pour ${participant.name}"
                    AppLanguage.IT -> "Icona predefinita ripristinata per ${participant.name}"
                    AppLanguage.ZH -> "已为 ${participant.name} 恢复默认图标"
                    AppLanguage.NL -> "Standaardpictogram hersteld voor ${participant.name}"
                    AppLanguage.DE -> "Standard-Icon für ${participant.name} wiederhergestellt"
                    AppLanguage.PL -> "Przywrócono domyślną ikonę dla ${participant.name}"
                    AppLanguage.PT -> "Ícone predefinido restaurado para ${participant.name}"
                }
            }
            showToast(msg)
        }
    }

    fun updateParticipantName(participant: ParticipantEntity, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        val lang = _language.value
        viewModelScope.launch {
            repository.updateParticipant(participant.copy(name = clean))
            val msg = when (lang) {
                AppLanguage.ES -> "Nombre actualizado a \"$clean\""
                AppLanguage.EN -> "Name updated to \"$clean\""
                AppLanguage.FR -> "Nom mis à jour en « $clean »"
                AppLanguage.IT -> "Nome aggiornato a \"$clean\""
                AppLanguage.ZH -> "名称已更新为 \"$clean\""
                AppLanguage.NL -> "Naam bijgewerkt naar \"$clean\""
                AppLanguage.DE -> "Name in „$clean“ geändert"
                AppLanguage.PL -> "Nazwa zaktualizowana na „$clean”"
                AppLanguage.PT -> "Nome atualizado para \"$clean\""
            }
            showToast(msg)
        }
    }

    fun deleteParticipant(participant: ParticipantEntity) {
        val lang = _language.value
        viewModelScope.launch {
            repository.deleteParticipant(participant.id)
            val msg = when (lang) {
                AppLanguage.ES -> "Participante \"${participant.name}\" eliminado"
                AppLanguage.EN -> "Participant \"${participant.name}\" deleted"
                AppLanguage.FR -> "Participant « ${participant.name} » supprimé"
                AppLanguage.IT -> "Partecipante \"${participant.name}\" eliminato"
                AppLanguage.ZH -> "参与者 \"${participant.name}\" 已删除"
                AppLanguage.NL -> "Deelnemer \"${participant.name}\" verwijderd"
                AppLanguage.DE -> "Teilnehmer „${participant.name}“ gelöscht"
                AppLanguage.PL -> "Uczestnik „${participant.name}” usunięty"
                AppLanguage.PT -> "Participante \"${participant.name}\" eliminado"
            }
            showToast(msg)
        }
    }

    // =========================================================================
    // CRUD DE GASTOS
    // =========================================================================
    fun saveExpense(expense: ExpenseEntity, isEdit: Boolean) {
        val texts = UiTexts.get(_language.value)
        viewModelScope.launch {
            if (isEdit) {
                repository.updateExpense(expense)
                showToast(texts.toastExpenseUpdated)
            } else {
                repository.insertExpense(expense)
                showToast(texts.toastExpenseSaved)
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        val texts = UiTexts.get(_language.value)
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            showToast(texts.toastExpenseDeleted)
        }
    }

    // =========================================================================
    // CRUD DE INGRESOS (BOTE COMÚN)
    // =========================================================================
    fun saveIncome(income: IncomeEntity, isEdit: Boolean) {
        val texts = UiTexts.get(_language.value)
        viewModelScope.launch {
            if (isEdit) {
                repository.updateIncome(income)
                showToast(texts.toastIncomeSaved)
            } else {
                repository.insertIncome(income)
                showToast(texts.toastIncomeSaved)
            }
        }
    }

    fun deleteIncome(incomeId: String) {
        val texts = UiTexts.get(_language.value)
        viewModelScope.launch {
            repository.deleteIncome(incomeId)
            showToast(texts.toastIncomeDeleted)
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
        val lang = _language.value
        val sheet = ExportImportManager.parseCsvContent(csvContent)
        if (sheet.rows.isEmpty()) {
            val emptyError = when (lang) {
                AppLanguage.ES -> "El archivo CSV está vacío o no tiene filas válidas"
                AppLanguage.EN -> "CSV file is empty or has no valid rows"
                AppLanguage.FR -> "Le fichier CSV est vide ou ne contient aucune ligne valide"
                AppLanguage.IT -> "Il file CSV è vuoto o non contiene righe valide"
                AppLanguage.ZH -> "CSV 文件为空或没有有效数据行"
                AppLanguage.NL -> "CSV-bestand is leeg of bevat geen geldige rijen"
                AppLanguage.DE -> "Die CSV-Datei ist leer oder enthält keine gültigen Zeilen"
                AppLanguage.PL -> "Plik CSV jest pusty lub nie zawiera prawidłowych wierszy"
                AppLanguage.PT -> "O ficheiro CSV está vazio ou não tem linhas válidas"
            }
            showToast(emptyError, isError = true)
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
                val lang = _language.value
                val noAmountsError = when (lang) {
                    AppLanguage.ES -> "No se encontraron importes válidos en las filas seleccionadas"
                    AppLanguage.EN -> "No valid amounts found in selected rows"
                    AppLanguage.FR -> "Aucun montant valide trouvé dans les lignes sélectionnées"
                    AppLanguage.IT -> "Nessun importo valido trovato nelle righe selezionate"
                    AppLanguage.ZH -> "所选行中未找到有效金额"
                    AppLanguage.NL -> "Geen geldige bedragen gevonden in geselecteerde rijen"
                    AppLanguage.DE -> "Keine gültigen Beträge in den ausgewählten Zeilen gefunden"
                    AppLanguage.PL -> "Nie znaleziono prawidłowych kwot w wybranych wierszach"
                    AppLanguage.PT -> "Não foram encontrados montantes válidos nas linhas selecionadas"
                }
                showToast(noAmountsError, isError = true)
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
            setActiveTripId(targetTripId)
            val lang = _language.value
            val successMsg = when (lang) {
                AppLanguage.ES -> "CSV importado: ${newExpenses.size} gastos registrados"
                AppLanguage.EN -> "CSV imported: ${newExpenses.size} expenses added"
                AppLanguage.FR -> "CSV importé : ${newExpenses.size} dépenses enregistrées"
                AppLanguage.IT -> "CSV importato: ${newExpenses.size} spese registrate"
                AppLanguage.ZH -> "CSV 导入成功: 已添加 ${newExpenses.size} 笔支出"
                AppLanguage.NL -> "CSV geïmporteerd: ${newExpenses.size} uitgaven geregistreerd"
                AppLanguage.DE -> "CSV importiert: ${newExpenses.size} Ausgaben erfasst"
                AppLanguage.PL -> "Zaimportowano CSV: dodano ${newExpenses.size} wydatków"
                AppLanguage.PT -> "CSV importado: ${newExpenses.size} despesas registadas"
            }
            showToast(successMsg)
        }
    }

    // =========================================================================
    // EXPORTACIÓN E IMPORTACIÓN DE VIAJES (.ZIP / .JSON)
    // =========================================================================
    private val _pendingDuplicates = MutableStateFlow<List<DuplicateTripCandidate>>(emptyList())
    val pendingDuplicate: StateFlow<DuplicateTripCandidate?> = _pendingDuplicates
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun exportTripsToZipUri(uri: Uri, targetTripId: String?) {
        val lang = _language.value
        val texts = UiTexts.get(lang)
        viewModelScope.launch {
            try {
                val state = uiState.value
                val tripsToExport = if (targetTripId == null) {
                    state.trips
                } else {
                    state.trips.filter { it.id == targetTripId }
                }
                if (tripsToExport.isEmpty()) {
                    showToast(texts.toastNoTripsToExport, isError = true)
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        ExportImportManager.writeTripsToZip(
                            outputStream = out,
                            tripsToExport = tripsToExport,
                            allParticipants = state.allParticipants,
                            allExpenses = state.allExpenses,
                            allIncomes = state.allIncomes
                        )
                    } ?: throw IllegalStateException("Cannot open output stream")
                }
                val msg = if (targetTripId == null) {
                    texts.toastZipExported
                } else {
                    val title = tripsToExport.first().title
                    when (lang) {
                        AppLanguage.ES -> "Viaje \"$title\" exportado en ZIP"
                        AppLanguage.EN -> "Trip \"$title\" exported in ZIP"
                        AppLanguage.FR -> "Voyage « $title » exporté en ZIP"
                        AppLanguage.IT -> "Viaggio \"$title\" esportato in ZIP"
                        AppLanguage.ZH -> "行程 \"$title\" 已导出为 ZIP"
                        AppLanguage.NL -> "Reis \"$title\" geëxporteerd in ZIP"
                        AppLanguage.DE -> "Reise „$title“ als ZIP exportiert"
                        AppLanguage.PL -> "Podróż „$title” wyeksportowana w ZIP"
                        AppLanguage.PT -> "Viagem \"$title\" exportada em ZIP"
                    }
                }
                showToast(msg)
            } catch (e: Exception) {
                showToast(texts.toastZipExportError, isError = true)
            }
        }
    }

    fun exportSingleTripJsonToUri(uri: Uri, targetTripId: String) {
        val lang = _language.value
        val texts = UiTexts.get(lang)
        viewModelScope.launch {
            try {
                val state = uiState.value
                val targetTrip = state.trips.find { it.id == targetTripId } ?: state.activeTrip
                if (targetTrip == null) {
                    showToast(texts.toastTripNotFound, isError = true)
                    return@launch
                }
                val parts = state.allParticipants.filter { it.tripId == targetTrip.id }
                val exps = state.allExpenses.filter { it.tripId == targetTrip.id }
                val incs = state.allIncomes.filter { it.tripId == targetTrip.id }
                val json = ExportImportManager.exportFullBackupJson(
                    trips = listOf(targetTrip),
                    participants = parts,
                    expenses = exps,
                    incomes = incs
                )
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                        out.flush()
                    } ?: throw IllegalStateException("Cannot open output stream")
                }
                val msg = when (lang) {
                    AppLanguage.ES -> "Viaje \"${targetTrip.title}\" exportado (.json)"
                    AppLanguage.EN -> "Trip \"${targetTrip.title}\" exported (.json)"
                    AppLanguage.FR -> "Voyage « ${targetTrip.title} » exporté (.json)"
                    AppLanguage.IT -> "Viaggio \"${targetTrip.title}\" esportato (.json)"
                    AppLanguage.ZH -> "行程 \"${targetTrip.title}\" 已导出 (.json)"
                    AppLanguage.NL -> "Reis \"${targetTrip.title}\" geëxporteerd (.json)"
                    AppLanguage.DE -> "Reise „${targetTrip.title}“ exportiert (.json)"
                    AppLanguage.PL -> "Podróż „${targetTrip.title}” wyeksportowana (.json)"
                    AppLanguage.PT -> "Viagem \"${targetTrip.title}\" exportada (.json)"
                }
                showToast(msg)
            } catch (e: Exception) {
                val errMsg = when (lang) {
                    AppLanguage.ES -> "Error al exportar el JSON del viaje"
                    AppLanguage.EN -> "Error exporting trip JSON"
                    AppLanguage.FR -> "Erreur lors de l'exportation du JSON du voyage"
                    AppLanguage.IT -> "Errore durante l'esportazione del JSON del viaggio"
                    AppLanguage.ZH -> "导出行程 JSON 时出错"
                    AppLanguage.NL -> "Fout bij exporteren van reis-JSON"
                    AppLanguage.DE -> "Fehler beim Exportieren der Reise-JSON"
                    AppLanguage.PL -> "Błąd podczas eksportowania pliku JSON podróży"
                    AppLanguage.PT -> "Erro ao exportar o JSON da viagem"
                }
                showToast(errMsg, isError = true)
            }
        }
    }

    fun importTripsFromUri(uri: Uri) {
        val lang = _language.value
        viewModelScope.launch {
            try {
                val packages = withContext(Dispatchers.IO) {
                    val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes()
                    } ?: ByteArray(0)
                    ExportImportManager.readTripsFromBytes(bytes)
                }
                if (packages.isEmpty()) {
                    val noTripsMsg = when (lang) {
                        AppLanguage.ES -> "No se encontraron viajes válidos en el archivo seleccionado"
                        AppLanguage.EN -> "No valid trips found in the selected file"
                        AppLanguage.FR -> "Aucun voyage valide trouvé dans le fichier sélectionné"
                        AppLanguage.IT -> "Nessun viaggio valido trovato nel file selezionato"
                        AppLanguage.ZH -> "所选文件中未找到有效的行程数据"
                        AppLanguage.NL -> "Geen geldige reizen gevonden in het geselecteerde bestand"
                        AppLanguage.DE -> "Keine gültigen Reisen in der ausgewählten Datei gefunden"
                        AppLanguage.PL -> "Nie znaleziono prawidłowych podróży w wybranym pliku"
                        AppLanguage.PT -> "Não foram encontradas viagens válidas no ficheiro selecionado"
                    }
                    showToast(noTripsMsg, isError = true)
                } else {
                    processZipImport(packages)
                }
            } catch (e: Exception) {
                val readErrMsg = when (lang) {
                    AppLanguage.ES -> "Error al leer el archivo a importar"
                    AppLanguage.EN -> "Error reading import file"
                    AppLanguage.FR -> "Erreur lors de la lecture du fichier d'importation"
                    AppLanguage.IT -> "Errore durante la lettura del file da importare"
                    AppLanguage.ZH -> "读取导入文件时出错"
                    AppLanguage.NL -> "Fout bij lezen van te importeren bestand"
                    AppLanguage.DE -> "Fehler beim Lesen der Importdatei"
                    AppLanguage.PL -> "Błąd podczas odczytu importowanego pliku"
                    AppLanguage.PT -> "Erro ao ler o ficheiro a importar"
                }
                showToast(readErrMsg, isError = true)
            }
        }
    }

    private fun remapTripPackage(pkg: TripPackage, targetTripId: String): TripPackage {
        val capitalizedTitle = ExportImportManager.capitalizeTripName(pkg.trip.title)
        val newTrip = pkg.trip.copy(id = targetTripId, title = capitalizedTitle)

        val participantIdMap = mutableMapOf<String, String>()
        val newParticipants = pkg.participants.map { p ->
            val newPartId = "part-${UUID.randomUUID().toString().take(8)}"
            participantIdMap[p.id] = newPartId
            p.copy(id = newPartId, tripId = targetTripId)
        }

        val newExpenses = pkg.expenses.map { exp ->
            val newExpId = "exp-${UUID.randomUUID().toString().take(8)}"
            val remappedPaidBy = if (exp.paidById == DebtCalculator.BOTE_COMUN_ID) {
                DebtCalculator.BOTE_COMUN_ID
            } else {
                participantIdMap[exp.paidById] ?: exp.paidById
            }
            val remappedSplits = exp.splits.mapIndexed { idx, split ->
                split.copy(
                    id = "$newExpId-s-$idx",
                    expenseId = newExpId,
                    participantId = participantIdMap[split.participantId] ?: split.participantId
                )
            }
            exp.copy(
                id = newExpId,
                tripId = targetTripId,
                paidById = remappedPaidBy,
                splits = remappedSplits
            )
        }

        val newIncomes = pkg.incomes.map { inc ->
            val newIncId = "inc-${UUID.randomUUID().toString().take(8)}"
            val remappedContribs = inc.contributions.map { c ->
                c.copy(participantId = participantIdMap[c.participantId] ?: c.participantId)
            }
            inc.copy(
                id = newIncId,
                tripId = targetTripId,
                contributions = remappedContribs
            )
        }

        return TripPackage(newTrip, newParticipants, newExpenses, newIncomes)
    }

    fun processZipImport(packages: List<TripPackage>) {
        viewModelScope.launch {
            val existingTrips = repository.getAllTripsOnce().toMutableList()
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
                    existingTrips.add(remapped.trip)
                    newlyImportedCount++

                    if (_activeTripId.value == null) {
                        setActiveTripId(newTripId)
                    }
                }
            }

            val lang = _language.value
            if (duplicates.isNotEmpty()) {
                _pendingDuplicates.value = duplicates
            } else if (newlyImportedCount > 0) {
                val importedMsg = when (lang) {
                    AppLanguage.ES -> "Se importaron $newlyImportedCount viaje(s) con éxito"
                    AppLanguage.EN -> "Imported $newlyImportedCount trip(s) successfully"
                    AppLanguage.FR -> "$newlyImportedCount voyage(s) importé(s) avec succès"
                    AppLanguage.IT -> "Importati $newlyImportedCount viaggio/i con successo"
                    AppLanguage.ZH -> "成功导入 $newlyImportedCount 个行程"
                    AppLanguage.NL -> "$newlyImportedCount reis/reizen succesvol geïmporteerd"
                    AppLanguage.DE -> "$newlyImportedCount Reise(n) erfolgreich importiert"
                    AppLanguage.PL -> "Zaimportowano $newlyImportedCount podróż(e) pomyślnie"
                    AppLanguage.PT -> "Foram importadas $newlyImportedCount viagem(ns) com sucesso"
                }
                showToast(importedMsg)
            } else if (packages.isEmpty()) {
                val noTripsMsg = when (lang) {
                    AppLanguage.ES -> "No se encontraron viajes válidos en el archivo seleccionado"
                    AppLanguage.EN -> "No valid trips found in the selected file"
                    AppLanguage.FR -> "Aucun voyage valide trouvé dans le fichier sélectionné"
                    AppLanguage.IT -> "Nessun viaggio valido trovato nel file selezionato"
                    AppLanguage.ZH -> "所选文件中未找到有效的行程数据"
                    AppLanguage.NL -> "Geen geldige reizen gevonden in het geselecteerde bestand"
                    AppLanguage.DE -> "Keine gültigen Reisen in der ausgewählten Datei gefunden"
                    AppLanguage.PL -> "Nie znaleziono prawidłowych podróży w wybranym pliku"
                    AppLanguage.PT -> "Não foram encontradas viagens válidas no ficheiro selecionado"
                }
                showToast(noTripsMsg, isError = true)
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

                val lang = _language.value
                val overwrittenMsg = when (lang) {
                    AppLanguage.ES -> "Viaje \"${remapped.trip.title}\" sobreescrito"
                    AppLanguage.EN -> "Trip \"${remapped.trip.title}\" overwritten"
                    AppLanguage.FR -> "Voyage « ${remapped.trip.title} » écrasé"
                    AppLanguage.IT -> "Viaggio \"${remapped.trip.title}\" sovrascritto"
                    AppLanguage.ZH -> "行程 \"${remapped.trip.title}\" 已覆盖"
                    AppLanguage.NL -> "Reis \"${remapped.trip.title}\" overschreven"
                    AppLanguage.DE -> "Reise „${remapped.trip.title}“ überschrieben"
                    AppLanguage.PL -> "Podróż „${remapped.trip.title}” nadpisana"
                    AppLanguage.PT -> "Viagem \"${remapped.trip.title}\" substituída"
                }
                showToast(overwrittenMsg)
            }

            _pendingDuplicates.value = _pendingDuplicates.value.drop(1)
            if (_pendingDuplicates.value.isEmpty()) {
                val texts = UiTexts.get(_language.value)
                showToast(texts.toastImportSuccess)
            }
        }
    }
}

data class DuplicateTripCandidate(
    val imported: TripPackage,
    val existing: TripEntity
)
