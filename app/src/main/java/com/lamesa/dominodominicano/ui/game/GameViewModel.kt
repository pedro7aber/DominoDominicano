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
    private val aiWest = DominoAI(DominoAI.Personality.AGGRESSIVE, 123L)
    private val aiEast = DominoAI(DominoAI.Personality.DEFENSIVE, 456L)

    private val _state = MutableStateFlow(engine.newGame())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _selected = MutableStateFlow<Tile?>(null)
    val selected: StateFlow<Tile?> = _selected.asStateFlow()

    /** Cuando la ficha seleccionada tiene 2 extremos legales, se pide elegir. */
    private val _pendingEnds = MutableStateFlow<List<Board.End>>(emptyList())
    val pendingEnds: StateFlow<List<Board.End>> = _pendingEnds.asStateFlow()

    private var aiJob: Job? = null

    init { ensureAiTurnIfNeeded() }

    fun startNewGame(config: GameConfig = GameConfig()) {
        aiJob?.cancel()
        _selected.value = null
        _pendingEnds.value = emptyList()
        resetAllAiMemory()
        _state.value = engine.newGame(config)
        ensureAiTurnIfNeeded()
    }

    fun onTileTapped(tile: Tile) {
        val s = _state.value
        if (s.phase != GameState.Phase.PLAYING || s.currentPlayer != PlayerId.SOUTH) return
        val legal = engine.legalMoves(s, PlayerId.SOUTH).filter { it.first == tile }
        if (legal.isEmpty()) return

        if (_selected.value == tile && _pendingEnds.value.isEmpty()) {
            // ya seleccionada y un solo extremo → jugar
            playHuman(tile, legal.first().second)
            return
        }

        _selected.value = tile
        val ends = legal.mapNotNull { it.second }.distinct()
        if (ends.size >= 2) {
            _pendingEnds.value = ends // mostrar Izquierda / Derecha
        } else {
            _pendingEnds.value = emptyList()
        }
    }

    fun onChooseEnd(end: Board.End) {
        val tile = _selected.value ?: return
        playHuman(tile, end)
    }

    fun onPlaySelected() {
        val tile = _selected.value ?: return
        val ends = _pendingEnds.value
        if (ends.size >= 2) return // debe elegir extremo
        val legal = engine.legalMoves(_state.value, PlayerId.SOUTH).filter { it.first == tile }
        playHuman(tile, legal.firstOrNull()?.second)
    }

    fun onPass() {
        val s = _state.value
        if (s.phase != GameState.Phase.PLAYING || s.currentPlayer != PlayerId.SOUTH) return
        applyState(engine.pass(s, PlayerId.SOUTH))
    }

    fun onNextHand() {
        val s = _state.value
        if (s.phase != GameState.Phase.HAND_OVER) return
        aiJob?.cancel()
        _selected.value = null
        _pendingEnds.value = emptyList()
        resetAllAiMemory()
        _state.value = engine.dealNewHand(s)
        ensureAiTurnIfNeeded()
    }

    fun legalTiles(): Set<Tile> =
        engine.legalMoves(_state.value, PlayerId.SOUTH).map { it.first }.toSet()

    private fun playHuman(tile: Tile, end: Board.End?) {
        val newState = engine.playTile(_state.value, PlayerId.SOUTH, tile, end)
        _pendingEnds.value = emptyList()
        applyState(newState)
    }

    private fun applyState(newState: GameState) {
        _state.value = newState
        _selected.value = null
        _pendingEnds.value = emptyList()
        ensureAiTurnIfNeeded()
    }

    private fun resetAllAiMemory() {
        aiPartner.resetHandMemory()
        aiWest.resetHandMemory()
        aiEast.resetHandMemory()
    }

    private fun ensureAiTurnIfNeeded() {
        val s = _state.value
        if (s.phase == GameState.Phase.PLAYING && !s.currentPlayer.isHuman()) scheduleAi()
    }

    private fun scheduleAi() {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            delay(600)
            runAiLoop()
        }
    }

    private fun runAiLoop() {
        var current = _state.value
        var guard = 0
        while (current.phase == GameState.Phase.PLAYING && !current.currentPlayer.isHuman() && guard < 16) {
            guard++
            val player = current.currentPlayer
            val ai = when (player) {
                PlayerId.NORTH -> aiPartner
                PlayerId.WEST -> aiWest
                PlayerId.EAST -> aiEast
                else -> return
            }
            val move = if (current.board.isEmpty && current.handNumber == 1) {
                val d6 = current.handOf(player).find { it.left == 6 && it.right == 6 }
                if (d6 != null) d6 to null else ai.chooseMove(current, player)
            } else ai.chooseMove(current, player)

            current = if (move == null) engine.pass(current, player)
            else engine.playTile(current, player, move.first, move.second)
            _state.value = current
        }
    }
}
