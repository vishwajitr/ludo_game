package ludo.engine

import java.net.URLDecoder
import java.net.URLEncoder

/** One finished game: who played, and their final rank (1 = winner). */
data class GameRecord(val time: Long, val players: List<RecordPlayer>)
data class RecordPlayer(val name: String, val bot: Difficulty?, val rank: Int)

data class PlayerStats(val name: String, val bot: Difficulty?, val games: Int, val wins: Int) {
    val winPercent: Int get() = if (games == 0) 0 else wins * 100 / games
}

/** Builds the record of a finished game; [state].winners holds seat indexes in finishing order. */
fun recordOf(state: GameState, seats: List<Seat>, time: Long) =
    GameRecord(time, state.winners.mapIndexed { i, seat -> RecordPlayer(seats[seat].name, seats[seat].bot, i + 1) })

/**
 * Humans are identified by name (case-insensitive), bots by name + difficulty.
 * Ranked by wins, then win rate, then games played.
 */
fun leaderboard(records: List<GameRecord>, includeBots: Boolean): List<PlayerStats> {
    val byKey = LinkedHashMap<String, PlayerStats>()
    // oldest first, so the most recent spelling of a name is the one displayed
    for (p in records.sortedBy { it.time }.flatMap { r -> r.players }) {
        if (p.bot != null && !includeBots) continue
        val key = if (p.bot == null) p.name.trim().lowercase() else "${p.name.trim().lowercase()}|${p.bot}"
        val old = byKey[key]
        byKey[key] = PlayerStats(p.name.trim(), p.bot, (old?.games ?: 0) + 1, (old?.wins ?: 0) + if (p.rank == 1) 1 else 0)
    }
    return byKey.values.sortedWith(compareByDescending<PlayerStats> { it.wins }.thenByDescending { it.winPercent }.thenByDescending { it.games })
}

/** Text codec, one record per line: `time;name:BOT|-:rank/name:...`. Malformed lines are skipped. */
object HistoryCodec {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun dec(s: String) = URLDecoder.decode(s, "UTF-8")

    fun encode(records: List<GameRecord>): String = records.joinToString("\n") { r ->
        "${r.time};" + r.players.joinToString("/") { "${enc(it.name)}:${it.bot?.name ?: "-"}:${it.rank}" }
    }

    fun decode(text: String): List<GameRecord> = text.lines().mapNotNull { line ->
        runCatching {
            val (time, players) = line.split(";")
            GameRecord(time.toLong(), players.split("/").map { f ->
                val (n, b, r) = f.split(":")
                RecordPlayer(dec(n), if (b == "-") null else Difficulty.valueOf(b), r.toInt())
            }.also { require(it.size in 2..4) })
        }.getOrNull()
    }
}
