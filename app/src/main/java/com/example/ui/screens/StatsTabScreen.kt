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
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.PaymentMethodCatalog
import com.example.data.model.TripEntity
import com.example.domain.DebtCalculator
import com.example.domain.ParticipantLedgerEntryType
import com.example.domain.ParticipantNetSpendCalculator
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.OceanNavy
import com.example.ui.theme.TravelBlue
import com.example.util.FormatUtils
import com.example.util.UiTexts
import java.util.Locale
import kotlin.math.abs

@Composable
fun StatsTabScreen(
    trip: TripEntity,
    participants: List<ParticipantEntity>,
    expenses: List<ExpenseEntity>,
    incomes: List<IncomeEntity> = emptyList(),
    onParticipantAvatarClick: (ParticipantEntity) -> Unit = {},
    onExpenseClick: (ExpenseEntity) -> Unit,
    topHeader: @Composable () -> Unit = {}
) {
    val currentLang = LocalAppLanguage.current
    val texts = UiTexts.get(currentLang)
    val participantMap = remember(participants) { participants.associateBy { it.id } }

    // Informe completo de gasto neto por participante (excluye liquidaciones y movimientos de bote del gasto real)
    val netSpendReport = remember(participants, expenses, incomes) {
        ParticipantNetSpendCalculator.calculate(
            participants = participants,
            expenses = expenses,
            incomes = incomes
        )
    }
    val realExpenses = remember(expenses) {
        expenses.filter { !CategoryCatalog.isDebtSettlementExpense(it) }
    }
    val totalSpent = netSpendReport.realTotalSpent
    val tripDays = remember(trip.startDate, trip.endDate) {
        FormatUtils.calculateTripDays(trip.startDate, trip.endDate)
    }
    val avgPerDay = if (tripDays > 0) totalSpent / tripDays else 0.0
    val totalPerPerson = if (participants.isNotEmpty()) totalSpent / participants.size else 0.0
    val avgPerDayPerPerson = if (participants.isNotEmpty() && tripDays > 0) avgPerDay / participants.size else 0.0

    val byPersonAndCategory = remember(participants, expenses) {
        ParticipantNetSpendCalculator.calculateByParticipantAndCategory(
            participants = participants,
            expenses = expenses
        )
    }

    // Estado interactivo de filas desplegadas en Categorías, Personas y Persona × Categoría
    var expandedCategoryName by remember { mutableStateOf<String?>(null) }
    var expandedPersonId by remember { mutableStateOf<String?>(null) }
    var expandedPersonCategoryId by remember { mutableStateOf<String?>(null) }
    var expandedPersonCategoryDetailKey by remember { mutableStateOf<String?>(null) }

    val byCategory = remember(expenses) {
        expenses.groupBy { CategoryCatalog.resolveCategoryName(it) }
            .map { (catName, list) ->
                Triple(catName, list, list.sumOf { it.amountInBaseCurrency })
            }
            .sortedByDescending { it.third }
    }

    val byPaymentMethod = remember(realExpenses) {
        realExpenses.groupBy { it.paymentMethod }
            .map { (method, list) -> method to list.sumOf { it.amountInBaseCurrency } }
            .sortedByDescending { it.second }
    }

    val unspecifiedLabel = texts.unspecifiedLabel
    val byAddressOrZone = remember(realExpenses, unspecifiedLabel) {
        realExpenses.groupBy { it.country.trim().ifEmpty { unspecifiedLabel } }
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

        // TARJETAS RESUMEN SUPERIORES (Gasto Total, Media por Día, Gasto por Persona y Media día/persona)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val realExpCount = netSpendReport.realExpenseCount
                    val expCountSubtitle = when (currentLang) {
                        AppLanguage.ES -> "$realExpCount gastos registrados"
                        AppLanguage.EN -> "$realExpCount expenses recorded"
                        AppLanguage.FR -> "$realExpCount dépenses enregistrées"
                        AppLanguage.IT -> "$realExpCount spese registrate"
                        AppLanguage.ZH -> "已记录 $realExpCount 笔支出"
                        AppLanguage.NL -> "$realExpCount geregistreerde uitgaven"
                        AppLanguage.DE -> "$realExpCount erfasste Ausgaben"
                        AppLanguage.PL -> "$realExpCount zarejestrowanych wydatków"
                        AppLanguage.PT -> "$realExpCount despesas registadas"
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
                    val totalPerPersonTitle = when (currentLang) {
                        AppLanguage.ES -> "Gasto por persona"
                        AppLanguage.EN -> "Spend per person"
                        AppLanguage.FR -> "Dépense par personne"
                        AppLanguage.IT -> "Spesa per persona"
                        AppLanguage.ZH -> "每人总支出"
                        AppLanguage.NL -> "Uitgave per persoon"
                        AppLanguage.DE -> "Ausgaben pro Person"
                        AppLanguage.PL -> "Wydatek na osobę"
                        AppLanguage.PT -> "Despesa por pessoa"
                    }
                    val totalPerPersonSubtitle = when (currentLang) {
                        AppLanguage.ES -> "Total en todo el viaje (${participants.size} pers.)"
                        AppLanguage.EN -> "Total across trip (${participants.size} pers.)"
                        AppLanguage.FR -> "Total sur tout le voyage (${participants.size} pers.)"
                        AppLanguage.IT -> "Totale dell'intero viaggio (${participants.size} pers.)"
                        AppLanguage.ZH -> "整个行程总计（${participants.size} 人）"
                        AppLanguage.NL -> "Totaal voor hele reis (${participants.size} pers.)"
                        AppLanguage.DE -> "Gesamt für die Reise (${participants.size} Pers.)"
                        AppLanguage.PL -> "Łącznie w podróży (${participants.size} os.)"
                        AppLanguage.PT -> "Total em toda a viagem (${participants.size} pess.)"
                    }
                    SummaryStatCard(
                        modifier = Modifier.weight(1f),
                        title = totalPerPersonTitle,
                        value = FormatUtils.formatCurrency(totalPerPerson, trip.baseCurrency, currentLang.locale),
                        subtitle = totalPerPersonSubtitle,
                        accentColor = Color(0xFF6366F1)
                    )

                    val avgPerDayPerPersonTitle = when (currentLang) {
                        AppLanguage.ES -> "Media día/persona"
                        AppLanguage.EN -> "Avg day/person"
                        AppLanguage.FR -> "Moy. jour/personne"
                        AppLanguage.IT -> "Media giorno/persona"
                        AppLanguage.ZH -> "日均/每人"
                        AppLanguage.NL -> "Gem. dag/persoon"
                        AppLanguage.DE -> "Schnitt Tag/Person"
                        AppLanguage.PL -> "Śr. dzień/osobę"
                        AppLanguage.PT -> "Média dia/pessoa"
                    }
                    val avgPerDayPerPersonSubtitle = when (currentLang) {
                        AppLanguage.ES -> "$tripDays días · ${participants.size} participantes"
                        AppLanguage.EN -> "$tripDays days · ${participants.size} participants"
                        AppLanguage.FR -> "$tripDays jours · ${participants.size} participants"
                        AppLanguage.IT -> "$tripDays giorni · ${participants.size} partecipanti"
                        AppLanguage.ZH -> "$tripDays 天 · ${participants.size} 位参与者"
                        AppLanguage.NL -> "$tripDays dagen · ${participants.size} deelnemers"
                        AppLanguage.DE -> "$tripDays Tage · ${participants.size} Teilnehmer"
                        AppLanguage.PL -> "$tripDays dni · ${participants.size} uczestników"
                        AppLanguage.PT -> "$tripDays dias · ${participants.size} participantes"
                    }
                    SummaryStatCard(
                        modifier = Modifier.weight(1f),
                        title = avgPerDayPerPersonTitle,
                        value = FormatUtils.formatCurrency(avgPerDayPerPerson, trip.baseCurrency, currentLang.locale),
                        subtitle = avgPerDayPerPersonSubtitle,
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
                                        val isDebtSettlement = CategoryCatalog.isDebtSettlementCategory(catName)
                                        val count = catExpenses.size
                                        val pctFormatted = String.format(currentLang.locale, "%.1f", pct)
                                        val pctText = if (isDebtSettlement) {
                                            when (currentLang) {
                                                AppLanguage.ES -> "${if (count == 1) "1 pago" else "$count pagos"} · $pctFormatted% del total"
                                                AppLanguage.EN -> "${if (count == 1) "1 payment" else "$count payments"} · $pctFormatted% of total"
                                                AppLanguage.FR -> "${if (count == 1) "1 paiement" else "$count paiements"} · $pctFormatted% du total"
                                                AppLanguage.IT -> "${if (count == 1) "1 pagamento" else "$count pagamenti"} · $pctFormatted% del totale"
                                                AppLanguage.ZH -> "$count 笔付款 · 占总额 $pctFormatted%"
                                                AppLanguage.NL -> "${if (count == 1) "1 betaling" else "$count betalingen"} · $pctFormatted% van totaal"
                                                AppLanguage.DE -> "${if (count == 1) "1 Zahlung" else "$count Zahlungen"} · $pctFormatted% der Gesamtsumme"
                                                AppLanguage.PL -> "${if (count == 1) "1 płatność" else "$count płatności"} · $pctFormatted% całości"
                                                AppLanguage.PT -> "${if (count == 1) "1 pagamento" else "$count pagamentos"} · $pctFormatted% do total"
                                            }
                                        } else {
                                            when (currentLang) {
                                                AppLanguage.ES -> "$count gastos · $pctFormatted% del total"
                                                AppLanguage.EN -> "$count expenses · $pctFormatted% of total"
                                                AppLanguage.FR -> "$count dépenses · $pctFormatted% du total"
                                                AppLanguage.IT -> "$count spese · $pctFormatted% del totale"
                                                AppLanguage.ZH -> "$count 笔支出 · 占总额 $pctFormatted%"
                                                AppLanguage.NL -> "$count uitgaven · $pctFormatted% van totaal"
                                                AppLanguage.DE -> "$count Ausgaben · $pctFormatted% der Gesamtsumme"
                                                AppLanguage.PL -> "$count wydatków · $pctFormatted% całości"
                                                AppLanguage.PT -> "$count despesas · $pctFormatted% do total"
                                            }
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
                                            catExpenses.sortedByDescending { FormatUtils.parseFlexibleDateToIso(it.date) }.forEach { exp ->
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

        // SECCIÓN 2: GASTOS POR PARTICIPANTE (GASTO NETO REAL, AJUSTES Y DESGLOSE INTERACTIVO)
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
                        val tapToViewBreakdown = when (currentLang) {
                            AppLanguage.ES -> "Pulsa para ver desglose"
                            AppLanguage.EN -> "Tap for breakdown"
                            AppLanguage.FR -> "Appuyer pour le détail"
                            AppLanguage.IT -> "Tocca per il dettaglio"
                            AppLanguage.ZH -> "点击查看明细"
                            AppLanguage.NL -> "Tik voor overzicht"
                            AppLanguage.DE -> "Tippen für Details"
                            AppLanguage.PL -> "Dotknij po szczegóły"
                            AppLanguage.PT -> "Tocar para detalhes"
                        }
                        Text(
                            text = tapToViewBreakdown,
                            style = MaterialTheme.typography.labelSmall,
                            color = TravelBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val paidColLabel = when (currentLang) {
                        AppLanguage.ES -> "Pagado"
                        AppLanguage.EN -> "Paid"
                        AppLanguage.FR -> "Payé"
                        AppLanguage.IT -> "Pagato"
                        AppLanguage.ZH -> "已支付"
                        AppLanguage.NL -> "Betaald"
                        AppLanguage.DE -> "Bezahlt"
                        AppLanguage.PL -> "Zapłacono"
                        AppLanguage.PT -> "Pago"
                    }
                    val assumedColLabel = when (currentLang) {
                        AppLanguage.ES -> "Asumido"
                        AppLanguage.EN -> "Assumed"
                        AppLanguage.FR -> "Assumé"
                        AppLanguage.IT -> "Assunto"
                        AppLanguage.ZH -> "应分摊"
                        AppLanguage.NL -> "Aandeel"
                        AppLanguage.DE -> "Anteil"
                        AppLanguage.PL -> "Udział"
                        AppLanguage.PT -> "Assumido"
                    }
                    val reserveColLabel = when (currentLang) {
                        AppLanguage.ES -> "Bote personal"
                        AppLanguage.EN -> "Personal pot"
                        AppLanguage.FR -> "Cagnotte pers."
                        AppLanguage.IT -> "Cassa pers."
                        AppLanguage.ZH -> "个人备用金"
                        AppLanguage.NL -> "Persoonlijke pot"
                        AppLanguage.DE -> "Persönliche Kasse"
                        AppLanguage.PL -> "Własna kasa"
                        AppLanguage.PT -> "Fundo pessoal"
                    }
                    val debtColLabel = when (currentLang) {
                        AppLanguage.ES -> "Debe"
                        AppLanguage.EN -> "Owes"
                        AppLanguage.FR -> "Doit"
                        AppLanguage.IT -> "Deve"
                        AppLanguage.ZH -> "欠款"
                        AppLanguage.NL -> "Schuld"
                        AppLanguage.DE -> "Schuldet"
                        AppLanguage.PL -> "Dług"
                        AppLanguage.PT -> "Deve"
                    }
                    val balanceColLabel = when (currentLang) {
                        AppLanguage.ES -> "Balance"
                        AppLanguage.EN -> "Balance"
                        AppLanguage.FR -> "Solde"
                        AppLanguage.IT -> "Bilancio"
                        AppLanguage.ZH -> "差额"
                        AppLanguage.NL -> "Saldo"
                        AppLanguage.DE -> "Saldo"
                        AppLanguage.PL -> "Bilans"
                        AppLanguage.PT -> "Balanço"
                    }
                    val settledBadgeLabel = when (currentLang) {
                        AppLanguage.ES -> "Al día"
                        AppLanguage.EN -> "Settled"
                        AppLanguage.FR -> "À jour"
                        AppLanguage.IT -> "In pari"
                        AppLanguage.ZH -> "已结清"
                        AppLanguage.NL -> "Voldaan"
                        AppLanguage.DE -> "Ausgeglichen"
                        AppLanguage.PL -> "Rozliczony"
                        AppLanguage.PT -> "Em dia"
                    }
                    val inFavorSuffix = when (currentLang) {
                        AppLanguage.ES -> "a favor / bote"
                        AppLanguage.EN -> "credit / pot"
                        AppLanguage.FR -> "en faveur / cagnotte"
                        AppLanguage.IT -> "a favore / cassa"
                        AppLanguage.ZH -> "结余 / 备用金"
                        AppLanguage.NL -> "tegoed / pot"
                        AppLanguage.DE -> "Guthaben / Kasse"
                        AppLanguage.PL -> "na plus / kasa"
                        AppLanguage.PT -> "a favor / fundo"
                    }
                    val owesSuffix = when (currentLang) {
                        AppLanguage.ES -> "debe"
                        AppLanguage.EN -> "owes"
                        AppLanguage.FR -> "doit"
                        AppLanguage.IT -> "deve"
                        AppLanguage.ZH -> "欠款"
                        AppLanguage.NL -> "schuld"
                        AppLanguage.DE -> "schuldet"
                        AppLanguage.PL -> "dług"
                        AppLanguage.PT -> "deve"
                    }
                    val paidDirectlyLabel = when (currentLang) {
                        AppLanguage.ES -> "Gastos pagados directamente"
                        AppLanguage.EN -> "Direct expenses paid"
                        AppLanguage.FR -> "Dépenses payées directement"
                        AppLanguage.IT -> "Spese pagate direttamente"
                        AppLanguage.ZH -> "直接支付的费用"
                        AppLanguage.NL -> "Direct betaalde uitgaven"
                        AppLanguage.DE -> "Direkt bezahlte Ausgaben"
                        AppLanguage.PL -> "Bezpośrednie wydatki"
                        AppLanguage.PT -> "Despesas pagas diretamente"
                    }
                    val transfersSentLabel = when (currentLang) {
                        AppLanguage.ES -> "Pagos enviados a participantes"
                        AppLanguage.EN -> "Payments sent to participants"
                        AppLanguage.FR -> "Paiements envoyés"
                        AppLanguage.IT -> "Pagamenti inviati"
                        AppLanguage.ZH -> "转给其他参与者"
                        AppLanguage.NL -> "Betalingen verzonden"
                        AppLanguage.DE -> "Gesendete Zahlungen"
                        AppLanguage.PL -> "Wysłane płatności"
                        AppLanguage.PT -> "Pagamentos enviados"
                    }
                    val transfersReceivedLabel = when (currentLang) {
                        AppLanguage.ES -> "Pagos recibidos de participantes"
                        AppLanguage.EN -> "Payments received"
                        AppLanguage.FR -> "Paiements reçus"
                        AppLanguage.IT -> "Pagamenti ricevuti"
                        AppLanguage.ZH -> "收到其他参与者付款"
                        AppLanguage.NL -> "Ontvangen betalingen"
                        AppLanguage.DE -> "Erhaltene Zahlungen"
                        AppLanguage.PL -> "Otrzymane płatności"
                        AppLanguage.PT -> "Pagamentos recebidos"
                    }
                    val potContribSummaryLabel = when (currentLang) {
                        AppLanguage.ES -> "Aportado al bote"
                        AppLanguage.EN -> "Contributed to pot"
                        AppLanguage.FR -> "Versé à la cagnotte"
                        AppLanguage.IT -> "Versato nella cassa"
                        AppLanguage.ZH -> "存入公账"
                        AppLanguage.NL -> "Bijgedragen aan pot"
                        AppLanguage.DE -> "In Kasse eingezahlt"
                        AppLanguage.PL -> "Wpłacono do kasy"
                        AppLanguage.PT -> "Contribuído para o fundo"
                    }
                    val potRecvSummaryLabel = when (currentLang) {
                        AppLanguage.ES -> "Recibido del bote"
                        AppLanguage.EN -> "Received from pot"
                        AppLanguage.FR -> "Reçu de la cagnotte"
                        AppLanguage.IT -> "Ricevuto dalla cassa"
                        AppLanguage.ZH -> "从公账收到"
                        AppLanguage.NL -> "Ontvangen uit pot"
                        AppLanguage.DE -> "Aus Kasse erhalten"
                        AppLanguage.PL -> "Otrzymano z kasy"
                        AppLanguage.PT -> "Recebido do fundo"
                    }

                    val summaries = netSpendReport.participantSummaries
                    summaries.forEachIndexed { idx, summary ->
                        val person = summary.participant
                        val avatarColor = AvatarColorPalette.parseHexColor(person.avatarColor)
                        val pct = if (totalSpent > 0) (summary.netSpend / totalSpent) * 100.0 else 0.0
                        val isExpanded = expandedPersonId == person.id

                        val balance = summary.balance
                        val balanceChipText = when {
                            summary.isBalancedWithAssumed -> settledBadgeLabel
                            balance > 0 -> "+${FormatUtils.formatCurrency(balance, trip.baseCurrency, currentLang.locale)} $inFavorSuffix"
                            else -> "-${FormatUtils.formatCurrency(abs(balance), trip.baseCurrency, currentLang.locale)} $owesSuffix"
                        }
                        val balanceValueSigned = when {
                            summary.isBalancedWithAssumed -> FormatUtils.formatCurrency(0.0, trip.baseCurrency, currentLang.locale)
                            balance > 0 -> "+${FormatUtils.formatCurrency(balance, trip.baseCurrency, currentLang.locale)}"
                            else -> "-${FormatUtils.formatCurrency(abs(balance), trip.baseCurrency, currentLang.locale)}"
                        }
                        val balanceColTitle = when {
                            summary.isBalancedWithAssumed -> balanceColLabel
                            balance > 0 -> reserveColLabel
                            else -> debtColLabel
                        }
                        val balanceChipBg = when {
                            summary.isBalancedWithAssumed -> Color(0xFFECFDF5)
                            balance > 0 -> Color(0xFFECFDF5)
                            else -> Color(0xFFFEF2F2)
                        }
                        val balanceChipColor = when {
                            summary.isBalancedWithAssumed -> EmeraldGreenDark
                            balance > 0 -> EmeraldGreenDark
                            else -> CoralRed
                        }

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
                                            color = OceanNavy,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = FormatUtils.formatCurrency(summary.netSpend, trip.baseCurrency, currentLang.locale),
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
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val assumedFormatted = FormatUtils.formatCurrency(summary.assumedSpend, trip.baseCurrency, currentLang.locale)
                                        Text(
                                            text = "$assumedColLabel: $assumedFormatted",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = balanceChipBg
                                        ) {
                                            Text(
                                                text = balanceChipText,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = balanceChipColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Desglose completo desplegable: Pagado, Asumido, Balance/Reserva + movimientos detallados
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
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Tarjeta clara con los 3 valores clave: Pagado, Asumido y Reserva/Balance
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        horizontalAlignment = Alignment.Start
                                                    ) {
                                                        Text(
                                                            text = paidColLabel,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF64748B)
                                                        )
                                                        Text(
                                                            text = FormatUtils.formatCurrency(summary.netSpend, trip.baseCurrency, currentLang.locale),
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                                            color = OceanNavy,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = assumedColLabel,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF64748B)
                                                        )
                                                        Text(
                                                            text = FormatUtils.formatCurrency(summary.assumedSpend, trip.baseCurrency, currentLang.locale),
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                                            color = TravelBlue,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        horizontalAlignment = Alignment.End
                                                    ) {
                                                        Text(
                                                            text = balanceColTitle,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF64748B)
                                                        )
                                                        Text(
                                                            text = balanceValueSigned,
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                                            color = balanceChipColor,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                val hasAdjustments = summary.transfersSent > 0.004 ||
                                                        summary.transfersReceived > 0.004 ||
                                                        summary.potContributions > 0.004 ||
                                                        summary.potReceived > 0.004

                                                if (hasAdjustments) {
                                                    HorizontalDivider(color = Color(0xFFE2E8F0))
                                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                        NetSpendBreakdownRow(
                                                            label = paidDirectlyLabel,
                                                            value = "+${FormatUtils.formatCurrency(summary.realExpensesPaid, trip.baseCurrency, currentLang.locale)}",
                                                            valueColor = OceanNavy
                                                        )
                                                        if (summary.transfersSent > 0.004) {
                                                            NetSpendBreakdownRow(
                                                                label = transfersSentLabel,
                                                                value = "+${FormatUtils.formatCurrency(summary.transfersSent, trip.baseCurrency, currentLang.locale)}",
                                                                valueColor = TravelBlue
                                                            )
                                                        }
                                                        if (summary.potContributions > 0.004) {
                                                            NetSpendBreakdownRow(
                                                                label = potContribSummaryLabel,
                                                                value = "+${FormatUtils.formatCurrency(summary.potContributions, trip.baseCurrency, currentLang.locale)}",
                                                                valueColor = Color(0xFF4F46E5)
                                                            )
                                                        }
                                                        if (summary.transfersReceived > 0.004) {
                                                            NetSpendBreakdownRow(
                                                                label = transfersReceivedLabel,
                                                                value = "-${FormatUtils.formatCurrency(summary.transfersReceived, trip.baseCurrency, currentLang.locale)}",
                                                                valueColor = EmeraldGreenDark
                                                            )
                                                        }
                                                        if (summary.potReceived > 0.004) {
                                                            NetSpendBreakdownRow(
                                                                label = potRecvSummaryLabel,
                                                                value = "-${FormatUtils.formatCurrency(summary.potReceived, trip.baseCurrency, currentLang.locale)}",
                                                                valueColor = EmeraldGreenDark
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (summary.entries.isEmpty()) {
                                            val noMovementsPerson = when (currentLang) {
                                                AppLanguage.ES -> "${person.name} no tiene gastos ni movimientos registrados aún."
                                                AppLanguage.EN -> "${person.name} has no expenses or movements recorded yet."
                                                AppLanguage.FR -> "${person.name} n'a pas encore de dépenses ni de mouvements enregistrés."
                                                AppLanguage.IT -> "${person.name} non ha ancora spese o movimenti registrati."
                                                AppLanguage.ZH -> "${person.name} 尚未记录任何支出或资金往来。"
                                                AppLanguage.NL -> "${person.name} heeft nog geen uitgaven of transacties geregistreerd."
                                                AppLanguage.DE -> "${person.name} hat noch keine Ausgaben oder Buchungen erfasst."
                                                AppLanguage.PL -> "${person.name} nie ma jeszcze żadnych wydatków ani operacji."
                                                AppLanguage.PT -> "${person.name} ainda não tem despesas nem movimentos registados."
                                            }
                                            Text(
                                                text = noMovementsPerson,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF64748B)
                                            )
                                        } else {
                                            summary.entries.forEach { entry ->
                                                val entryLabel = entry.displayLabel(currentLang)
                                                val subtitleText = when (entry.type) {
                                                    ParticipantLedgerEntryType.REAL_EXPENSE_PAID -> {
                                                        val catLabel = CategoryCatalog.displayName(entry.categoryName.orEmpty(), currentLang)
                                                        "$catLabel · ${FormatUtils.formatShortDate(entry.date, currentLang.locale)}"
                                                    }
                                                    else -> {
                                                        val datePart = FormatUtils.formatShortDate(entry.date, currentLang.locale)
                                                        if (entry.description.isNotBlank()) {
                                                            "$datePart · ${entry.description}"
                                                        } else {
                                                            datePart
                                                        }
                                                    }
                                                }
                                                val signedSign = if (entry.signedAmount >= 0) "+" else "-"
                                                val formattedSigned = "$signedSign${FormatUtils.formatCurrency(entry.absoluteAmount, trip.baseCurrency, currentLang.locale)}"
                                                val amountColor = when (entry.type) {
                                                    ParticipantLedgerEntryType.REAL_EXPENSE_PAID -> avatarColor
                                                    ParticipantLedgerEntryType.TRANSFER_SENT -> TravelBlue
                                                    ParticipantLedgerEntryType.POT_CONTRIBUTION -> Color(0xFF4F46E5)
                                                    ParticipantLedgerEntryType.TRANSFER_RECEIVED,
                                                    ParticipantLedgerEntryType.POT_RECEIVED -> EmeraldGreenDark
                                                }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .let { baseMod ->
                                                            if (entry.sourceExpense != null) {
                                                                baseMod.clickable { onExpenseClick(entry.sourceExpense) }
                                                            } else {
                                                                baseMod
                                                            }
                                                        }
                                                        .padding(vertical = 3.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = entryLabel,
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                            color = OceanNavy,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = subtitleText,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF64748B),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = formattedSigned,
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = amountColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (idx < summaries.lastIndex) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }

        // SECCIÓN 2B: DESGLOSE DE GASTOS POR PERSONA Y CATEGORÍA
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stats_person_category_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val personCategoryTitle = when (currentLang) {
                        AppLanguage.ES -> "Gastos por persona y categoría"
                        AppLanguage.EN -> "Expenses by person & category"
                        AppLanguage.FR -> "Dépenses par personne et catégorie"
                        AppLanguage.IT -> "Spese per persona e categoria"
                        AppLanguage.ZH -> "按人员与类别统计支出"
                        AppLanguage.NL -> "Uitgaven per persoon en categorie"
                        AppLanguage.DE -> "Ausgaben nach Person & Kategorie"
                        AppLanguage.PL -> "Wydatki według osoby i kategorii"
                        AppLanguage.PT -> "Despesas por pessoa e categoria"
                    }
                    val tapForCategoriesHint = when (currentLang) {
                        AppLanguage.ES -> "Pulsa para ver categorías"
                        AppLanguage.EN -> "Tap for categories"
                        AppLanguage.FR -> "Appuyer pour les catégories"
                        AppLanguage.IT -> "Tocca per le categorie"
                        AppLanguage.ZH -> "点击查看类别"
                        AppLanguage.NL -> "Tik voor categorieën"
                        AppLanguage.DE -> "Tippen für Kategorien"
                        AppLanguage.PL -> "Dotknij po kategorie"
                        AppLanguage.PT -> "Tocar para categorias"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = personCategoryTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = OceanNavy,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tapForCategoriesHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = TravelBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (byPersonAndCategory.isEmpty() || byPersonAndCategory.all { it.categories.isEmpty() }) {
                        Text(
                            text = texts.noExpensesYetStats,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        byPersonAndCategory.forEachIndexed { idx, personBreakdown ->
                            val person = personBreakdown.participant
                            val avatarColor = AvatarColorPalette.parseHexColor(person.avatarColor)
                            val isPersonExpanded = expandedPersonCategoryId == person.id
                            val catCount = personBreakdown.categories.size
                            val expCount = personBreakdown.totalExpenseCount

                            val summarySubtitle = when (currentLang) {
                                AppLanguage.ES -> "${if (catCount == 1) "1 categoría" else "$catCount categorías"} · ${if (expCount == 1) "1 gasto" else "$expCount gastos"}"
                                AppLanguage.EN -> "${if (catCount == 1) "1 category" else "$catCount categories"} · ${if (expCount == 1) "1 expense" else "$expCount expenses"}"
                                AppLanguage.FR -> "${if (catCount == 1) "1 catégorie" else "$catCount catégories"} · ${if (expCount == 1) "1 dépense" else "$expCount dépenses"}"
                                AppLanguage.IT -> "${if (catCount == 1) "1 categoria" else "$catCount categorie"} · ${if (expCount == 1) "1 spesa" else "$expCount spese"}"
                                AppLanguage.ZH -> "$catCount 个类别 · $expCount 笔支出"
                                AppLanguage.NL -> "${if (catCount == 1) "1 categorie" else "$catCount categorieën"} · ${if (expCount == 1) "1 uitgave" else "$expCount uitgaven"}"
                                AppLanguage.DE -> "${if (catCount == 1) "1 Kategorie" else "$catCount Kategorien"} · ${if (expCount == 1) "1 Ausgabe" else "$expCount Ausgaben"}"
                                AppLanguage.PL -> "$catCount kat. · $expCount wyd."
                                AppLanguage.PT -> "${if (catCount == 1) "1 categoria" else "$catCount categorias"} · ${if (expCount == 1) "1 despesa" else "$expCount despesas"}"
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        expandedPersonCategoryId = if (isPersonExpanded) null else person.id
                                    }
                                    .padding(vertical = 6.dp)
                                    .testTag("stats_person_cat_row_${person.id}")
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
                                        size = 36.dp
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
                                                color = OceanNavy,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = FormatUtils.formatCurrency(personBreakdown.totalAmount, trip.baseCurrency, currentLang.locale),
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                                    color = OceanNavy
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = if (isPersonExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = Color(0xFF64748B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = summarySubtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isPersonExpanded) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, avatarColor.copy(alpha = 0.35f))
                                    ) {
                                        if (personBreakdown.categories.isEmpty()) {
                                            val noCatExpensesForPerson = when (currentLang) {
                                                AppLanguage.ES -> "${person.name} no tiene gastos asignados por categoría aún."
                                                AppLanguage.EN -> "${person.name} has no expenses assigned by category yet."
                                                AppLanguage.FR -> "${person.name} n'a pas encore de dépenses par catégorie."
                                                AppLanguage.IT -> "${person.name} non ha ancora spese per categoria."
                                                AppLanguage.ZH -> "${person.name} 尚未有分类支出。"
                                                AppLanguage.NL -> "${person.name} heeft nog geen uitgaven per categorie."
                                                AppLanguage.DE -> "${person.name} hat noch keine Ausgaben nach Kategorie."
                                                AppLanguage.PL -> "${person.name} nie ma jeszcze wydatków według kategorii."
                                                AppLanguage.PT -> "${person.name} ainda não tem despesas por categoria."
                                            }
                                            Text(
                                                text = noCatExpensesForPerson,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF64748B),
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        } else {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                personBreakdown.categories.forEachIndexed { catIdx, catItem ->
                                                    val catInfo = CategoryCatalog.getCategory(catItem.categoryName)
                                                    val displayCatName = CategoryCatalog.displayName(catItem.categoryName, currentLang)
                                                    val catPct = if (personBreakdown.totalAmount > 0.0) {
                                                        (catItem.totalAmount / personBreakdown.totalAmount) * 100.0
                                                    } else 0.0
                                                    val pctStr = String.format(currentLang.locale, "%.1f", catPct)
                                                    val detailKey = "${person.id}::${catItem.categoryName}"
                                                    val isCatDetailOpen = expandedPersonCategoryDetailKey == detailKey
                                                    val catExpCountText = when (currentLang) {
                                                        AppLanguage.ES -> "${if (catItem.expenseCount == 1) "1 gasto" else "${catItem.expenseCount} gastos"} · $pctStr%"
                                                        AppLanguage.EN -> "${if (catItem.expenseCount == 1) "1 expense" else "${catItem.expenseCount} expenses"} · $pctStr%"
                                                        AppLanguage.FR -> "${if (catItem.expenseCount == 1) "1 dépense" else "${catItem.expenseCount} dépenses"} · $pctStr%"
                                                        AppLanguage.IT -> "${if (catItem.expenseCount == 1) "1 spesa" else "${catItem.expenseCount} spese"} · $pctStr%"
                                                        AppLanguage.ZH -> "${catItem.expenseCount} 笔支出 · $pctStr%"
                                                        AppLanguage.NL -> "${if (catItem.expenseCount == 1) "1 uitgave" else "${catItem.expenseCount} uitgaven"} · $pctStr%"
                                                        AppLanguage.DE -> "${if (catItem.expenseCount == 1) "1 Ausgabe" else "${catItem.expenseCount} Ausgaben"} · $pctStr%"
                                                        AppLanguage.PL -> "${catItem.expenseCount} wyd. · $pctStr%"
                                                        AppLanguage.PT -> "${if (catItem.expenseCount == 1) "1 despesa" else "${catItem.expenseCount} despesas"} · $pctStr%"
                                                    }

                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.White)
                                                            .clickable {
                                                                expandedPersonCategoryDetailKey = if (isCatDetailOpen) null else detailKey
                                                            }
                                                            .padding(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(30.dp)
                                                                    .clip(CircleShape)
                                                                    .background(catInfo.color.copy(alpha = 0.16f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = catInfo.icon,
                                                                    contentDescription = displayCatName,
                                                                    tint = catInfo.color,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(
                                                                        text = displayCatName,
                                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                                        color = OceanNavy,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis,
                                                                        modifier = Modifier.weight(1f)
                                                                    )
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text(
                                                                        text = FormatUtils.formatCurrency(catItem.totalAmount, trip.baseCurrency, currentLang.locale),
                                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                                                        color = catInfo.color
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.height(3.dp))
                                                                LinearProgressIndicator(
                                                                    progress = { (catPct / 100.0).toFloat().coerceIn(0f, 1f) },
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .height(5.dp)
                                                                        .clip(CircleShape),
                                                                    color = catInfo.color,
                                                                    trackColor = Color(0xFFE2E8F0)
                                                                )
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Text(
                                                                    text = catExpCountText,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = Color(0xFF64748B)
                                                                )
                                                            }
                                                        }

                                                        AnimatedVisibility(visible = isCatDetailOpen) {
                                                            Column(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(top = 6.dp),
                                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                                                catItem.details.forEach { detail ->
                                                                    Row(
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .clickable { onExpenseClick(detail.expense) }
                                                                            .padding(vertical = 2.dp),
                                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        Column(modifier = Modifier.weight(1f)) {
                                                                            Text(
                                                                                text = detail.expense.placeName.ifBlank { detail.expense.notes.ifBlank { displayCatName } },
                                                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                                                color = OceanNavy,
                                                                                maxLines = 1,
                                                                                overflow = TextOverflow.Ellipsis
                                                                            )
                                                                            Text(
                                                                                text = FormatUtils.formatShortDate(detail.expense.date, currentLang.locale),
                                                                                style = MaterialTheme.typography.labelSmall,
                                                                                color = Color(0xFF64748B)
                                                                            )
                                                                        }
                                                                        Spacer(modifier = Modifier.width(8.dp))
                                                                        Text(
                                                                            text = FormatUtils.formatCurrency(detail.participantAmount, trip.baseCurrency, currentLang.locale),
                                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                                            color = catInfo.color
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if (catIdx < personBreakdown.categories.lastIndex) {
                                                        HorizontalDivider(color = Color(0xFFE2E8F0).copy(alpha = 0.6f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (idx < byPersonAndCategory.lastIndex) {
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
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

@Composable
private fun NetSpendBreakdownRow(
    label: String,
    value: String,
    valueColor: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Medium
            ),
            color = if (isBold) OceanNavy else Color(0xFF475569),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isBold) FontWeight.Black else FontWeight.Bold
            ),
            color = valueColor
        )
    }
}
