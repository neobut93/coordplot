package com.kodeco.android.coordplot.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kodeco.android.coordplot.R
import com.kodeco.android.coordplot.constants.Dimensions
import com.kodeco.android.coordplot.ui.theme.MyApplicationTheme

@Composable
fun BoxTracker(xPercent: Float, yPercent: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(Dimensions.squareBox.dp)
    ) {
        Image(
            modifier = Modifier
                .fillMaxSize(),
            painter = painterResource(id = R.drawable.back), contentDescription = "aaa")
        Box(
            modifier
                .offset(
                    (xPercent * Dimensions.squareBox - Dimensions.innerBox / 2).dp,
                    (yPercent * Dimensions.squareBox - Dimensions.innerBox / 2).dp
                )
                .size(Dimensions.innerBox.dp)
                .clip(shape = CircleShape)
        ) {
            Image(painter = painterResource(id = R.drawable.engine), contentDescription = "Engine")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapPreview() {
    MyApplicationTheme {
        BoxTracker(xPercent = 0.5f, yPercent = 0.5f)
    }
}