package ludo.engine

import kotlin.test.*

class LeaderboardTest {
    private fun rec(t: Long, vararg p: Pair<String, Difficulty?>) = GameRecord(t, p.mapIndexed { i, (n, b) -> RecordPlayer(n, b, i + 1) })

    private val records = listOf(
        rec(1, "Ann" to null, "Computer" to Difficulty.HARD),
        rec(2, "Bob" to null, "ann" to null),               // same human as "Ann", different case
        rec(3, "Ann" to null, "Computer" to Difficulty.EASY, "Bob" to null),
    )

    @Test fun `humans are merged by name and ranked by wins`() {
        val s = leaderboard(records, includeBots = false)
        assertEquals(listOf("Ann", "Bob"), s.map { it.name })
        assertEquals(listOf(3 to 2, 2 to 1), s.map { it.games to it.wins })
        assertEquals(66, s[0].winPercent)
    }

    @Test fun `bots are separate per difficulty and optional`() {
        val s = leaderboard(records, includeBots = true)
        assertEquals(4, s.size)
        assertTrue(s.any { it.name == "Computer" && it.bot == Difficulty.HARD && it.games == 1 && it.wins == 0 })
        assertTrue(s.any { it.name == "Computer" && it.bot == Difficulty.EASY })
    }

    @Test fun `record of a finished game follows winners order`() {
        val state = newGame(defaultColors(3)).copy(winners = listOf(2, 0, 1), phase = Phase.GAME_OVER)
        val r = recordOf(state, listOf(Seat("A"), Seat("B", Difficulty.HARD), Seat("C")), 9)
        assertEquals(listOf("C" to 1, "A" to 2, "B" to 3), r.players.map { it.name to it.rank })
    }

    @Test fun `codec round trips and skips bad lines`() {
        val odd = rec(5, "Zoë; /:,\n" to null, "Bot" to Difficulty.MEDIUM)
        val text = HistoryCodec.encode(records + odd) + "\ngarbage\n1;x:y"
        assertEquals(records + odd, HistoryCodec.decode(text))
        assertEquals(emptyList(), HistoryCodec.decode(""))
    }
}
