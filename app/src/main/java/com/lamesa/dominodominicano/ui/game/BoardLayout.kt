package com.lamesa.dominodominicano.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lamesa.dominodominicano.domain.Board
import com.lamesa.dominodominicano.domain.Tile
import kotlin.math.cos
import kotlin.math.sin

/**
 * Layout realista de Dominó Dominicano.
 *
 * Reglas:
 * 1. La ficha de salida se coloca SIEMPRE en el centro.
 *    Si es doble → transversal. Exactamente 2 direcciones.
 * 2. Rama Norte: norte → giro 90° este en borde superior →
 *    segundo giro 90° después de 2 fichas horizontales → apunta sur.
 * 3. Rama Sur: sur → giro 90° oeste en borde inferior →
 *    segundo giro 90° después de 2 fichas horizontales → apunta norte.
 * 4. Sin solapamiento. Solo se tocan en los extremos correctos.
 */
object BoardLayout {

    data class PlacedTile(
        val tile: Tile,
        val isDouble: Boolean,
        val x: Float,          // centro de la ficha (px relativos al centro de mesa)
        val y: Float,
        val rotationDeg: Float // 0 = vertical (norte-sur), 90 = horizontal (este-oeste)
    )

    // Tamaños base (dp convertidos a px por el caller)
    val TILE_SHORT = 28f   // lado corto de una ficha normal
    val TILE_LONG = 52f    // lado largo
    val GAP = 2f           // separación mínima entre extremos que se tocan

    /**
     * Calcula las posiciones de todas las fichas de la cadena.
     * @param board la cadena actual
     * @param tableWidth ancho usable de la mesa (px)
     * @param tableHeight alto usable de la mesa (px)
     */
    fun layout(
        board: Board,
        tableWidth: Float,
        tableHeight: Float
    ): List<PlacedTile> {
        if (board.isEmpty) return emptyList()

        val result = mutableListOf<PlacedTile>()
        val chain = board.chain

        // --- Ficha de salida (índice 0) en el centro ---
        val starter = chain[0]
        val starterRot = if (starter.isDouble) 90f else 0f
        result.add(
            PlacedTile(
                tile = starter.tile,
                isDouble = starter.isDouble,
                x = 0f,
                y = 0f,
                rotationDeg = starterRot
            )
        )

        if (chain.size == 1) return result

        // La cadena se guarda como [left ... starter ... right]
        // leftEnd está en chain.first(), rightEnd en chain.last()
        // Pero el starter es el primero que se jugó.
        // En nuestro Board, play LEFT inserta al inicio y play RIGHT al final.
        // Por tanto: chain[0] es el extremo izquierdo actual, chain.last() el derecho.
        // Necesitamos saber dónde está el starter dentro de la cadena.

        // Simplificación robusta para la primera versión:
        // Tratamos la cadena como dos brazos a partir del starter.
        // Como el starter siempre es el primero jugado, y luego se añade
        // a izquierda o derecha, reconstruimos los dos brazos.

        // En la implementación actual del Board, el starter queda en alguna posición.
        // Para esta versión usamos un layout tipo "dos brazos desde el centro"
        // donde las fichas de la mitad izquierda van a la rama Norte
        // y las de la mitad derecha a la rama Sur (o viceversa según el extremo).

        val mid = chain.indexOfFirst { it.tile == starter.tile && it.isDouble == starter.isDouble }
            .coerceAtLeast(0)

        val leftArm = chain.subList(0, mid).reversed()   // desde el starter hacia leftEnd
        val rightArm = if (mid + 1 < chain.size) chain.subList(mid + 1, chain.size) else emptyList()

        // Rama Norte = leftArm (sale hacia norte)
        placeArm(
            arm = leftArm,
            startX = 0f,
            startY = 0f,
            initialDir = Direction.NORTH,
            tableWidth = tableWidth,
            tableHeight = tableHeight,
            isNorthBranch = true,
            into = result
        )

        // Rama Sur = rightArm (sale hacia sur)
        placeArm(
            arm = rightArm,
            startX = 0f,
            startY = 0f,
            initialDir = Direction.SOUTH,
            tableWidth = tableWidth,
            tableHeight = tableHeight,
            isNorthBranch = false,
            into = result
        )

        return result
    }

    private enum class Direction { NORTH, SOUTH, EAST, WEST }

    private fun placeArm(
        arm: List<Board.OrientedTile>,
        startX: Float,
        startY: Float,
        initialDir: Direction,
        tableWidth: Float,
        tableHeight: Float,
        isNorthBranch: Boolean,
        into: MutableList<PlacedTile>
    ) {
        if (arm.isEmpty()) return

        var dir = initialDir
        var cx = startX
        var cy = startY
        var tilesSinceLastTurn = 0
        var turnCount = 0

        // Margen interior para no tocar el borde de la mesa
        val marginX = tableWidth * 0.42f
        val marginY = tableHeight * 0.38f

        // Distancia del segundo giro = 2 fichas horizontales
        val secondTurnAfter = 2

        for (ot in arm) {
            val step = if (ot.isDouble) TILE_SHORT + GAP else TILE_LONG + GAP

            // Avanzar en la dirección actual
            when (dir) {
                Direction.NORTH -> cy -= step
                Direction.SOUTH -> cy += step
                Direction.EAST  -> cx += step
                Direction.WEST  -> cx -= step
            }

            // Rotación visual
            val rot = when {
                ot.isDouble -> when (dir) {
                    Direction.NORTH, Direction.SOUTH -> 90f  // transversal
                    Direction.EAST, Direction.WEST   -> 0f
                }
                else -> when (dir) {
                    Direction.NORTH, Direction.SOUTH -> 0f
                    Direction.EAST, Direction.WEST   -> 90f
                }
            }

            into.add(
                PlacedTile(
                    tile = ot.tile,
                    isDouble = ot.isDouble,
                    x = cx,
                    y = cy,
                    rotationDeg = rot
                )
            )

            tilesSinceLastTurn++

            // Lógica de giros
            if (turnCount == 0) {
                // Primer giro: al acercarse al borde
                val nearEdge = when (dir) {
                    Direction.NORTH -> cy <= -marginY
                    Direction.SOUTH -> cy >= marginY
                    else -> false
                }
                if (nearEdge) {
                    dir = if (isNorthBranch) Direction.EAST else Direction.WEST
                    turnCount = 1
                    tilesSinceLastTurn = 0
                }
            } else if (turnCount == 1 && tilesSinceLastTurn >= secondTurnAfter) {
                // Segundo giro: después de 2 fichas horizontales
                dir = if (isNorthBranch) Direction.SOUTH else Direction.NORTH
                turnCount = 2
                tilesSinceLastTurn = 0
            }
        }
    }
}
