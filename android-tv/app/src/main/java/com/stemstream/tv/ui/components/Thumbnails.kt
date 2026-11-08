package com.stemstream.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.stemstream.tv.data.Video
import com.stemstream.tv.data.categoryColor
import com.stemstream.tv.ui.theme.Ink
import com.stemstream.tv.ui.theme.SurfaceRaised

enum class ThumbnailQuality {
    CARD,
    HERO,
}

private const val YOUTUBE_THUMB_BASE = "https://i.ytimg.com/vi/"

internal fun thumbnailUrl(youtubeId: String, quality: String): String =
    "$YOUTUBE_THUMB_BASE$youtubeId/$quality.jpg"

/**
 * Thumbnail loader with a two-step fallback. `maxresdefault` is not generated
 * for every upload, so the hero falls back to `hqdefault`; card images use the
 * almost-universally present `hqdefault` and then `mqdefault`.
 *
 * A failed primary request changes a small Int state, so only this image is
 * recomposed. There is no crossfade: focus-driven scrolling stays crisp and
 * cheap on low-end Fire TV sticks.
 */
@Composable
fun VideoThumbnail(
    video: Video,
    modifier: Modifier = Modifier,
    quality: ThumbnailQuality = ThumbnailQuality.CARD,
) {
    var stage by remember(video.youtubeId) { mutableIntStateOf(0) }
    val context = LocalContext.current
    val primaryQuality = if (quality == ThumbnailQuality.HERO) "maxresdefault" else "hqdefault"
    val fallbackQuality = if (quality == ThumbnailQuality.HERO) "hqdefault" else "mqdefault"
    val targetWidth = if (quality == ThumbnailQuality.HERO) 1280 else 480
    val targetHeight = if (quality == ThumbnailQuality.HERO) 720 else 360

    val request = remember(video.youtubeId, stage, quality) {
        ImageRequest.Builder(context)
            .data(
                thumbnailUrl(
                    youtubeId = video.youtubeId,
                    quality = if (stage == 0) primaryQuality else fallbackQuality,
                ),
            )
            .size(targetWidth, targetHeight)
            .build()
    }

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(categoryColor(video.category).copy(alpha = 0.22f), SurfaceRaised),
            ),
        ),
    ) {
        if (stage < 2) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = { stage += 1 },
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                StemMark(
                    modifier = Modifier
                        .size(52.dp)
                        .alpha(0.22f),
                )
                Text(
                    text = video.category,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Start,
                )
            }
        }

        // A subtle bottom scrim keeps the duration badge legible on bright stills.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.55f to Color.Transparent,
                            1f to Color(0xB3000000),
                        ),
                    ),
                ),
        )
    }
}
