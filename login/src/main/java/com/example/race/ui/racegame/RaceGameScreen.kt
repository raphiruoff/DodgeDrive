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
import com.example.race.data.network.WebSocketManager
import com.example.race.navigation.Routes
import com.example.race.ui.racegame.components.Car
import com.example.race.ui.racegame.components.Obstacle
import com.example.race.ui.racegame.components.ScrollingRaceTrack
import com.example.race.ui.racegame.state.CarState
import de.ruoff.consistency.events.ObstacleSpawnedEvent
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
    var startAt by remember { mutableStateOf(0L) }
    val renderTick = remember { mutableStateOf(0L) }
    var gameStartElapsed by remember { mutableStateOf(0L) }
    val previousOpponentScore = remember { mutableStateOf(0) }



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
            val centerX = (streetLeft + streetRight) / 2f
            val lowerY = screenHeight * 3f / 4f


            val pendingObstacles = remember { mutableStateListOf<ObstacleSpawnedEvent>() }

            LaunchedEffect(Unit) {
                println("✅ Spielaufbau gestartet für gameId: $gameId")

                // 1. WebSocket-Verbindung herstellen, damit Obstacle-Events empfangen werden können
                WebSocketManager.connect(
                    gameId = gameId,
                    onObstacle = { obstacle ->
                        println("📥 Obstacle empfangen: $obstacle")
                        pendingObstacles.add(obstacle)
                    },
                    onScoreUpdate = { event ->
                        println("👤 Lokaler Username: $username")
                        println("📩 Event Username: ${event.username}")
                        println("📊 Event Score: ${event.newScore}")
                        println("🟡 Match? ${event.username == username}")

                        if (event.username.equals(username, ignoreCase = true)) {
                            playerScore = event.newScore
                        } else {
                            opponentScore.value = event.newScore

                            AllClients.logClient.logEventWithTimestamp(
                                gameId = gameId,
                                username = username,
                                eventType = "opponent_update",
                                originTimestamp = event.timestamp
                            )
                        }

                    }
                )


                // 2. Kleine Wartezeit, damit die Subscriptions vollständig aktiv sind
                delay(500)

                // 3. Spiel starten – Hindernisse werden jetzt vom Server über WebSocket gepusht
                val (success, startAtServer, _) = AllClients.gameClient.startGameByGameId(
                    gameId,
                    username
                )
                if (!success) {
                    println("❌ Spielstart fehlgeschlagen für gameId: $gameId")
                    return@LaunchedEffect
                }

                // 4. Auto auf Startposition setzen
                carState.value = CarState(carX = centerX, carY = lowerY, angle = 0f)

                // 5. Countdown vorbereiten
                println("🕒 Server startAt: $startAtServer")
                val countdownTarget = startAtServer - 3000L
                val countdownStartElapsed =
                    SystemClock.elapsedRealtime() + (countdownTarget - System.currentTimeMillis())
                val gameStartElapsedTarget =
                    SystemClock.elapsedRealtime() + (startAtServer - System.currentTimeMillis())

                // 6. Warten auf Countdown-Beginn
                while (SystemClock.elapsedRealtime() < countdownStartElapsed) {
                    delay(1)
                }

                // 7. Countdown anzeigen
                for (i in 3 downTo 1) {
                    countdown = i
                    delay(1000L)
                }
                countdown = null

                // 8. Warten bis Spielstart
                while (SystemClock.elapsedRealtime() < gameStartElapsedTarget) {
                    delay(1)
                }

                // 9. Spielstart lokal registrieren & Logging
                gameStartElapsed = SystemClock.elapsedRealtime()
                val localStartTime = System.currentTimeMillis()
                val diff = localStartTime - startAtServer
                println("🚦 Spieler $username startet lokal um $localStartTime (startAt: $startAtServer, Differenz: ${diff}ms)")

                AllClients.logClient.logEventWithTimestamp(
                    gameId = gameId,
                    username = username,
                    eventType = "game_start",
                    originTimestamp = startAtServer
                )

                isStarted = true
                gameStartDelay = SystemClock.elapsedRealtime() - gameStartTime
            }


            val seenObstacleIds = remember { mutableSetOf<String>() }

            LaunchedEffect(Unit) {
                while (true) {
                    val nextObstacle = pendingObstacles.minByOrNull { it.timestamp }

                    if (nextObstacle != null) {
                        val waitTime = nextObstacle.timestamp - System.currentTimeMillis()
                        if (waitTime > 0) delay(waitTime)

                        // Nur loggen, wenn ID noch nicht verarbeitet wurde
                        if (seenObstacleIds.add(nextObstacle.id)) {
                            AllClients.logClient.logEventWithTimestamp(
                                gameId = gameId,
                                username = username,
                                eventType = "obstacle_spawned",
                                originTimestamp = nextObstacle.timestamp
                            )
                        }

                        val obstacle = Obstacle(
                            id = nextObstacle.id,
                            x = nextObstacle.x * screenWidth,
                            y = -50f,
                            timestamp = nextObstacle.timestamp
                        )
                        obstacles.add(obstacle)
                        pendingObstacles.remove(nextObstacle)
                    } else {
                        delay(10L)
                    }
                }
            }







            if (isStarted) {

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

                            if (obstacle.y > screenHeight && !obstacle.scored) {
                                obstacle.scored = true
                                toRemove.add(obstacle)

                                val start = SystemClock.elapsedRealtime()
                                val success = AllClients.gameClient.incrementScore(
                                    gameId = gameId,
                                    player = username,
                                    obstacleId = obstacle.id,
                                    originTimestamp = System.currentTimeMillis()
                                )

                                val end = SystemClock.elapsedRealtime()
                                if (success) {
                                    AllClients.logClient.logEventWithDelay(
                                        gameId,
                                        username,
                                        "score_updated",
                                        end - start
                                    )
                                }
                            }

                        }
                    }

                    obstacles.removeAll(toRemove)
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
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                color = Color.White
            )

            Text(
                "Gegner: ${opponentScore.value}" + if (isOpponentGameOver.value) " ❌" else "",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                color = if (isOpponentGameOver.value) Color.Red else Color.Yellow
            )

            if (isStarted && !isGameOver.value) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Button(onClick = {
                        carState.value = carState.value.copy(
                            carX = (carState.value.carX - moveStep).coerceIn(
                                streetLeft,
                                streetRight
                            )
                        )
                    }) { Text("⬅️ Links") }

                    Button(onClick = {
                        carState.value = carState.value.copy(
                            carX = (carState.value.carX + moveStep).coerceIn(
                                streetLeft,
                                streetRight
                            )
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
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000)),
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
