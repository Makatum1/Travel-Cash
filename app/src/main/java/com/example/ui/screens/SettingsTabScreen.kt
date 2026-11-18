package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.TripEntity
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.TravelBlue
import com.example.util.DailyReminderScheduler
import com.example.util.UiTexts

@Composable
fun SettingsTabScreen(
    themeMode: AppThemeMode,
    language: AppLanguage,
    reminderNotificationsEnabled: Boolean,
    reminderHour: Int = DailyReminderScheduler.DEFAULT_HOUR,
    reminderMinute: Int = DailyReminderScheduler.DEFAULT_MINUTE,
    trips: List<TripEntity>,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onReminderNotificationsChange: (Boolean) -> Unit,
    onReminderTimeChange: (Int, Int) -> Unit = { _, _ -> },
    onExportZipClick: (TripEntity?) -> Unit,
    onImportZipClick: () -> Unit
) {
    val context = LocalContext.current
    val texts = UiTexts.get(language)

    var appearanceExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

    // Permiso POST_NOTIFICATIONS en Android 13+ al activar las notificaciones de recordatorio
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onReminderNotificationsChange(true)
    }

    fun toggleReminderNotifications(newValue: Boolean) {
        if (newValue && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        onReminderNotificationsChange(newValue)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // =========================================================================
        // 1. SECCIÓN RECONFIGURADA: EXPORTAR E IMPORTAR (.ZIP CON ARCHIVOS .JSON)
        // =========================================================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val exportImportSectionTitle = when (language) {
                        AppLanguage.ES -> "Exportar e Importar"
                        AppLanguage.EN -> "Export and Import"
                        AppLanguage.FR -> "Exporter et importer"
                        AppLanguage.IT -> "Esporta e importa"
                        AppLanguage.ZH -> "导出与导入"
                        AppLanguage.NL -> "Exporteren en importeren"
                        AppLanguage.DE -> "Exportieren und Importieren"
                        AppLanguage.PL -> "Eksport i import"
                        AppLanguage.PT -> "Exportar e Importar"
                    }
                    Text(
                        text = exportImportSectionTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    var exportMenuExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Botón 1: Exportar con Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { exportMenuExpanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = TravelBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_export_button")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = texts.exportBtn,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }

                            DropdownMenu(
                                expanded = exportMenuExpanded,
                                onDismissRequest = { exportMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = texts.allTripsOption,
                                            fontWeight = FontWeight.Bold,
                                            color = TravelBlue
                                        )
                                    },
                                    onClick = {
                                        exportMenuExpanded = false
                                        onExportZipClick(null)
                                    }
                                )
                                if (trips.isNotEmpty()) {
                                    HorizontalDivider()
                                    trips.forEach { trip ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "✈️ ${trip.title}",
                                                    fontWeight = FontWeight.Medium
                                                )
                                            },
                                            onClick = {
                                                exportMenuExpanded = false
                                                onExportZipClick(trip)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Botón 2: Importar (directo sin dropdown)
                        Button(
                            onClick = onImportZipClick,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_import_button")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = texts.importBtn,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. NOTIFICACIONES DE RECORDATORIO DIARIO
        // =========================================================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (reminderNotificationsEnabled) EmeraldGreenLight else Color(0xFFF1F5F9),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (reminderNotificationsEnabled) {
                                            Icons.Default.NotificationsActive
                                        } else {
                                            Icons.Default.NotificationsOff
                                        },
                                        contentDescription = null,
                                        tint = if (reminderNotificationsEnabled) EmeraldGreenDark else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = texts.dailyReminderTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val formattedTime = DailyReminderScheduler.formatTime(reminderHour, reminderMinute)
                                val reminderSubtitle = if (reminderNotificationsEnabled) {
                                    when (language) {
                                        AppLanguage.ES -> "Aviso diario a las $formattedTime h"
                                        AppLanguage.EN -> "Daily notification at $formattedTime"
                                        AppLanguage.FR -> "Notification quotidienne à $formattedTime"
                                        AppLanguage.IT -> "Notifica giornaliera alle $formattedTime"
                                        AppLanguage.ZH -> "每天 $formattedTime 发送提醒通知"
                                        AppLanguage.NL -> "Dagelijkse melding om $formattedTime uur"
                                        AppLanguage.DE -> "Tägliche Benachrichtigung um $formattedTime Uhr"
                                        AppLanguage.PL -> "Codzienne powiadomienie o $formattedTime"
                                        AppLanguage.PT -> "Aviso diário às $formattedTime h"
                                    }
                                } else {
                                    when (language) {
                                        AppLanguage.ES -> "Desactivado"
                                        AppLanguage.EN -> "Disabled"
                                        AppLanguage.FR -> "Désactivé"
                                        AppLanguage.IT -> "Disattivato"
                                        AppLanguage.ZH -> "已停用"
                                        AppLanguage.NL -> "Uitgeschakeld"
                                        AppLanguage.DE -> "Deaktiviert"
                                        AppLanguage.PL -> "Wyłączone"
                                        AppLanguage.PT -> "Desativado"
                                    }
                                }
                                Text(
                                    text = reminderSubtitle,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (reminderNotificationsEnabled) EmeraldGreenDark else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = reminderNotificationsEnabled,
                            onCheckedChange = { checked ->
                                toggleReminderNotifications(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldGreenDark
                            ),
                            modifier = Modifier.testTag("settings_reminder_notifications_switch")
                        )
                    }

                    // Selector de hora del recordatorio (por defecto 21:00, modificable)
                    val formattedReminderTime = DailyReminderScheduler.formatTime(reminderHour, reminderMinute)
                    val reminderTimeLabel = when (language) {
                        AppLanguage.ES -> "Hora del recordatorio"
                        AppLanguage.EN -> "Reminder time"
                        AppLanguage.FR -> "Heure du rappel"
                        AppLanguage.IT -> "Orario del promemoria"
                        AppLanguage.ZH -> "提醒时间"
                        AppLanguage.NL -> "Herinneringstijd"
                        AppLanguage.DE -> "Erinnerungszeit"
                        AppLanguage.PL -> "Godzina przypomnienia"
                        AppLanguage.PT -> "Hora do lembrete"
                    }
                    val changeTimeLabel = when (language) {
                        AppLanguage.ES -> "Cambiar"
                        AppLanguage.EN -> "Change"
                        AppLanguage.FR -> "Modifier"
                        AppLanguage.IT -> "Modifica"
                        AppLanguage.ZH -> "修改"
                        AppLanguage.NL -> "Wijzigen"
                        AppLanguage.DE -> "Ändern"
                        AppLanguage.PL -> "Zmień"
                        AppLanguage.PT -> "Alterar"
                    }
                    Surface(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, selectedHour, selectedMinute ->
                                    onReminderTimeChange(selectedHour, selectedMinute)
                                },
                                reminderHour,
                                reminderMinute,
                                true
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            if (reminderNotificationsEnabled) EmeraldGreenDark.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_reminder_time_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (reminderNotificationsEnabled) EmeraldGreenDark else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                                Text(
                                    text = reminderTimeLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (reminderNotificationsEnabled) EmeraldGreenLight else Color(0xFFE2E8F0)
                                ) {
                                    Text(
                                        text = formattedReminderTime,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (reminderNotificationsEnabled) EmeraldGreenDark else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = changeTimeLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TravelBlue
                                )
                            }
                        }
                    }

                    val dynamicReminderDesc = when (language) {
                        AppLanguage.ES -> "Envía una notificación diaria a las $formattedReminderTime para registrar los gastos del día y mantener las cuentas al día."
                        AppLanguage.EN -> "Sends a daily notification at $formattedReminderTime to record the day's expenses and keep accounts squared."
                        AppLanguage.FR -> "Envoie une notification quotidienne à $formattedReminderTime pour enregistrer les dépenses de la journée et garder les comptes à jour."
                        AppLanguage.IT -> "Invia una notifica quotidiana alle $formattedReminderTime per registrare le spese del giorno e tenere i conti in ordine."
                        AppLanguage.ZH -> "每天 $formattedReminderTime 发送通知，提醒您记录当天的支出，保持账目清晰。"
                        AppLanguage.NL -> "Stuurt dagelijks om $formattedReminderTime uur een melding om de uitgaven van de dag bij te houden en rekeningen kloppend te houden."
                        AppLanguage.DE -> "Sendet täglich um $formattedReminderTime Uhr eine Benachrichtigung, um die Ausgaben des Tages einzutragen und die Abrechnung aktuell zu halten."
                        AppLanguage.PL -> "Wysyła codzienne powiadomienie o godzinie $formattedReminderTime, aby zapisać wydatki z danego dnia i utrzymać finanse pod kontrolą."
                        AppLanguage.PT -> "Envia uma notificação diária às $formattedReminderTime para registar as despesas do dia e manter as contas em dia."
                    }
                    Text(
                        text = dynamicReminderDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // =========================================================================
        // 3. MODO DE TEMA: CLARO (PREDETERMINADO), OSCURO O SISTEMA - CERRADO POR DEFECTO
        // =========================================================================
        item {
            fun getThemeLabel(mode: AppThemeMode): String = when (mode) {
                AppThemeMode.LIGHT -> when (language) {
                    AppLanguage.ES -> "Modo Claro (Predeterminado)"
                    AppLanguage.EN -> "Light Mode (Default)"
                    AppLanguage.FR -> "Mode clair (par défaut)"
                    AppLanguage.IT -> "Modalità chiara (predefinita)"
                    AppLanguage.ZH -> "浅色模式（默认）"
                    AppLanguage.NL -> "Lichte modus (standaard)"
                    AppLanguage.DE -> "Heller Modus (Standard)"
                    AppLanguage.PL -> "Tryb jasny (domyślny)"
                    AppLanguage.PT -> "Modo Claro (Predefinido)"
                }
                AppThemeMode.DARK -> when (language) {
                    AppLanguage.ES -> "Modo Oscuro"
                    AppLanguage.EN -> "Dark Mode"
                    AppLanguage.FR -> "Mode sombre"
                    AppLanguage.IT -> "Modalità scura"
                    AppLanguage.ZH -> "深色模式"
                    AppLanguage.NL -> "Donkere modus"
                    AppLanguage.DE -> "Dunkler Modus"
                    AppLanguage.PL -> "Tryb ciemny"
                    AppLanguage.PT -> "Modo Escuro"
                }
                AppThemeMode.SYSTEM -> when (language) {
                    AppLanguage.ES -> "Modo Sistema"
                    AppLanguage.EN -> "System Default"
                    AppLanguage.FR -> "Système par défaut"
                    AppLanguage.IT -> "Predefinito di sistema"
                    AppLanguage.ZH -> "跟随系统"
                    AppLanguage.NL -> "Systeemstandaard"
                    AppLanguage.DE -> "Systemstandard"
                    AppLanguage.PL -> "Domyślny systemowy"
                    AppLanguage.PT -> "Padrão do Sistema"
                }
            }

            val currentThemeLabel = getThemeLabel(themeMode)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { appearanceExpanded = !appearanceExpanded }
                            .testTag("settings_appearance_header"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brightness4,
                                contentDescription = null,
                                tint = TravelBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                val appearanceHeader = when (language) {
                                    AppLanguage.ES -> "Apariencia (Modo de Pantalla)"
                                    AppLanguage.EN -> "Appearance (Theme Mode)"
                                    AppLanguage.FR -> "Apparence (mode de thème)"
                                    AppLanguage.IT -> "Aspetto (modalità tema)"
                                    AppLanguage.ZH -> "外观（主题模式）"
                                    AppLanguage.NL -> "Uiterlijk (themamodus)"
                                    AppLanguage.DE -> "Erscheinungsbild (Designmodus)"
                                    AppLanguage.PL -> "Wygląd (tryb motywu)"
                                    AppLanguage.PT -> "Aparência (Modo de Ecrã)"
                                }
                                Text(
                                    text = appearanceHeader,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentThemeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = if (appearanceExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = appearanceExpanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            val themeOptions = listOf(
                                Triple(
                                    AppThemeMode.LIGHT,
                                    getThemeLabel(AppThemeMode.LIGHT),
                                    Icons.Default.LightMode
                                ),
                                Triple(
                                    AppThemeMode.DARK,
                                    getThemeLabel(AppThemeMode.DARK),
                                    Icons.Default.DarkMode
                                ),
                                Triple(
                                    AppThemeMode.SYSTEM,
                                    getThemeLabel(AppThemeMode.SYSTEM),
                                    Icons.Default.SettingsSuggest
                                )
                            )

                            themeOptions.forEach { (mode, label, icon) ->
                                val selected = themeMode == mode
                                Surface(
                                    onClick = { onThemeModeChange(mode) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) TravelBlue.copy(alpha = 0.12f) else Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = { onThemeModeChange(mode) }
                                        )
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (selected) TravelBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 4. IDIOMA: DISPONIBLES EN LA APP - CERRADO POR DEFECTO
        // =========================================================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { languageExpanded = !languageExpanded }
                            .testTag("settings_language_header"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = EmeraldGreenDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                val languageSectionTitle = when (language) {
                                    AppLanguage.ES -> "Idioma de la Aplicación"
                                    AppLanguage.EN -> "App Language"
                                    AppLanguage.FR -> "Langue de l'application"
                                    AppLanguage.IT -> "Lingua dell'applicazione"
                                    AppLanguage.ZH -> "应用语言"
                                    AppLanguage.NL -> "Taal van de app"
                                    AppLanguage.DE -> "App-Sprache"
                                    AppLanguage.PL -> "Język aplikacji"
                                    AppLanguage.PT -> "Idioma da Aplicação"
                                }
                                Text(
                                    text = languageSectionTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${language.flag} ${language.nativeName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = if (languageExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = languageExpanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            val collator = remember(language) {
                                java.text.Collator.getInstance(language.locale).apply {
                                    strength = java.text.Collator.SECONDARY
                                }
                            }
                            val sortedLanguages = remember(language) {
                                AppLanguage.entries.sortedWith { a, b ->
                                    collator.compare(a.getDisplayName(language), b.getDisplayName(language))
                                }
                            }
                            sortedLanguages.forEach { lang ->
                                val selected = language == lang
                                val langDescription = lang.getDisplayName(language)
                                val label = if (lang.nativeName.equals(langDescription, ignoreCase = true)) {
                                    langDescription
                                } else {
                                    "$langDescription (${lang.nativeName})"
                                }
                                val flag = "${lang.flag} "
                                Surface(
                                    onClick = { onLanguageChange(lang) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) EmeraldGreenDark.copy(alpha = 0.12f) else Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = { onLanguageChange(lang) }
                                        )
                                        Text(
                                            text = "$flag$label",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
