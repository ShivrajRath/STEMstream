package com.stemstream.tv.ui.screens

import android.text.format.DateFormat
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.content.Context
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.stemstream.tv.BuildConfig
import com.stemstream.tv.ui.components.StemMark
import com.stemstream.tv.ui.contentPaddingFor
import com.stemstream.tv.ui.theme.Accent
import com.stemstream.tv.ui.theme.Faint
import com.stemstream.tv.ui.theme.Ink
import com.stemstream.tv.ui.theme.Line
import com.stemstream.tv.ui.theme.Muted
import com.stemstream.tv.ui.theme.Surface
import java.util.Date

@Composable
fun AboutScreen(
    videoCount: Int = 0,
    updatedAtMillis: Long = 0L,
    refreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val contentPadding = contentPaddingFor(configuration.screenWidthDp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StemMark(modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "STEMStream TV",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.14f))
                            .border(1.dp, Accent.copy(alpha = 0.3f), CircleShape)
                            .padding(horizontal = 11.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = "CURATED · NO ADS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Accent,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Feed your curiosity. A hand-picked science, space, engineering and mathematics library for the biggest screen in the house.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Faint,
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatCard(videoCount.toString(), "hand-picked videos", Modifier.weight(1f))
            StatCard("7", "disciplines", Modifier.weight(1f))
            StatCard("0", "ads, trackers or accounts", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InfoCard(
                title = "Playback",
                body = "Play opens the official YouTube app in the best quality the device supports. STEMStream does not host, extract or alter any video stream. If YouTube shows an ad, YouTube Premium is the ad-free route.",
                modifier = Modifier.weight(1f),
            )
            InfoCard(
                title = "The library",
                body = "The same hand-curated catalogue as the website. A snapshot ships inside the APK so browsing works offline, and the app checks the published catalogue every few minutes, so new daily picks appear without reinstalling anything. Artwork still needs a connection.",
                modifier = Modifier.weight(1f),
            )
            InfoCard(
                title = "Privacy",
                body = "No account, no analytics, no advertising SDK. The app talks to i.ytimg.com for thumbnails and hands playback to YouTube. Nothing about your viewing is collected by STEMStream.",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StemMark(modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = Faint,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = when {
                    refreshing -> "Checking the published catalogue..."
                    updatedAtMillis > 0L -> "Catalogue updated ${catalogueDate(context, updatedAtMillis)}"
                    else -> "Using the bundled catalogue"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Faint,
            )
            if (onRefresh != null) {
                Spacer(modifier = Modifier.width(14.dp))
                Button(
                    onClick = { if (!refreshing) onRefresh() },
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        contentColor = Color.White.copy(alpha = 0.82f),
                        focusedContainerColor = Color.White,
                        focusedContentColor = Ink,
                        pressedContainerColor = Color.White.copy(alpha = 0.9f),
                        pressedContentColor = Ink,
                    ),
                    scale = ButtonDefaults.scale(focusedScale = 1.04f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = "Check for updates",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Videos belong to their creators. Playback and ads are handled by YouTube.",
                style = MaterialTheme.typography.labelSmall,
                color = Faint,
            )
        }
    }
}

private fun catalogueDate(context: Context, millis: Long): String =
    DateFormat.getMediumDateFormat(context).format(Date(millis))

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = Accent,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
            color = Muted,
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = 150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.92f),
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = Muted,
        )
    }
}
