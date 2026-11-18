package ludo.engine

private fun GameState.me() = players[current]

private fun GameState.tokensAt(square: Int, exceptPlayer: Int = -1): List<Pair<Int, Int>> =
    players.flatMapIndexed { pi, p ->
        if (pi == exceptPlayer) emptyList()
        else p.tokens.withIndex().filter { absSquare(p.color, it.value) == square }.map { pi to it.index }
    }

private fun GameState.blocked(square: Int): Boolean =
    rules.blockades && players.withIndex().any { (pi, p) ->
        pi != current && p.tokens.count { absSquare(p.color, it) == square } >= 2
    }

/** Moves the current player may make with the current dice. Empty unless phase is CHOOSING_MOVE. */
fun legalMoves(s: GameState): List<Move> {
    if (s.phase != Phase.CHOOSING_MOVE) return emptyList()
    val me = s.me()
    val d = s.dice
    val all = me.tokens.withIndex().mapNotNull { (i, pos) ->
        val to = when {
            pos == YARD -> if (d == 6 || (d == 1 && s.rules.oneOrSixToLeave)) 0 else return@mapNotNull null
            pos + d > HOME -> return@mapNotNull null // exact roll needed
            else -> pos + d
        }
        // blockades: can't pass or land on an opponent blockade (exiting the yard only needs the start square)
        val trackSteps = ((if (pos == YARD) 0 else pos + 1)..minOf(to, LAST_TRACK))
        if (trackSteps.any { q -> s.blocked(absSquare(me.color, q)!!) }) return@mapNotNull null
        Move(i, pos, to)
    }.distinctBy { it.from to it.to }
    if (!s.rules.mustCaptureToEnterHome || me.hasCaptured) return all
    val strict = all.filterNot { it.from <= LAST_TRACK && it.to > LAST_TRACK }
    // ponytail: if the restriction would leave no move at all it is waived, so games can't deadlock
    return strict.ifEmpty { all }
}

private fun pass(s: GameState): GameState {
    var next = s.current
    do next = (next + 1) % s.players.size while (next in s.winners)
    return s.copy(current = next, dice = 0, consecutiveSixes = 0, phase = Phase.ROLLING)
}

/** Registers a dice roll. Forfeits (triple six / no legal move) pass the turn immediately. */
fun applyRoll(s: GameState, value: Int): MoveResult {
    require(s.phase == Phase.ROLLING && value in 1..6)
    val sixes = if (value == 6) s.consecutiveSixes + 1 else 0
    val rolled = listOf<GameEvent>(GameEvent.Rolled(s.current, value))
    if (sixes == 3) return MoveResult(pass(s), rolled + GameEvent.TurnForfeited(s.current, ForfeitReason.TRIPLE_SIX))
    val next = s.copy(dice = value, consecutiveSixes = sixes, phase = Phase.CHOOSING_MOVE)
    if (legalMoves(next).isEmpty())
        return MoveResult(pass(s), rolled + GameEvent.TurnForfeited(s.current, ForfeitReason.NO_LEGAL_MOVE))
    return MoveResult(next, rolled)
}

fun applyMove(s: GameState, move: Move): MoveResult {
    require(s.phase == Phase.CHOOSING_MOVE)
    val m = legalMoves(s).firstOrNull { it.from == move.from && it.to == move.to }
        ?: throw IllegalArgumentException("illegal move $move")
    val me = s.me()
    val events = mutableListOf<GameEvent>(GameEvent.Moved(s.current, m.token, m.from, m.to))
    val players = s.players.toMutableList()
    var captured = false

    val sq = absSquare(me.color, m.to)
    if (sq != null && sq !in SAFE_SQUARES) {
        for ((pi, ti) in s.tokensAt(sq, exceptPlayer = s.current)) {
            players[pi] = players[pi].copy(tokens = players[pi].tokens.toMutableList().also { it[ti] = YARD })
            events += GameEvent.Captured(s.current, pi, ti, sq)
            captured = true
        }
    }
    val home = m.to == HOME
    if (home) events += GameEvent.ReachedHome(s.current, m.token)
    players[s.current] = me.copy(
        tokens = me.tokens.toMutableList().also { it[m.token] = m.to },
        hasCaptured = me.hasCaptured || captured,
    )

    var st = s.copy(players = players, dice = 0)
    var winners = s.winners
    if (players[s.current].finished) {
        winners = winners + s.current
        events += GameEvent.PlayerFinished(s.current, winners.size)
    }
    val remaining = players.indices.filter { it !in winners }
    if (remaining.size <= 1) {
        winners = winners + remaining
        events += GameEvent.GameOver(winners)
        return MoveResult(st.copy(winners = winners, phase = Phase.GAME_OVER), events)
    }
    st = st.copy(winners = winners)
    val extra = s.current !in winners && (s.dice == 6 || captured || home)
    return if (extra) {
        events += GameEvent.ExtraTurn(s.current)
        MoveResult(st.copy(phase = Phase.ROLLING), events)
    } else MoveResult(pass(st), events)
}
