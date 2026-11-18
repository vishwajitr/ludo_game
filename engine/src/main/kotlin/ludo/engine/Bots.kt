package ludo.engine

import kotlin.random.Random

enum class Difficulty { EASY, MEDIUM, HARD }

/** Picks one of [moves] (the non-empty result of [legalMoves]) for the current player. */
fun interface Bot { fun choose(s: GameState, moves: List<Move>): Move }

fun botFor(d: Difficulty, rnd: Random = Random.Default): Bot = when (d) {
    Difficulty.EASY -> Bot { _, m -> m[rnd.nextInt(m.size)] }
    Difficulty.MEDIUM -> MediumBot
    Difficulty.HARD -> HardBot
}

/** Opponent track squares that threaten (reach with a 1..6) the given absolute square. */
private fun threatened(s: GameState, me: Int, square: Int?): Boolean {
    if (square == null || square in SAFE_SQUARES) return false
    return s.players.withIndex().any { (pi, p) ->
        pi != me && p.tokens.any { pos ->
            val a = absSquare(p.color, pos) ?: return@any false
            val d = (square - a + TRACK_LEN) % TRACK_LEN
            d in 1..6 && pos + d <= LAST_TRACK
        }
    }
}

/** Priority: capture > reach home > leave yard > escape danger > advance furthest token. */
object MediumBot : Bot {
    override fun choose(s: GameState, moves: List<Move>): Move {
        val me = s.players[s.current]
        fun score(m: Move): Int {
            val events = applyMove(s, m).events
            val capture = events.any { it is GameEvent.Captured }
            val home = m.to == HOME
            val leave = m.from == YARD
            val escape = threatened(s, s.current, absSquare(me.color, m.from)) &&
                !threatened(s, s.current, absSquare(me.color, m.to))
            return (if (capture) 1_000_000 else 0) + (if (home) 100_000 else 0) + (if (leave) 10_000 else 0) +
                (if (escape) 1_000 else 0) + m.to
        }
        return moves.maxBy { score(it) }
    }
}

/**
 * Scores each move by progress, captures, reaching home, blockades and the change in
 * expected loss from the opponents' replies (1-ply lookahead over their six dice outcomes).
 * Everything is in "progress points", so exposure of an advanced token costs more.
 */
object HardBot : Bot {
    override fun choose(s: GameState, moves: List<Move>): Move {
        val before = exposure(s, s.current)
        return moves.maxBy { score(s, it, before) }
    }

    private fun score(s: GameState, m: Move, exposureBefore: Double): Double {
        val me = s.current
        val r = applyMove(s, m)
        var v = (m.to - m.from).toDouble()
        if (m.from == YARD) v += 8
        if (m.to == HOME) v += 15
        for (e in r.events) if (e is GameEvent.Captured) v += 6 + 1.5 * (s.players[e.victim].tokens[e.token] + 1)
        if (s.rules.blockades) {
            val color = s.players[me].color
            val sq = absSquare(color, m.to)
            if (sq != null && r.state.players[me].tokens.count { absSquare(color, it) == sq } >= 2) v += 4
        }
        // r.state may already belong to the next player; exposure only looks at my tokens
        return v + exposureBefore - exposure(r.state, me)
    }

    /** Expected progress points [me] loses to opponent captures over one round of replies. */
    private fun exposure(s: GameState, me: Int): Double {
        val mine = s.players[me]
        val n = s.players.size
        var total = 0.0
        for (step in 1 until n) {
            val o = (me + step) % n
            if (o in s.winners) continue
            val weight = if (step == 1) 1.0 else .4 // the next player moves first
            var loss = 0.0
            for (d in 1..6) {
                val st = s.copy(current = o, dice = d, phase = Phase.CHOOSING_MOVE, consecutiveSixes = 0)
                val color = s.players[o].color
                loss += legalMoves(st).maxOfOrNull { m ->
                    val sq = absSquare(color, m.to)
                    if (sq == null || sq in SAFE_SQUARES) 0
                    else mine.tokens.sumOf { if (absSquare(mine.color, it) == sq) it + 1 else 0 }
                } ?: 0
            }
            total += weight * loss / 6
        }
        return total
    }
}
