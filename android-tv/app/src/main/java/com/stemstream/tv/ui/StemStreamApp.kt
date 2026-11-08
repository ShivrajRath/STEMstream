package com.stemstream.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.stemstream.tv.data.CatalogRepository
import com.stemstream.tv.data.Library
import com.stemstream.tv.data.Video
import com.stemstream.tv.ui.components.NavBar
import com.stemstream.tv.ui.components.StemMark
import com.stemstream.tv.ui.screens.AboutScreen
import com.stemstream.tv.ui.screens.BrowseScreen
import com.stemstream.tv.ui.screens.HomeScreen
import com.stemstream.tv.ui.screens.SearchScreen
import com.stemstream.tv.ui.player.VideoPlayerScreen
import com.stemstream.tv.ui.screens.VideoDetailDialog
import com.stemstream.tv.ui.theme.Faint
import com.stemstream.tv.ui.theme.Ink
import com.stemstream.tv.ui.theme.Muted
import kotlinx.coroutines.delay

private sealed interface LibraryState {
    data object Loading : LibraryState
    data class Ready(val library: Library) : LibraryState
    data class Failed(val message: String) : LibraryState
}

/** How often an open app re-checks the published catalogue. */
private const val FRESHNESS_POLL_MS = 5 * 60 * 1000L

@Composable
fun StemStreamApp() {
    val context = LocalContext.current
    val repository = remember(context) { CatalogRepository(context) }
    var libraryState by remember { mutableStateOf<LibraryState>(LibraryState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    var updatedAtMillis by remember { mutableLongStateOf(0L) }
    var currentTab by rememberSaveable { mutableStateOf(Tab.Home) }
    var browseCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVideo by remember { mutableStateOf<Video?>(null) }
    var playerVideo by remember { mutableStateOf<Video?>(null) }
    val stateHolder = rememberSaveableStateHolder()

    LaunchedEffect(reloadKey) {
        libraryState = LibraryState.Loading
        val loaded = runCatching { repository.current() }
        libraryState = loaded.fold(
            onSuccess = { LibraryState.Ready(it) },
            onFailure = { LibraryState.Failed(it.message ?: "The catalogue could not be read.") },
        )
        updatedAtMillis = repository.updatedAtMillis
    }

    // Freshness without a new APK: the first check runs shortly after launch,
    // then every five minutes while the app is open. An unchanged catalogue
    // answers 304, so a poll is far cheaper than a thumbnail.
    LaunchedEffect(repository) {
        delay(2_000L)
        while (true) {
            runCatching { repository.refresh() }.getOrNull()?.let { library ->
                libraryState = LibraryState.Ready(library)
                updatedAtMillis = repository.updatedAtMillis
            }
            delay(FRESHNESS_POLL_MS)
        }
    }

    LaunchedEffect(refreshTick) {
        if (refreshTick == 0) return@LaunchedEffect
        refreshing = true
        val result = runCatching { repository.refresh(force = true) }
        refreshing = false
        result.getOrNull()?.let { library ->
            libraryState = LibraryState.Ready(library)
            updatedAtMillis = repository.updatedAtMillis
        }
    }

    BackHandler(enabled = currentTab != Tab.Home) {
        currentTab = Tab.Home
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        when (val state = libraryState) {
            LibraryState.Loading -> LoadingScreen()

            is LibraryState.Failed -> ErrorScreen(
                message = state.message,
                onRetry = { reloadKey += 1 },
            )

            is LibraryState.Ready -> {
                val library = state.library

                Column(modifier = Modifier.fillMaxSize()) {
                    NavBar(
                        currentTab = currentTab,
                        onSelectTab = { tab -> currentTab = tab },
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        stateHolder.SaveableStateProvider(key = currentTab.name) {
                            when (currentTab) {
                                Tab.Home -> HomeScreen(
                                    library = library,
                                    onOpenVideo = { video -> selectedVideo = video },
                                    onPlayVideo = { video -> playerVideo = video },
                                    onBrowseCategory = { category ->
                                        browseCategory = category
                                        currentTab = Tab.Browse
                                    },
                                    onSurprise = {
                                        library.videos.randomOrNull()?.let { video ->
                                            selectedVideo = video
                                        }
                                    },
                                )

                                Tab.Browse -> BrowseScreen(
                                    library = library,
                                    selectedCategory = browseCategory,
                                    onSelectCategory = { category ->
                                        browseCategory = category
                                    },
                                    onOpenVideo = { video -> selectedVideo = video },
                                )

                                Tab.Search -> SearchScreen(
                                    library = library,
                                    onOpenVideo = { video -> selectedVideo = video },
                                    onBrowseCategory = { category ->
                                        browseCategory = category
                                        currentTab = Tab.Browse
                                    },
                                )

                                Tab.About -> AboutScreen(
                                    videoCount = library.count,
                                    updatedAtMillis = updatedAtMillis,
                                    refreshing = refreshing,
                                    onRefresh = { refreshTick += 1 },
                                )
                            }
                        }
                    }
                }
            }
        }

        selectedVideo?.let { video ->
            VideoDetailDialog(
                video = video,
                onDismiss = { selectedVideo = null },
                onPlay = { playingVideo ->
                    selectedVideo = null
                    playerVideo = playingVideo
                },
            )
        }

        // Full-screen playback stays inside the app; it is the last composable
        // in the tree so its BackHandler claims the back key first.
        playerVideo?.let { playing ->
            VideoPlayerScreen(
                video = playing,
                onClose = { playerVideo = null },
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StemMark(modifier = Modifier.size(66.dp))
            Text(
                text = "STEMStream",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Text(
                text = "Loading the library...",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
) {
    val retryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { retryFocusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(40.dp),
        ) {
            Text(
                text = "The library could not load.",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Faint,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.focusRequester(retryFocusRequester),
                colors = ButtonDefaults.colors(
                    containerColor = Color.White,
                    contentColor = Ink,
                    focusedContainerColor = Color.White,
                    focusedContentColor = Ink,
                ),
            ) {
                Text("Try again", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}