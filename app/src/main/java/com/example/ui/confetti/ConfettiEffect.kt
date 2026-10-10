package com.example.ui.confetti

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class ConfettiParticle(
    val xRatio: Float,
    val startYRatio: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float,
    val horizontalWobble: Float
)

@Composable
fun ConfettiCelebration(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {}
) {
    val progress = remember { Animatable(0f) }

    val colors = listOf(
        Color(0xFF22C55E),
        Color(0xFFF59E0B),
        Color(0xFFEC4899),
        Color(0xFF3B82F6),
        Color(0xFF8B5CF6),
        Color(0xFFEAB308),
        Color(0xFFEF4444),
        Color(0xFF10B981)
    )

    val particles = remember {
        List(70) {
            ConfettiParticle(
                xRatio = Random.nextFloat(),
                startYRatio = Random.nextFloat() * -0.5f,
                speed = 0.8f + Random.nextFloat() * 0.6f,
                size = 14f + Random.nextFloat() * 16f,
                color = colors[Random.nextInt(colors.size)],
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                horizontalWobble = (Random.nextFloat() - 0.5f) * 80f
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2800, easing = LinearEasing)
        )
        onFinished()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val p = progress.value
        val canvasWidth = size.width
        val canvasHeight = size.height

        particles.forEach { particle ->
            val currentY = (particle.startYRatio + p * particle.speed) * canvasHeight
            val currentX = particle.xRatio * canvasWidth + kotlin.math.sin(p * 10f) * particle.horizontalWobble
            val rotation = p * particle.rotationSpeed

            if (currentY in -50f..canvasHeight + 50f) {
                rotate(degrees = rotation, pivot = Offset(currentX, currentY)) {
                    drawRect(
                        color = particle.color.copy(alpha = (1f - (p * 0.3f)).coerceIn(0f, 1f)),
                        topLeft = Offset(currentX, currentY),
                        size = Size(particle.size, particle.size * 0.6f)
                    )
                }
            }
        }
    }
}
