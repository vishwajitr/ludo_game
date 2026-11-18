package com.example.ludo.ui

import ludo.engine.*
import kotlin.math.abs
import kotlin.test.*

class BoardGeometryTest {
    private val track = BoardGeometry.track

    @Test fun `52 distinct track squares forming a loop`() {
        assertEquals(52, track.toSet().size)
        for (i in 0 until 52) {
            val (r1, c1) = track[i]; val (r2, c2) = track[(i + 1) % 52]
            assertEquals(1, maxOf(abs(r1 - r2), abs(c1 - c2)), "gap after square $i") // corners are diagonal
        }
    }

    @Test fun `start squares and stars are where Ludo puts them`() {
        assertEquals(6 to 1, track[Color.RED.start])
        assertEquals(1 to 8, track[Color.GREEN.start])
        assertEquals(8 to 13, track[Color.YELLOW.start])
        assertEquals(13 to 6, track[Color.BLUE.start])
        assertEquals(2 to 6, track[8])
    }

    @Test fun `home column continues from the last track square`() {
        for (c in Color.entries) {
            val (r1, c1) = track[(c.start + LAST_TRACK) % 52]; val (r2, c2) = BoardGeometry.homeColumn(c).first()
            assertEquals(1, abs(r1 - r2) + abs(c1 - c2))
            assertTrue(BoardGeometry.homeColumn(c).none { it in track })
        }
    }
}
