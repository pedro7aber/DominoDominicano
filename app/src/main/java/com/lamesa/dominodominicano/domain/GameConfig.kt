package com.lamesa.dominodominicano.domain

data class GameConfig(
    val goal: Int = 200, // 100,150,200,250,300,350,400
    val countAllTiles: Boolean = true, // true = todas, false = solo rivales
    val bonusValue: Int = 25, // 25 o 30
    val chuchazoEnabled: Boolean = false,
    val tranqueIndividual: Boolean = true // true = individual, false = por parejas
) {
    companion object {
        val VALID_GOALS = listOf(100, 150, 200, 250, 300, 350, 400)
        fun isValidGoal(g: Int) = g in VALID_GOALS
    }
}
