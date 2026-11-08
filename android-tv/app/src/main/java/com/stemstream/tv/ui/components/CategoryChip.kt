package com.stemstream.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.stemstream.tv.ui.theme.Ink
import com.stemstream.tv.ui.theme.Line
import com.stemstream.tv.ui.theme.Muted

@Composable
fun CategoryChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (selected) color.copy(alpha = 0.16f) else Color.Transparent,
            contentColor = if (selected) color else Muted,
            focusedContainerColor = color,
            focusedContentColor = Ink,
            pressedContainerColor = color.copy(alpha = 0.85f),
            pressedContentColor = Ink,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.04f),
        shape = ButtonDefaults.shape(shape = CircleShape),
        border = ButtonDefaults.border(
            border = Border(
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected) color.copy(alpha = 0.5f) else Line,
                ),
                shape = CircleShape,
            ),
            focusedBorder = Border.None,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) {
        if (label != "All") {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(color, CircleShape),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
