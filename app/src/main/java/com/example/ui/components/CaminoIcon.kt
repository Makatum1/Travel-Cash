package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R

/**
 * Icono Oficial de la Aplicación en la barra superior junto al título "Travel Cash".
 * Muestra el icono proporcionado por el usuario (ic_launcher_custom).
 */
@Composable
fun AppHeaderLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val cornerRadius = size * 0.24f
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_custom),
        contentDescription = "Logo Travel Cash",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
    )
}

/**
 * Icono de cada ruta/viaje:
 * Muestra el icono de la carretera por defecto (CaminoIcon).
 * Si el usuario lo pincha, permite elegir una foto personalizada para esa ruta;
 * si no se pone ninguna foto, se mantiene el icono de la carretera por defecto.
 */
@Composable
fun EditableTripRouteIcon(
    photoUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val cornerRadius = size * 0.24f
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable { onClick() }
    ) {
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Foto de la ruta (pulsa para cambiar)",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
            )
        } else {
            CaminoIcon(size = size)
        }

        // Pequeño indicador de cámara en la esquina inferior derecha indicando que es pulsable
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(size * 0.34f)
                .clip(CircleShape)
                .background(Color(0xFF0F172A).copy(alpha = 0.82f))
                .border(0.8.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Cambiar foto de ruta",
                tint = Color.White,
                modifier = Modifier.size(size * 0.20f)
            )
        }
    }
}

/**
 * Icono de Participante del Viaje:
 * Por defecto muestra un círculo con la inicial de su nombre y su color asignado.
 * Al pinchar sobre él, permite añadir una foto personalizada; si no se pone foto,
 * permanece por defecto con la inicial y su color.
 */
@Composable
fun EditableParticipantAvatar(
    name: String,
    avatarColorHex: String,
    photoUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val avatarColor = com.example.data.model.AvatarColorPalette.parseHexColor(avatarColorHex)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Foto de $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = name.take(1).uppercase(java.util.Locale.ROOT),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                    ),
                    color = Color.White
                )
            }
        }

        // Indicador sutil de cámara
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(size * 0.34f)
                .clip(CircleShape)
                .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                .border(0.7.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Añadir o cambiar foto de $name",
                tint = Color.White,
                modifier = Modifier.size(size * 0.19f)
            )
        }
    }
}

/**
 * Icono por defecto de cada ruta (Carretera escénica con montañas, sol y pin).
 */
@Composable
fun CaminoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val cornerRadius = size * 0.24f
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF153B4E))
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Fondo degradado cielo nocturno / oceánico
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF153B4E), Color(0xFF1E5670), Color(0xFF0F2937))
                ),
                cornerRadius = CornerRadius(w * 0.24f, h * 0.24f),
                size = Size(w, h)
            )

            // Sol dorado / Moneda en la esquina superior derecha
            val sunCenter = Offset(w * 0.74f, h * 0.26f)
            val sunRadius = w * 0.16f
            drawCircle(
                color = Color(0xFFF59E0B).copy(alpha = 0.28f),
                radius = sunRadius * 1.28f,
                center = sunCenter
            )
            drawCircle(
                color = Color(0xFFFBBF24),
                radius = sunRadius,
                center = sunCenter
            )
            drawCircle(
                color = Color(0xFFD97706),
                radius = sunRadius * 0.78f,
                center = sunCenter,
                style = Stroke(width = w * 0.025f)
            )

            // Montañas escénicas al fondo
            val leftMountain = Path().apply {
                moveTo(0f, h * 0.65f)
                lineTo(w * 0.34f, h * 0.32f)
                lineTo(w * 0.65f, h * 0.65f)
                close()
            }
            drawPath(
                path = leftMountain,
                color = Color(0xFF286684)
            )

            // Cumbre nevada izquierda
            val snowCap = Path().apply {
                moveTo(w * 0.34f, h * 0.32f)
                lineTo(w * 0.25f, h * 0.42f)
                lineTo(w * 0.31f, h * 0.44f)
                lineTo(w * 0.34f, h * 0.40f)
                lineTo(w * 0.38f, h * 0.44f)
                lineTo(w * 0.43f, h * 0.42f)
                close()
            }
            drawPath(path = snowCap, color = Color(0xFFF8FAFC))

            val rightMountain = Path().apply {
                moveTo(w * 0.35f, h * 0.65f)
                lineTo(w * 0.72f, h * 0.40f)
                lineTo(w, h * 0.65f)
                close()
            }
            drawPath(
                path = rightMountain,
                color = Color(0xFF1F4E66)
            )

            // Suelo / Valle esmeralda
            drawRect(
                color = Color(0xFF0D9488),
                topLeft = Offset(0f, h * 0.63f),
                size = Size(w, h * 0.37f)
            )

            // Camino en perspectiva hacia el horizonte
            val roadPath = Path().apply {
                moveTo(w * 0.16f, h)
                lineTo(w * 0.45f, h * 0.61f)
                lineTo(w * 0.55f, h * 0.61f)
                lineTo(w * 0.84f, h)
                close()
            }
            drawPath(
                path = roadPath,
                color = Color(0xFF334155)
            )

            // Bordes blancos de la ruta
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(w * 0.18f, h),
                end = Offset(w * 0.45f, h * 0.61f),
                strokeWidth = w * 0.025f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(w * 0.82f, h),
                end = Offset(w * 0.55f, h * 0.61f),
                strokeWidth = w * 0.025f,
                cap = StrokeCap.Round
            )

            // Línea central discontinua dorada en el camino
            drawLine(
                color = Color(0xFFFBBF24),
                start = Offset(w * 0.50f, h * 0.96f),
                end = Offset(w * 0.50f, h * 0.63f),
                strokeWidth = w * 0.035f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.07f, w * 0.05f), 0f)
            )

            // Pin de ubicación en el horizonte del camino
            val pinTip = Offset(w * 0.50f, h * 0.60f)
            val pinCenter = Offset(w * 0.50f, h * 0.43f)
            val pinRadius = w * 0.11f
            val pinTriangle = Path().apply {
                moveTo(pinCenter.x - pinRadius * 0.85f, pinCenter.y + pinRadius * 0.35f)
                lineTo(pinTip.x, pinTip.y)
                lineTo(pinCenter.x + pinRadius * 0.85f, pinCenter.y + pinRadius * 0.35f)
                close()
            }
            drawPath(path = pinTriangle, color = Color(0xFFEF4444))
            drawCircle(
                color = Color(0xFFEF4444),
                radius = pinRadius,
                center = pinCenter
            )
            drawCircle(
                color = Color.White,
                radius = pinRadius * 0.42f,
                center = pinCenter
            )
        }
    }
}

