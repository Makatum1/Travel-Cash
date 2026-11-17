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
import com.example.ui.theme.AppLanguage

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
    // Taxi se sitúa junto a Vuelos (Transporte, Vuelos, Taxi), y al final junto a Entradas y Ocio queda General
    val categories = listOf(
        CategoryInfo("Restaurantes y Comida", "#F59E0B", Color(0xFFF59E0B), Icons.Default.Restaurant),
        CategoryInfo("Alojamiento", "#6366F1", Color(0xFF6366F1), Icons.Default.Hotel),
        CategoryInfo("Transporte", "#0066FF", Color(0xFF0066FF), Icons.Default.DirectionsBus),
        CategoryInfo("Vuelos", "#06B6D4", Color(0xFF06B6D4), Icons.Default.Flight),
        CategoryInfo("Taxi", "#D97706", Color(0xFFD97706), Icons.Default.LocalTaxi),
        CategoryInfo("Compras y Supermercado", "#10B981", Color(0xFF10B981), Icons.Default.ShoppingCart),
        CategoryInfo("Cafés y Snacks", "#8B5CF6", Color(0xFF8B5CF6), Icons.Default.LocalCafe),
        CategoryInfo("Entradas y Ocio", "#EC4899", Color(0xFFEC4899), Icons.Default.ConfirmationNumber),
        CategoryInfo("General", "#64748B", Color(0xFF64748B), Icons.Default.Category)
    )

    fun getCategory(name: String): CategoryInfo {
        if (name.equals("Transporte y Gasolina", ignoreCase = true)) {
            return categories.first { it.name == "Transporte" }
        }
        return categories.find { it.name.equals(name, ignoreCase = true) }
            ?: if (name.equals("Salud y Farmacia", ignoreCase = true)) {
                CategoryInfo("Salud y Farmacia", "#EF4444", Color(0xFFEF4444), Icons.Default.LocalHospital)
            } else {
                CategoryInfo(
                    name = name.ifBlank { "General" },
                    hexColor = "#64748B",
                    color = Color(0xFF64748B),
                    icon = Icons.Default.Category
                )
            }
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
    fun displayName(rawCategoryName: String, lang: AppLanguage): String {
        val norm = rawCategoryName.trim().lowercase()
        return when (lang) {
            AppLanguage.ES -> when (norm) {
                "transporte y gasolina" -> "Transporte"
                else -> rawCategoryName
            }
            AppLanguage.EN -> when (norm) {
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
                else -> rawCategoryName
            }
            AppLanguage.FR -> when (norm) {
                "restaurantes y comida" -> "Restaurants et Alimentation"
                "alojamiento" -> "Hébergement"
                "transporte", "transporte y gasolina" -> "Transport"
                "entradas y ocio", "entradas" -> "Billets et Loisirs"
                "taxi" -> "Taxi"
                "compras y supermercado" -> "Achats et Supermarché"
                "cafés y snacks", "cafes y snacks" -> "Cafés et Snacks"
                "vuelos", "vuelo" -> "Vols"
                "salud y farmacia" -> "Santé et Pharmacie"
                "general" -> "Général"
                else -> rawCategoryName
            }
            AppLanguage.IT -> when (norm) {
                "restaurantes y comida" -> "Ristoranti e Cibo"
                "alojamiento" -> "Alloggio"
                "transporte", "transporte y gasolina" -> "Trasporti"
                "entradas y ocio", "entradas" -> "Biglietti e Tempo Libero"
                "taxi" -> "Taxi"
                "compras y supermercado" -> "Spesa e Supermercato"
                "cafés y snacks", "cafes y snacks" -> "Caffè e Snack"
                "vuelos", "vuelo" -> "Voli"
                "salud y farmacia" -> "Salute e Farmacia"
                "general" -> "Generale"
                else -> rawCategoryName
            }
            AppLanguage.ZH -> when (norm) {
                "restaurantes y comida" -> "餐厅与餐饮"
                "alojamiento" -> "住宿"
                "transporte", "transporte y gasolina" -> "交通"
                "entradas y ocio", "entradas" -> "门票与休闲"
                "taxi" -> "出租车"
                "compras y supermercado" -> "购物与超市"
                "cafés y snacks", "cafes y snacks" -> "咖啡与小吃"
                "vuelos", "vuelo" -> "航班"
                "salud y farmacia" -> "健康与药房"
                "general" -> "通用"
                else -> rawCategoryName
            }
            AppLanguage.NL -> when (norm) {
                "restaurantes y comida" -> "Restaurants & Eten"
                "alojamiento" -> "Accommodatie"
                "transporte", "transporte y gasolina" -> "Vervoer"
                "entradas y ocio", "entradas" -> "Tickets & Vrije Tijd"
                "taxi" -> "Taxi"
                "compras y supermercado" -> "Winkelen & Supermarkt"
                "cafés y snacks", "cafes y snacks" -> "Cafés & Snacks"
                "vuelos", "vuelo" -> "Vluchten"
                "salud y farmacia" -> "Gezondheid & Apotheek"
                "general" -> "Algemeen"
                else -> rawCategoryName
            }
            AppLanguage.DE -> when (norm) {
                "restaurantes y comida" -> "Restaurants & Verpflegung"
                "alojamiento" -> "Unterkunft"
                "transporte", "transporte y gasolina" -> "Transport"
                "entradas y ocio", "entradas" -> "Tickets & Freizeit"
                "taxi" -> "Taxi"
                "compras y supermercado" -> "Einkaufen & Supermarkt"
                "cafés y snacks", "cafes y snacks" -> "Cafés & Snacks"
                "vuelos", "vuelo" -> "Flüge"
                "salud y farmacia" -> "Gesundheit & Apotheke"
                "general" -> "Allgemein"
                else -> rawCategoryName
            }
            AppLanguage.PL -> when (norm) {
                "restaurantes y comida" -> "Restauracje i Jedzenie"
                "alojamiento" -> "Zakwaterowanie"
                "transporte", "transporte y gasolina" -> "Transport"
                "entradas y ocio", "entradas" -> "Bilety i Rozrywka"
                "taxi" -> "Taksówka"
                "compras y supermercado" -> "Zakupy i Supermarket"
                "cafés y snacks", "cafes y snacks" -> "Kawiarnie i Przekąski"
                "vuelos", "vuelo" -> "Loty"
                "salud y farmacia" -> "Zdrowie i Apteka"
                "general" -> "Ogólne"
                else -> rawCategoryName
            }
            AppLanguage.PT -> when (norm) {
                "restaurantes y comida" -> "Restaurantes e Comida"
                "alojamiento" -> "Alojamento"
                "transporte", "transporte y gasolina" -> "Transporte"
                "entradas y ocio", "entradas" -> "Bilhetes e Lazer"
                "taxi" -> "Táxi"
                "compras y supermercado" -> "Compras e Supermercado"
                "cafés y snacks", "cafes y snacks" -> "Cafés e Snacks"
                "vuelos", "vuelo" -> "Voos"
                "salud y farmacia" -> "Saúde e Farmácia"
                "general" -> "Geral"
                else -> rawCategoryName
            }
        }
    }

    fun displayName(rawCategoryName: String, isEn: Boolean): String =
        displayName(rawCategoryName, if (isEn) AppLanguage.EN else AppLanguage.ES)
}

