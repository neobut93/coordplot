package com.kodeco.android.coordplot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kodeco.android.coordplot.R
import com.kodeco.android.coordplot.ui.theme.MyApplicationTheme
import com.kodeco.android.coordplot.utils.UtilMethods.convertFloatToInt
import kotlin.math.roundToInt

@Composable
fun MapSlider(
    xPercentage: Float,
    yPercentage: Float,
    xValueChanged: (Float) -> Unit,
    yValueChanged: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = stringResource(id = R.string.x_axis_title, convertFloatToInt(xPercentage)),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .requiredWidth(120.dp)
            )
            Slider(
                value = xPercentage,
                onValueChange = xValueChanged,
                valueRange = 0.001f..1f,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.y_axis_title, convertFloatToInt(yPercentage)),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .requiredWidth(120.dp)
            )
            Slider(
                value = yPercentage,
                onValueChange = yValueChanged,
                valueRange = 0.001f..1f
            )
        }
    }
}

@Preview
@Composable
fun MapSliderPreview() {
    var xPercentage: Float by remember { mutableStateOf(0.5f) }
    var yPercentage: Float by remember { mutableStateOf(0.5f) }
    MyApplicationTheme {
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