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
import com.example.ui.theme.isEnglish
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.OceanNavy
import com.example.util.CurrencyCatalog
import com.example.util.FormatUtils
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
    onSaveIncome: (IncomeEntity, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEn = LocalAppLanguage.current.isEnglish()
    val isEdit = existingIncome != null
    val symbol = CurrencyCatalog.getSymbol(trip.baseCurrency)

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

    var selectedDate by remember {
        mutableStateOf(existingIncome?.date ?: LocalDate.now().toString())
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
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 10.dp)
                .padding(top = 8.dp, bottom = 28.dp),
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
                            text = if (isEdit) {
                                if (isEn) "Edit Common Pot" else "Editar Bote Común"
                            } else {
                                if (isEn) "Add Common Pot Income" else "Añadir Ingreso al Bote Común"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = if (isEn) "Close" else "Cerrar",
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
                    Text(
                        text = if (isEn) "Pot Calculation Mode" else "Modo de Cálculo del Bote",
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
                                text = if (isEn) "Amount per person" else "Cuantía por persona",
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
                                text = if (isEn) "Total global amount" else "Importe total global",
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
                                Text(
                                    text = if (isEn) {
                                        "Amount contributed per person ($symbol)"
                                    } else {
                                        "Cuantía que pone cada persona ($symbol)"
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = EmeraldGreenDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = basePerPersonText,
                                    onValueChange = { recalculatePerPersonQuotas(it) },
                                    label = {
                                        Text(
                                            text = if (isEn) "Fixed quota ($symbol)" else "Cuota fija ($symbol)",
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
                                Text(
                                    text = if (isEn) {
                                        "💡 If any participant is owed money for out-of-pocket expenses, it is automatically deducted from their quota (never less than 0 $symbol)."
                                    } else {
                                        "💡 Si a algún participante se le debe dinero por gastos adelantados, se le resta automáticamente de su cuota (nunca menor que 0 $symbol)."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }
                    } else {
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
                                    text = if (isEn) "Total to split ($symbol)" else "Total a repartir ($symbol)",
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEn) "Total Collected in Cash" else "Total Recaudado en Caja",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = if (isEn) "Total Pot Amount (actual cash)" else "Importe Total del Bote (efectivo real)",
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
                    Text(
                        text = if (isEn) "Breakdown by Participant (editable)" else "Desglose por Participante (editable)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = grayAmountAndNameColor
                    )
                    Text(
                        text = if (isEn) {
                            "Indicate who contributes or pays into the pot, payment method (Bizum/Cash) and amount:"
                        } else {
                            "Indica quién aporta o paga para el bote, su forma de pago (Bizum/Efectivo) y su importe:"
                        },
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
                                                    Text(
                                                        text = if (isEn) {
                                                            "Owed: +${FormatUtils.formatCurrency(deudaAFavor, trip.baseCurrency)}"
                                                        } else {
                                                            "A favor: +${FormatUtils.formatCurrency(deudaAFavor, trip.baseCurrency)}"
                                                        },
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
                                                    Text(
                                                        text = if (isEn) "Cash" else "Efectivo",
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
                                                    text = if (isEn) "Puts ($symbol)" else "Pone ($symbol)",
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
                                        val formulaText = if (isEn) {
                                            if (currentCashVal <= 0.009) {
                                                "${FormatUtils.formatCurrency(quotaValue, trip.baseCurrency)} (quota) - ${FormatUtils.formatCurrency(deudaAFavor, trip.baseCurrency)} (owed) → Puts 0 $symbol (compensates ${FormatUtils.formatCurrency(appliedDiscount, trip.baseCurrency)})"
                                            } else {
                                                "${FormatUtils.formatCurrency(quotaValue, trip.baseCurrency)} (quota) - ${FormatUtils.formatCurrency(deudaAFavor, trip.baseCurrency)} (owed) → Puts ${FormatUtils.formatCurrency(currentCashVal, trip.baseCurrency)}"
                                            }
                                        } else {
                                            if (currentCashVal <= 0.009) {
                                                "${FormatUtils.formatCurrency(quotaValue, trip.baseCurrency)} (cuantía) - ${FormatUtils.formatCurrency(deudaAFavor, trip.baseCurrency)} (se le debe) → Pone 0 $symbol (compensa ${FormatUtils.formatCurrency(appliedDiscount, trip.baseCurrency)})"
                                            } else {
                                                "${FormatUtils.formatCurrency(quotaValue, trip.baseCurrency)} (cuantía) - ${FormatUtils.formatCurrency(deudaAFavor, trip.baseCurrency)} (se le debe) → Pone ${FormatUtils.formatCurrency(currentCashVal, trip.baseCurrency)}"
                                            }
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
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = {
                            Text(
                                text = if (isEn) "Description / Pot reason" else "Descripción / Motivo del bote",
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
                                LocalDate.parse(selectedDate)
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
                            Text(
                                text = "${if (isEn) "Date" else "Fecha"}: ${FormatUtils.formatShortDate(selectedDate)}",
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
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = grayAmountAndNameColor),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isEn) "Cancel" else "Cancelar", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Button(
                        onClick = {
                            val activePartList = participants.filter { selectedMap[it.id] == true }
                            if (activePartList.isEmpty()) {
                                errorText = if (isEn) {
                                    "Select at least one participant for this pot round."
                                } else {
                                    "Selecciona al menos un participante para esta ronda de bote."
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
                                errorText = if (isEn) "Enter a valid amount greater than 0." else "Introduce una cuantía válida mayor que 0."
                                return@Button
                            }

                            val incomeEntity = IncomeEntity(
                                id = existingIncome?.id ?: "inc-${UUID.randomUUID().toString().take(8)}",
                                tripId = trip.id,
                                amount = ((totalReal * 100.0).roundToLong()) / 100.0,
                                date = selectedDate,
                                description = description.trim().ifEmpty { "Aportación al Bote Común" },
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
                            .testTag("save_income_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEn) "Save Pot" else "Guardar Bote", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}
