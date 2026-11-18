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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as UiColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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

private val GridLine = UiColor(0xFF9A9A9A)
private val RingBrown = UiColor(0xFF7A2E2A)
private val Frame = UiColor(0xFF12305F)

/** Token pins stand about this many cells above their base ring. */
private const val PIN_MID = .55f

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
    Box(modifier.aspectRatio(1f).background(Frame, RoundedCornerShape(10.dp)).padding(6.dp).semantics { contentDescription = "Ludo board, ${active.label}'s turn" }) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            BoardBackground(Modifier.fillMaxSize().graphicsLayer(), active, colorblind) // static: cached, not redrawn per animation frame
            TokenLayer(state, tokens, movable, flash, hopMs, colorblind, onTokenTap, Modifier.fillMaxSize())
            // accessibility: one node per token, 48dp focus area, actionable when it can move (touches are handled by the canvas)
            val u = maxWidth / BoardGeometry.SIZE
            for (t in tokens) {
                val canMove = t.player == state.current && t.index in movable
                Box(
                    Modifier.offset(u * t.point.x - 24.dp, u * (t.point.y - PIN_MID) - 24.dp).size(48.dp).semantics {
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
        val u = size.width / BoardGeometry.SIZE
        fun px(p: P) = Offset(p.x * u, p.y * u)
        fun cell(rc: Pair<Int, Int>, fill: UiColor) {
            val tl = Offset(rc.second * u, rc.first * u)
            drawRect(fill, tl, Size(u, u))
            drawRect(GridLine, tl, Size(u, u), style = Stroke(1f))
        }

        for (color in Color.entries) {
            val (r0, c0) = BoardGeometry.yardOrigin(color)
            drawRect(color.ui, Offset(c0 * u, r0 * u), Size(6 * u, 6 * u))
            drawRect(UiColor.White, Offset((c0 + 1) * u, (r0 + 1) * u), Size(4 * u, 4 * u))
            repeat(4) { drawCircle(color.ui, u * .6f, px(BoardGeometry.yardSlot(color, it))) }
            if (colorblind) shape(color, px(P(c0 + 3f, r0 + 3f)), u * .5f, color.ui.copy(alpha = .35f)) // pattern marker in each yard
            if (color == active) // highlight the active player's yard
                drawRect(UiColor.Black, Offset(c0 * u, r0 * u), Size(6 * u, 6 * u), style = Stroke(u * .2f))
        }

        BoardGeometry.track.forEachIndexed { abs, rc ->
            val startOf = Color.entries.firstOrNull { it.start == abs }
            cell(rc, startOf?.ui ?: UiColor.White)
            if (abs in SAFE_SQUARES && startOf == null) star(px(P(rc.second + .5f, rc.first + .5f)), u * .36f)
        }
        for (color in Color.entries) {
            BoardGeometry.homeColumn(color).forEach { cell(it, color.ui) }
            // arrow outside the home column, pointing into it
            val a = BoardGeometry.arrowCell(color)
            withTransform({ translate((a.second + .5f) * u, (a.first + .5f) * u); rotate(BoardGeometry.seat(color) * 90f, Offset.Zero); scale(u, u, Offset.Zero) }) {
                drawLine(color.ui, Offset(-.3f, 0f), Offset(.25f, 0f), .09f, StrokeCap.Round)
                drawLine(color.ui, Offset(.25f, 0f), Offset(.05f, -.2f), .09f, StrokeCap.Round)
                drawLine(color.ui, Offset(.25f, 0f), Offset(.05f, .2f), .09f, StrokeCap.Round)
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
                // the pin's body sits above its base, so measure the tap against the middle of the pin
                tokens.filter { it.player == state.current && it.index in movable }
                    .map { it to hypot(tap.x - it.point.x * u, tap.y - (it.point.y - PIN_MID) * u) }
                    .minByOrNull { it.second }?.takeIf { it.second < u * .9f }?.let { onTokenTap(it.first.index) }
            }
        },
    ) {
        val u = size.width / BoardGeometry.SIZE
        // draw back to front so nearer (lower) pins overlap the ones behind them
        groups.sortedBy { anim[it.first()].value.y }.forEach { group ->
            group.forEachIndexed { i, ti ->
                val t = tokens[ti]
                val at = anim[ti].value * u + Offset(i * u * .12f, -i * u * .12f)
                if (t.player to t.index in flash) drawCircle(UiColor(0xFFFF6F00), u * .9f, at, style = Stroke(u * .15f))
                if (t.player == state.current && t.index in movable)
                    drawCircle(UiColor.White, u * .4f * pulse, at, style = Stroke(u * .12f))
                pin(t.color, at, u, colorblind)
            }
            if (group.size > 1) {
                val at = anim[group.first()].value * u + Offset(u * .4f, -u * 1.35f)
                drawCircle(UiColor.Black, u * .2f, at)
                val layout = measurer.measure(group.size.toString(), TextStyle(UiColor.White, (u * .28f).toSp(), FontWeight.Bold))
                drawText(layout, topLeft = at - Offset(layout.size.width / 2f, layout.size.height / 2f))
            }
        }
    }
}

// Map-pin outline in unit space: tip at (0,0), round head centered (0,-1), radius .48.
private val PinPath: Path = run {
    val head = Path().apply { addOval(Rect(Offset(0f, -1f), .48f)) }
    val point = Path().apply { moveTo(0f, 0f); lineTo(-.421f, -.77f); lineTo(.421f, -.77f); close() }
    Path.combine(PathOperation.Union, head, point)
}
private val PinFill = Brush.verticalGradient(listOf(UiColor.White, UiColor(0xFFB8B8B8)), startY = -1.5f, endY = 0f)

/** A silver map pin with a glossy colored ball, standing on a dark ring (the base point is [at]). */
private fun DrawScope.pin(color: Color, at: Offset, u: Float, colorblind: Boolean) {
    drawCircle(UiColor(0x33000000), u * .42f, at)
    drawCircle(RingBrown, u * .38f, at, style = Stroke(u * .09f))
    withTransform({ translate(at.x, at.y); scale(u, u, Offset.Zero) }) {
        drawPath(PinPath, PinFill)
        drawPath(PinPath, UiColor(0x66000000), style = Stroke(.03f))
        val head = Offset(0f, -1f)
        if (colorblind) {
            shape(color, head, .34f, UiColor.Black)
            shape(color, head, .27f, color.ui)
        } else {
            drawCircle(Brush.radialGradient(listOf(lerp(color.ui, UiColor.White, .55f), color.ui), head + Offset(-.1f, -.1f), .4f), .32f, head)
            drawCircle(UiColor(0x66000000), .32f, head, style = Stroke(.025f))
        }
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

/** Hollow safe-square star: white with a dark outline. */
private fun DrawScope.star(at: Offset, r: Float) {
    val path = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) r else r * .45f
        val a = -PI / 2 + i * PI / 5
        val x = at.x + (rad * cos(a)).toFloat(); val y = at.y + (rad * sin(a)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, UiColor.White)
    drawPath(path, UiColor(0xFF444444), style = Stroke(r * .12f))
}
