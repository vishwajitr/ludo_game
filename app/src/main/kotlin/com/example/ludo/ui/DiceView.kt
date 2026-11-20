package com.example.ludo.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

// pip positions on a 3x3 grid (0..8), by face value
private val PIPS = mapOf(
    1 to listOf(4), 2 to listOf(0, 8), 3 to listOf(0, 4, 8), 4 to listOf(0, 2, 6, 8),
    5 to listOf(0, 2, 4, 6, 8), 6 to listOf(0, 2, 3, 5, 6, 8),
)

@Composable
fun DiceView(value: Int, rolling: Boolean, enabled: Boolean, tint: Color, modifier: Modifier = Modifier, rollMs: Int = 600, dim: Dp = 52.dp, onClick: () -> Unit) {
    var face by remember { mutableIntStateOf(value.coerceAtLeast(1)) }
    val spin = remember { Animatable(0f) }
    LaunchedEffect(rolling, value) {
        if (rolling) {
            while (true) { face = Random.nextInt(1, 7); delay(70) }
        } else if (value > 0) face = value
    }
    LaunchedEffect(rolling) { if (rolling) { spin.snapTo(0f); spin.animateTo(360f, tween(rollMs, easing = FastOutSlowInEasing)) } else spin.snapTo(0f) }
    val label = if (value == 0) "Dice. Tap to roll" else "Dice, rolled $value" + if (enabled) ". Tap to roll" else ""
    Canvas(
        modifier.size(dim) // >= 48dp touch target
            .graphicsLayer { if (rolling) { rotationX = spin.value; rotationY = spin.value * 2f; cameraDistance = 12f * density } }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        val r = CornerRadius(size.width * .22f)
        val lift = size.height * .08f // the darker band under the face makes it read as a cube
        drawRoundRect(Ludo.colors.diceSide, cornerRadius = r)
        drawRoundRect(Brush.verticalGradient(listOf(Color.White, Ludo.colors.diceFace)), size = Size(size.width, size.height - lift), cornerRadius = r)
        if (enabled) drawRoundRect(tint, size = Size(size.width, size.height - lift), cornerRadius = r, style = Stroke(size.width * .05f))
        val inset = size.width * .14f
        val cell = (size.width - 2 * inset) / 3f
        PIPS.getValue(face).forEach { i ->
            drawCircle(Ludo.colors.dicePip, cell * .22f, Offset(inset + (i % 3 + .5f) * cell, inset * .8f + (i / 3 + .5f) * cell))
        }
    }
}
