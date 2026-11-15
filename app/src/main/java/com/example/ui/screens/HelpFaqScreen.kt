package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.OceanNavy
import com.example.ui.theme.TravelBlue
import com.example.ui.theme.isEnglish
import java.text.Normalizer
import java.util.Locale

/**
 * Modelo de datos para las preguntas frecuentes de Travel Cash.
 */
data class FaqItem(
    val id: String,
    val questionEs: String,
    val questionEn: String,
    val answerEs: String,
    val answerEn: String
)

/**
 * Normaliza un texto eliminando diacríticos (tildes, acentos, diéresis), signos y pasándolo a minúsculas
 * para que el buscador funcione de forma inteligente e insensible a acentos, mayúsculas o caracteres especiales.
 */
fun normalizeFaqSearchText(text: String): String {
    val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
    val withoutDiacritics = normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    return withoutDiacritics.lowercase(Locale.ROOT)
        .replace(Regex("[¿?¡!.,:;\"'()\\[\\]{}]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

/**
 * Lista de preguntas y respuestas oficiales adaptadas estrictamente al modelo de Travel Cash
 * (Viajes, Gastos, Bote Común, Participantes, Liquidaciones, Presupuesto, Divisas, Copias de Seguridad).
 */
val TRAVEL_CASH_FAQS = listOf(
    FaqItem(
        id = "faq_create_trip",
        questionEs = "¿Cómo creo un viaje nuevo?",
        questionEn = "How do I create a new trip?",
        answerEs = "Dirígete a la pestaña \"Viajes\" en la barra de navegación inferior y pulsa sobre el botón flotante \"+ Nuevo Viaje\" (o la tarjeta para crear viaje). Introduce el nombre de tu viaje o destino, las fechas de inicio y fin, el presupuesto estimado (opcional), la divisa principal y los participantes iniciales. Al guardar, quedará establecido como tu viaje activo.",
        answerEn = "Go to the \"Trips\" tab on the bottom navigation bar and tap the \"+ New Trip\" floating button (or the new trip card). Enter your trip or destination name, start and end dates, estimated budget (optional), base currency, and initial participants. Once saved, it will become your active trip."
    ),
    FaqItem(
        id = "faq_common_pot",
        questionEs = "¿Cómo funciona el bote común?",
        questionEn = "How does the common pot work?",
        answerEs = "El Bote Común es un fondo colectivo para abonar gastos grupales. Puedes añadir fondos pulsando en el botón verde \"+ Ingreso (Bote)\" de la cabecera. Puedes aportar un importe global o una \"Cuantía por persona\". En este último modo, la app calcula automáticamente si algún participante adelantó pagos de su bolsillo y descuenta lo que se le debe de su cuota, calculando en tiempo real el Saldo Extra disponible.",
        answerEn = "The Common Pot is a group fund used to pay shared expenses. You can add funds by tapping the green \"+ Pot Income\" button in the top header. You can add a lump sum or an \"Amount per person\". In per-person mode, the app automatically calculates if any participant paid out-of-pocket and deducts what they are owed from their share, updating the Extra Balance in real time."
    ),
    FaqItem(
        id = "faq_add_expense",
        questionEs = "¿Cómo añado un gasto a un viaje?",
        questionEn = "How do I add an expense to a trip?",
        answerEs = "En la pestaña \"Gastos\", pulsa el botón flotante \"+\". Introduce el concepto o comercio, el importe, la fecha y la categoría correspondiente (comida, transporte, alojamiento, compras, ocio, etc.). Selecciona quién realizó el pago (si se abonó con el Bote Común o si lo pagó un viajero de su bolsillo) y marca qué participantes comparten el gasto.",
        answerEn = "On the \"Expenses\" tab, tap the \"+\" floating action button. Enter the description or merchant, amount, date, and category (food, transport, accommodation, shopping, leisure, etc.). Select who paid (from the Common Pot or by an individual traveler out-of-pocket) and select which participants share this expense."
    ),
    FaqItem(
        id = "faq_split_expenses",
        questionEs = "¿Cómo se divide un gasto o se reparten los gastos entre participantes?",
        questionEn = "How do I divide or split an expense among participants?",
        answerEs = "Al crear o editar cualquier gasto, puedes realizar la división a partes iguales o desmarcar a los participantes que no hayan consumido en él. En la pestaña \"Cuentas\", la app calcula la división y el balance matemático exacto de cada participante (lo pagado frente a lo consumido) y genera las transferencias mínimas optimizadas para saldar todas las deudas del grupo.",
        answerEn = "When creating or editing an expense, you can perform equal cost division or uncheck participants who did not consume. In the \"Splits\" tab, the app calculates the expense division and exact balance for each traveler (paid vs consumed) and generates the minimum optimized transfers to settle all group debts."
    ),
    FaqItem(
        id = "faq_order_by_date",
        questionEs = "¿Cómo consulto u ordeno los gastos por fecha o día dentro de un viaje?",
        questionEn = "How do I view or sort expenses by date or day in a trip?",
        answerEs = "En la pestaña \"Gastos\", todos los registros se muestran agrupados y ordenados cronológicamente por día. Además, puedes usar el buscador rápido y los filtros superiores para localizar gastos por fecha específica, categoría o participante, así como acudir a la pestaña \"Datos\" para analizar estadísticas de gasto medio diario.",
        answerEn = "On the \"Expenses\" tab, all entries are automatically grouped and ordered chronologically by day. You can also use the quick search and top filter chips to locate expenses by specific date, category, or participant, or check the \"Stats\" tab to analyze daily average spend."
    ),
    FaqItem(
        id = "faq_change_currency",
        questionEs = "¿Cómo cambio la divisa o el formato de moneda?",
        questionEn = "How do I change currency or currency format?",
        answerEs = "Cada viaje cuenta con su divisa base (EUR, USD, GBP, JPY...) definida al crearlo o editarlo desde la pestaña \"Viajes\". Además, al registrar un gasto en el extranjero puedes introducir el importe en moneda extranjera y el tipo de cambio aplicado, convirtiéndose de inmediato a la divisa del viaje.",
        answerEn = "Each trip has a base currency (EUR, USD, GBP, JPY...) set when creating or editing it on the \"Trips\" tab. Additionally, when logging expenses abroad you can enter foreign currency amounts along with the conversion rate, automatically converting it to your trip's base currency."
    ),
    FaqItem(
        id = "faq_export_zip",
        questionEs = "¿Cómo exporto un viaje a ZIP?",
        questionEn = "How do I export a trip to ZIP?",
        answerEs = "Ve a la pestaña \"Ajustes\", localiza la sección \"Exportar e Importar\" y pulsa el botón \"Exportar\". Puedes elegir exportar \"Todos los viajes\" o un viaje específico. El archivo .zip se genera en caché y abre el menú nativo de compartir de Android para que lo envíes por WhatsApp, correo o lo guardes en Google Drive o tu gestor de archivos.",
        answerEn = "Go to the \"Settings\" tab, find the \"Export and Import\" section, and tap \"Export\". You can choose to export \"All trips\" or a specific trip. A .zip file will be generated in cache and will open Android's native Share Sheet to send via WhatsApp, email, or save to Google Drive or Downloads."
    ),
    FaqItem(
        id = "faq_export_pdf",
        questionEs = "¿Cómo genero el PDF de resumen de un viaje?",
        questionEn = "How do I generate a trip summary PDF report?",
        answerEs = "En la pestaña \"Viajes\", expande las opciones de la tarjeta del viaje y pulsa \"Exportar a PDF\". Se creará un documento profesional multipágina con el título del viaje, el balance general, resumen de bote común, desglose de gastos por categoría y liquidación de deudas, abriendo el selector de compartir para enviarlo o guardarlo.",
        answerEn = "On the \"Trips\" tab, expand the trip card options and tap \"Export to PDF\". A clean multi-page document will be generated featuring the trip title, financial summary, common pot balance, category breakdowns, and debt settlements, opening the Share Sheet to send or save it."
    ),
    FaqItem(
        id = "faq_backup_restore",
        questionEs = "¿Cómo hago una copia de seguridad o restauro un viaje?",
        questionEn = "How do I back up or restore a trip?",
        answerEs = "Puedes respaldar todos tus viajes exportando el archivo ZIP desde la pestaña \"Ajustes\", o respaldar un viaje individual en formato JSON desde su tarjeta en la pestaña \"Viajes\". Para restaurar tus datos en un dispositivo nuevo o reinstalado, pulsa el botón \"Importar\" y selecciona tu archivo .zip o .json.",
        answerEn = "You can back up all your trips by exporting a ZIP file from the \"Settings\" tab, or back up an individual trip in JSON format from its card on the \"Trips\" tab. To restore your data on a new device, tap \"Import\" and select your .zip or .json file."
    ),
    FaqItem(
        id = "faq_attach_photo",
        questionEs = "¿Cómo adjunto una imagen o foto a un gasto?",
        questionEn = "How do I attach a receipt photo or image to an expense?",
        answerEs = "En el formulario para añadir o editar un gasto, pulsa el botón para adjuntar imagen. Puedes tomar una fotografía instantánea del ticket con tu cámara o seleccionar un comprobante guardado en tu galería. La foto quedará vinculada al registro y podrás pulsarla para verla en tamaño completo en cualquier momento.",
        answerEn = "In the add or edit expense dialog, tap the attach image button. You can take an instant photo of your receipt with your camera or select an image from your gallery. The receipt will be linked to the expense and can be viewed full-screen anytime by tapping it."
    )
)

/**
 * Pantalla completa modal de Ayuda y Preguntas Frecuentes.
 */
@Composable
fun HelpFaqModal(
    onDismiss: () -> Unit
) {
    val isEn = LocalAppLanguage.current.isEnglish()
    var searchQuery by remember { mutableStateOf("") }
    var expandedItemIds by remember { mutableStateOf(setOf<String>()) }

    BackHandler {
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Barra superior con botón volver y título
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("help_faq_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEn) "Back to Suggestions" else "Volver a Sugerencias",
                            tint = OceanNavy
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "Help & FAQ" else "Ayuda y Preguntas Frecuentes",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        ),
                        color = OceanNavy
                    )
                }

                // Buscador inteligente con OutlinedTextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("faq_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = {
                        Text(
                            text = if (isEn) "Search questions or solutions…" else "Buscar preguntas o soluciones…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TravelBlue
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = if (isEn) "Clear search" else "Limpiar búsqueda",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TravelBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                // Filtrado inteligente sin acentos, mayúsculas ni diacríticos
                val normalizedQuery = remember(searchQuery) { normalizeFaqSearchText(searchQuery) }
                val tokens = remember(normalizedQuery) {
                    normalizedQuery.split(Regex("\\s+")).filter { it.isNotEmpty() }
                }

                val filteredFaqs = remember(normalizedQuery, isEn) {
                    if (tokens.isEmpty()) {
                        TRAVEL_CASH_FAQS
                    } else {
                        TRAVEL_CASH_FAQS.filter { item ->
                            val qText = if (isEn) item.questionEn else item.questionEs
                            val aText = if (isEn) item.answerEn else item.answerEs
                            val normQ = normalizeFaqSearchText(qText)
                            val normA = normalizeFaqSearchText(aText)

                            tokens.all { token ->
                                normQ.contains(token) || normA.contains(token)
                            }
                        }
                    }
                }

                if (filteredFaqs.isEmpty()) {
                    // Estado vacío cuando no hay resultados
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .testTag("faq_empty_results"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        tint = TravelBlue,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (isEn) "No results found for your search" else "No se han encontrado resultados para tu búsqueda",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = OceanNavy,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isEn) "Try searching for keywords like \"pot\", \"expenses\", \"split\" or \"backup\"." else "Prueba a buscar términos como \"bote\", \"gastos\", \"repartir\" o \"backup\".",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { searchQuery = "" },
                                    colors = ButtonDefaults.buttonColors(containerColor = TravelBlue),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isEn) "Clear search" else "Limpiar búsqueda",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Lista de preguntas y respuestas en acordeón
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("faq_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredFaqs, key = { it.id }) { item ->
                            val isExpanded = expandedItemIds.contains(item.id)
                            val question = if (isEn) item.questionEn else item.questionEs
                            val answer = if (isEn) item.answerEn else item.answerEs

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                                    .clickable {
                                        expandedItemIds = if (isExpanded) {
                                            expandedItemIds - item.id
                                        } else {
                                            expandedItemIds + item.id
                                        }
                                    }
                                    .testTag("faq_item_${item.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(
                                    1.dp,
                                    if (isExpanded) TravelBlue.copy(alpha = 0.5f) else Color(0xFFE2E8F0)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 2.dp else 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                                contentDescription = null,
                                                tint = TravelBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = question,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                ),
                                                color = OceanNavy
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                                            tint = if (isExpanded) TravelBlue else Color(0xFF64748B),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    AnimatedVisibility(visible = isExpanded) {
                                        Column {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            HorizontalDivider(color = Color(0xFFF1F5F9))
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = answer,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    lineHeight = 21.sp,
                                                    fontSize = 13.5.sp
                                                ),
                                                color = Color(0xFF334155)
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
