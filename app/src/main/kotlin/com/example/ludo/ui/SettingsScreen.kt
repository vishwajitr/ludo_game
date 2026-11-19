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
fun SettingsScreen(s: Settings, update: ((Settings) -> Settings) -> Unit, onAdPrivacy: (() -> Unit)?, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScreenHeader("Settings", onBack)
            Section("Sound & feel") {
                SwitchRow("Sound effects", s.sound) { v -> update { it.copy(sound = v) } }
                SwitchRow("Music", s.music) { v -> update { it.copy(music = v) } }
                SwitchRow("Haptics", s.haptics) { v -> update { it.copy(haptics = v) } }
            }
            Section("Gameplay") {
                SwitchRow("Auto-move", s.autoMove, "Play automatically when only one move is possible") { v -> update { it.copy(autoMove = v) } }
                Text("Animation speed", Modifier.padding(top = 8.dp, bottom = 8.dp), style = MaterialTheme.typography.titleSmall, color = Ludo.colors.text)
                Segmented(listOf("1x" to 1, "2x" to 2), s.animSpeed, Modifier.padding(bottom = 8.dp)) { v -> update { it.copy(animSpeed = v) } }
            }
            Section("Appearance") {
                SwitchRow("Colorblind mode", s.colorblind, "Different shapes for each player's tokens") { v -> update { it.copy(colorblind = v) } }
            }
            if (onAdPrivacy != null) Section("Privacy") {
                TextButton(onAdPrivacy, Modifier.heightIn(min = 48.dp)) { Text("Ad privacy settings", color = Ludo.colors.accent) }
            }
            Section("Default house rules") {
                HouseRules(s.rules) { v -> update { it.copy(rules = v) } }
                Text("Applies to the next game you start.", Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.bodySmall, color = Ludo.colors.textMuted)
            }
        }
    }
}
