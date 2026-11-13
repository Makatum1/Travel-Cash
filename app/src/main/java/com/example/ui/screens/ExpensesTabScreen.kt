package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.data.model.CategoryCatalog
import com.example.data.model.ExpenseEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.PaymentMethodCatalog
import com.example.data.model.TripEntity
import com.example.domain.DebtCalculator
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.OceanNavy
import com.example.ui.theme.TravelBlue
import com.example.util.FormatUtils
import java.time.LocalDate
import java.util.Locale

@Composable
fun ExpensesTabScreen(
    trip: TripEntity,
    participants: List<ParticipantEntity>,
    expenses: List<ExpenseEntity>,
    searchQuery: String,
    selectedDateFilter: String?,
    selectedCategoryFilter: String?,
    onSearchQueryChange: (String) -> Unit,
    onDateFilterChange: (String?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onClearAllFilters: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onExpenseClick: (ExpenseEntity) -> Unit,
    topHeader: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val isEn = LocalAppLanguage.current == AppLanguage.EN
    val participantMap = remember(participants) { participants.associateBy { it.id } }

    val standardFirstEightNames = remember {
        CategoryCatalog.categories.dropLast(1).map { it.name.lowercase(Locale.ROOT) }.toSet()
    }

    // Contadores por categoría sobre los gastos del viaje
    val categoryCounts = remember(expenses) {
        expenses.groupingBy { exp ->
            if (standardFirstEightNames.contains(exp.category.lowercase(Locale.ROOT))) {
                exp.category
            } else {
                "General"
            }
        }.eachCount()
    }

    // Filtrado en tiempo real combinable (texto/fecha/categoría)
    val filteredExpenses = remember(expenses, searchQuery, selectedDateFilter, selectedCategoryFilter) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        expenses.filter { exp ->
            val matchesCategory = when {
                selectedCategoryFilter == null -> true
                selectedCategoryFilter.equals("General", ignoreCase = true) ->
                    !standardFirstEightNames.contains(exp.category.lowercase(Locale.ROOT))
                else -> exp.category.equals(selectedCategoryFilter, ignoreCase = true)
            }

            val matchesDate = selectedDateFilter == null || exp.date == selectedDateFilter

            val formattedDateShort = FormatUtils.formatShortDate(exp.date).lowercase(Locale.ROOT)
            val matchesQuery = q.isEmpty() ||
                    exp.placeName.lowercase(Locale.ROOT).contains(q) ||
                    exp.notes.lowercase(Locale.ROOT).contains(q) ||
                    exp.category.lowercase(Locale.ROOT).contains(q) ||
                    exp.country.lowercase(Locale.ROOT).contains(q) ||
                    exp.date.lowercase(Locale.ROOT).contains(q) ||
                    formattedDateShort.contains(q)

            matchesCategory && matchesDate && matchesQuery
        }.sortedWith(compareByDescending<ExpenseEntity> { it.date }.thenByDescending { it.createdAt })
    }

    val groupedByDate = remember(filteredExpenses) {
        filteredExpenses.groupBy { it.date }
    }

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
                                contentDescription = if (isEn) "Search" else "Buscar",
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
                                                text = if (isEn) {
                                                    "Search place, note, category or date..."
                                                } else {
                                                    "Buscar lugar, nota, categoría o fecha..."
                                                },
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
                                        contentDescription = if (isEn) "Clear search" else "Limpiar búsqueda",
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
                                contentDescription = if (isEn) "Filter by date" else "Filtrar por fecha",
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
                                    text = "📅 ${if (isEn) "Date" else "Fecha"}: ${FormatUtils.formatShortDate(selectedDateFilter)}  ✕",
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
                            text = "${if (isEn) "All" else "Todas"} (${expenses.size})",
                            fontSize = 11.sp,
                            fontWeight = if (selectedCategoryFilter == null) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedCategoryFilter == null) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    CategoryCatalog.categories.forEach { cat ->
                        val count = categoryCounts[cat.name] ?: 0
                        val isSelected = selectedCategoryFilter.equals(cat.name, ignoreCase = true)
                        val displayCatName = CategoryCatalog.displayName(cat.name, isEn)
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
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else cat.color,
                                    modifier = Modifier.size(12.dp)
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
                }
            }

            // 5. Estado vacío o Listado agrupado por fecha
            if (filteredExpenses.isEmpty()) {
                item(key = "expenses_empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (expenses.isEmpty()) {
                                    if (isEn) "No expenses recorded in this trip yet" else "Aún no hay gastos en este viaje"
                                } else {
                                    if (isEn) "No expenses match the filter" else "No hay gastos que coincidan con el filtro"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (expenses.isEmpty()) {
                                    if (isEn) "Tap '+ Add Expense' to record your first entry." else "Pulsa el botón '+ Añadir Gasto' para registrar tu primer movimiento."
                                } else {
                                    if (isEn) "Try clearing the date or category filters." else "Prueba a limpiar los filtros de fecha o categoría."
                                },
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
                                        text = if (isEn) "Clear filters" else "Limpiar filtros",
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
                groupedByDate.forEach { (dateIso, dayExpenses) ->
                    val dayTotal = dayExpenses.sumOf { it.amountInBaseCurrency }
                    item(key = "header-$dateIso") {
                        DayStickyHeader(
                            dateIso = dateIso,
                            expenseCount = dayExpenses.size,
                            dayTotal = dayTotal,
                            baseCurrency = trip.baseCurrency,
                            isEn = isEn
                        )
                    }
                    items(dayExpenses, key = { it.id }) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            baseCurrency = trip.baseCurrency,
                            payerName = if (expense.paidById == DebtCalculator.BOTE_COMUN_ID) {
                                if (isEn) "Common Pot" else "Bote Común"
                            } else {
                                participantMap[expense.paidById]?.name ?: if (isEn) "Traveler" else "Viajero"
                            },
                            isEn = isEn,
                            onClick = { onExpenseClick(expense) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayStickyHeader(
    dateIso: String,
    expenseCount: Int,
    dayTotal: Double,
    baseCurrency: String,
    isEn: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFE2E8F0).copy(alpha = 0.7f)
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
                    text = FormatUtils.formatLongSpanishDate(dateIso, isEn),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = OceanNavy
                )
                Text(
                    text = if (isEn) {
                        if (expenseCount == 1) "1 expense recorded" else "$expenseCount expenses recorded"
                    } else {
                        if (expenseCount == 1) "1 gasto registrado" else "$expenseCount gastos registrados"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475569)
                )
            }
            Text(
                text = FormatUtils.formatCurrency(dayTotal, baseCurrency),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                color = OceanNavy
            )
        }
    }
}

