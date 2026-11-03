package com.lamesa.dominodominicano.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamesa.dominodominicano.domain.GameConfig

@Composable
fun GameConfigScreen(
    onStart: (GameConfig) -> Unit,
    onBack: () -> Unit
) {
    var goal by remember { mutableIntStateOf(200) }
    var countAll by remember { mutableStateOf(true) }
    var bonusValue by remember { mutableIntStateOf(25) }
    var chuchazo by remember { mutableStateOf(false) }
    var tranqueIndividual by remember { mutableStateOf(true) }

    val bg = Color(0xFF0D1F0D)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Configurar Partida", color = Color(0xFFC9A227), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        // Meta
        Text("Meta de puntos", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(100, 150, 200, 250, 300, 350, 400).forEach { g ->
                FilterChip(
                    selected = goal == g,
                    onClick = { goal = g },
                    label = { Text("$g") }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Conteo
        Text("Conteo de puntos", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row {
            FilterChip(selected = countAll, onClick = { countAll = true }, label = { Text("Todas las fichas") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = !countAll, onClick = { countAll = false }, label = { Text("Solo rivales") })
        }

        Spacer(Modifier.height(20.dp))

        // Valor de bonos
        Text("Valor de bonos (Capicúa, Pase, etc.)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row {
            FilterChip(selected = bonusValue == 25, onClick = { bonusValue = 25 }, label = { Text("+25") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = bonusValue == 30, onClick = { bonusValue = 30 }, label = { Text("+30") })
        }

        Spacer(Modifier.height(20.dp))

        // Tranque
        Text("Tipo de Tranque", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row {
            FilterChip(selected = tranqueIndividual, onClick = { tranqueIndividual = true }, label = { Text("Individual") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = !tranqueIndividual, onClick = { tranqueIndividual = false }, label = { Text("Por parejas") })
        }

        Spacer(Modifier.height(20.dp))

        // Chuchazo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = chuchazo, onCheckedChange = { chuchazo = it })
            Text("Activar Chuchazo (0-0 como última ficha)", color = Color.White, fontSize = 14.sp)
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                onStart(
                    GameConfig(
                        goal = goal,
                        countAllTiles = countAll,
                        bonusValue = bonusValue,
                        chuchazoEnabled = chuchazo,
                        tranqueIndividual = tranqueIndividual
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Empezar Partida", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text("Volver")
        }
    }
}
