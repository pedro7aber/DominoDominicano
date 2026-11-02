package com.lamesa.dominodominicano.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lamesa.dominodominicano.domain.*
import com.lamesa.dominodominicano.ui.components.DominoTileView

@Composable
fun GameScreen(
    onMenu: () -> Unit = {},
    onRules: () -> Unit = {},
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val selected by viewModel.selectedTile.collectAsState()

    val bgDark = Color(0xFF0D1F0D)
    val wood = Color(0xFF8B5A2B)
    val woodDark = Color(0xFF5D3A1A)
    val felt = Color(0xFF1B5E20)
    val goldBorder = Color(0xFFC9A227)
    val labelBg = Color(0xFF2E7D32)

    // Asegurar que si el turno no es del humano, la IA juegue
    LaunchedEffect(state.currentPlayer, state.phase) {
        if (state.phase == GameState.Phase.PLAYING && !state.currentPlayer.isHuman()) {
            // El ViewModel ya programa la IA, esto es refuerzo visual
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {

            // === TOP BAR ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onMenu,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) { Text("Menú", color = Color.White, fontSize = 13.sp) }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = Color(0xFF1B3A1B),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, goldBorder)
                    ) {
                        Text(
                            "Mano ${state.handNumber} · Nosotros ${state.scores["us"] ?: 0} · Ellos ${state.scores["them"] ?: 0}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    when {
                        state.phase == GameState.Phase.PLAYING && state.currentPlayer == PlayerId.SOUTH -> {
                            Surface(color = Color(0xFF1565C0), shape = RoundedCornerShape(12.dp)) {
                                Text("▶ Tu turno", modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp), color = Color.White, fontSize = 11.sp)
                            }
                        }
                        state.phase == GameState.Phase.PLAYING -> {
                            Surface(color = Color(0xFF5D4037), shape = RoundedCornerShape(12.dp)) {
                                Text("Pensando...", modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp), color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Button(
                    onClick = onRules,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC9A227)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) { Text("Regla", color = Color.Black, fontSize = 13.sp) }
            }

            Spacer(Modifier.height(8.dp))

            // === COMPAÑERO ===
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(state.handOf(PlayerId.NORTH).size.coerceAtMost(7)) {
                        DominoTileView(tile = Tile(0, 0), width = 28.dp, height = 48.dp, faceUp = false)
                    }
                }
                Surface(color = labelBg, shape = RoundedCornerShape(10.dp)) {
                    Text("Compañero", modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp), color = Color.White, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(6.dp))

            // === MESA + LATERALES ===
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // West
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(color = Color(0xFFFFA000), shape = RoundedCornerShape(6.dp)) {
                        Text("El de\narriba", modifier = Modifier.padding(4.dp, 2.dp), color = Color.Black, fontSize = 9.sp, lineHeight = 11.sp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(state.handOf(PlayerId.WEST).size.coerceAtMost(7)) {
                            DominoTileView(tile = Tile(0, 0), width = 36.dp, height = 22.dp, faceUp = false)
                        }
                    }
                }

                // Board
                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .fillMaxHeight(0.85f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(wood, woodDark, wood)))
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp))
                                .background(felt)
                                .border(1.dp, Color(0xFF0D3B0D), RoundedCornerShape(4.dp))
                        ) {
                            if (state.board.isEmpty) {
                                Text("Esperando salida...", color = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.align(Alignment.Center), fontSize = 13.sp)
                            } else {
                                Row(
                                    modifier = Modifier.align(Alignment.Center).padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    state.board.chain.take(12).forEach { ot ->
                                        DominoTileView(
                                            tile = ot.tile,
                                            width = if (ot.isDouble) 34.dp else 26.dp,
                                            height = if (ot.isDouble) 26.dp else 46.dp,
                                            faceUp = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // East
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(color = Color(0xFFE53935), shape = RoundedCornerShape(6.dp)) {
                        Text("El de\nabajo", modifier = Modifier.padding(4.dp, 2.dp), color = Color.White, fontSize = 9.sp, lineHeight = 11.sp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(state.handOf(PlayerId.EAST).size.coerceAtMost(7)) {
                            DominoTileView(tile = Tile(0, 0), width = 36.dp, height = 22.dp, faceUp = false)
                        }
                    }
                }
            }

            // Label Tú
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(color = Color(0xFF1565C0), shape = RoundedCornerShape(10.dp)) {
                    Text("Tú", modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp), color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(4.dp))

            // === MANO DEL JUGADOR ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3E2723), RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.handOf(PlayerId.SOUTH).forEach { tile ->
                        val isSelected = selected == tile
                        val isLegal = viewModel.isLegal(tile)
                        DominoTileView(
                            tile = tile,
                            width = 42.dp,
                            height = 76.dp,
                            selected = isSelected,
                            faceUp = true,
                            onClick = {
                                if (state.currentPlayer == PlayerId.SOUTH && state.phase == GameState.Phase.PLAYING) {
                                    if (isLegal) {
                                        // Tap en ficha legal → seleccionar o jugar si ya estaba seleccionada
                                        if (isSelected) {
                                            viewModel.playTile(tile)
                                        } else {
                                            viewModel.selectTile(tile)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Botón JUGAR cuando hay ficha seleccionada
            if (selected != null && state.currentPlayer == PlayerId.SOUTH && state.phase == GameState.Phase.PLAYING) {
                Button(
                    onClick = { viewModel.playSelected() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("JUGAR ${selected?.left}-${selected?.right}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Mensaje de estado
            if (state.message != null) {
                Text(
                    state.message ?: "",
                    color = Color(0xFFFFF59D),
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Controles de fin de mano / partida
            when (state.phase) {
                GameState.Phase.HAND_OVER -> {
                    Button(
                        onClick = { viewModel.nextHand() },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) { Text("Siguiente mano") }
                }
                GameState.Phase.GAME_OVER -> {
                    Text(
                        "¡Fin de la partida! Ganador: ${if (state.winnerOfGame == "us") "Nosotros" else "Ellos"}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.startNewGame() },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) { Text("Nueva Partida") }
                }
                else -> {}
            }
        }
    }
}
