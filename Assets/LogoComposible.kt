package com.example.healthmeter.Assets


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.healthmeter.R

// This file contains Composable functions to display various logos used in the app.
@Composable
fun BlackSideLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.black_side_logo),
        contentDescription = "Black Side Logo",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
fun BlackMiddleLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.black_middle_logo),
        contentDescription = "Black Middle Logo",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
fun WhiteMiddleLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.white_middle_logo),
        contentDescription = "White Middle Logo",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
fun WhiteSideLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.white_side_logo),
        contentDescription = "White Side Logo",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
