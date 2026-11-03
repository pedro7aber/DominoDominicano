package com.lamesa.dominodominicano.domain

import kotlin.random.Random

class GameEngine(private val rng: Random = Random.Default) {

    fun newGame(config: GameConfig = GameConfig()): GameState {
        return dealNewHand(GameState(config = config, phase = GameState.Phase.DEALING), isFirstHand = true)
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

        val endsBefore = state.board.leftEnd to state.board.rightEnd
        val passesBefore = state.consecutivePasses

        if (state.board.isEmpty) {
            if (state.handNumber == 1 && !(tile.left == 6 && tile.right == 6)) return state
            val newBoard = state.board.play(tile, Board.End.LEFT, player)
            hand.remove(tile)
            var newState = state.copy(
                hands = state.hands.toMutableMap().apply { put(player, hand) },
                board = newBoard,
                currentPlayer = player.next(),
                lastMove = GameState.Move(player, tile, Board.End.LEFT),
                passes = emptyMap(),
                consecutivePasses = 0,
                lastPassPlayer = null,
                message = null
            )
            if (hand.isEmpty()) newState = finishHandByDomino(newState, player, tile, endsBefore, passesBefore)
            return newState
        }

        val legalEnds = state.board.legalEnds(tile)
        if (legalEnds.isEmpty()) return state
        val chosenEnd = end ?: legalEnds.first()
        if (chosenEnd !in legalEnds) return state

        val newBoard = state.board.play(tile, chosenEnd, player)
        hand.remove(tile)

        var newState = state.copy(
            hands = state.hands.toMutableMap().apply { put(player, hand) },
            board = newBoard,
            currentPlayer = player.next(),
            lastMove = GameState.Move(player, tile, chosenEnd),
            passes = state.passes.toMutableMap().apply { remove(player) },
            consecutivePasses = 0,
            lastPassPlayer = null,
            message = null
        )

        if (hand.isEmpty()) {
            newState = finishHandByDomino(newState, player, tile, endsBefore, passesBefore)
        } else if (passesBefore >= 3) {
            val bonus = state.config.bonusValue
            val ev = GameState.BonusEvent(GameState.BonusType.PASO_REDONDO, player, bonus, state.handNumber)
            val team = newState.teamOf(player)
            val newScores = newState.scores.toMutableMap()
            newScores[team] = (newScores[team] ?: 0) + bonus
            newState = newState.copy(
                bonusesThisHand = newState.bonusesThisHand + ev,
                allBonuses = newState.allBonuses + ev,
                scores = newScores,
                message = "¡Paso Redondo! +$bonus"
            )
        }
        return newState
    }

    fun pass(state: GameState, player: PlayerId): GameState {
        if (state.phase != GameState.Phase.PLAYING || state.currentPlayer != player) return state
        if (state.handOf(player).any { state.board.canPlay(it) }) return state

        val newPasses = state.passes.toMutableMap().apply { put(player, true) }
        val consecutive = state.consecutivePasses + 1
        val bonuses = state.bonusesThisHand.toMutableList()
        var allBonuses = state.allBonuses
        var msg = "${playerLabel(player)} pasó"

        val passRec = GameState.PassRecord(
            player, state.board.leftEnd, state.board.rightEnd, state.handNumber
        )

        val starter = state.starterOfHand
        if (player == starter.next() && state.lastMove?.player == starter && state.consecutivePasses == 0) {
            val bp = state.config.bonusValue
            val ev = GameState.BonusEvent(GameState.BonusType.PASE_DE_SALIDA, starter, bp, state.handNumber)
            bonuses.add(ev)
            allBonuses = allBonuses + ev
            msg += " · ¡Pase de Salida! +$bp"
        }
        if (player == starter.partner() && bonuses.any { it.type == GameState.BonusType.PASE_DE_SALIDA }) {
            bonuses.removeAll { it.type == GameState.BonusType.PASE_DE_SALIDA }
            allBonuses = allBonuses.filterNot {
                it.type == GameState.BonusType.PASE_DE_SALIDA && it.handNumber == state.handNumber
            }
            msg += " · Bono anulado"
        }

        var newState = state.copy(
            currentPlayer = player.next(),
            lastMove = GameState.Move(player, null, null, isPass = true),
            passes = newPasses,
            consecutivePasses = consecutive,
            lastPassPlayer = player,
            bonusesThisHand = bonuses,
            allBonuses = allBonuses,
            passHistory = state.passHistory + passRec,
            message = msg
        )
        if (consecutive >= 4) newState = finishHandByTranque(newState)
        return newState
    }

