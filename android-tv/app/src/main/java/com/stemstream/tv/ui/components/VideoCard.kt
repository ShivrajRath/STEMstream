package com.stemstream.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
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
fun VideoCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCategory: Boolean = true,
) {
    val accent = categoryColor(video.category)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val imageShape = RoundedCornerShape(12.dp)

    Card(
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        shape = CardDefaults.shape(shape = shape),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White,
        ),
        scale = CardDefaults.scale(focusedScale = 1.045f),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, accent.copy(alpha = 0.92f)),
                shape = shape,
            ),
            pressedBorder = Border(
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.9f)),
                shape = shape,
            ),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(imageShape)
                .background(SurfaceRaised),
        ) {
            VideoThumbnail(
                video = video,
                modifier = Modifier.fillMaxSize(),
                quality = ThumbnailQuality.CARD,
            )

            Text(
                text = video.duration,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color(0xC9000000), RoundedCornerShape(5.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.92f),
            )

            if (focused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.16f)),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(50.dp)
                        .background(Color.White.copy(alpha = 0.94f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Play,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = Ink,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = video.title,
            style = MaterialTheme.typography.titleSmall,
            color = if (focused) Color.White else Color.White.copy(alpha = 0.9f),
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        )

        Spacer(modifier = Modifier.height(5.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(accent, CircleShape),
            )
            Spacer(modifier = Modifier.width(7.dp))

            if (showCategory) {
                Text(
                    text = categoryShortName(video.category),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent.copy(alpha = 0.9f),
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelMedium,
                    color = Faint,
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = video.channel,
                style = MaterialTheme.typography.labelMedium,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
