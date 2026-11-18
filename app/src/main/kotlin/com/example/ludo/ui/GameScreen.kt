package com.example.ludo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ludo.game.GameViewModel
import com.example.ludo.game.Seat
import ludo.engine.*

@Composable
fun GameScreen(modifier: Modifier = Modifier, vm: GameViewModel = viewModel { GameViewModel(seats = listOf(Seat("You"), Seat("Easy", Difficulty.EASY), Seat("Medium", Difficulty.MEDIUM), Seat("Hard", Difficulty.HARD))) }) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
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
                if (g.phase == Phase.GAME_OVER) "Game over: " + g.winners.joinToString(" > ") { vm.seats[it].name }
                else "${seat.name}'s turn" + (seat.bot?.let { " (${it.name.lowercase()} bot)" } ?: ""),
            )
        }
        BoardCanvas(board, Modifier.fillMaxWidth(), ui.movable, ui.flash, vm.hopMs, vm::onTokenTap)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DiceView(
                value = ui.diceValue, rolling = ui.rolling, tint = color.ui,
                enabled = !ui.busy && g.phase == Phase.ROLLING && seat.bot == null,
            ) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.onRoll() }
            Text(ui.message ?: if (g.phase == Phase.CHOOSING_MOVE && seat.bot == null) "Tap a highlighted token" else "", Modifier.heightIn(min = 24.dp))
        }
    }
}
