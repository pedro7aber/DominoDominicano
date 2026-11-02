package com.lamesa.dominodominicano.domain

import kotlin.random.Random

/**
 * Motor de reglas estricto de Dominó Dominicano v3.
 * Implementa: salida 6-6, obligación de jugar, Capicúa, Pase de Salida,
 * Paso Redondo, Chuchazo, Tranque individual/por parejas, conteo configurable.
 */
class GameEngine(private val rng: Random = Random.Default) {

    fun newGame(config: GameConfig = GameConfig()): GameState {
        val state = GameState(config = config, phase = GameState.Phase.DEALING)
        return dealNewHand(state, isFirstHand = true)
    }

    fun dealNewHand(state: GameState, isFirstHand: Boolean = false): GameState {
        val tiles = Tile.fullSet().shuffled(rng)
        val hands = mutableMapOf<PlayerId, List<Tile>>()
        var idx = 0
        for (p in PlayerId.entries) {
            hands[p] = tiles.subList(idx, idx + 7).sorted()
            idx += 7
        }

        val starter: PlayerId = if (isFirstHand) {
            hands.entries.first { (_, h) -> h.any { it.left == 6 && it.right == 6 } }.key
        } else {
            state.winnerOfHand ?: state.starterOfHand
        }

        return state.copy(
            hands = hands,
            board = Board(),
            currentPlayer = starter,
            handNumber = if (isFirstHand) 1 else state.handNumber + 1,
            passes = emptyMap(),
            consecutivePasses = 0,
            lastPassPlayer = null,
            starterOfHand = starter,
            phase = GameState.Phase.PLAYING,
            lastMove = null,
            bonusesThisHand = emptyList(),
            winnerOfHand = null,
            message = if (isFirstHand) "Sale el que tiene el 6-6" else "Nueva mano · Sale ${playerLabel(starter)}"
        )
    }

    fun playTile(state: GameState, player: PlayerId, tile: Tile, end: Board.End?): GameState {
        if (state.phase != GameState.Phase.PLAYING || state.currentPlayer != player) return state
        val hand = state.handOf(player).toMutableList()
        if (!hand.contains(tile)) return state

        // --- Apertura ---
        if (state.board.isEmpty) {
            if (state.handNumber == 1 && !(tile.left == 6 && tile.right == 6)) return state
            val newBoard = state.board.play(tile)
            hand.remove(tile)
            val newHands = state.hands.toMutableMap().apply { put(player, hand) }
            var newState = state.copy(
                hands = newHands,
                board = newBoard,
                currentPlayer = player.next(),
                lastMove = GameState.Move(player, tile, Board.End.LEFT),
                passes = emptyMap(),
                consecutivePasses = 0,
                lastPassPlayer = null,
                message = null
            )
            if (hand.isEmpty()) newState = finishHandByDomino(newState, player, tile)
            return newState
        }

        val legalEnds = state.board.legalEnds(tile)
        if (legalEnds.isEmpty()) return state
        val chosenEnd = end ?: legalEnds.first()
        if (chosenEnd !in legalEnds) return state

        val newBoard = state.board.play(tile, chosenEnd)
        hand.remove(tile)
        val newHands = state.hands.toMutableMap().apply { put(player, hand) }

        // --- Evaluación de bonos inmediatos ---
        val bonuses = state.bonusesThisHand.toMutableList()
        var bonusMsg: String? = null

        // Capicúa: la ficha cierra ambos extremos (leftEnd == rightEnd después de jugar y la ficha coincide con ambos)
        val isCapicua = !tile.isDouble &&
                state.board.leftEnd != null &&
                state.board.rightEnd != null &&
                state.board.leftEnd == state.board.rightEnd &&
                tile.matches(state.board.leftEnd!!) &&
                hand.isEmpty().not().let { true } // se evalúa al final si es la última

        // Pase de Salida: si el jugador anterior fue el salidor de la mano y el actual (siguiente) acaba de jugar
        // (el bono se otorga cuando el rival de la derecha del salidor PASA, no cuando juega).
        // Se maneja en pass().

        var newState = state.copy(
            hands = newHands,
            board = newBoard,
            currentPlayer = player.next(),
            lastMove = GameState.Move(player, tile, chosenEnd),
            passes = state.passes.toMutableMap().apply { remove(player) },
            consecutivePasses = 0,
            lastPassPlayer = null,
            bonusesThisHand = bonuses,
            message = bonusMsg
        )

        if (hand.isEmpty()) {
            newState = finishHandByDomino(newState, player, tile)
        }
        return newState
    }

