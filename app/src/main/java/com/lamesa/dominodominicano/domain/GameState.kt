package com.lamesa.dominodominicano.domain

/**
 * Estado completo de una partida de Dominó Dominicano.
 */
data class GameState(
    val config: GameConfig = GameConfig(),
    val hands: Map<PlayerId, List<Tile>> = emptyMap(),
    val board: Board = Board(),
    val currentPlayer: PlayerId = PlayerId.SOUTH,
    val scores: Map<String, Int> = mapOf("us" to 0, "them" to 0),
    val handNumber: Int = 1,
    val passes: Map<PlayerId, Boolean> = emptyMap(),
    val consecutivePasses: Int = 0,
    val lastPassPlayer: PlayerId? = null,
    val starterOfHand: PlayerId = PlayerId.SOUTH,
    val phase: Phase = Phase.DEALING,
    val lastMove: Move? = null,
    val bonusesThisHand: List<BonusEvent> = emptyList(),
    val winnerOfHand: PlayerId? = null,
    val winnerOfGame: String? = null,
    val message: String? = null
) {
    enum class Phase {
        DEALING, PLAYING, HAND_OVER, GAME_OVER
    }

    data class Move(
        val player: PlayerId,
        val tile: Tile?,
        val end: Board.End?,
        val isPass: Boolean = false
    )

    data class BonusEvent(
        val type: BonusType,
        val player: PlayerId,
        val points: Int
    )

    enum class BonusType {
        CAPICUA, PASO_REDONDO, PASE_DE_SALIDA, CHUCHAZO
    }

    fun teamOf(player: PlayerId): String = if (player == PlayerId.SOUTH || player == PlayerId.NORTH) "us" else "them"

    fun handOf(player: PlayerId): List<Tile> = hands[player] ?: emptyList()

    fun remainingPoints(player: PlayerId): Int = handOf(player).sumOf { it.points }

    fun isGameOver(): Boolean {
        val us = scores["us"] ?: 0
        val them = scores["them"] ?: 0
        return us >= config.goal || them >= config.goal
    }
}
