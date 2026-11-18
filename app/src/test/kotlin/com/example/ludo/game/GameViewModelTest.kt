package com.example.ludo.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import ludo.engine.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vm(vararg rolls: Int, auto: Boolean = true, start: GameState = newGame(defaultColors(2))): GameViewModel {
        val it = rolls.iterator()
        return GameViewModel(start, { it.next() }, auto)
    }

    @Test fun `no legal move passes the turn after showing a message`() = runTest {
        val m = vm(3)
        m.onRoll(); runCurrent()
        assertTrue(m.state.value.rolling || m.state.value.busy)
        advanceUntilIdle()
        assertEquals(1, m.state.value.game.current)
        assertFalse(m.state.value.busy)
        assertEquals(3, m.state.value.diceValue)
    }

    @Test fun `six lets the player choose then keeps the turn`() = runTest {
        val m = vm(6, auto = false)
        m.onRoll(); advanceUntilIdle()
        assertEquals(Phase.CHOOSING_MOVE, m.state.value.game.phase)
        m.onTokenTap(m.state.value.movable.min()); advanceUntilIdle()
        val s = m.state.value
        assertEquals(0, s.game.current) // six = extra roll
        assertEquals(Phase.ROLLING, s.game.phase)
        assertEquals(0, s.game.players[0].tokens.max())
        assertEquals(s.game.players.map { it.tokens }, s.shown)
    }

    @Test fun `single legal move is auto-played`() = runTest {
        val m = vm(6)
        m.onRoll(); advanceUntilIdle()
        assertEquals(0, m.state.value.game.players[0].tokens.max())
        assertEquals(Phase.ROLLING, m.state.value.game.phase)
    }

    @Test fun `taps and rolls are ignored while busy or not movable`() = runTest {
        val m = vm(6, 6, auto = false)
        m.onRoll(); m.onRoll(); advanceUntilIdle() // second roll ignored (busy)
        assertEquals(1, m.state.value.game.consecutiveSixes)
        m.onTokenTap(3) // not a movable token
        assertEquals(Phase.CHOOSING_MOVE, m.state.value.game.phase)
        m.onRoll() // can't roll while choosing
        advanceUntilIdle()
        assertEquals(Phase.CHOOSING_MOVE, m.state.value.game.phase)
    }

    @Test fun `tokens hop one square at a time`() = runTest {
        val s = newGame(defaultColors(2)).let { g -> g.copy(players = g.players.mapIndexed { i, p -> if (i == 0) p.copy(tokens = listOf(10, YARD, YARD, YARD)) else p }) }
        val m = vm(3, start = s)
        m.onRoll()
        advanceTimeBy(600 + 350 + 60) // rolled, auto-move started, first hop shown
        assertEquals(11, m.state.value.shown[0][0])
        advanceUntilIdle()
        assertEquals(13, m.state.value.shown[0][0])
    }
}