    fun pass(state: GameState, player: PlayerId): GameState {
        if (state.phase != GameState.Phase.PLAYING || state.currentPlayer != player) return state
        val hand = state.handOf(player)
        if (hand.any { state.board.canPlay(it) }) return state // obligatorio jugar

        val newPasses = state.passes.toMutableMap().apply { put(player, true) }
        val consecutive = state.consecutivePasses + 1
        val bonuses = state.bonusesThisHand.toMutableList()
        var msg = "${playerLabel(player)} pasó"

        // --- Pase de Salida ---
        // Si el jugador que pasa es el siguiente al salidor de la mano (rival de la derecha del salidor)
        // y es el primer pase después de la salida, se otorga el bono al salidor.
        val starter = state.starterOfHand
        val rightOfStarter = starter.next()
        if (player == rightOfStarter && state.lastMove?.player == starter && state.consecutivePasses == 0) {
            val bonusPoints = state.config.bonusValue
            bonuses.add(GameState.BonusEvent(GameState.BonusType.PASE_DE_SALIDA, starter, bonusPoints))
            msg += " · ¡Pase de Salida! +$bonusPoints para ${playerLabel(starter)}"
        }

        // Si después del Pase de Salida el compañero del salidor también pasa, se elimina el bono.
        val partnerOfStarter = starter.partner()
        if (player == partnerOfStarter && bonuses.any { it.type == GameState.BonusType.PASE_DE_SALIDA }) {
            bonuses.removeAll { it.type == GameState.BonusType.PASE_DE_SALIDA }
            msg += " · Bono de Pase de Salida anulado"
        }

        // --- Paso Redondo ---
        // Los otros tres pasan y el jugador actual aún puede jugar → se evalúa cuando alguien juega después de 3 pases.
        // Se detecta en el siguiente playTile si consecutivePasses == 3.

        var newState = state.copy(
            currentPlayer = player.next(),
            lastMove = GameState.Move(player, null, null, isPass = true),
            passes = newPasses,
            consecutivePasses = consecutive,
            lastPassPlayer = player,
            bonusesThisHand = bonuses,
            message = msg
        )

        // Tranque: 3 pases consecutivos (los 3 jugadores restantes no pueden jugar)
        if (consecutive >= 3) {
            newState = finishHandByTranque(newState)
        }
        return newState
    }

    private fun finishHandByDomino(state: GameState, winner: PlayerId, lastTile: Tile): GameState {
        var points = calculatePoints(state, winner)
        val bonuses = state.bonusesThisHand.toMutableList()
        var msg = "¡Dominó! ${playerLabel(winner)} gana la mano"

        // Capicúa: última ficha cierra ambos extremos (leftEnd == rightEnd antes de jugar y la ficha coincide)
        val board = state.board
        if (!lastTile.isDouble && board.leftEnd != null && board.rightEnd != null &&
            board.leftEnd == board.rightEnd && lastTile.matches(board.leftEnd!!)) {
            val bonus = state.config.bonusValue
            bonuses.add(GameState.BonusEvent(GameState.BonusType.CAPICUA, winner, bonus))
            points += bonus
            msg += " · ¡Capicúa! +$bonus"
        }

        // Chuchazo: última ficha es 0-0 y la opción está activada
        if (state.config.chuchazoEnabled && lastTile.left == 0 && lastTile.right == 0) {
            val bonus = state.config.bonusValue
            bonuses.add(GameState.BonusEvent(GameState.BonusType.CHUCHAZO, winner, bonus))
            points += bonus
            msg += " · ¡Chuchazo! +$bonus"
        }

        // Paso Redondo: si había 3 pases consecutivos antes de esta jugada (el jugador pudo jugar cuando los otros 3 no)
        if (state.consecutivePasses >= 3) {
            val bonus = state.config.bonusValue
            bonuses.add(GameState.BonusEvent(GameState.BonusType.PASO_REDONDO, winner, bonus))
            points += bonus
            msg += " · ¡Paso Redondo! +$bonus"
        }

        // Sumar bonos de Pase de Salida ya registrados
        val paseSalidaBonus = bonuses.filter { it.type == GameState.BonusType.PASE_DE_SALIDA }.sumOf { it.points }
        points += paseSalidaBonus

        val team = state.teamOf(winner)
        val newScores = state.scores.toMutableMap()
        newScores[team] = (newScores[team] ?: 0) + points

        val gameWinner = when {
            (newScores["us"] ?: 0) >= state.config.goal -> "us"
            (newScores["them"] ?: 0) >= state.config.goal -> "them"
            else -> null
        }

        return state.copy(
            scores = newScores,
            phase = if (gameWinner != null) GameState.Phase.GAME_OVER else GameState.Phase.HAND_OVER,
            winnerOfHand = winner,
            winnerOfGame = gameWinner,
            bonusesThisHand = bonuses,
            message = "$msg (+$points)"
        )
    }

