package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import java.io.File

object SampleReceiptGenerator {

    /**
     * Genera una imagen realista de un ticket/recibo de restaurante para el viaje de prueba
     * y la guarda en el almacenamiento interno de la app, devolviendo su URI de archivo.
     */
    fun getOrCreateSampleReceipt(context: Context): String {
        val file = File(context.filesDir, "sample_receipt_trattoria.png")
        if (!file.exists() || file.length() == 0L) {
            val width = 420
            val height = 620
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
            }
            canvas.drawRoundRect(RectF(12f, 12f, width - 12f, height - 12f), 10f, 10f, borderPaint)

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 23f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 13.5f
                textAlign = Paint.Align.CENTER
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 14.5f
            }
            val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 15.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 14.5f
                textAlign = Paint.Align.RIGHT
            }
            val rightBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1.5f
            }

            var curY = 50f
            canvas.drawText("TRATTORIA ROMANA", width / 2f, curY, headerPaint)
            curY += 24f
            canvas.drawText("Via dei Fori Imperiali, 42 — Roma (Italia)", width / 2f, curY, subPaint)
            curY += 18f
            canvas.drawText("P.IVA: IT08492010582 • Tel: +39 06 6987 1234", width / 2f, curY, subPaint)
            curY += 24f
            canvas.drawLine(28f, curY, width - 28f, curY, linePaint)
            curY += 25f

            val items = listOf(
                "1x Pizza Margherita D.O.P." to "9,00 €",
                "1x Spaghetti Carbonara Tradizionale" to "14,50 €",
                "1x Bistecca ai Ferri con Patate" to "22,00 €",
                "2x Acqua Minerale Naturale" to "5,00 €",
                "1x Tiramisù Fatto in Casa" to "6,00 €",
                "2x Caffè Espresso" to "4,00 €",
                "2x Servizio & Coperto" to "4,00 €"
            )
            for ((item, price) in items) {
                canvas.drawText(item, 28f, curY, textPaint)
                canvas.drawText(price, width - 28f, curY, rightPaint)
                curY += 25f
            }

            curY += 8f
            canvas.drawLine(28f, curY, width - 28f, curY, linePaint)
            curY += 30f

            canvas.drawText("TOTALE CONTO", 28f, curY, boldPaint)
            canvas.drawText("64,50 €", width - 28f, curY, rightBoldPaint)
            curY += 24f

            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 13f
            }
            canvas.drawText("Pagamento: Carta di Credito (Contactless)", 28f, curY, metaPaint)
            curY += 34f

            // Código de barras decorativo
            val barPaint = Paint().apply { color = Color.parseColor("#475569") }
            for (i in 0..38) {
                val barW = when {
                    i % 4 == 0 -> 4f
                    i % 2 == 0 -> 2f
                    else -> 6f
                }
                val barX = 55f + i * 8f
                canvas.drawRect(barX, curY, barX + barW, curY + 34f, barPaint)
            }
            curY += 50f
            canvas.drawText("¡GRAZIE PER LA VISITA / BUON VIAGGIO!", width / 2f, curY, subPaint)

            file.outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)
            }
            bitmap.recycle()
        }
        return Uri.fromFile(file).toString()
    }
}
