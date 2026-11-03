package com.lamesa.dominodominicano.ui.game

import com.lamesa.dominodominicano.domain.Board
import com.lamesa.dominodominicano.domain.PlayerId
import com.lamesa.dominodominicano.domain.Tile
import kotlin.math.abs

/**
 * Layout dominicano completo y estable:
 * - Salida anclada en (0,0)
 * - Posiciones fijas relativas al starter (no recentrar)
 * - Rama izquierda (LEFT arm) = Norte → este → sur (giros a 2 fichas horizontales)
 * - Rama derecha (RIGHT arm) = Sur → oeste → norte
 * - Matching leftIsLeft; dobles transversales; tamaño uniforme; sin solapes
 */
object BoardLayout {

    const val TILE_W = 36f
    const val TILE_H = 64f
    const val GAP = 2f
    private const val SECOND_TURN_AFTER = 2

    data class PlacedTile(
        val tile: Tile,
        val isDouble: Boolean,
        val x: Float,
        val y: Float,
        val rotationDeg: Float,
        val playedBy: PlayerId?,
        val leftIsLeft: Boolean,
        /** Valor dibujado en la mitad "hacia el centro/prev" y "hacia fuera". */
        val nearValue: Int,
        val farValue: Int
    )

    private enum class Dir { N, S, E, W }

    fun layout(board: Board, tableWidth: Float, tableHeight: Float): List<PlacedTile> {
        if (board.isEmpty) return emptyList()
        val chain = board.chain
        val si = board.starterIndex.coerceIn(0, chain.lastIndex)
        val result = mutableListOf<PlacedTile>()

        // --- Starter en (0,0) ---
        val st = chain[si]
        result.add(place(st, 0f, 0f, if (st.isDouble) 90f else 0f))

        val marginX = tableWidth * 0.40f
        val marginY = tableHeight * 0.36f
        val stepLong = TILE_H + GAP
        val stepShort = TILE_W + GAP

        // Brazo izquierdo = índices si-1, si-2, ... (hacia leftEnd) = rama Norte
        placeArm(
            tiles = (si - 1 downTo 0).map { chain[it] },
            startX = 0f, startY = 0f,
            initial = Dir.N,
            isNorth = true,
            marginX = marginX, marginY = marginY,
            stepLong = stepLong, stepShort = stepShort,
            into = result
        )

        // Brazo derecho = índices si+1 ... = rama Sur
        placeArm(
            tiles = ((si + 1) until chain.size).map { chain[it] },
            startX = 0f, startY = 0f,
            initial = Dir.S,
            isNorth = false,
            marginX = marginX, marginY = marginY,
            stepLong = stepLong, stepShort = stepShort,
            into = result
        )

        return result
    }

    private fun placeArm(
        tiles: List<Board.OrientedTile>,
        startX: Float,
        startY: Float,
        initial: Dir,
        isNorth: Boolean,
        marginX: Float,
        marginY: Float,
        stepLong: Float,
        stepShort: Float,
        into: MutableList<PlacedTile>
    ) {
        var dir = initial
        var cx = startX
        var cy = startY
        var turns = 0
        var sinceTurn = 0

        for (ot in tiles) {
            val step = if (ot.isDouble) stepShort else stepLong
            when (dir) {
                Dir.N -> cy -= step
                Dir.S -> cy += step
                Dir.E -> cx += step
                Dir.W -> cx -= step
            }

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
            sinceTurn++

            if (turns == 0) {
                val nearEdge = when (dir) {
                    Dir.N -> cy <= -marginY
                    Dir.S -> cy >= marginY
                    else -> false
                }
                if (nearEdge) {
                    dir = if (isNorth) Dir.E else Dir.W
                    turns = 1
                    sinceTurn = 0
                }
            } else if (turns == 1 && sinceTurn >= SECOND_TURN_AFTER) {
                dir = if (isNorth) Dir.S else Dir.N
                turns = 2
                sinceTurn = 0
            }
        }
    }

    private fun place(ot: Board.OrientedTile, x: Float, y: Float, rot: Float): PlacedTile {
        val near = if (ot.leftIsLeft) ot.tile.left else ot.tile.right
        val far = if (ot.leftIsLeft) ot.tile.right else ot.tile.left
        return PlacedTile(
            tile = ot.tile,
            isDouble = ot.isDouble,
            x = x,
            y = y,
            rotationDeg = rot,
            playedBy = ot.playedBy,
            leftIsLeft = ot.leftIsLeft,
            nearValue = near,
            farValue = far
        )
    }
}