    private fun finishHandByTranque(state: GameState): GameState {
        val config = state.config
        val newScores = state.scores.toMutableMap()
        var points: Int
        var winnerTeam: String
        var msg: String

        if (config.tranqueIndividual) {
            // Tranque individual: quien hizo el último pase (el que cerró el tranque) vs el de su derecha
            val lastPasser = state.lastPassPlayer ?: state.currentPlayer
            val rightOfPasser = lastPasser.next()
            val ptsPasser = state.remainingPoints(lastPasser)
            val ptsRight = state.remainingPoints(rightOfPasser)
            val winnerPlayer = when {
                ptsPasser < ptsRight -> lastPasser
                ptsRight < ptsPasser -> rightOfPasser
                else -> lastPasser // empate → gana quien hizo el tranque
            }
            winnerTeam = state.teamOf(winnerPlayer)
            points = if (config.countAllTiles) {
                PlayerId.entries.sumOf { state.remainingPoints(it) }
            } else {
                state.remainingPoints(if (winnerPlayer == lastPasser) rightOfPasser else lastPasser)
            }
            msg = "¡Tranque individual! ${playerLabel(winnerPlayer)} gana (+$points)"
        } else {
            // Tranque por parejas
            val usPts = state.remainingPoints(PlayerId.SOUTH) + state.remainingPoints(PlayerId.NORTH)
            val themPts = state.remainingPoints(PlayerId.WEST) + state.remainingPoints(PlayerId.EAST)
            winnerTeam = when {
                usPts < themPts -> "us"
                themPts < usPts -> "them"
                else -> state.teamOf(state.lastPassPlayer ?: state.starterOfHand)
            }
            points = if (config.countAllTiles) usPts + themPts else if (winnerTeam == "us") themPts else usPts
            msg = "¡Tranque por parejas! Equipo $winnerTeam suma $points"
        }

        newScores[winnerTeam] = (newScores[winnerTeam] ?: 0) + points

        val gameWinner = when {
            (newScores["us"] ?: 0) >= state.config.goal -> "us"
            (newScores["them"] ?: 0) >= state.config.goal -> "them"
            else -> null
        }

        return state.copy(
            scores = newScores,
            phase = if (gameWinner != null) GameState.Phase.GAME_OVER else GameState.Phase.HAND_OVER,
            winnerOfHand = if (winnerTeam == "us") PlayerId.SOUTH else PlayerId.WEST,
            winnerOfGame = gameWinner,
            message = msg
        )
    }

    private fun calculatePoints(state: GameState, winner: PlayerId): Int {
        val winnerTeam = state.teamOf(winner)
        return if (state.config.countAllTiles) {
            PlayerId.entries.sumOf { state.remainingPoints(it) }
        } else {
            PlayerId.entries.filter { state.teamOf(it) != winnerTeam }.sumOf { state.remainingPoints(it) }
        }
    }

    fun legalMoves(state: GameState, player: PlayerId): List<Pair<Tile, Board.End?>> {
        if (state.currentPlayer != player || state.phase != GameState.Phase.PLAYING) return emptyList()
        val hand = state.handOf(player)
        if (state.board.isEmpty) {
            return if (state.handNumber == 1) {
                hand.filter { it.left == 6 && it.right == 6 }.map { it to null }
            } else {
                hand.map { it to null }
            }
        }
        val moves = mutableListOf<Pair<Tile, Board.End?>>()
        for (tile in hand) {
            for (e in state.board.legalEnds(tile)) {
                moves.add(tile to e)
            }
        }
        return moves
    }

    private fun playerLabel(p: PlayerId): String = when (p) {
        PlayerId.SOUTH -> "Tú"
        PlayerId.NORTH -> "Compañero"
        PlayerId.WEST -> "El de arriba"
        PlayerId.EAST -> "El de abajo"
    }
}
