package com.example.ludo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ludo.SetupConfig
import ludo.engine.*

@Composable
fun HouseRules(r: Rules, onChange: (Rules) -> Unit) {
    SwitchRow("Blockades (two tokens block passing)", r.blockades) { onChange(r.copy(blockades = it)) }
    SwitchRow("Must capture to enter home column", r.mustCaptureToEnterHome) { onChange(r.copy(mustCaptureToEnterHome = it)) }
    SwitchRow("Start with one token out", r.startWithOneOut) { onChange(r.copy(startWithOneOut = it)) }
    SwitchRow("1 or 6 to leave yard", r.oneOrSixToLeave) { onChange(r.copy(oneOrSixToLeave = it)) }
}

@Composable
fun SetupScreen(cfg: SetupConfig, onChange: (SetupConfig) -> Unit, onStart: () -> Unit, onBack: () -> Unit) {
    val colors = defaultColors(cfg.count)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("New game", style = MaterialTheme.typography.headlineMedium)
        Text("Players")
        ChipRow(listOf("2" to 2, "3" to 3, "4" to 4), cfg.count) { onChange(cfg.copy(count = it)) }
        for (i in 0 until cfg.count) {
            val seat = cfg.seats[i]
            fun set(s: Seat) = onChange(cfg.copy(seats = cfg.seats.toMutableList().also { it[i] = s }))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(24.dp).background(colors[i].ui, CircleShape))
                        Spacer(Modifier.width(12.dp))
                        OutlinedTextField(seat.name, { set(seat.copy(name = it.take(12))) }, Modifier.weight(1f), singleLine = true, label = { Text(colors[i].label) })
                    }
                    ChipRow(
                        listOf("Human" to null, "Easy" to Difficulty.EASY, "Medium" to Difficulty.MEDIUM, "Hard" to Difficulty.HARD),
                        seat.bot,
                    ) { set(seat.copy(bot = it)) }
                }
            }
        }
        Text("House rules", style = MaterialTheme.typography.titleMedium)
        HouseRules(cfg.rules) { onChange(cfg.copy(rules = it)) }
        Button(onStart, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Start") }
        OutlinedButton(onBack, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Back") }
    }
}
