package com.stemstream.tv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.stemstream.tv.ui.components.AppIcons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.stemstream.tv.data.Library
import com.stemstream.tv.data.Video
import com.stemstream.tv.data.categoryColor
import com.stemstream.tv.data.searchVideos
import com.stemstream.tv.ui.components.CategoryChip
import com.stemstream.tv.ui.components.VideoCard
import com.stemstream.tv.ui.contentPaddingFor
import com.stemstream.tv.ui.gridColumnsFor
import com.stemstream.tv.ui.theme.Accent
import com.stemstream.tv.ui.theme.Faint
import com.stemstream.tv.ui.theme.Ink
import com.stemstream.tv.ui.theme.Line
import com.stemstream.tv.ui.theme.Muted
import com.stemstream.tv.ui.theme.Surface
import com.stemstream.tv.ui.theme.SurfaceRaised

private val POPULAR_SEARCHES = listOf(
    "black holes",
    "quantum",
    "CRISPR",
    "fusion",
    "prime numbers",
    "semiconductors",
    "consciousness",
    "climate",
)

@Composable
fun SearchScreen(
    library: Library,
    onOpenVideo: (Video) -> Unit,
    onBrowseCategory: (String) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val contentPadding = contentPaddingFor(configuration.screenWidthDp)
    val columns = gridColumnsFor(configuration.screenWidthDp)
    val keyboard = LocalSoftwareKeyboardController.current
    val fieldFocusRequester = remember { FocusRequester() }

    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, library) {
        if (query.isBlank()) emptyList() else searchVideos(library.videos, query)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
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
                        text = "Search",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Look for an idea, not just a title.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Faint,
                    )
                }
                Text(
                    text = "${library.count} videos",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search ${library.count} videos, ideas, channels",
                    focusRequester = fieldFocusRequester,
                    modifier = Modifier.weight(1f),
                )

                if (query.isNotEmpty()) {
                    Button(
                        onClick = {
                            query = ""
                            fieldFocusRequester.requestFocus()
                            keyboard?.show()
                        },
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.07f),
                            contentColor = Color.White.copy(alpha = 0.7f),
                            focusedContainerColor = Color.White,
                            focusedContentColor = Ink,
                        ),
                        scale = ButtonDefaults.scale(focusedScale = 1.03f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = "Press OK on the field to open the on-screen keyboard. Back closes it.",
                style = MaterialTheme.typography.labelSmall,
                color = Faint,
            )
        }

        if (query.isBlank()) {
            SearchLanding(
                contentPadding = contentPadding,
                modifier = Modifier.weight(1f),
                onSuggestion = { suggestion ->
                    query = suggestion
                    keyboard?.hide()
                },
                onBrowseCategory = onBrowseCategory,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${results.size} ${if (results.size == 1) "result" else "results"} for \"$query\"",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (results.isEmpty()) {
                EmptyResults(
                    query = query,
                    onClear = {
                        query = ""
                        fieldFocusRequester.requestFocus()
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .focusGroup(),
                    contentPadding = PaddingValues(
                        start = contentPadding,
                        end = contentPadding,
                        top = 4.dp,
                        bottom = 48.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    items(
                        items = results,
                        key = { video -> video.id },
                    ) { video ->
                        VideoCard(
                            video = video,
                            onClick = { onOpenVideo(video) },
                            showCategory = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(58.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                focused = state.isFocused
                if (state.isFocused) {
                    keyboard?.show()
                }
            },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.White),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { keyboard?.hide() },
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (focused) SurfaceRaised else Surface, CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (focused) Color.White.copy(alpha = 0.82f) else Line,
                        shape = CircleShape,
                    )
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = AppIcons.Search,
                    contentDescription = null,
                    modifier = Modifier.size(23.dp),
                    tint = if (focused) Color.White else Muted,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.titleMedium,
                            color = Muted,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun SearchLanding(
    contentPadding: androidx.compose.ui.unit.Dp,
    onSuggestion: (String) -> Unit,
    onBrowseCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .focusGroup(),
        contentPadding = PaddingValues(
            start = contentPadding,
            end = contentPadding,
            top = 10.dp,
            bottom = 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "popular-title") {
            Text(
                text = "Popular searches",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.9f),
            )
        }

        item(key = "popular") {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(POPULAR_SEARCHES, key = { it }) { suggestion ->
                    SuggestionChip(
                        label = suggestion,
                        color = Accent,
                        onClick = { onSuggestion(suggestion) },
                    )
                }
            }
        }

        item(key = "categories-title") {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Browse by discipline",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.9f),
            )
        }

        item(key = "categories") {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = com.stemstream.tv.data.CATEGORY_ORDER,
                    key = { it },
                ) { category ->
                    CategoryChip(
                        label = category,
                        color = categoryColor(category),
                        selected = false,
                        onClick = { onBrowseCategory(category) },
                    )
                }
            }
        }

        item(key = "hint") {
            Text(
                text = "Search looks through titles, questions, descriptions, channels, takeaways and concepts. Try two words to narrow things down.",
                style = MaterialTheme.typography.bodySmall,
                color = Faint,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SuggestionChip(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = color.copy(alpha = 0.09f),
            contentColor = color.copy(alpha = 0.9f),
            focusedContainerColor = color,
            focusedContentColor = Ink,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.04f),
        shape = ButtonDefaults.shape(shape = CircleShape),
        border = ButtonDefaults.border(
            border = BorderStroke(1.dp, color.copy(alpha = 0.24f)).let {
                androidx.tv.material3.Border(border = it, shape = CircleShape)
            },
            focusedBorder = androidx.tv.material3.Border.None,
        ),
        contentPadding = PaddingValues(horizontal = 17.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun EmptyResults(
    query: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Nothing matched \"$query\".",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.9f),
            )
            Text(
                text = "Try a broader word, a channel name, or a concept.",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
            Button(
                onClick = onClear,
                colors = ButtonDefaults.colors(
                    containerColor = Color.White,
                    contentColor = Ink,
                    focusedContainerColor = Color.White,
                    focusedContentColor = Ink,
                ),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp),
            ) {
                Text("Clear search", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
