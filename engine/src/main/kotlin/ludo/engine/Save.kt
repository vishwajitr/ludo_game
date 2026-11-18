package ludo.engine

import java.net.URLDecoder
import java.net.URLEncoder

/** A seat at the table: a human, or a bot of the given difficulty. */
data class Seat(val name: String, val bot: Difficulty? = null)

data class SavedGame(val state: GameState, val seats: List<Seat>)

/**
 * Compact text format, e.g. `v1;0101;0;0;0;ROLLING;;RED:0,-1,-1,-1:0/GREEN:...;Ann:-/Bot:HARD`.
 * Plain text keeps :engine dependency-free; decode returns null for anything malformed.
 */
object SaveCodec {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun dec(s: String) = URLDecoder.decode(s, "UTF-8")

    fun encode(g: SavedGame): String {
        val s = g.state; val r = s.rules
        return listOf(
            "v1",
            listOf(r.blockades, r.mustCaptureToEnterHome, r.startWithOneOut, r.oneOrSixToLeave).joinToString("") { if (it) "1" else "0" },
            s.current, s.dice, s.consecutiveSixes, s.phase.name, s.winners.joinToString(","),
            s.players.joinToString("/") { "${it.color.name}:${it.tokens.joinToString(",")}:${if (it.hasCaptured) 1 else 0}" },
            g.seats.joinToString("/") { "${enc(it.name)}:${it.bot?.name ?: "-"}" },
        ).joinToString(";")
    }

    fun decode(text: String): SavedGame? = runCatching {
        val p = text.split(";")
        require(p.size == 9 && p[0] == "v1" && p[1].length == 4)
        val rules = Rules(p[1][0] == '1', p[1][1] == '1', p[1][2] == '1', p[1][3] == '1')
        val players = p[7].split("/").map { f ->
            val (c, t, cap) = f.split(":")
            Player(Color.valueOf(c), t.split(",").map { it.toInt() }.also { tk -> require(tk.size == 4 && tk.all { it in YARD..HOME }) }, cap == "1")
        }
        val seats = p[8].split("/").map { f ->
            val (n, b) = f.split(":")
            Seat(dec(n), if (b == "-") null else Difficulty.valueOf(b))
        }
        val winners = p[6].split(",").filter { it.isNotEmpty() }.map { it.toInt() }
        val state = GameState(players, p[2].toInt(), p[3].toInt(), p[4].toInt(), Phase.valueOf(p[5]), winners, rules)
        require(players.size in 2..4 && seats.size == players.size && state.current in players.indices && winners.all { it in players.indices })
        SavedGame(state, seats)
    }.getOrNull()
}
