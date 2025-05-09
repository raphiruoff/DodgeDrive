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
import com.example.race.data.network.NotificationClient
import com.example.race.data.network.TokenHolder
import de.ruoff.consistency.service.notification.InvitationNotification
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
    val friendClient = remember { AllClients.friendListClient }
    val coroutineScope = rememberCoroutineScope()

    var friends by remember { mutableStateOf<List<String>>(emptyList()) }
    val invitedFriends = remember { mutableStateListOf<String>() }
    val acceptedFriends = remember { mutableStateListOf<String>() }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var sessionPartner by remember { mutableStateOf<String?>(null) }
    var sessionStatus by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf("") }
    var invitations by remember { mutableStateOf<List<de.ruoff.consistency.service.session.Session.Invitation>>(emptyList()) }

    var countdown by remember { mutableStateOf(0) }
    var isCountingDown by remember { mutableStateOf(false) }
    val streamRegistered = remember { mutableStateOf(false) }
    val notificationClient = remember { NotificationClient() }

    LaunchedEffect(Unit) {
        if (username == null) {
            infoMessage = "Benutzername konnte nicht gelesen werden"
            return@LaunchedEffect
        }

        try {
            friends = withContext(Dispatchers.IO) { friendClient.getFriends(username) }
            invitations = withContext(Dispatchers.IO) { sessionClient.getInvitations(username) }
            if (invitations.isNotEmpty()) {
                infoMessage = "Du hast ${invitations.size} Einladung(en)"
            }
        } catch (e: Exception) {
            infoMessage = "Fehler beim Initialisieren:\n${e::class.simpleName}: ${e.message}"
        }

        if (!streamRegistered.value) {
            streamRegistered.value = true
            notificationClient.streamInvitations(username, object : StreamObserver<InvitationNotification> {
                override fun onNext(value: InvitationNotification) {
                    coroutineScope.launch {
                        val newInvite = de.ruoff.consistency.service.session.Session.Invitation.newBuilder()
                            .setSessionId(value.sessionId)
                            .setRequester(value.requester)
                            .build()
                        invitations = invitations + newInvite
                        infoMessage = "📨 Neue Einladung von ${value.requester}"
                    }
                }

                override fun onError(t: Throwable) {}
                override fun onCompleted() {}
            })
        }
    }

    LaunchedEffect(sessionId) {
        if (sessionId != null && username != null) {
            while (true) {
                delay(2000L)
                val session = withContext(Dispatchers.IO) { sessionClient.getSession(sessionId!!) }
                if (session != null) {
                    sessionStatus = session.status

                    if (session.status == "ACTIVE" && session.playerB != null) {
                        val partner = if (session.playerA == username) session.playerB else session.playerA
                        sessionPartner = partner
                        if (partner != null && !acceptedFriends.contains(partner)) {
                            acceptedFriends.add(partner)
                        }
                        invitedFriends.remove(partner)
                        infoMessage = "✅ $partner hat die Einladung angenommen!"
                    }

                    if (session.status == "WAITING_FOR_START" && !isCountingDown) {
                        isCountingDown = true
                        coroutineScope.launch {
                            for (i in 3 downTo 1) {
                                countdown = i
                                delay(1000L)
                            }
                            countdown = 0

                            val safeSessionId = sessionId ?: return@launch
                            val safeUsername = username
                            val safePartner = sessionPartner

                            if (safePartner != null) {
                                val gameId = withContext(Dispatchers.IO) {
                                    GameClient().getGameBySession(safeSessionId)?.gameId
                                        ?: GameClient().createGame(safeSessionId, safeUsername, safePartner)
                                }

                                if (!gameId.isNullOrBlank()) {
                                    onNavigateToRaceGame(gameId, username)
                                } else {
                                    infoMessage = "❌ Spiel konnte nicht erstellt werden"
                                    isCountingDown = false
                                }
                            } else {
                                infoMessage = "❌ Kein gültiger Mitspieler"
                                isCountingDown = false
                            }
                        }
                        break
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                if (sessionId != null && username != null) {
                    try {
                        sessionClient.leaveSession(sessionId!!, username)
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
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
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
                                            if (username == null) return@launch
                                            if (sessionId == null) {
                                                sessionId = withContext(Dispatchers.IO) {
                                                    sessionClient.createSession(username)
                                                }
                                            }
                                            invitedFriends.add(friend)
                                            val success = withContext(Dispatchers.IO) {
                                                sessionClient.invitePlayer(username, friend)
                                            }
                                            if (!success) {
                                                infoMessage = "Einladung an $friend fehlgeschlagen"
                                                invitedFriends.remove(friend)
                                            }
                                        }
                                    },
                                    enabled = !isLoading
                                ) { Text("Einladen") }
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
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Von ${invitation.requester}")
                            Button(onClick = {
                                coroutineScope.launch {
                                    if (username == null) return@launch
                                    val accepted = withContext(Dispatchers.IO) {
                                        sessionClient.acceptInvitation(invitation.sessionId, username)
                                    }
                                    if (accepted) {
                                        sessionId = invitation.sessionId
                                        invitations = emptyList()
                                        val session = withContext(Dispatchers.IO) {
                                            sessionClient.getSession(invitation.sessionId)
                                        }
                                        session?.let {
                                            sessionPartner = if (it.playerA == username) it.playerB else it.playerA
                                            infoMessage = "✅ Du bist in einer Session mit ${sessionPartner ?: "unbekannt"}"
                                            sessionStatus = it.status
                                        }
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

        sessionPartner?.let {
            Text(
                text = "✅ Du bist in einer Session mit: $it",
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
                    if (sessionId != null && username != null && sessionStatus == "ACTIVE") {
                        val started = withContext(Dispatchers.IO) {
                            sessionClient.startGame(sessionId!!, username)
                        }
                        if (!started) {
                            infoMessage = "Spielstart fehlgeschlagen"
                        } else {
                            sessionStatus = "WAITING_FOR_START"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = sessionPartner != null && !isCountingDown && sessionStatus == "ACTIVE"
        ) {
            Text("🚗 Spiel starten")
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⬅️ Zurück")
        }

        if (isCountingDown) {
            Text(
                text = "$countdown",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
