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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
    var allowMovement by remember { mutableStateOf(false) }

    val gameStartTime = remember { SystemClock.elapsedRealtime() }
    var gameStartDelay by remember { mutableStateOf(0L) }
    var opponentUpdateDelay by remember { mutableStateOf(0L) }
    var playerScore by remember { mutableStateOf(0) }
    val scorableObstacles = remember { mutableStateListOf<Obstacle>() }

    var isStarted by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var startAt by remember { mutableStateOf(0L) }
    val renderTick = remember { mutableStateOf(0L) }
    var gameStartElapsed by remember { mutableStateOf(0L) }
    val previousOpponentScore = remember { mutableStateOf(0) }
    val loggedObstacleIds = remember { mutableSetOf<String>() }
    var isWebSocketReady by remember { mutableStateOf(false) }
    var isCarInitialized by remember { mutableStateOf(false) }

    val loggedKeys = remember { mutableSetOf<String>() }

    fun logEventOnceLocal(
        eventType: String,
        scheduledAt: Long,
        score: Int? = null,
        opponentUsername: String? = null
    ): Boolean {
        val key = "$eventType-$username-$scheduledAt"
        if (!loggedKeys.add(key)) return false

        return AllClients.logClient.logEventWithDelay(
            gameId = gameId,
            username = username,
            eventType = eventType,
            scheduledAt = scheduledAt,
            score = score,
            opponentUsername = opponentUsername
        )
    }


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

            var allowMovement by remember { mutableStateOf(false) }

            var timeOffset: Long = 0

            LaunchedEffect(Unit) {
                // 0. Serverzeit abfragen & Offset berechnen
                val serverTime = AllClients.gameClient.getServerTime() // <-- muss System.currentTimeMillis() liefern
                val localTime = System.currentTimeMillis()
                timeOffset = localTime - serverTime


                // 1. WebSocket verbinden
                WebSocketManager.connect(
                    gameId = gameId,
                    onObstacle = { obstacle ->
                        println("📥 Obstacle empfangen: $obstacle")
                        pendingObstacles.add(obstacle)
                    },
                    onScoreUpdate = { event ->
                        println("📨 ScoreUpdate erhalten: ${event.username}, Score: ${event.newScore}")

                        if (event.username.equals(username, ignoreCase = true)) {
                            playerScore = event.newScore
                            val roundtripDelay = System.currentTimeMillis() - event.timestamp
                            println("🌀 RTT (gRPC → Backend → Kafka/WebSocket → Client): $roundtripDelay ms")

                            AllClients.logClient.logEventWithFixedDelay(
                                gameId = gameId,
                                username = username,
                                eventType = "score_roundtrip",
                                scheduledAt = event.timestamp,
                                delayMs = roundtripDelay,
                                score = event.newScore
                            )
                        } else {
                            opponentScore.value = event.newScore
                        }
                    },
                    onConnected = {
                        println("WebSocket ist jetzt verbunden & bereit")
                        isWebSocketReady = true
                    }
                )

                // 2. Warte auf WebSocket-Readiness
                while (!isWebSocketReady) {
                    println("⏳ Warte auf WebSocket readiness...")
                    delay(100)
                }

                // 3. Spielstart über gRPC
                val grpcSentAt = System.currentTimeMillis()
                val (success, startAtServer, _) = AllClients.gameClient.startGameByGameId(gameId, username)

                if (!success || startAtServer <= 0L) {
                    println("Spielstart fehlgeschlagen – kein gültiger startAt")
                    return@LaunchedEffect
                }

                val grpcRtt = System.currentTimeMillis() - grpcSentAt
                AllClients.gameClient.measureLatency(gameId, username)?.let { latency ->
                    println("📏 Direkte gRPC-Latenz: $latency ms")
                }

                // 4. Auto initialisieren
                carState.value = CarState(carX = centerX, carY = lowerY, angle = 0f)
                isCarInitialized = true

                // 5. Warte auf Hindernisse oder Timeout (auch mit offset-sicherem Start)
                val obstacleWaitStart = System.currentTimeMillis() - timeOffset
                val obstacleWaitTimeout = 2000L
                val minObstacles = 3

                println("⏳ Warte auf $minObstacles Hindernisse oder $obstacleWaitTimeout ms...")
                while ((System.currentTimeMillis() - timeOffset) - obstacleWaitStart < obstacleWaitTimeout) {
                    if (pendingObstacles.size >= minObstacles) {
                        println(" Genug Hindernisse empfangen: ${pendingObstacles.size}")
                        break
                    }
                    delay(50)
                }

                if (pendingObstacles.isEmpty()) {
                    println(" Kein einziges Hindernis empfangen – Spiel startet trotzdem")
                }

                // 6. Warten bis Spielstart mit offset-korrigierter Zeit
                while (System.currentTimeMillis() - timeOffset < startAtServer) {
                    delay(1)
                }

                // 7. Spielstart registrieren
                gameStartElapsed = SystemClock.elapsedRealtime()
                val localSyncedStart = System.currentTimeMillis() - timeOffset
                val diff = localSyncedStart - startAtServer

                AllClients.logClient.logEventWithFixedDelay(
                    gameId = gameId,
                    username = username,
                    eventType = "game_start",
                    scheduledAt = startAtServer,
                    delayMs = diff.coerceAtLeast(0)
                )

                // 8. Spiel starten
                isStarted = true
                delay(200)
                allowMovement = true
                gameStartDelay = SystemClock.elapsedRealtime() - gameStartTime
            }




            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    AllClients.gameClient.measureLatency(gameId, username)?.let { latency ->


                    }
                }
            }



            val seenObstacleIds = remember { mutableSetOf<String>() }

            LaunchedEffect(Unit) {
                while (true) {
                    val nextObstacle = pendingObstacles.minByOrNull { it.timestamp }

                    if (nextObstacle != null) {
                        val waitTime = nextObstacle.timestamp - (System.currentTimeMillis() - timeOffset)
                        if (waitTime > 0) delay(waitTime)

                        // Nur anzeigen + loggen, wenn die ID noch nicht verarbeitet wurde
                        if (seenObstacleIds.add(nextObstacle.id)) {
                            val obstacle = Obstacle(
                                id = nextObstacle.id,
                                x = nextObstacle.x * screenWidth,
                                y = -50f,
                                timestamp = nextObstacle.timestamp
                            )
                            obstacles.add(obstacle)

                            logEventOnceLocal(
                                eventType = "obstacle_spawned",
                                scheduledAt = nextObstacle.timestamp
                            )
                            logEventOnceLocal(
                                eventType = "obstacle_spawned_latency",
                                scheduledAt = nextObstacle.timestamp
                            )




                        }

                        pendingObstacles.remove(nextObstacle)
                    } else {
                        delay(10L)
                    }
                }
            }


            val scoreQueue = remember { mutableStateListOf<Obstacle>() } // GANZ OBEN

            if (isStarted) {

                // 1. Render Tick für Game Loop
                LaunchedEffect(Unit) {
                    while (true) {
                        renderTick.value = System.currentTimeMillis()
                        delay(16L) // 60 FPS
                    }
                }

                // 2. SPIEL-LOGIK mit Bewegungen
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

                            // Score nur, wenn unterhalb Schwelle & noch nicht gescored
                            if (!obstacle.scored && obstacle.y > screenHeight - obstacle.height / 2) {
                                println("🧮 Versuche Score für Obstacle: ${obstacle.id}")
                                obstacle.scored = true
                                toRemove.add(obstacle)
                                scoreQueue.add(obstacle)
                            }
                        }
                    }

                    obstacles.removeAll(toRemove)
                }


                LaunchedEffect(Unit) {
                    while (true) {
                        if (scoreQueue.isNotEmpty()) {
                            val obstacle = scoreQueue.removeFirstOrNull()
                            if (obstacle != null) {
                                val originTimestamp = System.currentTimeMillis()
                                val success = AllClients.gameClient.incrementScore(
                                    gameId = gameId,
                                    player = username,
                                    obstacleId = obstacle.id,
                                    originTimestamp = originTimestamp
                                )
                                if (!success) {
                                    println("Score konnte nicht erhöht werden für ${obstacle.id}")
                                    println("Wurde trotzdem als scored markiert: ${obstacle.scored}")
                                    obstacle.scored = false
                                } else {
                                    println("✅ incrementScore für ${obstacle.id} (wirklich gescored!)")
                                    val now = System.currentTimeMillis()
                                    val delay = now - originTimestamp
                                    AllClients.logClient.logEventWithFixedDelay(
                                        gameId = gameId,
                                        username = username,
                                        eventType = "score_updated",
                                        scheduledAt = originTimestamp,
                                        delayMs = delay,
                                        score = playerScore
                                    )
                                }
                            }
                        }
                        delay(30L)
                    }
                }

            }


            ScrollingRaceTrack()
            if (isCarInitialized) {
                Car(carState = carState.value)
            }
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
                obstacles.filter { !it.scored }.forEach {
                    println("⚠️ Nicht gescored: ${it.id}, y=${it.y}")
                }
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
                                else -> {
                                    isOpponentGameOver.value = true
                                    "😢 Du hast verloren"
                                }
                            }
                            break
                        }
                        delay(250)
                    }
                    WebSocketManager.disconnect()

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
