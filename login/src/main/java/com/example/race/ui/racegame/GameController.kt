package com.example.race.ui.racegame

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class GameController {
    var gameState by mutableStateOf(GameState())

    fun moveForward() {
        val radians = Math.toRadians(gameState.angle.toDouble())
        val deltaX = (10 * Math.sin(radians)).toFloat()
        val deltaY = (-10 * Math.cos(radians)).toFloat()
        gameState = gameState.copy(
            x = gameState.x + deltaX,
            y = gameState.y + deltaY
        )
    }

    fun rotateLeft() {
        gameState = gameState.copy(angle = gameState.angle - 10f)
    }

    fun rotateRight() {
        gameState = gameState.copy(angle = gameState.angle + 10f)
    }
}
