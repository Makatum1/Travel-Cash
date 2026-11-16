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
import androidx.compose.foundation.layout.imePadding
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
import com.example.ui.theme.AppLanguage
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
    val categoryEs: String,
    val categoryEn: String,
    val questionEs: String,
    val questionEn: String,
    val answerEs: String,
    val answerEn: String
) {
    fun getCategory(lang: AppLanguage): String =
        FaqTranslations.getCategory(id, lang, categoryEs, categoryEn)

    fun getQuestion(lang: AppLanguage): String =
        FaqTranslations.getQuestion(id, lang, questionEs, questionEn)

    fun getAnswer(lang: AppLanguage): String =
        FaqTranslations.getAnswer(id, lang, answerEs, answerEn)
}

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
 * Guía completa y detallada de preguntas y respuestas oficiales adaptadas estrictamente al modelo de Travel Cash
 * (Viajes, Gastos, Bote Común, Participantes, Liquidaciones, Presupuesto, Divisas, Copias de Seguridad, Exportación).
 */
val TRAVEL_CASH_FAQS = listOf(
    // -------------------------------------------------------------------------
    // 1. GESTIÓN DE VIAJES
    // -------------------------------------------------------------------------
    FaqItem(
        id = "faq_create_trip",
        categoryEs = "Gestión de Viajes",
        categoryEn = "Trip Management",
        questionEs = "¿Cómo creo un viaje nuevo y qué datos necesito?",
        questionEn = "How do I create a new trip and what data do I need?",
        answerEs = "Dirígete a la pestaña \"Viajes\" en la barra de navegación inferior y pulsa sobre el botón flotante \"+ Nuevo Viaje\" (o la tarjeta para crear viaje). Introduce el nombre de tu viaje o destino, las fechas de inicio y fin, el presupuesto estimado (opcional), la divisa base (por ejemplo EUR, USD...) y los participantes iniciales. Al guardar, quedará establecido como tu viaje activo sobre el que registrarás gastos.",
        answerEn = "Go to the \"Trips\" tab on the bottom navigation bar and tap the \"+ New Trip\" button (or the new trip card). Enter your trip or destination name, start and end dates, estimated budget (optional), base currency (e.g. EUR, USD...), and initial participants. Once saved, it will become your active trip where you can log expenses."
    ),
    FaqItem(
        id = "faq_switch_trip",
        categoryEs = "Gestión de Viajes",
        categoryEn = "Trip Management",
        questionEs = "¿Cómo cambio el viaje activo o consulto mis viajes anteriores?",
        questionEn = "How do I switch the active trip or view my previous trips?",
        answerEs = "En la pestaña \"Viajes\" verás la lista de todos tus viajes ordenados. El viaje que tenga la etiqueta \"ACTIVO\" es el que se muestra en las pestañas de Gastos, Datos y Cuentas. Para activar otro viaje, simplemente pulsa sobre su tarjeta o selecciona la opción para activarlo. Todos los registros y balances cambiarán instantáneamente a los del viaje seleccionado.",
        answerEn = "In the \"Trips\" tab you will find all your trips listed. The trip marked with the \"ACTIVE\" badge is the one displayed on the Expenses, Stats, and Splits tabs. To switch to another trip, simply tap on its card or activate it. All expenses, balances, and data will instantly switch to that trip."
    ),
    FaqItem(
        id = "faq_edit_delete_trip",
        categoryEs = "Gestión de Viajes",
        categoryEn = "Trip Management",
        questionEs = "¿Cómo edito o elimino un viaje existente?",
        questionEn = "How do I edit or delete an existing trip?",
        answerEs = "Dentro de la pestaña \"Viajes\", pulsa sobre el icono de lápiz (o despliega el menú de opciones de la tarjeta del viaje) para editar su título, fechas, presupuesto, divisa o participantes. Si deseas eliminarlo, pulsa el icono de papelera: la app te solicitará confirmación antes de borrar el viaje y todos sus gastos asociados.",
        answerEn = "In the \"Trips\" tab, tap the pencil icon (or expand the trip card options) to edit its title, dates, budget, currency, or participants. If you wish to delete it, tap the trash icon: the app will ask for confirmation before removing the trip and all its associated expenses."
    ),
    FaqItem(
        id = "faq_trip_header_photo",
        categoryEs = "Gestión de Viajes",
        categoryEn = "Trip Management",
        questionEs = "¿Cómo cambio la foto o icono de cabecera de un viaje?",
        questionEn = "How do I change the header photo or route icon of a trip?",
        answerEs = "En la tarjeta superior del viaje activo (o en la tarjeta del viaje en la pestaña \"Viajes\"), pulsa directamente sobre el icono redondo de la ruta. Se abrirá un diálogo que te permite tomar una foto con la cámara, elegir una imagen de tu galería o restaurar el icono de carretera por defecto.",
        answerEn = "On the active trip header card (or on the trip card in the \"Trips\" tab), tap directly on the round route icon. A dialog will open allowing you to capture a photo with your camera, pick an image from your gallery, or restore the default road icon."
    ),

    // -------------------------------------------------------------------------
    // 2. BOTE COMÚN Y FINANZAS GRUPALES
    // -------------------------------------------------------------------------
    FaqItem(
        id = "faq_common_pot",
        categoryEs = "Bote Común",
        categoryEn = "Common Pot",
        questionEs = "¿Cómo funciona el bote común y qué es el \"Saldo Extra\"?",
        questionEn = "How does the common pot work and what is \"Extra Balance\"?",
        answerEs = "El Bote Común es un fondo colectivo que el grupo reúne para abonar compras compartidas (taxis, comidas, entradas...). En la cabecera verás siempre la tarjeta \"SALDO EXTRA\": representa el dinero neto disponible en el bote descontando los gastos pagados con él y los reembolsos debidos. Si el saldo es positivo aparece en verde; si se han gastado más fondos de los aportados se mostrará en rojo para avisarte de que se necesita reponer dinero.",
        answerEn = "The Common Pot is a collective fund the group gathers to pay for shared purchases (taxis, meals, tickets...). In the top header you will always see the \"EXTRA BALANCE\" card: it shows the net cash available in the pot after deducting pot expenses and owed reimbursements. A positive balance appears in green; if spending exceeded contributions it turns red to alert you that replenishment is needed."
    ),
    FaqItem(
        id = "faq_golden_rule",
        categoryEs = "Bote Común",
        categoryEn = "Common Pot",
        questionEs = "¿Cuál es la \"Regla de Oro\" al ingresar dinero al bote común por persona?",
        questionEn = "What is the \"Golden Rule\" when adding common pot funds per person?",
        answerEs = "Al pulsar \"+ Ingreso (Bote)\" y seleccionar el modo \"Cuantía por persona\", la app aplica automáticamente la Regla de Oro financiera: si algún participante ya pagó gastos del grupo de su propio bolsillo, el sistema descuenta automáticamente lo que se le debe de su cuota de aportación. Así, quien adelantó dinero paga menos (o nada) en esa ronda de bote y el grupo salda deudas sin transferencias intermedias.",
        answerEn = "When tapping \"+ Pot Income\" and choosing \"Amount per person\" mode, the app automatically applies the financial Golden Rule: if any traveler previously paid group expenses out of pocket, the system automatically deducts what is owed to them from their pot contribution share. This way, whoever fronted money pays less (or nothing) in that collection round, settling debts without intermediate bank transfers."
    ),
    FaqItem(
        id = "faq_pot_vs_personal",
        categoryEs = "Bote Común",
        categoryEn = "Common Pot",
        questionEs = "¿Cuándo debo marcar un gasto pagado con \"Bote\" o por un participante?",
        questionEn = "When should an expense be marked as paid by \"Pot\" vs an individual?",
        answerEs = "Al añadir un gasto, en el campo \"Pagado por\":\n• Elige \"🏺 Bote Común\" si el dinero salió físicamente de la cartera común del grupo o de una tarjeta común.\n• Elige el nombre de un participante si esa persona pagó de su propio dinero en efectivo o con su tarjeta personal. De este modo, la app sabrá que el grupo le debe ese dinero a dicho participante.",
        answerEn = "When adding an expense, under the \"Paid by\" field:\n• Select \"🏺 Common Pot\" if the cash came directly from the group's common wallet or shared card.\n• Select a traveler's name if that individual paid with their own personal money. This way, the app tracks that the group owes money back to that person."
    ),
    FaqItem(
        id = "faq_leftover_pot",
        categoryEs = "Bote Común",
        categoryEn = "Common Pot",
        questionEs = "¿Qué ocurre si sobra dinero en el bote común al terminar el viaje?",
        questionEn = "What happens if there is leftover money in the common pot when the trip ends?",
        answerEs = "Si al finalizar el viaje el \"Saldo Extra\" del bote es positivo, ese dinero restante pertenece al grupo. En la pestaña \"Cuentas\", la app toma en cuenta las aportaciones netas de cada viajero y calcula exactamente cuánto debe devolverse a cada uno para que todos hayan aportado exactamente lo proporcional a lo que consumieron.",
        answerEn = "If at the end of the trip the pot's \"Extra Balance\" is positive, that surplus belongs to the group. In the \"Splits\" tab, the app takes into account each traveler's net contributions and calculates exactly how much should be refunded to each person so everyone pays strictly what they consumed."
    ),

    // -------------------------------------------------------------------------
    // 3. REGISTRO Y DIVISIÓN DE GASTOS
    // -------------------------------------------------------------------------
    FaqItem(
        id = "faq_add_expense",
        categoryEs = "Gastos",
        categoryEn = "Expenses",
        questionEs = "¿Cómo añado un gasto a un viaje?",
        questionEn = "How do I add an expense to a trip?",
        answerEs = "En la pestaña \"Gastos\", pulsa el botón flotante circular \"+\". Introduce el concepto o comercio, el importe, la fecha y la categoría correspondiente (comida, transporte, alojamiento, compras, ocio, vuelos, salud...). Selecciona quién pagó (el Bote o un participante) y marca qué viajeros se benefician del gasto.",
        answerEn = "On the \"Expenses\" tab, tap the circular floating action button \"+\". Enter the description or merchant, amount, date, and category (food, transport, lodging, shopping, leisure, flights, health...). Select who paid (the Pot or a specific traveler) and mark which travelers benefit from this expense."
    ),
    FaqItem(
        id = "faq_split_expenses",
        categoryEs = "Gastos",
        categoryEn = "Expenses",
        questionEs = "¿Cómo se divide un gasto o se reparten los gastos entre participantes? (División de gastos)",
        questionEn = "How do I divide or split an expense among participants? (Expense division)",
        answerEs = "Al crear o editar cualquier gasto, verás la lista de todos los participantes del viaje con casillas de verificación. De forma predeterminada están todos marcados para una división equitativa a partes iguales. Si alguien no participó en esa compra (por ejemplo, alguien no cenó o no fue a una excursión), desmarca su casilla: el importe se dividirá exclusivamente entre los participantes marcados.",
        answerEn = "When creating or editing an expense, you will see all trip participants with checkboxes. By default all are checked for an equal division. If someone did not take part in that purchase (e.g. skipped dinner or an excursion), simply uncheck their box: the amount will be divided only among checked members."
    ),
    FaqItem(
        id = "faq_order_by_date",
        categoryEs = "Gastos",
        categoryEn = "Expenses",
        questionEs = "¿Cómo consulto u ordeno los gastos por fecha o día dentro de un viaje?",
        questionEn = "How do I view or sort expenses by date or day in a trip?",
        answerEs = "En la pestaña \"Gastos\", todos los registros se agrupan y ordenan de manera automática y cronológica por día. Puedes utilizar la barra de búsqueda superior para encontrar compras por texto, usar los chips de filtro para aislar categorías o participantes concretos, y consultar en la pestaña \"Datos\" la evolución del gasto por periodos.",
        answerEn = "On the \"Expenses\" tab, all records are automatically grouped and sorted chronologically by day. You can use the top search bar to locate purchases by text, use filter chips to isolate specific categories or participants, and consult the \"Stats\" tab for spending over time."
    ),
    FaqItem(
        id = "faq_attach_photo",
        categoryEs = "Gastos",
        categoryEn = "Expenses",
        questionEs = "¿Cómo adjunto una imagen o foto a un gasto?",
        questionEn = "How do I attach a receipt photo or image to an expense?",
        answerEs = "Al añadir o editar un gasto, pulsa el botón con icono de cámara o galería. Puedes tomar una fotografía instantánea del ticket con tu cámara o seleccionar un comprobante guardado en tu galería. La foto queda guardada de forma segura junto al registro; al pulsar sobre el gasto podrás ver la foto del ticket ampliada en alta resolución.",
        answerEn = "When adding or editing an expense, tap the camera or gallery button. You can take an instant photo of the receipt with your camera or select an image from your gallery. The photo is securely stored with the record; tapping the expense allows you to view the receipt enlarged in high resolution."
    ),
    FaqItem(
        id = "faq_gps_location",
        categoryEs = "Gastos",
        categoryEn = "Expenses",
        questionEs = "¿Cómo funciona la geolocalización GPS y sugerencias automáticas de comercios?",
        questionEn = "How does GPS geolocation and automatic venue suggestions work?",
        answerEs = "Al escribir el nombre de un restaurante o tienda al registrar un gasto, si tienes el permiso de ubicación activo, Travel Cash consulta en tiempo real OpenStreetMap (Nominatim) usando tus coordenadas GPS para sugerir los comercios más cercanos a tu posición, rellenando automáticamente la dirección y situando el gasto en la pestaña \"Mapa\".",
        answerEn = "When typing a restaurant or shop name while creating an expense, if location permission is granted, Travel Cash queries OpenStreetMap (Nominatim) in real time using your GPS coordinates to suggest the closest venues, automatically filling in the address and plotting it on the \"Map\" tab."
    ),

    // -------------------------------------------------------------------------
    // 4. LIQUIDACIÓN DE CUENTAS Y BALANCES
    // -------------------------------------------------------------------------
    FaqItem(
        id = "faq_splits_calculation",
        categoryEs = "Cuentas y Balances",
        categoryEn = "Splits & Balances",
        questionEs = "¿Cómo calcula la app las deudas y saldos en la pestaña \"Cuentas\"?",
        questionEn = "How does the app calculate debts and balances in the \"Splits\" tab?",
        answerEs = "Para cada viajero, la app calcula dos cifras fundamentales:\n1. Lo aportado: la suma de todos los gastos que pagó de su bolsillo más su dinero entregado al bote común.\n2. Lo consumido: la suma de su parte proporcional en todos los gastos en los que participó.\nLa diferencia entre ambas cifras determina su saldo: si es positivo se le debe dinero (+); si es negativo debe dinero (-).",
        answerEn = "For each participant, the app computes two core values:\n1. Paid: sum of all expenses paid out-of-pocket plus money contributed to the pot.\n2. Consumed: sum of their proportional share in all expenses they took part in.\nThe difference determines their net balance: positive means they are owed money (+); negative means they owe money (-)."
    ),
    FaqItem(
        id = "faq_optimized_settlements",
        categoryEs = "Cuentas y Balances",
        categoryEn = "Splits & Balances",
        questionEs = "¿Qué son los \"Pagos de liquidación optimizados\"?",
        questionEn = "What are the \"optimized settlement payments\"?",
        answerEs = "En lugar de que cada persona tenga que hacer múltiples transferencias entre sí, el algoritmo matemático de Travel Cash simplifica todas las deudas del grupo al número mínimo indispensable de transferencias (por ejemplo: \"Carlos paga 15,50 € a Lucía\"). Así, con un par de pagos directos todas las cuentas del viaje quedan 100% saldadas.",
        answerEn = "Instead of everyone making dozens of confusing transfers to each other, Travel Cash's mathematical algorithm simplifies all group debts into the absolute minimum number of transfers (e.g. \"Carlos pays €15.50 to Lucia\"). With just a few direct payments, all trip balances are 100% settled."
    ),

    // -------------------------------------------------------------------------
    // 5. DIVISAS, MONEDAS Y PRESUPUESTOS
    // -------------------------------------------------------------------------
    FaqItem(
        id = "faq_change_currency",
        categoryEs = "Divisas y Presupuestos",
        categoryEn = "Currencies & Budgets",
        questionEs = "¿Cómo cambio la divisa o el formato de moneda?",
        questionEn = "How do I change currency or currency format?",
        answerEs = "La divisa base del viaje se define al crearlo o editarlo en la pestaña \"Viajes\" (por ejemplo EUR, USD, GBP, JPY, MXN...). Todos los reportes y balances se calculan en esa moneda. Además, al añadir un gasto individual puedes elegir una divisa extranjera e indicar el tipo de cambio aplicado para convertirlo con precisión a la moneda de tu viaje.",
        answerEn = "The trip's base currency is chosen when creating or editing it on the \"Trips\" tab (e.g. EUR, USD, GBP, JPY, MXN...). All reports and balances are calculated in that currency. In addition, when logging individual expenses you can pick a foreign currency and enter the exchange rate to accurately convert it into your trip currency."
    ),
    FaqItem(
        id = "faq_budget_alerts",
        categoryEs = "Divisas y Presupuestos",
        categoryEn = "Currencies & Budgets",
        questionEs = "¿Cómo funciona el presupuesto y la barra de gasto del viaje?",
        questionEn = "How does the budget and trip spending bar work?",
        answerEs = "Si asignas un presupuesto a tu viaje al crearlo o editarlo, verás una barra de progreso que indica el porcentaje consumido en tiempo real. Si el gasto acumulado supera el 90% del límite fijado, la barra cambia a color rojo para alertar al grupo de que se está alcanzando el tope previsto.",
        answerEn = "If you assign a budget to your trip when creating or editing it, you will see a progress bar indicating the percentage used in real time. If total spending exceeds 90% of the limit, the bar turns coral red to alert the group that the limit is close."
    ),

    // -------------------------------------------------------------------------
    // 6. EXPORTACIÓN, INFORMES Y COPIAS DE SEGURIDAD
    // -------------------------------------------------------------------------
    FaqItem(
        id = "faq_export_zip",
        categoryEs = "Exportación y Respaldo",
        categoryEn = "Exports & Backup",
        questionEs = "¿Cómo exporto un viaje a ZIP?",
        questionEn = "How do I export a trip to ZIP?",
        answerEs = "Ve a la pestaña \"Ajustes\", localiza el bloque \"Exportar e Importar\" y pulsa \"Exportar\". Puedes elegir exportar \"Todos los viajes\" o un viaje individual. El archivo comprimido .zip contiene toda la información en JSON estructurado y despliega la hoja nativa de compartir de Android para que lo envíes o guardes.",
        answerEn = "Go to the \"Settings\" tab, locate the \"Export and Import\" section, and tap \"Export\". You can choose to export \"All trips\" or an individual trip. The compressed .zip file contains all data in structured JSON and opens the native Android Share Sheet to send or save."
    ),
    FaqItem(
        id = "faq_export_pdf",
        categoryEs = "Exportación y Respaldo",
        categoryEn = "Exports & Backup",
        questionEs = "¿Cómo genero el PDF de resumen de un viaje?",
        questionEn = "How do I generate a trip summary PDF report?",
        answerEs = "En la pestaña \"Viajes\", pulsa en el botón \"Exportar a PDF\" de la tarjeta del viaje (o desde las opciones del viaje activo). Travel Cash genera un reporte PDF multipágina con título, resumen financiero, gráficos por categorías, tabla de participantes y liquidación de deudas, abriendo el selector de compartir para enviarlo o abrirlo en tu visor de documentos.",
        answerEn = "In the \"Trips\" tab, tap the \"Export to PDF\" button on the trip card (or from the active trip options). Travel Cash generates a multi-page PDF report with the trip title, financial summary, category breakdowns, participant balances, and debt settlements, opening the Share Sheet to send or view it."
    ),
    FaqItem(
        id = "faq_backup_restore",
        categoryEs = "Exportación y Respaldo",
        categoryEn = "Exports & Backup",
        questionEs = "¿Cómo hago una copia de seguridad o restauro un viaje?",
        questionEn = "How do I back up or restore a trip?",
        answerEs = "Para respaldar tus viajes, exporta el archivo .zip desde Ajustes o el .json desde la pestaña Viajes. Guarda ese archivo en Google Drive o tu correo. Cuando cambies de teléfono o reinstales la app, pulsa el botón \"Importar\" y selecciona dicho archivo para restaurar todos tus viajes, gastos y participantes sin perder ningún dato.",
        answerEn = "To back up your trips, export the .zip file from Settings or .json from the Trips tab. Save that file to Google Drive or your email. When switching devices or reinstalling, tap \"Import\" and choose that file to restore all your trips, expenses, and participants without losing any data."
    ),
    FaqItem(
        id = "faq_share_sheet",
        categoryEs = "Exportación y Respaldo",
        categoryEn = "Exports & Backup",
        questionEs = "¿Cómo comparto los archivos directamente por WhatsApp, Correo o Google Drive?",
        questionEn = "How do I share files directly via WhatsApp, Email, or Google Drive?",
        answerEs = "Al pulsar en cualquier opción de exportar (PDF, CSV, JSON o ZIP), Travel Cash guarda el archivo de manera temporal y despliega inmediatamente el menú nativo de compartir de Android (Share Sheet). Desde ese menú puedes elegir directamente la aplicación destino: WhatsApp, Gmail, Telegram, guardar en Drive o descargarlo a tu dispositivo.",
        answerEn = "When tapping any export action (PDF, CSV, JSON, or ZIP), Travel Cash saves the file temporarily and immediately opens the native Android Share Sheet. From there you can directly pick your destination app: WhatsApp, Gmail, Telegram, save to Google Drive, or save to device Downloads."
    ),

    // -------------------------------------------------------------------------
    // 7. AJUSTES Y CONFIGURACIÓN
    // -------------------------------------------------------------------------
    FaqItem(
        id = "faq_change_language",
        categoryEs = "Ajustes y Personalización",
        categoryEn = "Settings & Customization",
        questionEs = "¿Cómo cambio el idioma de la app?",
        questionEn = "How do I switch the app language?",
        answerEs = "Ve a la pestaña \"Ajustes\" y en el selector de Idioma elige tu idioma preferido entre Español, Inglés, Francés, Italiano, Chino, Holandés, Alemán y Polaco. Toda la interfaz, los menús, las cuentas, los reportes PDF y esta sección de Ayuda cambiarán de inmediato al idioma seleccionado.",
        answerEn = "Go to the \"Settings\" tab and under the Language selector choose your preferred language among Spanish, English, French, Italian, Chinese, Dutch, German, and Polish. The entire interface, menus, calculations, PDF reports, and this Help section will switch immediately to the selected language."
    ),
    FaqItem(
        id = "faq_daily_notifications",
        categoryEs = "Ajustes y Personalización",
        categoryEn = "Settings & Customization",
        questionEs = "¿Cómo activo las notificaciones de recordatorio diario de gastos?",
        questionEn = "How do I enable daily expense reminder notifications?",
        answerEs = "En la pestaña \"Ajustes\", localiza la tarjeta \"Recordatorio Diario de Gastos\" y activa el interruptor. Puedes fijar la hora deseada (por ejemplo a las 21:00) para recibir una notificación que te recuerde anotar los gastos del día antes de finalizar la jornada.",
        answerEn = "In the \"Settings\" tab, find the \"Daily Expense Reminder\" card and toggle the switch on. You can configure your preferred reminder time (for example 9:00 PM) to receive a notification reminding you to log the day's expenses before going to sleep."
    )
)

