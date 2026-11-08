package com.stemstream.tv.ui.screens

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.stemstream.tv.data.Library
import com.stemstream.tv.data.Video
import com.stemstream.tv.data.categoryColor
import com.stemstream.tv.data.categoryShortName
import com.stemstream.tv.ui.components.CategoryChip
import com.stemstream.tv.ui.components.VideoCard
import com.stemstream.tv.ui.contentPaddingFor
import com.stemstream.tv.ui.gridColumnsFor
import com.stemstream.tv.ui.theme.Faint
import com.stemstream.tv.ui.theme.Muted

@Composable
fun BrowseScreen(
    library: Library,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    onOpenVideo: (Video) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val contentPadding = contentPaddingFor(configuration.screenWidthDp)
    val columns = gridColumnsFor(configuration.screenWidthDp)
    val gridState = rememberLazyGridState()
    val firstCardRequester = remember { FocusRequester() }

    val filtered = if (selectedCategory == null) {
        library.videos
    } else {
        library.byCategory[selectedCategory].orEmpty()
    }

    LaunchedEffect(selectedCategory) {
        gridState.scrollToItem(0)
        withFrameNanos { }
        runCatching { firstCardRequester.requestFocus() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = contentPadding, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Browse",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (selectedCategory == null) {
                            "Every video, filed by discipline."
                        } else {
                            "${categoryShortName(selectedCategory)} · ${filtered.size} videos"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Faint,
                    )
                }

                Text(
                    text = "${filtered.size} videos",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "all") {
                    CategoryChip(
                        label = "All",
                        color = Color.White,
                        selected = selectedCategory == null,
                        onClick = { onSelectCategory(null) },
                    )
                }
                items(library.categories, key = { it }) { category ->
                    CategoryChip(
                        label = category,
                        color = categoryColor(category),
                        selected = selectedCategory == category,
                        onClick = { onSelectCategory(category) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusGroup(),
            contentPadding = PaddingValues(
                start = contentPadding,
                end = contentPadding,
                top = 8.dp,
                bottom = 48.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            itemsIndexed(
                items = filtered,
                key = { _, video -> video.id },
            ) { index, video ->
                VideoCard(
                    video = video,
                    onClick = { onOpenVideo(video) },
                    modifier = if (index == 0) {
                        Modifier.focusRequester(firstCardRequester)
                    } else {
                        Modifier
                    },
                    showCategory = selectedCategory == null,
                )
            }
        }
    }
}
