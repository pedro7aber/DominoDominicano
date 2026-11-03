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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lamesa.dominodominicano.domain.*
import com.lamesa.dominodominicano.ui.components.DominoTileView

private val COLOR_SOUTH = Color(0xFF1565C0)
private val COLOR_NORTH = Color(0xFF2E7D32)
private val COLOR_WEST = Color(0xFFFFA000)
private val COLOR_EAST = Color(0xFFE53935)

private fun playerColor(p: PlayerId?): Color = when (p) {
    PlayerId.SOUTH -> COLOR_SOUTH
    PlayerId.NORTH -> COLOR_NORTH
    PlayerId.WEST -> COLOR_WEST
    PlayerId.EAST -> COLOR_EAST
    null -> Color(0xFF4E342E)
}

@Composable
fun GameScreen(
    onMenu: () -> Unit = {},
    onRules: () -> Unit = {},
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val pendingEnds by viewModel.pendingEnds.collectAsState()
    val legal = remember(state) { viewModel.legalTiles() }
    val canPass = state.currentPlayer == PlayerId.SOUTH &&
            state.phase == GameState.Phase.PLAYING && legal.isEmpty()

    val bg = Color(0xFF0B1A0B)
    val wood = Color(0xFF8B5A2B)
    val woodDark = Color(0xFF5D3A1A)
    val felt = Color(0xFF1B5E20)
    val gold = Color(0xFFC9A227)

    var showTable by remember { mutableStateOf(false) }
    var showPasses by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(bg).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)) {

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onMenu) { Text("Menú", color = Color.White, fontSize = 13.sp) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(color = Color(0xFF1A331A), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, gold)) {
                        Text(
                            "Mano ${state.handNumber} · Nos ${state.scores["us"] ?: 0} · Ellos ${state.scores["them"] ?: 0}",
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium
                        )
                    }
                    if (state.phase == GameState.Phase.PLAYING) {
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            color = if (state.currentPlayer == PlayerId.SOUTH) COLOR_SOUTH else Color(0xFF5D4037),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                if (state.currentPlayer == PlayerId.SOUTH) "▶ Tu turno" else "IA pensando...",
                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = Color.White, fontSize = 10.sp
                            )
                        }
                    }
                }
                Row {
                    TextButton(onClick = { showTable = true }) { Text("Tabla", color = gold, fontSize = 12.sp) }
                    TextButton(onClick = { showPasses = true }) { Text("Pases", color = gold, fontSize = 12.sp) }
                }
            }

            Spacer(Modifier.height(2.dp))

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(state.handOf(PlayerId.NORTH).size.coerceAtMost(7)) {
                        DominoTileView(Tile(0, 0), width = 28.dp, height = 48.dp, faceUp = false)
                    }
                }
                Surface(color = COLOR_NORTH, shape = RoundedCornerShape(6.dp)) {
                    Text("Compañero", Modifier.padding(6.dp, 2.dp), color = Color.White, fontSize = 10.sp)
                }
            }

            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OpponentCol("Arriba", COLOR_WEST, state.handOf(PlayerId.WEST).size, state.passes[PlayerId.WEST] == true)

                Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 3.dp), contentAlignment = Alignment.Center) {
                    var tableSize by remember { mutableStateOf(IntSize.Zero) }
                    Box(
                        Modifier.fillMaxWidth(0.98f).fillMaxHeight(0.88f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(wood, woodDark, wood)))
                            .padding(6.dp)
                            .onSizeChanged { tableSize = it }
                    ) {
                        Box(
                            Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(felt)
                                .border(1.dp, Color(0xFF0D3B0D), RoundedCornerShape(6.dp))
                        ) {
                            if (state.board.isEmpty) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Esperando salida del 6-6...", color = Color.White.copy(0.4f), fontSize = 12.sp)
                                }
                            } else {
                                val placed = remember(state.board, tableSize) {
                                    if (tableSize.width > 0)
                                        BoardLayout.layout(state.board, tableSize.width.toFloat(), tableSize.height.toFloat())
                                    else emptyList()
                                }
                                Canvas(Modifier.fillMaxSize()) {
                                    val cx = size.width / 2
                                    val cy = size.height / 2
                                    for (p in placed) {
                                        val w = BoardLayout.TILE_W
                                        val h = BoardLayout.TILE_H
                                        val borderCol = playerColor(p.playedBy)
                                        rotate(p.rotationDeg, Offset(cx + p.x, cy + p.y)) {
                                            val tl = Offset(cx + p.x - w / 2, cy + p.y - h / 2)
                                            drawRoundRect(Color.Black.copy(0.3f), tl + Offset(1.5f, 2f), Size(w, h), CornerRadius(3f))
                                            drawRoundRect(Color(0xFFF8F1E3), tl, Size(w, h), CornerRadius(3f))
                                            drawRoundRect(borderCol, tl, Size(w, h), CornerRadius(3f), style = Stroke(3f))
                                            drawLine(Color(0xFF5D4037).copy(0.5f), tl + Offset(3f, h / 2), tl + Offset(w - 3f, h / 2), 1.2f)
                                            // Pips orientados por matching (near = hacia conexión previa)
                                            drawPips(p.nearValue, tl, Size(w, h / 2))
                                            drawPips(p.farValue, tl + Offset(0f, h / 2), Size(w, h / 2))
                                        }
                                    }
                                }
                                Surface(
                                    Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
                                    color = Color(0xFF0D3B0D), shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        " ${state.board.leftEnd} ← → ${state.board.rightEnd} ",
                                        color = gold, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OpponentCol("Abajo", COLOR_EAST, state.handOf(PlayerId.EAST).size, state.passes[PlayerId.EAST] == true)
            }

            Surface(color = COLOR_SOUTH, shape = RoundedCornerShape(6.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Tú", Modifier.padding(8.dp, 2.dp), color = Color.White, fontSize = 10.sp)
            }

            Box(
                Modifier.fillMaxWidth().background(Color(0xFF3E2723), RoundedCornerShape(10.dp))
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.handOf(PlayerId.SOUTH).forEach { tile ->
                        val isSel = selected == tile
                        val isLeg = tile in legal
                        val dimmed = state.currentPlayer == PlayerId.SOUTH && !isLeg && legal.isNotEmpty()
                        Box(Modifier.alpha(if (dimmed) 0.35f else 1f)) {
                            DominoTileView(
                                tile = tile, width = 40.dp, height = 72.dp,
                                selected = isSel,
                                legal = isLeg && state.currentPlayer == PlayerId.SOUTH,
                                faceUp = true,
                                onClick = { viewModel.onTileTapped(tile) }
                            )
                        }
                    }
                }
            }

            if (pendingEnds.size >= 2 && selected != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.onChooseEnd(Board.End.LEFT) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("← Izquierda (${state.board.leftEnd})", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { viewModel.onChooseEnd(Board.End.RIGHT) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Derecha (${state.board.rightEnd}) →", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }
            } else if (selected != null && state.currentPlayer == PlayerId.SOUTH && state.phase == GameState.Phase.PLAYING) {
                Button(
                    onClick = { viewModel.onPlaySelected() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("JUGAR  ${selected!!.left}-${selected!!.right}", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }

            if (canPass) {
                Button(
                    onClick = { viewModel.onPass() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF360C)),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("PASAR", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }

            state.message?.let {
                Text(it, color = Color(0xFFFFF59D), fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(2.dp), textAlign = TextAlign.Center)
            }

            if (state.phase == GameState.Phase.HAND_OVER || state.phase == GameState.Phase.GAME_OVER) {
                HandOverPanel(state)
                if (state.phase == GameState.Phase.HAND_OVER) {
                    Button(onClick = { viewModel.onNextHand() }, modifier = Modifier.fillMaxWidth().padding(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Siguiente mano") }
                } else {
                    Text("¡Partida terminada! ${if (state.winnerOfGame == "us") "Nosotros" else "Ellos"}",
                        color = Color.White, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center)
                    Button(onClick = { viewModel.startNewGame() }, modifier = Modifier.fillMaxWidth().padding(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))) { Text("Nueva Partida") }
                }
            }
        }

        if (showTable) {
            AlertDialog(
                onDismissRequest = { showTable = false },
                title = { Text("Tabla de puntuación", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Nosotros: ${state.scores["us"] ?: 0}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Ellos: ${state.scores["them"] ?: 0}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Bonos de la partida:", fontWeight = FontWeight.Medium)
                        if (state.allBonuses.isEmpty()) Text("Ninguno aún", color = Color.Gray)
                        else state.allBonuses.forEach { b ->
                            Text("Mano ${b.handNumber}: ${b.type.name} · ${playerLabel(b.player)} +${b.points}", fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showTable = false }) { Text("Cerrar") } }
            )
        }

        if (showPasses) {
            AlertDialog(
                onDismissRequest = { showPasses = false },
                title = { Text("Historial de pases", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        if (state.passHistory.isEmpty()) Text("Ningún pase aún", color = Color.Gray)
                        else state.passHistory.takeLast(20).forEach { r ->
                            Text("Mano ${r.handNumber}: ${playerLabel(r.player)} pasó a [${r.leftEnd}|${r.rightEnd}]", fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showPasses = false }) { Text("Cerrar") } }
            )
        }
    }
}

@Composable
private fun HandOverPanel(state: GameState) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF1A2E1A), RoundedCornerShape(8.dp)).padding(8.dp)) {
        Text("Fichas restantes", color = Color(0xFFC9A227), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        PlayerId.entries.forEach { p ->
            val tiles = state.handOf(p)
            if (tiles.isNotEmpty()) {
                val pts = tiles.sumOf { it.points }
                Text("${playerLabel(p)}: ${tiles.joinToString(" ") { "${it.left}-${it.right}" }}  = $pts pts",
                    color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun OpponentCol(name: String, color: Color, count: Int, passed: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Surface(color = color, shape = RoundedCornerShape(6.dp)) {
            Text(name, Modifier.padding(4.dp, 2.dp), color = if (color == COLOR_WEST) Color.Black else Color.White, fontSize = 9.sp)
        }
        if (passed) Text("PASÓ", color = Color(0xFFFF8A80), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(count.coerceAtMost(7)) {
                DominoTileView(Tile(0, 0), width = 32.dp, height = 18.dp, faceUp = false)
            }
        }
    }
}

private fun playerLabel(p: PlayerId) = when (p) {
    PlayerId.SOUTH -> "Tú"; PlayerId.NORTH -> "Compañero"
    PlayerId.WEST -> "Arriba"; PlayerId.EAST -> "Abajo"
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPips(value: Int, origin: Offset, area: Size) {
    val cx = origin.x + area.width / 2
    val cy = origin.y + area.height / 2
    val r = minOf(area.width, area.height) * 0.09f
    val o = minOf(area.width, area.height) * 0.22f
    val c = Color(0xFF1C1C1C)
    fun p(x: Float, y: Float) = drawCircle(c, r, Offset(x, y))
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
