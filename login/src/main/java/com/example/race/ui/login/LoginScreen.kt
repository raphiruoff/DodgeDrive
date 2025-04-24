package com.example.race.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToSession: () -> Unit
) {
    val message by authViewModel.message.collectAsState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "🚗 Race Game Login", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Passwort") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { authViewModel.login(username, password, onNavigateToSession) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔐 Login")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { authViewModel.register(username, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📝 Registrieren")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Divider()

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { authViewModel.sendPing() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📡 Ping senden")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { authViewModel.testGrpcConnection() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🧪 gRPC-Verbindung testen")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = message)
    }
}
