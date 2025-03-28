package com.example.race.ui.racegame.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.login.R


@Composable
fun RaceTrack(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Image(
                painter = painterResource(R.drawable.wiese_links_custom),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .weight(4f)
        ) {
            Image(
                painter = painterResource(R.drawable.road_center),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Image(
                painter = painterResource(R.drawable.wiese_rechts_custom),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
