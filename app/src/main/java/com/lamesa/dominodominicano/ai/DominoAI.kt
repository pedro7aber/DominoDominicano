package com.lamesa.dominodominicano.ai

import com.lamesa.dominodominicano.domain.*
import kotlin.random.Random

class DominoAI(
    private val personality: Personality = Personality.BALANCED,
    private val seed: Long = System.currentTimeMillis()
) {
    enum class Personality { AGGRESSIVE, DEFENSIVE, BALANCED }

    private val rng = Random(seed)

    fun chooseMove(state: GameState, player: PlayerId): Pair<Tile, Board.End?>? {
        val engine = GameEngine()
        val legal = engine.legalMoves(state, player)
        if (legal.isEmpty()) return null
        if (legal.size == 1) return legal.first()

        // Apertura: siempre preferir el 6-6 si está disponible
        if (state.board.isEmpty) {
            val double6 = legal.find { it.first.left == 6 && it.first.right == 6 }
            if (double6 != null) return double6
        }

        val scored = legal.map { move ->
            move to evaluate(state, player, move.first, move.second)
        }
        return scored.maxByOrNull { it.second + rng.nextDouble() * 0.3 }?.first
    }

    private fun evaluate(state: GameState, player: PlayerId, tile: Tile, end: Board.End?): Double {
        var score = 0.0
        val hand = state.handOf(player)
        val myHandSize = hand.size

        if (tile.isDouble) {
            score += 12.0 - tile.left * 1.2
            if (myHandSize > 4) score += 6.0
        }
        score += tile.points * 0.7

        val board = state.board
        val newBoard = if (board.isEmpty) board.play(tile) else board.play(tile, end ?: Board.End.LEFT)
        val newLeft = newBoard.leftEnd
        val newRight = newBoard.rightEnd
        val myRemaining = hand.filter { it != tile }
        val matchesLeft = myRemaining.count { it.matches(newLeft!!) }
        val matchesRight = myRemaining.count { it.matches(newRight!!) }
        score += (matchesLeft + matchesRight) * 3.5

        if (myHandSize == 1) score += 80.0

        when (personality) {
            Personality.AGGRESSIVE -> score += tile.points * 0.4
            Personality.DEFENSIVE -> if (matchesLeft + matchesRight == 0) score -= 15.0
            Personality.BALANCED -> {}
        }
        return score
    }
}
