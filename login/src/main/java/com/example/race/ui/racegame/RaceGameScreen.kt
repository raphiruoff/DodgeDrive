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
    val gameResultMessage = remember { mutableStateOf<String?>(null) }

    val gameStartTime = remember { SystemClock.elapsedRealtime() }
    var gameStartDelay by remember { mutableStateOf(0L) }
    var opponentUpdateDelay by remember { mutableStateOf(0L) }
    var playerScore by remember { mutableStateOf(0) }

    var isStarted by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var allServerObstacles by remember { mutableStateOf<List<Obstacle>>(emptyList()) }
    var startAt by remember { mutableStateOf(0L) }
    val renderTick = remember { mutableStateOf(0L) }
    var gameStartElapsed by remember { mutableStateOf(0L) }
    val previousOpponentScore = remember { mutableStateOf(0) }



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
                AllClients.logClient.logEventWithTimestamp(
                    gameId = gameId,
                    username = username,
                    eventType = "debug_start_marker",
                    originTimestamp = 1747777777000L
                )

                carState.value = CarState(carX = centerX, carY = lowerY, angle = 0f)

                val session = AllClients.sessionClient.getSession(gameId)

                if (username == session?.playerA) {
                    AllClients.sessionClient.startGame(gameId, username)
                    // Re-fetch to ensure updated startAt after startGame
                    val updatedSession = AllClients.sessionClient.getSession(gameId)
                    startAt = updatedSession?.startAt ?: 0L
                } else {
                    startAt = session?.startAt ?: 0L
                }

                println("🕒 Server startAt: $startAt")

                val countdownStartAt = startAt - 3000L
                val countdownStartElapsed = SystemClock.elapsedRealtime() + (countdownStartAt - System.currentTimeMillis())
                val startAtElapsed = SystemClock.elapsedRealtime() + (startAt - System.currentTimeMillis())

                while (SystemClock.elapsedRealtime() < countdownStartElapsed) {
                    delay(1)
                }

                for (i in 3 downTo 1) {
                    countdown = i
                    delay(1000L)
                }
                countdown = null

                while (SystemClock.elapsedRealtime() < startAtElapsed) {
                    delay(1)
                }

                gameStartElapsed = SystemClock.elapsedRealtime()

                println("Client $username startet bei ${System.currentTimeMillis()}, erwartet: $startAt, Differenz: ${System.currentTimeMillis() - startAt}")

