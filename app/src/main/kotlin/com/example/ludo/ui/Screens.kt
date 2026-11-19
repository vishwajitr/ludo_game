package com.example.ludo.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ludo.engine.Color
import ludo.engine.Seat
import kotlin.math.sin
import kotlin.random.Random

/** The four-colored app mark: a tilted board quartered into the player colors. */
@Composable
fun LogoMark(modifier: Modifier = Modifier, dim: androidx.compose.ui.unit.Dp = 132.dp) {
    val r = dim * .26f
    val inner = dim * .18f
    val small = dim * .08f
    Box(
        modifier.size(dim).graphicsLayer { rotationZ = -6f }.shadow(18.dp, RoundedCornerShape(r)).clip(RoundedCornerShape(r))
            .background(Ludo.colors.boardCell).padding(dim * .06f)
            .semantics { contentDescription = "Ludo" },
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(dim * .045f)) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dim * .045f)) {
                Box(Modifier.weight(1f).fillMaxHeight().background(Color.BLUE.ui, RoundedCornerShape(inner, small, small, small)), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(dim * .17f).shadow(3.dp, CircleShape).background(Ludo.colors.boardCell, CircleShape))
                }
                Box(Modifier.weight(1f).fillMaxHeight().background(Color.RED.ui, RoundedCornerShape(small, inner, small, small)))
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dim * .045f)) {
                Box(Modifier.weight(1f).fillMaxHeight().background(Color.YELLOW.ui, RoundedCornerShape(small, small, small, inner)))
                Box(Modifier.weight(1f).fillMaxHeight().background(Color.GREEN.ui, RoundedCornerShape(small, small, inner, small)))
            }
        }
    }
}

