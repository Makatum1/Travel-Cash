package com.example

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplit
import com.example.data.model.IncomeEntity
import com.example.data.model.CategoryCatalog
import com.example.data.model.ParticipantEntity
import com.example.data.model.TripEntity
import com.example.domain.DebtCalculator
import com.example.util.FormatUtils
import com.example.util.UiTexts
import com.example.ui.components.ActiveTripHeaderAndBudgetCard
import com.example.ui.components.EditableParticipantAvatar
import com.example.ui.components.EditableTripRouteIcon
import com.example.ui.components.TopFeedbackToast
import com.example.ui.components.TravelCashTopBar
import com.example.ui.components.TravelLocationPinIcon
import com.example.ui.modals.AddExpenseModal
import com.example.ui.modals.CreateTripModal
import com.example.ui.modals.CsvColumnMappingModal
import com.example.ui.modals.IncomeModal
import com.example.ui.modals.SuggestionsModal
import com.example.ui.screens.ExpensesTabScreen
import com.example.ui.screens.HelpFaqModal
import com.example.ui.screens.MapTabScreen
import com.example.ui.screens.SettingsTabScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SplitsTabScreen
import com.example.ui.screens.StatsTabScreen
import com.example.ui.screens.TripsTabScreen
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.isEnglish
import com.example.ui.theme.CoralRed
import com.example.ui.theme.TravelBlue
import com.example.ui.theme.TravelBlueLight
import com.example.ui.theme.TravelCashTheme
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.TravelCashViewModel
import com.example.util.ExportImportManager
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: TravelCashViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
            val language by vm.language.collectAsStateWithLifecycle()

            TravelCashTheme(
                themeMode = themeMode,
                language = language
            ) {
                var isSplashActive by remember { mutableStateOf(true) }

                if (isSplashActive) {
                    SplashScreen(
                        onSplashFinished = { isSplashActive = false }
                    )
                } else {
                    TravelCashApp(viewModel = vm)
                }
            }
        }
    }
}

