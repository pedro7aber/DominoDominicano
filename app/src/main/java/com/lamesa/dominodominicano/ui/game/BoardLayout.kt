package com.lamesa.dominodominicano.ui.game

import com.lamesa.dominodominicano.domain.Board
import com.lamesa.dominodominicano.domain.PlayerId
import com.lamesa.dominodominicano.domain.Tile

/**
 * Layout ESTABLE:
 * - Salida anclada en (0,0) via starterIndex
 * - Fichas ya colocadas no se mueven al crecer la cadena
 * - No-dobles alineados al eje de la cadena (rot 90)
 * - Dobles transversales (rot 0)
 * - leftIsLeft orienta el numero correcto hacia el vecino
 */
object BoardLayout {

    const val TILE_W = 36f
    const val TILE_H = 64f
    const val GAP = 1.5f

    data class PlacedTile(
        val tile: Tile,
        val isDouble: Boolean,
        val x: Float,
        val y: Float,
        val rotationDeg: Float,
        val playedBy: PlayerId?,
        val leftIsLeft: Boolean
    )

    fun layout(board: Board, tableWidth: Float, tableHeight: Float): List<PlacedTile> {
        if (board.isEmpty) return emptyList()
        val chain = board.chain
        val starterIdx = board.starterIndex.coerceIn(0, chain.lastIndex)
        // Paso: longitud de ficha a lo largo de la cadena + gap
        val step = TILE_H + GAP // no-doble horizontal usa TILE_H como largo a lo largo del eje

        return chain.mapIndexed { i, ot ->
            val offset = (i - starterIdx).toFloat()
            val x = offset * step
            val y = 0f
            // Cadena horizontal: no-doble se acuesta (90), doble queda transversal (0)
            val rot = if (ot.isDouble) 0f else 90f
            PlacedTile(
                tile = ot.tile,
                isDouble = ot.isDouble,
                x = x,
                y = y,
                rotationDeg = rot,
                playedBy = ot.playedBy,
                leftIsLeft = ot.leftIsLeft
            )
        }
    }
}
