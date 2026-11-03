package com.lamesa.dominodominicano.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lamesa.dominodominicano.data.SettingsViewModel
import com.lamesa.dominodominicano.domain.GameConfig
import com.lamesa.dominodominicano.ui.game.GameScreen
import com.lamesa.dominodominicano.ui.game.GameViewModel

@Composable
fun AppNavHost(viewModel: SettingsViewModel) {
    val navController = rememberNavController()
    var showRules by remember { mutableStateOf(false) }
    val gameViewModel: GameViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onPlay = { navController.navigate("config") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("config") {
            GameConfigScreen(
                onStart = { config ->
                    gameViewModel.startNewGame(config)
                    navController.navigate("game") {
                        popUpTo("home")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("game") {
            GameScreen(
                onMenu = { navController.popBackStack("home", inclusive = false) },
                onRules = { showRules = true },
                viewModel = gameViewModel
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }

    if (showRules) {
        AlertDialog(
            onDismissRequest = { showRules = false },
            title = { Text("Reglas Dominó Dominicano v3") },
            text = {
                Text(
                    "• 4 jugadores, 2 parejas fijas.\n" +
                    "• 28 fichas, 7 por jugador, sin pozo.\n" +
                    "• Sentido antihorario.\n" +
                    "• Primera mano sale el que tiene el 6-6.\n" +
                    "• Obligatorio jugar si se tiene ficha legal.\n" +
                    "• Dobles se colocan transversales.\n" +
                    "• Bonos: Capicúa, Paso Redondo, Pase de Salida, Chuchazo.\n" +
                    "• Tranque: individual o por parejas.\n" +
                    "• Meta configurable (100-400)."
                )
            },
            confirmButton = {
                TextButton(onClick = { showRules = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}
