package com.example.race.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.race.common.TokenUtils
import com.example.race.data.network.FriendListClient
import com.example.race.data.network.SessionClient
import com.example.race.data.network.TokenHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SessionScreen(
    onNavigateToRaceGame: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val username = remember { TokenUtils.decodeUsername(TokenHolder.jwtToken) }
    val friendClient = remember { FriendListClient() }
    val sessionClient = remember { SessionClient() }
    val coroutineScope = rememberCoroutineScope()

    var friends by remember { mutableStateOf<List<String>>(emptyList()) }
    val invitedFriends = remember { mutableStateListOf<String>() }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf("") }

    LaunchedEffect(username) {
        if (username == null) {
            infoMessage = "Benutzername konnte nicht gelesen werden"
            return@LaunchedEffect
        }

        println(">> SessionScreen gestartet mit Username: $username")

        try {
            println(">> Lade Freundesliste...")
            friends = withContext(Dispatchers.IO) {
                friendClient.getFriends(username)
            }
            println(">> Freunde geladen: $friends")

            println(">> Frage offene Session ab...")
            val existingSession = withContext(Dispatchers.IO) {
                sessionClient.getOpenSessionForPlayer(username)
            }

            sessionId = if (existingSession != null && existingSession.status == "WAITING_FOR_PLAYER") {
                println(">> Es gibt bereits eine offene Session: ${existingSession.sessionId}")
                existingSession.sessionId
            } else {
                println(">> Es gibt keine offene Session – erstelle eine neue.")
                withContext(Dispatchers.IO) {
                    sessionClient.createSession(username)
                }.also {
                    println(">> Neue Session erstellt mit ID: $it")
                }
            }

        } catch (e: Exception) {
            println(">> Fehler beim Erstellen oder Laden der Session:")
            e.printStackTrace()
            infoMessage = "Fehler beim Laden oder Erstellen der Session: ${e.message ?: "unbekannt"}"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                if (sessionId != null && username != null) {
                    try {
                        println("🧹 Verlasse Session $sessionId für $username")
                        sessionClient.leaveSession(sessionId!!, username)
                    } catch (e: Exception) {
                        println("⚠️ Fehler beim Verlassen der Session: ${e.message}")
                    }
                }
            }
        }
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🎯 Session erstellen", style = MaterialTheme.typography.headlineMedium)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(friends) { friend ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(friend)

                        if (invitedFriends.contains(friend)) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        if (username == null || sessionId == null) {
                                            infoMessage = "Session oder Benutzername fehlt"
                                            return@launch
                                        }

                                        invitedFriends.add(friend) // Ladeindikator anzeigen

                                        try {
                                            val success = withContext(Dispatchers.IO) {
                                                sessionClient.invitePlayer(username, friend)
                                            }

                                            if (!success) {
                                                infoMessage = "Einladung an $friend fehlgeschlagen"
                                                invitedFriends.remove(friend)
                                            }
                                        } catch (e: Exception) {
                                            infoMessage = "Fehler beim Einladen: ${e.message ?: "unbekannt"}"
                                            invitedFriends.remove(friend)
                                        }
                                    }
                                },
                                enabled = !isLoading
                            ) {
                                Text("Einladen")
                            }
                        }
                    }
                }
            }
        }

        if (infoMessage.isNotBlank()) {
            Text(
                text = infoMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToRaceGame,
            modifier = Modifier.fillMaxWidth(),
            enabled = invitedFriends.isNotEmpty() && sessionId != null
        ) {
            Text("🚗 Spiel starten")
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⬅️ Zurück")
        }
    }
}
