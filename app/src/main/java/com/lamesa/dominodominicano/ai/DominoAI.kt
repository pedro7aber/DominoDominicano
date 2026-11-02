package com.lamesa.dominodominicano.ai

import com.lamesa.dominodominicano.domain.*
import kotlin.random.Random

/**
 * IA de nivel élite para Dominó Dominicano.
 * Heurística profunda: conteo de fichas, protección de compañero,
 * control de extremos, preferencia de dobles, conciencia de bonos y meta.
 * Se instancia 3 veces con diferentes seeds/personalidades.
 */
class DominoAI(
    private val personality: Personality = Personality.BALANCED,
    private val seed: Long = System.currentTimeMillis()
) {
    enum class Personality {
        AGGRESSIVE,   // busca dominar rápido, menos tranque
        DEFENSIVE,    // protege, tranca más
        BALANCED
    }

    private val rng = Random(seed)

    fun chooseMove(state: GameState, player: PlayerId): Pair<Tile, Board.End?>? {
        val engine = GameEngine()
        val legal = engine.legalMoves(state, player)
        if (legal.isEmpty()) return null

        if (legal.size == 1) return legal.first()

        // Evaluar cada movimiento
        val scored = legal.map { move ->
            move to evaluate(state, player, move.first, move.second)
        }

        // Ordenar por score descendente + un poco de ruido para variedad
        val best = scored.maxByOrNull { it.second + rng.nextDouble() * 0.5 }
        return best?.first
    }

    private fun evaluate(state: GameState, player: PlayerId, tile: Tile, end: Board.End?): Double {
        var score = 0.0
        val board = state.board
        val hand = state.handOf(player)
        val partner = player.partner()
        val partnerHandSize = state.handOf(partner).size
        val myHandSize = hand.size

        // 1. Preferir jugar dobles altos temprano
        if (tile.isDouble) {
            score += 15.0 - tile.left * 1.5
            if (myHandSize > 4) score += 8.0
        }

        // 2. Contar puntos: preferir deshacerse de fichas altas
        score += tile.points * 0.8

        // 3. Control de extremos
        val newBoard = if (board.isEmpty) board.play(tile) else board.play(tile, end ?: Board.End.LEFT)
        val newLeft = newBoard.leftEnd
        val newRight = newBoard.rightEnd

        val myRemaining = hand.filter { it != tile }
        val matchesLeft = myRemaining.count { it.matches(newLeft!!) }
        val matchesRight = myRemaining.count { it.matches(newRight!!) }
        score += (matchesLeft + matchesRight) * 4.0

        // 4. Protección del compañero
        if (partnerHandSize <= 2) {
            if (matchesLeft == 0) score -= 12.0
            if (matchesRight == 0) score -= 12.0
        }

        // 5. Bloqueo potencial
        score += estimateBlockingValue(state, player, newLeft, newRight)

        // 6. Si puedo ganar la mano
        if (myHandSize == 1) {
            score += 100.0
            if (tile.isDouble && tile.left == 0 && state.config.chuchazoEnabled) score += 30.0
        }

        // 7. Personalidad
        when (personality) {
            Personality.AGGRESSIVE -> score += tile.points * 0.5
            Personality.DEFENSIVE -> {
                if (matchesLeft + matchesRight == 0) score -= 20.0
            }
            Personality.BALANCED -> {}
        }

        // 8. Evitar dejar el 0-0 si no es chuchazo
        if (tile.left == 0 && tile.right == 0 && myHandSize > 1) score -= 5.0

        return score
    }

    private fun estimateBlockingValue(state: GameState, player: PlayerId, left: Int?, right: Int?): Double {
        val allPlayed = state.board.chain.flatMap { listOf(it.tile.left, it.tile.right) }
        val remainingByValue = (0..6).associateWith { v ->
            7 - allPlayed.count { it == v }
        }
        val myHand = state.handOf(player)
        var score = 0.0
        if (left != null) {
            val possible = remainingByValue[left] ?: 0
            val mine = myHand.count { it.matches(left) }
            if (possible - mine <= 1) score += 6.0
        }
        if (right != null && right != left) {
            val possible = remainingByValue[right] ?: 0
            val mine = myHand.count { it.matches(right) }
            if (possible - mine <= 1) score += 6.0
        }
        return score
    }
}
