package com.example.ludo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ludo.data.AppContainer
import com.example.ludo.data.Settings
import com.example.ludo.game.GameViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ludo.engine.*

sealed interface Screen {
    data object Home : Screen
    data object Setup : Screen
    data object Game : Screen
    data object Settings : Screen
    data object HowTo : Screen
    data object Leaderboard : Screen
    data class Results(val ranking: List<Pair<Seat, Color>>) : Screen
}

data class SetupConfig(
    val count: Int = 2,
    val seats: List<Seat> = listOf(Seat("Player 1"), Seat("Computer", Difficulty.MEDIUM), Seat("Player 3"), Seat("Player 4")),
    val rules: Rules = Rules(anyRollToLeave = true),
)

/** Owns navigation, settings, the saved game and the running [GameViewModel]; survives rotation. */
class AppViewModel(private val c: AppContainer) : ViewModel() {
    val settings = c.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val saved = c.savedGame.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val history = c.history.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen = _screen.asStateFlow()
    private val _setup = MutableStateFlow(SetupConfig())
    val setup = _setup.asStateFlow()
    private val _game = MutableStateFlow<GameViewModel?>(null)
    val game = _game.asStateFlow()

    fun go(s: Screen) { _screen.value = s }
    fun openSetup() { _setup.update { it.copy(rules = settings.value.rules) }; go(Screen.Setup) }
    fun updateSetup(f: (SetupConfig) -> SetupConfig) = _setup.update(f)
    fun clearHistory() { viewModelScope.launch { c.clearHistory() } }
    fun updateSettings(f: (Settings) -> Settings) { viewModelScope.launch { c.updateSettings(f) } }

    fun startGame() {
        val cfg = _setup.value
        val seats = cfg.seats.take(cfg.count).mapIndexed { i, s -> s.copy(name = s.name.trim().ifEmpty { "Player ${i + 1}" }) }
        begin(newGame(defaultColors(cfg.count), cfg.rules), seats)
    }

    fun continueGame() { saved.value?.let { begin(it.state, it.seats) } }

    fun restart() {
        val g = _game.value ?: return
        val s = g.state.value.game
        begin(newGame(s.players.map { it.color }, s.rules), g.seats)
    }

    /** Leaves the game; progress stays saved so "Continue" resumes it. */
    fun quit() { _game.value?.close(); _game.value = null; go(Screen.Home) }

    fun finish() {
        val g = _game.value ?: return
        val s = g.state.value.game
        _screen.value = Screen.Results(s.winners.map { g.seats[it] to s.players[it].color })
    }

    private fun begin(state: GameState, seats: List<Seat>) {
        _game.value?.close()
        val st = settings.value
        _game.value = GameViewModel(state, seats, autoMove = st.autoMove, speed = st.animSpeed.toFloat(), onSettled = { persist(it, seats, record = true) })
        persist(state, seats, record = false)
        go(Screen.Game)
    }

    private fun persist(s: GameState, seats: List<Seat>, record: Boolean) {
        viewModelScope.launch {
            if (s.phase != Phase.GAME_OVER) c.saveGame(SavedGame(s, seats))
            else {
                c.clearGame()
                if (record) c.addRecord(recordOf(s, seats, System.currentTimeMillis())) // finished games feed the leaderboard
            }
        }
    }
}
