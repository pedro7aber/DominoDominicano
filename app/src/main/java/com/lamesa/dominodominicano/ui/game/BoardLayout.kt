package com.lamesa.dominodominicano.ui.game

import com.lamesa.dominodominicano.domain.Board
import com.lamesa.dominodominicano.domain.PlayerId
import com.lamesa.dominodominicano.domain.Tile

/**
 * Layout ESTABLE e impoluto:
 * - Ficha de salida anclada en (0,0)
 * - Al añadir fichas, las ya colocadas NO se mueven
 * - Matching: leftIsLeft orienta el número correcto hacia el vecino
 * - Tamaño uniforme
 * - Dobles transversales
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
        /** Si true, la mitad "left" del dibujo está hacia -X (izquierda de la cadena). */
        val leftIsLeft: Boolean
    )

    fun layout(board: Board, tableWidth: Float, tableHeight: Float): List<PlacedTile> {
        if (board.isEmpty) return emptyList()
        val chain = board.chain
        val starterIdx = board.starterIndex.coerceIn(0, chain.lastIndex)

        // Paso uniforme: cada ficha ocupa el mismo espacio horizontal
        val step = TILE_W + GAP

        return chain.mapIndexed { i, ot ->
            // Offset relativo al starter: negativo a la izquierda, positivo a la derecha
            val offset = (i - starterIdx).toFloat()
            val x = offset * step
            val y = 0f
            val rot = if (ot.isDouble) 90f else 0f
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
