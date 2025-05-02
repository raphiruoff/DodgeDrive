package com.example.race.ui.session

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.race.common.TokenUtils
import com.example.race.data.network.AllClients
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
    val sessionClient = remember { AllClients.sessionClient }
    val friendClient = remember { AllClients.friendListClient }
    val coroutineScope = rememberCoroutineScope()

    var friends by remember { mutableStateOf<List<String>>(emptyList()) }
    val invitedFriends = remember { mutableStateListOf<String>() }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf("") }
    var invitations by remember { mutableStateOf<List<de.ruoff.consistency.service.session.Session.Invitation>>(emptyList()) }

    LaunchedEffect(username) {
        if (username == null) {
            infoMessage = "Benutzername konnte nicht gelesen werden"
            return@LaunchedEffect
        }

        try {
            friends = withContext(Dispatchers.IO) {
                friendClient.getFriends(username)
            }

            invitations = withContext(Dispatchers.IO) {
                sessionClient.getInvitations(username).also {
                    Log.d("SessionScreen", "Einladungen für $username: $it")
                }
            }

            if (invitations.isNotEmpty()) {
                infoMessage = "Du hast ${invitations.size} Einladung(en)"
            }

        } catch (e: Exception) {
            infoMessage = "Fehler beim Initialisieren:\n${e::class.simpleName}: ${e.message}"
            Log.e("SessionScreen", "Initialisierungsfehler", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                if (sessionId != null && username != null) {
                    try {
                        sessionClient.leaveSession(sessionId!!, username)
                    } catch (e: Exception) {
                        Log.e("SessionScreen", "Fehler beim Verlassen der Session", e)
                    }
                }
            }
        }
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
                                        if (username == null) {
                                            infoMessage = "Benutzername fehlt"
                                            return@launch
                                        }

                                        if (sessionId == null) {
                                            // Session wird erst bei Einladung erstellt
                                            sessionId = withContext(Dispatchers.IO) {
                                                sessionClient.createSession(username)
                                            }
                                        }

                                        invitedFriends.add(friend)

                                        try {
                                            val success = withContext(Dispatchers.IO) {
                                                sessionClient.invitePlayer(username, friend)
                                            }

                                            if (!success) {
                                                infoMessage = "Einladung an $friend fehlgeschlagen"
                                                invitedFriends.remove(friend)
                                            }
                                        } catch (e: Exception) {
                                            infoMessage = "Fehler beim Einladen:\n${e::class.simpleName}: ${e.message}"
                                            Log.e("SessionScreen", "Fehler beim Einladen", e)
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

        if (invitations.isNotEmpty()) {
            Text("📨 Einladungen", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(invitations) { invitation ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Von ${invitation.requester}")

                            Button(onClick = {
                                coroutineScope.launch {
                                    val accepted = withContext(Dispatchers.IO) {
                                        sessionClient.acceptInvitation(invitation.sessionId, username!!)
                                    }
                                    if (accepted) {
                                        sessionId = invitation.sessionId
                                        infoMessage = "Einladung angenommen. Session ID: ${invitation.sessionId}"
                                        invitations = emptyList()
                                    } else {
                                        infoMessage = "Fehler beim Annehmen der Einladung"
                                    }
                                }
                            }) {
                                Text("Annehmen")
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
