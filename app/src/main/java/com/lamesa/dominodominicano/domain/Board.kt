package com.lamesa.dominodominicano.domain

/**
 * Cadena de fichas en la mesa.
 * leftEnd / rightEnd = extremos abiertos.
 */
data class Board(
    val chain: List<OrientedTile> = emptyList(),
    val leftEnd: Int? = null,
    val rightEnd: Int? = null
) {
    val isEmpty: Boolean get() = chain.isEmpty()

    data class OrientedTile(
        val tile: Tile,
        val isDouble: Boolean,
        val leftIsLeft: Boolean = true
    )

    fun canPlay(tile: Tile): Boolean {
        if (isEmpty) return true
        return tile.matches(leftEnd!!) || tile.matches(rightEnd!!)
    }

    fun legalEnds(tile: Tile): List<End> {
        if (isEmpty) return listOf(End.LEFT)
        val ends = mutableListOf<End>()
        if (tile.matches(leftEnd!!)) ends.add(End.LEFT)
        if (tile.matches(rightEnd!!)) {
            // Si ambos extremos son el mismo número, jugar en LEFT o RIGHT es equivalente;
            // solo añadimos RIGHT si es distinto o si aún no está LEFT.
            if (leftEnd != rightEnd || End.LEFT !in ends) {
                ends.add(End.RIGHT)
            }
        }
        return ends
    }

    enum class End { LEFT, RIGHT }

    fun play(tile: Tile, end: End = End.LEFT): Board {
        if (isEmpty) {
            return Board(
                chain = listOf(OrientedTile(tile, tile.isDouble, true)),
                leftEnd = tile.left,
                rightEnd = tile.right
            )
        }

        return when (end) {
            End.LEFT -> {
                val matching = if (tile.left == leftEnd) tile.left else tile.right
                val other = tile.other(matching)
                val oriented = OrientedTile(tile, tile.isDouble, tile.left == matching)
                Board(
                    chain = listOf(oriented) + chain,
                    leftEnd = other,
                    rightEnd = rightEnd
                )
            }
            End.RIGHT -> {
                val matching = if (tile.left == rightEnd) tile.left else tile.right
                val other = tile.other(matching)
                val oriented = OrientedTile(tile, tile.isDouble, tile.left != matching)
                Board(
                    chain = chain + listOf(oriented),
                    leftEnd = leftEnd,
                    rightEnd = other
                )
            }
        }
    }
}
