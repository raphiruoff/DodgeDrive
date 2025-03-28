package com.example.race.ui.racegame

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.race.ui.racegame.components.Car
import com.example.race.ui.racegame.components.ScrollingRaceTrack
import com.example.race.ui.racegame.state.CarState

@Composable
fun RaceGameScreen() {
    val carState = remember { mutableStateOf(CarState()) }

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val screenWidth = constraints.maxWidth
            val screenHeight = constraints.maxHeight

            val carWidth = 48f // tatsächliche Auto-Breite in px
            val moveStep = 20f
            val offsetFix = 100f //Korrektur der Breite der Straße, 5x20

            val streetLeft = screenWidth * 1f / 6f - offsetFix
            val streetRight = screenWidth * 5f / 6f - carWidth - offsetFix

            LaunchedEffect(screenWidth, screenHeight) {
                val centerX = (streetLeft + streetRight) / 2f
                val lowerY = screenHeight * 3f / 4f
                carState.value = CarState(carX = centerX, carY = lowerY, angle = 0f)
            }

            ScrollingRaceTrack()
            Car(carState = carState.value)


            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Button(onClick = {
                    carState.value = carState.value.copy(
                        carX = (carState.value.carX - moveStep).coerceIn(streetLeft, streetRight)
                    )
                }) {
                    Text("⬅️ Links")
                }

                Button(onClick = {
                    carState.value = carState.value.copy(
                        carX = (carState.value.carX + moveStep).coerceIn(streetLeft, streetRight)
                    )
                }) {
                    Text("➡️ Rechts")
                }
            }
        }
    }
}
