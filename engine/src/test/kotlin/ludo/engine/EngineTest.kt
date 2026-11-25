package ludo.engine

import kotlin.test.*

/** Players take the first n colors; [pos] lists each player's four token positions. */
private fun game(vararg pos: List<Int>, rules: Rules = Rules(), captured: Boolean = false) =
    GameState(pos.mapIndexed { i, t -> Player(Color.entries[i], t, captured) }, rules = rules)

private val Y = YARD
private fun roll(s: GameState, v: Int) = applyRoll(s, v)
private fun play(s: GameState, v: Int, from: Int): MoveResult {
    val r = roll(s, v)
    return applyMove(r.state, legalMoves(r.state).first { it.from == from })
}

class EngineTest {
    private val idle = listOf(Y, Y, Y, Y)

    @Test fun `yard exit only on six`() {
        val s = game(idle, idle)
        val r = roll(s, 5)
        assertTrue(GameEvent.TurnForfeited(0, ForfeitReason.NO_LEGAL_MOVE) in r.events)
        assertEquals(1, r.state.current)
        val six = roll(s, 6)
        assertEquals(Phase.CHOOSING_MOVE, six.state.phase)
        assertEquals(0, applyMove(six.state, legalMoves(six.state).single()).state.players[0].tokens[0])
    }

    @Test fun `six gives extra roll`() {
        val r = play(game(listOf(10, Y, Y, Y), idle), 6, 10)
        assertEquals(0, r.state.current)
        assertEquals(Phase.ROLLING, r.state.phase)
        assertTrue(GameEvent.ExtraTurn(0) in r.events)
        assertEquals(1, r.state.consecutiveSixes)
    }

    @Test fun `third six forfeits the turn`() {
        val s = game(listOf(10, Y, Y, Y), idle).copy(consecutiveSixes = 2)
        val r = roll(s, 6)
        assertTrue(GameEvent.TurnForfeited(0, ForfeitReason.TRIPLE_SIX) in r.events)
        assertEquals(1, r.state.current)
        assertEquals(10, r.state.players[0].tokens[0]) // third move not made
    }

    @Test fun `non-six resets six streak and passes turn`() {
        val r = play(game(listOf(10, Y, Y, Y), idle).copy(consecutiveSixes = 2), 3, 10)
        assertEquals(1, r.state.current)
        assertEquals(0, r.state.consecutiveSixes)
    }

    @Test fun `capture sends token home and grants extra roll`() {
        // red at 5 + 4 -> abs 9; green progress 48 -> abs (13+48)%52 = 9 (not safe)
        val r = play(game(listOf(5, Y, Y, Y), listOf(48, Y, Y, Y)), 4, 5)
        assertEquals(Y, r.state.players[1].tokens[0])
        assertTrue(r.events.any { it is GameEvent.Captured && it.victim == 1 })
        assertEquals(0, r.state.current)
        assertTrue(r.state.players[0].hasCaptured)
    }

    @Test fun `no capture on safe squares`() {
        // abs 8 is a star; abs 13 is green's start
        val star = play(game(listOf(4, Y, Y, Y), listOf(47, Y, Y, Y)), 4, 4)
        assertEquals(47, star.state.players[1].tokens[0])
        val start = play(game(listOf(9, Y, Y, Y), listOf(0, Y, Y, Y)), 4, 9)
        assertEquals(0, start.state.players[1].tokens[0])
        assertEquals(1, start.state.current) // no capture, no extra
    }

    @Test fun `exact roll needed for home, home grants extra roll`() {
        val s = game(listOf(53, Y, Y, Y), idle)
        assertEquals(Phase.ROLLING, roll(s, 4).state.phase) // overshoot -> forfeit
        assertEquals(1, roll(s, 4).state.current)
        val r = play(s, 3, 53)
        assertEquals(HOME, r.state.players[0].tokens[0])
        assertTrue(GameEvent.ReachedHome(0, 0) in r.events)
        assertEquals(0, r.state.current)
    }

