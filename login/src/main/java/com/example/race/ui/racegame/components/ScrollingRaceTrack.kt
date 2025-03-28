package com.example.race.ui.racegame.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import com.example.login.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
@Composable
fun ScrollingRaceTrack(modifier: Modifier = Modifier) {
    var offsetY by remember { mutableStateOf(0f) }
    val scrollSpeed = 4f

    LaunchedEffect(Unit) {
        while (true) {
            offsetY += scrollSpeed
            if (offsetY >= 1000f) {
                offsetY = 0f
            }
            delay(16)
        }
    }

    Box(modifier = modifier) {
        // Obere Strecke (wird runtergeschoben)
        RaceTrack(
            Modifier.offset { IntOffset(0, offsetY.roundToInt()) }
        )
        // Zweite Strecke direkt über der ersten
        RaceTrack(
            Modifier.offset { IntOffset(0, offsetY.roundToInt() - 1000) }
        )
    }
}