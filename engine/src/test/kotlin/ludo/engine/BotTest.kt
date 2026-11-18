package ludo.engine

import kotlin.random.Random
import kotlin.test.*

class BotTest {
    private fun choosing(a: List<Int>, b: List<Int>, dice: Int, rules: Rules = Rules()): GameState {
        val s = GameState(listOf(Player(Color.RED, a), Player(Color.GREEN, b)), rules = rules)
        return applyRoll(s, dice).state
    }
    private val idle = listOf(YARD, YARD, YARD, YARD)

    @Test fun `medium prefers capture over home over leaving yard`() {
        // red 5 -> 9 captures green (progress 48 = abs 9); red 53 -> 56 is home; red yard leaves only on 6
        val capture = choosing(listOf(5, 20, YARD, YARD), listOf(48, YARD, YARD, YARD), 4)
        assertEquals(5, MediumBot.choose(capture, legalMoves(capture)).from)
        val home = choosing(listOf(30, 53, YARD, YARD), idle, 3)
        assertEquals(53, MediumBot.choose(home, legalMoves(home)).from)
        val leave = choosing(listOf(30, YARD, YARD, YARD), idle, 6)
        assertEquals(YARD, MediumBot.choose(leave, legalMoves(leave)).from)
    }

    @Test fun `medium escapes danger and otherwise advances furthest`() {
        // green token at abs 7 sits 3 behind red's rear token (abs 10); moving it 5 escapes, moving the lead token doesn't
        val danger = choosing(listOf(10, 30, YARD, YARD), listOf(46, YARD, YARD, YARD), 5)
        assertEquals(10, MediumBot.choose(danger, legalMoves(danger)).from)
        val far = choosing(listOf(10, 30, YARD, YARD), idle, 2)
        assertEquals(30, MediumBot.choose(far, legalMoves(far)).from)
    }

    @Test fun `hard captures and avoids exposed landing`() {
        val capture = choosing(listOf(5, 20, YARD, YARD), listOf(48, YARD, YARD, YARD), 4)
        assertEquals(5, HardBot.choose(capture, legalMoves(capture)).from)
        // token A (30) could land at 34 (star, safe) vs token B (10) landing unsafe at 14 right in front of a green token at abs 12
        val s = choosing(listOf(30, 10, YARD, YARD), listOf(51, YARD, YARD, YARD), 4)
        assertEquals(30, HardBot.choose(s, legalMoves(s)).from)
    }

    private fun wins(a: Difficulty, b: Difficulty, games: Int): Int {
        var aWins = 0
        repeat(games) { g ->
            val bots = listOf(botFor(a, Random(g)), botFor(b, Random(g)))
            // alternate who sits first so seat order doesn't decide
            val order = if (g % 2 == 0) listOf(0, 1) else listOf(1, 0)
            val dice = SeededDice(g.toLong())
            var s = newGame(defaultColors(2))
            var guard = 0
            while (s.phase != Phase.GAME_OVER && ++guard < 100_000) {
                s = applyRoll(s, dice.roll()).state
                if (s.phase == Phase.CHOOSING_MOVE) {
                    val moves = legalMoves(s)
                    s = applyMove(s, bots[order[s.current]].choose(s, moves)).state
                }
            }
            assertEquals(Phase.GAME_OVER, s.phase)
            if (order[s.winners.first()] == 0) aWins++
        }
        return aWins
    }

    @Test fun `stronger bots beat weaker ones`() {
        val n = 200
        assertTrue(wins(Difficulty.MEDIUM, Difficulty.EASY, n) > n * .6, "medium vs easy")
        assertTrue(wins(Difficulty.HARD, Difficulty.EASY, n) > n * .6, "hard vs easy")
        assertTrue(wins(Difficulty.HARD, Difficulty.MEDIUM, n) > n * .5, "hard vs medium")
    }

    @Test fun `mixed four-bot games always finish with a full ranking`() {
        for (seed in 1L..40L) {
            val diffs = Difficulty.entries
            val bots = (0..3).map { botFor(diffs[((seed + it) % 3).toInt()], Random(seed + it)) }
            val dice = SeededDice(seed)
            var s = newGame(defaultColors(4), Rules(blockades = seed % 2 == 0L))
            var guard = 0
            while (s.phase != Phase.GAME_OVER) {
                check(++guard < 100_000) { "stalled, seed $seed" }
                s = applyRoll(s, dice.roll()).state
                if (s.phase == Phase.CHOOSING_MOVE) s = applyMove(s, bots[s.current].choose(s, legalMoves(s))).state
            }
            assertEquals(setOf(0, 1, 2, 3), s.winners.toSet())
        }
    }
}
