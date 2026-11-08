package com.stemstream.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.stemstream.tv.ui.Tab
import com.stemstream.tv.ui.theme.Ink

@Composable
fun NavBar(
    currentTab: Tab,
    onSelectTab: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        BrandLockup(markSize = 28.dp)
        Spacer(modifier = Modifier.width(34.dp))

        for (tab in Tab.entries) {
            NavPill(
                tab = tab,
                selected = tab == currentTab,
                onClick = { onSelectTab(tab) },
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Spacer(modifier = Modifier.weight(1f))
        TvClock()
    }
}

@Composable
private fun NavPill(
    tab: Tab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.13f) else Color.Transparent,
            contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
            focusedContainerColor = Color.White,
            focusedContentColor = Ink,
            pressedContainerColor = Color.White.copy(alpha = 0.9f),
            pressedContentColor = Ink,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.03f),
        shape = ButtonDefaults.shape(shape = CircleShape),
        border = ButtonDefaults.border(
            border = if (selected) {
                Border(
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    shape = CircleShape,
                )
            } else {
                Border.None
            },
            focusedBorder = Border.None,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
