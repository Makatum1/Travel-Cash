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
import com.example.ui.theme.CoralRed
import com.example.ui.theme.CoralRedDark
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.isEnglish
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
    val isEn = LocalAppLanguage.current.isEnglish()
    val today = remember { LocalDate.now() }
    var tripPendingDeletion by remember { mutableStateOf<TripEntity?>(null) }
    var expandedTripIds by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "trips_top_header") {
            topHeader()
        }

        // Barra de acciones superiores en Pestaña Viajes
        item {
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
                    Text(
                        text = if (isEn) "Trip Management" else "Gestión de Viajes",
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
                            text = if (isEn) "New Trip" else "Nuevo Viaje",
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
                                text = if (isEn) "Add Test Trip" else "Añadir viaje de prueba",
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
                                text = if (isEn) "Import Trip" else "Importar Viaje",
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
            item {
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
                            text = if (isEn) "No trips yet" else "No tienes ningún viaje todavía",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = OceanNavy
                        )

                        Text(
                            text = if (isEn)
                                "Create your first trip to start managing expenses, participants and routes."
                            else
                                "Crea tu primer viaje para empezar a registrar y organizar todos tus gastos.",
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
            val tParts = allParticipants.filter { it.tripId == trip.id }
            val tExps = allExpenses.filter { it.tripId == trip.id }
            val tIncs = allIncomes.filter { it.tripId == trip.id }
            val spent = tExps.sumOf { it.amountInBaseCurrency }
            val inc = tIncs.sumOf { it.amount }

            val startLocal = runCatching { LocalDate.parse(trip.startDate.take(10)) }.getOrNull()
            val endLocal = runCatching { LocalDate.parse(trip.endDate.take(10)) }.getOrNull()
            val (dateStatusLabel, dateStatusBg, dateStatusColor) = when {
                startLocal != null && today.isBefore(startLocal) -> Triple(
                    if (isEn) "⏳ Upcoming" else "⏳ Por iniciar",
                    Color(0xFFFEF3C7),
                    Color(0xFFB45309)
                )
                endLocal != null && today.isAfter(endLocal) -> Triple(
                    if (isEn) "🏁 Completed" else "🏁 Finalizado",
                    Color(0xFFF1F5F9),
                    Color(0xFF475569)
                )
                else -> Triple(
                    if (isEn) "🟢 Ongoing" else "🟢 En curso",
                    EmeraldGreenLight,
                    EmeraldGreenDark
                )
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
                                            text = if (isEn) "ACTIVE" else "ACTIVO",
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
                        Surface(
                            shape = CircleShape,
                            color = if (isExpanded) TravelBlue.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) {
                                        if (isEn) "Collapse" else "Contraer"
                                    } else {
                                        if (isEn) "Expand all options" else "Desplegar todas las opciones"
                                    },
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
                            Text(
                                text = "📅 ${FormatUtils.formatShortDate(trip.startDate)} - ${FormatUtils.formatShortDate(trip.endDate)} · 👥 ${tParts.size} ${if (isEn) "travelers" else "viajeros"}",
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
                                        text = if (isEn) "Set as Active Trip" else "Establecer como viaje activo",
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
                                            text = if (isEn) "Currently active trip in the app" else "Viaje actualmente activo en la app",
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
                                        text = if (isEn) "Edit Trip" else "Editar Viaje",
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
                                        text = if (isEn) "Delete" else "Eliminar",
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
                                            text = if (isEn) "Spent" else "Gastado",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = FormatUtils.formatCurrency(spent, trip.baseCurrency),
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
                                            text = if (isEn) "Common Pot" else "Bote Común",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldGreenDark
                                        )
                                        Text(
                                            text = "+${FormatUtils.formatCurrency(inc, trip.baseCurrency)}",
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
                                            text = if (isEn) "Budget" else "Presupuesto",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TravelBlue
                                        )
                                        Text(
                                            text = trip.budget?.let { FormatUtils.formatCurrency(it, trip.baseCurrency) }
                                                ?: if (isEn) "Flexible" else "Flexible",
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
                                        text = if (isEn) "Export" else "Exportar",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
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
                                        text = if (isEn) "Export to PDF" else "Exportar a PDF",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            // Botón para contraer opciones
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
                                    text = if (isEn) "Collapse options" else "Cerrar opciones",
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
        AlertDialog(
            onDismissRequest = { tripPendingDeletion = null },
            title = {
                Text(
                    text = if (isEn) "Delete trip \"${target.title}\"" else "Eliminar viaje \"${target.title}\"",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = if (isEn) {
                        "Are you sure you want to delete this trip and all its expenses, participants and pot contributions? This action cannot be undone."
                    } else {
                        "¿Estás seguro de que deseas eliminar este viaje y todos sus gastos, participantes y aportaciones al bote? Esta acción no se puede deshacer."
                    }
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
                    Text(if (isEn) "Yes, delete trip" else "Sí, eliminar viaje", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripPendingDeletion = null }) {
                    Text(if (isEn) "Cancel" else "Cancelar")
                }
            }
        )
    }
}
