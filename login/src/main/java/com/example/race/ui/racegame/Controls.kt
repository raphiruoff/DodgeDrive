package com.example.race.ui.racegame

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Controls(controller: GameController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = { controller.rotateLeft() }) {
            Text("Left")
        }
        Button(onClick = { controller.rotateRight() }) {
            Text("Right")
        }
        Button(onClick = { controller.moveForward() }) {
            Text("Forward")
        }
    }
}