    @Test fun `home column is isolated`() {
        // green token in its home column (52) is not hit by red entering its own column square 52
        val r = play(game(listOf(50, Y, Y, Y), listOf(52, Y, Y, Y)), 2, 50)
        assertEquals(52, r.state.players[1].tokens[0])
        assertEquals(1, r.state.current)
    }

    @Test fun `no legal move skips turn`() {
        val r = roll(game(listOf(55, 55, 55, 55), idle), 3)
        assertEquals(1, r.state.current)
        assertEquals(Phase.ROLLING, r.state.phase)
    }

    @Test fun `blockade house rule blocks passing and landing`() {
        val twoGreen = listOf(48, 48, Y, Y) // abs 9
        val off = game(listOf(5, Y, Y, Y), twoGreen)
        val on = off.copy(rules = Rules(blockades = true))
        assertEquals(1, legalMoves(roll(off, 4).state).size) // capture both
        assertEquals(Phase.ROLLING, roll(on, 4).state.phase) // landing blocked -> forfeit
        assertEquals(Phase.ROLLING, roll(on, 5).state.phase) // passing blocked -> forfeit
        assertEquals(Phase.CHOOSING_MOVE, roll(on, 3).state.phase)
    }

    @Test fun `must capture to enter home column`() {
        val rules = Rules(mustCaptureToEnterHome = true)
        val s = game(listOf(50, 0, Y, Y), idle, rules = rules)
        assertEquals(listOf(0), legalMoves(roll(s, 1).state).map { it.from })
        val c = s.copy(players = s.players.map { it.copy(hasCaptured = true) })
        assertEquals(setOf(50, 0), legalMoves(roll(c, 1).state).map { it.from }.toSet())
        // waived when it is the only possible move (prevents deadlock)
        val only = game(listOf(50, Y, Y, Y), idle, rules = rules)
        assertEquals(listOf(50), legalMoves(roll(only, 1).state).map { it.from })
    }

    @Test fun `start with one token out`() {
        val s = newGame(defaultColors(4), Rules(startWithOneOut = true))
        assertTrue(s.players.all { it.tokens == listOf(0, Y, Y, Y) })
    }

    @Test fun `any roll leaves yard`() {
        val r = roll(game(idle, idle, rules = Rules(anyRollToLeave = true)), 3)
        assertEquals(Phase.CHOOSING_MOVE, r.state.phase)
        assertEquals(3, applyMove(r.state, legalMoves(r.state).first()).state.players[0].tokens.count { it == Y }) // token placed on start
        assertEquals(Phase.ROLLING, roll(game(idle, idle), 3).state.phase) // classic: 3 can't leave
    }

    @Test fun `one or six to leave yard`() {
        val s = game(idle, idle, rules = Rules(oneOrSixToLeave = true))
        val r = roll(s, 1)
        assertEquals(Phase.CHOOSING_MOVE, r.state.phase)
        assertEquals(1, applyMove(r.state, legalMoves(r.state).single()).state.current) // 1 gives no extra roll
        assertEquals(Phase.ROLLING, roll(game(idle, idle), 1).state.phase)
    }

    @Test fun `finishing ranks players and ends game`() {
        val r = play(game(listOf(HOME, HOME, HOME, 55), listOf(10, Y, Y, Y)), 1, 55)
        assertEquals(Phase.GAME_OVER, r.state.phase)
        assertEquals(listOf(0, 1), r.state.winners)
        assertTrue(r.events.any { it is GameEvent.GameOver })
    }

    @Test fun `seeded dice are reproducible`() {
        assertEquals(List(20) { SeededDice(7).roll() }.size, 20)
        val a = SeededDice(7); val b = SeededDice(7)
        repeat(50) { assertEquals(a.roll(), b.roll()) }
    }
}
