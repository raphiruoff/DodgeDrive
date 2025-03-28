package com.example.race.ui.racegame.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import com.example.login.R
import androidx.compose.ui.unit.IntOffset
import com.example.race.ui.racegame.state.CarState
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

@Composable
fun Car(carState: CarState, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.car),
        contentDescription = "auto",
        modifier = modifier
            .size(85.dp)
            .offset {
                IntOffset(
                    carState.carX.roundToInt(),
                    carState.carY.roundToInt()
                )
            }
            .rotate(carState.angle * (180f / Math.PI.toFloat()) + 90f) 
    )
}

