package com.lamesa.dominodominicano.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
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
    val selected by viewModel.selected.collectAsState()
    val legal = remember(state) { viewModel.legalTiles() }
    val canPass = state.currentPlayer == PlayerId.SOUTH &&
            state.phase == GameState.Phase.PLAYING &&
            legal.isEmpty()

    val bg = Color(0xFF0B1A0B)
    val wood = Color(0xFF8B5A2B)
    val woodDark = Color(0xFF5D3A1A)
    val felt = Color(0xFF1B5E20)
    val gold = Color(0xFFC9A227)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)) {

            // TOP BAR
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onMenu) {
                    Text("Menú", color = Color.White, fontSize = 14.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = Color(0xFF1A331A),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, gold)
                    ) {
                        Text(
                            "Mano ${state.handNumber}  ·  Nos ${state.scores["us"] ?: 0}  ·  Ellos ${state.scores["them"] ?: 0}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    TurnBadge(state)
                }

                TextButton(onClick = onRules) {
                    Text("Reglas", color = gold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(4.dp))

            // COMPAÑERO
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(state.handOf(PlayerId.NORTH).size.coerceAtMost(7)) {
                        DominoTileView(Tile(0, 0), width = 26.dp, height = 44.dp, faceUp = false)
                    }
                }
                Spacer(Modifier.height(2.dp))
                LabelChip("Compañero", Color(0xFF2E7D32))
            }

            Spacer(Modifier.height(4.dp))

            // MESA + LATERALES
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OpponentColumn(
                    name = "Arriba",
                    color = Color(0xFFFFA000),
                    count = state.handOf(PlayerId.WEST).size,
                    passed = state.passes[PlayerId.WEST] == true
                )

                // TABLERO CON LAYOUT REALISTA
                Box(
                    Modifier.weight(1f).fillMaxHeight().padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    var tableSize by remember { mutableStateOf(IntSize.Zero) }
                    Box(
                        Modifier
                            .fillMaxWidth(0.98f)
                            .fillMaxHeight(0.90f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(wood, woodDark, wood)))
                            .padding(8.dp)
                            .onSizeChanged { tableSize = it }
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(6.dp))
                                .background(felt)
                                .border(1.dp, Color(0xFF0D3B0D), RoundedCornerShape(6.dp))
                        ) {
                            if (state.board.isEmpty) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "Esperando salida del 6-6...",
                                        color = Color.White.copy(0.45f),
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                val density = LocalDensity.current
                                val placed = remember(state.board, tableSize) {
                                    if (tableSize.width > 0) {
                                        BoardLayout.layout(
                                            state.board,
                                            tableSize.width.toFloat(),
                                            tableSize.height.toFloat()
                                        )
                                    } else emptyList()
                                }

                                Canvas(Modifier.fillMaxSize()) {
                                    val cx = size.width / 2
                                    val cy = size.height / 2
                                    for (p in placed) {
                                        val w = if (p.isDouble) BoardLayout.TILE_LONG else BoardLayout.TILE_SHORT
                                        val h = if (p.isDouble) BoardLayout.TILE_SHORT else BoardLayout.TILE_LONG
                                        // Ajuste: cuando rotation es 90, intercambiamos visualmente
                                        val drawW = if (p.rotationDeg % 180f == 90f) h else w
                                        val drawH = if (p.rotationDeg % 180f == 90f) w else h

                                        rotate(p.rotationDeg, pivot = Offset(cx + p.x, cy + p.y)) {
                                            drawDominoFace(
                                                tile = p.tile,
                                                topLeft = Offset(cx + p.x - drawW / 2, cy + p.y - drawH / 2),
                                                width = drawW,
                                                height = drawH
                                            )
                                        }
                                    }
                                }

                                // Extremos
                                Column(
                                    Modifier.align(Alignment.TopCenter).padding(top = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(color = Color(0xFF0D3B0D), shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            "  ${state.board.leftEnd}  ← →  ${state.board.rightEnd}  ",
                                            color = gold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OpponentColumn(
                    name = "Abajo",
                    color = Color(0xFFE53935),
                    count = state.handOf(PlayerId.EAST).size,
                    passed = state.passes[PlayerId.EAST] == true
                )
            }

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                LabelChip("Tú", Color(0xFF1565C0))
            }

            Spacer(Modifier.height(3.dp))

            // MANO DEL JUGADOR
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3E2723), RoundedCornerShape(10.dp))
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    state.handOf(PlayerId.SOUTH).forEach { tile ->
                        val isSel = selected == tile
                        val isLeg = tile in legal
                        val dimmed = state.currentPlayer == PlayerId.SOUTH && !isLeg && legal.isNotEmpty()
                        Box(modifier = Modifier.alpha(if (dimmed) 0.38f else 1f)) {
                            DominoTileView(
                                tile = tile,
                                width = 46.dp,
                                height = 84.dp,
                                selected = isSel,
                                legal = isLeg && state.currentPlayer == PlayerId.SOUTH,
                                faceUp = true,
                                onClick = { viewModel.onTileTapped(tile) }
                            )
                        }
                    }
                }
            }

            if (selected != null && state.currentPlayer == PlayerId.SOUTH && state.phase == GameState.Phase.PLAYING) {
                Button(
                    onClick = { viewModel.onPlaySelected() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("JUGAR  ${selected!!.left}-${selected!!.right}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (canPass) {
                Button(
                    onClick = { viewModel.onPass() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF360C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("PASAR", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }

            state.message?.let { msg ->
                Text(
                    msg,
                    color = Color(0xFFFFF59D),
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    textAlign = TextAlign.Center
                )
            }

            when (state.phase) {
                GameState.Phase.HAND_OVER -> {
                    Button(
                        onClick = { viewModel.onNextHand() },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) { Text("Siguiente mano") }
                }
                GameState.Phase.GAME_OVER -> {
                    Text(
                        "¡Partida terminada! Ganador: ${if (state.winnerOfGame == "us") "Nosotros" else "Ellos"}",
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDominoFace(
    tile: Tile,
    topLeft: Offset,
    width: Float,
    height: Float
) {
    val radius = 4f
    // Sombra
    drawRoundRect(Color.Black.copy(0.3f), topLeft + Offset(1.5f, 2f), Size(width, height), CornerRadius(radius))
    // Cuerpo
    drawRoundRect(Color(0xFFF8F1E3), topLeft, Size(width, height), CornerRadius(radius))
    // Borde
    drawRoundRect(Color(0xFF4E342E), topLeft, Size(width, height), CornerRadius(radius), style = Stroke(1.5f))

    val mid = height / 2
    drawLine(
        Color(0xFF5D4037).copy(0.5f),
        topLeft + Offset(4f, mid),
        topLeft + Offset(width - 4f, mid),
        strokeWidth = 1.2f
    )

    drawPipsSimple(tile.left, topLeft, Size(width, mid))
    drawPipsSimple(tile.right, topLeft + Offset(0f, mid), Size(width, mid))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPipsSimple(
    value: Int,
    origin: Offset,
    area: Size
) {
    val cx = origin.x + area.width / 2
    val cy = origin.y + area.height / 2
    val r = minOf(area.width, area.height) * 0.09f
    val o = minOf(area.width, area.height) * 0.22f
    val color = Color(0xFF1C1C1C)
    fun p(x: Float, y: Float) = drawCircle(color, r, Offset(x, y))
    when (value) {
        1 -> p(cx, cy)
        2 -> { p(cx - o, cy - o); p(cx + o, cy + o) }
        3 -> { p(cx - o, cy - o); p(cx, cy); p(cx + o, cy + o) }
        4 -> { p(cx - o, cy - o); p(cx + o, cy - o); p(cx - o, cy + o); p(cx + o, cy + o) }
        5 -> { p(cx - o, cy - o); p(cx + o, cy - o); p(cx, cy); p(cx - o, cy + o); p(cx + o, cy + o) }
        6 -> {
            p(cx - o, cy - o); p(cx + o, cy - o)
            p(cx - o, cy); p(cx + o, cy)
            p(cx - o, cy + o); p(cx + o, cy + o)
        }
    }
}

@Composable
private fun TurnBadge(state: GameState) {
    when {
        state.phase == GameState.Phase.PLAYING && state.currentPlayer == PlayerId.SOUTH ->
            Surface(color = Color(0xFF1565C0), shape = RoundedCornerShape(10.dp)) {
                Text("▶ Tu turno", Modifier.padding(horizontal = 10.dp, vertical = 2.dp), color = Color.White, fontSize = 11.sp)
            }
        state.phase == GameState.Phase.PLAYING ->
            Surface(color = Color(0xFF5D4037), shape = RoundedCornerShape(10.dp)) {
                Text("IA pensando...", Modifier.padding(horizontal = 10.dp, vertical = 2.dp), color = Color.White, fontSize = 11.sp)
            }
        else -> {}
    }
}

@Composable
private fun LabelChip(text: String, color: Color) {
    Surface(color = color, shape = RoundedCornerShape(8.dp)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 2.dp), color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun OpponentColumn(name: String, color: Color, count: Int, passed: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(color = color, shape = RoundedCornerShape(6.dp)) {
            Text(
                name,
                Modifier.padding(4.dp, 2.dp),
                color = if (color == Color(0xFFFFA000)) Color.Black else Color.White,
                fontSize = 9.sp,
                lineHeight = 11.sp
            )
        }
        if (passed) {
            Text("PASÓ", color = Color(0xFFFF8A80), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(count.coerceAtMost(7)) {
                DominoTileView(Tile(0, 0), width = 34.dp, height = 20.dp, faceUp = false)
            }
        }
    }
}
