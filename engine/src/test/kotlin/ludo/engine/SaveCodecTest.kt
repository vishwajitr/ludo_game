package ludo.engine

import kotlin.test.*

class SaveCodecTest {
    @Test fun `round trips a mid-game state with awkward names`() {
        var s = newGame(defaultColors(3), Rules(blockades = true, oneOrSixToLeave = true))
        val dice = SeededDice(5)
        repeat(60) {
            s = applyRoll(s, dice.roll()).state
            if (s.phase == Phase.CHOOSING_MOVE) s = applyMove(s, legalMoves(s).first()).state
        }
        val saved = SavedGame(s, listOf(Seat("Ann; /:,é"), Seat("Bot", Difficulty.HARD), Seat("")))
        assertEquals(saved, SaveCodec.decode(SaveCodec.encode(saved)))
    }

    @Test fun `round trips winners and game over`() {
        val s = newGame(defaultColors(2)).copy(winners = listOf(1, 0), phase = Phase.GAME_OVER)
        val saved = SavedGame(s, listOf(Seat("a"), Seat("b", Difficulty.EASY)))
        assertEquals(saved, SaveCodec.decode(SaveCodec.encode(saved)))
    }

    @Test fun `malformed input decodes to null`() {
        for (bad in listOf("", "garbage", "v1;0000;0;0;0;ROLLING;;RED:99,9,9,9:0/GREEN:-1,-1,-1,-1:0;a:-/b:-", "v2;0;0;0;0;ROLLING;;;"))
            assertNull(SaveCodec.decode(bad), bad)
        val ok = SaveCodec.encode(SavedGame(newGame(defaultColors(2)), listOf(Seat("a"), Seat("b"))))
        assertNull(SaveCodec.decode(ok.replace("RED", "PINK")))
        assertNull(SaveCodec.decode(ok.dropLast(8)))
    }
}
