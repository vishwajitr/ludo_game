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
import androidx.compose.ui.graphics.Color as UiColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ludo.SetupConfig
import ludo.engine.*

@Composable
fun HouseRules(r: Rules, onChange: (Rules) -> Unit) {
    SwitchRow("Blockades", r.blockades, "Two of your tokens on a square block passing") { onChange(r.copy(blockades = it)) }
    SwitchRow("Capture to enter home", r.mustCaptureToEnterHome, "Capture once before entering the home column") { onChange(r.copy(mustCaptureToEnterHome = it)) }
    SwitchRow("Start with one token out", r.startWithOneOut) { onChange(r.copy(startWithOneOut = it)) }
    SwitchRow("1 or 6 to leave yard", r.oneOrSixToLeave) { onChange(r.copy(oneOrSixToLeave = it)) }
}

private fun hint(bot: Difficulty?) = when (bot) {
    null -> "Plays on this device"
    Difficulty.EASY -> "Easy: random moves"
    Difficulty.MEDIUM -> "Medium: captures and dodges danger"
    Difficulty.HARD -> "Hard: weighs risk and plans ahead"
}

@Composable
fun SetupScreen(cfg: SetupConfig, onChange: (SetupConfig) -> Unit, onStart: () -> Unit, onBack: () -> Unit) {
    val colors = defaultColors(cfg.count)
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text("New game", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Choose who plays", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Section("Players") {
                Segmented(listOf("2 players" to 2, "3 players" to 3, "4 players" to 4), cfg.count) { onChange(cfg.copy(count = it)) }
            }
            for (i in 0 until cfg.count) {
                val seat = cfg.seats[i]
                fun set(s: Seat) = onChange(cfg.copy(seats = cfg.seats.toMutableList().also { it[i] = s }))
                val c = colors[i]
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        Box(Modifier.width(8.dp).fillMaxHeight().background(c.ui)) // seat color stripe
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp).background(c.ui, CircleShape), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", fontWeight = FontWeight.Bold, color = if (c == Color.YELLOW) UiColor.Black else UiColor.White)
                                }
                                Spacer(Modifier.width(12.dp))
                                OutlinedTextField(
                                    seat.name, { set(seat.copy(name = it.take(12))) }, Modifier.weight(1f),
                                    singleLine = true, label = { Text("${c.label} player") },
                                )
                            }
                            Segmented(
                                listOf("Human" to null, "Easy" to Difficulty.EASY, "Medium" to Difficulty.MEDIUM, "Hard" to Difficulty.HARD),
                                seat.bot,
                            ) { set(seat.copy(bot = it)) }
                            Text(hint(seat.bot), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Section("House rules") { HouseRules(cfg.rules) { onChange(cfg.copy(rules = it)) } }
        }
        // actions stay visible below the scrolling content
        Surface(tonalElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onBack, Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Back") }
                Button(onStart, Modifier.weight(2f).heightIn(min = 52.dp)) { Text("Start game") }
            }
        }
    }
}
