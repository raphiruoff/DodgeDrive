package com.example.race.ui.racegame.control

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.race.ui.racegame.state.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameController {
    var gameState by mutableStateOf(GameState())
    private var isMoving = false
    private var isRotatingLeft = false
    private var isRotatingRight = false

    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                if (isMoving) moveForward()
                if (isRotatingLeft) rotateLeft()
                if (isRotatingRight) rotateRight()
                delay(16) // ca. 60 FPS
            }
        }
    }

    fun startMovingForward() { isMoving = true }
    fun stopMoving() { isMoving = false }

    fun startRotatingLeft() { isRotatingLeft = true }
    fun startRotatingRight() { isRotatingRight = true }
    fun stopRotating() {
        isRotatingLeft = false
        isRotatingRight = false
    }

    fun moveForward() {
        val radians = gameState.angle
        val speed = 3f

        val deltaX = (-speed * kotlin.math.cos(radians)).toFloat()
        val deltaY = (-speed * kotlin.math.sin(radians)).toFloat()

        gameState = gameState.copy(
            x = gameState.x + deltaX,
            y = gameState.y + deltaY
        )
    }



    fun rotateLeft() {
        gameState = gameState.copy(angle = gameState.angle - 0.03f)
    }

    fun rotateRight() {
        gameState = gameState.copy(angle = gameState.angle + 0.03f)
    }
}

