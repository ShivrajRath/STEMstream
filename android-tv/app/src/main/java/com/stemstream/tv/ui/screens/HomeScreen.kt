package com.stemstream.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.stemstream.tv.data.Library
import com.stemstream.tv.data.Video
import com.stemstream.tv.data.categoryColor
import com.stemstream.tv.ui.components.Hero
import com.stemstream.tv.ui.components.StemMark
import com.stemstream.tv.ui.components.VideoRailSection
import com.stemstream.tv.ui.contentPaddingFor
import com.stemstream.tv.ui.heroHeightFor
import com.stemstream.tv.ui.railCardWidthFor
import com.stemstream.tv.ui.theme.AccentAmber
import com.stemstream.tv.ui.theme.Faint
import com.stemstream.tv.ui.theme.Ink
import com.stemstream.tv.ui.theme.Line
import com.stemstream.tv.ui.theme.Muted

@Composable
fun HomeScreen(
    library: Library,
    onOpenVideo: (Video) -> Unit,
    onPlayVideo: (Video) -> Unit,
    onBrowseCategory: (String) -> Unit,
    onSurprise: () -> Unit,
) {
    val featured = library.videos.firstOrNull() ?: return
    val configuration = LocalConfiguration.current
    val contentPadding = contentPaddingFor(configuration.screenWidthDp)
    val cardWidth = railCardWidthFor(configuration.screenWidthDp)
    val heroHeight = heroHeightFor(configuration.screenHeightDp)
    val listState = rememberLazyListState()
    val heroFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // The hero is the first meaningful action on launch. Waiting two frames
        // lets the LazyColumn and the image placeholder attach first.
        withFrameNanos { }
        withFrameNanos { }
        runCatching { heroFocusRequester.requestFocus() }
    }

    val fresh = library.videos.drop(1).take(18).ifEmpty { library.videos.take(18) }
    val deepDives = library.videos
        .filter { it.difficulty.equals("Deep Dive", ignoreCase = true) }
        .take(18)
    val quickWatches = library.videos
        .filter { video ->
            val minutes = video.duration.substringBefore(" min").trim().toIntOrNull()
            minutes != null && minutes <= 12
        }
        .take(18)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        item(key = "hero") {
            Hero(
                video = featured,
                height = heroHeight,
                contentPadding = contentPadding,
                playFocusRequester = heroFocusRequester,
                onPlay = { onPlayVideo(featured) },
                onDetails = { onOpenVideo(featured) },
                onSurprise = onSurprise,
            )
        }

        item(key = "fresh") {
            VideoRailSection(
                title = "Fresh from the curation desk",
                subtitle = "The newest additions, newest first.",
                videos = fresh,
                onOpenVideo = onOpenVideo,
                accent = AccentAmber,
                cardWidth = cardWidth,
                contentPadding = contentPadding,
            )
        }

        if (deepDives.isNotEmpty()) {
            item(key = "deep-dives") {
                VideoRailSection(
                    title = "Deep dives",
                    subtitle = "Longer watches for when you want to go all the way down.",
                    videos = deepDives,
                    onOpenVideo = onOpenVideo,
                    accent = com.stemstream.tv.ui.theme.AccentWarm,
                    cardWidth = cardWidth,
                    contentPadding = contentPadding,
                )
            }
        }

        if (quickWatches.isNotEmpty()) {
            item(key = "quick-watches") {
                VideoRailSection(
                    title = "A quick dose of wonder",
                    subtitle = "Twelve minutes or less.",
                    videos = quickWatches,
                    onOpenVideo = onOpenVideo,
                    accent = androidx.compose.ui.graphics.Color(0xFF34D399),
                    cardWidth = cardWidth,
                    contentPadding = contentPadding,
                )
            }
        }

        for (category in com.stemstream.tv.data.CATEGORY_ORDER) {
            val videos = library.byCategory[category].orEmpty().take(18)
            if (videos.isEmpty()) continue

            item(key = "category-$category") {
                VideoRailSection(
                    title = category,
                    subtitle = "${library.byCategory[category].orEmpty().size} curated videos.",
                    videos = videos,
                    onOpenVideo = onOpenVideo,
                    accent = categoryColor(category),
                    cardWidth = cardWidth,
                    contentPadding = contentPadding,
                    onSeeAll = { onBrowseCategory(category) },
                )
            }
        }

        item(key = "footer") {
            HomeFooter(
                count = library.count,
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun HomeFooter(
    count: Int,
    contentPadding: androidx.compose.ui.unit.Dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = contentPadding, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StemMark(modifier = Modifier.size(20.dp))
        Text(
            text = "$count hand-picked videos",
            style = MaterialTheme.typography.labelMedium,
            color = Muted,
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "NO ADS",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Faint,
            )
            Text(text = "·", color = Line)
            Text(
                text = "NO TRACKING",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Faint,
            )
            Text(text = "·", color = Line)
            Text(
                text = "OFFICIAL YOUTUBE PLAYBACK",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Faint,
            )
        }
    }
}
