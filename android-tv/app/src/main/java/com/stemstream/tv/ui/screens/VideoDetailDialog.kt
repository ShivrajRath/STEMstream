package com.stemstream.tv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.stemstream.tv.data.Video
import com.stemstream.tv.data.categoryColor
import com.stemstream.tv.data.categoryShortName
import com.stemstream.tv.ui.components.AppIcons
import com.stemstream.tv.ui.components.VideoThumbnail
import com.stemstream.tv.ui.components.ThumbnailQuality
import com.stemstream.tv.ui.contentPaddingFor
import com.stemstream.tv.ui.theme.Accent
import com.stemstream.tv.ui.theme.Faint
import com.stemstream.tv.ui.theme.Ink
import com.stemstream.tv.ui.theme.Line
import com.stemstream.tv.ui.theme.Muted
import com.stemstream.tv.ui.theme.SurfaceRaised

@Composable
fun VideoDetailDialog(
    video: Video,
    saved: Boolean,
    watched: Boolean,
    onToggleSaved: () -> Unit,
    onToggleWatched: () -> Unit,
    onDismiss: () -> Unit,
    onPlay: (Video) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val contentPadding = contentPaddingFor(configuration.screenWidthDp)
    val accent = categoryColor(video.category)
    val playFocusRequester = remember { FocusRequester() }

    LaunchedEffect(video.id) {
        withFrameNanos { }
        withFrameNanos { }
        runCatching { playFocusRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ink),
        ) {
            VideoThumbnail(
                video = video,
                modifier = Modifier.fillMaxSize(),
                quality = ThumbnailQuality.HERO,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0f to Ink,
                                0.42f to Ink.copy(alpha = 0.94f),
                                0.78f to Ink.copy(alpha = 0.55f),
                                1f to Ink.copy(alpha = 0.3f),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Ink.copy(alpha = 0.1f),
                                0.55f to Ink.copy(alpha = 0.55f),
                                1f to Ink,
                            ),
                        ),
                    ),
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = contentPadding, vertical = 30.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(36.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1.12f),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DetailTag(
                            text = categoryShortName(video.category),
                            color = accent,
                            dotColor = accent,
                        )
                        DetailTag(
                            text = video.difficulty.ifBlank { "Video" },
                            color = Color.White.copy(alpha = 0.78f),
                            dotColor = Color.White.copy(alpha = 0.55f),
                        )
                        DetailTag(
                            text = video.duration.ifBlank { "—" },
                            color = Color.White.copy(alpha = 0.62f),
                            dotColor = Color.Transparent,
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (video.coreQuestion.isNotBlank()) {
                        Spacer(modifier = Modifier.height(9.dp))
                        Text(
                            text = video.coreQuestion,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.68f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = video.channel,
                        style = MaterialTheme.typography.labelLarge,
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { onPlay(video) },
                            modifier = Modifier.focusRequester(playFocusRequester),
                            colors = ButtonDefaults.colors(
                                containerColor = Color.White,
                                contentColor = Ink,
                                focusedContainerColor = Color.White,
                                focusedContentColor = Ink,
                            ),
                            scale = ButtonDefaults.scale(focusedScale = 1.05f),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        ) {
                            Icon(
                                imageVector = AppIcons.Play,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Play",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        DetailAction(
                            active = saved,
                            onClick = onToggleSaved,
                            labelActive = "In my list",
                            labelIdle = "My list",
                            activeIcon = AppIcons.Bookmark,
                            idleIcon = AppIcons.BookmarkBorder,
                        )

                        DetailAction(
                            active = watched,
                            onClick = onToggleWatched,
                            labelActive = "Watched",
                            labelIdle = "Mark watched",
                            activeIcon = AppIcons.CheckCircle,
                            idleIcon = AppIcons.CheckCircle,
                        )

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                contentColor = Color.White.copy(alpha = 0.8f),
                                focusedContainerColor = Color.White,
                                focusedContentColor = Ink,
                            ),
                            scale = ButtonDefaults.scale(focusedScale = 1.05f),
                            border = ButtonDefaults.border(
                                border = androidx.tv.material3.Border(
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                    shape = CircleShape,
                                ),
                                focusedBorder = androidx.tv.material3.Border.None,
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Icon(
                                imageVector = AppIcons.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "Close",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(9.dp))
                    Text(
                        text = "Plays inside STEMStream with YouTube's official player. Back returns to the library.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Faint,
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(0.72f)
                        .background(Line),
                )

                Column(
                    modifier = Modifier.weight(0.88f),
                ) {
                    Text(
                        text = "ABOUT THIS VIDEO",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = video.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.74f),
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (video.concepts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "CONCEPTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Faint,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = video.concepts.joinToString("  ·  "),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.66f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (video.takeaway.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .background(SurfaceRaised.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
                                .border(1.dp, Line, RoundedCornerShape(10.dp)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(accent),
                            )
                            Text(
                                text = video.takeaway,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                ),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A compact remote-friendly toggle: My list and Watched use the same shape. */
@Composable
private fun DetailAction(
    active: Boolean,
    onClick: () -> Unit,
    labelActive: String,
    labelIdle: String,
    activeIcon: ImageVector,
    idleIcon: ImageVector,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (active) Accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
            contentColor = if (active) Accent else Color.White.copy(alpha = 0.8f),
            focusedContainerColor = Color.White,
            focusedContentColor = Ink,
            pressedContainerColor = Color.White.copy(alpha = 0.9f),
            pressedContentColor = Ink,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.05f),
        border = ButtonDefaults.border(
            border = androidx.tv.material3.Border(
                border = BorderStroke(
                    1.dp,
                    (if (active) Accent else Color.White).copy(alpha = 0.2f),
                ),
                shape = CircleShape,
            ),
            focusedBorder = androidx.tv.material3.Border.None,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = if (active) activeIcon else idleIcon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = if (active) labelActive else labelIdle,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun DetailTag(
    text: String,
    color: Color,
    dotColor: Color,
) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), CircleShape)
            .border(1.dp, color.copy(alpha = 0.22f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dotColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(dotColor, CircleShape),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
