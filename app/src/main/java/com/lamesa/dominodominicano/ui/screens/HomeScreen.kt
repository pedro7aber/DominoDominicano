package com.lamesa.dominodominicano.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onPlay: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val bg = Color(0xFF0D1F0D)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1A0A), bg, Color(0xFF1B3A1B))))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text("DOMINÓ", color = Color(0xFFC9A227), fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Text("DOMINICANO", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Medium)
            Text("Élite · Reglas v3 · IA de alto nivel", color = Color(0xFF81C784), fontSize = 14.sp)

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(0.75f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Jugar", fontSize = 20.sp, color = Color.White)
            }

            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.fillMaxWidth(0.75f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Ajustes")
            }
        }
    }
}
