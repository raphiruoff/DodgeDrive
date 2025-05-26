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
import com.example.race.data.network.AllClients
import com.example.race.data.network.GameClient
import com.example.race.data.network.TokenHolder
import de.ruoff.consistency.service.session.Session
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SessionScreen(
    onNavigateToRaceGame: (gameId: String, username: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val username = remember { TokenUtils.decodeUsername(TokenHolder.jwtToken) }
    val sessionClient = remember { AllClients.sessionClient }
    val friendClient = remember { AllClients.friendClient }
    val coroutineScope = rememberCoroutineScope()

    var friends by remember { mutableStateOf<List<String>>(emptyList()) }
    val invitedFriends = remember { mutableStateListOf<String>() }
    val acceptedFriends = remember { mutableStateListOf<String>() }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var sessionPartner by remember { mutableStateOf<String?>(null) }
    var sessionStatus by remember { mutableStateOf<String?>(null) }
    var session by remember { mutableStateOf<Session.GetSessionResponse?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf("") }
    var invitations by remember { mutableStateOf<List<Session.Invitation>>(emptyList()) }

    val streamRegistered = remember { mutableStateOf(false) }

    fun refreshInvitations() {
        coroutineScope.launch {
            username?.let {
                invitations = withContext(Dispatchers.IO) { sessionClient.getInvitations(it) }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (username == null) return@LaunchedEffect

        try {
            friends = withContext(Dispatchers.IO) { friendClient.getFriends(username) }
            invitations = withContext(Dispatchers.IO) { sessionClient.getInvitations(username) }
        } catch (_: Exception) {}

        if (!streamRegistered.value) {
            streamRegistered.value = true
            sessionClient.streamInvitations(username, object : StreamObserver<Session.Invitation> {
                override fun onNext(value: Session.Invitation) {
                    coroutineScope.launch {
                        refreshInvitations()
                        infoMessage = "📨 Neue Einladung von ${value.requester}"
                    }
                }
                override fun onError(t: Throwable) {}
                override fun onCompleted() {}
            })
        }
    }

    LaunchedEffect(sessionId) {
        val safeUsername = username
        val safeSessionId = sessionId
        if (safeSessionId != null && safeUsername != null) {
            while (true) {
                delay(500L)
                val session = withContext(Dispatchers.IO) { sessionClient.getSession(safeSessionId) }
                if (session != null) {
                    sessionStatus = session.status
                    if (session.status == "ACTIVE" && session.playerB != null) {
                        val partner = if (session.playerA == safeUsername) session.playerB else session.playerA
                        sessionPartner = partner
                        if (partner != null && !acceptedFriends.contains(partner)) {
                            acceptedFriends.add(partner)
                        }
                        invitedFriends.remove(partner)
                    }

                    if (session.status == "WAITING_FOR_START") {
                        val game = withContext(Dispatchers.IO) {
                            GameClient().getGameBySession(safeSessionId)
                        }

                        if (game != null && game.gameId.isNotBlank()) {
                            onNavigateToRaceGame(game.gameId, safeUsername)
                            break
                        } else {
                            infoMessage = "⏳ Warte auf Spielinitialisierung..."
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                val safeUsername = username
                val safeSessionId = sessionId
                if (safeSessionId != null && safeUsername != null) {
                    try {
                        sessionClient.leaveSession(safeSessionId, safeUsername)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
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
                        when {
                            acceptedFriends.contains(friend) -> {
                                Button(onClick = {}, enabled = false) { Text("✅") }
                            }

                            invitedFriends.contains(friend) -> {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }

                            else -> {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val safeUsername = username
                                            if (safeUsername != null) {
                                                // Session erzeugen (nur wenn noch keine existiert)
                                                if (sessionId == null) {
                                                    val sentAt = System.currentTimeMillis()
                                                    val createdSessionId = withContext(Dispatchers.IO) {
                                                        sessionClient.createSession(safeUsername)
                                                    }
                                                    val receivedAt = System.currentTimeMillis()
                                                    val delayMs = receivedAt - sentAt

                                                    sessionId = createdSessionId

                                                    AllClients.logClient.logEventWithFixedDelay(
                                                        gameId = createdSessionId,
                                                        username = safeUsername,
                                                        eventType = "session_created",
                                                        scheduledAt = sentAt,
                                                        delayMs = delayMs
                                                    )
                                                }

                                                // Spieler einladen
                                                invitedFriends.add(friend)

                                                val sentAtInvite = System.currentTimeMillis()
                                                val success = withContext(Dispatchers.IO) {
                                                    sessionClient.invitePlayer(safeUsername, friend)
                                                }
                                                val receivedAtInvite = System.currentTimeMillis()
                                                val inviteDelay = receivedAtInvite - sentAtInvite

                                                if (success) {
                                                    AllClients.logClient.logEventWithFixedDelay(
                                                        gameId = sessionId ?: "unknown",
                                                        username = safeUsername,
                                                        eventType = "invitation_sent",
                                                        scheduledAt = sentAtInvite,
                                                        delayMs = inviteDelay,
                                                        opponentUsername = friend
                                                    )
                                                } else {
                                                    invitedFriends.remove(friend)
                                                }
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
        }


        if (invitations.isNotEmpty()) {
            Text("📨 Einladungen", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(invitations) { invitation ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Von ${invitation.requester}")
                            Button(onClick = {
                                coroutineScope.launch {
                                    val safeUsername = username
                                    if (safeUsername != null) {
                                        val sentAtAccept = System.currentTimeMillis()
                                        val accepted = withContext(Dispatchers.IO) {
                                            sessionClient.acceptInvitation(invitation.sessionId, safeUsername)
                                        }
                                        val receivedAtAccept = System.currentTimeMillis()
                                        val delayAccept = receivedAtAccept - sentAtAccept

                                        if (accepted) {
                                            AllClients.logClient.logEventWithFixedDelay(
                                                gameId = invitation.sessionId,
                                                username = safeUsername,
                                                eventType = "invitation_accepted",
                                                scheduledAt = sentAtAccept,
                                                delayMs = delayAccept,
                                                opponentUsername = invitation.requester
                                            )

                                            sessionId = invitation.sessionId
                                            invitations = emptyList()
                                            val session = withContext(Dispatchers.IO) {
                                                sessionClient.getSession(invitation.sessionId)
                                            }
                                            session?.let {
                                                sessionPartner = if (it.playerA == safeUsername) it.playerB else it.playerA
                                                sessionStatus = it.status
                                            }
                                        }
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

        sessionPartner?.let {
            Text(
                "✅ Du bist in einer Session mit: $it",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
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
            onClick = {
                coroutineScope.launch {
                    val safeSessionId = sessionId
                    val safeUsername = username
                    if (safeSessionId != null && safeUsername != null && sessionStatus == "ACTIVE") {
                        val sentAt = System.currentTimeMillis()

                        val (success, _, gameId) = withContext(Dispatchers.IO) {
                            sessionClient.triggerGameStart(safeSessionId, safeUsername)
                        }


                        if (!success) {
                            infoMessage = "Spielstart fehlgeschlagen"
                        } else {
                            sessionStatus = "WAITING_FOR_START"
                        }

                        withContext(Dispatchers.IO) {
                            AllClients.logClient.exportLogs(safeSessionId)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = sessionPartner != null && sessionStatus == "ACTIVE"
        ) {
            Text("🚗 Spiel starten")
        }

        Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
            Text("⬅️ Zurück")
        }
    }
}
