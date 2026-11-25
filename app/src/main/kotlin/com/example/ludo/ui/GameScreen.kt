package com.example.ludo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
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
    sound: Boolean = true,
    music: Boolean = true,
    onSoundChange: (Boolean) -> Unit = {},
    onMusicChange: (Boolean) -> Unit = {},
) {
    var showPause by rememberSaveable { mutableStateOf(false) }
    fun pause(p: Boolean) { showPause = p; vm.setPaused(p) }
    BackHandler { pause(true) }
    val ui by vm.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    fun colorOf(player: Int) = vm.state.value.game.players[player].color

    LaunchedEffect(ui.game.phase) { if (ui.game.phase == Phase.GAME_OVER) { delay(1200); onFinished() } }
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                is GameEvent.Captured -> { fx?.play(Sfx.CAPTURE, colorOf(e.by)); if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                is GameEvent.ReachedHome -> fx?.play(Sfx.HOME, colorOf(e.player))
                is GameEvent.GameOver -> fx?.play(Sfx.WIN)
                else -> {}
            }
        }
    }
    LaunchedEffect(vm) { vm.hops.collect { fx?.play(Sfx.STEP, colorOf(it)) } }
    LaunchedEffect(ui.rolling) { if (ui.rolling) fx?.play(Sfx.ROLL, colorOf(ui.game.current)) }

    if (showPause) AlertDialog(
        onDismissRequest = { pause(false) },
        containerColor = Ludo.colors.raised,
        shape = LudoShapes.large,
        title = { Text("Paused", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text("Progress is saved after every move.", color = Ludo.colors.textMuted)
                SwitchRow("Sound effects", sound, onChange = onSoundChange)
                SwitchRow("Music", music, onChange = onMusicChange)
            }
        },
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
    val c = Ludo.colors
    val humanTurn = seat.bot == null && g.phase != Phase.GAME_OVER

    val dice = @Composable {
        DiceView(
            value = ui.diceValue, rolling = ui.rolling, tint = color.ui, rollMs = vm.rollMs,
            enabled = !ui.busy && g.phase == Phase.ROLLING && seat.bot == null,
        ) { if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.onRoll() }
    }
    /** The card for whoever sits in board corner [corner] (0 top-left, 1 top-right, 2 bottom-right, 3 bottom-left). */
    val card = @Composable { corner: Int, m: Modifier ->
        val i = g.players.indexOfFirst { BoardGeometry.seat(it.color) == corner }
        if (i < 0) Spacer(m)
        else PlayerCard(
            seat = vm.seats[i], color = g.players[i].color, home = ui.shown[i].count { it == HOME },
            active = i == g.current && g.phase != Phase.GAME_OVER, modifier = m, dice = dice,
        )
    }
    val topBar = @Composable {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconSquare(LudoIcons.Pause, "Pause", { pause(true) })
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row(
                    Modifier.clip(CircleShape).background(color.ui.copy(alpha = .16f)).border(1.dp, color.ui.copy(alpha = .5f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(10.dp).background(color.ui, CircleShape))
                    Text(
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, // TalkBack announces turn changes
                        text = if (g.phase == Phase.GAME_OVER) "Game over: " + g.winners.joinToString(" > ") { vm.seats[it].name }
                        else "${seat.name}'s turn",
                        style = MaterialTheme.typography.titleLarge, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconSquare(if (sound) LudoIcons.SoundOn else LudoIcons.SoundOff, if (sound) "Mute sound" else "Unmute sound", { onSoundChange(!sound) })
        }
    }
    val hint = @Composable {
        val text = ui.message ?: when {
            g.phase == Phase.GAME_OVER -> ""
            !humanTurn -> "${seat.name} is playing…"
            g.phase == Phase.CHOOSING_MOVE -> "Tap a glowing token"
            g.phase == Phase.ROLLING && !ui.busy -> "Tap the dice to roll"
            else -> ""
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp)
                .background(c.accent.copy(alpha = if (text.isEmpty()) 0f else .1f), LudoShapes.medium)
                .border(1.dp, c.accent.copy(alpha = if (text.isEmpty()) 0f else .35f), LudoShapes.medium)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            if (text.isNotEmpty()) Box(Modifier.size(8.dp).background(c.accent, CircleShape))
            Text(text, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodyLarge, color = c.text)
        }
    }
    val boardView = @Composable { m: Modifier ->
        BoardCanvas(board, m, ui.movable, ui.flash, vm.hopMs, colorblind, vm::onTokenTap)
    }

    BoxWithConstraints(modifier.fillMaxSize().padding(Dimens.screenPadding)) {
        if (maxWidth > maxHeight) { // landscape: board on the left, players and controls beside it
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                boardView(Modifier.fillMaxHeight())
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    topBar()
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { card(0, Modifier.weight(1f)); card(1, Modifier.weight(1f)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { card(3, Modifier.weight(1f)); card(2, Modifier.weight(1f)) }
                    hint()
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                topBar()
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { card(0, Modifier.weight(1f)); card(1, Modifier.weight(1f)) }
                // full width when there is room; on short screens the board shrinks so the hint stays visible
                boardView(Modifier.fillMaxWidth().weight(1f, fill = false))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { card(3, Modifier.weight(1f)); card(2, Modifier.weight(1f)) }
                Spacer(Modifier.weight(.001f)) // pushes the hint to the bottom without taking space from the board
                hint()
            }
        }
    }
}

/** A player's card: avatar, name, tokens home; the current player's card glows and holds the dice. */
@Composable
private fun PlayerCard(seat: Seat, color: Color, home: Int, active: Boolean, modifier: Modifier, dice: @Composable () -> Unit) {
    val c = Ludo.colors
    Row(
        modifier.heightIn(min = 64.dp)
            .then(if (active) Modifier.shadow(12.dp, LudoShapes.medium, ambientColor = color.ui, spotColor = color.ui) else Modifier)
            .clip(LudoShapes.medium).background(c.surface)
            .border(2.dp, if (active) color.ui else c.outline, LudoShapes.medium)
            .alpha(if (active) 1f else .85f)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // icons only: the name lives in the screen-reader label
        Box(
            Modifier.size(44.dp).background(Brush.linearGradient(listOf(color.ui.copy(alpha = .75f), color.ui)), CircleShape)
                .border(2.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = .25f), CircleShape)
                .semantics { contentDescription = seat.name + if (seat.bot != null) ", computer" else "" },
            contentAlignment = Alignment.Center,
        ) { Icon(if (seat.bot != null) LudoIcons.Bot else LudoIcons.Person, null, Modifier.size(24.dp), tint = color.on) }
        // 2x2 tokens, lit as each one reaches home
        Column(
            Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = "$home of 4 tokens home" },
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(2) { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(2) { k ->
                        val done = r * 2 + k < home // round token like on the board: filled in colour once home, a faint ring until then
                        Box(
                            Modifier.size(16.dp)
                                .then(if (done) Modifier.background(Brush.radialGradient(0f to androidx.compose.ui.graphics.Color.White.copy(alpha = .6f), .45f to color.ui, 1f to color.ui, center = Offset(13f, 13f), radius = 34f), CircleShape) else Modifier)
                                .border(2.dp, if (done) color.ui else c.textMuted.copy(alpha = .35f), CircleShape),
                        )
                    }
                }
            }
        }
        if (active) dice()
    }
}
