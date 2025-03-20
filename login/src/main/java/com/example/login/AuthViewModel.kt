package com.example.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.login.proto.AuthServiceGrpc
import com.example.login.proto.RegisterRequest
import com.example.login.proto.LoginRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {

    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress("10.0.2.2", 9090) // Emulator -> localhost
        .usePlaintext()
        .build()

    private val stub = AuthServiceGrpc.newBlockingStub(channel)

    private val _message = MutableStateFlow("")
    val message = _message.asStateFlow()

    fun register(username: String, password: String) {
        viewModelScope.launch {
            try {
                val request = RegisterRequest.newBuilder()
                    .setUsername(username)
                    .setPassword(password)
                    .build()

                val response = stub.register(request)
                _message.value = response.message
            } catch (e: Exception) {
                _message.value = "Registrierung fehlgeschlagen: ${e.message}"
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val request = LoginRequest.newBuilder()
                    .setUsername(username)
                    .setPassword(password)
                    .build()

                val response = stub.login(request)
                _message.value = response.message
            } catch (e: Exception) {
                _message.value = "Login fehlgeschlagen: ${e.message}"
            }
        }
    }
}
