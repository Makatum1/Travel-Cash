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
    val isEn = LocalAppLanguage.current.isEnglish()
    val participantMap = remember(participants) { participants.associateBy { it.id } }

    var newParticipantName by remember { mutableStateOf("") }
    var editingParticipantId by remember { mutableStateOf<String?>(null) }
    var editingNameText by remember { mutableStateOf("") }

    // Estados de tarjetas desplegables interactivas
    var expandedTransactionIndex by remember { mutableStateOf<Int?>(null) }
    var expandedParticipantId by remember { mutableStateOf<String?>(null) }
    var showSettleDebtsModal by remember { mutableStateOf(false) }

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
                                text = if (isEn) "Settlement of Accounts" else "Liquidación de Cuentas",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = OceanNavy
                            )
                            Text(
                                text = if (isEn) {
                                    "Tap on any payment to view its mathematical breakdown"
                                } else {
                                    "Pulsa sobre cualquier pago para ver el desglose matemático"
                                },
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
                                text = if (isEn) "Settle debts" else "Saldar deudas",
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
                                text = if (isEn) {
                                    "✅ All trip accounts are settled!"
                                } else {
                                    "✅ ¡Todas las cuentas del viaje están saldadas!"
                                },
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
                                    isEn = isEn,
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
                                text = if (isEn) "Detailed Balance by Person" else "Balance Detallado por Persona",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = OceanNavy
                            )
                            Text(
                                text = if (isEn) {
                                    "Tap a traveler to view their accounting summary & movements"
                                } else {
                                    "Pulsa un viajero para ver su resumen contable y movimientos"
                                },
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
                                isEn = isEn,
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
                                text = if (isEn) "Common Pot Contributions" else "Aportaciones al Bote Común",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = OceanNavy
                            )
                            Text(
                                text = if (isEn) {
                                    "Total collected in cash: ${FormatUtils.formatCurrency(debtResult.totalBoteCollected, trip.baseCurrency)}"
                                } else {
                                    "Total recaudado en caja: ${FormatUtils.formatCurrency(debtResult.totalBoteCollected, trip.baseCurrency)}"
                                },
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
                                text = if (isEn) "Add Pot" else "Añadir Bote",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (incomes.isEmpty()) {
                        Text(
                            text = if (isEn) {
                                "No common pot contributions have been recorded for this trip."
                            } else {
                                "No se han registrado aportaciones al bote común en este viaje."
                            },
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
                                                Text(
                                                    text = inc.description.ifBlank {
                                                        if (isEn) "Pot Contribution" else "Aportación al Bote"
                                                    },
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = OceanNavy
                                                )
                                                val methodLabel = if (inc.calculationMethod == "perPerson") {
                                                    if (isEn) "Amount per person" else "Cuantía por persona"
                                                } else {
                                                    if (isEn) "Global amount" else "Importe global"
                                                }
                                                Text(
                                                    text = "📅 ${FormatUtils.formatShortDate(inc.date)} · $methodLabel",
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
                                                        contentDescription = if (isEn) "Edit contribution" else "Editar aportación",
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
                                                        contentDescription = if (isEn) "Delete contribution" else "Eliminar aportación",
                                                        tint = CoralRed,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (inc.contributions.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            inc.contributions.forEach { c ->
                                                val pName = participantMap[c.participantId]?.name
                                                    ?: if (isEn) "Traveler" else "Viajero"
                                                val disc = c.debtDiscount ?: 0.0
                                                val lineText = if (isEn) {
                                                    if (disc > 0.009) {
                                                        "• $pName: Put ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)} (deducted ${FormatUtils.formatCurrency(disc, trip.baseCurrency)} owed in their favor)"
                                                    } else {
                                                        "• $pName: Put ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)}"
                                                    }
                                                } else {
                                                    if (disc > 0.009) {
                                                        "• $pName: Puso ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)} (descontados ${FormatUtils.formatCurrency(disc, trip.baseCurrency)} por deuda a su favor)"
                                                    } else {
                                                        "• $pName: Puso ${FormatUtils.formatCurrency(c.amount, trip.baseCurrency)}"
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
                        text = if (isEn) "Trip Participants (${participants.size})" else "Participantes del Viaje (${participants.size})",
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
                                    text = if (isEn) "New participant name..." else "Nombre del nuevo participante...",
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
                                contentDescription = if (isEn) "Add participant" else "Añadir participante"
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
                                                contentDescription = if (isEn) "Save name" else "Guardar nombre",
                                                tint = EmeraldGreenDark
                                            )
                                        }
                                        IconButton(onClick = { editingParticipantId = null }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = if (isEn) "Cancel" else "Cancelar",
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
                                                    text = if (isEn) {
                                                        "Tap icon to add photo"
                                                    } else {
                                                        "Pulsa el icono para añadir foto"
                                                    },
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
                                                    contentDescription = if (isEn) "Edit participant name" else "Editar nombre de participante",
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
                                                        contentDescription = if (isEn) "Delete participant" else "Eliminar participante",
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
            isEn = isEn,
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
    isEn: Boolean,
    onClick: () -> Unit
) {
    val participantMap = remember(participants) { participants.associateBy { it.id } }

    val subjectParticipantId = if (tx.isFromBote) tx.toParticipantId else tx.fromParticipantId
    val subjectBalance = debtResult.participantBalances.find { it.participantId == subjectParticipantId }
    val fromDisplayName = if (tx.isFromBote && isEn) "Common Pot" else tx.fromName

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
                        contentDescription = if (isEn) "pays to" else "paga a",
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
                        text = FormatUtils.formatCurrency(tx.amount, baseCurrency),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        ),
                        color = if (tx.isFromBote) EmeraldGreenDark else TravelBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isEn) "View detail" else "Ver detalle",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded && subjectBalance != null) {
                subjectBalance?.let { sb ->
                    val subjectExpenses = expenses.filter { exp ->
                        exp.splits.any { it.participantId == sb.participantId && it.amount > 0.0 } ||
                                exp.paidById == sb.participantId
                    }.sortedByDescending { it.date }

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
                                if (isEn) {
                                    "📊 Mathematical Summary for Creditor (${sb.participantName}) receiving from Common Pot:"
                                } else {
                                    "📊 Resumen Matemático del Acreedor (${sb.participantName}) que cobra del Bote Común:"
                                }
                            } else {
                                if (isEn) {
                                    "📊 Mathematical Summary for ${sb.participantName}:"
                                } else {
                                    "📊 Resumen Matemático de ${sb.participantName}:"
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
                                    label = if (isEn) "Expenses assigned to them:" else "Gastos que le correspondían asumir:",
                                    value = FormatUtils.formatCurrency(sb.totalExpensesOwed, baseCurrency),
                                    valueColor = OceanNavy
                                )
                                MathRow(
                                    label = if (isEn) "Expenses paid out of pocket:" else "Gastos pagados de su bolsillo:",
                                    value = FormatUtils.formatCurrency(sb.totalExpensesPaid, baseCurrency),
                                    valueColor = TravelBlue
                                )
                                MathRow(
                                    label = if (isEn) "Money already put in pot:" else "Dinero que ya puso en el bote:",
                                    value = "-${FormatUtils.formatCurrency(sb.totalBoteContributed, baseCurrency)}",
                                    valueColor = EmeraldGreenDark
                                )
                                if (sb.totalDebtDiscountCompensated > 0.009) {
                                    MathRow(
                                        label = if (isEn) "Compensated when contributing to pot:" else "Compensado al poner bote:",
                                        value = FormatUtils.formatCurrency(sb.totalDebtDiscountCompensated, baseCurrency),
                                        valueColor = AmberGoldDark
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFCBD5E1))
                                val labelFinal = if (sb.netBalance >= 0) {
                                    if (isEn) "Net balance to receive:" else "Saldo neto a percibir:"
                                } else {
                                    if (isEn) "Pending net debt:" else "Deuda neta pendiente:"
                                }
                                MathRow(
                                    label = labelFinal,
                                    value = FormatUtils.formatCurrency(abs(sb.netBalance), baseCurrency),
                                    valueColor = if (sb.netBalance >= 0) EmeraldGreenDark else CoralRedDark,
                                    isBold = true
                                )
                            }
                        }

                        // Listado desglosado de todos los gastos registrados
                        Text(
                            text = if (isEn) {
                                "🧾 Expense Breakdown (${subjectExpenses.size}):"
                            } else {
                                "🧾 Desglose de Gastos (${subjectExpenses.size}):"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = OceanNavy
                        )
                        subjectExpenses.forEach { exp ->
                            val payer = if (exp.paidById == DebtCalculator.BOTE_COMUN_ID) {
                                if (isEn) "Common Pot" else "Bote Común"
                            } else {
                                participantMap[exp.paidById]?.name ?: if (isEn) "Traveler" else "Viajero"
                            }
                            val shareForSubject = exp.splits.find { it.participantId == sb.participantId }?.amount ?: 0.0
                            val displayCat = CategoryCatalog.displayName(exp.category, isEn)
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
                                    Text(
                                        text = "${FormatUtils.formatShortDate(exp.date)} · ${if (isEn) "Paid total" else "Pagó total"} (${FormatUtils.formatCurrency(exp.amountInBaseCurrency, baseCurrency)}): $payer",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Text(
                                    text = "${if (isEn) "Share" else "Su parte"}: ${FormatUtils.formatCurrency(shareForSubject, baseCurrency)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = CoralRedDark
                                )
                            }
                        }

                        // Desglose de aportaciones al bote realizadas
                        if (subjectIncomes.isNotEmpty()) {
                            Text(
                                text = if (isEn) {
                                    "🏺 Pot Contributions Breakdown (${subjectIncomes.size}):"
                                } else {
                                    "🏺 Desglose de Aportaciones al Bote (${subjectIncomes.size}):"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = EmeraldGreenDark
                            )
                            subjectIncomes.forEach { (inc, contrib) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${FormatUtils.formatShortDate(inc.date)} · ${inc.description.ifBlank { if (isEn) "Pot" else "Bote" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        text = "${if (isEn) "Put" else "Puso"}: +${FormatUtils.formatCurrency(contrib.amount, baseCurrency)}",
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
    isEn: Boolean,
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
                                if (isEn) "To receive (owed)" else "A percibir (se le debe)"
                            } else if (balance.netBalance < -0.009) {
                                if (isEn) "Net debt (must pay)" else "Deuda neta (debe pagar)"
                            } else {
                                if (isEn) "Settled (0,00)" else "En paz (0,00)"
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
                            text = FormatUtils.formatSignedCurrency(balance.netBalance, baseCurrency),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = if (isPositive) EmeraldGreenDark else CoralRedDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isEn) "Expand accounting summary" else "Expandir resumen contable",
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
                        label = if (isEn) "Paid out of pocket:" else "Pagado de su bolsillo:",
                        value = "+${FormatUtils.formatCurrency(balance.totalExpensesPaid, baseCurrency)}",
                        valueColor = TravelBlue
                    )
                    MathRow(
                        label = if (isEn) "Money put into pot (deducted):" else "Dinero puesto en el bote (se descuenta):",
                        value = "+${FormatUtils.formatCurrency(balance.totalBoteContributed, baseCurrency)}",
                        valueColor = EmeraldGreenDark
                    )
                    if (balance.totalDebtDiscountCompensated > 0.009) {
                        MathRow(
                            label = if (isEn) "Compensated when contributing to pot:" else "Compensado al poner bote:",
                            value = "-${FormatUtils.formatCurrency(balance.totalDebtDiscountCompensated, baseCurrency)}",
                            valueColor = AmberGoldDark
                        )
                    }
                    MathRow(
                        label = if (isEn) "Assigned expense share:" else "Gastos que le tocaban asumir:",
                        value = "-${FormatUtils.formatCurrency(balance.totalExpensesOwed, baseCurrency)}",
                        valueColor = CoralRedDark
                    )
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MathRow(
                        label = if (isEn) "Net Balance Formula:" else "Fórmula Balance Neto:",
                        value = FormatUtils.formatSignedCurrency(balance.netBalance, baseCurrency),
                        valueColor = if (isPositive) EmeraldGreenDark else CoralRedDark,
                        isBold = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isEn) {
                            "Chronological movements for ${balance.participantName}:"
                        } else {
                            "Movimientos cronológicos de ${balance.participantName}:"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy
                    )

                    expenses.filter { exp ->
                        exp.paidById == balance.participantId || exp.splits.any { it.participantId == balance.participantId }
                    }.sortedByDescending { it.date }.forEach { exp ->
                        val paidThis = exp.paidById == balance.participantId
                        val share = exp.splits.find { it.participantId == balance.participantId }?.amount ?: 0.0
                        val displayCat = CategoryCatalog.displayName(exp.category, isEn)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• ${FormatUtils.formatShortDate(exp.date)} · ${exp.placeName.ifBlank { displayCat }.take(22)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OceanNavy,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = buildString {
                                    if (paidThis) {
                                        append(
                                            if (isEn) {
                                                "Paid ${FormatUtils.formatCurrency(exp.amountInBaseCurrency, baseCurrency)} · "
                                            } else {
                                                "Pagó ${FormatUtils.formatCurrency(exp.amountInBaseCurrency, baseCurrency)} · "
                                            }
                                        )
                                    }
                                    append(
                                        if (isEn) {
                                            "Share: ${FormatUtils.formatCurrency(share, baseCurrency)}"
                                        } else {
                                            "Cuota: ${FormatUtils.formatCurrency(share, baseCurrency)}"
                                        }
                                    )
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
    isEn: Boolean,
    onDismiss: () -> Unit,
    onConfirmSettlement: (DebtTransaction, Double, String) -> Unit
) {
    var selectedDebt by remember { mutableStateOf<DebtTransaction?>(null) }
    var amountText by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf(if (isEn) "Cash" else "Efectivo") }

    val paymentMethods = remember(isEn) {
        listOf(
            if (isEn) "Cash" else "Efectivo",
            if (isEn) "Bizum / Transfer" else "Bizum / Transferencia",
            if (isEn) "Card" else "Tarjeta"
        )
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
                            text = if (isEn) "Settle Debts" else "Saldar Deudas",
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
                                    text = if (isEn) "Back" else "Volver",
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
                                text = if (isEn) "Close" else "Cerrar",
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
                            text = if (isEn) {
                                "Select a pending debt to settle it partially or in full. The balance adjustment will be automatically applied to the trip accounts."
                            } else {
                                "Selecciona una deuda pendiente para liquidarla total o parcialmente. El ajuste se aplicará automáticamente a las cuentas del viaje."
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
                                    text = if (isEn) "✅ No pending debts in this trip!" else "✅ ¡No hay deudas pendientes en este viaje!",
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
                                                Text(
                                                    text = if (tx.isFromBote) "🐷 ${tx.fromName}" else "👤 ${tx.fromName}",
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
                                            Text(
                                                text = "${if (isEn) "Pending amount" else "Importe pendiente"}: ${FormatUtils.formatCurrency(tx.amount, trip.baseCurrency)}",
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
                                                text = if (isEn) "Settle" else "Saldar",
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
                                    text = if (isEn) "Debt to settle:" else "Deuda a liquidar:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (debt.isFromBote) "🐷 ${debt.fromName}" else "👤 ${debt.fromName}",
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
                                Text(
                                    text = "${if (isEn) "Total pending debt" else "Deuda total pendiente"}: ${FormatUtils.formatCurrency(debt.amount, trip.baseCurrency)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TravelBlue
                                )
                            }
                        }

                        // Campo de importe editable (total o parcial)
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = {
                                Text(if (isEn) "Amount to settle (${trip.baseCurrency})" else "Importe a saldar (${trip.baseCurrency})")
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
                                    text = if (isEn) "Total (100%)" else "Total (100%)",
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
                                    text = if (isEn) "Half (50%)" else "Mitad (50%)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Método de pago para registrar la liquidación
                        Text(
                            text = if (isEn) "Payment Method:" else "Método de Pago:",
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
                                text = if (isEn) "Back" else "Volver",
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
                                text = if (isEn) "Confirm Settlement" else "Confirmar Liquidación",
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
                                text = if (isEn) "Close" else "Cerrar",
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
