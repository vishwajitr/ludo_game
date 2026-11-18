package com.example.ludo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ludo.engine.*

@Composable
fun GameScreen(state: GameState, modifier: Modifier = Modifier) {
    val color = state.players[state.current].color
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).background(color.ui, CircleShape))
            Spacer(Modifier.width(12.dp))
            Text("${color.label}'s turn")
        }
        BoardCanvas(state, Modifier.fillMaxWidth())
    }
}

/** Static placeholder position until the turn flow lands in phase 4. */
fun sampleState(): GameState {
    val s = newGame(defaultColors(4))
    val tokens = listOf(
        listOf(0, 10, 51, YARD), listOf(13, 13, YARD, HOME), listOf(26, 40, 55, YARD), listOf(YARD, YARD, YARD, YARD),
    )
    return s.copy(players = s.players.mapIndexed { i, p -> p.copy(tokens = tokens[i]) })
}
