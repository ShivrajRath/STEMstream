package com.stemstream.tv.ui.components

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stemstream.tv.data.Video
import com.stemstream.tv.ui.theme.Accent

@Composable
fun VideoRailSection(
    title: String,
    videos: List<Video>,
    onOpenVideo: (Video) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    caption: String? = null,
    accent: Color = Accent,
    cardWidth: Dp = 250.dp,
    contentPadding: Dp = 40.dp,
    onSeeAll: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = title,
            accent = accent,
            subtitle = subtitle,
            count = videos.size,
            onSeeAll = onSeeAll,
            modifier = Modifier.padding(horizontal = contentPadding),
        )

        if (caption != null) {
            Spacer(modifier = Modifier.height(2.dp))
            androidx.tv.material3.Text(
                text = caption,
                modifier = Modifier.padding(horizontal = contentPadding),
                style = androidx.tv.material3.MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.35f),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            contentPadding = PaddingValues(
                start = contentPadding,
                end = contentPadding,
                top = 8.dp,
                bottom = 8.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(
                items = videos,
                key = { video -> video.id },
            ) { video ->
                VideoCard(
                    video = video,
                    onClick = { onOpenVideo(video) },
                    modifier = Modifier.width(cardWidth),
                )
            }
        }
    }
}