    private fun finishHandByDomino(
        state: GameState, winner: PlayerId, lastTile: Tile,
        endsBefore: Pair<Int?, Int?>, passesBefore: Int
    ): GameState {
        var points = calculatePoints(state, winner)
        val bonuses = state.bonusesThisHand.toMutableList()
        var allB = state.allBonuses
        var msg = "¡Dominó! ${playerLabel(winner)}"

        val (lb, rb) = endsBefore
        if (!lastTile.isDouble && lb != null && rb != null && lb != rb &&
            lastTile.matches(lb) && lastTile.matches(rb)
        ) {
            val b = state.config.bonusValue
            val ev = GameState.BonusEvent(GameState.BonusType.CAPICUA, winner, b, state.handNumber)
            bonuses.add(ev); allB = allB + ev; points += b; msg += " · Capicúa +$b"
        }
        if (state.config.chuchazoEnabled && lastTile.left == 0 && lastTile.right == 0) {
            val b = state.config.bonusValue
            val ev = GameState.BonusEvent(GameState.BonusType.CHUCHAZO, winner, b, state.handNumber)
            bonuses.add(ev); allB = allB + ev; points += b; msg += " · Chuchazo +$b"
        }
        if (passesBefore >= 3) {
            val b = state.config.bonusValue
            val ev = GameState.BonusEvent(GameState.BonusType.PASO_REDONDO, winner, b, state.handNumber)
            bonuses.add(ev); allB = allB + ev; points += b; msg += " · Paso Redondo +$b"
        }
        points += bonuses.filter { it.type == GameState.BonusType.PASE_DE_SALIDA }.sumOf { it.points }

        val team = state.teamOf(winner)
        val newScores = state.scores.toMutableMap()
        newScores[team] = (newScores[team] ?: 0) + points
        val gw = when {
            (newScores["us"] ?: 0) >= state.config.goal -> "us"
            (newScores["them"] ?: 0) >= state.config.goal -> "them"
            else -> null
        }
        return state.copy(
            scores = newScores,
            phase = if (gw != null) GameState.Phase.GAME_OVER else GameState.Phase.HAND_OVER,
            winnerOfHand = winner,
            winnerOfGame = gw,
            bonusesThisHand = bonuses,
            allBonuses = allB,
            message = "$msg (+$points)"
        )
    }

    private fun finishHandByTranque(state: GameState): GameState {
        val config = state.config
        val newScores = state.scores.toMutableMap()
        val points: Int
        val winnerPlayer: PlayerId
        val winnerTeam: String
        val msg: String

        if (config.tranqueIndividual) {
            val lastPasser = state.lastPassPlayer ?: state.currentPlayer
            val right = lastPasser.next()
            winnerPlayer = when {
                state.remainingPoints(lastPasser) < state.remainingPoints(right) -> lastPasser
                state.remainingPoints(right) < state.remainingPoints(lastPasser) -> right
                else -> lastPasser // empate favorece al que trancó
            }
            winnerTeam = state.teamOf(winnerPlayer)
            points = if (config.countAllTiles) {
                PlayerId.entries.sumOf { state.remainingPoints(it) }
            } else {
                state.remainingPoints(if (winnerPlayer == lastPasser) right else lastPasser)
            }
            msg = "¡Tranque! ${playerLabel(winnerPlayer)} (+$points)"
        } else {
            val us = state.remainingPoints(PlayerId.SOUTH) + state.remainingPoints(PlayerId.NORTH)
            val them = state.remainingPoints(PlayerId.WEST) + state.remainingPoints(PlayerId.EAST)
            winnerTeam = when {
                us < them -> "us"
                them < us -> "them"
                else -> state.teamOf(state.lastPassPlayer ?: state.starterOfHand)
            }
            // Jugador representativo real del equipo ganador (el de menor puntos del equipo)
            winnerPlayer = if (winnerTeam == "us") {
                if (state.remainingPoints(PlayerId.SOUTH) <= state.remainingPoints(PlayerId.NORTH))
                    PlayerId.SOUTH else PlayerId.NORTH
            } else {
                if (state.remainingPoints(PlayerId.WEST) <= state.remainingPoints(PlayerId.EAST))
                    PlayerId.WEST else PlayerId.EAST
            }
            points = if (config.countAllTiles) us + them else if (winnerTeam == "us") them else us
            msg = "¡Tranque parejas! $winnerTeam (+$points)"
        }

        newScores[winnerTeam] = (newScores[winnerTeam] ?: 0) + points
        val gw = when {
            (newScores["us"] ?: 0) >= config.goal -> "us"
            (newScores["them"] ?: 0) >= config.goal -> "them"
            else -> null
        }
        return state.copy(
            scores = newScores,
            phase = if (gw != null) GameState.Phase.GAME_OVER else GameState.Phase.HAND_OVER,
            winnerOfHand = winnerPlayer,
            winnerOfGame = gw,
            message = msg
        )
    }

    private fun calculatePoints(state: GameState, winner: PlayerId): Int {
        val wt = state.teamOf(winner)
        return if (state.config.countAllTiles) PlayerId.entries.sumOf { state.remainingPoints(it) }
        else PlayerId.entries.filter { state.teamOf(it) != wt }.sumOf { state.remainingPoints(it) }
    }

    fun legalMoves(state: GameState, player: PlayerId): List<Pair<Tile, Board.End?>> {
        if (state.currentPlayer != player || state.phase != GameState.Phase.PLAYING) return emptyList()
        val hand = state.handOf(player)
        if (state.board.isEmpty) {
            return if (state.handNumber == 1)
                hand.filter { it.left == 6 && it.right == 6 }.map { it to null }
            else hand.map { it to null }
        }
        val moves = mutableListOf<Pair<Tile, Board.End?>>()
        for (tile in hand) for (e in state.board.legalEnds(tile)) moves.add(tile to e)
        return moves
    }

    private fun playerLabel(p: PlayerId) = when (p) {
        PlayerId.SOUTH -> "Tú"; PlayerId.NORTH -> "Compañero"
        PlayerId.WEST -> "Arriba"; PlayerId.EAST -> "Abajo"
    }
}
