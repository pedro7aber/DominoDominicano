package com.lamesa.dominodominicano.ui.game

import com.lamesa.dominodominicano.domain.Board
import com.lamesa.dominodominicano.domain.PlayerId
import com.lamesa.dominodominicano.domain.Tile
import kotlin.math.abs

/**
 * Layout estable por CONTEO de fichas (no solo margen de pantalla).
 * Starter (0,0). Rama N/S con giro1 por conteo+borde, giro2 tras 2 horizontales + marginX.
 */
object BoardLayout {

    const val TILE_W = 36f
    const val TILE_H = 64f
    const val GAP = 2f
    private const val SECOND_TURN_AFTER = 2
    /** Mínimo de fichas en dirección inicial antes de permitir primer giro por borde. */
    private const val MIN_BEFORE_FIRST_TURN = 2

    data class PlacedTile(
        val tile: Tile,
        val isDouble: Boolean,
        val x: Float,
        val y: Float,
        val rotationDeg: Float,
        val playedBy: PlayerId?,
        val leftIsLeft: Boolean,
        val nearValue: Int,
        val farValue: Int
    )

    private enum class Dir { N, S, E, W }

    fun layout(board: Board, tableWidth: Float, tableHeight: Float): List<PlacedTile> {
        if (board.isEmpty) return emptyList()
        val chain = board.chain
        val si = board.starterIndex.coerceIn(0, chain.lastIndex)
        val result = mutableListOf<PlacedTile>()

        val st = chain[si]
        result.add(place(st, 0f, 0f, if (st.isDouble) 90f else 0f))

        // Ratios fijos → mismo camino para el mismo índice de ficha (L2 estable)
        val marginX = tableWidth * 0.40f
        val marginY = tableHeight * 0.36f
        val stepLong = TILE_H + GAP
        val stepShort = TILE_W + GAP

        placeArm(
            tiles = (si - 1 downTo 0).map { chain[it] },
            initial = Dir.N, isNorth = true,
            marginX = marginX, marginY = marginY,
            stepLong = stepLong, stepShort = stepShort,
            into = result
        )
        placeArm(
            tiles = ((si + 1) until chain.size).map { chain[it] },
            initial = Dir.S, isNorth = false,
            marginX = marginX, marginY = marginY,
            stepLong = stepLong, stepShort = stepShort,
            into = result
        )
        return result
    }

    private fun placeArm(
        tiles: List<Board.OrientedTile>,
        initial: Dir,
        isNorth: Boolean,
        marginX: Float,
        marginY: Float,
        stepLong: Float,
        stepShort: Float,
        into: MutableList<PlacedTile>
    ) {
        var dir = initial
        var cx = 0f
        var cy = 0f
        var turns = 0
        var sinceTurn = 0
        var tilesInArm = 0

        for (ot in tiles) {
            val step = if (ot.isDouble) stepShort else stepLong
            when (dir) {
                Dir.N -> cy -= step
                Dir.S -> cy += step
                Dir.E -> cx += step
                Dir.W -> cx -= step
            }
            tilesInArm++
            sinceTurn++

            val rot = when {
                ot.isDouble -> when (dir) {
                    Dir.N, Dir.S -> 90f
                    Dir.E, Dir.W -> 0f
                }
                else -> when (dir) {
                    Dir.N, Dir.S -> 0f
                    Dir.E, Dir.W -> 90f
                }
            }
            into.add(place(ot, cx, cy, rot))

            // Giro 1: tras MIN fichas Y (borde Y o conteo alto)
            if (turns == 0 && tilesInArm >= MIN_BEFORE_FIRST_TURN) {
                val nearEdge = when (dir) {
                    Dir.N -> cy <= -marginY
                    Dir.S -> cy >= marginY
                    else -> false
                }
                // También girar tras 4 fichas en línea para estabilidad independiente de resolución
                if (nearEdge || tilesInArm >= 4) {
                    dir = if (isNorth) Dir.E else Dir.W
                    turns = 1
                    sinceTurn = 0
                }
            } else if (turns == 1 && sinceTurn >= SECOND_TURN_AFTER) {
                // Giro 2: tras 2 horizontales Y (opcional) no pasar marginX en exceso
                val pastSide = abs(cx) >= marginX * 0.85f
                if (sinceTurn >= SECOND_TURN_AFTER || pastSide) {
                    dir = if (isNorth) Dir.S else Dir.N
                    turns = 2
                    sinceTurn = 0
                }
            }
        }
    }

    private fun place(ot: Board.OrientedTile, x: Float, y: Float, rot: Float): PlacedTile {
        // near = hacia el starter (conexión); far = extremo libre de esa ficha en el brazo
        val near = if (ot.leftIsLeft) ot.tile.left else ot.tile.right
        val far = if (ot.leftIsLeft) ot.tile.right else ot.tile.left
        return PlacedTile(
            tile = ot.tile,
            isDouble = ot.isDouble,
            x = x, y = y,
            rotationDeg = rot,
            playedBy = ot.playedBy,
            leftIsLeft = ot.leftIsLeft,
            nearValue = near,
            farValue = far
        )
    }
}
