package com.example.race.ui.racegame.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.example.login.R
import androidx.compose.ui.draw.scale
import com.example.race.ui.racegame.state.CarState

@Composable
fun Car(carState: CarState, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.car),
        contentDescription = "Auto",
        modifier = modifier
            .graphicsLayer(
                rotationZ = Math.toDegrees(carState.angle.toDouble()).toFloat(),
                translationX = carState.x,
                translationY = carState.y
            )
            .scale(0.2f)
    )
}
