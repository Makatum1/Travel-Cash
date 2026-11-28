package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CategoryCatalog
import com.example.data.model.ExpenseEntity
import com.example.data.model.IncomeEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.PaymentMethodCatalog
import com.example.data.model.TripEntity
import com.example.domain.DebtCalculator
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.CoralRedDark
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.OceanNavy
import com.example.ui.theme.TravelBlue
import com.example.util.FormatUtils
import com.example.util.UiTexts
import java.time.LocalDate
import java.util.Locale

@Composable
fun ExpensesTabScreen(
    trip: TripEntity,
    participants: List<ParticipantEntity>,
    expenses: List<ExpenseEntity>,
    incomes: List<IncomeEntity> = emptyList(),
    searchQuery: String,
    selectedDateFilter: String?,
    selectedCategoryFilter: String?,
    onSearchQueryChange: (String) -> Unit,
    onDateFilterChange: (String?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onClearAllFilters: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onExpenseClick: (ExpenseEntity) -> Unit,
    onIncomeClick: ((IncomeEntity) -> Unit)? = null,
    onExportDayPdfClick: ((String, List<ExpenseEntity>) -> Unit)? = null,
    onExportDayCsvClick: ((String, List<ExpenseEntity>) -> Unit)? = null,
    topHeader: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val currentLang = LocalAppLanguage.current
    val texts = UiTexts.get(currentLang)
    val locale = currentLang.locale
    val participantMap = remember(participants) { participants.associateBy { it.id } }

    var selectedDayForDetail by remember { mutableStateOf<Pair<String, List<ExpenseEntity>>?>(null) }

    val standardNonGeneralNames = remember {
        CategoryCatalog.categories.dropLast(1).map { it.name.lowercase(Locale.ROOT) }.toSet()
    }

    // Separar gastos reales del viaje y liquidaciones de deuda (que se muestran agrupadas)
    val realExpenses = remember(expenses) {
        expenses.filter { !CategoryCatalog.isDebtSettlementExpense(it) }
    }
    val settlementExpenses = remember(expenses) {
        expenses.filter { CategoryCatalog.isDebtSettlementExpense(it) }
    }

    val isSettlementsFilterSelected = selectedCategoryFilter != null &&
        CategoryCatalog.isDebtSettlementCategory(selectedCategoryFilter)

    val settlementsLabel = when (currentLang) {
        AppLanguage.ES -> "Liquidaciones"
        AppLanguage.EN -> "Settlements"
        AppLanguage.FR -> "Règlements"
        AppLanguage.IT -> "Liquidazioni"
        AppLanguage.ZH -> "债务结算"
        AppLanguage.NL -> "Vereffeningen"
        AppLanguage.DE -> "Abrechnungen"
        AppLanguage.PL -> "Rozliczenia"
        AppLanguage.PT -> "Liquidações"
    }

    // Contadores por categoría sobre los gastos reales del viaje
    val categoryCounts = remember(realExpenses) {
        realExpenses.groupingBy { exp ->
            val catName = exp.category.trim()
            if (standardNonGeneralNames.contains(catName.lowercase(Locale.ROOT))) {
                CategoryCatalog.categories.find { it.name.equals(catName, ignoreCase = true) }?.name ?: catName
            } else {
                "General (Personalizable)"
            }
        }.eachCount()
    }

    val normalizedDateFilter = remember(selectedDateFilter) {
        selectedDateFilter?.let { FormatUtils.parseFlexibleDateToIso(it) }
    }

    // Filtrado en tiempo real combinable (texto/fecha/categoría) sobre gastos reales
    val filteredExpenses = remember(realExpenses, searchQuery, normalizedDateFilter, selectedCategoryFilter) {
        if (isSettlementsFilterSelected) {
            emptyList()
        } else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            realExpenses.filter { exp ->
                val matchesCategory = when {
                    selectedCategoryFilter == null -> true
                    selectedCategoryFilter.equals("General", ignoreCase = true) ||
                    selectedCategoryFilter.equals("General (Personalizable)", ignoreCase = true) ->
                        !standardNonGeneralNames.contains(exp.category.lowercase(Locale.ROOT))
                    else -> exp.category.equals(selectedCategoryFilter, ignoreCase = true)
                }

                val matchesDate = normalizedDateFilter == null ||
                    FormatUtils.parseFlexibleDateToIso(exp.date) == normalizedDateFilter

                val formattedDateShort = FormatUtils.formatShortDate(exp.date, locale).lowercase(Locale.ROOT)
                val matchesQuery = q.isEmpty() ||
                    exp.placeName.lowercase(Locale.ROOT).contains(q) ||
                    exp.notes.lowercase(Locale.ROOT).contains(q) ||
                    exp.category.lowercase(Locale.ROOT).contains(q) ||
                    exp.country.lowercase(Locale.ROOT).contains(q) ||
                    exp.date.lowercase(Locale.ROOT).contains(q) ||
                    formattedDateShort.contains(q)

                matchesCategory && matchesDate && matchesQuery
            }.sortedWith(
                compareByDescending<ExpenseEntity> { FormatUtils.parseFlexibleDateToIso(it.date) }
                    .thenByDescending { it.createdAt }
            )
        }
    }

    // Filtrado en tiempo real sobre liquidaciones de deudas
    val filteredSettlements = remember(
        settlementExpenses,
        searchQuery,
        normalizedDateFilter,
        selectedCategoryFilter,
        participantMap,
        settlementsLabel
    ) {
        if (selectedCategoryFilter != null && !isSettlementsFilterSelected) {
            emptyList()
        } else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            settlementExpenses.filter { exp ->
                val matchesDate = normalizedDateFilter == null ||
                    FormatUtils.parseFlexibleDateToIso(exp.date) == normalizedDateFilter

                val formattedDateShort = FormatUtils.formatShortDate(exp.date, locale).lowercase(Locale.ROOT)
                val payerName = if (exp.paidById == DebtCalculator.BOTE_COMUN_ID) {
                    texts.commonPotName
                } else {
                    participantMap[exp.paidById]?.name ?: ""
                }
                val recipientNames = exp.splits.mapNotNull { participantMap[it.participantId]?.name }.joinToString(" ")

                val matchesQuery = q.isEmpty() ||
                    settlementsLabel.lowercase(Locale.ROOT).contains(q) ||
                    exp.placeName.lowercase(Locale.ROOT).contains(q) ||
                    exp.notes.lowercase(Locale.ROOT).contains(q) ||
                    exp.date.lowercase(Locale.ROOT).contains(q) ||
                    formattedDateShort.contains(q) ||
                    payerName.lowercase(Locale.ROOT).contains(q) ||
                    recipientNames.lowercase(Locale.ROOT).contains(q)

                matchesDate && matchesQuery
            }.sortedWith(
                compareByDescending<ExpenseEntity> { FormatUtils.parseFlexibleDateToIso(it.date) }
                    .thenByDescending { it.createdAt }
            )
        }
    }

    val filteredIncomes = remember(
        incomes,
        searchQuery,
        normalizedDateFilter,
        selectedCategoryFilter,
        isSettlementsFilterSelected
    ) {
        if (selectedCategoryFilter != null && !isSettlementsFilterSelected) {
            emptyList()
        } else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            incomes.filter { inc ->
                val matchesDate = normalizedDateFilter == null ||
                    FormatUtils.parseFlexibleDateToIso(inc.date) == normalizedDateFilter

                val formattedDateShort = FormatUtils.formatShortDate(inc.date, locale).lowercase(Locale.ROOT)
                val contribNames = inc.contributions
                    .mapNotNull { participantMap[it.participantId]?.name }
                    .joinToString(" ")

                val matchesQuery = q.isEmpty() ||
                    inc.description.lowercase(Locale.ROOT).contains(q) ||
                    inc.date.lowercase(Locale.ROOT).contains(q) ||
                    formattedDateShort.contains(q) ||
                    contribNames.lowercase(Locale.ROOT).contains(q) ||
                    "bote".contains(q) ||
                    "ingreso".contains(q)

                matchesDate && matchesQuery
            }.sortedWith(
                compareByDescending<IncomeEntity> { FormatUtils.parseFlexibleDateToIso(it.date) }
            )
        }
    }

    var settlementsExpanded by remember(selectedCategoryFilter) {
        mutableStateOf(isSettlementsFilterSelected)
    }

    val groupedByDate = remember(filteredExpenses) {
        filteredExpenses.groupBy { FormatUtils.parseFlexibleDateToIso(it.date) }
    }

    val showGroupedSettlementsCard = (selectedCategoryFilter == null || isSettlementsFilterSelected) &&
        (filteredSettlements.isNotEmpty() || filteredIncomes.isNotEmpty())
    val hasAnyVisibleRecords = filteredExpenses.isNotEmpty() || showGroupedSettlementsCard

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp, start = 10.dp, end = 10.dp, top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Tarjeta simplificada del Viaje Activo
            if (topHeader != null) {
                item(key = "expenses_top_header") {
                    topHeader()
                }
            }

            // 2. Barra de búsqueda compacta + Selector de fecha exacta
            item(key = "expenses_search_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Buscador compacto
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color(0xFF334155),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("expense_search_input"),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = texts.searchPlaceholder,
                                                style = TextStyle(
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 11.5.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .clickable { onSearchQueryChange("") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Botón compacto selector de fecha exacta
                    Surface(
                        onClick = {
                            val initial = try {
                                LocalDate.parse(selectedDateFilter ?: LocalDate.now().toString())
                            } catch (e: Exception) {
                                LocalDate.now()
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val picked = String.format(
                                        Locale.US,
                                        "%04d-%02d-%02d",
                                        year,
                                        month + 1,
                                        dayOfMonth
                                    )
                                    onDateFilterChange(picked)
                                },
                                initial.year,
                                initial.monthValue - 1,
                                initial.dayOfMonth
                            ).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedDateFilter != null) TravelBlue else Color.White,
                        border = BorderStroke(1.dp, if (selectedDateFilter != null) TravelBlue else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("date_filter_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (selectedDateFilter != null) Color.White else OceanNavy,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            // 3. Chip activo si hay fecha seleccionada
            if (selectedDateFilter != null) {
                item(key = "expenses_date_chip") {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Surface(
                            onClick = { onDateFilterChange(null) },
                            shape = CircleShape,
                            color = Color(0xFFE0EDFF),
                            border = BorderStroke(1.dp, TravelBlue)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📅 $dateLabel: ${FormatUtils.formatShortDate(selectedDateFilter, locale)}  ✕",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    fontSize = 10.5.sp,
                                    color = TravelBlue
                                )
                            }
                        }
                    }
                }
            }

            // 4. Botones compactos horizontales por Categoría con contador
            item(key = "expenses_category_chips") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { onCategoryFilterChange(null) },
                        shape = RoundedCornerShape(7.dp),
                        color = if (selectedCategoryFilter == null) OceanNavy else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            0.8.dp,
                            if (selectedCategoryFilter == null) OceanNavy else Color(0xFFCBD5E1)
                        )
                    ) {
                        Text(
                            text = "${texts.allFilter} (${expenses.size})",
                            fontSize = 11.sp,
                            fontWeight = if (selectedCategoryFilter == null) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedCategoryFilter == null) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    CategoryCatalog.categories.forEach { cat ->
                        val count = categoryCounts[cat.name] ?: 0
                        val isSelected = selectedCategoryFilter.equals(cat.name, ignoreCase = true) ||
                            (cat.name.startsWith("General", ignoreCase = true) && selectedCategoryFilter?.startsWith("General", ignoreCase = true) == true)
                        val displayCatName = CategoryCatalog.displayName(cat.name, currentLang)
                        Surface(
                            onClick = { onCategoryFilterChange(cat.name) },
                            shape = RoundedCornerShape(7.dp),
                            color = if (isSelected) cat.color else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                0.8.dp,
                                if (isSelected) cat.color else Color(0xFFCBD5E1)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else cat.color,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "$displayCatName ($count)",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF334155)
                                )
                            }
                        }
                    }

                    // Botón de filtro dedicado para Liquidaciones de deuda
                    val settlementChipColor = Color(0xFF0E7490)
                    Surface(
                        onClick = { onCategoryFilterChange(CategoryCatalog.DEBT_SETTLEMENT_CATEGORY) },
                        shape = RoundedCornerShape(7.dp),
                        color = if (isSettlementsFilterSelected) settlementChipColor else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            0.8.dp,
                            if (isSettlementsFilterSelected) settlementChipColor else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("filter_chip_settlements")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Euro,
                                contentDescription = null,
                                tint = if (isSettlementsFilterSelected) Color.White else settlementChipColor,
                                modifier = Modifier.size(13.dp)
                            )
                            val totalSettlementsAndIncomes = settlementExpenses.size + incomes.size
                            Text(
                                text = "$settlementsLabel ($totalSettlementsAndIncomes)",
                                fontSize = 11.sp,
                                fontWeight = if (isSettlementsFilterSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSettlementsFilterSelected) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // 5. Estado vacío o Listado agrupado por fecha + Registro agrupado de Liquidaciones
            if (!hasAnyVisibleRecords) {
                item(key = "expenses_empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (expenses.isEmpty()) texts.noExpensesRecorded else texts.noExpensesMatchFilter,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (expenses.isEmpty()) texts.noExpensesRecordedSubtitle else texts.noExpensesMatchFilterSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (expenses.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    onClick = onClearAllFilters,
                                    shape = RoundedCornerShape(10.dp),
                                    color = TravelBlue
                                ) {
                                    Text(
                                        text = texts.clearFilters,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Registro único agrupado de Liquidaciones: primero, debajo de los filtros y encima de cualquier fecha
                if (showGroupedSettlementsCard) {
                    item(key = "grouped_settlements_card") {
                        GroupedSettlementsCard(
                            title = settlementsLabel,
                            settlements = filteredSettlements,
                            incomes = filteredIncomes,
                            participantMap = participantMap,
                            baseCurrency = trip.baseCurrency,
                            currentLang = currentLang,
                            isExpanded = settlementsExpanded,
                            onToggleExpand = { settlementsExpanded = !settlementsExpanded },
                            onSettlementClick = onExpenseClick,
                            onIncomeClick = onIncomeClick
                        )
                    }
                }

                groupedByDate.forEach { (dateIso, dayExpenses) ->
                    val dayTotal = dayExpenses.sumOf { it.amountInBaseCurrency }
                    item(key = "header-$dateIso") {
                        DayStickyHeader(
                            dateIso = dateIso,
                            expenseCount = dayExpenses.size,
                            dayTotal = dayTotal,
                            baseCurrency = trip.baseCurrency,
                            currentLang = currentLang,
                            onClick = {
                                selectedDayForDetail = Pair(dateIso, dayExpenses)
                            }
                        )
                    }
                    items(dayExpenses, key = { it.id }) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            baseCurrency = trip.baseCurrency,
                            payerName = if (expense.paidById == DebtCalculator.BOTE_COMUN_ID) {
                                texts.commonPotName
                            } else {
                                participantMap[expense.paidById]?.name ?: texts.defaultTravelerName
                            },
                            currentLang = currentLang,
                            onClick = { onExpenseClick(expense) }
                        )
                    }
                }
            }
        }
    }

    selectedDayForDetail?.let { (dateIso, dayExps) ->
        DayExpensesDetailModal(
            dateIso = dateIso,
            expenses = dayExps,
            trip = trip,
            participantMap = participantMap,
            currentLang = currentLang,
            onDismiss = { selectedDayForDetail = null },
            onExpenseClick = { exp ->
                selectedDayForDetail = null
                onExpenseClick(exp)
            },
            onExportPdfClick = if (onExportDayPdfClick != null) {
                { onExportDayPdfClick(dateIso, dayExps) }
            } else null,
            onExportCsvClick = if (onExportDayCsvClick != null) {
                { onExportDayCsvClick(dateIso, dayExps) }
            } else null
        )
    }
}

