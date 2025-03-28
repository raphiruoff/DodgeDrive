package com.example.race.ui.racegame

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.login.R
import com.example.race.ui.racegame.components.Car
import com.example.race.ui.racegame.components.Obstacle
import com.example.race.ui.racegame.components.ScrollingRaceTrack
import com.example.race.ui.racegame.state.CarState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

// Kollisionsprüfung zwischen Auto und Hindernis
fun checkCollision(car: CarState, obstacle: Obstacle): Boolean {
    val carWidth = 48f
    val carHeight = 96f
    return car.carX < obstacle.x + obstacle.width &&
            car.carX + carWidth > obstacle.x &&
            car.carY < obstacle.y + obstacle.height &&
            car.carY + carHeight > obstacle.y
}

@Composable
fun RaceGameScreen() {
    val carState = remember { mutableStateOf(CarState()) }
    val obstacles = remember { mutableStateListOf<Obstacle>() }
    val score = remember { mutableStateOf(0) }
    val isGameOver = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val screenWidth = constraints.maxWidth
            val screenHeight = constraints.maxHeight

            val carWidth = 48f
            val moveStep = 20f
            val offsetFix = 100f

            val streetLeft = screenWidth * 1f / 6f - offsetFix
            val streetRight = screenWidth * 5f / 6f - carWidth - offsetFix

            // Auto initial mittig unten positionieren
            LaunchedEffect(screenWidth, screenHeight) {
                val centerX = (streetLeft + streetRight) / 2f
                val lowerY = screenHeight * 3f / 4f
                carState.value = CarState(carX = centerX, carY = lowerY, angle = 0f)
            }

            // Hindernisse spawnen
            if (!isGameOver.value) {
                LaunchedEffect(Unit) {
                    while (true) {
                        val laneX = listOf(
                            screenWidth * 2f / 6f,
                            screenWidth * 3f / 6f,
                            screenWidth * 4f / 6f
                        ).random()

                        obstacles.add(Obstacle(x = laneX, y = -50f))
                        delay(2500L)
                    }
                }

                // Hindernisse bewegen und Score aktualisieren
                LaunchedEffect(Unit) {
                    while (true) {
                        val iterator = obstacles.iterator()
                        while (iterator.hasNext()) {
                            val obstacle = iterator.next()
                            obstacle.y += 8f

                            if (checkCollision(carState.value, obstacle)) {
                                isGameOver.value = true
                            }

                            if (obstacle.y > screenHeight) {
                                iterator.remove()
                                score.value += 1
                            }
                        }
                        delay(20L)
                    }
                }
            }

            // Strecke + Auto + Hindernisse
            ScrollingRaceTrack()
            Car(carState = carState.value)

            obstacles.forEach { obstacle ->
                Image(
                    painter = painterResource(id = R.drawable.obstacle),
                    contentDescription = "Hindernis",
                    modifier = Modifier
                        .offset { IntOffset(obstacle.x.roundToInt(), obstacle.y.roundToInt()) }
                        .size(48.dp)
                )
            }

            // Score-Anzeige
            Text(
                text = "Score: ${score.value}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                color = Color.White
            )

            // Steuerung
            if (!isGameOver.value) {
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

            // Game Over Overlay
            if (isGameOver.value) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000)),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💥 Game Over",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Score: ${score.value}",
                        fontSize = 24.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        obstacles.clear()
                        score.value = 0
                        isGameOver.value = false
                    }) {
                        Text("🔄 Neustart")
                    }
                }
            }
        }
    }
}
