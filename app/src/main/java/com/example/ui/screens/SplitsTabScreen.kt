package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.CategoryCatalog
import com.example.data.model.ExpenseEntity
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.PaymentMethodCatalog
import com.example.data.model.TripEntity
import com.example.domain.DebtCalculationResult
import com.example.domain.DebtCalculator
import com.example.domain.DebtTransaction
import com.example.domain.ParticipantBalance
import com.example.ui.theme.AmberGoldDark
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
import com.example.ui.theme.TravelBlue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToLong
import com.example.util.FormatUtils
import com.example.util.UiTexts
import kotlin.math.abs

@Composable
fun SplitsTabScreen(
    trip: TripEntity,
    participants: List<ParticipantEntity>,
    expenses: List<ExpenseEntity>,
    incomes: List<IncomeEntity>,
    debtResult: DebtCalculationResult,
    onAddParticipant: (String) -> Unit,
    onUpdateParticipantName: (ParticipantEntity, String) -> Unit,
    onParticipantAvatarClick: (ParticipantEntity) -> Unit,
    onDeleteParticipant: (ParticipantEntity) -> Unit,
    onAddIncomeClick: () -> Unit,
    onEditIncomeClick: (IncomeEntity) -> Unit,
    onDeleteIncomeClick: (String) -> Unit,
    onSettleDebt: (DebtTransaction, Double, String) -> Unit = { _, _, _ -> },
    topHeader: @Composable () -> Unit = {}
) {
    val currentLang = LocalAppLanguage.current
    val texts = UiTexts.get(currentLang)
    val participantMap = remember(participants) { participants.associateBy { it.id } }

    var newParticipantName by remember { mutableStateOf("") }
    var editingParticipantId by remember { mutableStateOf<String?>(null) }
    var editingNameText by remember { mutableStateOf("") }

    // Estados de tarjetas desplegables interactivas
    var expandedTransactionIndex by remember { mutableStateOf<Int?>(null) }
    var expandedParticipantId by remember { mutableStateOf<String?>(null) }
    var showSettleDebtsModal by remember { mutableStateOf(false) }

    val settlementAccountsTitle = when (currentLang) {
        AppLanguage.ES -> "Liquidación de Cuentas"
        AppLanguage.EN -> "Settlement of Accounts"
        AppLanguage.FR -> "Règlement des comptes"
        AppLanguage.IT -> "Liquidazione dei conti"
        AppLanguage.ZH -> "账目清算"
        AppLanguage.NL -> "Rekeningvereffening"
        AppLanguage.DE -> "Abrechnung der Konten"
        AppLanguage.PL -> "Rozliczenie rachunków"
    }

    val settlementAccountsSubtitle = when (currentLang) {
        AppLanguage.ES -> "Pulsa sobre cualquier pago para ver el desglose matemático"
        AppLanguage.EN -> "Tap on any payment to view its mathematical breakdown"
        AppLanguage.FR -> "Appuyez sur un paiement pour voir le détail mathématique"
        AppLanguage.IT -> "Tocca un pagamento per visualizzare la ripartizione matematica"
        AppLanguage.ZH -> "点击任意付款可查看详细计算明细"
        AppLanguage.NL -> "Tik op een betaling voor de wiskundige uitsplitsing"
        AppLanguage.DE -> "Tippen Sie auf eine Zahlung für die mathematische Aufschlüsselung"
        AppLanguage.PL -> "Dotknij dowolnej płatności, aby zobaczyć szczegóły obliczeń"
    }

    val allSettledBanner = when (currentLang) {
        AppLanguage.ES -> "✅ ¡Todas las cuentas del viaje están saldadas!"
        AppLanguage.EN -> "✅ All trip accounts are settled!"
        AppLanguage.FR -> "✅ Tous les comptes du voyage sont réglés !"
        AppLanguage.IT -> "✅ Tutti i conti del viaggio sono saldati!"
        AppLanguage.ZH -> "✅ 本次行程所有账目已结清！"
        AppLanguage.NL -> "✅ Alle rekeningen van de reis zijn vereffend!"
        AppLanguage.DE -> "✅ Alle Reisekonten sind ausgeglichen!"
        AppLanguage.PL -> "✅ Wszystkie rachunki z podróży zostały rozliczone!"
    }

    val detailedBalanceTitle = when (currentLang) {
        AppLanguage.ES -> "Balance Detallado por Persona"
        AppLanguage.EN -> "Detailed Balance by Person"
        AppLanguage.FR -> "Bilan détaillé par personne"
        AppLanguage.IT -> "Bilancio dettagliato per persona"
        AppLanguage.ZH -> "个人收支明细"
        AppLanguage.NL -> "Gedetailleerd saldo per persoon"
        AppLanguage.DE -> "Detaillierte Bilanz pro Person"
        AppLanguage.PL -> "Szczegółowy bilans na osobę"
    }

    val detailedBalanceSubtitle = when (currentLang) {
        AppLanguage.ES -> "Pulsa un viajero para ver su resumen contable y movimientos"
        AppLanguage.EN -> "Tap a traveler to view their accounting summary & movements"
        AppLanguage.FR -> "Appuyez sur un voyageur pour voir son résumé et ses mouvements"
        AppLanguage.IT -> "Tocca un viaggiatore per visualizzare riepilogo e movimenti"
        AppLanguage.ZH -> "点击旅行者查看其账目摘要及收支流水"
        AppLanguage.NL -> "Tik op een reiziger om boekhouding en bewegingen te zien"
        AppLanguage.DE -> "Tippen Sie auf einen Reisenden, um Buchhaltung und Buchungen zu sehen"
        AppLanguage.PL -> "Dotknij podróżnika, aby zobaczyć podsumowanie i operacje"
    }

    val potContributionsTitleText = when (currentLang) {
        AppLanguage.ES -> "Aportaciones al Bote Común"
        AppLanguage.EN -> "Common Pot Contributions"
        AppLanguage.FR -> "Contributions à la cagnotte"
        AppLanguage.IT -> "Contributi alla cassa comune"
        AppLanguage.ZH -> "公账存入记录"
        AppLanguage.NL -> "Inleg in de gezamenlijke pot"
        AppLanguage.DE -> "Beiträge zur Gemeinschaftskasse"
        AppLanguage.PL -> "Wpłaty do wspólnej kasy"
    }

    val potCollectedSubtitle = when (currentLang) {
        AppLanguage.ES -> "Total recaudado en caja: ${FormatUtils.formatCurrency(debtResult.totalBoteCollected, trip.baseCurrency)}"
        AppLanguage.EN -> "Total collected in cash: ${FormatUtils.formatCurrency(debtResult.totalBoteCollected, trip.baseCurrency)}"
        AppLanguage.FR -> "Total collecté en caisse : ${FormatUtils.formatCurrency(debtResult.totalBoteCollected, trip.baseCurrency)}"
        AppLanguage.IT -> "Totale raccolto in cassa: ${FormatUtils.formatCurrency(debtResult.totalBoteCollected, trip.baseCurrency)}"
        AppLanguage.ZH -> "公账实收现金总计: ${FormatUtils.formatCurrency(debtResult.totalBoteCollected, trip.baseCurrency)}"
        AppLanguage.NL -> "Totaal ingelegd in pot: ${FormatUtils.formatCurrency(debtResult.totalBoteCollected, trip.baseCurrency)}"
        AppLanguage.DE -> "Insgesamt in Kasse eingezahlt: ${FormatUtils.formatCurrency(debtResult.totalBoteCollected, trip.baseCurrency)}"
        AppLanguage.PL -> "Łącznie zebrano w kasie: ${FormatUtils.formatCurrency(debtResult.totalBoteCollected, trip.baseCurrency)}"
    }

    val addPotBtnText = when (currentLang) {
        AppLanguage.ES -> "Añadir Bote"
        AppLanguage.EN -> "Add Pot"
        AppLanguage.FR -> "Ajouter cagnotte"
        AppLanguage.IT -> "Aggiungi cassa"
        AppLanguage.ZH -> "存入公账"
        AppLanguage.NL -> "Pot toevoegen"
        AppLanguage.DE -> "Kasse einzahlen"
        AppLanguage.PL -> "Dodaj wpłatę"
    }

    val noPotContributionsText = when (currentLang) {
        AppLanguage.ES -> "No se han registrado aportaciones al bote común en este viaje."
        AppLanguage.EN -> "No common pot contributions have been recorded for this trip."
        AppLanguage.FR -> "Aucune contribution à la cagnotte enregistrée pour ce voyage."
        AppLanguage.IT -> "Nessun contributo alla cassa registrato per questo viaggio."
        AppLanguage.ZH -> "本次行程尚未记录任何公账出资。"
        AppLanguage.NL -> "Nog geen inleg in de gezamenlijke pot geregistreerd voor deze reis."
        AppLanguage.DE -> "Noch keine Kassenbeiträge für diese Reise erfasst."
        AppLanguage.PL -> "Nie zarejestrowano jeszcze wpłat do wspólnej kasy w tej podróży."
    }

    val participantsTitleText = when (currentLang) {
        AppLanguage.ES -> "Participantes del Viaje (${participants.size})"
        AppLanguage.EN -> "Trip Participants (${participants.size})"
        AppLanguage.FR -> "Participants au voyage (${participants.size})"
        AppLanguage.IT -> "Partecipanti al viaggio (${participants.size})"
        AppLanguage.ZH -> "行程参与者 (${participants.size})"
        AppLanguage.NL -> "Reisdeelnemers (${participants.size})"
        AppLanguage.DE -> "Reiseteilnehmer (${participants.size})"
        AppLanguage.PL -> "Uczestnicy podróży (${participants.size})"
    }

    val tapIconToAddPhotoText = when (currentLang) {
        AppLanguage.ES -> "Pulsa el icono para añadir foto"
        AppLanguage.EN -> "Tap icon to add photo"
        AppLanguage.FR -> "Appuyez sur l'icône pour ajouter une photo"
        AppLanguage.IT -> "Tocca l'icona per aggiungere una foto"
        AppLanguage.ZH -> "点击图标添加照片"
        AppLanguage.NL -> "Tik op pictogram voor foto"
        AppLanguage.DE -> "Icon antippen, um Foto hinzuzufügen"
        AppLanguage.PL -> "Dotknij ikony, aby dodać zdjęcie"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "splits_top_header") {
            topHeader()
        }

        // =========================================================================
        // 1. LIQUIDACIÓN DE CUENTAS (CLICKABLE CON PANEL DETALLADO)
        // =========================================================================
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = settlementAccountsTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = OceanNavy
                            )
                            Text(
                                text = settlementAccountsSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = TravelBlue
                            )
                        }

                        // Botón arriba a la derecha: Saldar deudas
                        Button(
                            onClick = { showSettleDebtsModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("settle_debts_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = texts.btnSettle,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (debtResult.transactions.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldGreenLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = allSettledBanner,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldGreenDark,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            debtResult.transactions.forEachIndexed { idx, tx ->
                                val isExpanded = expandedTransactionIndex == idx
                                SettlementTransactionCard(
                                    tx = tx,
                                    baseCurrency = trip.baseCurrency,
                                    isExpanded = isExpanded,
                                    debtResult = debtResult,
                                    participants = participants,
                                    expenses = expenses,
                                    incomes = incomes,
                                    currentLang = currentLang,
                                    onClick = {
                                        expandedTransactionIndex = if (isExpanded) null else idx
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. BALANCE DETALLADO POR PERSONA (CLICKABLE)
        // =========================================================================
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
                        Column {
                            Text(
                                text = detailedBalanceTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = OceanNavy
                            )
                            Text(
                                text = detailedBalanceSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = TravelBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        debtResult.participantBalances.forEach { balance ->
                            val isExpanded = expandedParticipantId == balance.participantId
                            ParticipantBalanceInteractiveCard(
                                balance = balance,
                                baseCurrency = trip.baseCurrency,
                                isExpanded = isExpanded,
                                expenses = expenses,
                                currentLang = currentLang,
                                onAvatarClick = {
                                    participantMap[balance.participantId]?.let { onParticipantAvatarClick(it) }
                                },
                                onClick = {
                                    expandedParticipantId = if (isExpanded) null else balance.participantId
                                }
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3. BOTE COMÚN E HISTORIAL DE APORTACIONES (CON LÁPIZ EDITAR Y PAPELERA ELIMINAR)
        // =========================================================================
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = potContributionsTitleText,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = OceanNavy
                            )
                            Text(
                                text = potCollectedSubtitle,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldGreenDark
                            )
                        }
                        Button(
                            onClick = onAddIncomeClick,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = addPotBtnText,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (incomes.isEmpty()) {
                        Text(
                            text = noPotContributionsText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            incomes.forEach { inc ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                val potContributionFallback = when (currentLang) {
                                                    AppLanguage.ES -> "Aportación al Bote"
                                                    AppLanguage.EN -> "Pot Contribution"
                                                    AppLanguage.FR -> "Contribution cagnotte"
                                                    AppLanguage.IT -> "Contributo cassa"
                                                    AppLanguage.ZH -> "公账出资"
                                                    AppLanguage.NL -> "Inleg pot"
                                                    AppLanguage.DE -> "Kassenbeitrag"
                                                    AppLanguage.PL -> "Wpłata do kasy"
                                                }
                                                Text(
                                                    text = inc.description.ifBlank { potContributionFallback },
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = OceanNavy
                                                )
                                                val methodLabel = if (inc.calculationMethod == "perPerson") {
                                                    texts.amountPerPersonLabel
                                                } else {
                                                    texts.globalAmountLabel
                                                }
                                                Text(
                                                    text = "📅 ${FormatUtils.formatShortDate(inc.date, currentLang.locale)} · $methodLabel",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF475569)
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "+${FormatUtils.formatCurrency(inc.amount, trip.baseCurrency)}",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                                    color = EmeraldGreenDark
                                                )
                                                // Botón de editar (lápiz)
                                                IconButton(
                                                    onClick = { onEditIncomeClick(inc) },
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .testTag("edit_income_${inc.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = null,
                                                        tint = TravelBlue,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                // Botón de eliminar (papelera)
                                                IconButton(
                                                    onClick = { onDeleteIncomeClick(inc.id) },
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .testTag("delete_income_${inc.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = null,
                                                        tint = CoralRed,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (inc.contributions.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            inc.contributions.forEach { c ->
                                                val pName = participantMap[c.participantId]?.name ?: texts.defaultTravelerName
                                                val disc = c.debtDiscount ?: 0.0
                                                val lineText = when (currentLang) {
                                                    AppLanguage.ES -> {
                                                        if (disc > 0.009) "• $pName: Puso ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)} (descontados ${FormatUtils.formatCurrency(disc, trip.baseCurrency)} por deuda a su favor)"
                                                        else "• $pName: Puso ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)}"
                                                    }
                                                    AppLanguage.EN -> {
                                                        if (disc > 0.009) "• $pName: Contributed ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)} (deducted ${FormatUtils.formatCurrency(disc, trip.baseCurrency)} owed in their favor)"
                                                        else "• $pName: Contributed ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)}"
                                                    }
                                                    AppLanguage.FR -> {
                                                        if (disc > 0.009) "• $pName : A versé ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)} (dont ${FormatUtils.formatCurrency(disc, trip.baseCurrency)} déduits en sa faveur)"
                                                        else "• $pName : A versé ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)}"
                                                    }
                                                    AppLanguage.IT -> {
                                                        if (disc > 0.009) "• $pName: Ha versato ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)} (detratti ${FormatUtils.formatCurrency(disc, trip.baseCurrency)} a suo favore)"
                                                        else "• $pName: Ha versato ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)}"
                                                    }
                                                    AppLanguage.ZH -> {
                                                        if (disc > 0.009) "• $pName: 出资 ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)} (已扣除 ${FormatUtils.formatCurrency(disc, trip.baseCurrency)} 债权)"
                                                        else "• $pName: 出资 ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)}"
                                                    }
                                                    AppLanguage.NL -> {
                                                        if (disc > 0.009) "• $pName: Heeft ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)} ingelegd (waarvan ${FormatUtils.formatCurrency(disc, trip.baseCurrency)} verrekend)"
                                                        else "• $pName: Heeft ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)} ingelegd"
                                                    }
                                                    AppLanguage.DE -> {
                                                        if (disc > 0.009) "• $pName: Hat ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)} eingezahlt (davon ${FormatUtils.formatCurrency(disc, trip.baseCurrency)} verrechnet)"
                                                        else "• $pName: Hat ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)} eingezahlt"
                                                    }
                                                    AppLanguage.PL -> {
                                                        if (disc > 0.009) "• $pName: Wpłacił(a) ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)} (odliczono ${FormatUtils.formatCurrency(disc, trip.baseCurrency)} należności)"
                                                        else "• $pName: Wpłacił(a) ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)}"
                                                    }
                                                }
                                                Text(
                                                    text = lineText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF065F46)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 4. GESTIÓN DE PARTICIPANTES (AÑADIR, EDITAR NOMBRE, CAMBIAR FOTO Y ELIMINAR)
        // =========================================================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = participantsTitleText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Añadir nuevo participante
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newParticipantName,
                            onValueChange = { newParticipantName = it },
                            placeholder = {
                                Text(
                                    text = texts.addParticipantPlaceholder,
                                    color = Color(0xFF64748B)
                                )
                            },
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF475569),
                                unfocusedTextColor = Color(0xFF475569),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = TravelBlue,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_participant_input")
                        )
                        Button(
                            onClick = {
                                if (newParticipantName.trim().isNotEmpty()) {
                                    onAddParticipant(newParticipantName.trim())
                                    newParticipantName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TravelBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        participants.forEach { p ->
                            val isEditingThis = editingParticipantId == p.id

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (isEditingThis) {
                                        OutlinedTextField(
                                            value = editingNameText,
                                            onValueChange = { editingNameText = it },
                                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color(0xFF475569),
                                                unfocusedTextColor = Color(0xFF475569),
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Color.White,
                                                focusedBorderColor = TravelBlue,
                                                unfocusedBorderColor = Color(0xFFCBD5E1)
                                            ),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                if (editingNameText.trim().isNotEmpty()) {
                                                    onUpdateParticipantName(p, editingNameText.trim())
                                                }
                                                editingParticipantId = null
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = EmeraldGreenDark
                                            )
                                        }
                                        IconButton(onClick = { editingParticipantId = null }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = null,
                                                tint = Color(0xFF64748B)
                                            )
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            com.example.ui.components.EditableParticipantAvatar(
                                                name = p.name,
                                                avatarColorHex = p.avatarColor,
                                                photoUri = p.photoUri,
                                                onClick = { onParticipantAvatarClick(p) },
                                                size = 36.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = p.name,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = OceanNavy
                                                )
                                                Text(
                                                    text = tapIconToAddPhotoText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }

                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingParticipantId = p.id
                                                    editingNameText = p.name
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = TravelBlue,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                            if (participants.size > 1) {
                                                IconButton(
                                                    onClick = { onDeleteParticipant(p) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = null,
                                                        tint = CoralRed,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(160.dp))
        }
    }

    if (showSettleDebtsModal) {
        SettleDebtsModal(
            trip = trip,
            debtTransactions = debtResult.transactions,
            currentLang = currentLang,
            onDismiss = { showSettleDebtsModal = false },
            onConfirmSettlement = onSettleDebt
        )
    }
}

@Composable
private fun SettlementTransactionCard(
    tx: DebtTransaction,
    baseCurrency: String,
    isExpanded: Boolean,
    debtResult: DebtCalculationResult,
    participants: List<ParticipantEntity>,
    expenses: List<ExpenseEntity>,
    incomes: List<IncomeEntity>,
    currentLang: AppLanguage,
    onClick: () -> Unit
) {
    val texts = UiTexts.get(currentLang)
    val participantMap = remember(participants) { participants.associateBy { it.id } }

    val subjectParticipantId = if (tx.isFromBote) tx.toParticipantId else tx.fromParticipantId
    val subjectBalance = debtResult.participantBalances.find { it.participantId == subjectParticipantId }
    val fromDisplayName = if (tx.isFromBote) texts.commonPotName else tx.fromName

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("settlement_tx_${tx.fromParticipantId}_${tx.toParticipantId}"),
        shape = RoundedCornerShape(14.dp),
        color = if (tx.isFromBote) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
        border = BorderStroke(
            1.5.dp,
            if (tx.isFromBote) EmeraldGreen else TravelBlue.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (tx.isFromBote) "🐷 $fromDisplayName" else "👤 $fromDisplayName",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (tx.isFromBote) EmeraldGreenDark else CoralRedDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = when (currentLang) {
                            AppLanguage.ES -> "paga a"
                            AppLanguage.EN -> "pays to"
                            AppLanguage.FR -> "paie à"
                            AppLanguage.IT -> "paga a"
                            AppLanguage.ZH -> "支付给"
                            AppLanguage.NL -> "betaalt aan"
                            AppLanguage.DE -> "zahlt an"
                            AppLanguage.PL -> "płaci do"
                        },
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "👤 ${tx.toName}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = FormatUtils.formatCurrency(tx.amount, baseCurrency, currentLang.locale),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        ),
                        color = if (tx.isFromBote) EmeraldGreenDark else TravelBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = when (currentLang) {
                            AppLanguage.ES -> "Ver detalle"
                            AppLanguage.EN -> "View detail"
                            AppLanguage.FR -> "Voir les détails"
                            AppLanguage.IT -> "Visualizza dettagli"
                            AppLanguage.ZH -> "查看详情"
                            AppLanguage.NL -> "Details bekijken"
                            AppLanguage.DE -> "Details anzeigen"
                            AppLanguage.PL -> "Pokaż szczegóły"
                        },
                        tint = Color(0xFF64748B)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded && subjectBalance != null) {
                subjectBalance?.let { sb ->
                    val subjectExpenses = expenses.filter { exp ->
                        exp.splits.any { it.participantId == sb.participantId && it.amount > 0.0 } ||
                                exp.paidById == sb.participantId
                    }.sortedWith(compareBy<ExpenseEntity> { FormatUtils.parseFlexibleDateToIso(it.date) }.thenBy { it.createdAt })

                    val subjectIncomes = incomes.mapNotNull { inc ->
                        val c = inc.contributions.find { it.participantId == sb.participantId }
                        if (c != null) inc to c else null
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .background(Color.White, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (tx.isFromBote) {
                                when (currentLang) {
                                    AppLanguage.ES -> "📊 Resumen Matemático del Acreedor (${sb.participantName}) que cobra del Bote Común:"
                                    AppLanguage.EN -> "📊 Mathematical Summary for Creditor (${sb.participantName}) receiving from Common Pot:"
                                    AppLanguage.FR -> "📊 Résumé Mathématique du Créancier (${sb.participantName}) percevant de la Cagnotte :"
                                    AppLanguage.IT -> "📊 Riepilogo Matematico per il Creditore (${sb.participantName}) che riceve dalla Cassa Comune:"
                                    AppLanguage.ZH -> "📊 从共同基金收款的债权人 (${sb.participantName}) 数学汇总："
                                    AppLanguage.NL -> "📊 Wiskundig overzicht voor schuldeiser (${sb.participantName}) die ontvangt uit Gemeenschappelijke Pot:"
                                    AppLanguage.DE -> "📊 Mathematische Zusammenfassung für Gläubiger (${sb.participantName}) aus der Gemeinschaftskasse:"
                                    AppLanguage.PL -> "📊 Podsumowanie matematyczne dla wierzyciela (${sb.participantName}) ze Wspólnej kasy:"
                                }
                            } else {
                                when (currentLang) {
                                    AppLanguage.ES -> "📊 Resumen Matemático de ${sb.participantName}:"
                                    AppLanguage.EN -> "📊 Mathematical Summary for ${sb.participantName}:"
                                    AppLanguage.FR -> "📊 Résumé Mathématique pour ${sb.participantName} :"
                                    AppLanguage.IT -> "📊 Riepilogo Matematico per ${sb.participantName}:"
                                    AppLanguage.ZH -> "📊 ${sb.participantName} 的数学汇总："
                                    AppLanguage.NL -> "📊 Wiskundig overzicht voor ${sb.participantName}:"
                                    AppLanguage.DE -> "📊 Mathematische Zusammenfassung für ${sb.participantName}:"
                                    AppLanguage.PL -> "📊 Podsumowanie matematyczne dla ${sb.participantName}:"
                                }
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = OceanNavy
                        )

                        // Cajas del resumen matemático
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                MathRow(
                                    label = when (currentLang) {
                                        AppLanguage.ES -> "Gastos que le correspondían asumir:"
                                        AppLanguage.EN -> "Expenses assigned to them:"
                                        AppLanguage.FR -> "Dépenses qui lui incombaient :"
                                        AppLanguage.IT -> "Spese a suo carico:"
                                        AppLanguage.ZH -> "应承担的支出："
                                        AppLanguage.NL -> "Toegewezen uitgaven:"
                                        AppLanguage.DE -> "Zugewiesene Ausgaben:"
                                        AppLanguage.PL -> "Przypisane wydatki:"
                                    },
                                    value = FormatUtils.formatCurrency(sb.totalExpensesOwed, baseCurrency, currentLang.locale),
                                    valueColor = OceanNavy
                                )
                                MathRow(
                                    label = when (currentLang) {
                                        AppLanguage.ES -> "Gastos pagados de su bolsillo:"
                                        AppLanguage.EN -> "Expenses paid out of pocket:"
                                        AppLanguage.FR -> "Dépenses payées de sa poche :"
                                        AppLanguage.IT -> "Spese pagate di tasca propria:"
                                        AppLanguage.ZH -> "自费支付的支出："
                                        AppLanguage.NL -> "Uit eigen zak betaalde uitgaven:"
                                        AppLanguage.DE -> "Aus eigener Tasche bezahlt:"
                                        AppLanguage.PL -> "Wydatki opłacone z własnej kieszeni:"
                                    },
                                    value = FormatUtils.formatCurrency(sb.totalExpensesPaid, baseCurrency, currentLang.locale),
                                    valueColor = TravelBlue
                                )
                                MathRow(
                                    label = when (currentLang) {
                                        AppLanguage.ES -> "Dinero que ya puso en el bote:"
                                        AppLanguage.EN -> "Money already put in pot:"
                                        AppLanguage.FR -> "Argent déjà versé dans la cagnotte :"
                                        AppLanguage.IT -> "Denaro già versato nella cassa:"
                                        AppLanguage.ZH -> "已投入共同基金的金额："
                                        AppLanguage.NL -> "Geld reeds in de pot gestopt:"
                                        AppLanguage.DE -> "Bereits in die Kasse eingezahltes Geld:"
                                        AppLanguage.PL -> "Pieniądze już wpłacone do wspólnej kasy:"
                                    },
                                    value = "-${FormatUtils.formatCurrency(sb.totalBoteContributed, baseCurrency, currentLang.locale)}",
                                    valueColor = EmeraldGreenDark
                                )
                                if (sb.totalDebtDiscountCompensated > 0.009) {
                                    MathRow(
                                        label = when (currentLang) {
                                            AppLanguage.ES -> "Compensado al poner bote:"
                                            AppLanguage.EN -> "Compensated when contributing to pot:"
                                            AppLanguage.FR -> "Compensé lors de la contribution :"
                                            AppLanguage.IT -> "Compensato al versamento nella cassa:"
                                            AppLanguage.ZH -> "投入基金时已补偿："
                                            AppLanguage.NL -> "Gecompenseerd bij inleg in pot:"
                                            AppLanguage.DE -> "Bei Einzahlung kompensiert:"
                                            AppLanguage.PL -> "Skompensowane przy wpłacie do kasy:"
                                        },
                                        value = FormatUtils.formatCurrency(sb.totalDebtDiscountCompensated, baseCurrency, currentLang.locale),
                                        valueColor = AmberGoldDark
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFCBD5E1))
                                val labelFinal = if (sb.netBalance >= 0) {
                                    when (currentLang) {
                                        AppLanguage.ES -> "Saldo neto a percibir:"
                                        AppLanguage.EN -> "Net balance to receive:"
                                        AppLanguage.FR -> "Solde net à percevoir :"
                                        AppLanguage.IT -> "Saldo netto da ricevere:"
                                        AppLanguage.ZH -> "应收净余额："
                                        AppLanguage.NL -> "Te ontvangen nettosaldo:"
                                        AppLanguage.DE -> "Zu erhaltender Nettosaldo:"
                                        AppLanguage.PL -> "Saldo netto do odbioru:"
                                    }
                                } else {
                                    when (currentLang) {
                                        AppLanguage.ES -> "Deuda neta pendiente:"
                                        AppLanguage.EN -> "Pending net debt:"
                                        AppLanguage.FR -> "Dette nette en attente :"
                                        AppLanguage.IT -> "Debito netto in sospeso:"
                                        AppLanguage.ZH -> "待结清净债务："
                                        AppLanguage.NL -> "Openstaande nettoschuld:"
                                        AppLanguage.DE -> "Ausstehende Nettoschuld:"
                                        AppLanguage.PL -> "Zaległy dług netto:"
                                    }
                                }
                                MathRow(
                                    label = labelFinal,
                                    value = FormatUtils.formatCurrency(abs(sb.netBalance), baseCurrency, currentLang.locale),
                                    valueColor = if (sb.netBalance >= 0) EmeraldGreenDark else CoralRedDark,
                                    isBold = true
                                )
                            }
                        }

                        // Listado desglosado de todos los gastos registrados
                        Text(
                            text = when (currentLang) {
                                AppLanguage.ES -> "🧾 Desglose de Gastos (${subjectExpenses.size}):"
                                AppLanguage.EN -> "🧾 Expense Breakdown (${subjectExpenses.size}):"
                                AppLanguage.FR -> "🧾 Détail des dépenses (${subjectExpenses.size}) :"
                                AppLanguage.IT -> "🧾 Dettaglio delle spese (${subjectExpenses.size}):"
                                AppLanguage.ZH -> "🧾 支出明细 (${subjectExpenses.size})："
                                AppLanguage.NL -> "🧾 Uitgavenoverzicht (${subjectExpenses.size}):"
                                AppLanguage.DE -> "🧾 Ausgabenaufschlüsselung (${subjectExpenses.size}):"
                                AppLanguage.PL -> "🧾 Zestawienie wydatków (${subjectExpenses.size}):"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = OceanNavy
                        )
                        subjectExpenses.forEach { exp ->
                            val payer = if (exp.paidById == DebtCalculator.BOTE_COMUN_ID) {
                                texts.commonPotName
                            } else {
                                participantMap[exp.paidById]?.name ?: texts.defaultTravelerName
                            }
                            val shareForSubject = exp.splits.find { it.participantId == sb.participantId }?.amount ?: 0.0
                            val displayCat = CategoryCatalog.displayName(exp.category, currentLang)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${exp.placeName.ifBlank { displayCat }} ($displayCat)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = OceanNavy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val paidTotalLabel = when (currentLang) {
                                        AppLanguage.ES -> "Pagó total"
                                        AppLanguage.EN -> "Paid total"
                                        AppLanguage.FR -> "A payé au total"
                                        AppLanguage.IT -> "Totale pagato"
                                        AppLanguage.ZH -> "共支付"
                                        AppLanguage.NL -> "Totaal betaald"
                                        AppLanguage.DE -> "Gesamt bezahlt"
                                        AppLanguage.PL -> "Zapłacono łącznie"
                                    }
                                    Text(
                                        text = "${FormatUtils.formatShortDate(exp.date, currentLang.locale)} · $paidTotalLabel (${FormatUtils.formatCurrency(exp.amountInBaseCurrency, baseCurrency, currentLang.locale)}): $payer",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                val shareLabel = when (currentLang) {
                                    AppLanguage.ES -> "Su parte"
                                    AppLanguage.EN -> "Share"
                                    AppLanguage.FR -> "Sa part"
                                    AppLanguage.IT -> "Sua quota"
                                    AppLanguage.ZH -> "其份额"
                                    AppLanguage.NL -> "Zijn/haar deel"
                                    AppLanguage.DE -> "Anteil"
                                    AppLanguage.PL -> "Część"
                                }
                                Text(
                                    text = "$shareLabel: ${FormatUtils.formatCurrency(shareForSubject, baseCurrency, currentLang.locale)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = CoralRedDark
                                )
                            }
                        }

                        // Desglose de aportaciones al bote realizadas
                        if (subjectIncomes.isNotEmpty()) {
                            Text(
                                text = when (currentLang) {
                                    AppLanguage.ES -> "🏺 Desglose de Aportaciones al Bote (${subjectIncomes.size}):"
                                    AppLanguage.EN -> "🏺 Pot Contributions Breakdown (${subjectIncomes.size}):"
                                    AppLanguage.FR -> "🏺 Détail des contributions à la cagnotte (${subjectIncomes.size}) :"
                                    AppLanguage.IT -> "🏺 Dettaglio versamenti alla cassa (${subjectIncomes.size}):"
                                    AppLanguage.ZH -> "🏺 共同基金出资明细 (${subjectIncomes.size})："
                                    AppLanguage.NL -> "🏺 Overzicht van inleg in de pot (${subjectIncomes.size}):"
                                    AppLanguage.DE -> "🏺 Aufschlüsselung der Kasseneinzahlungen (${subjectIncomes.size}):"
                                    AppLanguage.PL -> "🏺 Zestawienie wpłat do wspólnej kasy (${subjectIncomes.size}):"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = EmeraldGreenDark
                            )
                            val putLabel = when (currentLang) {
                                AppLanguage.ES -> "Puso"
                                AppLanguage.EN -> "Put"
                                AppLanguage.FR -> "A versé"
                                AppLanguage.IT -> "Ha versato"
                                AppLanguage.ZH -> "出资"
                                AppLanguage.NL -> "Ingelegd"
                                AppLanguage.DE -> "Eingezahlt"
                                AppLanguage.PL -> "Wpłacono"
                            }
                            subjectIncomes.forEach { (inc, contrib) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${FormatUtils.formatShortDate(inc.date, currentLang.locale)} · ${inc.description.ifBlank { texts.commonPotName }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        text = "$putLabel: +${FormatUtils.formatCurrency(contrib.amount, baseCurrency, currentLang.locale)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldGreenDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantBalanceInteractiveCard(
    balance: ParticipantBalance,
    baseCurrency: String,
    isExpanded: Boolean,
    expenses: List<ExpenseEntity>,
    currentLang: AppLanguage,
    onAvatarClick: () -> Unit,
    onClick: () -> Unit
) {
    val isPositive = balance.netBalance >= -0.004

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("participant_balance_card_${balance.participantId}"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.example.ui.components.EditableParticipantAvatar(
                        name = balance.participantName,
                        avatarColorHex = balance.avatarColor,
                        photoUri = balance.participantPhotoUri,
                        onClick = onAvatarClick,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = balance.participantName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = OceanNavy
                        )
                        Text(
                            text = if (balance.netBalance > 0.009) {
                                when (currentLang) {
                                    AppLanguage.ES -> "A percibir (se le debe)"
                                    AppLanguage.EN -> "To receive (owed)"
                                    AppLanguage.FR -> "À percevoir (on lui doit)"
                                    AppLanguage.IT -> "Da ricevere (credito)"
                                    AppLanguage.ZH -> "应收（待还）"
                                    AppLanguage.NL -> "Te ontvangen (tegoed)"
                                    AppLanguage.DE -> "Zu erhalten (Guthaben)"
                                    AppLanguage.PL -> "Do odbioru (należność)"
                                }
                            } else if (balance.netBalance < -0.009) {
                                when (currentLang) {
                                    AppLanguage.ES -> "Deuda neta (debe pagar)"
                                    AppLanguage.EN -> "Net debt (must pay)"
                                    AppLanguage.FR -> "Dette nette (doit payer)"
                                    AppLanguage.IT -> "Debito netto (deve pagare)"
                                    AppLanguage.ZH -> "净债务（须支付）"
                                    AppLanguage.NL -> "Nettoschuld (moet betalen)"
                                    AppLanguage.DE -> "Nettoschuld (muss zahlen)"
                                    AppLanguage.PL -> "Dług netto (musi zapłacić)"
                                }
                            } else {
                                when (currentLang) {
                                    AppLanguage.ES -> "En paz (0,00)"
                                    AppLanguage.EN -> "Settled (0.00)"
                                    AppLanguage.FR -> "À jour (0,00)"
                                    AppLanguage.IT -> "In pari (0,00)"
                                    AppLanguage.ZH -> "结清 (0.00)"
                                    AppLanguage.NL -> "Kwijtgescholden (0,00)"
                                    AppLanguage.DE -> "Ausgeglichen (0,00)"
                                    AppLanguage.PL -> "Rozliczone (0,00)"
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPositive) EmeraldGreenDark else CoralRedDark
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPositive) EmeraldGreenLight else CoralRedLight
                    ) {
                        Text(
                            text = FormatUtils.formatSignedCurrency(balance.netBalance, baseCurrency, currentLang.locale),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = if (isPositive) EmeraldGreenDark else CoralRedDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = when (currentLang) {
                            AppLanguage.ES -> "Expandir resumen contable"
                            AppLanguage.EN -> "Expand accounting summary"
                            AppLanguage.FR -> "Développer le résumé comptable"
                            AppLanguage.IT -> "Espandi riepilogo contabile"
                            AppLanguage.ZH -> "展开账务汇总"
                            AppLanguage.NL -> "Boekhoudkundig overzicht uitvouwen"
                            AppLanguage.DE -> "Buchhaltungsübersicht erweitern"
                            AppLanguage.PL -> "Rozwiń podsumowanie księgowe"
                        },
                        tint = Color(0xFF64748B)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MathRow(
                        label = when (currentLang) {
                            AppLanguage.ES -> "Pagado de su bolsillo:"
                            AppLanguage.EN -> "Paid out of pocket:"
                            AppLanguage.FR -> "Payé de sa poche :"
                            AppLanguage.IT -> "Pagato di tasca propria:"
                            AppLanguage.ZH -> "自费支付："
                            AppLanguage.NL -> "Uit eigen zak betaald:"
                            AppLanguage.DE -> "Aus eigener Tasche bezahlt:"
                            AppLanguage.PL -> "Zapłacono z własnej kieszeni:"
                        },
                        value = "+${FormatUtils.formatCurrency(balance.totalExpensesPaid, baseCurrency, currentLang.locale)}",
                        valueColor = TravelBlue
                    )
                    MathRow(
                        label = when (currentLang) {
                            AppLanguage.ES -> "Dinero puesto en el bote (se descuenta):"
                            AppLanguage.EN -> "Money put into pot (deducted):"
                            AppLanguage.FR -> "Argent versé dans la cagnotte (déduit) :"
                            AppLanguage.IT -> "Denaro versato nella cassa (dedotto):"
                            AppLanguage.ZH -> "投入基金的金额（已扣除）："
                            AppLanguage.NL -> "Geld in de pot gestopt (in mindering):"
                            AppLanguage.DE -> "In die Kasse eingezahlt (abgezogen):"
                            AppLanguage.PL -> "Pieniądze wpłacone do kasy (odliczone):"
                        },
                        value = "+${FormatUtils.formatCurrency(balance.totalBoteContributed, baseCurrency, currentLang.locale)}",
                        valueColor = EmeraldGreenDark
                    )
                    if (balance.totalDebtDiscountCompensated > 0.009) {
                        MathRow(
                            label = when (currentLang) {
                                AppLanguage.ES -> "Compensado al poner bote:"
                                AppLanguage.EN -> "Compensated when contributing to pot:"
                                AppLanguage.FR -> "Compensé lors de la contribution :"
                                AppLanguage.IT -> "Compensato al versamento nella cassa:"
                                AppLanguage.ZH -> "投入基金时已补偿："
                                AppLanguage.NL -> "Gecompenseerd bij inleg in pot:"
                                AppLanguage.DE -> "Bei Einzahlung kompensiert:"
                                AppLanguage.PL -> "Skompensowane przy wpłacie:"
                            },
                            value = "-${FormatUtils.formatCurrency(balance.totalDebtDiscountCompensated, baseCurrency, currentLang.locale)}",
                            valueColor = AmberGoldDark
                        )
                    }
                    MathRow(
                        label = when (currentLang) {
                            AppLanguage.ES -> "Gastos que le tocaban asumir:"
                            AppLanguage.EN -> "Assigned expense share:"
                            AppLanguage.FR -> "Part des dépenses assignées :"
                            AppLanguage.IT -> "Spese a suo carico:"
                            AppLanguage.ZH -> "应承担的支出份额："
                            AppLanguage.NL -> "Toegewezen uitgavenaandeel:"
                            AppLanguage.DE -> "Zugewiesener Ausgabenanteil:"
                            AppLanguage.PL -> "Przypisana część wydatków:"
                        },
                        value = "-${FormatUtils.formatCurrency(balance.totalExpensesOwed, baseCurrency, currentLang.locale)}",
                        valueColor = CoralRedDark
                    )
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MathRow(
                        label = when (currentLang) {
                            AppLanguage.ES -> "Fórmula Balance Neto:"
                            AppLanguage.EN -> "Net Balance Formula:"
                            AppLanguage.FR -> "Formule du solde net :"
                            AppLanguage.IT -> "Formula saldo netto:"
                            AppLanguage.ZH -> "净余额公式："
                            AppLanguage.NL -> "Formule nettosaldo:"
                            AppLanguage.DE -> "Nettosaldo-Formel:"
                            AppLanguage.PL -> "Wzór salda netto:"
                        },
                        value = FormatUtils.formatSignedCurrency(balance.netBalance, baseCurrency, currentLang.locale),
                        valueColor = if (isPositive) EmeraldGreenDark else CoralRedDark,
                        isBold = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (currentLang) {
                            AppLanguage.ES -> "Movimientos cronológicos de ${balance.participantName}:"
                            AppLanguage.EN -> "Chronological movements for ${balance.participantName}:"
                            AppLanguage.FR -> "Mouvements chronologiques de ${balance.participantName} :"
                            AppLanguage.IT -> "Movimenti cronologici di ${balance.participantName}:"
                            AppLanguage.ZH -> "${balance.participantName} 的时间顺序流水："
                            AppLanguage.NL -> "Chronologische bewegingen voor ${balance.participantName}:"
                            AppLanguage.DE -> "Chronologische Bewegungen für ${balance.participantName}:"
                            AppLanguage.PL -> "Ruchy chronologiczne dla ${balance.participantName}:"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy
                    )

                    expenses.filter { exp ->
                        exp.paidById == balance.participantId || exp.splits.any { it.participantId == balance.participantId }
                    }.sortedWith(compareBy<ExpenseEntity> { FormatUtils.parseFlexibleDateToIso(it.date) }.thenBy { it.createdAt }).forEach { exp ->
                        val paidThis = exp.paidById == balance.participantId
                        val share = exp.splits.find { it.participantId == balance.participantId }?.amount ?: 0.0
                        val displayCat = CategoryCatalog.displayName(exp.category, currentLang)
                        val paidLabel = when (currentLang) {
                            AppLanguage.ES -> "Pagó"
                            AppLanguage.EN -> "Paid"
                            AppLanguage.FR -> "A payé"
                            AppLanguage.IT -> "Ha pagato"
                            AppLanguage.ZH -> "支付了"
                            AppLanguage.NL -> "Betaalde"
                            AppLanguage.DE -> "Bezahlte"
                            AppLanguage.PL -> "Zapłacił(a)"
                        }
                        val shareLabel = when (currentLang) {
                            AppLanguage.ES -> "Cuota"
                            AppLanguage.EN -> "Share"
                            AppLanguage.FR -> "Part"
                            AppLanguage.IT -> "Quota"
                            AppLanguage.ZH -> "份额"
                            AppLanguage.NL -> "Aandeel"
                            AppLanguage.DE -> "Anteil"
                            AppLanguage.PL -> "Udział"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• ${FormatUtils.formatShortDate(exp.date, currentLang.locale)} · ${exp.placeName.ifBlank { displayCat }.take(22)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OceanNavy,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = buildString {
                                    if (paidThis) {
                                        append("$paidLabel ${FormatUtils.formatCurrency(exp.amountInBaseCurrency, baseCurrency, currentLang.locale)} · ")
                                    }
                                    append("$shareLabel: ${FormatUtils.formatCurrency(share, baseCurrency, currentLang.locale)}")
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (paidThis) TravelBlue else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MathRow(
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
            style = if (isBold) MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold) else MaterialTheme.typography.labelMedium,
            color = OceanNavy
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black) else MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = valueColor
        )
    }
}

@Composable
fun SettleDebtsModal(
    trip: TripEntity,
    debtTransactions: List<DebtTransaction>,
    currentLang: AppLanguage,
    onDismiss: () -> Unit,
    onConfirmSettlement: (DebtTransaction, Double, String) -> Unit
) {
    val texts = UiTexts.get(currentLang)
    var selectedDebt by remember { mutableStateOf<DebtTransaction?>(null) }
    var amountText by remember { mutableStateOf("") }

    val cashLabel = PaymentMethodCatalog.displayName("Efectivo", currentLang)
    val bizumLabel = PaymentMethodCatalog.displayName("Bizum / Transferencia", currentLang)
    val cardLabel = PaymentMethodCatalog.displayName("Tarjeta", currentLang)

    val paymentMethods = remember(currentLang) {
        listOf(cashLabel, bizumLabel, cardLabel)
    }
    var selectedPaymentMethod by remember(currentLang) { mutableStateOf(cashLabel) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(top = 16.dp, bottom = 16.dp)
                .imePadding(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Cabecera del modal con botón de Cerrar (y Volver si hay deuda seleccionada)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OceanNavy)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (currentLang) {
                                AppLanguage.ES -> "Saldar Deudas"
                                AppLanguage.EN -> "Settle Debts"
                                AppLanguage.FR -> "Régler les dettes"
                                AppLanguage.IT -> "Saldare i debiti"
                                AppLanguage.ZH -> "结清债务"
                                AppLanguage.NL -> "Schulden vereffenen"
                                AppLanguage.DE -> "Schulden begleichen"
                                AppLanguage.PL -> "Ureguluj długi"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (selectedDebt != null) {
                            Button(
                                onClick = { selectedDebt = null },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.22f),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (currentLang) {
                                        AppLanguage.ES -> "Volver"
                                        AppLanguage.EN -> "Back"
                                        AppLanguage.FR -> "Retour"
                                        AppLanguage.IT -> "Indietro"
                                        AppLanguage.ZH -> "返回"
                                        AppLanguage.NL -> "Terug"
                                        AppLanguage.DE -> "Zurück"
                                        AppLanguage.PL -> "Wstecz"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.22f),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = texts.closeBtn,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedDebt == null) {
                        // Vista 1: Listado de deudas pendientes
                        Text(
                            text = when (currentLang) {
                                AppLanguage.ES -> "Selecciona una deuda pendiente para liquidarla total o parcialmente. El ajuste se aplicará automáticamente a las cuentas del viaje."
                                AppLanguage.EN -> "Select a pending debt to settle it partially or in full. The balance adjustment will be automatically applied to the trip accounts."
                                AppLanguage.FR -> "Sélectionnez une dette en attente pour la régler en tout ou en partie. L'ajustement sera automatiquement appliqué aux comptes du voyage."
                                AppLanguage.IT -> "Seleziona un debito in sospeso per estinguerlo totalmente o parzialmente. La modifica verrà applicata automaticamente ai conti del viaggio."
                                AppLanguage.ZH -> "选择一笔待结清债务进行部分或全部结清。调整将自动计入旅行账目。"
                                AppLanguage.NL -> "Selecteer een openstaande schuld om deze geheel of gedeeltelijk te vereffenen. De aanpassing wordt automatisch toegepast op de reisrekeningen."
                                AppLanguage.DE -> "Wählen Sie eine ausstehende Schuld aus, um sie ganz oder teilweise zu begleichen. Die Anpassung wird automatisch übernommen."
                                AppLanguage.PL -> "Wybierz zaległy dług, aby go spłacić w całości lub częściowo. Korekta zostanie automatycznie zastosowana do kont podróży."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )

                        if (debtTransactions.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldGreenLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = when (currentLang) {
                                        AppLanguage.ES -> "✅ ¡No hay deudas pendientes en este viaje!"
                                        AppLanguage.EN -> "✅ No pending debts in this trip!"
                                        AppLanguage.FR -> "✅ Aucune dette en attente dans ce voyage !"
                                        AppLanguage.IT -> "✅ Nessun debito in sospeso in questo viaggio!"
                                        AppLanguage.ZH -> "✅ 本次旅行没有待结清的债务！"
                                        AppLanguage.NL -> "✅ Geen openstaande schulden in deze reis!"
                                        AppLanguage.DE -> "✅ Keine ausstehenden Schulden für diese Reise!"
                                        AppLanguage.PL -> "✅ Brak zaległych długów w tej podróży!"
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldGreenDark,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            debtTransactions.forEach { tx ->
                                Surface(
                                    onClick = {
                                        selectedDebt = tx
                                        amountText = String.format(java.util.Locale.US, "%.2f", tx.amount)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.2.dp, if (tx.isFromBote) EmeraldGreen else TravelBlue),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val payerName = if (tx.isFromBote) texts.commonPotName else tx.fromName
                                                Text(
                                                    text = if (tx.isFromBote) "🐷 $payerName" else "👤 $payerName",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (tx.isFromBote) EmeraldGreenDark else CoralRedDark
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    tint = Color(0xFF64748B),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "👤 ${tx.toName}",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = OceanNavy
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val pendingAmtLabel = when (currentLang) {
                                                AppLanguage.ES -> "Importe pendiente"
                                                AppLanguage.EN -> "Pending amount"
                                                AppLanguage.FR -> "Montant en attente"
                                                AppLanguage.IT -> "Importo in sospeso"
                                                AppLanguage.ZH -> "待结算金额"
                                                AppLanguage.NL -> "Openstaand bedrag"
                                                AppLanguage.DE -> "Ausstehender Betrag"
                                                AppLanguage.PL -> "Zaległa kwota"
                                            }
                                            Text(
                                                text = "$pendingAmtLabel: ${FormatUtils.formatCurrency(tx.amount, trip.baseCurrency, currentLang.locale)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = Color(0xFF475569)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                selectedDebt = tx
                                                amountText = String.format(java.util.Locale.US, "%.2f", tx.amount)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldGreen,
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = texts.btnSettle,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Vista 2: Introducir importe total o parcial y método de pago
                        val debt = selectedDebt!!

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, TravelBlue.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = when (currentLang) {
                                        AppLanguage.ES -> "Deuda a liquidar:"
                                        AppLanguage.EN -> "Debt to settle:"
                                        AppLanguage.FR -> "Dette à régler :"
                                        AppLanguage.IT -> "Debito da saldare:"
                                        AppLanguage.ZH -> "要结清的债务："
                                        AppLanguage.NL -> "Te vereffenen schuld:"
                                        AppLanguage.DE -> "Zu begleichende Schuld:"
                                        AppLanguage.PL -> "Dług do uregulowania:"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val debtPayer = if (debt.isFromBote) texts.commonPotName else debt.fromName
                                    Text(
                                        text = if (debt.isFromBote) "🐷 $debtPayer" else "👤 $debtPayer",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (debt.isFromBote) EmeraldGreenDark else CoralRedDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "👤 ${debt.toName}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = OceanNavy
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val totalDebtPending = when (currentLang) {
                                    AppLanguage.ES -> "Deuda total pendiente"
                                    AppLanguage.EN -> "Total pending debt"
                                    AppLanguage.FR -> "Dette totale en attente"
                                    AppLanguage.IT -> "Debito totale in sospeso"
                                    AppLanguage.ZH -> "待清偿债务总额"
                                    AppLanguage.NL -> "Totale openstaande schuld"
                                    AppLanguage.DE -> "Ausstehende Gesamtschuld"
                                    AppLanguage.PL -> "Całkowity zaległy dług"
                                }
                                Text(
                                    text = "$totalDebtPending: ${FormatUtils.formatCurrency(debt.amount, trip.baseCurrency, currentLang.locale)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TravelBlue
                                )
                            }
                        }

                        // Campo de importe editable (total o parcial)
                        val amountToSettleInputLabel = when (currentLang) {
                            AppLanguage.ES -> "Importe a saldar (${trip.baseCurrency})"
                            AppLanguage.EN -> "Amount to settle (${trip.baseCurrency})"
                            AppLanguage.FR -> "Montant à régler (${trip.baseCurrency})"
                            AppLanguage.IT -> "Importo da saldare (${trip.baseCurrency})"
                            AppLanguage.ZH -> "结算金额 (${trip.baseCurrency})"
                            AppLanguage.NL -> "Te vereffenen bedrag (${trip.baseCurrency})"
                            AppLanguage.DE -> "Zu begleichender Betrag (${trip.baseCurrency})"
                            AppLanguage.PL -> "Kwota do spłaty (${trip.baseCurrency})"
                        }
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = {
                                Text(amountToSettleInputLabel)
                            },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            ),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF334155),
                                unfocusedTextColor = Color(0xFF334155),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = EmeraldGreenDark,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settlement_amount_input")
                        )

                        // Botones rápidos para rellenar el importe
                        val halfLabel = when (currentLang) {
                            AppLanguage.ES -> "Mitad (50%)"
                            AppLanguage.EN -> "Half (50%)"
                            AppLanguage.FR -> "Moitié (50%)"
                            AppLanguage.IT -> "Metà (50%)"
                            AppLanguage.ZH -> "一半 (50%)"
                            AppLanguage.NL -> "Helft (50%)"
                            AppLanguage.DE -> "Hälfte (50%)"
                            AppLanguage.PL -> "Połowa (50%)"
                        }
                        val totalLabel = when (currentLang) {
                            AppLanguage.ES -> "Total (100%)"
                            AppLanguage.EN -> "Total (100%)"
                            AppLanguage.FR -> "Total (100%)"
                            AppLanguage.IT -> "Totale (100%)"
                            AppLanguage.ZH -> "全部 (100%)"
                            AppLanguage.NL -> "Totaal (100%)"
                            AppLanguage.DE -> "Gesamt (100%)"
                            AppLanguage.PL -> "Całość (100%)"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    amountText = String.format(java.util.Locale.US, "%.2f", debt.amount)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF1F5F9),
                                    contentColor = OceanNavy
                                ),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text(
                                    text = totalLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Button(
                                onClick = {
                                    val half = ((debt.amount / 2.0 * 100.0).roundToLong()) / 100.0
                                    amountText = String.format(java.util.Locale.US, "%.2f", half)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF1F5F9),
                                    contentColor = OceanNavy
                                ),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text(
                                    text = halfLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Método de pago para registrar la liquidación
                        Text(
                            text = texts.paymentMethodLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF475569)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            paymentMethods.forEach { method ->
                                val isSelected = selectedPaymentMethod == method
                                Button(
                                    onClick = { selectedPaymentMethod = method },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) TravelBlue else Color(0xFFF8FAFC),
                                        contentColor = if (isSelected) Color.White else Color(0xFF334155)
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) TravelBlue else Color(0xFFCBD5E1)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = method,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Espacio inferior para scrollear botones de liquidación (Total, Mitad, Bizum, Efectivo) por encima del teclado
                    Spacer(modifier = Modifier.height(180.dp))
                }

                // Botones inferiores estilizados como botones completos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedDebt != null) {
                        Button(
                            onClick = { selectedDebt = null },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE2E8F0),
                                contentColor = OceanNavy
                            ),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settle_debts_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = OceanNavy
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (currentLang) {
                                    AppLanguage.ES -> "Volver"
                                    AppLanguage.EN -> "Back"
                                    AppLanguage.FR -> "Retour"
                                    AppLanguage.IT -> "Indietro"
                                    AppLanguage.ZH -> "返回"
                                    AppLanguage.NL -> "Terug"
                                    AppLanguage.DE -> "Zurück"
                                    AppLanguage.PL -> "Wstecz"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = OceanNavy
                            )
                        }

                        val parsedAmt = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val isValid = parsedAmt > 0.009
                        Button(
                            onClick = {
                                if (isValid && selectedDebt != null) {
                                    onConfirmSettlement(selectedDebt!!, parsedAmt, selectedPaymentMethod)
                                    onDismiss()
                                }
                            },
                            enabled = isValid,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFCBD5E1),
                                disabledContentColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier
                                .weight(1.35f)
                                .height(48.dp)
                                .testTag("confirm_settlement_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (currentLang) {
                                    AppLanguage.ES -> "Confirmar Liquidación"
                                    AppLanguage.EN -> "Confirm Settlement"
                                    AppLanguage.FR -> "Confirmer le règlement"
                                    AppLanguage.IT -> "Conferma liquidazione"
                                    AppLanguage.ZH -> "确认结清"
                                    AppLanguage.NL -> "Vereffening bevestigen"
                                    AppLanguage.DE -> "Abrechnung bestätigen"
                                    AppLanguage.PL -> "Potwierdź rozliczenie"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE2E8F0),
                                contentColor = OceanNavy
                            ),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("settle_debts_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = OceanNavy
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = texts.closeBtn,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = OceanNavy
                            )
                        }
                    }
                }
            }
        }
    }
}
