package com.stemstream.tv.ui.components

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.stemstream.tv.ui.theme.Accent
import com.stemstream.tv.ui.theme.Faint
import kotlinx.coroutines.delay
import java.util.Date

/** A tiny Canvas atom so the brand mark never needs a bitmap or a network call. */
@Composable
fun StemMark(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    accent: Color = Accent,
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = size.minDimension * 0.065f
        val ovalWidth = width * 0.88f
        val ovalHeight = height * 0.36f
        val topLeft = Offset(
            x = (width - ovalWidth) / 2f,
            y = (height - ovalHeight) / 2f,
        )
        val pivot = Offset(width / 2f, height / 2f)

        for (angle in listOf(0f, 60f, 120f)) {
            rotate(degrees = angle, pivot = pivot) {
                drawOval(
                    color = tint,
                    topLeft = topLeft,
                    size = Size(ovalWidth, ovalHeight),
                    style = Stroke(width = strokeWidth),
                )
            }
        }

        drawCircle(
            color = accent,
            radius = size.minDimension * 0.11f,
            center = pivot,
        )
    }
}

@Composable
fun BrandLockup(
    modifier: Modifier = Modifier,
    markSize: Dp = 28.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        StemMark(modifier = Modifier.size(markSize))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "STEMStream",
            style = textStyle,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
fun TvClock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var label by remember { mutableStateOf(currentTime(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            label = currentTime(context)
        }
    }

    Text(
        text = label,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = Faint,
    )
}

private fun currentTime(context: Context): String {
    return DateFormat.getTimeFormat(context).format(Date())
}
