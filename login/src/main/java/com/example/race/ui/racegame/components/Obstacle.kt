package com.example.race.ui.racegame.components

data class Obstacle(
    val id: String,
    val x: Float,
    var y: Float,
    val width: Float = 48f,
    val height: Float = 48f,
    val timestamp: Long = 0L,
    var scored: Boolean = false
)
