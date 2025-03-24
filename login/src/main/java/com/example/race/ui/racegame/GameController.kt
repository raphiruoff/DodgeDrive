package com.example.race.ui.racegame

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class GameController {
    var gameState by mutableStateOf(GameState())

    fun moveForward() {
        val radians = gameState.angle
        val speed = 5f

        val deltaX = (-speed * kotlin.math.cos(radians)).toFloat()
        val deltaY = (-speed * kotlin.math.sin(radians)).toFloat()

        gameState = gameState.copy(
            x = gameState.x + deltaX,
            y = gameState.y + deltaY
        )
    }



    fun rotateLeft() {
        gameState = gameState.copy(angle = gameState.angle - 0.05f)
    }

    fun rotateRight() {
        gameState = gameState.copy(angle = gameState.angle + 0.05f)
    }
}
