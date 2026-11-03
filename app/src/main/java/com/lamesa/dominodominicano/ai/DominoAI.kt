package com.lamesa.dominodominicano.ai

import com.lamesa.dominodominicano.domain.*
import kotlin.random.Random

/**
 * IA de primer nivel — Dominó Dominicano.
 *
 * Implementa los 7 pilares de la psicología de jugadores élite:
 * 1. Lectura de mano propia (puntos vs 42, dobles, cantidad por palo)
 * 2. Información pública (memoria permanente de pases, conteo 7-mesa-mano)
 * 3. Juego de pareja (señal de primera ficha, no reabrir pases del partner)
 * 4. Control de extremos (dejar firmes, matar palos del rival)
 * 5. Evaluación de tranca (aritmética 168, comparar con el de abajo)
 * 6. Conciencia de partida (agresividad según distancia a la meta)
 * 7. Conciencia de bonos (Capicúa, Paso Redondo, Pase de Salida)
 */
class DominoAI(
    private val personality: Personality = Personality.BALANCED,
    private val seed: Long = System.currentTimeMillis()
) {
    enum class Personality { AGGRESSIVE, DEFENSIVE, BALANCED }

    private val rng = Random(seed)
    private val engine = GameEngine()

    // Memoria de pases: player → set de valores que NO tiene
    private val passMemory = mutableMapOf<PlayerId, MutableSet<Int>>()

    // Señal de pareja: palo fuerte inferido del compañero
    private var partnerStrongSuit: Int? = null
    private var partnerFirstTileSeen = false

    fun chooseMove(state: GameState, player: PlayerId): Pair<Tile, Board.End?>? {
        updateMemory(state, player)

        val legal = engine.legalMoves(state, player)
        if (legal.isEmpty()) return null
        if (legal.size == 1) return legal.first()

        // Apertura: 6-6 obligatorio en mano 1; si no, mejor doble o ficha más acompañada
        if (state.board.isEmpty) {
            return chooseOpening(state, player, legal)
        }

        val scored = legal.map { move ->
            move to evaluate(state, player, move.first, move.second)
        }

        // Pequeña aleatoriedad solo para desempates (no diluye la estrategia)
        return scored.maxByOrNull { it.second + rng.nextDouble() * 0.15 }?.first
    }

    // -------------------------------------------------------------------------
    // 1 + 2: Memoria y conteo
    // -------------------------------------------------------------------------

    private fun updateMemory(state: GameState, me: PlayerId) {
        // Registrar pases
        for ((p, passed) in state.passes) {
            if (passed && state.board.leftEnd != null) {
                val mem = passMemory.getOrPut(p) { mutableSetOf() }
                state.board.leftEnd?.let { mem.add(it) }
                state.board.rightEnd?.let { mem.add(it) }
            }
        }
        // Si el último movimiento fue un pase, registrar extremos de ese momento
        state.lastMove?.let { move ->
            if (move.isPass && state.board.leftEnd != null) {
                val mem = passMemory.getOrPut(move.player) { mutableSetOf() }
                state.board.leftEnd?.let { mem.add(it) }
                state.board.rightEnd?.let { mem.add(it) }
            }
        }

        // Señal de pareja: primera ficha voluntaria del compañero
        val partner = me.partner()
        if (!partnerFirstTileSeen) {
            val partnerMove = state.lastMove
            if (partnerMove != null && partnerMove.player == partner && !partnerMove.isPass && partnerMove.tile != null) {
                // El extremo que dejó abierto (el que no matcheó) sugiere fuerza
                val t = partnerMove.tile
                partnerStrongSuit = when {
                    t.isDouble -> t.left
                    else -> t.high // aproximación: el valor alto de su primera ficha
                }
                partnerFirstTileSeen = true
            }
        }
    }

    /** Cuántas fichas de un valor quedan desconocidas (no en mesa ni en mi mano). */
    private fun unseenCount(value: Int, state: GameState, me: PlayerId): Int {
        val TOTAL = 7 // cada número aparece en 7 fichas
        val onBoard = state.board.chain.count { it.tile.matches(value) }
        val inHand = state.handOf(me).count { it.matches(value) }
        return (TOTAL - onBoard - inHand).coerceAtLeast(0)
    }

    /** ¿Este jugador pasó alguna vez a este valor? */
    private fun knownVoid(player: PlayerId, value: Int): Boolean =
        passMemory[player]?.contains(value) == true

    // -------------------------------------------------------------------------
    // Apertura
    // -------------------------------------------------------------------------

    private fun chooseOpening(
        state: GameState,
        player: PlayerId,
        legal: List<Pair<Tile, Board.End?>>
    ): Pair<Tile, Board.End?> {
        // 6-6 si existe
        legal.find { it.first.left == 6 && it.first.right == 6 }?.let { return it }

        val hand = state.handOf(player)
        val handPoints = hand.sumOf { it.points }
        val isHighHand = handPoints > 48 // por encima de media 42

        // Preferir doble más alto si hay dobles
        val doubles = legal.filter { it.first.isDouble }.sortedByDescending { it.first.left }
        if (doubles.isNotEmpty()) {
            // Si mano alta, sacar doble alto pronto; si baja, conservar control
            return if (isHighHand) doubles.first() else doubles.last()
        }

        // Ficha más acompañada (más fichas del mismo palo en mano)
        return legal.maxByOrNull { (tile, _) ->
            hand.count { it.matches(tile.left) } + hand.count { it.matches(tile.right) }
        } ?: legal.first()
    }

    // -------------------------------------------------------------------------
    // Evaluación profunda
    // -------------------------------------------------------------------------

    private fun evaluate(
        state: GameState,
        me: PlayerId,
        tile: Tile,
        end: Board.End?
    ): Double {
        var score = 0.0
        val hand = state.handOf(me)
        val remaining = hand.filter { it != tile }
        val partner = me.partner()
        val myTeam = state.teamOf(me)
        val board = state.board

        // --- Simular nuevo tablero ---
        val newBoard = if (board.isEmpty) board.play(tile) else board.play(tile, end ?: Board.End.LEFT)
        val newLeft = newBoard.leftEnd!!
        val newRight = newBoard.rightEnd!!

        // ===== 1. Lectura de mano propia =====
        val handPoints = hand.sumOf { it.points }
        val isHighHand = handPoints > 48
        score += tile.points * (if (isHighHand) 1.4 else 0.9) // descargar pesadas si mano alta

        if (tile.isDouble) {
            // Dobles en pelo: salir temprano
            val support = hand.count { it.matches(tile.left) && it != tile }
            if (support == 0) score += 18.0
            else score += 8.0 - support * 1.5
            if (hand.size >= 5) score += 6.0
        }

        // ===== 2. Conteo exacto de extremos =====
        val leftUnseen = unseenCount(newLeft, state, me)
        val rightUnseen = unseenCount(newRight, state, me)

        // Dejar un extremo "firm" (0-1 desconocidas) es oro
        if (leftUnseen <= 1) score += 14.0
        if (rightUnseen <= 1) score += 14.0
        if (leftUnseen == 0) score += 8.0
        if (rightUnseen == 0) score += 8.0

        // Matches residuales propios
        val myMatches = remaining.count { it.matches(newLeft) || it.matches(newRight) }
        score += myMatches * 6.0
        if (myMatches == 0 && remaining.isNotEmpty()) score -= 18.0 // suicidio de continuidad

        // ===== 3. Juego de pareja =====
        // No reabrir palo donde el partner pasó
        if (knownVoid(partner, newLeft)) score -= 22.0
        if (knownVoid(partner, newRight)) score -= 22.0

        // Reforzar el palo fuerte del compañero
        partnerStrongSuit?.let { strong ->
            if (newLeft == strong || newRight == strong) score += 12.0
        }

        // Si el partner tiene pocas fichas, protegerlo (extremos bajos)
        val partnerSize = state.handOf(partner).size
        if (partnerSize <= 3) {
            if (newLeft >= 5 || newRight >= 5) score -= 7.0
            if (newLeft <= 2 || newRight <= 2) score += 5.0
        }

        // ===== 4. Matar / controlar rivales =====
        val next = me.next()
        val nextIsRival = state.teamOf(next) != myTeam

        if (nextIsRival) {
            // Si el siguiente rival es void en un extremo, forzarlo a pasar
            if (knownVoid(next, newLeft) || knownVoid(next, newRight)) score += 16.0
            if (leftUnseen <= 1 || rightUnseen <= 1) score += 10.0
        }

        // Matar el palo que un rival ha estado resolviendo
        // (si un rival jugó varias veces el mismo valor, cerrarlo)
        // Simplificación: penalizar dejar extremos donde rivales NO son void y hay muchas desconocidas
        val rivalWest = if (me == PlayerId.SOUTH || me == PlayerId.NORTH) PlayerId.WEST else PlayerId.SOUTH
        val rivalEast = rivalWest.partner()
        if (!knownVoid(rivalWest, newLeft) && leftUnseen >= 3) score -= 4.0
        if (!knownVoid(rivalEast, newRight) && rightUnseen >= 3) score -= 4.0

        // ===== 5. Evaluación de tranca potencial =====
        // Si después de esta jugada es probable un bloqueo, calcular si conviene
        val wouldForcePass = (knownVoid(next, newLeft) && knownVoid(next, newRight)) ||
                (leftUnseen == 0 && rightUnseen == 0 && myMatches == 0)
        if (wouldForcePass && remaining.isNotEmpty()) {
            val myRemainingPts = remaining.sumOf { it.points }
            // Aritmética 168
            val boardPts = state.board.chain.sumOf { it.tile.points } + tile.points
            val totalOthers = 168 - boardPts - myRemainingPts
            val avgOther = totalOthers / 3.0
            if (myRemainingPts < avgOther) score += 20.0 // tranca favorable
            else score -= 12.0 // tranca peligrosa
        }

        // ===== 6. Conciencia de partida (meta) =====
        val usScore = state.scores["us"] ?: 0
        val themScore = state.scores["them"] ?: 0
        val goal = state.config.goal
        val myTeamScore = if (myTeam == "us") usScore else themScore
        val oppTeamScore = if (myTeam == "us") themScore else usScore
        val distUs = goal - myTeamScore
        val distThem = goal - oppTeamScore

        when {
            distUs <= 30 && distThem <= 30 -> {
                // Partida apretada: precisión, no arriesgar
                if (myMatches == 0) score -= 10.0
                score += (7 - tile.points) * 0.5 // preferir fichas bajas
            }
            distUs <= 50 && distThem > 80 -> {
                // Vamos ganando cómodos: no forzar, controlar
                score += 3.0
            }
            distThem <= 40 && distUs > 60 -> {
                // Rivales cerca de meta: bloquear agresivo
                if (knownVoid(next, newLeft) || knownVoid(next, newRight)) score += 12.0
                if (leftUnseen <= 1 || rightUnseen <= 1) score += 8.0
            }
            myTeamScore < 40 && oppTeamScore < 40 -> {
                // Ambos bajos: agresivo, buscar puntos
                score += tile.points * 0.4
            }
        }

        // ===== 7. Conciencia de bonos =====
        // Capicúa potencial: si me queda 1 ficha y los extremos son distintos y mi última ficha los une
        if (remaining.size == 1) {
            val last = remaining.first()
            if (!last.isDouble && last.matches(newLeft) && last.matches(newRight) && newLeft != newRight) {
                score += 50.0 // Capicúa casi segura
            }
            if (remaining.isEmpty()) score += 100.0 // Dominó inmediato (no debería llegar aquí)
        }
        if (remaining.isEmpty()) score += 200.0 // Dominó

        // Paso Redondo: si consecutivePasses >= 2 y esta jugada cierra
        if (state.consecutivePasses >= 2 && remaining.isEmpty()) {
            score += 40.0
        }

        // ===== Personalidad =====
        when (personality) {
            Personality.AGGRESSIVE -> {
                score += tile.points * 0.5
                if (tile.isDouble) score += 5.0
                if (knownVoid(next, newLeft) || knownVoid(next, newRight)) score += 6.0
            }
            Personality.DEFENSIVE -> {
                if (myMatches == 0) score -= 20.0
                score += myMatches * 4.0
                if (leftUnseen <= 1 && rightUnseen <= 1) score += 8.0
            }
            Personality.BALANCED -> {}
        }

        return score
    }

    /** Reiniciar memoria entre manos (llamar desde ViewModel al dealNewHand). */
    fun resetHandMemory() {
        passMemory.clear()
        partnerStrongSuit = null
        partnerFirstTileSeen = false
    }
}
