package com.example.ludo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
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
    val pal = Ludo.colors
    var rulesOpen by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Dimens.screenPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScreenHeader("New game", onBack, "Choose who plays")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Players", Modifier.padding(start = 4.dp))
                Segmented(listOf("2" to 2, "3" to 3, "4" to 4), cfg.count) { onChange(cfg.copy(count = it)) }
            }
            for (i in 0 until cfg.count) {
                val seat = cfg.seats[i]
                fun set(s: Seat) = onChange(cfg.copy(seats = cfg.seats.toMutableList().also { it[i] = s }))
                val c = colors[i]
                Column(Modifier.fillMaxWidth().card().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).border(2.dp, c.ui, CircleShape).padding(4.dp).background(c.ui, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text("${i + 1}", style = MaterialTheme.typography.titleLarge, color = c.on) }
                        Spacer(Modifier.width(12.dp))
                        OutlinedTextField(
                            seat.name, { set(seat.copy(name = it.take(12))) }, Modifier.weight(1f),
                            singleLine = true, label = { Text("${c.label} player") },
                            textStyle = MaterialTheme.typography.titleMedium,
                            shape = LudoShapes.small,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = c.ui, unfocusedBorderColor = pal.outline,
                                focusedLabelColor = c.ui, unfocusedLabelColor = pal.textMuted, cursorColor = c.ui,
                                focusedTextColor = pal.text, unfocusedTextColor = pal.text,
                            ),
                        )
                    }
                    Segmented(
                        listOf("Human" to null, "Easy" to Difficulty.EASY, "Medium" to Difficulty.MEDIUM, "Hard" to Difficulty.HARD),
                        seat.bot, accent = c.ui,
                    ) { set(seat.copy(bot = it)) }
                    Text(hint(seat.bot), style = MaterialTheme.typography.bodySmall, color = pal.textMuted)
                }
            }
            Column(Modifier.fillMaxWidth().card()) {
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { rulesOpen = !rulesOpen }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("House rules", style = MaterialTheme.typography.titleMedium, color = pal.text)
                        val on = with(cfg.rules) { listOf(blockades, mustCaptureToEnterHome, startWithOneOut, oneOrSixToLeave).count { it } }
                        Text(if (on == 0) "Classic rules" else "$on enabled", style = MaterialTheme.typography.bodySmall, color = pal.textMuted)
                    }
                    Icon(LudoIcons.Chevron, if (rulesOpen) "Hide house rules" else "Show house rules", Modifier.size(20.dp).rotate(if (rulesOpen) 90f else 0f), tint = pal.textMuted)
                }
                AnimatedVisibility(rulesOpen) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) { HouseRules(cfg.rules) { onChange(cfg.copy(rules = it)) } }
                }
            }
        }
        // the main action stays visible below the scrolling content
        Box(Modifier.fillMaxWidth().background(pal.surface).padding(horizontal = Dimens.screenPadding, vertical = 12.dp)) {
            PrimaryButton("Start game", onStart, icon = LudoIcons.Play)
        }
    }
}
