package com.stemstream.tv.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The eight glyphs this app draws, declared as vectors.
 *
 * `material-icons-core` is deprecated and, in the version the Compose BOM
 * resolves, no longer ships the icons used here — which is why the build failed
 * on `PlayArrow`, `Refresh`, `ArrowForward` and `Close`. Shipping the paths
 * ourselves removes the dependency, the transitive download and any future
 * breakage, at a cost of a few hundred bytes. `Icon` still tints them, so the
 * white fill is not the rendered colour.
 */
object AppIcons {
    val Home: ImageVector = icon("Home", "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z")

    val Browse: ImageVector = icon(
        "Browse",
        "M3,13h2v-2H3v2zm0,4h2v-2H3v2zm0,-8h2V7H3v2zm4,4h14v-2H7v2zm0,4h14v-2H7v2zM7,7v2h14V7H7z",
    )

    val Search: ImageVector = icon(
        "Search",
        "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 " +
            "5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zm-6,0" +
            "C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z",
    )

    val Info: ImageVector = icon(
        "Info",
        "M11,7h2v2h-2zM11,11h2v6h-2zM12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z",
    )

    val Play: ImageVector = icon("Play", "M8,5v14l11,-7z")

    val Pause: ImageVector = icon("Pause", "M6,19h4V5H6v14zM14,5v14h4V5h-4z")

    val Surprise: ImageVector = icon(
        "Surprise",
        "M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -7.99,8s3.57,8 7.99,8c3.73,0 6.84,-2.55 " +
            "7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 " +
            "3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z",
    )

    val ArrowForward: ImageVector = icon(
        "ArrowForward",
        "M12,4l-1.41,1.41L16.17,11H4v2h12.17l-5.58,5.59L12,20l8,-8z",
    )

    val Close: ImageVector = icon(
        "Close",
        "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z",
    )
}

private fun icon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.White),
    ).build()
