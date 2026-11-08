package com.stemstream.tv.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun contentPaddingFor(screenWidthDp: Int): Dp =
    if (screenWidthDp >= 1200) 48.dp else 36.dp

fun railCardWidthFor(screenWidthDp: Int): Dp = when {
    screenWidthDp >= 1600 -> 320.dp
    screenWidthDp >= 1200 -> 290.dp
    screenWidthDp >= 960 -> 250.dp
    else -> 220.dp
}

fun gridColumnsFor(screenWidthDp: Int): Int = when {
    screenWidthDp >= 1600 -> 6
    screenWidthDp >= 1200 -> 5
    screenWidthDp >= 900 -> 4
    else -> 3
}

fun heroHeightFor(screenHeightDp: Int): Dp = when {
    screenHeightDp >= 700 -> 470.dp
    screenHeightDp >= 540 -> 370.dp
    else -> 320.dp
}
