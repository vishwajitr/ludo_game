package com.example.ludo.ui

import ludo.engine.*

/** Point in board units: the board is 15x15, cell (row, col) has its center at (col + .5, row + .5). */
data class P(val x: Float, val y: Float)

/**
 * Pure layout math. Everything is defined for the top-left seat (moving clockwise) and rotated
 * 90 degrees clockwise per seat for the others. Seats: Blue top-left, Red top-right,
 * Green bottom-right, Yellow bottom-left (the engine's turn order Red > Green > Yellow > Blue goes clockwise).
 */
object BoardGeometry {
    const val SIZE = 15

    private fun rotCell(rc: Pair<Int, Int>, k: Int): Pair<Int, Int> =
        (0 until k).fold(rc) { (r, c), _ -> c to (SIZE - 1 - r) }

    private fun rotPoint(p: P, k: Int): P = (0 until k).fold(p) { q, _ -> P(SIZE - q.y, q.x) }

    private fun center(rc: Pair<Int, Int>) = P(rc.second + .5f, rc.first + .5f)

    /** Quarter turns from the top-left seat: 0 = top-left, 1 = top-right, 2 = bottom-right, 3 = bottom-left. */
    fun seat(color: Color): Int = (color.ordinal + 1) % 4

    private val armTrack = listOf(
        6 to 1, 6 to 2, 6 to 3, 6 to 4, 6 to 5, 5 to 6, 4 to 6, 3 to 6, 2 to 6, 1 to 6, 0 to 6, 0 to 7, 0 to 8,
    )

    /** (row, col) of the 52 main-track squares by absolute index. */
    val track: List<Pair<Int, Int>> = Color.entries.flatMap { c -> armTrack.map { rotCell(it, seat(c)) } }

    /** Home-column cells (positions 51..55) for [color]. */
    fun homeColumn(color: Color): List<Pair<Int, Int>> = (1..5).map { rotCell(7 to it, seat(color)) }

    /** Top-left (row, col) of the 6x6 yard. */
    fun yardOrigin(color: Color): Pair<Int, Int> = when (seat(color)) {
        0 -> 0 to 0; 1 -> 0 to 9; 2 -> 9 to 9; else -> 9 to 0
    }

    /** Cell just outside the start of the home column, where the entry arrow is drawn. */
    fun arrowCell(color: Color): Pair<Int, Int> = rotCell(7 to 0, seat(color))

    fun yardSlot(color: Color, i: Int): P {
        val base = listOf(P(2f, 2f), P(4f, 2f), P(2f, 4f), P(4f, 4f))[i]
        return rotPoint(base, seat(color))
    }

    /** Where a token sits on the board (in board units). */
    fun tokenPoint(color: Color, pos: Int, index: Int): P = when {
        pos == YARD -> yardSlot(color, index)
        pos <= LAST_TRACK -> center(track[(color.start + pos) % TRACK_LEN])
        pos < HOME -> center(homeColumn(color)[pos - 51])
        else -> rotPoint(P(6.6f, 7.5f + (index - 1.5f) * .25f), seat(color))
    }
}
