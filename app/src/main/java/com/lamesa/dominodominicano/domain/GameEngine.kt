package com.lamesa.dominodominicano.domain

import kotlin.random.Random

/**
 * Motor de reglas estricto de Dominó Dominicano v3.
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
            lastPassPlayer = null,
            starterOfHand = starter,
            phase = GameState.Phase.PLAYING,
            lastMove = null,
            bonusesThisHand = emptyList(),
            winnerOfHand = null,
            message = if (isFirstHand) "Sale el que tiene el 6-6" else "Nueva mano"
        )
    }

    fun playTile(state: GameState, player: PlayerId, tile: Tile, end: Board.End?): GameState {
        if (state.phase != GameState.Phase.PLAYING || state.currentPlayer != player) return state
        val hand = state.handOf(player).toMutableList()
        if (!hand.contains(tile)) return state

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

        var newState = state.copy(
            hands = newHands,
            board = newBoard,
            currentPlayer = player.next(),
            lastMove = GameState.Move(player, tile, chosenEnd),
            passes = state.passes.toMutableMap().apply { remove(player) },
            lastPassPlayer = null,
            message = null
        )
        if (hand.isEmpty()) newState = finishHandByDomino(newState, player, tile)
        return newState
    }

    fun pass(state: GameState, player: PlayerId): GameState {
        if (state.phase != GameState.Phase.PLAYING || state.currentPlayer != player) return state
        val hand = state.handOf(player)
        if (hand.any { state.board.canPlay(it) }) return state // obligatorio jugar

        val newPasses = state.passes.toMutableMap().apply { put(player, true) }
        var newState = state.copy(
            currentPlayer = player.next(),
            lastMove = GameState.Move(player, null, null, isPass = true),
            passes = newPasses,
            lastPassPlayer = player,
            message = "${playerLabel(player)} pasó"
        )
        if (newPasses.count { it.value } >= 3) {
            newState = finishHandByTranque(newState)
        }
        return newState
    }

    private fun finishHandByDomino(state: GameState, winner: PlayerId, lastTile: Tile): GameState {
        var points = calculatePoints(state, winner)
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
            message = "¡Dominó! ${playerLabel(winner)} gana la mano (+$points)"
        )
    }

    private fun finishHandByTranque(state: GameState): GameState {
        val lastMover = state.lastMove?.player ?: state.starterOfHand
        val usPts = state.remainingPoints(PlayerId.SOUTH) + state.remainingPoints(PlayerId.NORTH)
        val themPts = state.remainingPoints(PlayerId.WEST) + state.remainingPoints(PlayerId.EAST)
        val winnerTeam = when {
            usPts < themPts -> "us"
            themPts < usPts -> "them"
            else -> state.teamOf(lastMover)
        }
        val points = if (state.config.countAllTiles) usPts + themPts else if (winnerTeam == "us") themPts else usPts

        val newScores = state.scores.toMutableMap()
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
            message = "¡Tranque! Equipo $winnerTeam suma $points"
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
