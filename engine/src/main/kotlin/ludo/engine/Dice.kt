package ludo.engine

import kotlin.random.Random

fun interface DiceRoller { fun roll(): Int }

/** Seeded dice for tests and reproducible games. */
class SeededDice(seed: Long) : DiceRoller {
    private val rnd = Random(seed)
    override fun roll() = rnd.nextInt(1, 7)
}
