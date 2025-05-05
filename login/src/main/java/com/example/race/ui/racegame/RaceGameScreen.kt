package com.example.race.ui.racegame

import android.util.Log
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.login.R
import com.example.race.data.network.GameClient
import com.example.race.navigation.Routes
import com.example.race.ui.racegame.components.Car
import com.example.race.ui.racegame.components.Obstacle
import com.example.race.ui.racegame.components.ScrollingRaceTrack
import com.example.race.ui.racegame.state.CarState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

fun checkCollision(car: CarState, obstacle: Obstacle): Boolean {
    val carWidth = 48f
    val carHeight = 96f
    return car.carX < obstacle.x + obstacle.width &&
            car.carX + carWidth > obstacle.x &&
            car.carY < obstacle.y + obstacle.height &&
            car.carY + carHeight > obstacle.y
}

@Composable
fun RaceGameScreen(navController: NavHostController, gameId: String, username: String) {
    val carState = remember { mutableStateOf(CarState()) }
    val obstacles = remember { mutableStateListOf<Obstacle>() }
    val score = remember { mutableStateOf(0) }
    val opponentScore = remember { mutableStateOf(0) }
    val isGameOver = remember { mutableStateOf(false) }
    val isOpponentGameOver = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val screenWidth = constraints.maxWidth
            val screenHeight = constraints.maxHeight
            val carWidth = 48f
            val moveStep = 20f
            val offsetFix = 100f
            val streetLeft = screenWidth * 1f / 6f - offsetFix
            val streetRight = screenWidth * 5f / 6f - carWidth - offsetFix

            // Startposition
            LaunchedEffect(screenWidth, screenHeight) {
                val centerX = (streetLeft + streetRight) / 2f
                val lowerY = screenHeight * 3f / 4f
                carState.value = CarState(carX = centerX, carY = lowerY, angle = 0f)
            }

            // Hindernisse generieren
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

                // Hindernisse bewegen + Kollision prüfen
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

                // Score updaten
                LaunchedEffect(score.value) {
                    GameClient().updateScore(gameId, username, score.value)
                }

                // Gegnerstand abrufen + Status prüfen
                LaunchedEffect(gameId, isGameOver.value) {
                    while (!isGameOver.value && !isOpponentGameOver.value) {
                        val game = try {
                            GameClient().getGame(gameId)
                        } catch (e: Exception) {
                            Log.e("RaceGameScreen", "❌ Fehler beim Laden des Spiels", e)
                            null
                        }

                        val opponent = when (username) {
                            game?.playerA -> game.playerB
                            game?.playerB -> game.playerA
                            else -> null
                        }

                        opponentScore.value = game?.scoresMap?.get(opponent) ?: 0

                        if (game?.status == "FINISHED" && game.winner != username) {
                            isOpponentGameOver.value = true
                        }

                        delay(1000L)
                    }
                }
            }

            // Rennstrecke & Autos
            ScrollingRaceTrack()
            Car(carState = carState.value)
            obstacles.forEach {
                Image(
                    painter = painterResource(id = R.drawable.obstacle),
                    contentDescription = "Hindernis",
                    modifier = Modifier.offset { IntOffset(it.x.roundToInt(), it.y.roundToInt()) }.size(48.dp)
                )
            }

            // Punktestände
            Text(
                text = "Score: ${score.value}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                color = Color.White
            )

            Text(
                text = buildString {
                    append("Gegner: ${opponentScore.value}")
                    if (isOpponentGameOver.value) append(" ❌")
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                color = if (isOpponentGameOver.value) Color.Red else Color.Yellow
            )

            // Steuerung
            if (!isGameOver.value) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Button(onClick = {
                        carState.value = carState.value.copy(
                            carX = (carState.value.carX - moveStep).coerceIn(streetLeft, streetRight)
                        )
                    }) { Text("⬅️ Links") }

                    Button(onClick = {
                        carState.value = carState.value.copy(
                            carX = (carState.value.carX + moveStep).coerceIn(streetLeft, streetRight)
                        )
                    }) { Text("➡️ Rechts") }
                }
            }

            // Game Over
            if (isGameOver.value) {
                LaunchedEffect(true) {
                    GameClient().finishGame(gameId, username)
                }

                Column(
                    modifier = Modifier.fillMaxSize().background(Color(0x99000000)),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💥 Game Over", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Score: ${score.value}", fontSize = 24.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = {
                        obstacles.clear()
                        score.value = 0
                        isGameOver.value = false
                        isOpponentGameOver.value = false
                    }) { Text("🔄 Neustart") }

                    Button(onClick = {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.RACEGAME) { inclusive = true }
                        }
                    }) { Text("🏠 Zurück zum Hauptmenü") }
                }
            }
        }
    }
}
