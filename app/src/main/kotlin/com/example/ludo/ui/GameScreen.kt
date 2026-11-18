package com.example.ludo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ludo.game.GameViewModel
import ludo.engine.*

@Composable
fun GameScreen(vm: GameViewModel, haptics: Boolean, onQuit: () -> Unit, onRestart: () -> Unit, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    var showPause by rememberSaveable { mutableStateOf(false) }
    BackHandler { showPause = true; vm.setPaused(true) }
    val ui by vm.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(ui.game.phase) { if (ui.game.phase == Phase.GAME_OVER) { delay(1200); onFinished() } }
    if (showPause) AlertDialog(
        onDismissRequest = { showPause = false; vm.setPaused(false) },
        title = { Text("Paused") },
        text = { Text("Progress is saved after every move.") },
        confirmButton = { TextButton({ showPause = false; vm.setPaused(false) }, Modifier.heightIn(min = 48.dp)) { Text("Resume") } },
        dismissButton = {
            Row {
                TextButton({ showPause = false; onRestart() }, Modifier.heightIn(min = 48.dp)) { Text("Restart") }
                TextButton({ showPause = false; onQuit() }, Modifier.heightIn(min = 48.dp)) { Text("Quit") }
            }
        },
    )
    val g = ui.game
    val color = g.players[g.current].color
    val seat = vm.seats[g.current]
    // board shows `shown` positions, which lag the engine state while a move animates
    val board = g.copy(players = g.players.mapIndexed { i, p -> p.copy(tokens = ui.shown[i]) })
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).background(color.ui, CircleShape))
            Spacer(Modifier.width(12.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = if (g.phase == Phase.GAME_OVER) "Game over: " + g.winners.joinToString(" > ") { vm.seats[it].name }
                else "${seat.name}'s turn" + (seat.bot?.let { " (${it.name.lowercase()} bot)" } ?: ""),
            )
            TextButton({ showPause = true; vm.setPaused(true) }, Modifier.heightIn(min = 48.dp)) { Text("Pause") }
        }
        BoardCanvas(board, Modifier.fillMaxWidth(), ui.movable, ui.flash, vm.hopMs, vm::onTokenTap)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DiceView(
                value = ui.diceValue, rolling = ui.rolling, tint = color.ui,
                enabled = !ui.busy && g.phase == Phase.ROLLING && seat.bot == null,
            ) { if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.onRoll() }
            Text(ui.message ?: if (g.phase == Phase.CHOOSING_MOVE && seat.bot == null) "Tap a highlighted token" else "", Modifier.heightIn(min = 24.dp))
        }
    }
}
