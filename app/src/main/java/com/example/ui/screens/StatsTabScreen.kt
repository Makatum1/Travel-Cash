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
    val avgPerPerson = netSpendReport.equalShareAverage

    val highestSpendingDay = remember(realExpenses) {
        realExpenses.groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.amountInBaseCurrency } }
            .maxByOrNull { it.value }
    }

    // Estado interactivo de filas desplegadas en Categorías y Personas
    var expandedCategoryName by remember { mutableStateOf<String?>(null) }
    var expandedPersonId by remember { mutableStateOf<String?>(null) }

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

        // TARJETAS RESUMEN SUPERIORES (4 KPIs)
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

                    val netSpendLabel = when (currentLang) {
                        AppLanguage.ES -> "Gasto neto"
                        AppLanguage.EN -> "Net spend"
                        AppLanguage.FR -> "Dépense nette"
                        AppLanguage.IT -> "Spesa netta"
                        AppLanguage.ZH -> "净支出"
                        AppLanguage.NL -> "Netto uitgaven"
                        AppLanguage.DE -> "Nettoausgaben"
                        AppLanguage.PL -> "Wydatek netto"
                        AppLanguage.PT -> "Gasto líquido"
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
                    val diffVsAvgLabel = when (currentLang) {
                        AppLanguage.ES -> "Diferencia vs media"
                        AppLanguage.EN -> "Diff. vs average"
                        AppLanguage.FR -> "Écart vs moyenne"
                        AppLanguage.IT -> "Diff. vs media"
                        AppLanguage.ZH -> "与人均差额"
                        AppLanguage.NL -> "Verschil t.o.v. gem."
                        AppLanguage.DE -> "Diff. zum Ø"
                        AppLanguage.PL -> "Różnica vs śr."
                        AppLanguage.PT -> "Diferença vs média"
                    }
                    val equalBadgeLabel = when (currentLang) {
                        AppLanguage.ES -> "= Media"
                        AppLanguage.EN -> "= Avg"
                        AppLanguage.FR -> "= Moy."
                        AppLanguage.IT -> "= Media"
                        AppLanguage.ZH -> "= 均值"
                        AppLanguage.NL -> "= Gem."
                        AppLanguage.DE -> "= Ø"
                        AppLanguage.PL -> "= Śr."
                        AppLanguage.PT -> "= Média"
                    }

                    val summaries = netSpendReport.participantSummaries
                    summaries.forEachIndexed { idx, summary ->
                        val person = summary.participant
                        val avatarColor = AvatarColorPalette.parseHexColor(person.avatarColor)
                        val pct = if (totalSpent > 0) (summary.netSpend / totalSpent) * 100.0 else 0.0
                        val isExpanded = expandedPersonId == person.id

                        val diff = summary.differenceFromAverage
                        val diffChipText = if (summary.isBalancedWithAverage) {
                            equalBadgeLabel
                        } else {
                            val sign = if (diff > 0) "+" else "-"
                            "$sign${FormatUtils.formatCurrency(abs(diff), trip.baseCurrency, currentLang.locale)}"
                        }
                        val diffChipBg = when {
                            summary.isBalancedWithAverage -> Color(0xFFECFDF5)
                            diff > 0 -> Color(0xFFEFF6FF)
                            else -> Color(0xFFFEF2F2)
                        }
                        val diffChipColor = when {
                            summary.isBalancedWithAverage -> EmeraldGreenDark
                            diff > 0 -> TravelBlue
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
                                        val movementCount = summary.entries.size
                                        val pctFmt = String.format(currentLang.locale, "%.1f", pct)
                                        val movementsText = when (currentLang) {
                                            AppLanguage.ES -> "${if (movementCount == 1) "1 movimiento" else "$movementCount movimientos"} · $pctFmt% del total"
                                            AppLanguage.EN -> "${if (movementCount == 1) "1 movement" else "$movementCount movements"} · $pctFmt% of total"
                                            AppLanguage.FR -> "${if (movementCount == 1) "1 mouvement" else "$movementCount mouvements"} · $pctFmt% du total"
                                            AppLanguage.IT -> "${if (movementCount == 1) "1 movimento" else "$movementCount movimenti"} · $pctFmt% del totale"
                                            AppLanguage.ZH -> "$movementCount 条记录 · 占总额 $pctFmt%"
                                            AppLanguage.NL -> "${if (movementCount == 1) "1 transactie" else "$movementCount transacties"} · $pctFmt% van totaal"
                                            AppLanguage.DE -> "${if (movementCount == 1) "1 Buchung" else "$movementCount Buchungen"} · $pctFmt% gesamt"
                                            AppLanguage.PL -> "${if (movementCount == 1) "1 operacja" else "$movementCount operacji"} · $pctFmt% całości"
                                            AppLanguage.PT -> "${if (movementCount == 1) "1 movimento" else "$movementCount movimentos"} · $pctFmt% do total"
                                        }
                                        Text(
                                            text = movementsText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = diffChipBg
                                        ) {
                                            Text(
                                                text = diffChipText,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = diffChipColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Desglose completo desplegable: resumen del cálculo neto + lista detallada de movimientos
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
                                        // Mini resumen del cálculo del gasto neto
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
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
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 2.dp),
                                                    color = Color(0xFFE2E8F0)
                                                )
                                                NetSpendBreakdownRow(
                                                    label = netSpendLabel,
                                                    value = FormatUtils.formatCurrency(summary.netSpend, trip.baseCurrency, currentLang.locale),
                                                    valueColor = OceanNavy,
                                                    isBold = true
                                                )
                                                NetSpendBreakdownRow(
                                                    label = diffVsAvgLabel,
                                                    value = diffChipText,
                                                    valueColor = diffChipColor,
                                                    isBold = true
                                                )
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
