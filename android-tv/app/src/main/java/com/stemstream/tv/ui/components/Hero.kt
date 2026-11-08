package com.stemstream.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.stemstream.tv.data.Video
import com.stemstream.tv.data.categoryColor
import com.stemstream.tv.data.categoryShortName
import com.stemstream.tv.ui.theme.Faint
import com.stemstream.tv.ui.theme.Ink
import com.stemstream.tv.ui.theme.Muted
import com.stemstream.tv.ui.theme.SurfaceRaised

@Composable
fun Hero(
    video: Video,
    height: Dp,
    contentPadding: Dp,
    playFocusRequester: FocusRequester,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    onSurprise: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = categoryColor(video.category)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF2A0D14), Color(0xFF0E0B16), Ink),
                ),
            ),
    ) {
        VideoThumbnail(
            video = video,
            modifier = Modifier.fillMaxSize(),
            quality = ThumbnailQuality.HERO,
        )

        // Two gradients: one anchors the copy on the left, the other blends the
        // still into the page at the bottom.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to Ink,
                            0.32f to Ink.copy(alpha = 0.92f),
                            0.62f to Ink.copy(alpha = 0.45f),
                            1f to Color.Transparent,
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
                            0f to Color.Transparent,
                            0.58f to Ink.copy(alpha = 0.25f),
                            1f to Ink,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.64f)
                .padding(start = contentPadding, end = 16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeroTag(
                    text = "Latest pick",
                    color = Color.White.copy(alpha = 0.82f),
                    dotColor = accent,
                )
                HeroTag(
                    text = categoryShortName(video.category),
                    color = accent,
                    dotColor = accent,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = video.title,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (video.coreQuestion.isNotBlank()) {
                Spacer(modifier = Modifier.height(9.dp))
                Text(
                    text = video.coreQuestion,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.66f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(13.dp))

            Text(
                text = buildString {
                    append(video.channel)
                    if (video.difficulty.isNotBlank()) append("  ·  ").append(video.difficulty)
                    if (video.duration.isNotBlank()) append("  ·  ").append(video.duration)
                },
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
                    onClick = onPlay,
                    modifier = Modifier.focusRequester(playFocusRequester),
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White,
                        contentColor = Ink,
                        focusedContainerColor = Color.White,
                        focusedContentColor = Ink,
                        pressedContainerColor = Color.White.copy(alpha = 0.9f),
                        pressedContentColor = Ink,
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

                Button(
                    onClick = onDetails,
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                        contentColor = Color.White,
                        focusedContainerColor = Color.White,
                        focusedContentColor = Ink,
                        pressedContainerColor = Color.White.copy(alpha = 0.9f),
                        pressedContentColor = Ink,
                    ),
                    scale = ButtonDefaults.scale(focusedScale = 1.05f),
                    border = ButtonDefaults.border(
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)).let {
                            androidx.tv.material3.Border(border = it, shape = CircleShape)
                        },
                        focusedBorder = androidx.tv.material3.Border.None,
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Button(
                    onClick = onSurprise,
                    colors = ButtonDefaults.colors(
                        containerColor = SurfaceRaised.copy(alpha = 0.54f),
                        contentColor = Color.White.copy(alpha = 0.72f),
                        focusedContainerColor = Color.White,
                        focusedContentColor = Ink,
                        pressedContainerColor = Color.White.copy(alpha = 0.9f),
                        pressedContentColor = Ink,
                    ),
                    scale = ButtonDefaults.scale(focusedScale = 1.05f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Icon(
                        imageVector = AppIcons.Surprise,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "Surprise me",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroTag(
    text: String,
    color: Color,
    dotColor: Color,
) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), CircleShape)
            .border(1.dp, color.copy(alpha = 0.22f), CircleShape)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(dotColor, CircleShape),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}
