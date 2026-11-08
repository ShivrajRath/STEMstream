package com.stemstream.tv.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.stemstream.tv.ui.components.AppIcons

enum class Tab(
    val label: String,
    val icon: ImageVector,
) {
    Home("Home", AppIcons.Home),
    Browse("Browse", AppIcons.Browse),
    Search("Search", AppIcons.Search),
    About("About", AppIcons.Info),
}
