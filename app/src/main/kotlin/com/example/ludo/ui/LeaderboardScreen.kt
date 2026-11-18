package com.example.ludo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ludo.engine.*
import java.text.DateFormat
import java.util.Date

private fun label(name: String, bot: Difficulty?) = if (bot == null) name else "$name (${bot.name.lowercase()} bot)"

@Composable
fun LeaderboardScreen(records: List<GameRecord>, onClear: () -> Unit, onBack: () -> Unit) {
    var includeBots by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val stats = remember(records, includeBots) { leaderboard(records, includeBots) }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Clear history?") },
        text = { Text("This deletes all ${records.size} finished games from this device. It can't be undone.") },
        confirmButton = { TextButton({ confirmClear = false; onClear() }, Modifier.heightIn(min = 48.dp)) { Text("Clear") } },
        dismissButton = { TextButton({ confirmClear = false }, Modifier.heightIn(min = 48.dp)) { Text("Cancel") } },
    )

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text("Leaderboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Finished games on this device", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (records.isEmpty()) {
                Section("No games yet") { Text("Finish a game and the results will show up here.") }
            } else {
                Section("Standings") {
                    SwitchRow("Include bots", includeBots) { includeBots = it }
                    if (stats.isEmpty()) Text("No human players yet.")
                    stats.forEachIndexed { i, s ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}", Modifier.width(32.dp), style = MaterialTheme.typography.titleLarge)
                            Column(Modifier.weight(1f)) {
                                Text(label(s.name, s.bot), fontWeight = FontWeight.Medium)
                                Text("${s.games} games · ${s.winPercent}% wins", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${s.wins} wins", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Section("Recent games") {
                    val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
                    records.take(10).forEach { r ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(fmt.format(Date(r.time)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(r.players.sortedBy { it.rank }.joinToString("  ") { "${it.rank}. ${it.name}${if (it.bot != null) " (bot)" else ""}" })
                        }
                    }
                }
                TextButton({ confirmClear = true }, Modifier.heightIn(min = 48.dp)) { Text("Clear history") }
            }
        }
        Surface(tonalElevation = 3.dp) {
            OutlinedButton(onBack, Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 52.dp)) { Text("Back") }
        }
    }
}
