package com.example.race.ui.racegame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate

@Composable
fun Car(gameState: GameState, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Canvas(modifier = modifier) {
            rotate(gameState.angle, pivot = Offset(gameState.x, gameState.y)) {
                drawRect(
                    color = Color.Green,
                    topLeft = Offset(gameState.x - 25f, gameState.y - 50f),
                    size = androidx.compose.ui.geometry.Size(50f, 100f)
                )
            }
        }
    }
}