/**
 * Pantalla completa modal de Ayuda y Preguntas Frecuentes.
 */
@Composable
fun HelpFaqModal(
    onDismiss: () -> Unit
) {
    val currentLang = LocalAppLanguage.current
    var searchQuery by remember { mutableStateOf("") }
    var expandedItemIds by remember { mutableStateOf(setOf<String>()) }

    val titleText = when (currentLang) {
        AppLanguage.ES -> "Ayuda y Preguntas Frecuentes"
        AppLanguage.EN -> "Help & FAQ"
        AppLanguage.FR -> "Aide et FAQ"
        AppLanguage.IT -> "Aiuto e FAQ"
        AppLanguage.ZH -> "帮助与常见问题"
        AppLanguage.NL -> "Help & FAQ"
        AppLanguage.DE -> "Hilfe & FAQ"
        AppLanguage.PL -> "Pomoc i FAQ"
    }

    val backDescription = when (currentLang) {
        AppLanguage.ES -> "Volver"
        AppLanguage.EN -> "Back"
        AppLanguage.FR -> "Retour"
        AppLanguage.IT -> "Torna indietro"
        AppLanguage.ZH -> "返回"
        AppLanguage.NL -> "Terug"
        AppLanguage.DE -> "Zurück"
        AppLanguage.PL -> "Wstecz"
    }

    val searchPlaceholder = when (currentLang) {
        AppLanguage.ES -> "Buscar preguntas, términos o soluciones…"
        AppLanguage.EN -> "Search questions, terms or solutions…"
        AppLanguage.FR -> "Rechercher des questions, termes ou solutions…"
        AppLanguage.IT -> "Cerca domande, termini o soluzioni…"
        AppLanguage.ZH -> "搜索问题、术语或解决方案…"
        AppLanguage.NL -> "Zoek vragen, termen of oplossingen…"
        AppLanguage.DE -> "Fragen, Begriffe oder Lösungen suchen…"
        AppLanguage.PL -> "Szukaj pytań, haseł lub rozwiązań…"
    }

    val clearSearchText = when (currentLang) {
        AppLanguage.ES -> "Limpiar búsqueda"
        AppLanguage.EN -> "Clear search"
        AppLanguage.FR -> "Effacer la recherche"
        AppLanguage.IT -> "Cancella ricerca"
        AppLanguage.ZH -> "清除搜索"
        AppLanguage.NL -> "Zoekopdracht wissen"
        AppLanguage.DE -> "Suche löschen"
        AppLanguage.PL -> "Wyczyść wyszukiwanie"
    }

    val noResultsTitle = when (currentLang) {
        AppLanguage.ES -> "No se han encontrado resultados para tu búsqueda"
        AppLanguage.EN -> "No results found for your search"
        AppLanguage.FR -> "Aucun résultat trouvé pour votre recherche"
        AppLanguage.IT -> "Nessun risultato trovato per la tua ricerca"
        AppLanguage.ZH -> "未找到符合您搜索条件的结果"
        AppLanguage.NL -> "Geen resultaten gevonden voor je zoekopdracht"
        AppLanguage.DE -> "Keine Ergebnisse für Ihre Suche gefunden"
        AppLanguage.PL -> "Nie znaleziono wyników dla Twojego wyszukiwania"
    }

    val noResultsSubtitle = when (currentLang) {
        AppLanguage.ES -> "Prueba a buscar términos como \"bote\", \"gastos\", \"repartir\", \"presupuesto\" o \"backup\"."
        AppLanguage.EN -> "Try searching for keywords like \"pot\", \"expenses\", \"split\", \"budget\" or \"backup\"."
        AppLanguage.FR -> "Essayez de rechercher des termes comme « cagnotte », « dépenses », « partage », « budget » ou « sauvegarde »."
        AppLanguage.IT -> "Prova a cercare termini come \"cassa comune\", \"spese\", \"divisione\", \"budget\" o \"backup\"."
        AppLanguage.ZH -> "尝试搜索诸如“公账”、“支出”、“分摊”、“预算”或“备份”等词汇。"
        AppLanguage.NL -> "Probeer te zoeken op termen zoals \"pot\", \"uitgaven\", \"splitsen\", \"budget\" of \"back-up\"."
        AppLanguage.DE -> "Versuchen Sie Begriffe wie „Gemeinschaftskasse“, „Ausgaben“, „Aufteilung“, „Budget“ oder „Backup“."
        AppLanguage.PL -> "Spróbuj wyszukać hasła takie jak „wspólna kasa”, „wydatki”, „podział”, „budżet” lub „kopia zapasowa”."
    }

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
                .navigationBarsPadding()
                .imePadding(),
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
                            contentDescription = backDescription,
                            tint = OceanNavy
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = titleText,
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
                            text = searchPlaceholder,
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
                                    contentDescription = clearSearchText,
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

                val filteredFaqs = remember(normalizedQuery, currentLang) {
                    if (tokens.isEmpty()) {
                        TRAVEL_CASH_FAQS
                    } else {
                        TRAVEL_CASH_FAQS.filter { item ->
                            val qText = item.getQuestion(currentLang)
                            val aText = item.getAnswer(currentLang)
                            val catText = item.getCategory(currentLang)
                            val normQ = normalizeFaqSearchText(qText)
                            val normA = normalizeFaqSearchText(aText)
                            val normCat = normalizeFaqSearchText(catText)

                            val normQEs = normalizeFaqSearchText(item.questionEs)
                            val normAEs = normalizeFaqSearchText(item.answerEs)
                            val normQEn = normalizeFaqSearchText(item.questionEn)
                            val normAEn = normalizeFaqSearchText(item.answerEn)

                            tokens.all { token ->
                                normQ.contains(token) || normA.contains(token) || normCat.contains(token) ||
                                normQEs.contains(token) || normAEs.contains(token) ||
                                normQEn.contains(token) || normAEn.contains(token)
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
                                    text = noResultsTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = OceanNavy,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = noResultsSubtitle,
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
                                        text = clearSearchText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Lista de preguntas y respuestas en acordeón con margen inferior amplio para barra de navegación del sistema
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("faq_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
                    ) {
                        items(filteredFaqs, key = { it.id }) { item ->
                            val isExpanded = expandedItemIds.contains(item.id)
                            val question = item.getQuestion(currentLang)
                            val answer = item.getAnswer(currentLang)
                            val category = item.getCategory(currentLang)

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
                                            Column {
                                                Text(
                                                    text = category,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 11.sp
                                                    ),
                                                    color = TravelBlue
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = question,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    ),
                                                    color = OceanNavy
                                                )
                                            }
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
                                                    lineHeight = 22.sp,
                                                    fontSize = 13.5.sp
                                                ),
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                }
            }
        }
    }
}
