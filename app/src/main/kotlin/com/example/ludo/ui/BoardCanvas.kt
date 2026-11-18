package com.example.ludo.ui

import androidx.compose.foundation.Canvas
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
fun BoardCanvas(state: GameState, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val active = state.players[state.current].color
    Canvas(modifier.aspectRatio(1f).semantics { contentDescription = "Ludo board, ${active.label}'s turn" }) {
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
        val tokens = state.players.flatMap { p -> p.tokens.mapIndexed { i, pos -> Triple(p.color, pos, BoardGeometry.tokenPoint(p.color, pos, i)) } }
        tokens.groupBy { it.third }.forEach { (point, group) ->
            group.forEachIndexed { i, (color, _, _) ->
                val o = Offset(i * u * .12f, -i * u * .12f)
                token(color.ui, px(point) + o, u * .36f)
            }
            if (group.size > 1) {
                val at = px(point) + Offset(u * .3f, -u * .3f)
                drawCircle(UiColor.Black, u * .2f, at)
                val layout = measurer.measure(group.size.toString(), TextStyle(UiColor.White, (u * .28f).toSp(), FontWeight.Bold))
                drawText(layout, topLeft = at - Offset(layout.size.width / 2f, layout.size.height / 2f))
            }
        }
    }
}

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
