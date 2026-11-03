package com.lamesa.dominodominicano.domain

/**
 * Cadena con starterIndex fijo para layout estable.
 * LEFT prepende (índice 0); RIGHT appende.
 * starterIndex se incrementa en cada play LEFT para que la ficha de salida
 * mantenga su identidad en la cadena.
 */
data class Board(
    val chain: List<OrientedTile> = emptyList(),
    val leftEnd: Int? = null,
    val rightEnd: Int? = null,
    /** Índice de la ficha de salida dentro de chain. Nunca se recentra el layout. */
    val starterIndex: Int = 0
) {
    val isEmpty: Boolean get() = chain.isEmpty()

    data class OrientedTile(
        val tile: Tile,
        val isDouble: Boolean,
        /** true si tile.left apunta hacia el extremo izquierdo de la cadena */
        val leftIsLeft: Boolean = true,
        val playedBy: PlayerId? = null
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
            if (leftEnd != rightEnd || End.LEFT !in ends) ends.add(End.RIGHT)
        }
        return ends
    }

    enum class End { LEFT, RIGHT }

    fun play(tile: Tile, end: End = End.LEFT, playedBy: PlayerId? = null): Board {
        if (isEmpty) {
            return Board(
                chain = listOf(OrientedTile(tile, tile.isDouble, true, playedBy)),
                leftEnd = tile.left,
                rightEnd = tile.right,
                starterIndex = 0
            )
        }
        return when (end) {
            End.LEFT -> {
                val matching = if (tile.left == leftEnd) tile.left else tile.right
                val other = tile.other(matching)
                // leftIsLeft = true si el lado izquierdo de la ficha (en la cadena) es tile.left
                // Al prepender: el extremo que toca la cadena es matching, y queda hacia la DERECHA de esta ficha
                // Así leftIsLeft = (tile.left != matching) → el left de la ficha queda hacia fuera (izquierda)
                val leftIsLeft = tile.left != matching
                val oriented = OrientedTile(tile, tile.isDouble, leftIsLeft, playedBy)
                Board(
                    chain = listOf(oriented) + chain,
                    leftEnd = other,
                    rightEnd = rightEnd,
                    starterIndex = starterIndex + 1
                )
            }
            End.RIGHT -> {
                val matching = if (tile.left == rightEnd) tile.left else tile.right
                val other = tile.other(matching)
                // Al appender: matching toca la cadena por la IZQUIERDA de esta ficha
                val leftIsLeft = tile.left == matching
                val oriented = OrientedTile(tile, tile.isDouble, leftIsLeft, playedBy)
                Board(
                    chain = chain + listOf(oriented),
                    leftEnd = leftEnd,
                    rightEnd = other,
                    starterIndex = starterIndex
                )
            }
        }
    }
}
