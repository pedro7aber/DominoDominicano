package com.lamesa.dominodominicano.ui.game

import com.lamesa.dominodominicano.domain.Board
import com.lamesa.dominodominicano.domain.PlayerId
import com.lamesa.dominodominicano.domain.Tile

/**
 * Layout ESTABLE: cada ficha tiene posición fija según su índice en la cadena.
 * No se recalculan ni mueven las fichas ya colocadas.
 * Tamaño UNIFORME para todas las fichas.
 * Conexión correcta: se tocan solo por el extremo que matchea.
 */
object BoardLayout {

    // Tamaño uniforme de ficha (px lógicos)
    const val TILE_W = 36f   // ancho (lado corto cuando vertical)
    const val TILE_H = 64f   // alto (lado largo cuando vertical)
    const val GAP = 1.5f

    data class PlacedTile(
        val tile: Tile,
        val isDouble: Boolean,
        val x: Float,
        val y: Float,
        val rotationDeg: Float, // 0 = vertical, 90 = horizontal (doble transversal)
        val playedBy: PlayerId?
    )

    /**
     * Layout lineal estable centrado.
     * Cadena de izquierda a derecha. Dobles transversales.
     * Las posiciones dependen solo del índice → no se mueven al añadir fichas.
     */
    fun layout(board: Board, tableWidth: Float, tableHeight: Float): List<PlacedTile> {
        if (board.isEmpty) return emptyList()
        val chain = board.chain
        val n = chain.size

        // Espacio por ficha (uniforme)
        val step = TILE_W + GAP
        val totalWidth = n * step
        val startX = -totalWidth / 2f + step / 2f

        return chain.mapIndexed { i, ot ->
            val x = startX + i * step
            val y = 0f
            val rot = if (ot.isDouble) 90f else 0f
            PlacedTile(
                tile = ot.tile,
                isDouble = ot.isDouble,
                x = x,
                y = y,
                rotationDeg = rot,
                playedBy = ot.playedBy
            )
        }
    }
}
