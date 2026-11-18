package com.example.ludo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ludo.Sfx
import com.example.ludo.SoundFx
import com.example.ludo.game.GameViewModel
import kotlinx.coroutines.delay
import ludo.engine.*

@Composable
fun GameScreen(
    vm: GameViewModel,
    haptics: Boolean,
    onQuit: () -> Unit,
    onRestart: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    fx: SoundFx? = null,
    colorblind: Boolean = false,
) {
    var showPause by rememberSaveable { mutableStateOf(false) }
    fun pause(p: Boolean) { showPause = p; vm.setPaused(p) }
    BackHandler { pause(true) }
    val ui by vm.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(ui.game.phase) { if (ui.game.phase == Phase.GAME_OVER) { delay(1200); onFinished() } }
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                is GameEvent.Captured -> { fx?.play(Sfx.CAPTURE); if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                is GameEvent.ReachedHome -> fx?.play(Sfx.HOME)
                is GameEvent.GameOver -> fx?.play(Sfx.WIN)
                else -> {}
            }
        }
    }
    LaunchedEffect(vm) { vm.hops.collect { fx?.play(Sfx.STEP) } }
    LaunchedEffect(ui.rolling) { if (ui.rolling) fx?.play(Sfx.ROLL) }

    if (showPause) AlertDialog(
        onDismissRequest = { pause(false) },
        title = { Text("Paused") },
        text = { Text("Progress is saved after every move.") },
        confirmButton = { TextButton({ pause(false) }, Modifier.heightIn(min = 48.dp)) { Text("Resume") } },
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

    val turnBar = @Composable {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).background(color.ui, CircleShape))
            Spacer(Modifier.width(12.dp))
            Text(
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }, // TalkBack announces turn changes
                text = if (g.phase == Phase.GAME_OVER) "Game over: " + g.winners.joinToString(" > ") { vm.seats[it].name }
                else "${seat.name}'s turn" + (seat.bot?.let { " (${it.name.lowercase()} bot)" } ?: ""),
            )
            TextButton({ pause(true) }, Modifier.heightIn(min = 48.dp)) { Text("Pause") }
        }
    }
    val controls = @Composable {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DiceView(
                value = ui.diceValue, rolling = ui.rolling, tint = color.ui,
                enabled = !ui.busy && g.phase == Phase.ROLLING && seat.bot == null,
            ) { if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.onRoll() }
            Text(
                ui.message ?: if (g.phase == Phase.CHOOSING_MOVE && seat.bot == null) "Tap a highlighted token" else "",
                Modifier.heightIn(min = 24.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
    val boardView = @Composable { m: Modifier ->
        BoardCanvas(board, m, ui.movable, ui.flash, vm.hopMs, colorblind, vm::onTokenTap)
    }

    BoxWithConstraints(modifier.fillMaxSize().padding(16.dp)) {
        if (maxWidth > maxHeight) { // landscape: centered square board on the left, controls beside it
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                boardView(Modifier.fillMaxHeight())
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { turnBar(); controls() }
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                turnBar()
                boardView(Modifier.fillMaxWidth())
                controls()
            }
        }
    }
}
