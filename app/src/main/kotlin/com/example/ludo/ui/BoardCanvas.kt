package com.example.ludo.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.hypot
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as UiColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import ludo.engine.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val GridLine = UiColor(0xFF9E9E9E)

@Composable
fun BoardCanvas(
    state: GameState,
    modifier: Modifier = Modifier,
    movable: Set<Int> = emptySet(),
    flash: Set<Pair<Int, Int>> = emptySet(),
    hopMs: Int = 120,
    onTokenTap: (Int) -> Unit = {},
) {
    val measurer = rememberTextMeasurer()
    val active = state.players[state.current].color
    val tokens = state.players.flatMapIndexed { pi, p ->
        p.tokens.mapIndexed { ti, pos -> Tk(pi, ti, p.color, pos, BoardGeometry.tokenPoint(p.color, pos, ti)) }
    }
    // hop between squares, slide faster/slower back to the yard
    val anim = tokens.map {
        animateOffsetAsState(Offset(it.point.x, it.point.y), tween(if (it.pos == YARD) 350 else hopMs, easing = LinearEasing), label = "token")
    }
    val pulse by rememberInfiniteTransition(label = "pulse")
        .animateFloat(.9f, 1.3f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse")
    Canvas(
        modifier.aspectRatio(1f)
            .semantics { contentDescription = "Ludo board, ${active.label}'s turn" }
            .pointerInput(tokens, movable) {
                detectTapGestures { tap ->
                    val u = size.width / BoardGeometry.SIZE
                    tokens.filter { it.player == state.current && it.index in movable }
                        .map { it to hypot(tap.x - it.point.x * u, tap.y - it.point.y * u) }
                        .minByOrNull { it.second }?.takeIf { it.second < u * .75f }?.let { onTokenTap(it.first.index) }
                }
            },
    ) {
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
            repeat(4) { drawCircle(color.ui.copy(alpha = .35f), u * .6f, px(BoardGeometry.yardSlot(color, it))) }
            if (color == active) // highlight the active player's yard
                drawRect(UiColor.Black, Offset(c0 * u, r0 * u), Size(6 * u, 6 * u), style = Stroke(u * .2f))
        }

        BoardGeometry.track.forEachIndexed { abs, rc ->
            val startOf = Color.entries.firstOrNull { it.start == abs }
            cell(rc, startOf?.ui ?: UiColor.White)
            if (abs in SAFE_SQUARES && startOf == null) star(px(P(rc.second + .5f, rc.first + .5f)), u * .35f, GridLine)
        }
        for (color in Color.entries) BoardGeometry.homeColumn(color).forEach { cell(it, color.ui) }

        // home triangles
        val c = px(P(7.5f, 7.5f))
        fun tri(a: P, b: P, color: UiColor) = drawPath(
            Path().apply { moveTo(px(a).x, px(a).y); lineTo(px(b).x, px(b).y); lineTo(c.x, c.y); close() }, color,
        )
        tri(P(6f, 6f), P(6f, 9f), Color.RED.ui)
        tri(P(6f, 6f), P(9f, 6f), Color.GREEN.ui)
        tri(P(9f, 6f), P(9f, 9f), Color.YELLOW.ui)
        tri(P(6f, 9f), P(9f, 9f), Color.BLUE.ui)

        // tokens, stacked ones offset with a count badge
        val index = tokens.withIndex().associate { (i, t) -> t to i }
        tokens.groupBy { it.point }.forEach { (_, group) ->
            group.forEachIndexed { i, t ->
                val at = Offset(anim[index.getValue(t)].value.x * u + i * u * .12f, anim[index.getValue(t)].value.y * u - i * u * .12f)
                if (t.player to t.index in flash) drawCircle(UiColor(0xFFFF6F00), u * .9f, at, style = Stroke(u * .15f))
                if (t.player == state.current && t.index in movable)
                    drawCircle(UiColor.White, u * .36f * pulse * 1.25f, at, style = Stroke(u * .12f))
                token(t.color.ui, at, u * .36f)
            }
            if (group.size > 1) {
                val base = group.first()
                val at = anim[index.getValue(base)].value * u + Offset(u * .3f, -u * .3f)
                drawCircle(UiColor.Black, u * .2f, at)
                val layout = measurer.measure(group.size.toString(), TextStyle(UiColor.White, (u * .28f).toSp(), FontWeight.Bold))
                drawText(layout, topLeft = at - Offset(layout.size.width / 2f, layout.size.height / 2f))
            }
        }
    }
}

private data class Tk(val player: Int, val index: Int, val color: Color, val pos: Int, val point: P)

private fun DrawScope.token(color: UiColor, at: Offset, r: Float) {
    drawCircle(UiColor.Black, r, at)
    drawCircle(color, r * .85f, at)
    drawCircle(UiColor.White.copy(alpha = .45f), r * .35f, at - Offset(r * .25f, r * .25f))
}

private fun DrawScope.star(at: Offset, r: Float, color: UiColor) {
    val path = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) r else r * .45f
        val a = -PI / 2 + i * PI / 5
        val x = at.x + (rad * cos(a)).toFloat(); val y = at.y + (rad * sin(a)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
