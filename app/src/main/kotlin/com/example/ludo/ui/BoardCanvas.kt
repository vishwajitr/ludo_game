package com.example.ludo.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as UiColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import ludo.engine.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private data class Tk(val player: Int, val index: Int, val color: Color, val pos: Int, val point: P)

private fun describe(t: Tk, name: String) = "$name token ${t.index + 1}, " + when {
    t.pos == YARD -> "in yard"
    t.pos == HOME -> "home"
    t.pos > LAST_TRACK -> "in home column"
    else -> "${t.pos} steps from start"
}

@Composable
fun BoardCanvas(
    state: GameState,
    modifier: Modifier = Modifier,
    movable: Set<Int> = emptySet(),
    flash: Set<Pair<Int, Int>> = emptySet(),
    hopMs: Int = 120,
    colorblind: Boolean = false,
    onTokenTap: (Int) -> Unit = {},
) {
    val active = state.players[state.current].color
    val tokens = state.players.flatMapIndexed { pi, p ->
        p.tokens.mapIndexed { ti, pos -> Tk(pi, ti, p.color, pos, BoardGeometry.tokenPoint(p.color, pos, ti)) }
    }
    val c = Ludo.colors
    Box(
        modifier.aspectRatio(1f)
            .shadow(20.dp, RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(c.boardFrameTop, c.boardFrameBottom)), RoundedCornerShape(20.dp))
            .padding(7.dp)
            .semantics { contentDescription = "Ludo board, ${active.label}'s turn" },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().clip(RoundedCornerShape(13.dp)).background(c.boardCell)) {
            BoardBackground(Modifier.fillMaxSize().graphicsLayer(), active, colorblind) // static: cached, not redrawn per animation frame
            TokenLayer(state, tokens, movable, flash, hopMs, colorblind, onTokenTap, Modifier.fillMaxSize())
            // accessibility: one node per token, 48dp focus area, actionable when it can move (touches are handled by the canvas)
            val u = maxWidth / BoardGeometry.SIZE
            for (t in tokens) {
                val canMove = t.player == state.current && t.index in movable
                Box(
                    Modifier.offset(u * t.point.x - 24.dp, u * t.point.y - 24.dp).size(48.dp).semantics {
                        contentDescription = describe(t, t.color.label) + if (canMove) ", can move" else ""
                        if (canMove) onClick("Move") { onTokenTap(t.index); true }
                    },
                )
            }
        }
    }
}

@Composable
private fun BoardBackground(modifier: Modifier, active: Color, colorblind: Boolean) {
    Canvas(modifier) {
        val pal = Ludo.colors
        val u = size.width / BoardGeometry.SIZE
        fun px(p: P) = Offset(p.x * u, p.y * u)
        fun cell(rc: Pair<Int, Int>, fill: UiColor) {
            val tl = Offset(rc.second * u, rc.first * u)
            drawRect(fill, tl, Size(u, u))
            drawRect(pal.boardLine, tl, Size(u, u), style = Stroke(1f))
        }

        for (color in Color.entries) {
            val (r0, c0) = BoardGeometry.yardOrigin(color)
            val tl = Offset(c0 * u, r0 * u)
            drawRect(Brush.linearGradient(listOf(color.ui, color.dark), tl, tl + Offset(6 * u, 6 * u)), tl, Size(6 * u, 6 * u))
            // inner panel with a soft inset shadow
            val pTl = Offset((c0 + .96f) * u, (r0 + .96f) * u)
            val pSize = Size(4.08f * u, 4.08f * u)
            val corner = CornerRadius(u * .75f)
            drawRoundRect(UiColor(0x33000000), pTl - Offset(0f, u * .06f), pSize, corner)
            drawRoundRect(pal.yardPanel, pTl + Offset(0f, u * .04f), Size(pSize.width, pSize.height - u * .04f), corner)
            repeat(4) {
                val at = px(BoardGeometry.yardSlot(color, it))
                drawCircle(color.light, u * .56f, at)
                drawCircle(UiColor(0x22000000), u * .56f, at, style = Stroke(u * .07f))
            }
            if (colorblind) shape(color, px(P(c0 + 3f, r0 + 3f)), u * .5f, color.ui.copy(alpha = .35f)) // pattern marker in each yard
            if (color == active) // highlight the active player's yard
                drawRect(pal.accent, tl + Offset(u * .1f, u * .1f), Size(5.8f * u, 5.8f * u), style = Stroke(u * .2f))
        }

        BoardGeometry.track.forEachIndexed { abs, rc ->
            val startOf = Color.entries.firstOrNull { it.start == abs }
            cell(rc, startOf?.ui ?: pal.boardCell)
            if (abs in SAFE_SQUARES && startOf == null) star(px(P(rc.second + .5f, rc.first + .5f)), u * .36f, pal.star)
        }
        for (color in Color.entries) {
            BoardGeometry.homeColumn(color).forEach { cell(it, color.ui) }
            // arrow outside the home column, pointing into it
            val a = BoardGeometry.arrowCell(color)
            withTransform({ translate((a.second + .5f) * u, (a.first + .5f) * u); rotate(BoardGeometry.seat(color) * 90f, Offset.Zero); scale(u, u, Offset.Zero) }) {
                drawLine(color.ui, Offset(-.3f, 0f), Offset(.25f, 0f), .11f, StrokeCap.Round)
                drawLine(color.ui, Offset(.25f, 0f), Offset(.03f, -.22f), .11f, StrokeCap.Round)
                drawLine(color.ui, Offset(.25f, 0f), Offset(.03f, .22f), .11f, StrokeCap.Round)
            }
        }

        val c = px(P(7.5f, 7.5f))
        fun tri(a: P, b: P, color: UiColor) = drawPath(
            Path().apply { moveTo(px(a).x, px(a).y); lineTo(px(b).x, px(b).y); lineTo(c.x, c.y); close() }, color,
        )
        for (color in Color.entries) when (BoardGeometry.seat(color)) {
            0 -> tri(P(6f, 6f), P(6f, 9f), color.ui)
            1 -> tri(P(6f, 6f), P(9f, 6f), color.ui)
            2 -> tri(P(9f, 6f), P(9f, 9f), color.ui)
            else -> tri(P(6f, 9f), P(9f, 9f), color.ui)
        }
        drawCircle(UiColor.White.copy(alpha = .9f), u * .48f, c)
    }
}

