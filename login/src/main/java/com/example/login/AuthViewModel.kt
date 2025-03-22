package com.example.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val authClient = AuthClient()

    private val _message = MutableStateFlow("")
    val message = _message.asStateFlow()

    fun register(username: String, password: String) {
        viewModelScope.launch {
            val result = authClient.register(username, password)
            _message.value = result
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val result = authClient.login(username, password)
            _message.value = result
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            val result = authClient.testConnection()
            _message.value = result
        }
    }
}
