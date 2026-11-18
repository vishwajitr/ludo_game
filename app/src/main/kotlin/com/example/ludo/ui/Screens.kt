package com.example.ludo.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import ludo.engine.Color
import ludo.engine.Seat
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun HomeScreen(canContinue: Boolean, onContinue: () -> Unit, onNew: () -> Unit, onHowTo: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Ludo", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(24.dp))
        val m = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        if (canContinue) Button(onContinue, m) { Text("Continue") }
        if (canContinue) OutlinedButton(onNew, m) { Text("New game") } else Button(onNew, m) { Text("New game") }
        OutlinedButton(onHowTo, m) { Text("How to play") }
        OutlinedButton(onSettings, m) { Text("Settings") }
    }
}

@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    val rules = listOf(
        "Goal" to "Move all four of your tokens around the board and into the home triangle. The game goes on until only one player is left, and everyone is ranked by finishing order.",
        "Leaving the yard" to "A token leaves the yard only on a 6 and lands on your start square.",
        "Extra rolls" to "You roll again after a 6, after a capture, and when a token reaches home. Three 6s in a row lose your turn.",
        "Capturing" to "Landing on an opponent's token sends it back to its yard, unless it stands on a safe square (the four start squares and the four stars).",
        "Getting home" to "After a full lap your token turns into its home column. You need the exact roll to reach home.",
        "No moves" to "If no move is possible the turn passes automatically.",
        "House rules" to "Blockades, must-capture-to-enter-home, start with one token out, and 1-or-6-to-leave can be switched on in Settings or when you start a game.",
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("How to play", style = MaterialTheme.typography.headlineMedium)
        rules.forEach { (h, t) ->
            Column { Text(h, style = MaterialTheme.typography.titleMedium); Text(t) }
        }
        Text("Tap the dice to roll, then tap a pulsing token to move it.")
        OutlinedButton(onBack, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Back") }
    }
}

@Composable
fun ResultsScreen(ranking: List<Pair<Seat, Color>>, onAgain: () -> Unit, onHome: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Confetti(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
            Text("Results", style = MaterialTheme.typography.headlineLarge)
            ranking.forEachIndexed { i, (seat, color) ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}.", Modifier.width(36.dp), style = MaterialTheme.typography.titleLarge)
                    Box(Modifier.size(24.dp).background(color.ui, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(seat.name, style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onAgain, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Play again") }
            OutlinedButton(onHome, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Home") }
        }
    }
}

private class Piece(val x: Float, val phase: Float, val speed: Float, val color: androidx.compose.ui.graphics.Color, val rot: Float)

@Composable
private fun Confetti(modifier: Modifier) {
    val t by rememberInfiniteTransition(label = "confetti").animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "t")
    val pieces = remember { List(70) { Piece(Random.nextFloat(), Random.nextFloat(), 1f + Random.nextInt(3), Color.entries.random().ui, Random.nextFloat() * 360) } }
    Canvas(modifier) {
        pieces.forEach { p ->
            val y = ((p.phase + t * p.speed) % 1f) * size.height
            val x = p.x * size.width + sin((t * p.speed * 6.28f * 2) + p.phase * 6) * 24
            rotate(p.rot + t * 360 * p.speed, Offset(x, y)) { drawRect(p.color, Offset(x, y), Size(12f, 20f)) }
        }
    }
}
