package com.example.race.ui.racegame.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Controls(onLeft: () -> Unit, onRight: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = onLeft) {
            Text("⬅️ Links")
        }
        Button(onClick = onRight) {
            Text("➡️ Rechts")
        }
    }
}

