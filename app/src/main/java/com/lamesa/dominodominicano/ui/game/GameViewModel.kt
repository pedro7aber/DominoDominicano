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

/**
 * ViewModel de juego – arquitectura limpia.
 * - El dominio (GameEngine) es la única fuente de verdad de legalidad.
 * - La UI solo observa estado y envía intenciones.
 * - La IA se programa de forma segura y nunca se solapa.
 */
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
        // Si al crear el VM el turno no es del humano, la IA debe salir
        ensureAiTurnIfNeeded()
    }

    // ------------------------------------------------------------------
    // Intenciones públicas (llamadas desde la UI)
    // ------------------------------------------------------------------

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
        val isLegal = legal.any { it.first == tile }
        if (!isLegal) return

        // One-tap play: si ya estaba seleccionada → jugar; si no → seleccionar
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
        if (newState != s) {
            _state.value = newState
            _selected.value = null
            ensureAiTurnIfNeeded()
        }
    }

    fun onNextHand() {
        val s = _state.value
        if (s.phase != GameState.Phase.HAND_OVER) return
        aiJob?.cancel()
        _selected.value = null
        _state.value = engine.dealNewHand(s)
        ensureAiTurnIfNeeded()
    }

    fun legalTiles(): Set<Tile> {
        return engine.legalMoves(_state.value, PlayerId.SOUTH).map { it.first }.toSet()
    }

    // ------------------------------------------------------------------
    // Interno
    // ------------------------------------------------------------------

    private fun playHuman(tile: Tile) {
        val s = _state.value
        val legal = engine.legalMoves(s, PlayerId.SOUTH)
        val end = legal.find { it.first == tile }?.second
        val newState = engine.playTile(s, PlayerId.SOUTH, tile, end)
        if (newState != s) {
            _state.value = newState
            _selected.value = null
            ensureAiTurnIfNeeded()
        }
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
            delay(550) // tiempo de "pensamiento"
            runAiLoop()
        }
    }

    private fun runAiLoop() {
        var current = _state.value
        var guard = 0
        while (current.phase == GameState.Phase.PLAYING &&
               !current.currentPlayer.isHuman() &&
               guard < 12) {
            guard++
            val player = current.currentPlayer
            val ai = when (player) {
                PlayerId.NORTH -> aiPartner
                PlayerId.WEST  -> aiWest
                PlayerId.EAST  -> aiEast
                else -> return
            }

            // Forzar 6-6 en la primera mano si la IA lo tiene
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
        }
    }
}
