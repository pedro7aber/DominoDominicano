package com.lamesa.dominodominicano.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lamesa.dominodominicano.ai.DominoAI
import com.lamesa.dominodominicano.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val engine = GameEngine()
    private val aiPartner = DominoAI(DominoAI.Personality.BALANCED, 42L)
    private val aiWest = DominoAI(DominoAI.Personality.AGGRESSIVE, 123L)
    private val aiEast = DominoAI(DominoAI.Personality.DEFENSIVE, 456L)

    private val _state = MutableStateFlow(engine.newGame())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _selectedTile = MutableStateFlow<Tile?>(null)
    val selectedTile: StateFlow<Tile?> = _selectedTile.asStateFlow()

    fun startNewGame(config: GameConfig = GameConfig()) {
        _state.value = engine.newGame(config)
        _selectedTile.value = null
        if (!_state.value.currentPlayer.isHuman()) {
            scheduleAiTurn()
        }
    }

    fun selectTile(tile: Tile?) {
        _selectedTile.value = tile
    }

    fun playSelected(end: Board.End? = null) {
        val tile = _selectedTile.value ?: return
        val current = _state.value
        if (current.currentPlayer != PlayerId.SOUTH) return

        val newState = engine.playTile(current, PlayerId.SOUTH, tile, end)
        if (newState != current) {
            _state.value = newState
            _selectedTile.value = null
            if (newState.phase == GameState.Phase.PLAYING && !newState.currentPlayer.isHuman()) {
                scheduleAiTurn()
            }
        }
    }

    fun pass() {
        val current = _state.value
        if (current.currentPlayer != PlayerId.SOUTH) return
        val newState = engine.pass(current, PlayerId.SOUTH)
        if (newState != current) {
            _state.value = newState
            if (newState.phase == GameState.Phase.PLAYING && !newState.currentPlayer.isHuman()) {
                scheduleAiTurn()
            }
        }
    }

    fun nextHand() {
        val current = _state.value
        if (current.phase != GameState.Phase.HAND_OVER) return
        _state.value = engine.dealNewHand(current)
        if (!_state.value.currentPlayer.isHuman()) {
            scheduleAiTurn()
        }
    }

    private fun scheduleAiTurn() {
        viewModelScope.launch {
            delay(700)
            playAiTurn()
        }
    }

    private fun playAiTurn() {
        var current = _state.value
        while (current.phase == GameState.Phase.PLAYING && !current.currentPlayer.isHuman()) {
            val player = current.currentPlayer
            val ai = when (player) {
                PlayerId.NORTH -> aiPartner
                PlayerId.WEST -> aiWest
                PlayerId.EAST -> aiEast
                else -> return
            }
            val move = ai.chooseMove(current, player)
            current = if (move == null) {
                engine.pass(current, player)
            } else {
                engine.playTile(current, player, move.first, move.second)
            }
            _state.value = current
            if (current.phase != GameState.Phase.PLAYING || current.currentPlayer.isHuman()) break
        }
    }

    fun legalMovesForHuman(): List<Pair<Tile, Board.End?>> {
        return engine.legalMoves(_state.value, PlayerId.SOUTH)
    }
}
