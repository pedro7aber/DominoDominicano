package com.lamesa.dominodominicano.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lamesa.dominodominicano.ai.DominoAI
import com.lamesa.dominodominicano.domain.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val engine = GameEngine()
    private val aiPartner = DominoAI(DominoAI.Personality.BALANCED, 42L)
    private val aiWest   = DominoAI(DominoAI.Personality.AGGRESSIVE, 123L)
    private val aiEast   = DominoAI(DominoAI.Personality.DEFENSIVE, 456L)

    private val _state = MutableStateFlow(engine.newGame())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _selected = MutableStateFlow<Tile?>(null)
    val selected: StateFlow<Tile?> = _selected.asStateFlow()

    private var aiJob: Job? = null

    init {
        ensureAiTurnIfNeeded()
    }

    fun startNewGame(config: GameConfig = GameConfig()) {
        aiJob?.cancel()
        _selected.value = null
        _state.value = engine.newGame(config)
        ensureAiTurnIfNeeded()
    }

    fun onTileTapped(tile: Tile) {
        val s = _state.value
        if (s.phase != GameState.Phase.PLAYING || s.currentPlayer != PlayerId.SOUTH) return

        val legal = engine.legalMoves(s, PlayerId.SOUTH)
        if (legal.none { it.first == tile }) return

        if (_selected.value == tile) {
            playHuman(tile)
        } else {
            _selected.value = tile
        }
    }

    fun onPlaySelected() {
        val tile = _selected.value ?: return
        playHuman(tile)
    }

    fun onPass() {
        val s = _state.value
        if (s.phase != GameState.Phase.PLAYING || s.currentPlayer != PlayerId.SOUTH) return
        val newState = engine.pass(s, PlayerId.SOUTH)
        applyState(newState)
    }

    fun onNextHand() {
        val s = _state.value
        if (s.phase != GameState.Phase.HAND_OVER) return
        aiJob?.cancel()
        _selected.value = null
        _state.value = engine.dealNewHand(s)
        ensureAiTurnIfNeeded()
    }

    fun legalTiles(): Set<Tile> =
        engine.legalMoves(_state.value, PlayerId.SOUTH).map { it.first }.toSet()

    private fun playHuman(tile: Tile) {
        val s = _state.value
        val legal = engine.legalMoves(s, PlayerId.SOUTH)
        val end = legal.find { it.first == tile }?.second
        val newState = engine.playTile(s, PlayerId.SOUTH, tile, end)
        applyState(newState)
    }

    private fun applyState(newState: GameState) {
        _state.value = newState
        _selected.value = null
        ensureAiTurnIfNeeded()
    }

    private fun ensureAiTurnIfNeeded() {
        val s = _state.value
        if (s.phase == GameState.Phase.PLAYING && !s.currentPlayer.isHuman()) {
            scheduleAi()
        }
    }

    private fun scheduleAi() {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            delay(700)
            runAiLoop()
        }
    }

    private fun runAiLoop() {
        var current = _state.value
        var guard = 0
        while (current.phase == GameState.Phase.PLAYING &&
               !current.currentPlayer.isHuman() &&
               guard < 16) {
            guard++
            val player = current.currentPlayer
            val ai = when (player) {
                PlayerId.NORTH -> aiPartner
                PlayerId.WEST  -> aiWest
                PlayerId.EAST  -> aiEast
                else -> return
            }

            // Forzar 6-6 en apertura si lo tiene
            val move = if (current.board.isEmpty && current.handNumber == 1) {
                val d6 = current.handOf(player).find { it.left == 6 && it.right == 6 }
                if (d6 != null) d6 to null else ai.chooseMove(current, player)
            } else {
                ai.chooseMove(current, player)
            }

            current = if (move == null) {
                engine.pass(current, player)
            } else {
                engine.playTile(current, player, move.first, move.second)
            }
            _state.value = current

            // Pequeña pausa visual entre jugadas de IA
            if (current.phase == GameState.Phase.PLAYING && !current.currentPlayer.isHuman()) {
                // el delay está en scheduleAi; aquí solo actualizamos estado
            }
        }
    }
}
