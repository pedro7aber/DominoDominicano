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

    private var aiJobRunning = false

    init {
        // Si al crear el ViewModel el turno no es del humano, la IA debe empezar
        if (!_state.value.currentPlayer.isHuman()) {
            scheduleAiTurn()
        }
    }

    fun startNewGame(config: GameConfig = GameConfig()) {
        aiJobRunning = false
        _state.value = engine.newGame(config)
        _selectedTile.value = null
        if (!_state.value.currentPlayer.isHuman()) {
            scheduleAiTurn()
        }
    }

    fun selectTile(tile: Tile?) {
        if (_state.value.currentPlayer != PlayerId.SOUTH) return
        if (_state.value.phase != GameState.Phase.PLAYING) return
        _selectedTile.value = tile
    }

    /** Juega la ficha seleccionada o la que se pasa como parámetro. */
    fun playTile(tile: Tile, end: Board.End? = null) {
        val current = _state.value
        if (current.currentPlayer != PlayerId.SOUTH || current.phase != GameState.Phase.PLAYING) return

        val newState = engine.playTile(current, PlayerId.SOUTH, tile, end)
        if (newState !== current && newState != current) {
            _state.value = newState
            _selectedTile.value = null
            if (newState.phase == GameState.Phase.PLAYING && !newState.currentPlayer.isHuman()) {
                scheduleAiTurn()
            }
        }
    }

    fun playSelected(end: Board.End? = null) {
        val tile = _selectedTile.value ?: return
        playTile(tile, end)
    }

    fun pass() {
        val current = _state.value
        if (current.currentPlayer != PlayerId.SOUTH || current.phase != GameState.Phase.PLAYING) return
        val newState = engine.pass(current, PlayerId.SOUTH)
        if (newState != current) {
            _state.value = newState
            _selectedTile.value = null
            if (newState.phase == GameState.Phase.PLAYING && !newState.currentPlayer.isHuman()) {
                scheduleAiTurn()
            }
        }
    }

    fun nextHand() {
        val current = _state.value
        if (current.phase != GameState.Phase.HAND_OVER) return
        aiJobRunning = false
        _state.value = engine.dealNewHand(current)
        _selectedTile.value = null
        if (!_state.value.currentPlayer.isHuman()) {
            scheduleAiTurn()
        }
    }

    private fun scheduleAiTurn() {
        if (aiJobRunning) return
        aiJobRunning = true
        viewModelScope.launch {
            delay(600)
            playAiTurn()
            aiJobRunning = false
        }
    }

    private fun playAiTurn() {
        var current = _state.value
        var safety = 0
        while (current.phase == GameState.Phase.PLAYING && !current.currentPlayer.isHuman() && safety < 20) {
            safety++
            val player = current.currentPlayer
            val ai = when (player) {
                PlayerId.NORTH -> aiPartner
                PlayerId.WEST -> aiWest
                PlayerId.EAST -> aiEast
                else -> return
            }

            // Forzar el 6-6 en la primera mano si la IA lo tiene
            val move: Pair<Tile, Board.End?>? = if (current.board.isEmpty && current.handNumber == 1) {
                val double6 = current.handOf(player).find { it.left == 6 && it.right == 6 }
                if (double6 != null) double6 to null else ai.chooseMove(current, player)
            } else {
                ai.chooseMove(current, player)
            }

            current = if (move == null) {
                engine.pass(current, player)
            } else {
                engine.playTile(current, player, move.first, move.second)
            }
            _state.value = current

            if (current.phase != GameState.Phase.PLAYING || current.currentPlayer.isHuman()) break
            // pequeña pausa visual entre IAs
            // (no bloqueamos el hilo principal más de lo necesario)
        }
    }

    fun legalMovesForHuman(): List<Pair<Tile, Board.End?>> {
        return engine.legalMoves(_state.value, PlayerId.SOUTH)
    }

    fun isLegal(tile: Tile): Boolean {
        return legalMovesForHuman().any { it.first == tile }
    }
}
