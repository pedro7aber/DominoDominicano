package com.lamesa.dominodominicano.domain

/**
 * Ficha de dominó doble-seis. Valores de 0 a 6.
 * left y right son los valores de cada extremo.
 * Para dobles, left == right.
 */
data class Tile(
    val left: Int,
    val right: Int
) : Comparable<Tile> {
    init {
        require(left in 0..6 && right in 0..6) { "Valores de ficha deben estar entre 0 y 6" }
    }

    val isDouble: Boolean get() = left == right
    val points: Int get() = left + right
    val high: Int get() = maxOf(left, right)
    val low: Int get() = minOf(left, right)

    fun matches(value: Int): Boolean = left == value || right == value
    fun other(value: Int): Int = if (left == value) right else left

    /** Orientación canónica para comparación (menor primero). */
    fun canonical(): Tile = if (left <= right) this else Tile(right, left)

    override fun compareTo(other: Tile): Int {
        val c1 = this.canonical()
        val c2 = other.canonical()
        return when {
            c1.left != c2.left -> c1.left - c2.left
            else -> c1.right - c2.right
        }
    }

    override fun toString(): String = "[$left|$right]"

    companion object {
        /** Genera el set completo de 28 fichas. */
        fun fullSet(): List<Tile> {
            val tiles = mutableListOf<Tile>()
            for (i in 0..6) {
                for (j in i..6) {
                    tiles.add(Tile(i, j))
                }
            }
            return tiles
        }
    }
}
