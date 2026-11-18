package ludo.engine

/** Seats go clockwise; each color starts 13 squares after the previous one. */
enum class Color { RED, GREEN, YELLOW, BLUE;
    val start: Int get() = ordinal * 13
}

/** ANIMATING is set by the UI layer while events play; the engine never produces it. */
enum class Phase { ROLLING, CHOOSING_MOVE, ANIMATING, GAME_OVER }

/*
 * A token's position is its progress along its own lap:
 *  -1 = yard, 0..50 = main track (0 is the color's start square),
 *  51..55 = home column, 56 = home.
 * Main-track square 51 (the one just before start) is never used by that color.
 */
const val YARD = -1
const val LAST_TRACK = 50
const val HOME = 56
const val TRACK_LEN = 52

/** Absolute main-track square (0..51) for a position, or null in yard / home column / home. */
fun absSquare(color: Color, pos: Int): Int? =
    if (pos in 0..LAST_TRACK) (color.start + pos) % TRACK_LEN else null

val SAFE_SQUARES: Set<Int> = Color.entries.flatMap { listOf(it.start, (it.start + 8) % TRACK_LEN) }.toSet()

data class Rules(
    val blockades: Boolean = false,
    val mustCaptureToEnterHome: Boolean = false,
    val startWithOneOut: Boolean = false,
    val oneOrSixToLeave: Boolean = false,
)

data class Player(val color: Color, val tokens: List<Int>, val hasCaptured: Boolean = false) {
    val finished: Boolean get() = tokens.all { it == HOME }
}

data class GameState(
    val players: List<Player>,
    val current: Int = 0,
    val dice: Int = 0,
    val consecutiveSixes: Int = 0,
    val phase: Phase = Phase.ROLLING,
    val winners: List<Int> = emptyList(),
    val rules: Rules = Rules(),
)

/** Move of the current player's token [token] from position [from] to [to]. */
data class Move(val token: Int, val from: Int, val to: Int)

enum class ForfeitReason { NO_LEGAL_MOVE, TRIPLE_SIX }

sealed interface GameEvent {
    data class Rolled(val player: Int, val value: Int) : GameEvent
    data class Moved(val player: Int, val token: Int, val from: Int, val to: Int) : GameEvent
    data class Captured(val by: Int, val victim: Int, val token: Int, val square: Int) : GameEvent
    data class ReachedHome(val player: Int, val token: Int) : GameEvent
    data class ExtraTurn(val player: Int) : GameEvent
    data class TurnForfeited(val player: Int, val reason: ForfeitReason) : GameEvent
    data class PlayerFinished(val player: Int, val rank: Int) : GameEvent
    data class GameOver(val ranking: List<Int>) : GameEvent
}

data class MoveResult(val state: GameState, val events: List<GameEvent>)

fun defaultColors(n: Int): List<Color> = when (n) {
    2 -> listOf(Color.RED, Color.YELLOW)
    3 -> listOf(Color.RED, Color.GREEN, Color.YELLOW)
    else -> Color.entries
}

fun newGame(colors: List<Color>, rules: Rules = Rules()): GameState {
    require(colors.size in 2..4 && colors.distinct().size == colors.size)
    val first = if (rules.startWithOneOut) 0 else YARD
    return GameState(colors.map { Player(it, listOf(first, YARD, YARD, YARD)) }, rules = rules)
}