// Logge tatsächlichen Start mit synchronisiertem Startzeitpunkt (originTimestamp = startAt)
                println("📤 Sende Event: game_start mit origin=$startAt")
                AllClients.logClient.logEventWithTimestamp(
                    gameId = gameId,
                    username = username,
                    eventType = "game_start",
                    originTimestamp = startAt
                )

                isStarted = true
                gameStartDelay = SystemClock.elapsedRealtime() - gameStartTime


                println("📤 Logging obstacle")

                // Bei erstem Hindernis-Spawn
                if (obstacles.isEmpty()) {
                    AllClients.logClient.logEventWithTimestamp(gameId, username, "first_obstacle_check", System.currentTimeMillis())
                }
                println("📤 Logging oponent_score")

                // Beim ersten gegnerischen Score-Update
                if (previousOpponentScore.value == 0 && opponentScore.value > 0)
                    AllClients.logClient.logEventWithDelay(gameId, username, "opponent_score_visible", System.currentTimeMillis())


                val game = AllClients.gameClient.getGame(gameId)
                println(" Game received: ${game?.gameId}, Obstacles: ${game?.obstaclesList?.size}")

                allServerObstacles = game?.obstaclesList?.map {
                    println("➡ Obstacle loaded: x=${it.x}, timestamp=${it.timestamp}")
                    Obstacle(x = it.x * screenWidth, y = -50f, timestamp = it.timestamp)
                } ?: emptyList()

            }



            if (isStarted) {

                LaunchedEffect(allServerObstacles) {
                    var firstSpawned = false
                    for (obstacle in allServerObstacles.sortedBy { it.timestamp }) {
                        val delayTime = obstacle.timestamp - System.currentTimeMillis()
                        if (delayTime > 0) delay(delayTime)

                        obstacles.add(obstacle.copy(y = -50f))

                        if (!firstSpawned) {
                            AllClients.logClient.logEventWithTimestamp(gameId, username, "obstacle_spawned", System.currentTimeMillis())
                            firstSpawned = true
                        }

                        println("[$username] Spawn obstacle at ${System.currentTimeMillis()}, scheduled at ${obstacle.timestamp}")
                    }
                }


                // Spiel-Loop: Hindernisse bewegen, Score erhöhen
                LaunchedEffect(Unit) {
                    while (true) {
                        renderTick.value = System.currentTimeMillis()
                        delay(16L) // 60 FPS
                    }
                }
                LaunchedEffect(renderTick.value) {
                    val iterator = obstacles.iterator()
                    val toRemove = mutableListOf<Obstacle>()

                    if (!isGameOver.value) {
                        while (iterator.hasNext()) {
                            val obstacle = iterator.next()
                            obstacle.y += 8f

                            if (checkCollision(carState.value, obstacle)) {
                                isGameOver.value = true
                            }

                            if (obstacle.y > screenHeight) {
                                toRemove.add(obstacle)
                                val start = SystemClock.elapsedRealtime()
                                val success = AllClients.gameClient.incrementScore(
                                    gameId,
                                    username,
                                    System.currentTimeMillis()
                                )
                                val end = SystemClock.elapsedRealtime()
                                if (success) {
                                    AllClients.logClient.logEventWithDelay(gameId, username, "score_updated", end - start)
                                }
                            }
                        }
                    }

                    obstacles.removeAll(toRemove)
                }


                // Gegnerdaten regelmäßig abfragen
                LaunchedEffect(gameId) {
                    while (true) {
                        try {
                            val pollStart = SystemClock.elapsedRealtime()
                            val game = AllClients.gameClient.getGame(gameId)
                            val pollEnd = SystemClock.elapsedRealtime()

                            opponentUpdateDelay = pollEnd - pollStart
                            AllClients.logClient.logEventWithDelay(gameId, username, "opponent_update", opponentUpdateDelay)

                            if (game != null) {
                                val opponent = when (username) {
                                    game.playerA -> game.playerB
                                    game.playerB -> game.playerA
                                    else -> null
                                }

                                val currentOpponentScore = game.scoresMap[opponent] ?: 0
                                if (previousOpponentScore.value == 0 && currentOpponentScore > 0) {
                                    AllClients.logClient.logEventWithTimestamp(gameId, username, "opponent_score_visible", System.currentTimeMillis())
                                }
                                previousOpponentScore.value = currentOpponentScore

                                opponentScore.value = currentOpponentScore
                                playerScore = game.scoresMap[username] ?: 0

                                val finishedPlayers = game.getFinishedPlayersList()
                                if (opponent != null && finishedPlayers.contains(opponent)) {
                                    isOpponentGameOver.value = true
                                }

                            } else {
                                println("⚠ Game not found for gameId=$gameId during polling")
                            }

                        } catch (e: Exception) {
                            println(" Fehler beim Polling des Spiels: ${e.message}")
                            e.printStackTrace()
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

                    // Warte auf finale Server-Auswertung
                    while (true) {
                        val game = AllClients.gameClient.getGame(gameId)
                        if (game?.status == "FINISHED" && !game.winner.isNullOrEmpty()) {
                            val winner = game.winner
                            gameResultMessage.value = when {
                                winner == username -> "🏆 Du hast gewonnen!"
                                winner == "draw" -> "🤝 Unentschieden"
                                else -> "😢 Du hast verloren"
                            }
                            break
                        }
                        delay(250)
                    }
                }




                Column(
                    modifier = Modifier.fillMaxSize().background(Color(0x99000000)),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        gameResultMessage.value ?: "💥 Spiel beendet",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
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
