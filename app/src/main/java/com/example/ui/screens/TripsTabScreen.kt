package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseEntity
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.TripEntity
import com.example.ui.components.EditableTripRouteIcon
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.CoralRed
import com.example.ui.theme.CoralRedDark
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.LocalAppLanguage
import com.example.util.UiTexts
import com.example.ui.theme.OceanNavy
import com.example.ui.theme.TravelBlue
import com.example.util.FormatUtils
import java.time.LocalDate

@Composable
fun TripsTabScreen(
    trips: List<TripEntity>,
    activeTripId: String?,
    allParticipants: List<ParticipantEntity>,
    allExpenses: List<ExpenseEntity>,
    allIncomes: List<IncomeEntity>,
    onSelectTrip: (String) -> Unit,
    onTripIconClick: (TripEntity) -> Unit,
    onCreateNewTripClick: () -> Unit,
    onAddSampleTripClick: () -> Unit = {},
    onImportZipClick: () -> Unit = {},
    onEditTripClick: (TripEntity) -> Unit = {},
    onExportSingleTripJsonClick: (TripEntity) -> Unit,
    onExportSingleTripPdfClick: (TripEntity) -> Unit = {},
    onDeleteTripConfirmed: (String) -> Unit,
    topHeader: @Composable () -> Unit = {}
) {
    val currentLang = LocalAppLanguage.current
    val texts = UiTexts.get(currentLang)
    val today = remember { LocalDate.now() }
    var tripPendingDeletion by remember { mutableStateOf<TripEntity?>(null) }
    var expandedTripIds by remember { mutableStateOf(setOf<String>()) }

    val participantsByTrip = remember(allParticipants) { allParticipants.groupBy { it.tripId } }
    val expensesByTrip = remember(allExpenses) { allExpenses.groupBy { it.tripId } }
    val incomesByTrip = remember(allIncomes) { allIncomes.groupBy { it.tripId } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "trips_top_header") {
            topHeader()
        }

        // Barra de acciones superiores en Pestaña Viajes
        item(key = "trips_actions_header") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val tripManagementTitle = when (currentLang) {
                        AppLanguage.ES -> "Gestión de Viajes"
                        AppLanguage.EN -> "Trip Management"
                        AppLanguage.FR -> "Gestion des voyages"
                        AppLanguage.IT -> "Gestione dei viaggi"
                        AppLanguage.ZH -> "行程管理"
                        AppLanguage.NL -> "Reisbeheer"
                        AppLanguage.DE -> "Reiseverwaltung"
                        AppLanguage.PL -> "Zarządzanie podróżami"
                        AppLanguage.PT -> "Gestão de Viagens"
                    }
                    Text(
                        text = tripManagementTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy
                    )

                    // Botón de Nuevo Viaje
                    Button(
                        onClick = onCreateNewTripClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TravelBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("create_new_trip_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = texts.newTripBtn,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                    }

                    // Botón para añadir un viaje de prueba (solo visible cuando no haya ningún viaje creado)
                    if (trips.isEmpty()) {
                        OutlinedButton(
                            onClick = onAddSampleTripClick,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFF0FDF4),
                                contentColor = EmeraldGreenDark
                            ),
                            border = BorderStroke(1.5.dp, EmeraldGreen),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("add_sample_trip_button")
                        ) {
                            Icon(
                                Icons.Default.FlightTakeoff,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = EmeraldGreenDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = texts.addSampleTripBtn,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldGreenDark
                            )
                        }

                        // Botón Importar Viaje (solo visible cuando no haya ningún viaje creado)
                        OutlinedButton(
                            onClick = onImportZipClick,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFEFF6FF),
                                contentColor = TravelBlue
                            ),
                            border = BorderStroke(1.5.dp, TravelBlue),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("import_trip_button")
                        ) {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = TravelBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = texts.importZipBtn,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TravelBlue
                            )
                        }
                    }
                }
            }
        }

        // Si la lista de viajes está vacía
        if (trips.isEmpty()) {
            item(key = "trips_empty_state") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TravelBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = TravelBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Text(
                            text = texts.emptyTripsTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = OceanNavy
                        )

                        val emptyTripsSubtitle = when (currentLang) {
                            AppLanguage.ES -> "Crea tu primer viaje para empezar a registrar y organizar todos tus gastos."
                            AppLanguage.EN -> "Create your first trip to start managing expenses, participants and routes."
                            AppLanguage.FR -> "Créez votre premier voyage pour commencer à gérer vos dépenses et vos participants."
                            AppLanguage.IT -> "Crea il tuo primo viaggio per iniziare a gestire spese e partecipanti."
                            AppLanguage.ZH -> "创建您的第一次旅行，开始管理支出、参与者和路线。"
                            AppLanguage.NL -> "Maak je eerste reis aan om uitgaven en deelnemers te beheren."
                            AppLanguage.DE -> "Erstellen Sie Ihre erste Reise, um Ausgaben und Teilnehmer zu verwalten."
                            AppLanguage.PL -> "Utwórz swoją pierwszą podróż, aby zarządzać wydatkami i uczestnikami."
                            AppLanguage.PT -> "Crie a sua primeira viagem para começar a registar e organizar todas as suas despesas."
                        }
                        Text(
                            text = emptyTripsSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Listado de todos los viajes
        items(trips, key = { it.id }) { trip ->
            val isActive = trip.id == activeTripId
            val isExpanded = expandedTripIds.contains(trip.id)
            val tParts = participantsByTrip[trip.id] ?: emptyList()
            val tExps = expensesByTrip[trip.id] ?: emptyList()
            val tIncs = incomesByTrip[trip.id] ?: emptyList()
            val spent = tExps
                .filter { !com.example.data.model.CategoryCatalog.isDebtSettlementExpense(it) }
                .sumOf { it.amountInBaseCurrency }
            val inc = tIncs.sumOf { it.amount }

            val startLocal = runCatching { LocalDate.parse(trip.startDate.take(10)) }.getOrNull()
            val endLocal = runCatching { LocalDate.parse(trip.endDate.take(10)) }.getOrNull()
            val (dateStatusLabel, dateStatusBg, dateStatusColor) = when {
                startLocal != null && today.isBefore(startLocal) -> Triple(
                    "⏳ ${texts.statusUpcoming}",
                    Color(0xFFFEF3C7),
                    Color(0xFFB45309)
                )
                endLocal != null && today.isAfter(endLocal) -> Triple(
                    "🏁 ${texts.statusCompleted}",
                    Color(0xFFF1F5F9),
                    Color(0xFF475569)
                )
                else -> Triple(
                    "🟢 ${texts.statusOngoing}",
                    EmeraldGreenLight,
                    EmeraldGreenDark
                )
            }

            val setAsActiveLabel = when (currentLang) {
                AppLanguage.ES -> "Establecer como viaje activo"
                AppLanguage.EN -> "Set as Active Trip"
                AppLanguage.FR -> "Définir comme voyage actif"
                AppLanguage.IT -> "Imposta come viaggio attivo"
                AppLanguage.ZH -> "设为当前活动行程"
                AppLanguage.NL -> "Instellen als actieve reis"
                AppLanguage.DE -> "Als aktive Reise festlegen"
                AppLanguage.PL -> "Ustaw jako aktywną podróż"
                AppLanguage.PT -> "Definir como viagem ativa"
            }
            val currentlyActiveLabel = when (currentLang) {
                AppLanguage.ES -> "Viaje actualmente activo en la app"
                AppLanguage.EN -> "Currently active trip in the app"
                AppLanguage.FR -> "Voyage actuellement actif dans l'application"
                AppLanguage.IT -> "Viaggio attualmente attivo nell'app"
                AppLanguage.ZH -> "当前应用中的活动行程"
                AppLanguage.NL -> "Momenteel actieve reis in de app"
                AppLanguage.DE -> "Derzeit aktive Reise in der App"
                AppLanguage.PL -> "Aktualnie aktywna podróż w aplikacji"
                AppLanguage.PT -> "Viagem atualmente ativa na aplicação"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        expandedTripIds = if (isExpanded) expandedTripIds - trip.id else expandedTripIds + trip.id
                    }
                    .testTag("trip_card_${trip.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) Color(0xFFF8FAFC) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.5.dp),
                border = BorderStroke(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) TravelBlue else Color(0xFFE2E8F0)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cabecera compacta: Título, estado finalizado/en curso y si está activo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EditableTripRouteIcon(
                            photoUri = trip.headerPhotoUri,
                            onClick = { onTripIconClick(trip) },
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = trip.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = OceanNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isActive) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = TravelBlue
                                    ) {
                                        Text(
                                            text = texts.activeTripBadge,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = dateStatusBg
                                ) {
                                    Text(
                                        text = dateStatusLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = dateStatusColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        // Botón circular para activar viaje directamente sin tener que desplegar
                        Surface(
                            shape = CircleShape,
                            color = if (isActive) TravelBlue else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (isActive) TravelBlue else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { onSelectTrip(trip.id) }
                                .testTag("activate_trip_circle_${trip.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = if (isActive) currentlyActiveLabel else setAsActiveLabel,
                                    tint = if (isActive) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Botón para desplegar / contraer opciones del viaje
                        Surface(
                            shape = CircleShape,
                            color = if (isExpanded) TravelBlue.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable {
                                    expandedTripIds = if (isExpanded) expandedTripIds - trip.id else expandedTripIds + trip.id
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val collapseLabel = when (currentLang) {
                                    AppLanguage.ES -> "Contraer"
                                    AppLanguage.EN -> "Collapse"
                                    AppLanguage.FR -> "Réduire"
                                    AppLanguage.IT -> "Comprimi"
                                    AppLanguage.ZH -> "折叠"
                                    AppLanguage.NL -> "Inklappen"
                                    AppLanguage.DE -> "Einklappen"
                                    AppLanguage.PL -> "Zwiń"
                                    AppLanguage.PT -> "Recolher"
                                }
                                val expandAllLabel = when (currentLang) {
                                    AppLanguage.ES -> "Desplegar todas las opciones"
                                    AppLanguage.EN -> "Expand all options"
                                    AppLanguage.FR -> "Déployer toutes les options"
                                    AppLanguage.IT -> "Mostra tutte le opzioni"
                                    AppLanguage.ZH -> "展开所有选项"
                                    AppLanguage.NL -> "Alle opties uitvouwen"
                                    AppLanguage.DE -> "Alle Optionen erweitern"
                                    AppLanguage.PL -> "Rozwiń wszystkie opcje"
                                    AppLanguage.PT -> "Expandir todas as opções"
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) collapseLabel else expandAllLabel,
                                    tint = if (isExpanded) TravelBlue else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Una vez pulsado, se abre con todas las opciones
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                            // Fechas y cantidad de viajeros
                            val travelersUnit = when (currentLang) {
                                AppLanguage.ES -> "viajeros"
                                AppLanguage.EN -> "travelers"
                                AppLanguage.FR -> "voyageurs"
                                AppLanguage.IT -> "viaggiatori"
                                AppLanguage.ZH -> "位旅行者"
                                AppLanguage.NL -> "reizigers"
                                AppLanguage.DE -> "Reisende"
                                AppLanguage.PL -> "podróżnych"
                                AppLanguage.PT -> "viajantes"
                            }
                            Text(
                                text = "📅 ${FormatUtils.formatShortDate(trip.startDate, currentLang.locale)} - ${FormatUtils.formatShortDate(trip.endDate, currentLang.locale)} · 👥 ${tParts.size} $travelersUnit",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF64748B)
                            )

                            // Estado / Acción de selección como activo
                            if (!isActive) {
                                Button(
                                    onClick = { onSelectTrip(trip.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TravelBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("select_active_trip_${trip.id}")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = setAsActiveLabel,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = TravelBlue.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, TravelBlue.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = TravelBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = currentlyActiveLabel,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = TravelBlue
                                        )
                                    }
                                }
                            }

                            // Botones de acción: Editar y Eliminar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onEditTripClick(trip) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, TravelBlue),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TravelBlue),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("edit_trip_${trip.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = texts.editTripOption,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                OutlinedButton(
                                    onClick = { tripPendingDeletion = trip },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, CoralRed),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("delete_trip_${trip.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = texts.deleteBtn,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            // Métricas del viaje: Gastado, Bote Común, Presupuesto
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = texts.headerSpent,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = FormatUtils.formatCurrency(spent, trip.baseCurrency, currentLang.locale),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                            color = OceanNavy
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF0FDF4)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = texts.commonPotName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldGreenDark
                                        )
                                        Text(
                                            text = "+${FormatUtils.formatCurrency(inc, trip.baseCurrency, currentLang.locale)}",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                            color = EmeraldGreenDark
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = texts.headerBudget,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TravelBlue
                                        )
                                        Text(
                                            text = trip.budget?.let { FormatUtils.formatCurrency(it, trip.baseCurrency, currentLang.locale) }
                                                ?: texts.statusFlexible,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                            color = TravelBlue
                                        )
                                    }
                                }
                            }

                            if (trip.budget != null && trip.budget > 0.0) {
                                val progress = (spent / trip.budget).toFloat().coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = if (progress >= 0.9f) CoralRed else TravelBlue,
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            }

                            // Botones para exportar los gastos de este viaje a CSV o PDF
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val exportCsvLabel = when (currentLang) {
                                    AppLanguage.ES -> "Exportar"
                                    AppLanguage.EN -> "Export"
                                    AppLanguage.FR -> "Exporter"
                                    AppLanguage.IT -> "Esporta"
                                    AppLanguage.ZH -> "导出"
                                    AppLanguage.NL -> "Exporteren"
                                    AppLanguage.DE -> "Exportieren"
                                    AppLanguage.PL -> "Eksportuj"
                                    AppLanguage.PT -> "Exportar"
                                }
                                OutlinedButton(
                                    onClick = { onExportSingleTripJsonClick(trip) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TravelBlue),
                                    border = BorderStroke(1.dp, TravelBlue),
                                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp)
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = exportCsvLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                val exportPdfLabel = when (currentLang) {
                                    AppLanguage.ES -> "Exportar a PDF"
                                    AppLanguage.EN -> "Export to PDF"
                                    AppLanguage.FR -> "Exporter en PDF"
                                    AppLanguage.IT -> "Esporta in PDF"
                                    AppLanguage.ZH -> "导出为 PDF"
                                    AppLanguage.NL -> "Exporteer naar PDF"
                                    AppLanguage.DE -> "Als PDF exportieren"
                                    AppLanguage.PL -> "Eksportuj do PDF"
                                    AppLanguage.PT -> "Exportar para PDF"
                                }
                                OutlinedButton(
                                    onClick = { onExportSingleTripPdfClick(trip) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRedDark),
                                    border = BorderStroke(1.dp, CoralRedDark),
                                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = exportPdfLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            // Botón para contraer opciones
                            val collapseOptionsLabel = when (currentLang) {
                                AppLanguage.ES -> "Cerrar opciones"
                                AppLanguage.EN -> "Collapse options"
                                AppLanguage.FR -> "Fermer les options"
                                AppLanguage.IT -> "Chiudi opzioni"
                                AppLanguage.ZH -> "关闭选项"
                                AppLanguage.NL -> "Opties sluiten"
                                AppLanguage.DE -> "Optionen schließen"
                                AppLanguage.PL -> "Zamknij opcje"
                                AppLanguage.PT -> "Fechar opções"
                            }
                            TextButton(
                                onClick = { expandedTripIds = expandedTripIds - trip.id },
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = collapseOptionsLabel,
                                    color = Color(0xFF64748B),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // Diálogo de confirmación seguro para eliminar viaje
    if (tripPendingDeletion != null) {
        val target = tripPendingDeletion!!
        val deleteDialogTitle = when (currentLang) {
            AppLanguage.ES -> "Eliminar viaje \"${target.title}\""
            AppLanguage.EN -> "Delete trip \"${target.title}\""
            AppLanguage.FR -> "Supprimer le voyage « ${target.title} »"
            AppLanguage.IT -> "Elimina il viaggio \"${target.title}\""
            AppLanguage.ZH -> "删除行程 \"${target.title}\""
            AppLanguage.NL -> "Reis \"${target.title}\" verwijderen"
            AppLanguage.DE -> "Reise „${target.title}“ löschen"
            AppLanguage.PL -> "Usuń podróż „${target.title}”"
            AppLanguage.PT -> "Eliminar viagem \"${target.title}\""
        }
        val deleteDialogMessage = when (currentLang) {
            AppLanguage.ES -> "¿Estás seguro de que deseas eliminar este viaje y todos sus gastos, participantes y aportaciones al bote? Esta acción no se puede deshacer."
            AppLanguage.EN -> "Are you sure you want to delete this trip and all its expenses, participants and pot contributions? This action cannot be undone."
            AppLanguage.FR -> "Êtes-vous sûr de vouloir supprimer ce voyage ainsi que toutes ses dépenses, participants et contributions ? Cette action est irréversible."
            AppLanguage.IT -> "Sei sicuro di voler eliminare questo viaggio e tutte le relative spese, partecipanti e versamenti? Questa azione non può essere annullata."
            AppLanguage.ZH -> "您确定要删除此行程及其所有支出、参与者和共同基金出资吗？此操作无法撤消。"
            AppLanguage.NL -> "Weet u zeker dat u deze reis en alle bijbehorende uitgaven, deelnemers en potbijdragen wilt verwijderen? Dit kan niet ongedaan worden gemaakt."
            AppLanguage.DE -> "Sind Sie sicher, dass Sie diese Reise mit allen Ausgaben, Teilnehmern und Beiträgen löschen möchten? Dies kann nicht rückgängig gemacht werden."
            AppLanguage.PL -> "Czy na pewno chcesz usunąć tę podróż oraz wszystkie powiązane wydatki, uczestników i wpłaty? Tej operacji nie można cofnąć."
            AppLanguage.PT -> "Tem a certeza de que deseja eliminar esta viagem e todas as suas despesas, participantes e contribuições para o fundo? Esta ação não pode ser anulada."
        }
        val confirmDeleteLabel = when (currentLang) {
            AppLanguage.ES -> "Sí, eliminar viaje"
            AppLanguage.EN -> "Yes, delete trip"
            AppLanguage.FR -> "Oui, supprimer le voyage"
            AppLanguage.IT -> "Sì, elimina viaggio"
            AppLanguage.ZH -> "是的，删除行程"
            AppLanguage.NL -> "Ja, verwijder reis"
            AppLanguage.DE -> "Ja, Reise löschen"
            AppLanguage.PL -> "Tak, usuń podróż"
            AppLanguage.PT -> "Sim, eliminar viagem"
        }
        AlertDialog(
            onDismissRequest = { tripPendingDeletion = null },
            title = {
                Text(
                    text = deleteDialogTitle,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = deleteDialogMessage
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTripConfirmed(target.id)
                        tripPendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text(confirmDeleteLabel, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripPendingDeletion = null }) {
                    Text(texts.cancel)
                }
            }
        )
    }
}