@Composable
fun HomeScreen(canContinue: Boolean, onContinue: () -> Unit, onNew: () -> Unit, onHowTo: () -> Unit, onLeaderboard: () -> Unit, onSettings: () -> Unit) {
    val c = Ludo.colors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { IconSquare(LudoIcons.Settings, "Settings", onSettings) }
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            LogoMark()
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("LUDO", style = MaterialTheme.typography.displayLarge, color = c.text)
                Text("The classic race home, for 2–4 players", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            }
        }
        Spacer(Modifier.height(36.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (canContinue) ContinueCard(onContinue)
            PrimaryButton("New game", onNew, icon = LudoIcons.Play)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuTile("Leaderboard", LudoIcons.Trophy, c.accent, onLeaderboard, Modifier.weight(1f))
                MenuTile("How to play", LudoIcons.Book, Color.BLUE.light, onHowTo, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ContinueCard(onClick: () -> Unit) {
    val c = Ludo.colors
    Row(
        Modifier.fillMaxWidth().card(LudoShapes.medium).clickable(role = Role.Button, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.size(48.dp).background(c.raised, LudoShapes.iconButton).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(listOf(Color.BLUE, Color.RED), listOf(Color.YELLOW, Color.GREEN)).forEach { row ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { Box(Modifier.weight(1f).fillMaxHeight().background(it.ui, CircleShape)) }
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text("Continue game", style = MaterialTheme.typography.titleMedium, color = c.text)
            Text("Pick up where you left off", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Icon(LudoIcons.Chevron, null, Modifier.size(20.dp), tint = c.textMuted)
    }
}

@Composable
private fun MenuTile(label: String, icon: ImageVector, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier.height(92.dp).card(LudoShapes.medium).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = tint)
        Text(label, style = MaterialTheme.typography.titleSmall, color = Ludo.colors.text)
    }
}

@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    val rules = listOf(
        "Goal" to "Move all four of your tokens around the board and into the home triangle. The game goes on until only one player is left, and everyone is ranked by finishing order.",
        "Leaving the yard" to "A token leaves the yard only on a 6 and lands on your start square.",
        "Extra rolls" to "You roll again after a 6, after a capture, and when a token reaches home. Three 6s in a row lose your turn.",
        "Capturing" to "Landing on an opponent's token sends it back to its yard, unless it stands on a safe square (the four start squares and the four stars).",
        "Getting home" to "After a full lap your token turns into its home column. You need the exact roll to reach home.",
        "No moves" to "If no move is possible the turn passes automatically.",
        "House rules" to "Blockades, must-capture-to-enter-home, start with one token out, and 1-or-6-to-leave can be switched on in Settings or when you start a game.",
    )
    val c = Ludo.colors
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("How to play", onBack)
        Spacer(Modifier.height(4.dp))
        rules.forEachIndexed { i, (h, t) ->
            Row(Modifier.fillMaxWidth().card().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val color = Color.entries[(i + 3) % 4] // cycle Blue, Red, Green, Yellow
                Box(Modifier.size(32.dp).background(color.ui, CircleShape), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", style = MaterialTheme.typography.titleSmall, color = color.on)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(h, style = MaterialTheme.typography.titleMedium, color = c.text)
                    Text(t, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                }
            }
        }
        Text(
            "Tap the dice to roll, then tap a glowing token to move it.",
            Modifier.fillMaxWidth().background(c.accent.copy(alpha = .12f), LudoShapes.medium).border(1.dp, c.accent.copy(alpha = .4f), LudoShapes.medium).padding(16.dp),
            style = MaterialTheme.typography.bodyMedium, color = c.text,
        )
    }
}

@Composable
fun ResultsScreen(ranking: List<Pair<Seat, Color>>, onAgain: () -> Unit, onHome: () -> Unit) {
    val c = Ludo.colors
    val medals = listOf(
        c.accent to c.onAccent,
        c.silver to c.onMedal,
        c.bronze to c.onMedal,
    )
    Box(Modifier.fillMaxSize()) {
        Confetti(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                Modifier.size(132.dp).background(Brush.radialGradient(listOf(c.accent.copy(alpha = .35f), c.accent.copy(alpha = 0f))), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(LudoIcons.Trophy, "Trophy", Modifier.size(84.dp), tint = c.accent) }
            ranking.firstOrNull()?.let { (winner, _) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("WINNER", style = MaterialTheme.typography.labelMedium, color = c.accent)
                    Text(winner.name, style = MaterialTheme.typography.displaySmall, color = c.text)
                }
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ranking.forEachIndexed { i, (seat, color) ->
                    val first = i == 0
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 60.dp)
                            .background(if (first) c.accent.copy(alpha = .12f) else c.surface, LudoShapes.medium)
                            .border(if (first) 1.5.dp else 1.dp, if (first) c.accent.copy(alpha = .6f) else c.outline, LudoShapes.medium)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        val (medal, onMedal) = medals.getOrElse(i) { c.outline to c.text }
                        Box(Modifier.size(34.dp).background(medal, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.titleMedium.copy(fontFamily = DisplayFont, fontWeight = FontWeight.Bold), color = onMedal)
                        }
                        Box(Modifier.size(14.dp).background(color.ui, CircleShape))
                        Column(Modifier.weight(1f)) {
                            Text(seat.name, style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text(seat.bot?.let { "${it.name.lowercase().replaceFirstChar(Char::uppercase)} bot" } ?: "Human", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Play again", onAgain)
                SecondaryButton("Home", onHome)
            }
        }
    }
}

private class Piece(val x: Float, val phase: Float, val speed: Float, val color: androidx.compose.ui.graphics.Color, val rot: Float, val round: Boolean)

@Composable
private fun Confetti(modifier: Modifier) {
    val t by rememberInfiniteTransition(label = "confetti").animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "t")
    val pieces = remember {
        val colors = Color.entries.map { it.ui } + androidx.compose.ui.graphics.Color.White
        List(60) { Piece(Random.nextFloat(), Random.nextFloat(), 1f + Random.nextInt(2), colors.random(), Random.nextFloat() * 360, Random.nextInt(4) == 0) }
    }
    Canvas(modifier) {
        val s = density
        pieces.forEach { p ->
            val y = ((p.phase + t * p.speed) % 1f) * size.height
            val x = p.x * size.width + sin((t * p.speed * 6.28f * 2) + p.phase * 6) * 12 * s
            if (p.round) drawCircle(p.color.copy(alpha = .85f), 3.5f * s, Offset(x, y))
            else rotate(p.rot + t * 360 * p.speed, Offset(x, y)) { drawRect(p.color.copy(alpha = .85f), Offset(x, y), Size(4f * s, 8f * s)) }
        }
    }
}
