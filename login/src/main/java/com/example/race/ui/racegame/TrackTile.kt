package com.example.race.ui.racegame

data class TrackTile(
    val type: String, // "straight" oder "corner"
    val x: Int,
    val y: Int,
    val rotation: Float // in Grad
)