@Composable
private fun ExpenseItemCard(
    expense: ExpenseEntity,
    baseCurrency: String,
    payerName: String,
    isEn: Boolean,
    onClick: () -> Unit
) {
    val categoryInfo = CategoryCatalog.getCategory(expense.category)
    val translatedCategory = CategoryCatalog.displayName(expense.category, isEn)
    val translatedPaymentMethod = PaymentMethodCatalog.displayName(expense.paymentMethod, isEn)

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

                // Etiquetas de quién pagó, método de pago, ubicación y fotos
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                                    contentDescription = if (isEn) "Attached receipts & memories" else "Recibos y recuerdos adjuntos",
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
                            contentDescription = if (isEn) "Geolocated on map" else "Con coordenadas en mapa",
                            tint = TravelBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Bloque derecho: Precio total, Categoría y Prorrateo
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
                }

                // En medio: La categoría
                Text(
                    text = translatedCategory,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp
                    ),
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Abajo: El texto del prorrateo con fuente más pequeña y color morado
                if (expense.prorationDays != null && expense.prorationDays > 1) {
                    val perDay = expense.amountInBaseCurrency / expense.prorationDays
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEDE9FE)
                    ) {
                        Text(
                            text = "📅 ${expense.prorationDays} d (${FormatUtils.formatCurrency(perDay, baseCurrency)}/${if (isEn) "day" else "día"})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            ),
                            color = Color(0xFF6D28D9),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
