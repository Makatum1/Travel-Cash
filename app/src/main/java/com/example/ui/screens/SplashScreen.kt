package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlinx.coroutines.delay

/**
 * Pantalla de bienvenida / Splash Screen a pantalla completa mostrada al abrir la aplicación.
 * Muestra la nueva ilustración oficial de Travel Cash ("Controla tus gastos, disfruta el viaje")
 * durante 5 segundos o hasta que el usuario toque la pantalla para saltar a la aplicación.
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onSplashFinished: () -> Unit
) {
    val currentOnFinished by rememberUpdatedState(onSplashFinished)
    var hasFinished by remember { mutableStateOf(false) }
    val alphaAnim = remember { Animatable(0f) }

    fun finishSplash() {
        if (!hasFinished) {
            hasFinished = true
            currentOnFinished()
        }
    }

    LaunchedEffect(Unit) {
        // Entrada suave
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 150, easing = LinearEasing)
        )
        // Permanecer visible hasta completar exactamente 4.3 segundos (150ms + 4000ms + 150ms = 4300ms)
        delay(4000)
        // Salida suave
        alphaAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 150, easing = LinearEasing)
        )
        finishSplash()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF033E5C))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tocar la pantalla salta inmediatamente a la aplicación
                finishSplash()
            }
            .testTag("app_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_splash_screen),
            contentDescription = "Travel Cash - Gestión de Gastos - Controla tus gastos, disfruta el viaje",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = alphaAnim.value
                }
        )
    }
}