/**
 * Icono personalizado de Localización (Pin de Viaje Travel Cash) para la pestaña Mapa y encabezados.
 */
@Composable
fun TravelLocationPinIcon(
    tint: Color = Color(0xFF0066FF),
    accentColor: Color = Color(0xFFF59E0B),
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Sombra elíptica en la base del pin
        drawOval(
            color = tint.copy(alpha = 0.25f),
            topLeft = Offset(w * 0.25f, h * 0.84f),
            size = Size(w * 0.50f, h * 0.12f)
        )

        val pinCenter = Offset(w * 0.50f, h * 0.36f)
        val pinRadius = w * 0.32f
        val pinTip = Offset(w * 0.50f, h * 0.90f)

        // Cuerpo de gota del pin de localización
        val pinBody = Path().apply {
            moveTo(pinCenter.x - pinRadius * 0.88f, pinCenter.y + pinRadius * 0.35f)
            lineTo(pinTip.x, pinTip.y)
            lineTo(pinCenter.x + pinRadius * 0.88f, pinCenter.y + pinRadius * 0.35f)
            close()
        }
        drawPath(path = pinBody, color = tint)
        drawCircle(
            color = tint,
            radius = pinRadius,
            center = pinCenter
        )

        // Círculo interior blanco + punto dorado/azul acorde a Travel Cash
        drawCircle(
            color = Color.White,
            radius = pinRadius * 0.52f,
            center = pinCenter
        )
        drawCircle(
            color = accentColor,
            radius = pinRadius * 0.26f,
            center = pinCenter
        )
    }
}

/**
 * Genera un Drawable de pin de localización personalizado acorde a la estética de Travel Cash
 * para los marcadores del mapa interactivo osmdroid.
 */
fun createTravelMapPinDrawable(
    context: android.content.Context,
    pinColor: Color,
    isSelected: Boolean = false
): android.graphics.drawable.BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val widthPx = ((if (isSelected) 44 else 36) * density).toInt().coerceAtLeast(48)
    val heightPx = ((if (isSelected) 54 else 44) * density).toInt().coerceAtLeast(60)

    val bitmap = android.graphics.Bitmap.createBitmap(widthPx, heightPx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()
    val cx = w * 0.5f
    val cy = h * 0.36f
    val r = w * 0.34f
    val tipY = h * 0.92f

    val shadowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(65, 15, 23, 42)
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawOval(
        android.graphics.RectF(w * 0.26f, h * 0.85f, w * 0.74f, h * 0.97f),
        shadowPaint
    )

    val argbPin = android.graphics.Color.argb(
        255,
        (pinColor.red * 255).toInt(),
        (pinColor.green * 255).toInt(),
        (pinColor.blue * 255).toInt()
    )

    val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) android.graphics.Color.rgb(251, 191, 36) else android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.FILL
    }
    val outerRadius = r + (2.4f * density)
    val outerPath = android.graphics.Path().apply {
        moveTo(cx - outerRadius * 0.86f, cy + outerRadius * 0.36f)
        lineTo(cx, tipY + 1.5f * density)
        lineTo(cx + outerRadius * 0.86f, cy + outerRadius * 0.36f)
        close()
    }
    canvas.drawPath(outerPath, borderPaint)
    canvas.drawCircle(cx, cy, outerRadius, borderPaint)

    val bodyPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = argbPin
        style = android.graphics.Paint.Style.FILL
    }
    val innerPath = android.graphics.Path().apply {
        moveTo(cx - r * 0.86f, cy + r * 0.36f)
        lineTo(cx, tipY - 1.5f * density)
        lineTo(cx + r * 0.86f, cy + r * 0.36f)
        close()
    }
    canvas.drawPath(innerPath, bodyPaint)
    canvas.drawCircle(cx, cy, r, bodyPaint)

    val centerWhitePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, r * 0.50f, centerWhitePaint)

    val centerGoldPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(245, 158, 11) // Sol / Moneda dorada Travel Cash
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, r * 0.25f, centerGoldPaint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

