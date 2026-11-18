package com.example.ludo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.example.ludo.game.GameViewModel
import com.example.ludo.ui.GameScreen
import ludo.engine.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class GameFlowTest {
    @get:Rule val rule = createComposeRule()

    /** 2-player game: roll a 6, tap a red token in the yard, it moves to the start square. */
    @Test fun rollAndMoveToken() {
        val seats = listOf(Seat("Ann"), Seat("Bob"))
        val vm = GameViewModel(newGame(defaultColors(2)), seats, dice = { 6 }, autoMove = false)
        rule.setContent { GameScreen(vm, haptics = false, onQuit = {}, onRestart = {}, onFinished = {}) }

        rule.onNodeWithText("Ann's turn").assertExists()
        rule.onNodeWithContentDescription("Dice", substring = true).performClick()
        rule.waitUntil(5_000) { vm.state.value.movable.isNotEmpty() }

        // red's first yard slot is at (2, 2) in 15x15 board units
        rule.onNodeWithContentDescription("Ludo board", substring = true).performTouchInput {
            val u = width / 15f
            click(Offset(2 * u, 2 * u))
        }
        rule.waitUntil(5_000) { vm.state.value.game.players[0].tokens.contains(0) }
        assertEquals(1L, vm.state.value.game.players[0].tokens.count { it == 0 }.toLong())
        assertTrue(vm.state.value.game.current == 0) // a six earns another roll
    }
}