@Composable
private fun DayStickyHeader(
    dateIso: String,
    expenseCount: Int,
    dayTotal: Double,
    baseCurrency: String,
    currentLang: AppLanguage,
    onClick: () -> Unit
) {
    val locale = currentLang.locale
    val tapHint = when (currentLang) {
        AppLanguage.ES -> "Toca para ver y exportar día"
        AppLanguage.EN -> "Tap to view & export day"
        AppLanguage.FR -> "Appuyez pour voir et exporter"
        AppLanguage.IT -> "Tocca per vedere ed esportare"
        AppLanguage.ZH -> "点击查看并导出单日"
        AppLanguage.NL -> "Tik om te bekijken & exporteren"
        AppLanguage.DE -> "Tippen zum Anzeigen & Exportieren"
        AppLanguage.PL -> "Dotknij, aby zobaczyć i eksportować"
        AppLanguage.PT -> "Toque para ver e exportar dia"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFE2E8F0).copy(alpha = 0.85f),
        border = BorderStroke(0.8.dp, Color(0xFFCBD5E1))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = FormatUtils.formatLongDate(dateIso, locale),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = OceanNavy
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when (currentLang) {
                            AppLanguage.ES -> if (expenseCount == 1) "1 gasto registrado" else "$expenseCount gastos registrados"
                            AppLanguage.EN -> if (expenseCount == 1) "1 expense recorded" else "$expenseCount expenses recorded"
                            AppLanguage.FR -> if (expenseCount == 1) "1 dépense enregistrée" else "$expenseCount dépenses enregistrées"
                            AppLanguage.IT -> if (expenseCount == 1) "1 spesa registrata" else "$expenseCount spese registrate"
                            AppLanguage.ZH -> if (expenseCount == 1) "1 笔已记录支出" else "$expenseCount 笔已记录支出"
                            AppLanguage.NL -> if (expenseCount == 1) "1 geregistreerde uitgave" else "$expenseCount geregistreerde uitgaven"
                            AppLanguage.DE -> if (expenseCount == 1) "1 erfasste Ausgabe" else "$expenseCount erfasste Ausgaben"
                            AppLanguage.PL -> if (expenseCount == 1) "1 zarejestrowany wydatek" else "$expenseCount zarejestrowanych wydatków"
                            AppLanguage.PT -> if (expenseCount == 1) "1 despesa registada" else "$expenseCount despesas registadas"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = tapHint,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                        color = TravelBlue
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = FormatUtils.formatCurrency(dayTotal, baseCurrency, locale),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = OceanNavy
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = OceanNavy.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun DayExpensesDetailModal(
    dateIso: String,
    expenses: List<ExpenseEntity>,
    trip: TripEntity,
    participantMap: Map<String, ParticipantEntity>,
    currentLang: AppLanguage,
    onDismiss: () -> Unit,
    onExpenseClick: (ExpenseEntity) -> Unit,
    onExportPdfClick: (() -> Unit)?,
    onExportCsvClick: (() -> Unit)?
) {
    val locale = currentLang.locale
    val texts = UiTexts.get(currentLang)
    val dayTotal = expenses.sumOf { it.amountInBaseCurrency }
    val formattedDateLong = FormatUtils.formatLongDate(dateIso, locale)

    val modalSubtitle = when (currentLang) {
        AppLanguage.ES -> "Total del día: ${FormatUtils.formatCurrency(dayTotal, trip.baseCurrency, locale)} • ${if (expenses.size == 1) "1 gasto" else "${expenses.size} gastos"}"
        AppLanguage.EN -> "Day total: ${FormatUtils.formatCurrency(dayTotal, trip.baseCurrency, locale)} • ${if (expenses.size == 1) "1 expense" else "${expenses.size} expenses"}"
        AppLanguage.FR -> "Total du jour : ${FormatUtils.formatCurrency(dayTotal, trip.baseCurrency, locale)} • ${if (expenses.size == 1) "1 dépense" else "${expenses.size} dépenses"}"
        AppLanguage.IT -> "Totale del giorno: ${FormatUtils.formatCurrency(dayTotal, trip.baseCurrency, locale)} • ${if (expenses.size == 1) "1 spesa" else "${expenses.size} spese"}"
        AppLanguage.ZH -> "当日总计：${FormatUtils.formatCurrency(dayTotal, trip.baseCurrency, locale)} • ${expenses.size} 笔支出"
        AppLanguage.NL -> "Dag totaal: ${FormatUtils.formatCurrency(dayTotal, trip.baseCurrency, locale)} • ${if (expenses.size == 1) "1 uitgave" else "${expenses.size} uitgaven"}"
        AppLanguage.DE -> "Tagessumme: ${FormatUtils.formatCurrency(dayTotal, trip.baseCurrency, locale)} • ${if (expenses.size == 1) "1 Ausgabe" else "${expenses.size} Ausgaben"}"
        AppLanguage.PL -> "Suma dnia: ${FormatUtils.formatCurrency(dayTotal, trip.baseCurrency, locale)} • ${if (expenses.size == 1) "1 wydatek" else "${expenses.size} wydatków"}"
        AppLanguage.PT -> "Total do dia: ${FormatUtils.formatCurrency(dayTotal, trip.baseCurrency, locale)} • ${if (expenses.size == 1) "1 despesa" else "${expenses.size} despesas"}"
    }

    val exportPdfBtnText = when (currentLang) {
        AppLanguage.ES -> "Exportar Día a PDF"
        AppLanguage.EN -> "Export Day to PDF"
        AppLanguage.FR -> "Exporter Jour en PDF"
        AppLanguage.IT -> "Esporta Giorno in PDF"
        AppLanguage.ZH -> "导出单日 PDF 报表"
        AppLanguage.NL -> "Exporteer dag naar PDF"
        AppLanguage.DE -> "Tag als PDF exportieren"
        AppLanguage.PL -> "Eksportuj dzień do PDF"
        AppLanguage.PT -> "Exportar Dia para PDF"
    }

    val exportCsvBtnText = when (currentLang) {
        AppLanguage.ES -> "Exportar Día a CSV"
        AppLanguage.EN -> "Export Day to CSV"
        AppLanguage.FR -> "Exporter Jour en CSV"
        AppLanguage.IT -> "Esporta Giorno in CSV"
        AppLanguage.ZH -> "导出单日 CSV"
        AppLanguage.NL -> "Exporteer dag naar CSV"
        AppLanguage.DE -> "Tag als CSV exportieren"
        AppLanguage.PL -> "Eksportuj dzień do CSV"
        AppLanguage.PT -> "Exportar Dia para CSV"
    }

    val exportDesc = when (currentLang) {
        AppLanguage.ES -> "Genera el reporte ejecutivo completo de este día con el mismo formato, gráficos y reglas que el viaje completo."
        AppLanguage.EN -> "Generates the complete executive report for this day with the same layout, charts and rules as the full trip."
        AppLanguage.FR -> "Génère le rapport exécutif complet de cette journée avec le même format, graphiques et règles que le voyage complet."
        AppLanguage.IT -> "Genera il report esecutivo completo di questo giorno con lo stesso formato, grafici e regole del viaggio completo."
        AppLanguage.ZH -> "生成包含相同格式、图表与规则的该日完整执行报表。"
        AppLanguage.NL -> "Genereert het volledige leidinggevende rapport voor deze dag met dezelfde opmaak, grafieken en regels als de hele reis."
        AppLanguage.DE -> "Erstellt den vollständigen Managementbericht für diesen Tag mit demselben Format, Diagrammen und Regeln wie die gesamte Reise."
        AppLanguage.PL -> "Generuje pełny raport wykonawczy z tego dnia z tym samym formatem, wykresami i regułami co cała podróż."
        AppLanguage.PT -> "Gera o relatório executivo completo deste dia com o mesmo formato, gráficos e regras da viagem completa."
    }

    val dayExpensesListTitle = when (currentLang) {
        AppLanguage.ES -> "Gastos registrados en esta fecha"
        AppLanguage.EN -> "Expenses recorded on this date"
        AppLanguage.FR -> "Dépenses enregistrées à cette date"
        AppLanguage.IT -> "Spese registrate in questa data"
        AppLanguage.ZH -> "该日期记录的支出"
        AppLanguage.NL -> "Op deze datum geregistreerde uitgaven"
        AppLanguage.DE -> "An diesem Datum erfasste Ausgaben"
        AppLanguage.PL -> "Wydatki zarejestrowane w tym dniu"
        AppLanguage.PT -> "Despesas registadas nesta data"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFFF8FAFC),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Cabecera superior elegante en OceanNavy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OceanNavy)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formattedDateLong,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = modalSubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = Color(0xFF93C5FD)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = texts.closeBtn,
                            tint = Color.White
                        )
                    }
                }

                // Zona de exportación destacada ARRIBA DEL TODO
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = exportDesc,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onExportPdfClick != null) {
                                Button(
                                    onClick = onExportPdfClick,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CoralRedDark,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = exportPdfBtnText,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                            }
                            if (onExportCsvClick != null) {
                                OutlinedButton(
                                    onClick = onExportCsvClick,
                                    modifier = if (onExportPdfClick != null) Modifier.weight(0.7f) else Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OceanNavy),
                                    border = BorderStroke(1.2.dp, OceanNavy),
                                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (onExportPdfClick != null) "CSV" else exportCsvBtnText,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Cabecera de la lista de gastos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dayExpensesListTitle,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        ),
                        color = OceanNavy
                    )
                    Text(
                        text = FormatUtils.formatCurrency(dayTotal, trip.baseCurrency, locale),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp
                        ),
                        color = OceanNavy
                    )
                }

                // Listado con scroll de los gastos del día
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                ) {
                    items(expenses, key = { it.id }) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            baseCurrency = trip.baseCurrency,
                            payerName = if (expense.paidById == DebtCalculator.BOTE_COMUN_ID) {
                                texts.commonPotName
                            } else {
                                participantMap[expense.paidById]?.name ?: texts.defaultTravelerName
                            },
                            currentLang = currentLang,
                            onClick = { onExpenseClick(expense) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpenseItemCard(
    expense: ExpenseEntity,
    baseCurrency: String,
    payerName: String,
    currentLang: AppLanguage,
    onClick: () -> Unit
) {
    val resolvedCategory = CategoryCatalog.resolveCategoryName(expense)
    val categoryInfo = CategoryCatalog.getCategory(resolvedCategory)
    val translatedCategory = CategoryCatalog.displayName(resolvedCategory, currentLang)
    val translatedPaymentMethod = PaymentMethodCatalog.displayName(expense.paymentMethod, currentLang)

    val perDayUnit = when (currentLang) {
        AppLanguage.ES -> "día"
        AppLanguage.EN -> "day"
        AppLanguage.FR -> "jour"
        AppLanguage.IT -> "giorno"
        AppLanguage.ZH -> "天"
        AppLanguage.NL -> "dag"
        AppLanguage.DE -> "Tag"
        AppLanguage.PL -> "dzień"
        AppLanguage.PT -> "dia"
    }

    val receiptsDesc = when (currentLang) {
        AppLanguage.ES -> "Recibos y recuerdos adjuntos"
        AppLanguage.EN -> "Attached receipts & memories"
        AppLanguage.FR -> "Reçus et souvenirs joints"
        AppLanguage.IT -> "Ricevute e ricordi allegati"
        AppLanguage.ZH -> "附加收据与照片"
        AppLanguage.NL -> "Bijgevoegde bonnen en herinneringen"
        AppLanguage.DE -> "Angehängte Belege und Erinnerungen"
        AppLanguage.PL -> "Załączone paragony i wspomnienia"
        AppLanguage.PT -> "Recibos e recordações anexas"
    }

    val geoDesc = when (currentLang) {
        AppLanguage.ES -> "Con coordenadas en mapa"
        AppLanguage.EN -> "Geolocated on map"
        AppLanguage.FR -> "Géolocalisé sur la carte"
        AppLanguage.IT -> "Geolocalizzato sulla mappa"
        AppLanguage.ZH -> "地图已定位"
        AppLanguage.NL -> "Geolocatie op kaart"
        AppLanguage.DE -> "Auf Karte geolokalisiert"
        AppLanguage.PL -> "Zlokalizowane na mapie"
        AppLanguage.PT -> "Com coordenadas no mapa"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("expense_card_${expense.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono circular con el color de su categoría
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(categoryInfo.color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryInfo.icon,
                    contentDescription = translatedCategory,
                    tint = categoryInfo.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información central (datos introducidos por el usuario se mantienen tal cual)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.placeName.ifBlank { expense.notes.ifBlank { translatedCategory } },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OceanNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (expense.country.isNotBlank()) {
                    Text(
                        text = "📍 ${expense.country}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (expense.notes.isNotBlank() && expense.placeName.isNotBlank()) {
                    Text(
                        text = expense.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Etiquetas de quién pagó, método de pago, prorrateo (al lado de la forma de pago), fotos y ubicación
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        val payerIcon = if (expense.paidById == DebtCalculator.BOTE_COMUN_ID) "🐷" else "👤"
                        val methodIcon = if (expense.paymentMethod.equals("Bote Común", ignoreCase = true)) "🐷 " else ""
                        Text(
                            text = "$payerIcon $payerName · $methodIcon$translatedPaymentMethod",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF334155),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (expense.prorationDays != null && expense.prorationDays > 1) {
                        val perDay = expense.amountInBaseCurrency / expense.prorationDays
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEDE9FE)
                        ) {
                            Text(
                                text = "📅 ${expense.prorationDays}d (${FormatUtils.formatCurrency(perDay, baseCurrency)}/$perDayUnit)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFF6D28D9),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (expense.photos.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = receiptsDesc,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${expense.photos.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    if (expense.latitude != null && expense.longitude != null) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = geoDesc,
                            tint = TravelBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Bloque derecho: Precio total y Categoría
            Column(horizontalAlignment = Alignment.End) {
                // Arriba: El precio total (en moneda base + divisa extranjera si aplica)
                Text(
                    text = FormatUtils.formatCurrency(expense.amountInBaseCurrency, baseCurrency),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    ),
                    color = OceanNavy,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
                if (!expense.currency.equals(baseCurrency, ignoreCase = true)) {
                    Text(
                        text = FormatUtils.formatCurrency(expense.amount, expense.currency),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TravelBlue,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = translatedCategory,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupedSettlementsCard(
    title: String,
    settlements: List<ExpenseEntity>,
    incomes: List<IncomeEntity> = emptyList(),
    participantMap: Map<String, ParticipantEntity>,
    baseCurrency: String,
    currentLang: AppLanguage,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSettlementClick: (ExpenseEntity) -> Unit,
    onIncomeClick: ((IncomeEntity) -> Unit)? = null
) {
    val texts = UiTexts.get(currentLang)
    val locale = currentLang.locale
    val accentColor = Color(0xFF0E7490)
    val totalSettled = settlements.sumOf { it.amountInBaseCurrency } + incomes.sumOf { it.amount }
    val count = settlements.size + incomes.size

    val countLabel = when (currentLang) {
        AppLanguage.ES -> if (count == 1) "1 movimiento · Pulsa para ver y editar" else "$count liquidaciones e ingresos · Pulsa para ver y editar"
        AppLanguage.EN -> if (count == 1) "1 transaction · Tap to view & edit" else "$count settlements & pot deposits · Tap to view & edit"
        AppLanguage.FR -> if (count == 1) "1 mouvement · Appuyer pour voir et modifier" else "$count règlements et versements · Appuyer pour voir et modifier"
        AppLanguage.IT -> if (count == 1) "1 movimento · Tocca per vedere e modificare" else "$count liquidazioni e versamenti · Tocca per vedere e modificare"
        AppLanguage.ZH -> "$count 笔结算与公账注入 · 点击查看并编辑"
        AppLanguage.NL -> if (count == 1) "1 transactie · Tik om te bekijken en bewerken" else "$count vereffeningen en potinleg · Tik om te bekijken en bewerken"
        AppLanguage.DE -> if (count == 1) "1 Buchung · Tippen zum Ansehen & Bearbeiten" else "$count Abrechnungen & Kassen-Einzahlungen · Tippen zum Ansehen & Bearbeiten"
        AppLanguage.PL -> if (count == 1) "1 operacja · Dotknij, aby zobaczyć i edytować" else "$count rozliczeń i wpłat · Dotknij, aby zobaczyć i edytować"
        AppLanguage.PT -> if (count == 1) "1 movimento · Tocar para ver e editar" else "$count liquidações e depósitos · Tocar para ver e editar"
    }

    val unifiedEntries = remember(settlements, incomes) {
        val list = mutableListOf<Any>()
        list.addAll(settlements)
        list.addAll(incomes)
        list.sortedWith(
            compareByDescending<Any> {
                when (it) {
                    is ExpenseEntity -> FormatUtils.parseFlexibleDateToIso(it.date)
                    is IncomeEntity -> FormatUtils.parseFlexibleDateToIso(it.date)
                    else -> ""
                }
            }.thenByDescending {
                when (it) {
                    is ExpenseEntity -> it.createdAt
                    else -> 0L
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .testTag("grouped_settlements_record"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.8.dp),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cabecera del registro único agrupado "Liquidaciones"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Euro,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = OceanNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = FormatUtils.formatCurrency(totalSettled, baseCurrency, locale),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.5.sp
                        ),
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Desplegable con todas las liquidaciones e ingresos individuales editables
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    unifiedEntries.forEachIndexed { idx, entry ->
                        if (entry is ExpenseEntity) {
                            val settlement = entry
                            val payerName = if (settlement.paidById == DebtCalculator.BOTE_COMUN_ID) {
                                texts.commonPotName
                            } else {
                                participantMap[settlement.paidById]?.name ?: texts.defaultTravelerName
                            }
                            val recipientNames = settlement.splits
                                .filter { it.amount > 0.004 }
                                .mapNotNull { participantMap[it.participantId]?.name }
                                .joinToString(", ")
                            val routeSubtitle = if (recipientNames.isNotBlank()) {
                                "$payerName → $recipientNames"
                            } else {
                                payerName
                            }
                            val translatedMethod = PaymentMethodCatalog.displayName(settlement.paymentMethod, currentLang)

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSettlementClick(settlement) }
                                    .testTag("settlement_item_${settlement.id}"),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = settlement.placeName.ifBlank { routeSubtitle },
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = OceanNavy,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "📅 ${FormatUtils.formatShortDate(settlement.date, locale)} · $routeSubtitle · $translatedMethod",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = FormatUtils.formatCurrency(settlement.amountInBaseCurrency, baseCurrency, locale),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                            color = accentColor
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = TravelBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        } else if (entry is IncomeEntity) {
                            val income = entry
                            val contributorsDesc = income.contributions
                                .filter { it.amount > 0.004 }
                                .mapNotNull { participantMap[it.participantId]?.name }
                                .joinToString(", ")
                            val poteTitle = income.description.ifBlank {
                                when (currentLang) {
                                    AppLanguage.ES -> "Ingreso en Bote Común"
                                    AppLanguage.EN -> "Common Pot Deposit"
                                    AppLanguage.FR -> "Versement Cagnotte Commune"
                                    AppLanguage.IT -> "Versamento Cassa Comune"
                                    AppLanguage.ZH -> "公账注入"
                                    AppLanguage.NL -> "Inleg in Gemeenschappelijke Pot"
                                    AppLanguage.DE -> "Gemeinschaftskasse-Einzahlung"
                                    AppLanguage.PL -> "Wpłata do wspólnej kasy"
                                    AppLanguage.PT -> "Depósito no Fundo Comum"
                                }
                            }
                            val potSubtitle = if (contributorsDesc.isNotBlank()) {
                                "📅 ${FormatUtils.formatShortDate(income.date, locale)} · ${texts.commonPotName} ($contributorsDesc)"
                            } else {
                                "📅 ${FormatUtils.formatShortDate(income.date, locale)} · ${texts.commonPotName}"
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onIncomeClick?.invoke(income) }
                                    .testTag("income_item_${income.id}"),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFD1FAE5))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFD1FAE5)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Savings,
                                                contentDescription = poteTitle,
                                                tint = EmeraldGreenDark,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = poteTitle,
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = OceanNavy,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFD1FAE5)
                                                ) {
                                                    Text(
                                                        text = when (currentLang) {
                                                            AppLanguage.ES -> "Bote"
                                                            AppLanguage.EN -> "Pot"
                                                            AppLanguage.FR -> "Cagnotte"
                                                            AppLanguage.IT -> "Cassa"
                                                            AppLanguage.ZH -> "公账"
                                                            AppLanguage.NL -> "Pot"
                                                            AppLanguage.DE -> "Kasse"
                                                            AppLanguage.PL -> "Kasa"
                                                            AppLanguage.PT -> "Fundo"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.sp
                                                        ),
                                                        color = EmeraldGreenDark,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = potSubtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "+${FormatUtils.formatCurrency(income.amount, baseCurrency, locale)}",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                            color = EmeraldGreenDark
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = EmeraldGreenDark,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (idx < unifiedEntries.lastIndex) {
                            HorizontalDivider(color = Color(0xFFE2E8F0).copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}
