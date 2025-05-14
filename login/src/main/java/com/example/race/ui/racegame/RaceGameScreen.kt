package com.example.race.ui.racegame

import android.os.SystemClock
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
import com.example.race.data.network.AllClients
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
    val opponentScore = remember { mutableStateOf(0) }
    val isGameOver = remember { mutableStateOf(false) }
    val isOpponentGameOver = remember { mutableStateOf(false) }

    val gameStartTime = remember { SystemClock.elapsedRealtime() }
    var gameStartDelay by remember { mutableStateOf(0L) }
    var opponentUpdateDelay by remember { mutableStateOf(0L) }
    var playerScore by remember { mutableStateOf(0) }

    var isStarted by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var allServerObstacles by remember { mutableStateOf<List<Obstacle>>(emptyList()) }
    var startAt by remember { mutableStateOf(0L) }

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val screenWidth = constraints.maxWidth
            val screenHeight = constraints.maxHeight
            val carWidth = 48f
            val moveStep = 20f
            val offsetFix = 100f
            val streetLeft = screenWidth * 1f / 6f - offsetFix
            val streetRight = screenWidth * 5f / 6f - carWidth - offsetFix
            val centerX = (streetLeft + streetRight) / 2f
            val lowerY = screenHeight * 3f / 4f

            LaunchedEffect(Unit) {
                carState.value = CarState(carX = centerX, carY = lowerY, angle = 0f)
                println(" Server statAt: $startAt")

                val session = AllClients.sessionClient.getSession(gameId)
                startAt = session?.startAt ?: 0L
                println(" Server startAt: $startAt")

                val countdownStartAt = startAt - 3000L
                val now = System.currentTimeMillis()

                val delayUntilCountdown = countdownStartAt - now
                if (delayUntilCountdown > 0) delay(delayUntilCountdown)

                for (i in 3 downTo 1) {
                    countdown = i
                    delay(1000L)
                }
                countdown = null

                val finalWait = startAt - System.currentTimeMillis()
                if (finalWait > 0) delay(finalWait)

                println("Client $username startet bei ${System.currentTimeMillis()}, erwartet: $startAt, Differenz: ${System.currentTimeMillis() - startAt}")


                isStarted = true

                gameStartDelay = SystemClock.elapsedRealtime() - gameStartTime
                AllClients.logClient.logEvent(gameId, username, "game_start", gameStartDelay)

                val game = AllClients.gameClient.getGame(gameId)
                println(" Server gameId from getGame: ${game?.gameId}")
                allServerObstacles = game?.obstaclesList?.map {
                    Obstacle(x = it.x * screenWidth, y = -50f, timestamp = it.timestamp)

                } ?: emptyList()
            }

            if (isStarted) {
                // Hindernisse vom Server synchronisiert anzeigen
                LaunchedEffect(allServerObstacles, startAt) {
                    for (obstacle in allServerObstacles.sortedBy { it.timestamp }) {
                        val delayMs = obstacle.timestamp - startAt
                        val elapsed = System.currentTimeMillis() - startAt
                        val remaining = delayMs - elapsed
                        if (remaining > 0) delay(remaining)
                        obstacles.add(obstacle.copy(y = -50f))
                    }
                    println("Hindernisse vom Server empfangen: ${allServerObstacles.size}")

                }

                // Spiel-Loop: Hindernisse bewegen, Score erhöhen
                LaunchedEffect(Unit) {
                    while (!isGameOver.value) {
                        val iterator = obstacles.iterator()
                        while (iterator.hasNext()) {
                            val obstacle = iterator.next()
                            obstacle.y += 8f
                            if (checkCollision(carState.value, obstacle)) {
                                isGameOver.value = true
                            }
                            if (obstacle.y > screenHeight) {
                                iterator.remove()
                                val start = SystemClock.elapsedRealtime()
                                val success = AllClients.gameClient.incrementScore(
                                    gameId,
                                    username,
                                    System.currentTimeMillis()
                                )
                                val end = SystemClock.elapsedRealtime()
                                if (success) {
                                    AllClients.logClient.logEvent(gameId, username, "score_updated", end - start)
                                }
                            }
                        }
                        delay(20L)
                    }
                }

                // Gegnerdaten regelmäßig abfragen
                LaunchedEffect(gameId) {
                    while (true) {
                        val pollStart = SystemClock.elapsedRealtime()
                        val game = AllClients.gameClient.getGame(gameId)
                        val pollEnd = SystemClock.elapsedRealtime()
                        opponentUpdateDelay = pollEnd - pollStart
                        AllClients.logClient.logEvent(gameId, username, "opponent_update", opponentUpdateDelay)

                        val opponent = when (username) {
                            game?.playerA -> game.playerB
                            game?.playerB -> game.playerA
                            else -> null
                        }

                        opponentScore.value = game?.scoresMap?.get(opponent) ?: 0
                        playerScore = game?.scoresMap?.get(username) ?: 0

                        if (game?.status == "FINISHED" && game.winner != username) {
                            isOpponentGameOver.value = true
                        }

                        delay(100)
                    }
                }
            }

            ScrollingRaceTrack()
            Car(carState = carState.value)
            obstacles.forEach {
                Image(
                    painter = painterResource(id = R.drawable.obstacle),
                    contentDescription = "Hindernis",
                    modifier = Modifier
                        .offset { IntOffset(it.x.roundToInt(), it.y.roundToInt()) }
                        .size(48.dp)
                )
            }

            if (countdown != null) {
                Text(
                    "Start in $countdown",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0x99000000))
                        .padding(16.dp),
                    color = Color.White
                )
            }

            Text(
                "Score: $playerScore",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                color = Color.White
            )

            Text(
                "Gegner: ${opponentScore.value}" + if (isOpponentGameOver.value) " ❌" else "",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                color = if (isOpponentGameOver.value) Color.Red else Color.Yellow
            )

            if (isStarted && !isGameOver.value) {
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

            if (isGameOver.value) {
                LaunchedEffect(true) {
                    AllClients.gameClient.finishGame(gameId, username)
                    AllClients.logClient.exportLogs(gameId)
                }

                Column(
                    modifier = Modifier.fillMaxSize().background(Color(0x99000000)),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💥 Game Over", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Score: $playerScore", fontSize = 24.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        obstacles.clear()
                        isGameOver.value = false
                        isOpponentGameOver.value = false
                        isStarted = true
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
