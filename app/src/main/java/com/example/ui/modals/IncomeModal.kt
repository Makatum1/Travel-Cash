package com.example.ui.modals

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ExpenseEntity
import com.example.data.model.IncomeContribution
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.TripEntity
import com.example.domain.DebtCalculator
import com.example.ui.theme.AmberGoldDark
import com.example.ui.theme.AmberGoldLight
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.OceanNavy
import com.example.util.CurrencyCatalog
import com.example.util.FormatUtils
import com.example.util.UiTexts
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

@Composable
fun IncomeModal(
    trip: TripEntity,
    participants: List<ParticipantEntity>,
    expenses: List<ExpenseEntity>,
    incomes: List<IncomeEntity>,
    existingIncome: IncomeEntity?,
    systemNavBarBottomPadding: Dp = 0.dp,
    onSaveIncome: (IncomeEntity, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentLang = LocalAppLanguage.current
    val texts = UiTexts.get(currentLang)
    val isEdit = existingIncome != null
    val symbol = CurrencyCatalog.getSymbol(trip.baseCurrency)

    val navBottomFromSystem = remember(context, systemNavBarBottomPadding) {
        if (systemNavBarBottomPadding > 0.dp) {
            systemNavBarBottomPadding
        } else {
            val activity = context as? androidx.activity.ComponentActivity
            val decorView = activity?.window?.decorView
            val insets = if (decorView != null) androidx.core.view.ViewCompat.getRootWindowInsets(decorView) else null
            val bottomPx = insets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
            if (bottomPx > 0) {
                (bottomPx / context.resources.displayMetrics.density).dp
            } else {
                0.dp
            }
        }
    }

    // Si los botones de navegación del sistema están activos (>= 36.dp), guarda espacio suficiente por encima (+ 36.dp).
    // Si no están en uso o están ocultos (< 36.dp), se suben a una altura cómoda de al menos 52.dp para que no queden bajos.
    val bottomSpacing = if (navBottomFromSystem >= 36.dp) {
        navBottomFromSystem + 36.dp
    } else {
        maxOf(52.dp, navBottomFromSystem + 32.dp)
    }

    val initialFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Colores grises fijos para cuantías, nombres y etiquetas (siempre visibles tanto en modo Claro como en modo Oscuro)
    val grayAmountAndNameColor = Color(0xFF475569)
    val graySecondaryLabelColor = Color(0xFF64748B)

    val grayFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = grayAmountAndNameColor,
        unfocusedTextColor = grayAmountAndNameColor,
        disabledTextColor = Color(0xFF94A3B8),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color(0xFFF1F5F9),
        focusedLabelColor = grayAmountAndNameColor,
        unfocusedLabelColor = graySecondaryLabelColor,
        disabledLabelColor = Color(0xFF94A3B8),
        focusedBorderColor = EmeraldGreenDark,
        unfocusedBorderColor = Color(0xFF94A3B8),
        disabledBorderColor = Color(0xFFCBD5E1),
        cursorColor = EmeraldGreenDark
    )

    // Calcula cuánto se le debe actualmente a cada participante (excluyendo el ingreso que se esté editando)
    val debtInFavorMap = remember(participants, expenses, incomes, existingIncome) {
        DebtCalculator.calculateCurrentDebtInFavorMap(
            participants = participants,
            expenses = expenses,
            incomes = incomes,
            excludeIncomeId = existingIncome?.id
        )
    }

    // Modo de cálculo: "perPerson" (Cuantía por persona - predeterminado) | "total" (Importe total global)
    var calculationMethod by remember {
        mutableStateOf(existingIncome?.calculationMethod ?: "perPerson")
    }

    var basePerPersonText by remember {
        mutableStateOf(
            existingIncome?.basePerPerson?.let { String.format(Locale.US, "%.2f", it) }
                ?: "50.00"
        )
    }

    var globalTotalText by remember {
        mutableStateOf(
            existingIncome?.amount?.let { String.format(Locale.US, "%.2f", it) }
                ?: ""
        )
    }

    var description by remember {
        mutableStateOf(existingIncome?.description ?: "Aportación al Bote Común")
    }

    var selectedDate by remember(existingIncome?.id, existingIncome?.date) {
        mutableStateOf(
            existingIncome?.date?.let { FormatUtils.parseFlexibleDateToIso(it) }
                ?: LocalDate.now().toString()
        )
    }

    val selectedMap = remember {
        val m = mutableStateMapOf<String, Boolean>()
        if (existingIncome != null && existingIncome.contributions.isNotEmpty()) {
            participants.forEach { p ->
                m[p.id] = existingIncome.contributions.any { it.participantId == p.id }
            }
        } else {
            participants.forEach { p -> m[p.id] = true }
        }
        m
    }

    val individualAmounts = remember {
        val m = mutableStateMapOf<String, String>()
        val initialQuota = basePerPersonText.replace(",", ".").toDoubleOrNull() ?: 50.0
        participants.forEach { p ->
            val existingContrib = existingIncome?.contributions?.find { it.participantId == p.id }
            if (existingContrib != null) {
                m[p.id] = String.format(Locale.US, "%.2f", existingContrib.amount)
            } else {
                val owed = debtInFavorMap[p.id] ?: 0.0
                val cashToPay = max(0.0, initialQuota - owed)
                m[p.id] = String.format(Locale.US, "%.2f", cashToPay)
            }
        }
        m
    }

    // Método de pago por participante al ingresar en el bote: "Bizum" o "Efectivo" (mutuamente excluyentes)
    val participantPaymentMethods = remember {
        val m = mutableStateMapOf<String, String>()
        participants.forEach { p ->
            m[p.id] = "Efectivo"
        }
        m
    }

    val individualDiscounts = remember {
        val m = mutableStateMapOf<String, Double>()
        val initialQuota = basePerPersonText.replace(",", ".").toDoubleOrNull() ?: 50.0
        participants.forEach { p ->
            val existingContrib = existingIncome?.contributions?.find { it.participantId == p.id }
            if (existingContrib != null) {
                m[p.id] = existingContrib.debtDiscount ?: 0.0
            } else {
                val owed = debtInFavorMap[p.id] ?: 0.0
                m[p.id] = min(initialQuota, owed)
            }
        }
        m
    }

    var errorText by remember { mutableStateOf<String?>(null) }

    // Función que recalcula automáticamente las aportaciones al cambiar la cuantía fija por persona
    fun recalculatePerPersonQuotas(newQuotaStr: String) {
        basePerPersonText = newQuotaStr
        val quota = newQuotaStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        participants.forEach { p ->
            if (selectedMap[p.id] == true) {
                val deudaAFavor = debtInFavorMap[p.id] ?: 0.0
                val aportacionEfectivo = max(0.0, quota - deudaAFavor)
                val descuentoAplicado = min(quota, deudaAFavor)
                individualAmounts[p.id] = String.format(Locale.US, "%.2f", aportacionEfectivo)
                individualDiscounts[p.id] = ((descuentoAplicado * 100.0).roundToLong()) / 100.0
            } else {
                individualAmounts[p.id] = "0.00"
                individualDiscounts[p.id] = 0.0
            }
        }
    }

    // Suma real en efectivo recaudada en caja de los participantes seleccionados
    val totalCashCollected = participants.sumOf { p ->
        if (selectedMap[p.id] == true) {
            max(0.0, individualAmounts[p.id]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0)
        } else 0.0
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 10.dp)
                .padding(top = 10.dp, bottom = bottomSpacing)
                .imePadding(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EmeraldGreenDark)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEdit) texts.editPotIncomeTitle else texts.potIncomeTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = texts.closeBtn,
                            tint = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SELECTOR SUPERIOR DE MODO DE CÁLCULO DEL BOTE
                    val calculationModeTitle = when (currentLang) {
                        AppLanguage.ES -> "Modo de Cálculo del Bote"
                        AppLanguage.EN -> "Pot Calculation Mode"
                        AppLanguage.FR -> "Mode de calcul de la cagnotte"
                        AppLanguage.IT -> "Modalità di calcolo della cassa"
                        AppLanguage.ZH -> "公账计算模式"
                        AppLanguage.NL -> "Berekeningsmethode pot"
                        AppLanguage.DE -> "Berechnungsmodus der Kasse"
                        AppLanguage.PL -> "Tryb obliczania budżetu"
                        AppLanguage.PT -> "Modo de Cálculo do Fundo"
                    }
                    Text(
                        text = calculationModeTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = grayAmountAndNameColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isPerPerson = calculationMethod == "perPerson"
                        Surface(
                            onClick = {
                                calculationMethod = "perPerson"
                                recalculatePerPersonQuotas(basePerPersonText)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_per_person_button"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPerPerson) EmeraldGreenDark else Color(0xFFF1F5F9),
                            border = BorderStroke(1.5.dp, if (isPerPerson) EmeraldGreenDark else Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = texts.amountPerPersonLabel,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (isPerPerson) Color.White else grayAmountAndNameColor,
                                modifier = Modifier.padding(vertical = 11.dp, horizontal = 10.dp)
                            )
                        }

                        val isGlobal = calculationMethod == "total"
                        Surface(
                            onClick = {
                                calculationMethod = "total"
                                val activeCount = participants.count { selectedMap[it.id] == true }.coerceAtLeast(1)
                                val currentGlobal = globalTotalText.replace(",", ".").toDoubleOrNull() ?: totalCashCollected
                                globalTotalText = String.format(Locale.US, "%.2f", currentGlobal)
                                val perP = currentGlobal / activeCount
                                participants.forEach { p ->
                                    if (selectedMap[p.id] == true) {
                                        individualAmounts[p.id] = String.format(Locale.US, "%.2f", perP)
                                        individualDiscounts[p.id] = 0.0
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_total_global_button"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isGlobal) EmeraldGreenDark else Color(0xFFF1F5F9),
                            border = BorderStroke(1.5.dp, if (isGlobal) EmeraldGreenDark else Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = texts.globalAmountLabel,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (isGlobal) Color.White else grayAmountAndNameColor,
                                modifier = Modifier.padding(vertical = 11.dp, horizontal = 10.dp)
                            )
                        }
                    }

                    // CAMPO DESTACADO: "Cuantía que pone cada persona (€)" JUSTO ARRIBA DEL IMPORTE TOTAL DEL BOTE
                    if (calculationMethod == "perPerson") {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.5.dp, EmeraldGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val amountPerPersonTitle = when (currentLang) {
                                    AppLanguage.ES -> "Cuantía que pone cada persona ($symbol)"
                                    AppLanguage.EN -> "Amount contributed per person ($symbol)"
                                    AppLanguage.FR -> "Montant versé par personne ($symbol)"
                                    AppLanguage.IT -> "Importo versato per persona ($symbol)"
                                    AppLanguage.ZH -> "每人出资金额 ($symbol)"
                                    AppLanguage.NL -> "Inleg per persoon ($symbol)"
                                    AppLanguage.DE -> "Beitrag pro Person ($symbol)"
                                    AppLanguage.PL -> "Kwota wpłacana przez osobę ($symbol)"
                                    AppLanguage.PT -> "Quantia que cada pessoa coloca ($symbol)"
                                }
                                Text(
                                    text = amountPerPersonTitle,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = EmeraldGreenDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val fixedQuotaLabel = when (currentLang) {
                                    AppLanguage.ES -> "Cuota fija ($symbol)"
                                    AppLanguage.EN -> "Fixed quota ($symbol)"
                                    AppLanguage.FR -> "Quote-part fixe ($symbol)"
                                    AppLanguage.IT -> "Quota fissa ($symbol)"
                                    AppLanguage.ZH -> "固定份额 ($symbol)"
                                    AppLanguage.NL -> "Vaste bijdrage ($symbol)"
                                    AppLanguage.DE -> "Feste Quote ($symbol)"
                                    AppLanguage.PL -> "Stała składka ($symbol)"
                                    AppLanguage.PT -> "Quota fixa ($symbol)"
                                }
                                OutlinedTextField(
                                    value = basePerPersonText,
                                    onValueChange = { recalculatePerPersonQuotas(it) },
                                    label = {
                                        Text(
                                            text = fixedQuotaLabel,
                                            color = graySecondaryLabelColor
                                        )
                                    },
                                    textStyle = TextStyle(
                                        color = grayAmountAndNameColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    colors = grayFieldColors,
                                    modifier = Modifier
                                        .width(160.dp)
                                        .focusRequester(initialFocusRequester)
                                        .testTag("quota_per_person_input")
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val discountHintText = when (currentLang) {
                                    AppLanguage.ES -> "💡 Si a algún participante se le debe dinero por gastos adelantados, se le resta automáticamente de su cuota (nunca menor que 0 $symbol)."
                                    AppLanguage.EN -> "💡 If any participant is owed money for out-of-pocket expenses, it is automatically deducted from their quota (never less than 0 $symbol)."
                                    AppLanguage.FR -> "💡 Si un participant a droit à un remboursement pour frais avancés, il est automatiquement déduit de sa part (jamais moins de 0 $symbol)."
                                    AppLanguage.IT -> "💡 Se a un partecipante spetta un rimborso per spese anticipate, viene automaticamente sottratto dalla sua quota (mai meno di 0 $symbol)."
                                    AppLanguage.ZH -> "💡 如果有参与者垫付了费用，将自动从其份额中扣除（绝不少于 0 $symbol）。"
                                    AppLanguage.NL -> "💡 Als een deelnemer geld tegoed heeft voor voorgeschoten uitgaven, wordt dit automatisch van diens bijdrage afgetrokken (nooit minder dan 0 $symbol)."
                                    AppLanguage.DE -> "💡 Wenn einem Teilnehmer Geld für vorgestreckte Ausgaben geschuldet wird, wird dies automatisch von seinem Beitrag abgezogen (nie weniger als 0 $symbol)."
                                    AppLanguage.PL -> "💡 Jeśli uczestnikowi należy się zwrot za wyłożone wydatki, kwota ta zostanie automatycznie odjęta od jego składki (nigdy mniej niż 0 $symbol)."
                                    AppLanguage.PT -> "💡 Se algum participante tiver dinheiro a haver por despesas adiantadas, será automaticamente deduzido da sua quota (nunca inferior a 0 $symbol)."
                                }
                                Text(
                                    text = discountHintText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }
                    } else {
                        val totalSplitLabel = when (currentLang) {
                            AppLanguage.ES -> "Total a repartir ($symbol)"
                            AppLanguage.EN -> "Total to split ($symbol)"
                            AppLanguage.FR -> "Total à répartir ($symbol)"
                            AppLanguage.IT -> "Totale da dividere ($symbol)"
                            AppLanguage.ZH -> "待分摊总额 ($symbol)"
                            AppLanguage.NL -> "Totaal te verdelen ($symbol)"
                            AppLanguage.DE -> "Zu verteilender Gesamtbetrag ($symbol)"
                            AppLanguage.PL -> "Łącznie do podziału ($symbol)"
                            AppLanguage.PT -> "Total a dividir ($symbol)"
                        }
                        OutlinedTextField(
                            value = globalTotalText,
                            onValueChange = { newVal ->
                                globalTotalText = newVal
                                val totalNum = newVal.replace(",", ".").toDoubleOrNull() ?: 0.0
                                val activeCount = participants.count { selectedMap[it.id] == true }.coerceAtLeast(1)
                                val share = totalNum / activeCount
                                participants.forEach { p ->
                                    if (selectedMap[p.id] == true) {
                                        individualAmounts[p.id] = String.format(Locale.US, "%.2f", share)
                                        individualDiscounts[p.id] = 0.0
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = totalSplitLabel,
                                    color = graySecondaryLabelColor
                                )
                            },
                            textStyle = TextStyle(
                                color = grayAmountAndNameColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = grayFieldColors,
                            modifier = Modifier.width(170.dp)
                        )
                    }

                    // CAMPO "Total Recaudado en Caja (Importe Total del Bote)" situado debajo
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = OceanNavy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val totalCollectedTitle = when (currentLang) {
                                AppLanguage.ES -> "Total Recaudado en Caja"
                                AppLanguage.EN -> "Total Collected in Cash"
                                AppLanguage.FR -> "Total collecté en caisse"
                                AppLanguage.IT -> "Totale raccolto in cassa"
                                AppLanguage.ZH -> "实收现金总计"
                                AppLanguage.NL -> "Totaal opgehaald in kas"
                                AppLanguage.DE -> "In Kasse gesammelt"
                                AppLanguage.PL -> "Łącznie zebrane w kasie"
                                AppLanguage.PT -> "Total Angariado em Caixa"
                            }
                            val totalCollectedSubtitle = when (currentLang) {
                                AppLanguage.ES -> "Importe Total del Bote (efectivo real)"
                                AppLanguage.EN -> "Total Pot Amount (actual cash)"
                                AppLanguage.FR -> "Montant total de la cagnotte (espèces réelles)"
                                AppLanguage.IT -> "Importo totale cassa (denaro effettivo)"
                                AppLanguage.ZH -> "公账总金额（实际资金）"
                                AppLanguage.NL -> "Totaalbedrag van de pot (werkelijk geld)"
                                AppLanguage.DE -> "Gesamter Kassenbetrag (tatsächliches Geld)"
                                AppLanguage.PL -> "Całkowita kwota budżetu (rzeczywista gotówka)"
                                AppLanguage.PT -> "Montante Total do Fundo (dinheiro real)"
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = totalCollectedTitle,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = totalCollectedSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                text = FormatUtils.formatCurrency(totalCashCollected, trip.baseCurrency),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp
                                ),
                                color = Color(0xFF34D399),
                                modifier = Modifier.testTag("total_bote_collected_value")
                            )
                        }
                    }

                    // DESGLOSE POR PARTICIPANTE CON REGLA DE ORO Y ETIQUETA ÁMBAR
                    val breakdownTitle = when (currentLang) {
                        AppLanguage.ES -> "Desglose por Participante (editable)"
                        AppLanguage.EN -> "Breakdown by Participant (editable)"
                        AppLanguage.FR -> "Ventilation par participant (modifiable)"
                        AppLanguage.IT -> "Ripartizione per partecipante (modificabile)"
                        AppLanguage.ZH -> "参与者明细（可编辑）"
                        AppLanguage.NL -> "Uitsplitsing per deelnemer (bewerkbaar)"
                        AppLanguage.DE -> "Aufteilung nach Teilnehmer (bearbeitbar)"
                        AppLanguage.PL -> "Podział według uczestników (edytowalny)"
                        AppLanguage.PT -> "Desdobramento por Participante (editável)"
                    }
                    val breakdownSubtitle = when (currentLang) {
                        AppLanguage.ES -> "Indica quién aporta o paga para el bote, su forma de pago (Bizum/Efectivo) y su importe:"
                        AppLanguage.EN -> "Indicate who contributes or pays into the pot, payment method (Bizum/Cash) and amount:"
                        AppLanguage.FR -> "Indiquez qui contribue ou verse dans la cagnotte, son mode de paiement (Bizum/Espèces) et son montant :"
                        AppLanguage.IT -> "Indica chi versa o contribuisce alla cassa, il metodo di pagamento (Bizum/Contanti) e l'importo:"
                        AppLanguage.ZH -> "注明谁向公账出资、付款方式（Bizum/现金）及出资金额："
                        AppLanguage.NL -> "Geef aan wie bijdraagt aan de pot, de betaalmethode (Bizum/Contant) en het bedrag:"
                        AppLanguage.DE -> "Geben Sie an, wer in die Kasse einzahlt, die Zahlungsmethode (Bizum/Bar) und den Betrag:"
                        AppLanguage.PL -> "Wskaż, kto wpłaca na wspólny budżet, metodę płatności (Bizum/Gotówka) oraz kwotę:"
                        AppLanguage.PT -> "Indique quem contribui ou paga para o fundo, a sua forma de pagamento (Bizum/Numerário) e a respetiva quantia:"
                    }
                    Text(
                        text = breakdownTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = grayAmountAndNameColor
                    )
                    Text(
                        text = breakdownSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = graySecondaryLabelColor
                    )

                    val quotaValue = basePerPersonText.replace(",", ".").toDoubleOrNull() ?: 0.0

                    participants.forEach { p ->
                        val isIncluded = selectedMap[p.id] == true
                        val deudaAFavor = debtInFavorMap[p.id] ?: 0.0
                        val currentCashVal = individualAmounts[p.id]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                        val appliedDiscount = individualDiscounts[p.id] ?: 0.0

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isIncluded) Color(0xFFF8FAFC) else Color(0xFFF1F5F9).copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (deudaAFavor > 0.009 && calculationMethod == "perPerson") Color(0xFFF59E0B) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                                checked = isIncluded,
                                                onCheckedChange = { checked ->
                                                    selectedMap[p.id] = checked
                                                    if (checked && calculationMethod == "perPerson") {
                                                        recalculatePerPersonQuotas(basePerPersonText)
                                                    } else {
                                                        if (!checked) {
                                                            individualAmounts[p.id] = "0.00"
                                                        }
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = EmeraldGreenDark,
                                                    uncheckedColor = graySecondaryLabelColor,
                                                    checkmarkColor = Color.White
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Column {
                                                Text(
                                                    text = p.name,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    ),
                                                    color = grayAmountAndNameColor,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                if (deudaAFavor > 0.009) {
                                                    val owedPrefix = when (currentLang) {
                                                        AppLanguage.ES -> "A favor: +"
                                                        AppLanguage.EN -> "Owed: +"
                                                        AppLanguage.FR -> "À percevoir : +"
                                                        AppLanguage.IT -> "A favore: +"
                                                        AppLanguage.ZH -> "待收：+"
                                                        AppLanguage.NL -> "Tegoed: +"
                                                        AppLanguage.DE -> "Zugunsten: +"
                                                        AppLanguage.PL -> "Na korzyść: +"
                                                        AppLanguage.PT -> "A haver: +"
                                                    }
                                                    Text(
                                                        text = "$owedPrefix${FormatUtils.formatCurrency(deudaAFavor, trip.baseCurrency)}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                        color = EmeraldGreenDark
                                                    )
                                                }
                                            }
                                        }

                                        // Botones de Bizum y Efectivo uno encima del otro junto al cuadrado de la cuantía (sin estar pegados)
                                        val currentMethod = participantPaymentMethods[p.id] ?: "Efectivo"
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(3.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Surface(
                                                onClick = {
                                                    if (isIncluded) participantPaymentMethods[p.id] = "Bizum"
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (currentMethod == "Bizum") Color(0xFF00A9E0).copy(alpha = 0.16f) else Color(0xFFF1F5F9),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (currentMethod == "Bizum") Color(0xFF00A9E0) else Color(0xFFCBD5E1)
                                                ),
                                                modifier = Modifier.width(68.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(11.dp)
                                                            .clip(CircleShape)
                                                            .background(if (currentMethod == "Bizum") Color(0xFF00A9E0) else Color(0xFF94A3B8)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "B",
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 7.5.sp,
                                                                lineHeight = 8.sp
                                                            )
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "Bizum",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = if (currentMethod == "Bizum") FontWeight.Bold else FontWeight.Medium,
                                                            fontSize = 9.sp
                                                        ),
                                                        color = if (currentMethod == "Bizum") Color(0xFF0284C7) else Color(0xFF64748B)
                                                    )
                                                }
                                            }

                                            Surface(
                                                onClick = {
                                                    if (isIncluded) participantPaymentMethods[p.id] = "Efectivo"
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (currentMethod == "Efectivo") EmeraldGreenLight else Color(0xFFF1F5F9),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (currentMethod == "Efectivo") EmeraldGreen else Color(0xFFCBD5E1)
                                                ),
                                                modifier = Modifier.width(68.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = symbol,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 9.5.sp
                                                        ),
                                                        color = if (currentMethod == "Efectivo") EmeraldGreenDark else Color(0xFF64748B)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    val cashLabel = when (currentLang) {
                                                        AppLanguage.ES -> "Efectivo"
                                                        AppLanguage.EN -> "Cash"
                                                        AppLanguage.FR -> "Espèces"
                                                        AppLanguage.IT -> "Contanti"
                                                        AppLanguage.ZH -> "现金"
                                                        AppLanguage.NL -> "Contant"
                                                        AppLanguage.DE -> "Bargeld"
                                                        AppLanguage.PL -> "Gotówka"
                                                        AppLanguage.PT -> "Numerário"
                                                    }
                                                    Text(
                                                        text = cashLabel,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = if (currentMethod == "Efectivo") FontWeight.Bold else FontWeight.Medium,
                                                            fontSize = 9.sp
                                                        ),
                                                        color = if (currentMethod == "Efectivo") EmeraldGreenDark else Color(0xFF64748B)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        val putsLabel = when (currentLang) {
                                            AppLanguage.ES -> "Pone ($symbol)"
                                            AppLanguage.EN -> "Puts ($symbol)"
                                            AppLanguage.FR -> "Verse ($symbol)"
                                            AppLanguage.IT -> "Versa ($symbol)"
                                            AppLanguage.ZH -> "出资 ($symbol)"
                                            AppLanguage.NL -> "Legt in ($symbol)"
                                            AppLanguage.DE -> "Zahlt ($symbol)"
                                            AppLanguage.PL -> "Wpłaca ($symbol)"
                                            AppLanguage.PT -> "Coloca ($symbol)"
                                        }
                                        OutlinedTextField(
                                            value = individualAmounts[p.id] ?: "0.00",
                                            onValueChange = { newAmtStr ->
                                                individualAmounts[p.id] = newAmtStr
                                                val typed = max(0.0, newAmtStr.replace(",", ".").toDoubleOrNull() ?: 0.0)
                                                if (calculationMethod == "perPerson" && deudaAFavor > 0.009) {
                                                    individualDiscounts[p.id] = max(0.0, min(deudaAFavor, quotaValue - typed))
                                                }
                                            },
                                            enabled = isIncluded,
                                            label = {
                                                Text(
                                                    text = putsLabel,
                                                    color = graySecondaryLabelColor,
                                                    fontSize = 10.5.sp
                                                )
                                            },
                                            textStyle = TextStyle(
                                                color = grayAmountAndNameColor,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                                            ),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            colors = grayFieldColors,
                                            modifier = Modifier.width(96.dp)
                                        )
                                    }

                                // ETIQUETA ÁMBAR CON LA FÓRMULA APLICADA SI SE LE DEBE DINERO
                                if (isIncluded && calculationMethod == "perPerson" && deudaAFavor > 0.009 && quotaValue > 0.0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AmberGoldLight,
                                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val quotaWord = when (currentLang) {
                                            AppLanguage.ES -> "cuantía"
                                            AppLanguage.EN -> "quota"
                                            AppLanguage.FR -> "part"
                                            AppLanguage.IT -> "quota"
                                            AppLanguage.ZH -> "份额"
                                            AppLanguage.NL -> "bijdrage"
                                            AppLanguage.DE -> "Quote"
                                            AppLanguage.PL -> "składka"
                                            AppLanguage.PT -> "quota"
                                        }
                                        val owedWord = when (currentLang) {
                                            AppLanguage.ES -> "se le debe"
                                            AppLanguage.EN -> "owed"
                                            AppLanguage.FR -> "dû"
                                            AppLanguage.IT -> "dovuto"
                                            AppLanguage.ZH -> "垫付待收"
                                            AppLanguage.NL -> "tegoed"
                                            AppLanguage.DE -> "geschuldet"
                                            AppLanguage.PL -> "należne"
                                            AppLanguage.PT -> "a haver"
                                        }
                                        val putsWord = when (currentLang) {
                                            AppLanguage.ES -> "Pone"
                                            AppLanguage.EN -> "Puts"
                                            AppLanguage.FR -> "Verse"
                                            AppLanguage.IT -> "Versa"
                                            AppLanguage.ZH -> "出资"
                                            AppLanguage.NL -> "Legt in"
                                            AppLanguage.DE -> "Zahlt"
                                            AppLanguage.PL -> "Wpłaca"
                                            AppLanguage.PT -> "Coloca"
                                        }
                                        val compensatesWord = when (currentLang) {
                                            AppLanguage.ES -> "compensa"
                                            AppLanguage.EN -> "compensates"
                                            AppLanguage.FR -> "compense"
                                            AppLanguage.IT -> "compensa"
                                            AppLanguage.ZH -> "抵扣"
                                            AppLanguage.NL -> "compenseert"
                                            AppLanguage.DE -> "verrechnet"
                                            AppLanguage.PL -> "kompensuje"
                                            AppLanguage.PT -> "compensa"
                                        }
                                        val formulaText = if (currentCashVal <= 0.009) {
                                            "${FormatUtils.formatCurrency(quotaValue, trip.baseCurrency)} ($quotaWord) - ${FormatUtils.formatCurrency(deudaAFavor, trip.baseCurrency)} ($owedWord) → $putsWord 0 $symbol ($compensatesWord ${FormatUtils.formatCurrency(appliedDiscount, trip.baseCurrency)})"
                                        } else {
                                            "${FormatUtils.formatCurrency(quotaValue, trip.baseCurrency)} ($quotaWord) - ${FormatUtils.formatCurrency(deudaAFavor, trip.baseCurrency)} ($owedWord) → $putsWord ${FormatUtils.formatCurrency(currentCashVal, trip.baseCurrency)}"
                                        }
                                        Text(
                                            text = "⚖️ $formulaText",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = AmberGoldDark,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Concepto y Fecha de la aportación al bote
                    val potDescriptionLabel = when (currentLang) {
                        AppLanguage.ES -> "Descripción / Motivo del bote"
                        AppLanguage.EN -> "Description / Pot reason"
                        AppLanguage.FR -> "Description / Motif de la cagnotte"
                        AppLanguage.IT -> "Descrizione / Motivo della cassa"
                        AppLanguage.ZH -> "说明 / 公账事由"
                        AppLanguage.NL -> "Beschrijving / Reden van de pot"
                        AppLanguage.DE -> "Beschreibung / Grund der Kasse"
                        AppLanguage.PL -> "Opis / Powód budżetu"
                        AppLanguage.PT -> "Descrição / Motivo do fundo"
                    }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = {
                            Text(
                                text = potDescriptionLabel,
                                color = graySecondaryLabelColor
                            )
                        },
                        textStyle = TextStyle(
                            color = grayAmountAndNameColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        singleLine = true,
                        colors = grayFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        onClick = {
                            val d = try {
                                LocalDate.parse(FormatUtils.parseFlexibleDateToIso(selectedDate))
                            } catch (e: Exception) {
                                LocalDate.now()
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    selectedDate = String.format(
                                        Locale.US,
                                        "%04d-%02d-%02d",
                                        year,
                                        month + 1,
                                        dayOfMonth
                                    )
                                },
                                d.year,
                                d.monthValue - 1,
                                d.dayOfMonth
                            ).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldGreenDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            val dateLabel = when (currentLang) {
                                AppLanguage.ES -> "Fecha"
                                AppLanguage.EN -> "Date"
                                AppLanguage.FR -> "Date"
                                AppLanguage.IT -> "Data"
                                AppLanguage.ZH -> "日期"
                                AppLanguage.NL -> "Datum"
                                AppLanguage.DE -> "Datum"
                                AppLanguage.PL -> "Data"
                                AppLanguage.PT -> "Data"
                            }
                            Text(
                                text = "$dateLabel: ${FormatUtils.formatShortDate(selectedDate, currentLang.locale)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = grayAmountAndNameColor
                            )
                        }
                    }

                    if (errorText != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorText!!,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Espacio inferior para permitir scrollear todos los participantes y sus botones (Bizum/Efectivo) por encima del teclado
                    Spacer(modifier = Modifier.height(200.dp))
                }

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = grayAmountAndNameColor),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(texts.cancel, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Button(
                        onClick = {
                            val activePartList = participants.filter { selectedMap[it.id] == true }
                            if (activePartList.isEmpty()) {
                                errorText = when (currentLang) {
                                    AppLanguage.ES -> "Selecciona al menos un participante para esta ronda de bote."
                                    AppLanguage.EN -> "Select at least one participant for this pot round."
                                    AppLanguage.FR -> "Sélectionnez au moins un participant pour cette cagnotte."
                                    AppLanguage.IT -> "Seleziona almeno un partecipante per questa cassa."
                                    AppLanguage.ZH -> "请至少为此轮公账选择一名参与者。"
                                    AppLanguage.NL -> "Selecteer ten minste één deelnemer voor deze potronde."
                                    AppLanguage.DE -> "Wählen Sie mindestens einen Teilnehmer für diese Kassenrunde aus."
                                    AppLanguage.PL -> "Wybierz przynajmniej jednego uczestnika dla tej zbiórki."
                                    AppLanguage.PT -> "Selecione pelo menos um participante para esta ronda de fundo."
                                }
                                return@Button
                            }
                            val contribs = activePartList.map { p ->
                                val cashAmt = max(
                                    0.0,
                                    individualAmounts[p.id]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                                )
                                val disc = if (calculationMethod == "perPerson") {
                                    max(0.0, individualDiscounts[p.id] ?: 0.0)
                                } else 0.0
                                IncomeContribution(
                                    participantId = p.id,
                                    amount = ((cashAmt * 100.0).roundToLong()) / 100.0,
                                    debtDiscount = if (disc > 0.004) ((disc * 100.0).roundToLong()) / 100.0 else 0.0
                                )
                            }
                            val totalReal = contribs.sumOf { it.amount }
                            val totalDiscounts = contribs.sumOf { it.debtDiscount ?: 0.0 }
                            if (totalReal <= 0.0 && totalDiscounts <= 0.0) {
                                errorText = when (currentLang) {
                                    AppLanguage.ES -> "Introduce una cuantía válida mayor que 0."
                                    AppLanguage.EN -> "Enter a valid amount greater than 0."
                                    AppLanguage.FR -> "Saisissez un montant valide supérieur à 0."
                                    AppLanguage.IT -> "Inserisci un importo valido maggiore di 0."
                                    AppLanguage.ZH -> "请输入大于 0 的有效金额。"
                                    AppLanguage.NL -> "Voer een geldig bedrag in groter dan 0."
                                    AppLanguage.DE -> "Geben Sie einen gültigen Betrag größer als 0 ein."
                                    AppLanguage.PL -> "Wprowadź prawidłową kwotę większą niż 0."
                                    AppLanguage.PT -> "Introduza uma quantia válida superior a 0."
                                }
                                return@Button
                            }

                            val defaultIncomeDesc = when (currentLang) {
                                AppLanguage.ES -> "Aportación al Bote Común"
                                AppLanguage.EN -> "Contribution to Common Pot"
                                AppLanguage.FR -> "Contribution à la cagnotte commune"
                                AppLanguage.IT -> "Versamento nella cassa comune"
                                AppLanguage.ZH -> "公账出资"
                                AppLanguage.NL -> "Bijdrage aan gemeenschappelijke pot"
                                AppLanguage.DE -> "Beitrag zur Gemeinschaftskasse"
                                AppLanguage.PL -> "Wpłata do wspólnego budżetu"
                                AppLanguage.PT -> "Contribuição para o Fundo Comum"
                            }
                            val incomeEntity = IncomeEntity(
                                id = existingIncome?.id ?: "inc-${UUID.randomUUID().toString().take(8)}",
                                tripId = trip.id,
                                amount = ((totalReal * 100.0).roundToLong()) / 100.0,
                                date = FormatUtils.parseFlexibleDateToIso(selectedDate),
                                description = description.trim().ifEmpty { defaultIncomeDesc },
                                basePerPerson = if (calculationMethod == "perPerson") {
                                    basePerPersonText.replace(",", ".").toDoubleOrNull()
                                } else null,
                                calculationMethod = calculationMethod,
                                contributions = contribs
                            )
                            onSaveIncome(incomeEntity, isEdit)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenDark),
                        modifier = Modifier
                            .weight(1.35f)
                            .height(48.dp)
                            .testTag("save_income_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val savePotLabel = when (currentLang) {
                            AppLanguage.ES -> "Guardar Bote"
                            AppLanguage.EN -> "Save Pot"
                            AppLanguage.FR -> "Enregistrer la cagnotte"
                            AppLanguage.IT -> "Salva cassa"
                            AppLanguage.ZH -> "保存公账"
                            AppLanguage.NL -> "Pot opslaan"
                            AppLanguage.DE -> "Kasse speichern"
                            AppLanguage.PL -> "Zapisz budżet"
                            AppLanguage.PT -> "Guardar Fundo"
                        }
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(savePotLabel, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}
