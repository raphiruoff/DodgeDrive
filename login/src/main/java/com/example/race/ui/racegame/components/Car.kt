package com.example.race.ui.racegame.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.example.login.R
import androidx.compose.ui.draw.scale
import com.example.race.ui.racegame.state.GameState

@Composable
fun Car(gameState: GameState, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.car),
        contentDescription = "Auto",
        modifier = modifier
            .graphicsLayer(
                rotationZ = Math.toDegrees(gameState.angle.toDouble()).toFloat(),
                translationX = gameState.x,
                translationY = gameState.y
            )
            .scale(0.2f)
    )
}
