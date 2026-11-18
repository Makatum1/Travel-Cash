package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ui.theme.AppLanguage
import java.util.Calendar
import java.util.Locale

object DailyReminderScheduler {
    const val PREFS_NAME = "travel_cash_settings"
    const val KEY_ENABLED = "reminder_notifications_enabled"
    const val KEY_HOUR = "reminder_hour"
    const val KEY_MINUTE = "reminder_minute"
    const val KEY_LANGUAGE = "app_language"

    const val DEFAULT_HOUR = 21
    const val DEFAULT_MINUTE = 0

    const val CHANNEL_ID = "travel_cash_reminders"
    const val NOTIFICATION_ID = 1001
    const val ALARM_REQUEST_CODE = 2100
    const val ACTION_DAILY_REMINDER = "com.example.travelcash.ACTION_DAILY_REMINDER"

    fun formatTime(hour: Int, minute: Int): String {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    fun computeNextTriggerMillis(
        hour: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(context)
        val triggerAtMillis = computeNextTriggerMillis(hour, minute)

        try {
            alarmManager.cancel(pendingIntent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Si el dispositivo restringe alarmas, degradar con seguridad sin bloquear la app
        }
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.cancel(buildAlarmPendingIntent(context))
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(NOTIFICATION_ID)
    }

    fun showReminderNotification(
        context: Context,
        lang: AppLanguage,
        tripTitle: String? = null
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val channelName = when (lang) {
            AppLanguage.ES -> "Recordatorios de Gastos de Viaje"
            AppLanguage.EN -> "Trip Expense Reminders"
            AppLanguage.FR -> "Rappels de dépenses de voyage"
            AppLanguage.IT -> "Promemoria spese di viaggio"
            AppLanguage.ZH -> "旅行支出提醒"
            AppLanguage.NL -> "Reiskostenherinneringen"
            AppLanguage.DE -> "Reisekosten-Erinnerungen"
            AppLanguage.PL -> "Przypomnienia o wydatkach w podróży"
            AppLanguage.PT -> "Lembretes de Despesas de Viagem"
        }

        val channelDesc = when (lang) {
            AppLanguage.ES -> "Recordatorios para anotar los gastos diarios del viaje y el bote común"
            AppLanguage.EN -> "Reminders to log daily trip expenses and common pot contributions"
            AppLanguage.FR -> "Rappels pour enregistrer les dépenses quotidiennes du voyage et la cagnotte"
            AppLanguage.IT -> "Promemoria per registrare le spese giornaliere del viaggio e la cassa comune"
            AppLanguage.ZH -> "提醒记录每日旅行支出及公账存入"
            AppLanguage.NL -> "Herinneringen om dagelijkse reiskosten en inleg in de pot vast te leggen"
            AppLanguage.DE -> "Erinnerungen zur Erfassung täglicher Reiseausgaben und Kassenbeiträge"
            AppLanguage.PL -> "Przypomnienia o zapisywaniu codziennych wydatków z podróży i wspólnej kasy"
            AppLanguage.PT -> "Lembretes para registar as despesas diárias da viagem e o fundo comum"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = channelDesc
            }
            nm.createNotificationChannel(channel)
        }

        val notifTitle = when (lang) {
            AppLanguage.ES -> "Travel Cash · Recordatorio diario"
            AppLanguage.EN -> "Travel Cash · Daily Reminder"
            AppLanguage.FR -> "Travel Cash · Rappel quotidien"
            AppLanguage.IT -> "Travel Cash · Promemoria giornaliero"
            AppLanguage.ZH -> "Travel Cash · 每日提醒"
            AppLanguage.NL -> "Travel Cash · Dagelijkse herinnering"
            AppLanguage.DE -> "Travel Cash · Tägliche Erinnerung"
            AppLanguage.PL -> "Travel Cash · Codzienne przypomnienie"
            AppLanguage.PT -> "Travel Cash · Lembrete diário"
        }

        val activeName = tripTitle?.takeIf { it.isNotBlank() } ?: "Travel Cash"
        val notifText = when (lang) {
            AppLanguage.ES -> "No olvides registrar los gastos de hoy en \"$activeName\"."
            AppLanguage.EN -> "Don't forget to log today's expenses for \"$activeName\"."
            AppLanguage.FR -> "N'oubliez pas d'enregistrer les dépenses du jour pour « $activeName »."
            AppLanguage.IT -> "Non dimenticare di registrare le spese di oggi in \"$activeName\"."
            AppLanguage.ZH -> "别忘了记录今天在 \"$activeName\" 中的支出。"
            AppLanguage.NL -> "Vergeet niet de uitgaven van vandaag voor \"$activeName\" te registreren."
            AppLanguage.DE -> "Vergessen Sie nicht, die heutigen Ausgaben für „$activeName“ zu erfassen."
            AppLanguage.PL -> "Nie zapomnij zarejestrować dzisiejszych wydatków w „$activeName”."
            AppLanguage.PT -> "Não se esqueça de registar as despesas de hoje em \"$activeName\"."
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(notifTitle)
                .setContentText(notifText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .build()
            nm.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permiso POST_NOTIFICATIONS no concedido todavía
        }
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences(DailyReminderScheduler.PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(DailyReminderScheduler.KEY_ENABLED, true)
        if (!enabled) return

        val hour = prefs.getInt(DailyReminderScheduler.KEY_HOUR, DailyReminderScheduler.DEFAULT_HOUR)
        val minute = prefs.getInt(DailyReminderScheduler.KEY_MINUTE, DailyReminderScheduler.DEFAULT_MINUTE)
        val lang = runCatching {
            val saved = prefs.getString(DailyReminderScheduler.KEY_LANGUAGE, null)
            if (saved != null && saved != "SYSTEM") AppLanguage.valueOf(saved)
            else AppLanguage.detectSystemLanguage()
        }.getOrElse { AppLanguage.detectSystemLanguage() }

        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                DailyReminderScheduler.scheduleDailyReminder(context, hour, minute)
            }
            else -> {
                DailyReminderScheduler.showReminderNotification(context, lang)
                DailyReminderScheduler.scheduleDailyReminder(context, hour, minute)
            }
        }
    }
}
