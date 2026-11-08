package com.stemstream.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.stemstream.tv.ui.theme.Faint
import com.stemstream.tv.ui.theme.Ink

@Composable
fun SectionHeader(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    count: Int? = null,
    onSeeAll: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.padding(horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(accent, CircleShape),
        )
        Spacer(modifier = Modifier.width(11.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.94f),
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Faint,
                )
            }
        }

        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = Faint,
                modifier = Modifier.padding(end = 14.dp),
            )
        }

        if (onSeeAll != null) {
            Button(
                onClick = onSeeAll,
                colors = ButtonDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.06f),
                    contentColor = Color.White.copy(alpha = 0.66f),
                    focusedContainerColor = Color.White,
                    focusedContentColor = Ink,
                    pressedContainerColor = Color.White.copy(alpha = 0.9f),
                    pressedContentColor = Ink,
                ),
                scale = ButtonDefaults.scale(focusedScale = 1.04f),
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = AppIcons.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
