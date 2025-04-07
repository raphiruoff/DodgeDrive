package com.example.race.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SessionScreen(
    onNavigateToRaceGame: () -> Unit,
    onManageFriends: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🏁 Willkommen im Race Hub!",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Was möchtest du tun?",
            fontSize = 18.sp
        )

        Button(
            onClick = { onManageFriends() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("👥 Freunde verwalten")
        }

        Button(
            onClick = { /* TODO: Session starten Logik */ },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("🎯 Neue Session starten")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNavigateToRaceGame,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = true // später deaktivieren und nru wenn 2 Spieler da sind
        ) {
            Text("🚗 Spiel starten")
        }
    }
}
