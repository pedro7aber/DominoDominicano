package com.lamesa.dominodominicano.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lamesa.dominodominicano.data.SettingsViewModel
import com.lamesa.dominodominicano.ui.game.GameScreen

@Composable
fun AppNavHost(viewModel: SettingsViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onPlay = { navController.navigate("game") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("game") {
            GameScreen(
                onMenu = { navController.popBackStack() },
                onRules = { }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
