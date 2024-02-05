package com.kodeco.android.coordplot.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kodeco.android.coordplot.R
import com.kodeco.android.coordplot.components.BoxTracker
import com.kodeco.android.coordplot.components.MapSlider
import com.kodeco.android.coordplot.ui.theme.MyApplicationTheme

@Composable
fun MainScreen() {
    val orientation = LocalConfiguration.current.orientation
    var xPercentage: Float by rememberSaveable { mutableFloatStateOf(0.5f) }
    var yPercentage: Float by rememberSaveable { mutableFloatStateOf(0.5f) }
    Box {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 15.dp)
            ,
            contentScale = ContentScale.Crop,
            painter = painterResource(id = R.drawable.sky), contentDescription = stringResource(id = R.string.app_background)
        )
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Row(
                modifier = Modifier
                    .padding(start = 20.dp, top = 30.dp, bottom = 30.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
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
        } else {
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
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MyApplicationTheme {
        MainScreen()
    }
}