object PaymentMethodCatalog {
    val methods = listOf(
        "Efectivo",
        "Tarjeta",
        "Bizum",
        "Transferencia",
        "Bote Común"
    )

    fun displayName(rawMethod: String, lang: AppLanguage): String {
        val norm = rawMethod.trim().lowercase()
        return when (lang) {
            AppLanguage.ES -> rawMethod
            AppLanguage.EN -> when (norm) {
                "efectivo" -> "Cash"
                "tarjeta" -> "Card"
                "bizum" -> "Bizum"
                "transferencia" -> "Transfer"
                "bote común", "bote comun" -> "Common Pot"
                else -> rawMethod
            }
            AppLanguage.FR -> when (norm) {
                "efectivo" -> "Espèces"
                "tarjeta" -> "Carte bancaire"
                "bizum" -> "Bizum"
                "transferencia" -> "Virement"
                "bote común", "bote comun" -> "Cagnotte commune"
                else -> rawMethod
            }
            AppLanguage.IT -> when (norm) {
                "efectivo" -> "Contanti"
                "tarjeta" -> "Carta"
                "bizum" -> "Bizum"
                "transferencia" -> "Bonifico"
                "bote común", "bote comun" -> "Cassa comune"
                else -> rawMethod
            }
            AppLanguage.ZH -> when (norm) {
                "efectivo" -> "现金"
                "tarjeta" -> "银行卡"
                "bizum" -> "Bizum"
                "transferencia" -> "转账"
                "bote común", "bote comun" -> "公账"
                else -> rawMethod
            }
            AppLanguage.NL -> when (norm) {
                "efectivo" -> "Contant"
                "tarjeta" -> "Pinpas/Creditcard"
                "bizum" -> "Bizum"
                "transferencia" -> "Overschrijving"
                "bote común", "bote comun" -> "Gemeenschappelijke pot"
                else -> rawMethod
            }
            AppLanguage.DE -> when (norm) {
                "efectivo" -> "Bargeld"
                "tarjeta" -> "Karte"
                "bizum" -> "Bizum"
                "transferencia" -> "Überweisung"
                "bote común", "bote comun" -> "Gemeinschaftskasse"
                else -> rawMethod
            }
            AppLanguage.PL -> when (norm) {
                "efectivo" -> "Gotówka"
                "tarjeta" -> "Karta"
                "bizum" -> "Bizum"
                "transferencia" -> "Przelew"
                "bote común", "bote comun" -> "Wspólna kasa"
                else -> rawMethod
            }
            AppLanguage.PT -> when (norm) {
                "efectivo" -> "Dinheiro"
                "tarjeta" -> "Cartão"
                "bizum" -> "MB Way / Transferência"
                "transferencia" -> "Transferência"
                "bote común", "bote comun" -> "Fundo Comum"
                else -> rawMethod
            }
        }
    }

    fun displayName(rawMethod: String, isEn: Boolean): String =
        displayName(rawMethod, if (isEn) AppLanguage.EN else AppLanguage.ES)
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
