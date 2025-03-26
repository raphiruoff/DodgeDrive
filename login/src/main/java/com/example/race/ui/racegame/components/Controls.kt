package com.example.race.ui.racegame.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.race.ui.racegame.control.GameController

@Composable
fun Controls(controller: GameController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        HoldableButton(
            onPressStart = { controller.startRotatingLeft() },
            onPressEnd = { controller.stopRotating() }
        ) {
            Text("Left")
        }

        HoldableButton(
            onPressStart = { controller.startRotatingRight() },
            onPressEnd = { controller.stopRotating() }
        ) {
            Text("Right")
        }

        HoldableButton(
            onPressStart = { controller.startMovingForward() },
            onPressEnd = { controller.stopMoving() }
        ) {
            Text("Forward")
        }
    }
}
