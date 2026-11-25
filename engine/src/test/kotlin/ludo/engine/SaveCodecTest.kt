package ludo.engine

import kotlin.test.*

class SaveCodecTest {
    @Test fun `round trips a mid-game state with awkward names`() {
        var s = newGame(defaultColors(3), Rules(blockades = true, oneOrSixToLeave = true, anyRollToLeave = true))
        val dice = SeededDice(5)
        repeat(60) {
            s = applyRoll(s, dice.roll()).state
            if (s.phase == Phase.CHOOSING_MOVE) s = applyMove(s, legalMoves(s).first()).state
        }
        val saved = SavedGame(s, listOf(Seat("Ann; /:,é"), Seat("Bot", Difficulty.HARD), Seat("")))
        assertEquals(saved, SaveCodec.decode(SaveCodec.encode(saved)))
    }

    @Test fun `round trips winners between turns`() {
        val s = newGame(defaultColors(3)).copy(winners = listOf(1), current = 2)
        val saved = SavedGame(s, listOf(Seat("a"), Seat("b", Difficulty.EASY), Seat("c")))
        assertEquals(saved, SaveCodec.decode(SaveCodec.encode(saved)))
    }

    @Test fun `tampered states are rejected`() {
        val seats = listOf(Seat("a"), Seat("b"), Seat("c"))
        val ok = newGame(defaultColors(3))
        fun bad(s: GameState) = assertNull(SaveCodec.decode(SaveCodec.encode(SavedGame(s, seats))))
        bad(ok.copy(winners = listOf(0, 1, 2)))                      // everyone finished: used to hang the turn rotation
        bad(ok.copy(winners = listOf(0, 1)))                         // only one player left means the game is over
        bad(ok.copy(winners = listOf(0, 0)))                         // duplicate winner
        bad(ok.copy(winners = listOf(0), current = 0))               // current player already finished
        bad(ok.copy(phase = Phase.GAME_OVER))                        // not a between-turns state
        bad(ok.copy(phase = Phase.CHOOSING_MOVE, dice = 3))
        bad(ok.copy(consecutiveSixes = 7))
        bad(ok.copy(players = ok.players.map { it.copy(color = Color.RED) }))
    }

    @Test fun `malformed input decodes to null`() {
        for (bad in listOf("", "garbage", "v1;0000;0;0;0;ROLLING;;RED:99,9,9,9:0/GREEN:-1,-1,-1,-1:0;a:-/b:-", "v2;0;0;0;0;ROLLING;;;"))
            assertNull(SaveCodec.decode(bad), bad)
        val ok = SaveCodec.encode(SavedGame(newGame(defaultColors(2)), listOf(Seat("a"), Seat("b"))))
        assertNull(SaveCodec.decode(ok.replace("RED", "PINK")))
        assertNull(SaveCodec.decode(ok.dropLast(8)))
    }
}
