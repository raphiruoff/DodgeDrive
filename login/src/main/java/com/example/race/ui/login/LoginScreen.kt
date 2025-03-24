package com.example.race.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToSession: () -> Unit
) {
    val message by authViewModel.message.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { authViewModel.testGrpcConnection() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("gRPC-Verbindung testen")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { authViewModel.sendPing() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ping senden")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = message)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onNavigateToSession() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Weiter zum Session Screen")
        }
    }
}
