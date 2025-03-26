package com.example.race.ui.racegame.control

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.race.ui.racegame.state.CarState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameController {
    var carState by mutableStateOf(CarState())
    private var isMoving = false
    private var isRotatingLeft = false
    private var isRotatingRight = false

    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                if (isMoving) moveForward()
                if (isRotatingLeft) rotateLeft()
                if (isRotatingRight) rotateRight()
                delay(16)
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
        val radians = carState.angle
        val speed = 3f

        val deltaX = (-speed * kotlin.math.cos(radians)).toFloat()
        val deltaY = (-speed * kotlin.math.sin(radians)).toFloat()

        carState = carState.copy(
            x = carState.x + deltaX,
            y = carState.y + deltaY
        )
    }



    fun rotateLeft() {
        carState = carState.copy(angle = carState.angle - 0.03f)
    }

    fun rotateRight() {
        carState = carState.copy(angle = carState.angle + 0.03f)
    }
}

