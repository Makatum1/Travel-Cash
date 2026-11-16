package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AvatarColorPalette
import com.example.data.model.CategoryCatalog
import com.example.data.model.ExpenseEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.PaymentMethodCatalog
import com.example.data.model.TripEntity
import com.example.domain.DebtCalculator
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.OceanNavy
import com.example.ui.theme.TravelBlue
import com.example.util.FormatUtils
import com.example.util.UiTexts
import java.util.Locale

@Composable
fun StatsTabScreen(
    trip: TripEntity,
    participants: List<ParticipantEntity>,
    expenses: List<ExpenseEntity>,
    onParticipantAvatarClick: (ParticipantEntity) -> Unit = {},
    onExpenseClick: (ExpenseEntity) -> Unit,
    topHeader: @Composable () -> Unit = {}
) {
    val currentLang = LocalAppLanguage.current
    val texts = UiTexts.get(currentLang)
    val participantMap = remember(participants) { participants.associateBy { it.id } }
    val totalSpent = remember(expenses) { expenses.sumOf { it.amountInBaseCurrency } }
    val tripDays = remember(trip.startDate, trip.endDate) {
        FormatUtils.calculateTripDays(trip.startDate, trip.endDate)
    }
    val avgPerDay = if (tripDays > 0) totalSpent / tripDays else 0.0
    val avgPerPerson = if (participants.isNotEmpty()) totalSpent / participants.size else 0.0

    val highestSpendingDay = remember(expenses) {
        expenses.groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.amountInBaseCurrency } }
            .maxByOrNull { it.value }
    }

    // Estado interactivo de filas desplegadas en Categorías y Personas
    var expandedCategoryName by remember { mutableStateOf<String?>(null) }
    var expandedPersonId by remember { mutableStateOf<String?>(null) }

    val byCategory = remember(expenses) {
        expenses.groupBy { it.category }
            .map { (catName, list) ->
                Triple(catName, list, list.sumOf { it.amountInBaseCurrency })
            }
            .sortedByDescending { it.third }
    }

    val byPerson = remember(participants, expenses) {
        participants.map { p ->
            val list = expenses.filter { it.paidById == p.id }
            Triple(p, list, list.sumOf { it.amountInBaseCurrency })
        }.sortedByDescending { it.third }
    }

    val byPaymentMethod = remember(expenses) {
        expenses.groupBy { it.paymentMethod }
            .map { (method, list) -> method to list.sumOf { it.amountInBaseCurrency } }
            .sortedByDescending { it.second }
    }

    val unspecifiedLabel = texts.unspecifiedLabel
    val byAddressOrZone = remember(expenses, unspecifiedLabel) {
        expenses.groupBy { it.country.trim().ifEmpty { unspecifiedLabel } }
            .map { (zone, list) -> zone to list.sumOf { it.amountInBaseCurrency } }
            .sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "stats_top_header") {
            topHeader()
        }

        // TARJETAS RESUMEN SUPERIORES (4 KPIs)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val expCountSubtitle = when (currentLang) {
                        AppLanguage.ES -> "${expenses.size} gastos registrados"
                        AppLanguage.EN -> "${expenses.size} expenses recorded"
                        AppLanguage.FR -> "${expenses.size} dépenses enregistrées"
                        AppLanguage.IT -> "${expenses.size} spese registrate"
                        AppLanguage.ZH -> "已记录 ${expenses.size} 笔支出"
                        AppLanguage.NL -> "${expenses.size} geregistreerde uitgaven"
                        AppLanguage.DE -> "${expenses.size} erfasste Ausgaben"
                        AppLanguage.PL -> "${expenses.size} zarejestrowanych wydatków"
                        AppLanguage.PT -> "${expenses.size} despesas registadas"
                    }
                    SummaryStatCard(
                        modifier = Modifier.weight(1f),
                        title = texts.totalSpentTitle,
                        value = FormatUtils.formatCurrency(totalSpent, trip.baseCurrency, currentLang.locale),
                        subtitle = expCountSubtitle,
                        accentColor = TravelBlue
                    )
                    val daysSubtitle = when (currentLang) {
                        AppLanguage.ES -> "En $tripDays días de viaje"
                        AppLanguage.EN -> "Over $tripDays trip days"
                        AppLanguage.FR -> "Sur $tripDays jours de voyage"
                        AppLanguage.IT -> "In $tripDays giorni di viaggio"
                        AppLanguage.ZH -> "在 $tripDays 天的行程中"
                        AppLanguage.NL -> "Over $tripDays reisdagen"
                        AppLanguage.DE -> "Über $tripDays Reisetage"
                        AppLanguage.PL -> "W ciągu $tripDays dni podróży"
                        AppLanguage.PT -> "Em $tripDays dias de viagem"
                    }
                    SummaryStatCard(
                        modifier = Modifier.weight(1f),
                        title = texts.avgPerDayTitle,
                        value = FormatUtils.formatCurrency(avgPerDay, trip.baseCurrency, currentLang.locale),
                        subtitle = daysSubtitle,
                        accentColor = EmeraldGreenDark
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val partCountSubtitle = when (currentLang) {
                        AppLanguage.ES -> "${participants.size} participantes"
                        AppLanguage.EN -> "${participants.size} participants"
                        AppLanguage.FR -> "${participants.size} participants"
                        AppLanguage.IT -> "${participants.size} partecipanti"
                        AppLanguage.ZH -> "${participants.size} 位参与者"
                        AppLanguage.NL -> "${participants.size} deelnemers"
                        AppLanguage.DE -> "${participants.size} Teilnehmer"
                        AppLanguage.PL -> "${participants.size} uczestników"
                        AppLanguage.PT -> "${participants.size} participantes"
                    }
                    SummaryStatCard(
                        modifier = Modifier.weight(1f),
                        title = texts.avgPerPersonTitle,
                        value = FormatUtils.formatCurrency(avgPerPerson, trip.baseCurrency, currentLang.locale),
                        subtitle = partCountSubtitle,
                        accentColor = Color(0xFF6366F1)
                    )
                    SummaryStatCard(
                        modifier = Modifier.weight(1f),
                        title = texts.highestDayTitle,
                        value = highestSpendingDay?.let {
                            FormatUtils.formatCurrency(it.value, trip.baseCurrency, currentLang.locale)
                        } ?: texts.noDataLabel,
                        subtitle = highestSpendingDay?.let {
                            FormatUtils.formatShortDate(it.key, currentLang.locale)
                        } ?: texts.noDataLabel,
                        accentColor = Color(0xFFF59E0B)
                    )
                }
            }
        }

        // SECCIÓN 1: GASTOS POR CATEGORÍA (INTERACTIVO / DESPLEGABLE)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = texts.expensesByCategoryTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = OceanNavy
                        )
                        val tapToViewBreakdown = when (currentLang) {
                            AppLanguage.ES -> "Pulsa para ver desglose"
                            AppLanguage.EN -> "Tap to view breakdown"
                            AppLanguage.FR -> "Appuyer pour voir le détail"
                            AppLanguage.IT -> "Tocca per vedere i dettagli"
                            AppLanguage.ZH -> "点击查看明细"
                            AppLanguage.NL -> "Tik om overzicht te bekijken"
                            AppLanguage.DE -> "Tippen für Aufschlüsselung"
                            AppLanguage.PL -> "Dotknij, aby zobaczyć zestawienie"
                            AppLanguage.PT -> "Tocar para ver detalhes"
                        }
                        Text(
                            text = tapToViewBreakdown,
                            style = MaterialTheme.typography.labelSmall,
                            color = TravelBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (byCategory.isEmpty()) {
                        Text(
                            text = texts.noExpensesYetStats,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        byCategory.forEachIndexed { idx, (catName, catExpenses, catSum) ->
                            val catInfo = CategoryCatalog.getCategory(catName)
                            val displayCatName = CategoryCatalog.displayName(catName, currentLang)
                            val pct = if (totalSpent > 0) (catSum / totalSpent) * 100.0 else 0.0
                            val isExpanded = expandedCategoryName == catName

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        expandedCategoryName = if (isExpanded) null else catName
                                    }
                                    .padding(vertical = 6.dp)
                                    .testTag("stats_category_row_$idx")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(catInfo.color.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = catInfo.icon,
                                            contentDescription = null,
                                            tint = catInfo.color,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = displayCatName,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = OceanNavy
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = FormatUtils.formatCurrency(catSum, trip.baseCurrency, currentLang.locale),
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                                    color = OceanNavy
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = when (currentLang) {
                                                        AppLanguage.ES -> "Desplegar categoría"
                                                        AppLanguage.EN -> "Expand category"
                                                        AppLanguage.FR -> "Développer la catégorie"
                                                        AppLanguage.IT -> "Espandi categoria"
                                                        AppLanguage.ZH -> "展开类别"
                                                        AppLanguage.NL -> "Categorie uitvouwen"
                                                        AppLanguage.DE -> "Kategorie erweitern"
                                                        AppLanguage.PL -> "Rozwiń kategorię"
                                                        AppLanguage.PT -> "Expandir categoria"
                                                    },
                                                    tint = Color(0xFF64748B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape),
                                            color = catInfo.color,
                                            trackColor = Color(0xFFE2E8F0)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        val pctText = when (currentLang) {
                                            AppLanguage.ES -> "${catExpenses.size} gastos · ${String.format(Locale.US, "%.1f", pct)}% del total"
                                            AppLanguage.EN -> "${catExpenses.size} expenses · ${String.format(Locale.US, "%.1f", pct)}% of total"
                                            AppLanguage.FR -> "${catExpenses.size} dépenses · ${String.format(Locale.US, "%.1f", pct)}% du total"
                                            AppLanguage.IT -> "${catExpenses.size} spese · ${String.format(Locale.US, "%.1f", pct)}% del totale"
                                            AppLanguage.ZH -> "${catExpenses.size} 笔支出 · 占总额 ${String.format(Locale.US, "%.1f", pct)}%"
                                            AppLanguage.NL -> "${catExpenses.size} uitgaven · ${String.format(Locale.US, "%.1f", pct)}% van totaal"
                                            AppLanguage.DE -> "${catExpenses.size} Ausgaben · ${String.format(Locale.US, "%.1f", pct)}% der Gesamtsumme"
                                            AppLanguage.PL -> "${catExpenses.size} wydatków · ${String.format(Locale.US, "%.1f", pct)}% całości"
                                            AppLanguage.PT -> "${catExpenses.size} despesas · ${String.format(Locale.US, "%.1f", pct)}% do total"
                                        }
                                        Text(
                                            text = pctText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                // Desglose detallado desplegable con todos los gastos individuales de la categoría
                                AnimatedVisibility(visible = isExpanded) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, catInfo.color.copy(alpha = 0.35f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            catExpenses.sortedByDescending { it.date }.forEach { exp ->
                                                val payer = if (exp.paidById == DebtCalculator.BOTE_COMUN_ID) {
                                                    texts.commonPotName
                                                } else {
                                                    participantMap[exp.paidById]?.name ?: texts.defaultTravelerName
                                                }
                                                val paidLabel = when (currentLang) {
                                                    AppLanguage.ES -> "Pagó"
                                                    AppLanguage.EN -> "Paid by"
                                                    AppLanguage.FR -> "Payé par"
                                                    AppLanguage.IT -> "Pagato da"
                                                    AppLanguage.ZH -> "付款人"
                                                    AppLanguage.NL -> "Betaald door"
                                                    AppLanguage.DE -> "Bezahlt von"
                                                    AppLanguage.PL -> "Zapłacone przez"
                                                    AppLanguage.PT -> "Pago por"
                                                }
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { onExpenseClick(exp) }
                                                        .padding(vertical = 3.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = exp.placeName.ifBlank { exp.notes.ifBlank { displayCatName } },
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                            color = OceanNavy,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${FormatUtils.formatShortDate(exp.date, currentLang.locale)} · $paidLabel: $payer",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                    Text(
                                                        text = FormatUtils.formatCurrency(exp.amountInBaseCurrency, trip.baseCurrency, currentLang.locale),
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = catInfo.color
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (idx < byCategory.lastIndex) {
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }
        }

        // SECCIÓN 2: GASTOS POR PERSONA (INTERACTIVO / DESPLEGABLE)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = texts.expensesByParticipantTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = OceanNavy
                        )
                        val tapToViewPayments = when (currentLang) {
                            AppLanguage.ES -> "Pulsa para ver pagos"
                            AppLanguage.EN -> "Tap to view payments"
                            AppLanguage.FR -> "Appuyer pour voir les paiements"
                            AppLanguage.IT -> "Tocca per vedere i pagamenti"
                            AppLanguage.ZH -> "点击查看付款"
                            AppLanguage.NL -> "Tik om betalingen te bekijken"
                            AppLanguage.DE -> "Tippen für Zahlungen"
                            AppLanguage.PL -> "Dotknij, aby zobaczyć płatności"
                            AppLanguage.PT -> "Tocar para ver pagamentos"
                        }
                        Text(
                            text = tapToViewPayments,
                            style = MaterialTheme.typography.labelSmall,
                            color = TravelBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    byPerson.forEachIndexed { idx, (person, personExpenses, personSum) ->
                        val avatarColor = AvatarColorPalette.parseHexColor(person.avatarColor)
                        val pct = if (totalSpent > 0) (personSum / totalSpent) * 100.0 else 0.0
                        val isExpanded = expandedPersonId == person.id

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    expandedPersonId = if (isExpanded) null else person.id
                                }
                                .padding(vertical = 6.dp)
                                .testTag("stats_person_row_${person.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.example.ui.components.EditableParticipantAvatar(
                                    name = person.name,
                                    avatarColorHex = person.avatarColor,
                                    photoUri = person.photoUri,
                                    onClick = { onParticipantAvatarClick(person) },
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = person.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = OceanNavy
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = FormatUtils.formatCurrency(personSum, trip.baseCurrency, currentLang.locale),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                                color = OceanNavy
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = when (currentLang) {
                                                    AppLanguage.ES -> "Desplegar persona"
                                                    AppLanguage.EN -> "Expand person"
                                                    AppLanguage.FR -> "Développer la personne"
                                                    AppLanguage.IT -> "Espandi persona"
                                                    AppLanguage.ZH -> "展开参与人"
                                                    AppLanguage.NL -> "Persoon uitvouwen"
                                                    AppLanguage.DE -> "Person erweitern"
                                                    AppLanguage.PL -> "Rozwiń osobę"
                                                    AppLanguage.PT -> "Expandir pessoa"
                                                },
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = avatarColor,
                                        trackColor = Color(0xFFE2E8F0)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    val paymentsMadeText = when (currentLang) {
                                        AppLanguage.ES -> "${personExpenses.size} pagos realizados · ${String.format(Locale.US, "%.1f", pct)}% del total"
                                        AppLanguage.EN -> "${personExpenses.size} payments made · ${String.format(Locale.US, "%.1f", pct)}% of total"
                                        AppLanguage.FR -> "${personExpenses.size} paiements effectués · ${String.format(Locale.US, "%.1f", pct)}% du total"
                                        AppLanguage.IT -> "${personExpenses.size} pagamenti effettuati · ${String.format(Locale.US, "%.1f", pct)}% del totale"
                                        AppLanguage.ZH -> "${personExpenses.size} 次付款 · 占总额 ${String.format(Locale.US, "%.1f", pct)}%"
                                        AppLanguage.NL -> "${personExpenses.size} betalingen gedaan · ${String.format(Locale.US, "%.1f", pct)}% van totaal"
                                        AppLanguage.DE -> "${personExpenses.size} Zahlungen getätigt · ${String.format(Locale.US, "%.1f", pct)}% der Gesamtsumme"
                                        AppLanguage.PL -> "${personExpenses.size} dokonanych płatności · ${String.format(Locale.US, "%.1f", pct)}% całości"
                                        AppLanguage.PT -> "${personExpenses.size} pagamentos efetuados · ${String.format(Locale.US, "%.1f", pct)}% do total"
                                    }
                                    Text(
                                        text = paymentsMadeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // Listado detallado desplegable de todos los gastos pagados por esta persona
                            AnimatedVisibility(visible = isExpanded) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, avatarColor.copy(alpha = 0.35f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (personExpenses.isEmpty()) {
                                            val noExpensesPerson = when (currentLang) {
                                                AppLanguage.ES -> "${person.name} no ha realizado pagos de su bolsillo aún."
                                                AppLanguage.EN -> "${person.name} has not paid any out-of-pocket expenses yet."
                                                AppLanguage.FR -> "${person.name} n'a pas encore payé de dépenses de sa poche."
                                                AppLanguage.IT -> "${person.name} non ha ancora sostenuto spese di tasca propria."
                                                AppLanguage.ZH -> "${person.name} 尚未自掏腰包支付任何费用。"
                                                AppLanguage.NL -> "${person.name} heeft nog geen uitgaven uit eigen zak betaald."
                                                AppLanguage.DE -> "${person.name} hat noch keine Ausgaben aus eigener Tasche bezahlt."
                                                AppLanguage.PL -> "${person.name} nie poniósł jeszcze żadnych wydatków z własnej kieszeni."
                                                AppLanguage.PT -> "${person.name} ainda não realizou pagamentos do seu próprio bolso."
                                            }
                                            Text(
                                                text = noExpensesPerson,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF64748B)
                                            )
                                        } else {
                                            personExpenses.sortedByDescending { it.date }.forEach { exp ->
                                                val displayCat = CategoryCatalog.displayName(exp.category, currentLang)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { onExpenseClick(exp) }
                                                        .padding(vertical = 3.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = exp.placeName.ifBlank { exp.notes.ifBlank { displayCat } },
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                            color = OceanNavy,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "$displayCat · ${FormatUtils.formatShortDate(exp.date, currentLang.locale)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                    Text(
                                                        text = FormatUtils.formatCurrency(exp.amountInBaseCurrency, trip.baseCurrency, currentLang.locale),
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = avatarColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (idx < byPerson.lastIndex) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }

        // SECCIÓN 3: GASTOS POR MÉTODO DE PAGO Y POR DIRECCIÓN / ZONA
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Por Método de Pago
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = texts.expensesByMethodTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = OceanNavy
                        )
                        byPaymentMethod.forEach { (method, sum) ->
                            val pct = if (totalSpent > 0) (sum / totalSpent) * 100.0 else 0.0
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = PaymentMethodCatalog.displayName(method, currentLang),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = OceanNavy
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.0f", pct)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TravelBlue
                                    )
                                }
                                Text(
                                    text = FormatUtils.formatCurrency(sum, trip.baseCurrency, currentLang.locale),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                // Por Dirección / Zona
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = texts.expensesByZoneTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = OceanNavy
                        )
                        byAddressOrZone.forEach { (zoneName, sum) ->
                            val pct = if (totalSpent > 0) (sum / totalSpent) * 100.0 else 0.0
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "📍 $zoneName",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = OceanNavy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.0f", pct)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldGreenDark
                                    )
                                }
                                Text(
                                    text = FormatUtils.formatCurrency(sum, trip.baseCurrency, currentLang.locale),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
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
}

@Composable
private fun SummaryStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                ),
                color = OceanNavy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
