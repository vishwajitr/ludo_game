package ludo.engine

import kotlin.random.Random
import kotlin.test.*

/** Plays one game with random legal moves, checking invariants every step. Returns final state. */
internal fun simulate(seed: Long, players: Int, rules: Rules, pick: (GameState, List<Move>, Random) -> Move = { _, m, r -> m[r.nextInt(m.size)] }): GameState {
    val dice = SeededDice(seed); val rnd = Random(seed)
    var s = newGame(defaultColors(players), rules)
    var steps = 0
    while (s.phase != Phase.GAME_OVER) {
        check(++steps < 100_000) { "game did not terminate (seed $seed)" }
        s = applyRoll(s, dice.roll()).state
        if (s.phase == Phase.CHOOSING_MOVE) {
            val moves = legalMoves(s)
            s = applyMove(s, pick(s, moves, rnd)).state
        }
        assertValid(s)
    }
    return s
}

private fun assertValid(s: GameState) {
    val onTrack = HashMap<Int, MutableSet<Color>>()
    s.players.forEach { p ->
        p.tokens.forEach { t ->
            assertTrue(t in YARD..HOME)
            absSquare(p.color, t)?.let { onTrack.getOrPut(it) { mutableSetOf() }.add(p.color) }
        }
    }
    onTrack.forEach { (sq, colors) -> assertTrue(sq in SAFE_SQUARES || colors.size == 1, "mixed colors on square $sq") }
    assertEquals(s.winners.size, s.winners.distinct().size)
}

class SimulationTest {
    @Test fun `bot games finish with a full ranking`() {
        for (n in 2..4) for (seed in 1L..20L) {
            val s = simulate(seed, n, Rules())
            assertEquals((0 until n).toSet(), s.winners.toSet())
            assertEquals(n, s.winners.size)
        }
    }

    @Test fun `1000 random games with random house rules`() {
        val r = Random(42)
        repeat(1000) { i ->
            val rules = Rules(r.nextBoolean(), r.nextBoolean(), r.nextBoolean(), r.nextBoolean())
            val n = 2 + r.nextInt(3)
            val s = simulate(i.toLong(), n, rules)
            assertEquals(n, s.winners.size, "seed $i rules $rules")
        }
    }
}
