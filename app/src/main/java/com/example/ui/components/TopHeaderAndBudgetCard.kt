package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TripEntity
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.CoralRed
import com.example.ui.theme.CoralRedDark
import com.example.ui.theme.CoralRedLight
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.isEnglish
import com.example.ui.theme.OceanNavy
import com.example.ui.theme.OceanSlate
import com.example.ui.theme.TravelBlue
import com.example.util.CurrencyCatalog
import com.example.util.FormatUtils

@Composable
fun TopFeedbackToast(
    message: String?,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        if (message != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onDismiss() },
                shape = RoundedCornerShape(14.dp),
                color = if (isError) CoralRedDark else Color(0xFF065F46),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = message,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

/**
 * Barra Superior (Top Header):
 * - Fila 1: Icono de la app (AppHeaderLogoIcon) + título "Travel Cash" + subtítulo traducido según idioma,
 *   y a la derecha el botón de Sugerencias (el Backup se encuentra en Ajustes).
 * - Fila 2: Selector desplegable prominente de Viaje Activo que muestra el nombre del viaje y símbolo de su moneda base,
 *   e incluye opciones directas para crear nuevo viaje e importar viaje desde CSV.
 */
@Composable
fun TravelCashTopBar(
    trips: List<TripEntity>,
    activeTrip: TripEntity?,
    onSelectTrip: (String) -> Unit,
    onCreateNewTripClick: () -> Unit,
    onImportNewTripCsvClick: () -> Unit,
    onSuggestionsClick: () -> Unit
) {
    val currentLang = LocalAppLanguage.current
    var tripMenuExpanded by remember { mutableStateOf(false) }

    val appSubtitle = when (currentLang) {
        AppLanguage.ES -> "Gestor de gastos de viajes"
        AppLanguage.EN -> "Travel expense manager"
        AppLanguage.FR -> "Gestionnaire de dépenses de voyage"
        AppLanguage.IT -> "Gestione spese di viaggio"
        AppLanguage.ZH -> "旅行记账与费用分摊"
        AppLanguage.NL -> "Reiskostenbeheer"
        AppLanguage.DE -> "Reisekostenverwaltung"
        AppLanguage.PL -> "Menedżer wydatków w podróży"
        AppLanguage.PT -> "Gestor de despesas de viagem"
    }

    val selectOrCreateTripText = when (currentLang) {
        AppLanguage.ES -> "Seleccionar o crear un viaje..."
        AppLanguage.EN -> "Select or create a trip..."
        AppLanguage.FR -> "Sélectionner ou créer un voyage..."
        AppLanguage.IT -> "Seleziona o crea un viaggio..."
        AppLanguage.ZH -> "选择或创建行程..."
        AppLanguage.NL -> "Selecteer of maak een reis..."
        AppLanguage.DE -> "Reise auswählen oder erstellen..."
        AppLanguage.PL -> "Wybierz lub utwórz podróż..."
        AppLanguage.PT -> "Selecionar ou criar uma viagem..."
    }

    val switchActiveTripDesc = when (currentLang) {
        AppLanguage.ES -> "Cambiar viaje activo"
        AppLanguage.EN -> "Switch active trip"
        AppLanguage.FR -> "Changer de voyage actif"
        AppLanguage.IT -> "Cambia viaggio attivo"
        AppLanguage.ZH -> "切换当前行程"
        AppLanguage.NL -> "Actieve reis wisselen"
        AppLanguage.DE -> "Aktive Reise wechseln"
        AppLanguage.PL -> "Zmień aktywną podróż"
        AppLanguage.PT -> "Mudar viagem ativa"
    }

    val activeBadgeText = when (currentLang) {
        AppLanguage.ES -> "ACTIVO"
        AppLanguage.EN -> "ACTIVE"
        AppLanguage.FR -> "ACTIF"
        AppLanguage.IT -> "ATTIVO"
        AppLanguage.ZH -> "激活"
        AppLanguage.NL -> "ACTIEF"
        AppLanguage.DE -> "AKTIV"
        AppLanguage.PL -> "AKTYWNA"
        AppLanguage.PT -> "ATIVO"
    }

    val createNewTripText = when (currentLang) {
        AppLanguage.ES -> "Crear nuevo viaje"
        AppLanguage.EN -> "Create new trip"
        AppLanguage.FR -> "Créer un nouveau voyage"
        AppLanguage.IT -> "Crea nuovo viaggio"
        AppLanguage.ZH -> "创建新行程"
        AppLanguage.NL -> "Nieuwe reis aanmaken"
        AppLanguage.DE -> "Neue Reise erstellen"
        AppLanguage.PL -> "Utwórz nową podróż"
        AppLanguage.PT -> "Criar nova viagem"
    }

    Surface(
        color = OceanNavy,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(OceanNavy, OceanSlate)
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // FILA 1: Logo + Travel Cash + Subtítulo | Botón Sugerencias
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AppHeaderLogoIcon(size = 40.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Travel Cash",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = appSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Surface(
                    onClick = onSuggestionsClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF334155),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier.testTag("header_suggestions_button")
                ) {
                    val helpButtonLabel = when (currentLang) {
                        AppLanguage.ES -> "Ayuda"
                        AppLanguage.EN -> "Help"
                        AppLanguage.FR -> "Aide"
                        AppLanguage.IT -> "Aiuto"
                        AppLanguage.ZH -> "帮助"
                        AppLanguage.NL -> "Help"
                        AppLanguage.DE -> "Hilfe"
                        AppLanguage.PL -> "Pomoc"
                        AppLanguage.PT -> "Ajuda"
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = helpButtonLabel,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = helpButtonLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // FILA 2: Selector desplegable prominente de Viaje Activo
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { tripMenuExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_trip_selector")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "✈️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            val tripLabel = if (activeTrip != null) {
                                "${activeTrip.title} (${CurrencyCatalog.getSymbol(activeTrip.baseCurrency)})"
                            } else {
                                selectOrCreateTripText
                            }
                            Text(
                                text = tripLabel,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = switchActiveTripDesc,
                            tint = Color(0xFF93C5FD)
                        )
                    }
                }

                DropdownMenu(
                    expanded = tripMenuExpanded,
                    onDismissRequest = { tripMenuExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .background(Color.White)
                ) {
                    trips.forEach { trip ->
                        val isSelected = trip.id == activeTrip?.id
                        val sym = CurrencyCatalog.getSymbol(trip.baseCurrency)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "✈️ ${trip.title} ($sym)",
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) TravelBlue else OceanNavy
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = activeBadgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TravelBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            onClick = {
                                tripMenuExpanded = false
                                onSelectTrip(trip.id)
                            }
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Color(0xFFE2E8F0)
                    )
                    DropdownMenuItem(
                        text = {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.5.dp, TravelBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(TravelBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = createNewTripText,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = TravelBlue
                                    )
                                }
                            }
                        },
                        onClick = {
                            tripMenuExpanded = false
                            onCreateNewTripClick()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Encabezado del Viaje Activo y Tarjeta de Presupuesto
 */
@Composable
fun ActiveTripHeaderAndBudgetCard(
    trip: TripEntity,
    totalSpent: Double,
    totalIncome: Double,
    extraBalance: Double,
    onTripIconClick: () -> Unit,
    onExportCsvClick: () -> Unit = {},
    onExportPdfClick: () -> Unit = {},
    onImportCsvToTripClick: () -> Unit = {},
    onAddBoteIncomeClick: () -> Unit,
    isSimplified: Boolean = false
) {
    val currentLang = LocalAppLanguage.current

    val statusUpcomingText = when (currentLang) {
        AppLanguage.ES -> "⏳ Por iniciar"
        AppLanguage.EN -> "⏳ Upcoming"
        AppLanguage.FR -> "⏳ À venir"
        AppLanguage.IT -> "⏳ In arrivo"
        AppLanguage.ZH -> "⏳ 待出行"
        AppLanguage.NL -> "⏳ Binnenkort"
        AppLanguage.DE -> "⏳ Bevorstehend"
        AppLanguage.PL -> "⏳ Nadchodząca"
        AppLanguage.PT -> "⏳ Por iniciar"
    }
    val statusCompletedText = when (currentLang) {
        AppLanguage.ES -> "🏁 Finalizado"
        AppLanguage.EN -> "🏁 Completed"
        AppLanguage.FR -> "🏁 Terminé"
        AppLanguage.IT -> "🏁 Concluso"
        AppLanguage.ZH -> "🏁 已结束"
        AppLanguage.NL -> "🏁 Voltooid"
        AppLanguage.DE -> "🏁 Abgeschlossen"
        AppLanguage.PL -> "🏁 Zakończona"
        AppLanguage.PT -> "🏁 Concluída"
    }
    val statusOngoingText = when (currentLang) {
        AppLanguage.ES -> "🟢 En curso"
        AppLanguage.EN -> "🟢 Ongoing"
        AppLanguage.FR -> "🟢 En cours"
        AppLanguage.IT -> "🟢 In corso"
        AppLanguage.ZH -> "🟢 进行中"
        AppLanguage.NL -> "🟢 Bezig"
        AppLanguage.DE -> "🟢 Laufend"
        AppLanguage.PL -> "🟢 W trakcie"
        AppLanguage.PT -> "🟢 Em curso"
    }
    val potIncomeLabel = when (currentLang) {
        AppLanguage.ES -> "+ Ingreso (Bote)"
        AppLanguage.EN -> "+ Pot Income"
        AppLanguage.FR -> "+ Cagnotte"
        AppLanguage.IT -> "+ Cassa"
        AppLanguage.ZH -> "+ 公账存入"
        AppLanguage.NL -> "+ Inleg pot"
        AppLanguage.DE -> "+ Kassenbeitrag"
        AppLanguage.PL -> "+ Wpłata do kasy"
        AppLanguage.PT -> "+ Depósito (Fundo)"
    }
    val extraBalanceTitle = when (currentLang) {
        AppLanguage.ES -> "SALDO EXTRA"
        AppLanguage.EN -> "EXTRA BALANCE"
        AppLanguage.FR -> "SOLDE RESTANT"
        AppLanguage.IT -> "SALDO EXTRA"
        AppLanguage.ZH -> "公账余额"
        AppLanguage.NL -> "EXTRA SALDO"
        AppLanguage.DE -> "EXTRAGUTHABEN"
        AppLanguage.PL -> "DODATKOWE SALDO"
        AppLanguage.PT -> "SALDO EXTRA"
    }
    val spentTitle = when (currentLang) {
        AppLanguage.ES -> "Gastado"
        AppLanguage.EN -> "Spent"
        AppLanguage.FR -> "Dépensé"
        AppLanguage.IT -> "Speso"
        AppLanguage.ZH -> "已支出"
        AppLanguage.NL -> "Uitgegeven"
        AppLanguage.DE -> "Ausgegeben"
        AppLanguage.PL -> "Wydano"
        AppLanguage.PT -> "Gasto"
    }
    val incomeTitle = when (currentLang) {
        AppLanguage.ES -> "Ingresos"
        AppLanguage.EN -> "Incomes"
        AppLanguage.FR -> "Revenus"
        AppLanguage.IT -> "Entrate"
        AppLanguage.ZH -> "已存入"
        AppLanguage.NL -> "Inkomsten"
        AppLanguage.DE -> "Einnahmen"
        AppLanguage.PL -> "Wpływy"
        AppLanguage.PT -> "Receitas"
    }
    val flexibleBudgetTitle = when (currentLang) {
        AppLanguage.ES -> "Sin límite fijado / Viaje con presupuesto flexible"
        AppLanguage.EN -> "No fixed limit / Trip with flexible budget"
        AppLanguage.FR -> "Sans limite fixe / Voyage avec budget flexible"
        AppLanguage.IT -> "Senza limite fisso / Viaggio con budget flessibile"
        AppLanguage.ZH -> "无固定预算上限 / 灵活预算行程"
        AppLanguage.NL -> "Geen vaste limiet / Reis met flexibel budget"
        AppLanguage.DE -> "Kein festes Limit / Reise mit flexiblem Budget"
        AppLanguage.PL -> "Brak limitu / Podróż z elastycznym budżetem"
        AppLanguage.PT -> "Sem limite fixado / Viagem com orçamento flexível"
    }

    val today = remember { java.time.LocalDate.now() }
    val startLocal = remember(trip.startDate) {
        runCatching { java.time.LocalDate.parse(trip.startDate.take(10)) }.getOrNull()
    }
    val endLocal = remember(trip.endDate) {
        runCatching { java.time.LocalDate.parse(trip.endDate.take(10)) }.getOrNull()
    }
    val (statusText, statusBgColor, statusContentColor, statusBorderColor) = when {
        startLocal != null && today.isBefore(startLocal) -> {
            Quadruple(
                statusUpcomingText,
                Color(0xFFFEF3C7),
                Color(0xFFB45309),
                Color(0xFFF59E0B)
            )
        }
        endLocal != null && today.isAfter(endLocal) -> {
            Quadruple(
                statusCompletedText,
                Color(0xFFF1F5F9),
                Color(0xFF475569),
                Color(0xFFCBD5E1)
            )
        }
        else -> {
            Quadruple(
                statusOngoingText,
                EmeraldGreenLight,
                EmeraldGreenDark,
                EmeraldGreen
            )
        }
    }

    val isPositive = extraBalance >= -0.004
    val extraBg = if (isPositive) EmeraldGreenLight else CoralRedLight
    val extraBorder = if (isPositive) EmeraldGreen else CoralRed
    val extraText = if (isPositive) Color(0xFF065F46) else Color(0xFF991B1B)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = if (isSimplified) 6.dp else 7.dp)
        ) {
            // Fila superior: Icono de ruta + título del viaje activo + fechas + estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditableTripRouteIcon(
                    photoUri = trip.headerPhotoUri,
                    onClick = onTripIconClick,
                    size = if (isSimplified) 36.dp else 38.dp
                )
                Spacer(modifier = Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = trip.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (isSimplified) 16.sp else 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEFF6FF)
                        ) {
                            Text(
                                text = "${trip.baseCurrency} (${CurrencyCatalog.getSymbol(trip.baseCurrency)})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TravelBlue,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Fechas y Estado juntos en la misma fila para ocupar menos espacio
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📅 ${FormatUtils.formatShortDate(trip.startDate, currentLang.locale)} - ${FormatUtils.formatShortDate(trip.endDate, currentLang.locale)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = CircleShape,
                            color = statusBgColor,
                            border = BorderStroke(1.dp, statusBorderColor),
                            modifier = Modifier.testTag("pill_trip_date_status")
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                ),
                                color = statusContentColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (isSimplified) {
                // =========================================================================
                // MODO SIMPLIFICADO (SOLO PESTAÑA GASTOS):
                // Quita botón PDF, barra de presupuesto, gastado e ingresos.
                // Deja únicamente: + Ingreso (Bote) y SALDO EXTRA.
                // =========================================================================
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón + Ingreso (Bote)
                    Surface(
                        onClick = onAddBoteIncomeClick,
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldGreen,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pill_add_bote_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = potIncomeLabel,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = potIncomeLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }

                    // Tarjeta SALDO EXTRA
                    Surface(
                        modifier = Modifier
                            .weight(1.15f)
                            .testTag("extra_balance_card"),
                        shape = RoundedCornerShape(10.dp),
                        color = extraBg,
                        border = BorderStroke(1.2.dp, extraBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = extraBalanceTitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                color = extraText
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = FormatUtils.formatSignedCurrency(extraBalance, trip.baseCurrency, currentLang.locale),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                ),
                                color = extraText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                // =========================================================================
                // MODO COMPLETO (OTRAS PESTAÑAS):
                // =========================================================================
                Spacer(modifier = Modifier.height(5.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillActionButton(
                        label = potIncomeLabel,
                        icon = Icons.Default.Savings,
                        bgColor = EmeraldGreen,
                        contentColor = Color.White,
                        onClick = onAddBoteIncomeClick,
                        testTag = "pill_add_bote_button"
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                val hasFixedBudget = trip.budget != null && trip.budget > 0.0
                if (hasFixedBudget) {
                    val budgetVal = trip.budget ?: 1.0
                    val ratio = (totalSpent / budgetVal).toFloat().coerceIn(0f, 1f)
                    val isOver90 = (totalSpent / budgetVal) >= 0.90
                    val barColor = if (isOver90) CoralRed else TravelBlue

                    val budgetPrefix = when (currentLang) {
                        AppLanguage.ES -> "Presupuesto: "
                        AppLanguage.EN -> "Budget: "
                        AppLanguage.FR -> "Budget : "
                        AppLanguage.IT -> "Budget: "
                        AppLanguage.ZH -> "预算："
                        AppLanguage.NL -> "Budget: "
                        AppLanguage.DE -> "Budget: "
                        AppLanguage.PL -> "Budżet: "
                        AppLanguage.PT -> "Orçamento: "
                    }
                    val usedSuffix = when (currentLang) {
                        AppLanguage.ES -> "% usado"
                        AppLanguage.EN -> "% used"
                        AppLanguage.FR -> " % utilisé"
                        AppLanguage.IT -> "% utilizzato"
                        AppLanguage.ZH -> "% 已使用"
                        AppLanguage.NL -> "% verbruikt"
                        AppLanguage.DE -> "% verbraucht"
                        AppLanguage.PL -> "% wykorzystano"
                        AppLanguage.PT -> "% utilizado"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$budgetPrefix${FormatUtils.formatCurrency(budgetVal, trip.baseCurrency, currentLang.locale)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${((totalSpent / budgetVal) * 100).toInt()}$usedSuffix",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = barColor
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape),
                        color = barColor,
                        trackColor = Color(0xFFE2E8F0)
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = flexibleBudgetTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Caja 1: Gastado
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = spentTitle,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = FormatUtils.formatCurrency(totalSpent, trip.baseCurrency, currentLang.locale),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                                color = OceanNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Caja 2: Ingresos (Bote)
                    Surface(
                        modifier = Modifier.weight(1.05f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = incomeTitle,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = EmeraldGreenDark
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "+${FormatUtils.formatCurrency(totalIncome, trip.baseCurrency, currentLang.locale)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                                color = EmeraldGreenDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Caja 3: SALDO EXTRA
                    Surface(
                        modifier = Modifier
                            .weight(1.25f)
                            .testTag("extra_balance_card"),
                        shape = RoundedCornerShape(12.dp),
                        color = extraBg,
                        border = BorderStroke(1.5.dp, extraBorder),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = extraBalanceTitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.5.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                color = extraText
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = FormatUtils.formatSignedCurrency(extraBalance, trip.baseCurrency, currentLang.locale),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                ),
                                color = extraText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PillActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

