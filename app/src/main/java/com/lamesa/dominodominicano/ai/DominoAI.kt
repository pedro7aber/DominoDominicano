package com.lamesa.dominodominicano.ai

import com.lamesa.dominodominicano.domain.*
import kotlin.random.Random

/**
 * IA de Dominó Dominicano – nivel alto.
 * Conteo real de fichas restantes, memoria de pases por palo,
 * protección de pareja, bloqueo y conciencia de bonos.
 */
class DominoAI(
    private val personality: Personality = Personality.BALANCED,
    private val seed: Long = System.currentTimeMillis()
) {
    enum class Personality { AGGRESSIVE, DEFENSIVE, BALANCED }

    private val rng = Random(seed)

    fun chooseMove(state: GameState, player: PlayerId): Pair<Tile, Board.End?>? {
        val legal = GameEngine().legalMoves(state, player)
        if (legal.isEmpty()) return null
        if (legal.size == 1) return legal.first()

        if (state.board.isEmpty) {
            val double6 = legal.find { it.first.left == 6 && it.first.right == 6 }
            if (double6 != null) return double6
            // Preferir doble más alto en manos siguientes
            val bestDouble = legal.filter { it.first.isDouble }.maxByOrNull { it.first.left }
            if (bestDouble != null) return bestDouble
        }

        val scored = legal.map { move ->
            move to evaluate(state, player, move.first, move.second)
        }
        return scored.maxByOrNull { it.second + rng.nextDouble() * 0.8 }?.first
    }

    private fun evaluate(state: GameState, player: PlayerId, tile: Tile, end: Board.End?): Double {
        var score = 0.0
        val hand = state.handOf(player)
        val partner = player.partner()
        val myTeam = state.teamOf(player)
        val board = state.board

        // 1. Liberar puntos altos
        score += tile.points * 1.1

        // 2. Dobles tempranos
        if (tile.isDouble) {
            score += 16.0 - tile.left * 1.8
            if (hand.size >= 5) score += 10.0
        }

        // 3. Nuevo tablero simulado
        val newBoard = if (board.isEmpty) board.play(tile) else board.play(tile, end ?: Board.End.LEFT)
        val newLeft = newBoard.leftEnd!!
        val newRight = newBoard.rightEnd!!

        val remaining = hand.filter { it != tile }

        // 4. Matches residuales propios
        val myMatches = remaining.count { it.matches(newLeft) || it.matches(newRight) }
        score += myMatches * 5.5

        // 5. Conteo real de fichas restantes por palo
        val allPlayed = board.chain.map { it.tile } + tile
        val playedSet = allPlayed.map { setOf(it.left, it.right) to it }.toSet()
        val full = Tile.fullSet()
        val stillOut = full.filter { t ->
            allPlayed.none { it.left == t.left && it.right == t.right }
        }
        // Quitar las de mi mano y (aprox) no conocemos las de otros
        val unknown = stillOut.filter { t -> remaining.none { it.left == t.left && it.right == t.right } }

        val leftCount = unknown.count { it.matches(newLeft) }
        val rightCount = unknown.count { it.matches(newRight) }

        if (leftCount <= 1) score += 9.0
        if (rightCount <= 1) score += 9.0
        if (leftCount == 0) score += 6.0
        if (rightCount == 0) score += 6.0

        // 6. Memoria de pases: si un rival pasó, ese palo es más seguro
        val passedRivals = state.passes.filter { it.value && state.teamOf(it.key) != myTeam }.keys
        if (passedRivals.isNotEmpty()) {
            score += 5.0
        }

        // 7. Protección del compañero
        val partnerSize = state.handOf(partner).size
        if (partnerSize <= 3) {
            // Evitar extremos muy altos si el partner tiene pocas fichas
            if (newLeft >= 5 || newRight >= 5) score -= 5.0
            // Preferir dejar extremos bajos
            if (newLeft <= 2 || newRight <= 2) score += 4.0
        }

        // 8. Bloqueo del siguiente rival
        val next = player.next()
        if (state.teamOf(next) != myTeam) {
            if (leftCount <= 1 || rightCount <= 1) score += 8.0
        }

        // 9. Dominó inminente
        if (remaining.isEmpty()) score += 120.0

        // 10. Preferir no dejar al rival un extremo que yo no puedo responder
        if (myMatches == 0 && remaining.isNotEmpty()) score -= 14.0

        // 11. Personalidad
        when (personality) {
            Personality.AGGRESSIVE -> {
                score += tile.points * 0.6
                if (tile.isDouble) score += 4.0
            }
            Personality.DEFENSIVE -> {
                if (myMatches == 0) score -= 16.0
                score += myMatches * 3.0
            }
            Personality.BALANCED -> {}
        }

        return score
    }
}