@Composable
private fun TokenLayer(
    state: GameState, tokens: List<Tk>, movable: Set<Int>, flash: Set<Pair<Int, Int>>,
    hopMs: Int, colorblind: Boolean, onTokenTap: (Int) -> Unit, modifier: Modifier,
) {
    val measurer = rememberTextMeasurer()
    // hop between squares; slide back to the yard at a calmer pace
    val anim = tokens.map {
        animateOffsetAsState(Offset(it.point.x, it.point.y), tween(if (it.pos == YARD) 350 else hopMs, easing = LinearEasing), label = "token")
    }
    val pulse by rememberInfiniteTransition(label = "pulse")
        .animateFloat(.9f, 1.3f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse")
    val groups = remember(tokens) { tokens.indices.groupBy { tokens[it].point }.values.toList() }

    Canvas(
        modifier.pointerInput(tokens, movable) {
            detectTapGestures { tap ->
                val u = size.width / BoardGeometry.SIZE
                tokens.filter { it.player == state.current && it.index in movable }
                    .map { it to hypot(tap.x - it.point.x * u, tap.y - it.point.y * u) }
                    .minByOrNull { it.second }?.takeIf { it.second < u * .9f }?.let { onTokenTap(it.first.index) }
            }
        },
    ) {
        val u = size.width / BoardGeometry.SIZE
        // draw back to front so nearer (lower) tokens overlap the ones behind them
        groups.sortedBy { anim[it.first()].value.y }.forEach { group ->
            group.forEachIndexed { i, ti ->
                val t = tokens[ti]
                val at = anim[ti].value * u + Offset(i * u * .12f, -i * u * .12f)
                if (t.player to t.index in flash) drawCircle(UiColor.White, u * .75f, at, style = Stroke(u * .15f))
                if (t.player == state.current && t.index in movable) {
                    drawCircle(Ludo.colors.accent, u * .47f, at, style = Stroke(u * .09f))
                    drawCircle(Ludo.colors.accent.copy(alpha = ((1.3f - pulse) / .4f).coerceIn(0f, 1f)), u * .5f * pulse, at, style = Stroke(u * .1f))
                }
                token(t.color, at, u, colorblind)
            }
            if (group.size > 1) {
                val at = anim[group.first()].value * u + Offset(u * .38f, -u * .38f) + Offset((group.size - 1) * u * .12f, -(group.size - 1) * u * .12f)
                drawCircle(Ludo.colors.ground, u * .22f, at)
                drawCircle(UiColor.White, u * .22f, at, style = Stroke(u * .04f))
                val layout = measurer.measure(group.size.toString(), TextStyle(UiColor.White, (u * .28f).toSp(), FontWeight.Bold))
                drawText(layout, topLeft = at - Offset(layout.size.width / 2f, layout.size.height / 2f))
            }
        }
    }
}

/** A round glossy token: drop shadow, white rim, shaded player color and a highlight (centered on [at]). */
private fun DrawScope.token(color: Color, at: Offset, u: Float, colorblind: Boolean) {
    drawCircle(UiColor(0x55000000), u * .42f, at + Offset(0f, u * .08f))
    drawCircle(UiColor.White, u * .4f, at)
    drawCircle(
        Brush.radialGradient(listOf(color.light, color.ui, color.dark), at - Offset(u * .1f, u * .12f), u * .42f),
        u * .32f, at,
    )
    if (colorblind) {
        shape(color, at, u * .2f, UiColor.Black)
        shape(color, at, u * .14f, UiColor.White)
    } else {
        drawCircle(UiColor.White.copy(alpha = .75f), u * .08f, at - Offset(u * .12f, u * .13f))
    }
}

/** Colorblind mode: circle (red), square (green), triangle (yellow), diamond (blue). Otherwise always a circle. */
private fun DrawScope.shape(color: Color, at: Offset, r: Float, fill: UiColor) {
    when (color) {
        Color.RED -> drawCircle(fill, r, at)
        Color.GREEN -> drawRect(fill, at - Offset(r * .85f, r * .85f), Size(r * 1.7f, r * 1.7f))
        Color.YELLOW -> drawPath(poly(at, r, 3, -PI / 2), fill)
        Color.BLUE -> drawPath(poly(at, r, 4, -PI / 2), fill)
    }
}

private fun poly(at: Offset, r: Float, n: Int, start: Double) = Path().apply {
    for (i in 0 until n) {
        val a = start + i * 2 * PI / n
        val x = at.x + (r * 1.1f * cos(a)).toFloat(); val y = at.y + (r * 1.1f * sin(a)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Hollow safe-square star: an outline only. */
private fun DrawScope.star(at: Offset, r: Float, color: UiColor) {
    val path = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) r else r * .45f
        val a = -PI / 2 + i * PI / 5
        val x = at.x + (rad * cos(a)).toFloat(); val y = at.y + (rad * sin(a)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, style = Stroke(r * .14f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
}
