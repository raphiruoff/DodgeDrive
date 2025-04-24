package com.example.race.ui.friends

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "👥 Deine Freunde",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(onClick = { /* TODO: Freunde verwalten */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Freunde anzeigen")
        }

        Button(onClick = { /* TODO: Anfragen verwalten */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Freundschaftsanfragen")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onNavigateBack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⬅️ Zurück zum Hauptmenü")
        }
    }
}
