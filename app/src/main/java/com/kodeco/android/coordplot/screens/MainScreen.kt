package com.kodeco.android.coordplot.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kodeco.android.coordplot.R
import com.kodeco.android.coordplot.components.BoxTracker
import com.kodeco.android.coordplot.components.MapSlider
import com.kodeco.android.coordplot.ui.theme.MyApplicationTheme

@Composable
fun MainScreen() {
    Box(
    ) {
        var xPercentage: Float by remember { mutableStateOf(0.5f) }
        var yPercentage: Float by remember { mutableStateOf(0.5f) }
        Image(
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            painter = painterResource(id = R.drawable.sky), contentDescription = stringResource(id = R.string.app_background)
        )
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize(),
        ) {
            BoxTracker(xPercent = xPercentage, yPercent = yPercentage)
            MapSlider(xPercentage, yPercentage,
                xValueChanged = { xValue ->
                    xPercentage = xValue
                },
                yValueChanged = { yValue ->
                    yPercentage = yValue
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlotSurfacePreview() {
    MyApplicationTheme {
        MainScreen()
    }
}