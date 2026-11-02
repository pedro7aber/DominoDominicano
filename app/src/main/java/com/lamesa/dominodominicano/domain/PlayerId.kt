package com.lamesa.dominodominicano.domain

/**
 * Identificadores de los 4 jugadores.
 * South = humano (Tú)
 * North = Compañero
 * West = El de arriba
 * East = El de abajo
 *
 * Sentido de juego: antihorario = South -> West -> North -> East -> South
 */
enum class PlayerId {
    SOUTH, // Tú (humano)
    WEST,  // El de arriba (rival)
    NORTH, // Compañero
    EAST;  // El de abajo (rival)

    fun next(): PlayerId = when (this) {
        SOUTH -> WEST
        WEST -> NORTH
        NORTH -> EAST
        EAST -> SOUTH
    }

    fun previous(): PlayerId = when (this) {
        SOUTH -> EAST
        WEST -> SOUTH
        NORTH -> WEST
        EAST -> NORTH
    }

    fun partner(): PlayerId = when (this) {
        SOUTH -> NORTH
        NORTH -> SOUTH
        WEST -> EAST
        EAST -> WEST
    }

    fun isHuman(): Boolean = this == SOUTH
    fun isPartnerOfHuman(): Boolean = this == NORTH
    fun isRival(): Boolean = this == WEST || this == EAST
}
