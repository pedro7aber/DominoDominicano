package com.lamesa.dominodominicano.ai

import com.lamesa.dominodominicano.domain.*
import kotlin.random.Random

/**
 * IA de Dominó Dominicano – nivel intermedio/alto.
 * - Conteo aproximado de fichas restantes por palo
 * - Memoria de quién pasó a qué extremo
 * - Protección del compañero (evitar dejarle extremos difíciles)
 * - Bloqueo del rival cuando es posible
 * - Preferencia por dobles tempranos y por liberar puntos altos
 * - Forzado del 6-6 en apertura
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

        // Apertura: siempre 6-6 si está disponible
        if (state.board.isEmpty) {
            val double6 = legal.find { it.first.left == 6 && it.first.right == 6 }
            if (double6 != null) return double6
        }

        val scored = legal.map { move ->
            move to evaluate(state, player, move.first, move.second)
        }

        // Pequeña aleatoriedad para no ser predecible
        return scored.maxByOrNull { it.second + rng.nextDouble() * 1.5 }?.first
    }

    private fun evaluate(state: GameState, player: PlayerId, tile: Tile, end: Board.End?): Double {
        var score = 0.0
        val hand = state.handOf(player)
        val partner = player.partner()
        val myTeam = state.teamOf(player)
        val board = state.board

        // 1. Valor base de la ficha (liberar puntos altos es bueno)
        score += tile.points * 0.9

        // 2. Preferir dobles tempranos (son restrictivos)
        if (tile.isDouble) {
            score += 14.0 - tile.left * 1.5
            if (hand.size > 4) score += 8.0
        }

        // 3. Simular el nuevo tablero
        val newBoard = if (board.isEmpty) board.play(tile) else board.play(tile, end ?: Board.End.LEFT)
        val newLeft = newBoard.leftEnd!!
        val newRight = newBoard.rightEnd!!

        // 4. Matches residuales en mi mano (mantener opciones)
        val remaining = hand.filter { it != tile }
        val myMatches = remaining.count { it.matches(newLeft) || it.matches(newRight) }
        score += myMatches * 4.5

        // 5. Conteo aproximado de fichas restantes por palo (cuántas de ese número quedan fuera)
        val allPlayed = board.chain.map { it.tile } + tile
        val remainingInGame = Tile.fullSet().filter { t -> allPlayed.none { it.left == t.left && it.right == t.right } }
        val leftCount = remainingInGame.count { it.matches(newLeft) }
        val rightCount = remainingInGame.count { it.matches(newRight) }

        // Si dejo un extremo con pocas fichas restantes, es más fácil de bloquear / controlar
        if (leftCount <= 2) score += 6.0
        if (rightCount <= 2) score += 6.0

        // 6. Memoria de pases: si un rival pasó a un palo, es más seguro jugar ese palo
        val passedPlayers = state.passes.filter { it.value }.keys
        for (p in passedPlayers) {
            if (state.teamOf(p) != myTeam) {
                // Rival pasó → ese extremo es más seguro
                score += 3.5
            }
        }

        // 7. Protección del compañero: evitar dejar extremos que el compañero no pueda jugar
        // (aproximación: si el compañero tiene pocas fichas y el extremo es alto, penalizar)
        val partnerHandSize = state.handOf(partner).size
        if (partnerHandSize <= 3 && (newLeft >= 5 || newRight >= 5)) {
            score -= 4.0
        }

        // 8. Bloqueo: si puedo dejar un extremo que el siguiente rival probablemente no tenga
        val nextPlayer = player.next()
        if (state.teamOf(nextPlayer) != myTeam) {
            if (leftCount <= 1 || rightCount <= 1) score += 7.0
        }

        // 9. Dominó inminente
        if (remaining.isEmpty()) score += 100.0

        // 10. Personalidad
        when (personality) {
            Personality.AGGRESSIVE -> {
                score += tile.points * 0.5
                if (tile.isDouble) score += 3.0
            }
            Personality.DEFENSIVE -> {
                if (myMatches == 0) score -= 12.0
                score += myMatches * 2.0
            }
            Personality.BALANCED -> {}
        }

        return score
    }
}