@Composable
fun TravelCashApp(
    viewModel: TravelCashViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val placeSuggestions by viewModel.placeSuggestions.collectAsStateWithLifecycle()
    val isSearchingPlaces by viewModel.isSearchingPlaces.collectAsStateWithLifecycle()
    val pendingCsvManualImport by viewModel.pendingCsvManualImport.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val reminderNotificationsEnabled by viewModel.reminderNotificationsEnabled.collectAsStateWithLifecycle()
    val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()
    val texts = UiTexts.get(language)

    fun sanitizeTripFileName(title: String, extension: String): String {
        val clean = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        val finalName = if (clean.isEmpty()) "viaje" else clean
        return "$finalName.$extension"
    }

    // Estados de modales
    var showAddOrEditExpenseModal by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    var showIncomeModal by remember { mutableStateOf(false) }
    var editingIncome by remember { mutableStateOf<IncomeEntity?>(null) }

    var showCreateTripModal by remember { mutableStateOf(false) }
    var editingTrip by remember { mutableStateOf<TripEntity?>(null) }
    var showSuggestionsModal by remember { mutableStateOf(false) }
    var showHelpFaqModal by remember { mutableStateOf(false) }

    // Apertura automática del modal de primer viaje si la base de datos está limpia/vacía
    var hasAutoPromptedFirstTrip by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(uiState.isInitialized, uiState.trips.isEmpty()) {
        if (uiState.isInitialized && uiState.trips.isEmpty() && !hasAutoPromptedFirstTrip) {
            hasAutoPromptedFirstTrip = true
            editingTrip = null
            showCreateTripModal = true
        }
    }

    // 1. Estado para cambiar la foto del icono de una ruta/viaje (o dejar el de carretera por defecto)
    var tripForIconPhotoDialog by remember { mutableStateOf<TripEntity?>(null) }

    val tripPhotoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val targetTrip = tripForIconPhotoDialog ?: uiState.activeTrip
        if (uri != null && targetTrip != null) {
            try {
                val destFile = File(context.filesDir, "trip_icon_${targetTrip.id}_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.updateTrip(targetTrip.copy(headerPhotoUri = Uri.fromFile(destFile).toString()))
                tripForIconPhotoDialog = null
            } catch (e: Exception) {
                val saveRoutePhotoError = when (language) {
                    AppLanguage.ES -> "Error al guardar la foto de la ruta"
                    AppLanguage.EN -> "Error saving route photo"
                    AppLanguage.FR -> "Erreur lors de l'enregistrement de la photo de l'itinéraire"
                    AppLanguage.IT -> "Errore durante il salvataggio della foto dell'itinerario"
                    AppLanguage.ZH -> "保存路线照片时出错"
                    AppLanguage.NL -> "Fout bij opslaan van routefoto"
                    AppLanguage.DE -> "Fehler beim Speichern des Routenfotos"
                    AppLanguage.PL -> "Błąd podczas zapisywania zdjęcia trasy"
                    AppLanguage.PT -> "Erro ao guardar a foto da rota"
                }
                viewModel.showToast(saveRoutePhotoError, isError = true)
            }
        }
    }

    val tripPhotoCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        val targetTrip = tripForIconPhotoDialog ?: uiState.activeTrip
        if (bitmap != null && targetTrip != null) {
            try {
                val destFile = File(context.filesDir, "trip_icon_${targetTrip.id}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                viewModel.updateTrip(targetTrip.copy(headerPhotoUri = Uri.fromFile(destFile).toString()))
                tripForIconPhotoDialog = null
            } catch (e: Exception) {
                val saveCameraPhotoError = when (language) {
                    AppLanguage.ES -> "Error al guardar la foto de la cámara"
                    AppLanguage.EN -> "Error saving camera photo"
                    AppLanguage.FR -> "Erreur lors de l'enregistrement de la photo"
                    AppLanguage.IT -> "Errore durante il salvataggio della foto"
                    AppLanguage.ZH -> "保存相机照片时出错"
                    AppLanguage.NL -> "Fout bij opslaan van camerafoto"
                    AppLanguage.DE -> "Fehler beim Speichern des Kamerafotos"
                    AppLanguage.PL -> "Błąd podczas zapisywania zdjęcia z aparatu"
                    AppLanguage.PT -> "Erro ao guardar a foto da câmara"
                }
                viewModel.showToast(saveCameraPhotoError, isError = true)
            }
        }
    }

    // 2. Estado para añadir/cambiar la foto de un participante (o dejar inicial + color por defecto)
    var participantForPhotoDialog by remember { mutableStateOf<ParticipantEntity?>(null) }

    val participantPhotoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val targetParticipant = participantForPhotoDialog
        if (uri != null && targetParticipant != null) {
            try {
                val destFile = File(context.filesDir, "part_avatar_${targetParticipant.id}_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.updateParticipantPhoto(targetParticipant, Uri.fromFile(destFile).toString())
                participantForPhotoDialog = null
            } catch (e: Exception) {
                val savePartPhotoError = when (language) {
                    AppLanguage.ES -> "Error al guardar la foto del participante"
                    AppLanguage.EN -> "Error saving participant photo"
                    AppLanguage.FR -> "Erreur lors de l'enregistrement de la photo du participant"
                    AppLanguage.IT -> "Errore durante il salvataggio della foto del partecipante"
                    AppLanguage.ZH -> "保存参与者照片时出错"
                    AppLanguage.NL -> "Fout bij opslaan van foto van deelnemer"
                    AppLanguage.DE -> "Fehler beim Speichern des Teilnehmerfotos"
                    AppLanguage.PL -> "Błąd podczas zapisywania zdjęcia uczestnika"
                    AppLanguage.PT -> "Erro ao guardar a foto do participante"
                }
                viewModel.showToast(savePartPhotoError, isError = true)
            }
        }
    }

    val participantPhotoCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        val targetParticipant = participantForPhotoDialog
        if (bitmap != null && targetParticipant != null) {
            try {
                val destFile = File(context.filesDir, "part_avatar_${targetParticipant.id}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                viewModel.updateParticipantPhoto(targetParticipant, Uri.fromFile(destFile).toString())
                participantForPhotoDialog = null
            } catch (e: Exception) {
                val saveCameraPhotoError = when (language) {
                    AppLanguage.ES -> "Error al guardar la foto de la cámara"
                    AppLanguage.EN -> "Error saving camera photo"
                    AppLanguage.FR -> "Erreur lors de l'enregistrement de la photo"
                    AppLanguage.IT -> "Errore durante il salvataggio della foto"
                    AppLanguage.ZH -> "保存相机照片时出错"
                    AppLanguage.NL -> "Fout bij opslaan van camerafoto"
                    AppLanguage.DE -> "Fehler beim Speichern des Kamerafotos"
                    AppLanguage.PL -> "Błąd podczas zapisywania zdjęcia z aparatu"
                    AppLanguage.PT -> "Erro ao guardar a foto da câmara"
                }
                viewModel.showToast(saveCameraPhotoError, isError = true)
            }
        }
    }

    // Estado auxiliar para importaciones
    var csvImportCreatesNewTrip by remember { mutableStateOf(false) }

    // Launcher para importar archivo CSV
    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
                } ?: ""
                val csvSuggestedName = when (language) {
                    AppLanguage.ES -> "Viaje Importado CSV"
                    AppLanguage.EN -> "Imported CSV Trip"
                    AppLanguage.FR -> "Voyage importé CSV"
                    AppLanguage.IT -> "Viaggio importato CSV"
                    AppLanguage.ZH -> "导入的 CSV 行程"
                    AppLanguage.NL -> "Geïmporteerde CSV-reis"
                    AppLanguage.DE -> "Importierte CSV-Reise"
                    AppLanguage.PL -> "Zaimportowana podróż CSV"
                    AppLanguage.PT -> "Viagem Importada CSV"
                }
                viewModel.processCsvImportContent(
                    csvContent = content,
                    createNewTrip = csvImportCreatesNewTrip,
                    suggestedFileName = csvSuggestedName
                )
            } catch (e: Exception) {
                val csvReadError = when (language) {
                    AppLanguage.ES -> "No se pudo leer el archivo CSV seleccionado"
                    AppLanguage.EN -> "Could not read selected CSV file"
                    AppLanguage.FR -> "Impossible de lire le fichier CSV sélectionné"
                    AppLanguage.IT -> "Impossibile leggere il file CSV selezionato"
                    AppLanguage.ZH -> "无法读取所选的 CSV 文件"
                    AppLanguage.NL -> "Kan het geselecteerde CSV-bestand niet lezen"
                    AppLanguage.DE -> "Ausgewählte CSV-Datei konnte nicht gelesen werden"
                    AppLanguage.PL -> "Nie można odczytać wybranego pliku CSV"
                    AppLanguage.PT -> "Não foi possível ler o ficheiro CSV selecionado"
                }
                viewModel.showToast(csvReadError, isError = true)
            }
        }
    }

    // Launcher para importar archivo ZIP o JSON de viaje
    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importTripsFromUri(uri)
        }
    }

    // Función para invocar el selector nativo de compartir de Android (Share Sheet / Intent Chooser)
    fun shareGeneratedFile(file: File, mimeType: String, dialogTitle: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, dialogTitle)
                putExtra(Intent.EXTRA_TEXT, dialogTitle)
                clipData = ClipData.newRawUri(dialogTitle, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserIntent = Intent.createChooser(sendIntent, dialogTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            val shareError = when (language) {
                AppLanguage.ES -> "Error al compartir archivo: ${e.message}"
                AppLanguage.EN -> "Error sharing file: ${e.message}"
                AppLanguage.FR -> "Erreur lors du partage du fichier : ${e.message}"
                AppLanguage.IT -> "Errore durante la condivisione del file: ${e.message}"
                AppLanguage.ZH -> "分享文件时出错: ${e.message}"
                AppLanguage.NL -> "Fout bij delen van bestand: ${e.message}"
                AppLanguage.DE -> "Fehler beim Teilen der Datei: ${e.message}"
                AppLanguage.PL -> "Błąd podczas udostępniania pliku: ${e.message}"
                AppLanguage.PT -> "Erro ao partilhar ficheiro: ${e.message}"
            }
            viewModel.showToast(shareError, isError = true)
        }
    }

    fun exportAndShareTripPdf(trip: TripEntity) {
        try {
            val fileName = sanitizeTripFileName(trip.title, "pdf")
            val targetFile = File(context.cacheDir, fileName)
            val parts = uiState.allParticipants.filter { it.tripId == trip.id }
            val exps = uiState.allExpenses.filter { it.tripId == trip.id }
            val incs = uiState.allIncomes.filter { it.tripId == trip.id }
            FileOutputStream(targetFile).use { out ->
                ExportImportManager.writeTripPdfReport(out, trip, parts, exps, incs, context, language)
            }
            val title = when (language) {
                AppLanguage.ES -> "Compartir Reporte PDF - ${trip.title}"
                AppLanguage.EN -> "Share PDF Report - ${trip.title}"
                AppLanguage.FR -> "Partager le rapport PDF - ${trip.title}"
                AppLanguage.IT -> "Condividi report PDF - ${trip.title}"
                AppLanguage.ZH -> "分享 PDF 报表 - ${trip.title}"
                AppLanguage.NL -> "PDF-rapport delen - ${trip.title}"
                AppLanguage.DE -> "PDF-Bericht teilen - ${trip.title}"
                AppLanguage.PL -> "Udostępnij raport PDF - ${trip.title}"
                AppLanguage.PT -> "Partilhar Relatório PDF - ${trip.title}"
            }
            shareGeneratedFile(
                file = targetFile,
                mimeType = "application/pdf",
                dialogTitle = title
            )
        } catch (e: Exception) {
            val errorMsg = when (language) {
                AppLanguage.ES -> "Error al generar el reporte PDF"
                AppLanguage.EN -> "Error generating PDF report"
                AppLanguage.FR -> "Erreur lors de la génération du rapport PDF"
                AppLanguage.IT -> "Errore durante la generazione del report PDF"
                AppLanguage.ZH -> "生成 PDF 报表时出错"
                AppLanguage.NL -> "Fout bij genereren van PDF-rapport"
                AppLanguage.DE -> "Fehler beim Erstellen des PDF-Berichts"
                AppLanguage.PL -> "Błąd podczas generowania raportu PDF"
                AppLanguage.PT -> "Erro ao gerar o relatório PDF"
            }
            viewModel.showToast(errorMsg, isError = true)
        }
    }

    fun exportAndShareTripCsv(trip: TripEntity) {
        try {
            val fileName = sanitizeTripFileName(trip.title, "csv")
            val targetFile = File(context.cacheDir, fileName)
            val parts = uiState.allParticipants.filter { it.tripId == trip.id }
            val exps = uiState.allExpenses.filter { it.tripId == trip.id }
            val csvStr = ExportImportManager.generateTripExpensesCsv(trip, parts, exps)
            targetFile.writeText(csvStr, Charsets.UTF_8)
            val title = when (language) {
                AppLanguage.ES -> "Compartir Gastos CSV - ${trip.title}"
                AppLanguage.EN -> "Share CSV Expenses - ${trip.title}"
                AppLanguage.FR -> "Partager les dépenses CSV - ${trip.title}"
                AppLanguage.IT -> "Condividi spese CSV - ${trip.title}"
                AppLanguage.ZH -> "分享 CSV 支出 - ${trip.title}"
                AppLanguage.NL -> "CSV-uitgaven delen - ${trip.title}"
                AppLanguage.DE -> "CSV-Ausgaben teilen - ${trip.title}"
                AppLanguage.PL -> "Udostępnij wydatki CSV - ${trip.title}"
                AppLanguage.PT -> "Partilhar Despesas CSV - ${trip.title}"
            }
            shareGeneratedFile(
                file = targetFile,
                mimeType = "text/csv",
                dialogTitle = title
            )
        } catch (e: Exception) {
            val errorMsg = when (language) {
                AppLanguage.ES -> "Error al exportar CSV"
                AppLanguage.EN -> "Error exporting CSV"
                AppLanguage.FR -> "Erreur lors de l'exportation CSV"
                AppLanguage.IT -> "Errore durante l'esportazione CSV"
                AppLanguage.ZH -> "导出 CSV 时出错"
                AppLanguage.NL -> "Fout bij exporteren van CSV"
                AppLanguage.DE -> "Fehler beim Exportieren von CSV"
                AppLanguage.PL -> "Błąd podczas eksportowania CSV"
                AppLanguage.PT -> "Erro ao exportar CSV"
            }
            viewModel.showToast(errorMsg, isError = true)
        }
    }

    fun exportAndShareTripJson(trip: TripEntity) {
        try {
            val clean = trip.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            val safeName = if (clean.isEmpty()) "Viaje" else clean
            val targetFile = File(context.cacheDir, "Viaje_${safeName}.json")
            val parts = uiState.allParticipants.filter { it.tripId == trip.id }
            val exps = uiState.allExpenses.filter { it.tripId == trip.id }
            val incs = uiState.allIncomes.filter { it.tripId == trip.id }
            val jsonStr = ExportImportManager.exportFullBackupJson(
                trips = listOf(trip),
                participants = parts,
                expenses = exps,
                incomes = incs
            )
            targetFile.writeText(jsonStr, Charsets.UTF_8)
            val title = when (language) {
                AppLanguage.ES -> "Compartir Viaje JSON - ${trip.title}"
                AppLanguage.EN -> "Share Trip JSON - ${trip.title}"
                AppLanguage.FR -> "Partager le voyage JSON - ${trip.title}"
                AppLanguage.IT -> "Condividi viaggio JSON - ${trip.title}"
                AppLanguage.ZH -> "分享行程 JSON - ${trip.title}"
                AppLanguage.NL -> "Reis JSON delen - ${trip.title}"
                AppLanguage.DE -> "Reise-JSON teilen - ${trip.title}"
                AppLanguage.PL -> "Udostępnij podróż JSON - ${trip.title}"
                AppLanguage.PT -> "Partilhar Viagem JSON - ${trip.title}"
            }
            shareGeneratedFile(
                file = targetFile,
                mimeType = "application/json",
                dialogTitle = title
            )
        } catch (e: Exception) {
            val errorMsg = when (language) {
                AppLanguage.ES -> "Error al exportar JSON"
                AppLanguage.EN -> "Error exporting JSON"
                AppLanguage.FR -> "Erreur lors de l'exportation JSON"
                AppLanguage.IT -> "Errore durante l'esportazione JSON"
                AppLanguage.ZH -> "导出 JSON 时出错"
                AppLanguage.NL -> "Fout bij exporteren van JSON"
                AppLanguage.DE -> "Fehler beim Exportieren von JSON"
                AppLanguage.PL -> "Błąd podczas eksportowania JSON"
                AppLanguage.PT -> "Erro ao exportar JSON"
            }
            viewModel.showToast(errorMsg, isError = true)
        }
    }

    fun exportAndShareZip(targetTrip: TripEntity?) {
        try {
            val fileName = if (targetTrip != null) {
                val clean = targetTrip.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
                val safeName = if (clean.isEmpty()) "viaje" else clean
                "viaje_${safeName}.zip"
            } else {
                "travel_cash_todos_los_viajes.zip"
            }
            val targetFile = File(context.cacheDir, fileName)
            val tripsToExport = if (targetTrip != null) {
                uiState.trips.filter { it.id == targetTrip.id }
            } else {
                uiState.trips
            }
            val parts = if (targetTrip != null) {
                uiState.allParticipants.filter { it.tripId == targetTrip.id }
            } else {
                uiState.allParticipants
            }
            val exps = if (targetTrip != null) {
                uiState.allExpenses.filter { it.tripId == targetTrip.id }
            } else {
                uiState.allExpenses
            }
            val incs = if (targetTrip != null) {
                uiState.allIncomes.filter { it.tripId == targetTrip.id }
            } else {
                uiState.allIncomes
            }
            FileOutputStream(targetFile).use { out ->
                ExportImportManager.writeTripsToZip(out, tripsToExport, parts, exps, incs)
            }
            val title = if (targetTrip != null) {
                when (language) {
                    AppLanguage.ES -> "Compartir Viaje ZIP - ${targetTrip.title}"
                    AppLanguage.EN -> "Share Trip ZIP - ${targetTrip.title}"
                    AppLanguage.FR -> "Partager le voyage ZIP - ${targetTrip.title}"
                    AppLanguage.IT -> "Condividi viaggio ZIP - ${targetTrip.title}"
                    AppLanguage.ZH -> "分享行程 ZIP - ${targetTrip.title}"
                    AppLanguage.NL -> "Reis ZIP delen - ${targetTrip.title}"
                    AppLanguage.DE -> "Reise-ZIP teilen - ${targetTrip.title}"
                    AppLanguage.PL -> "Udostępnij podróż ZIP - ${targetTrip.title}"
                    AppLanguage.PT -> "Partilhar Viagem ZIP - ${targetTrip.title}"
                }
            } else {
                when (language) {
                    AppLanguage.ES -> "Compartir Copia de Seguridad ZIP"
                    AppLanguage.EN -> "Share Travel Cash Backup ZIP"
                    AppLanguage.FR -> "Partager la sauvegarde ZIP Travel Cash"
                    AppLanguage.IT -> "Condividi backup ZIP Travel Cash"
                    AppLanguage.ZH -> "分享 Travel Cash 备份 ZIP"
                    AppLanguage.NL -> "Travel Cash back-up ZIP delen"
                    AppLanguage.DE -> "Travel Cash Backup-ZIP teilen"
                    AppLanguage.PL -> "Udostępnij kopię zapasową ZIP Travel Cash"
                    AppLanguage.PT -> "Partilhar Cópia de Segurança ZIP"
                }
            }
            shareGeneratedFile(
                file = targetFile,
                mimeType = "application/zip",
                dialogTitle = title
            )
        } catch (e: Exception) {
            val errorMsg = when (language) {
                AppLanguage.ES -> "Error al exportar ZIP"
                AppLanguage.EN -> "Error exporting ZIP"
                AppLanguage.FR -> "Erreur lors de l'exportation ZIP"
                AppLanguage.IT -> "Errore durante l'esportazione ZIP"
                AppLanguage.ZH -> "导出 ZIP 时出错"
                AppLanguage.NL -> "Fout bij exporteren van ZIP"
                AppLanguage.DE -> "Fehler beim Exportieren von ZIP"
                AppLanguage.PL -> "Błąd podczas eksportowania ZIP"
                AppLanguage.PT -> "Erro ao exportar ZIP"
            }
            viewModel.showToast(errorMsg, isError = true)
        }
    }

    // BackHandler en pestañas secundarias para volver a la pestaña principal Gastos
    if (selectedTab != MainTab.EXPENSES) {
        BackHandler {
            viewModel.selectTab(MainTab.EXPENSES)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TravelCashTopBar(
                trips = uiState.trips,
                activeTrip = uiState.activeTrip,
                onSelectTrip = { viewModel.selectActiveTrip(it) },
                onCreateNewTripClick = {
                    editingTrip = null
                    showCreateTripModal = true
                },
                onImportNewTripCsvClick = {
                    csvImportCreatesNewTrip = true
                    importCsvLauncher.launch(arrayOf("text/*", "application/csv", "*/*"))
                },
                onSuggestionsClick = { showSuggestionsModal = true }
            )
        },
        floatingActionButton = {
            if (selectedTab == MainTab.EXPENSES && uiState.activeTrip != null) {
                val addExpenseLabel = when (language) {
                    AppLanguage.ES -> "Añadir Gasto"
                    AppLanguage.EN -> "Add Expense"
                    AppLanguage.FR -> "Ajouter une dépense"
                    AppLanguage.IT -> "Aggiungi spesa"
                    AppLanguage.ZH -> "添加支出"
                    AppLanguage.NL -> "Uitgave toevoegen"
                    AppLanguage.DE -> "Ausgabe hinzufügen"
                    AppLanguage.PL -> "Dodaj wydatek"
                    AppLanguage.PT -> "Adicionar Despesa"
                }
                ExtendedFloatingActionButton(
                    onClick = {
                        editingExpense = null
                        showAddOrEditExpenseModal = true
                    },
                    containerColor = TravelBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("fab_add_expense")
                ) {
                    Icon(Icons.Default.Add, contentDescription = addExpenseLabel)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = addExpenseLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Barra de navegación: Gastos, Cuentas, Estadística, Mapa, Viajes, Ajustes
                val navItems = listOf(
                    MainTab.EXPENSES to Icons.AutoMirrored.Filled.ReceiptLong,
                    MainTab.SPLITS to Icons.Default.Group,
                    MainTab.STATS to Icons.Default.BarChart,
                    MainTab.MAP to Icons.Default.Map,
                    MainTab.TRIPS to Icons.Default.FlightTakeoff,
                    MainTab.SETTINGS to Icons.Default.Settings
                )

                navItems.forEach { (tab, icon) ->
                    val selected = selectedTab == tab
                    val label = tab.getTitle(language)
                    val iconSize = if (tab == MainTab.SETTINGS) 20.dp else 25.dp
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            viewModel.selectTab(tab)
                        },
                        icon = {
                            if (tab == MainTab.MAP) {
                                TravelLocationPinIcon(
                                    tint = if (selected) TravelBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                    accentColor = if (selected) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                    size = iconSize,
                                    modifier = Modifier.size(iconSize)
                                )
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 9.5.sp,
                                    letterSpacing = (-0.2).sp
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TravelBlue,
                            selectedTextColor = TravelBlue,
                            indicatorColor = TravelBlueLight,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("bottom_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val activeTrip = uiState.activeTrip
            val activeTripHeaderContent: @Composable () -> Unit = {
                if (activeTrip != null) {
                    ActiveTripHeaderAndBudgetCard(
                        trip = activeTrip,
                        totalSpent = uiState.totalSpent,
                        totalIncome = uiState.totalIncome,
                        extraBalance = uiState.extraBalance,
                        availableBote = uiState.availableBote,
                        personalAdjustment = uiState.personalAdjustment,
                        onTripIconClick = {
                            tripForIconPhotoDialog = activeTrip
                        },
                        onExportCsvClick = {
                            exportAndShareTripCsv(activeTrip)
                        },
                        onExportPdfClick = {
                            exportAndShareTripPdf(activeTrip)
                        },
                        onImportCsvToTripClick = {
                            csvImportCreatesNewTrip = false
                            importCsvLauncher.launch(arrayOf("text/*", "application/csv", "*/*"))
                        },
                        onAddBoteIncomeClick = {
                            editingIncome = null
                            showIncomeModal = true
                        },
                        isSimplified = true
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val currentTab = selectedTab
                if (currentTab == MainTab.SETTINGS) {
                    SettingsTabScreen(
                        themeMode = themeMode,
                        language = language,
                        reminderNotificationsEnabled = reminderNotificationsEnabled,
                        reminderHour = reminderHour,
                        reminderMinute = reminderMinute,
                        trips = uiState.trips,
                        onThemeModeChange = viewModel::setThemeMode,
                        onLanguageChange = viewModel::setLanguage,
                        onReminderNotificationsChange = viewModel::setReminderNotificationsEnabled,
                        onReminderTimeChange = viewModel::setReminderTime,
                        onExportZipClick = { trip ->
                            if (trip == null && uiState.trips.isEmpty()) {
                                viewModel.showToast(texts.toastNoTripsToExport, isError = true)
                            } else {
                                exportAndShareZip(trip)
                            }
                        },
                        onImportZipClick = {
                            importZipLauncher.launch(arrayOf("*/*"))
                        }
                    )
                } else if (activeTrip != null) {
                    when (currentTab) {
                        MainTab.EXPENSES -> {
                            ExpensesTabScreen(
                                trip = activeTrip,
                                participants = uiState.activeParticipants,
                                expenses = uiState.activeExpenses,
                                searchQuery = searchQuery,
                                selectedDateFilter = selectedDateFilter,
                                selectedCategoryFilter = selectedCategoryFilter,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onDateFilterChange = viewModel::updateDateFilter,
                                onCategoryFilterChange = viewModel::updateCategoryFilter,
                                onClearAllFilters = viewModel::clearExpenseFilters,
                                onAddExpenseClick = {
                                    editingExpense = null
                                    showAddOrEditExpenseModal = true
                                },
                                onExpenseClick = { exp ->
                                    editingExpense = exp
                                    showAddOrEditExpenseModal = true
                                },
                                topHeader = activeTripHeaderContent
                            )
                        }

                        MainTab.SPLITS -> {
                            SplitsTabScreen(
                                trip = activeTrip,
                                participants = uiState.activeParticipants,
                                expenses = uiState.activeExpenses,
                                incomes = uiState.activeIncomes,
                                debtResult = uiState.debtResult,
                                onAddParticipant = viewModel::addParticipant,
                                onUpdateParticipantName = viewModel::updateParticipantName,
                                onParticipantAvatarClick = { participant ->
                                    participantForPhotoDialog = participant
                                },
                                onDeleteParticipant = viewModel::deleteParticipant,
                                onAddIncomeClick = {
                                    editingIncome = null
                                    showIncomeModal = true
                                },
                                onEditIncomeClick = { inc ->
                                    editingIncome = inc
                                    showIncomeModal = true
                                },
                                onDeleteIncomeClick = viewModel::deleteIncome,
                                onExpenseClick = { exp ->
                                    editingExpense = exp
                                    showAddOrEditExpenseModal = true
                                },
                                onSettleDebt = { tx, settledAmount, paymentMethod, settlementDate ->
                                    val expenseId = "settle-${java.util.UUID.randomUUID().toString().take(8)}"
                                    val fromName = if (tx.isFromBote) texts.commonPotName else tx.fromName
                                    val place = if (tx.isFromBote) {
                                        when (language) {
                                            AppLanguage.ES -> "Liquidación desde Bote Común a ${tx.toName}"
                                            AppLanguage.EN -> "Settlement from Common Pot to ${tx.toName}"
                                            AppLanguage.FR -> "Règlement depuis la cagnotte vers ${tx.toName}"
                                            AppLanguage.IT -> "Saldo da Cassa Comune a ${tx.toName}"
                                            AppLanguage.ZH -> "公账向 ${tx.toName} 结算"
                                            AppLanguage.NL -> "Vereffening vanuit de pot naar ${tx.toName}"
                                            AppLanguage.DE -> "Ausgleich aus Gemeinschaftskasse an ${tx.toName}"
                                            AppLanguage.PL -> "Rozliczenie ze wspólnej kasy dla ${tx.toName}"
                                            AppLanguage.PT -> "Liquidação do Fundo Comum para ${tx.toName}"
                                        }
                                    } else {
                                        when (language) {
                                            AppLanguage.ES -> "Liquidación de deuda: ${tx.fromName} → ${tx.toName}"
                                            AppLanguage.EN -> "Debt settlement: ${tx.fromName} → ${tx.toName}"
                                            AppLanguage.FR -> "Règlement de dette : ${tx.fromName} → ${tx.toName}"
                                            AppLanguage.IT -> "Saldo debito: ${tx.fromName} → ${tx.toName}"
                                            AppLanguage.ZH -> "债务结算: ${tx.fromName} → ${tx.toName}"
                                            AppLanguage.NL -> "Schuldaflossing: ${tx.fromName} → ${tx.toName}"
                                            AppLanguage.DE -> "Schuldenausgleich: ${tx.fromName} → ${tx.toName}"
                                            AppLanguage.PL -> "Rozliczenie długu: ${tx.fromName} → ${tx.toName}"
                                            AppLanguage.PT -> "Liquidação de dívida: ${tx.fromName} → ${tx.toName}"
                                        }
                                    }
                                    val payerId = if (tx.isFromBote) DebtCalculator.BOTE_COMUN_ID else tx.fromParticipantId
                                    val method = if (tx.isFromBote) "Bote Común" else paymentMethod
                                    val notes = when (language) {
                                        AppLanguage.ES -> "Saldado de cuenta ($fromName paga a ${tx.toName})"
                                        AppLanguage.EN -> "Settlement: $fromName pays ${tx.toName}"
                                        AppLanguage.FR -> "Règlement : $fromName paie à ${tx.toName}"
                                        AppLanguage.IT -> "Saldo: $fromName paga a ${tx.toName}"
                                        AppLanguage.ZH -> "结算: $fromName 支付给 ${tx.toName}"
                                        AppLanguage.NL -> "Vereffening: $fromName betaalt aan ${tx.toName}"
                                        AppLanguage.DE -> "Ausgleich: $fromName zahlt an ${tx.toName}"
                                        AppLanguage.PL -> "Rozliczenie: $fromName płaci dla ${tx.toName}"
                                        AppLanguage.PT -> "Liquidação: $fromName paga a ${tx.toName}"
                                    }
                                    val expense = ExpenseEntity(
                                        id = expenseId,
                                        tripId = activeTrip.id,
                                        amount = settledAmount,
                                        currency = activeTrip.baseCurrency,
                                        amountInBaseCurrency = settledAmount,
                                        exchangeRate = 1.0,
                                        category = CategoryCatalog.DEBT_SETTLEMENT_CATEGORY,
                                        date = FormatUtils.parseFlexibleDateToIso(settlementDate),
                                        paymentMethod = method,
                                        notes = notes,
                                        latitude = null,
                                        longitude = null,
                                        placeName = place,
                                        country = "",
                                        originName = "",
                                        originLatitude = null,
                                        originLongitude = null,
                                        paidById = payerId,
                                        splits = listOf(
                                            ExpenseSplit(
                                                id = "split-${java.util.UUID.randomUUID().toString().take(8)}",
                                                expenseId = expenseId,
                                                participantId = tx.toParticipantId,
                                                amount = settledAmount,
                                                percentage = 100.0
                                            )
                                        ),
                                        createdAt = System.currentTimeMillis()
                                    )
                                    viewModel.saveExpense(expense, isEdit = false)
                                    val debtSettledToast = when (language) {
                                        AppLanguage.ES -> "Deuda saldada: ${FormatUtils.formatCurrency(settledAmount, activeTrip.baseCurrency)}"
                                        AppLanguage.EN -> "Debt settled: ${FormatUtils.formatCurrency(settledAmount, activeTrip.baseCurrency)}"
                                        AppLanguage.FR -> "Dette réglée : ${FormatUtils.formatCurrency(settledAmount, activeTrip.baseCurrency)}"
                                        AppLanguage.IT -> "Debito saldato: ${FormatUtils.formatCurrency(settledAmount, activeTrip.baseCurrency)}"
                                        AppLanguage.ZH -> "债务已结清: ${FormatUtils.formatCurrency(settledAmount, activeTrip.baseCurrency)}"
                                        AppLanguage.NL -> "Schuld voldaan: ${FormatUtils.formatCurrency(settledAmount, activeTrip.baseCurrency)}"
                                        AppLanguage.DE -> "Schuld beglichen: ${FormatUtils.formatCurrency(settledAmount, activeTrip.baseCurrency)}"
                                        AppLanguage.PL -> "Dług uregulowany: ${FormatUtils.formatCurrency(settledAmount, activeTrip.baseCurrency)}"
                                        AppLanguage.PT -> "Dívida liquidada: ${FormatUtils.formatCurrency(settledAmount, activeTrip.baseCurrency)}"
                                    }
                                    viewModel.showToast(debtSettledToast)
                                },
                                topHeader = activeTripHeaderContent
                            )
                        }

                        MainTab.STATS -> {
                            StatsTabScreen(
                                trip = activeTrip,
                                participants = uiState.activeParticipants,
                                expenses = uiState.activeExpenses,
                                incomes = uiState.activeIncomes,
                                onParticipantAvatarClick = { participant ->
                                    participantForPhotoDialog = participant
                                },
                                onExpenseClick = { exp ->
                                    editingExpense = exp
                                    showAddOrEditExpenseModal = true
                                },
                                topHeader = activeTripHeaderContent
                            )
                        }

                        MainTab.MAP -> {
                            MapTabScreen(
                                trip = activeTrip,
                                participants = uiState.activeParticipants,
                                expenses = uiState.activeExpenses,
                                userLocation = userLocation,
                                onRequestLocationRefresh = viewModel::refreshDeviceLocation,
                                onEditExpenseClick = { exp ->
                                    editingExpense = exp
                                    showAddOrEditExpenseModal = true
                                }
                            )
                        }

                        MainTab.TRIPS -> {
                            TripsTabScreen(
                                trips = uiState.trips,
                                activeTripId = uiState.activeTripId,
                                allParticipants = uiState.allParticipants,
                                allExpenses = uiState.allExpenses,
                                allIncomes = uiState.allIncomes,
                                onSelectTrip = viewModel::selectActiveTrip,
                                onTripIconClick = { trip ->
                                    tripForIconPhotoDialog = trip
                                },
                                onCreateNewTripClick = {
                                    editingTrip = null
                                    showCreateTripModal = true
                                },
                                onAddSampleTripClick = viewModel::createSampleTrip,
                                onImportZipClick = {
                                    importZipLauncher.launch(arrayOf("*/*"))
                                },
                                onEditTripClick = { trip ->
                                    editingTrip = trip
                                    showCreateTripModal = true
                                },
                                onExportSingleTripJsonClick = { trip ->
                                    exportAndShareTripJson(trip)
                                },
                                onExportSingleTripPdfClick = { trip ->
                                    exportAndShareTripPdf(trip)
                                },
                                onDeleteTripConfirmed = viewModel::deleteTrip,
                                topHeader = activeTripHeaderContent
                            )
                        }

                        MainTab.SETTINGS -> {}
                    }
                } else {
                    TripsTabScreen(
                        trips = uiState.trips,
                        activeTripId = null,
                        allParticipants = uiState.allParticipants,
                        allExpenses = uiState.allExpenses,
                        allIncomes = uiState.allIncomes,
                        onSelectTrip = viewModel::selectActiveTrip,
                        onTripIconClick = { trip ->
                            tripForIconPhotoDialog = trip
                        },
                        onCreateNewTripClick = {
                            editingTrip = null
                            showCreateTripModal = true
                        },
                        onAddSampleTripClick = viewModel::createSampleTrip,
                        onImportZipClick = {
                            importZipLauncher.launch(arrayOf("*/*"))
                        },
                        onEditTripClick = { trip ->
                            editingTrip = trip
                            showCreateTripModal = true
                        },
                        onExportSingleTripJsonClick = { trip ->
                            exportAndShareTripJson(trip)
                        },
                        onExportSingleTripPdfClick = { trip ->
                            exportAndShareTripPdf(trip)
                        },
                        onDeleteTripConfirmed = viewModel::deleteTrip
                    )
                }
            }

            // Notificación superior flotante (Feedback visual)
            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                TopFeedbackToast(
                    message = toastMessage?.text,
                    isError = toastMessage?.isError == true,
                    onDismiss = viewModel::dismissToast
                )
            }
        }
    }

    // Diálogo para elegir foto personalizada al pinchar el icono de una ruta (o dejar el de carretera por defecto)
    if (tripForIconPhotoDialog != null) {
        val targetTrip = tripForIconPhotoDialog!!
        val routeIconTitle = when (language) {
            AppLanguage.ES -> "Icono de la ruta \"${targetTrip.title}\""
            AppLanguage.EN -> "Route icon for \"${targetTrip.title}\""
            AppLanguage.FR -> "Icône de l'itinéraire pour « ${targetTrip.title} »"
            AppLanguage.IT -> "Icona dell'itinerario per \"${targetTrip.title}\""
            AppLanguage.ZH -> "路线图标: \"${targetTrip.title}\""
            AppLanguage.NL -> "Routepictogram voor \"${targetTrip.title}\""
            AppLanguage.DE -> "Routen-Icon für „${targetTrip.title}“"
            AppLanguage.PL -> "Ikona trasy dla „${targetTrip.title}”"
            AppLanguage.PT -> "Ícone da rota \"${targetTrip.title}\""
        }
        val routeIconSubtitle = when (language) {
            AppLanguage.ES -> "Puedes poner una foto personalizada para esta ruta o dejar el icono de la carretera por defecto:"
            AppLanguage.EN -> "You can set a custom photo for this route or keep the default road icon:"
            AppLanguage.FR -> "Vous pouvez définir une photo personnalisée ou conserver l'icône par défaut :"
            AppLanguage.IT -> "Puoi impostare una foto personalizzata o mantenere l'icona predefinita:"
            AppLanguage.ZH -> "您可以为此路线设置自定义照片或保留默认公路图标:"
            AppLanguage.NL -> "Je kunt een eigen foto instellen of het standaard wegpictogram behouden:"
            AppLanguage.DE -> "Sie können ein eigenes Foto festlegen oder das Standard-Straßen-Icon behalten:"
            AppLanguage.PL -> "Możesz ustawić własne zdjęcie dla tej trasy lub zachować domyślną ikonę drogi:"
            AppLanguage.PT -> "Pode definir uma foto personalizada para esta rota ou manter o ícone predefinido:"
        }
        val defaultRoadIcon = when (language) {
            AppLanguage.ES -> "Dejar icono de carretera por defecto"
            AppLanguage.EN -> "Keep default road icon"
            AppLanguage.FR -> "Garder l'icône de route par défaut"
            AppLanguage.IT -> "Mantieni icona strada predefinita"
            AppLanguage.ZH -> "保留默认公路图标"
            AppLanguage.NL -> "Standaard wegpictogram behouden"
            AppLanguage.DE -> "Standard-Straßen-Icon behalten"
            AppLanguage.PL -> "Zachowaj domyślną ikonę drogi"
            AppLanguage.PT -> "Manter ícone de estrada predefinido"
        }
        AlertDialog(
            onDismissRequest = { tripForIconPhotoDialog = null },
            title = {
                Text(
                    text = routeIconTitle,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EditableTripRouteIcon(
                        photoUri = targetTrip.headerPhotoUri,
                        onClick = {},
                        size = 72.dp
                    )
                    Text(
                        text = routeIconSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                tripPhotoGalleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TravelBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(texts.galleryOption)
                        }
                        OutlinedButton(
                            onClick = {
                                tripPhotoCameraLauncher.launch(null)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(texts.cameraOption)
                        }
                    }
                    if (!targetTrip.headerPhotoUri.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateTrip(targetTrip.copy(headerPhotoUri = null))
                                tripForIconPhotoDialog = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(defaultRoadIcon)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { tripForIconPhotoDialog = null }) {
                    Text(texts.closeBtn, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Diálogo para añadir foto al icono de un participante (o dejar inicial + color aleatorio por defecto)
    if (participantForPhotoDialog != null) {
        val targetPart = participantForPhotoDialog!!
        val partIconTitle = when (language) {
            AppLanguage.ES -> "Icono de ${targetPart.name}"
            AppLanguage.EN -> "Icon for ${targetPart.name}"
            AppLanguage.FR -> "Icône de ${targetPart.name}"
            AppLanguage.IT -> "Icona di ${targetPart.name}"
            AppLanguage.ZH -> "${targetPart.name} 的图标"
            AppLanguage.NL -> "Pictogram voor ${targetPart.name}"
            AppLanguage.DE -> "Icon für ${targetPart.name}"
            AppLanguage.PL -> "Ikona użytkownika ${targetPart.name}"
            AppLanguage.PT -> "Ícone de ${targetPart.name}"
        }
        val partIconSubtitle = when (language) {
            AppLanguage.ES -> "Elige una foto para ${targetPart.name}. Si no pones ninguna, se mostrará por defecto su inicial con su color:"
            AppLanguage.EN -> "Choose a photo for ${targetPart.name}. If none is set, their initial and color are shown by default:"
            AppLanguage.FR -> "Choisissez une photo pour ${targetPart.name}. Par défaut, son initiale et sa couleur sont affichées :"
            AppLanguage.IT -> "Scegli una foto per ${targetPart.name}. Per impostazione predefinita vengono mostrati iniziale e colore:"
            AppLanguage.ZH -> "为 ${targetPart.name} 选择一张照片。若未设置，则默认显示首字母与颜色:"
            AppLanguage.NL -> "Kies een foto voor ${targetPart.name}. Zonder foto worden initialen en kleur standaard getoond:"
            AppLanguage.DE -> "Wählen Sie ein Foto für ${targetPart.name}. Ohne Foto werden standardmäßig Initiale und Farbe angezeigt:"
            AppLanguage.PL -> "Wybierz zdjęcie dla ${targetPart.name}. Jeśli brak, domyślnie wyświetlany jest inicjał i kolor:"
            AppLanguage.PT -> "Escolha uma foto para ${targetPart.name}. Se não definir nenhuma, são mostradas a inicial e a cor predefinidas:"
        }
        val defaultPartIcon = when (language) {
            AppLanguage.ES -> "Dejar por defecto (Inicial + Color)"
            AppLanguage.EN -> "Keep default (Initial + Color)"
            AppLanguage.FR -> "Garder par défaut (Initiale + Couleur)"
            AppLanguage.IT -> "Mantieni predefinito (Iniziale + Colore)"
            AppLanguage.ZH -> "保留默认 (首字母 + 颜色)"
            AppLanguage.NL -> "Standaard behouden (Initiaal + Kleur)"
            AppLanguage.DE -> "Standard beibehalten (Initiale + Farbe)"
            AppLanguage.PL -> "Zachowaj domyślne (Inicjał + Kolor)"
            AppLanguage.PT -> "Manter predefinido (Inicial + Cor)"
        }
        AlertDialog(
            onDismissRequest = { participantForPhotoDialog = null },
            title = {
                Text(
                    text = partIconTitle,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EditableParticipantAvatar(
                        name = targetPart.name,
                        avatarColorHex = targetPart.avatarColor,
                        photoUri = targetPart.photoUri,
                        onClick = {},
                        size = 72.dp
                    )
                    Text(
                        text = partIconSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                participantPhotoGalleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TravelBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(texts.galleryOption)
                        }
                        OutlinedButton(
                            onClick = {
                                participantPhotoCameraLauncher.launch(null)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(texts.cameraOption)
                        }
                    }
                    if (!targetPart.photoUri.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateParticipantPhoto(targetPart, null)
                                participantForPhotoDialog = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(defaultPartIcon)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { participantForPhotoDialog = null }) {
                    Text(texts.closeBtn, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // =========================================================================
    // MODALES EMERGENTES
    // =========================================================================
    val activeTrip = uiState.activeTrip
    val systemNavBarsBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    if (showAddOrEditExpenseModal && activeTrip != null) {
        AddExpenseModal(
            trip = activeTrip,
            participants = uiState.activeParticipants,
            existingExpense = editingExpense,
            userLocation = userLocation,
            placeSuggestions = placeSuggestions,
            isSearchingPlaces = isSearchingPlaces,
            onRequestLocationRefresh = viewModel::refreshDeviceLocation,
            onPlaceQueryChanged = viewModel::onPlaceQueryChanged,
            onClearPlaceSuggestions = viewModel::clearPlaceSuggestions,
            onSaveExpense = viewModel::saveExpense,
            onDeleteExpense = viewModel::deleteExpense,
            systemNavBarBottomPadding = systemNavBarsBottomPadding,
            onDismiss = {
                showAddOrEditExpenseModal = false
                editingExpense = null
            }
        )
    }

    if (showIncomeModal && activeTrip != null) {
        IncomeModal(
            trip = activeTrip,
            participants = uiState.activeParticipants,
            expenses = uiState.activeExpenses,
            incomes = uiState.activeIncomes,
            existingIncome = editingIncome,
            systemNavBarBottomPadding = systemNavBarsBottomPadding,
            onSaveIncome = viewModel::saveIncome,
            onDismiss = {
                showIncomeModal = false
                editingIncome = null
            }
        )
    }

    if (showCreateTripModal) {
        val targetTripToEdit = editingTrip
        val targetParticipants = if (targetTripToEdit != null) {
            uiState.allParticipants.filter { it.tripId == targetTripToEdit.id }
        } else {
            emptyList()
        }
        CreateTripModal(
            existingTrip = targetTripToEdit,
            existingParticipants = targetParticipants,
            isFirstTrip = uiState.trips.isEmpty() && targetTripToEdit == null,
            onCreateTrip = { title, startDate, endDate, budget, baseCurrency, participantNames ->
                if (targetTripToEdit != null) {
                    viewModel.updateTripDetails(
                        trip = targetTripToEdit,
                        title = title,
                        startDate = startDate,
                        endDate = endDate,
                        budget = budget,
                        baseCurrency = baseCurrency,
                        participantNames = participantNames
                    )
                } else {
                    viewModel.createTrip(
                        title = title,
                        startDate = startDate,
                        endDate = endDate,
                        budget = budget,
                        baseCurrency = baseCurrency,
                        participantNames = participantNames
                    )
                }
            },
            onDismiss = {
                showCreateTripModal = false
                editingTrip = null
            }
        )
    }

    if (pendingCsvManualImport != null) {
        val pending = pendingCsvManualImport!!
        CsvColumnMappingModal(
            sheet = pending.parsedSheet,
            createNewTrip = pending.createNewTrip,
            initialTripTitle = pending.suggestedTripTitle,
            onConfirmMapping = viewModel::confirmManualCsvMapping,
            onDismiss = viewModel::dismissManualCsvMapping
        )
    }

    val pendingDuplicateCandidate by viewModel.pendingDuplicate.collectAsStateWithLifecycle()
    if (pendingDuplicateCandidate != null) {
        val candidate = pendingDuplicateCandidate!!
        val dupMsg = when (language) {
            AppLanguage.ES -> "El viaje \"${candidate.existing.title}\" ya existe. ¿Quieres sobreescribirlo?"
            AppLanguage.EN -> "The trip \"${candidate.existing.title}\" already exists. Do you want to overwrite it?"
            AppLanguage.FR -> "Le voyage « ${candidate.existing.title} » existe déjà. Voulez-vous l'écraser ?"
            AppLanguage.IT -> "Il viaggio \"${candidate.existing.title}\" esiste già. Vuoi sovrascriverlo?"
            AppLanguage.ZH -> "行程 \"${candidate.existing.title}\" 已存在。是否要覆盖？"
            AppLanguage.NL -> "De reis \"${candidate.existing.title}\" bestaat al. Wil je deze overschrijven?"
            AppLanguage.DE -> "Die Reise „${candidate.existing.title}“ existiert bereits. Möchten Sie sie überschreiben?"
            AppLanguage.PL -> "Podróż „${candidate.existing.title}” już istnieje. Czy chcesz ją nadpisać?"
            AppLanguage.PT -> "A viagem \"${candidate.existing.title}\" já existe. Deseja substituí-la?"
        }
        AlertDialog(
            onDismissRequest = {
                viewModel.resolveDuplicateTrip(candidate, overwrite = false)
            },
            title = {
                Text(
                    text = texts.duplicateTripTitle,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = dupMsg)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resolveDuplicateTrip(candidate, overwrite = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TravelBlue)
                ) {
                    Text(texts.overwriteBtn)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.resolveDuplicateTrip(candidate, overwrite = false)
                    }
                ) {
                    Text(texts.skipBtn)
                }
            }
        )
    }

    if (showSuggestionsModal) {
        SuggestionsModal(
            onDismiss = { showSuggestionsModal = false },
            onOpenHelpFaq = {
                showSuggestionsModal = false
                showHelpFaqModal = true
            }
        )
    }

    if (showHelpFaqModal) {
        HelpFaqModal(
            onDismiss = {
                showHelpFaqModal = false
                showSuggestionsModal = true
            }
        )
    }
}
