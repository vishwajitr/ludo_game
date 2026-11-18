package com.example.ludo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ludo.data.Settings

@Composable
fun SettingsScreen(s: Settings, update: ((Settings) -> Settings) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        SwitchRow("Sound effects", s.sound) { v -> update { it.copy(sound = v) } }
        SwitchRow("Music", s.music) { v -> update { it.copy(music = v) } }
        SwitchRow("Haptics", s.haptics) { v -> update { it.copy(haptics = v) } }
        SwitchRow("Auto-move when only one move", s.autoMove) { v -> update { it.copy(autoMove = v) } }
        SwitchRow("Dark mode", s.dark) { v -> update { it.copy(dark = v) } }
        Text("Animation speed")
        ChipRow(listOf("1x" to 1, "2x" to 2), s.animSpeed) { v -> update { it.copy(animSpeed = v) } }
        Text("Default house rules", style = MaterialTheme.typography.titleMedium)
        HouseRules(s.rules) { v -> update { it.copy(rules = v) } }
        Text("Changes apply to the next game you start.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onBack, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Back") }
    }
}
