package com.example.ludo.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ludo.engine.*

data class GameUiState(
    val game: GameState,
    /** Token positions as displayed; lags [game] while a move animates. */
    val shown: List<List<Int>>,
    val diceValue: Int = 0,
    val rolling: Boolean = false,
    /** Token indices of the current player that can be tapped. */
    val movable: Set<Int> = emptySet(),
    val busy: Boolean = false,
    /** (player, token) pairs showing the capture flash. */
    val flash: Set<Pair<Int, Int>> = emptySet(),
    val message: String? = null,
)

class GameViewModel(
    initial: GameState = newGame(defaultColors(4)),
    private val dice: DiceRoller = RandomDice,
    private val autoMove: Boolean = true,
    private val speed: Float = 1f,
) : ViewModel() {
    private val _state = MutableStateFlow(GameUiState(initial, initial.players.map { it.tokens }))
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    /** Engine events as they play out, for sound and haptics. */
    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    val hopMs: Int get() = wait(120).toInt()
    private fun wait(ms: Long) = (ms / speed).toLong()

    fun onRoll() {
        val s = _state.value
        if (s.busy || s.game.phase != Phase.ROLLING) return
        _state.update { it.copy(busy = true, rolling = true, message = null) }
        viewModelScope.launch {
            delay(wait(600))
            val v = dice.roll()
            val r = applyRoll(s.game, v)
            r.events.forEach { _events.emit(it) }
            _state.update { it.copy(rolling = false, diceValue = v) }
            if (r.state.phase == Phase.CHOOSING_MOVE) {
                val moves = legalMoves(r.state)
                if (autoMove && moves.size == 1) {
                    delay(wait(350))
                    play(r.state, moves.single())
                } else _state.update { it.copy(game = r.state, movable = moves.map { m -> m.token }.toSet(), busy = false) }
            } else { // forfeited: show why, then pass the turn
                val why = r.events.filterIsInstance<GameEvent.TurnForfeited>().first().reason
                _state.update { it.copy(message = if (why == ForfeitReason.TRIPLE_SIX) "Three sixes! Turn lost" else "No move") }
                delay(wait(900))
                _state.update { it.copy(game = r.state, message = null, busy = false) }
            }
        }
    }

    fun onTokenTap(token: Int) {
        val s = _state.value
        if (s.busy || s.game.phase != Phase.CHOOSING_MOVE || token !in s.movable) return
        val from = s.game.players[s.game.current].tokens[token]
        val move = legalMoves(s.game).firstOrNull { it.from == from } ?: return
        _state.update { it.copy(busy = true, movable = emptySet()) }
        viewModelScope.launch { play(s.game, move) }
    }

    private suspend fun play(s: GameState, move: Move) {
        val r = applyMove(s, move)
        _state.update { it.copy(game = s, movable = emptySet(), busy = true) }
        for (e in r.events) {
            _events.emit(e)
            when (e) {
                is GameEvent.Moved -> {
                    // hop square by square; leaving the yard is a single hop
                    val path = if (e.from == YARD) listOf(0) else (e.from + 1..e.to).toList()
                    for (p in path) { setShown(e.player, e.token, p); delay(wait(120)) }
                }
                is GameEvent.Captured -> {
                    _state.update { it.copy(flash = setOf(e.victim to e.token)) }
                    delay(wait(250))
                    setShown(e.victim, e.token, YARD)
                    _state.update { it.copy(flash = emptySet()) }
                    delay(wait(400))
                }
                else -> {}
            }
        }
        _state.update { it.copy(game = r.state, shown = r.state.players.map { p -> p.tokens }, busy = false) }
    }

    private fun setShown(player: Int, token: Int, pos: Int) = _state.update { st ->
        st.copy(shown = st.shown.mapIndexed { i, t -> if (i == player) t.toMutableList().also { it[token] = pos } else t })
    }
}
