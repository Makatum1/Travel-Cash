package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startDate: String, // YYYY-MM-DD
    val endDate: String,   // YYYY-MM-DD
    val budget: Double?,   // Opcional (null o <= 0 significa sin límite)
    val baseCurrency: String = "EUR",
    val headerPhotoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "participants")
data class ParticipantEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val name: String,
    val avatarColor: String, // Hex color e.g. "#0066FF"
    val photoUri: String? = null
)

data class ExpenseSplit(
    val id: String,
    val expenseId: String,
    val participantId: String,
    val amount: Double,     // En moneda base
    val percentage: Double  // 0.0 - 100.0
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val amount: Double,               // Importe en moneda original
    val currency: String,             // Código ISO (EUR, USD, JPY, etc.)
    val amountInBaseCurrency: Double, // Importe convertido a la moneda base del viaje
    val exchangeRate: Double,         // Tipo de cambio editable
    val category: String,
    val date: String,                 // YYYY-MM-DD
    val paymentMethod: String,        // Efectivo, Tarjeta, Bizum, Transferencia, Bote Común
    val notes: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeName: String = "",
    val country: String = "",
    val originName: String = "",
    val originLatitude: Double? = null,
    val originLongitude: Double? = null,
    val paidById: String,             // ID del participante que pagó (o "bote-comun")
    val photos: List<String> = emptyList(),
    val splits: List<ExpenseSplit> = emptyList(),
    val prorationDays: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class IncomeContribution(
    val participantId: String,
    val amount: Double,               // Lo que realmente pone en efectivo esa persona (>= 0)
    val debtDiscount: Double? = null  // Importe descontado automáticamente porque se le debía dinero
)

@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val amount: Double,                  // Importe total real recaudado en caja
    val date: String,                    // YYYY-MM-DD
    val description: String,
    val basePerPerson: Double? = null,   // Cuantía base fijada por persona
    val calculationMethod: String = "perPerson", // "perPerson" | "total"
    val contributions: List<IncomeContribution> = emptyList()
)

data class CategoryInfo(
    val name: String,
    val hexColor: String,
    val color: Color,
    val icon: ImageVector
)

object CategoryCatalog {
    // Se cambia el lugar de Entradas y Ocio por Vuelos, y al lado de Entradas y Ocio se añade Taxi
    val categories = listOf(
        CategoryInfo("Restaurantes y Comida", "#F59E0B", Color(0xFFF59E0B), Icons.Default.Restaurant),
        CategoryInfo("Alojamiento", "#6366F1", Color(0xFF6366F1), Icons.Default.Hotel),
        CategoryInfo("Transporte", "#0066FF", Color(0xFF0066FF), Icons.Default.DirectionsBus),
        CategoryInfo("Vuelos", "#06B6D4", Color(0xFF06B6D4), Icons.Default.Flight),
        CategoryInfo("Compras y Supermercado", "#10B981", Color(0xFF10B981), Icons.Default.ShoppingCart),
        CategoryInfo("Cafés y Snacks", "#8B5CF6", Color(0xFF8B5CF6), Icons.Default.LocalCafe),
        CategoryInfo("Entradas y Ocio", "#EC4899", Color(0xFFEC4899), Icons.Default.ConfirmationNumber),
        CategoryInfo("Taxi", "#D97706", Color(0xFFD97706), Icons.Default.LocalTaxi),
        CategoryInfo("Salud y Farmacia", "#EF4444", Color(0xFFEF4444), Icons.Default.LocalHospital),
        CategoryInfo("General", "#64748B", Color(0xFF64748B), Icons.Default.Category)
    )

    fun getCategory(name: String): CategoryInfo {
        if (name.equals("Transporte y Gasolina", ignoreCase = true)) {
            return categories.first { it.name == "Transporte" }
        }
        return categories.find { it.name.equals(name, ignoreCase = true) }
            ?: CategoryInfo(
                name = name.ifBlank { "General" },
                hexColor = "#64748B",
                color = Color(0xFF64748B),
                icon = Icons.Default.Category
            )
    }

    /**
     * Indica si la categoría corresponde a un trayecto con Origen y Destino (Transporte, Taxi o Vuelos).
     */
    fun isRouteCategory(categoryName: String): Boolean {
        val norm = categoryName.trim().lowercase()
        return norm == "transporte" ||
                norm == "transporte y gasolina" ||
                norm == "taxi" ||
                norm == "vuelos" ||
                norm == "vuelo"
    }

    /**
     * Traduce únicamente los nombres base predefinidos de la aplicación al idioma seleccionado.
     * Cualquier nombre personalizado introducido por el usuario en el botón General se mantiene intacto.
     */
    fun displayName(rawCategoryName: String, isEn: Boolean): String {
        if (!isEn) {
            return when (rawCategoryName.trim().lowercase()) {
                "transporte y gasolina" -> "Transporte"
                else -> rawCategoryName
            }
        }
        return when (rawCategoryName.trim().lowercase()) {
            "restaurantes y comida" -> "Restaurants & Food"
            "alojamiento" -> "Accommodation"
            "transporte", "transporte y gasolina" -> "Transport"
            "entradas y ocio", "entradas" -> "Tickets & Leisure"
            "taxi" -> "Taxi"
            "compras y supermercado" -> "Shopping & Grocery"
            "cafés y snacks", "cafes y snacks" -> "Cafes & Snacks"
            "vuelos", "vuelo" -> "Flights"
            "salud y farmacia" -> "Health & Pharmacy"
            "general" -> "General"
            else -> rawCategoryName // Dato introducido por el usuario: no se traduce
        }
    }
}

object PaymentMethodCatalog {
    val methods = listOf(
        "Efectivo",
        "Tarjeta",
        "Bizum",
        "Transferencia",
        "Bote Común"
    )

    fun displayName(rawMethod: String, isEn: Boolean): String {
        if (!isEn) return rawMethod
        return when (rawMethod.trim().lowercase()) {
            "efectivo" -> "Cash"
            "tarjeta" -> "Card"
            "bizum" -> "Bizum"
            "transferencia" -> "Transfer"
            "bote común", "bote comun" -> "Common Pot"
            else -> rawMethod
        }
    }
}

object AvatarColorPalette {
    val colors = listOf(
        "#0066FF",
        "#10B981",
        "#F59E0B",
        "#EC4899",
        "#6366F1",
        "#8B5CF6",
        "#06B6D4",
        "#EF4444",
        "#0D9488"
    )

    fun parseHexColor(hex: String): Color {
        return try {
            val cleaned = hex.removePrefix("#")
            val longVal = cleaned.toLong(16)
            if (cleaned.length == 6) {
                Color(0xFF000000 or longVal)
            } else {
                Color(longVal)
            }
        } catch (e: Exception) {
            Color(0xFF0066FF)
        }
    }
}
