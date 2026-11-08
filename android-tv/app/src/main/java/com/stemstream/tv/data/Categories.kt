package com.stemstream.tv.data

import androidx.compose.ui.graphics.Color

/** The same editorial order the website uses. */
val CATEGORY_ORDER = listOf(
    "Physics",
    "Space",
    "Emerging Technology",
    "Engineering",
    "Biology",
    "Chemistry",
    "Math",
)

private val CATEGORY_COLORS = mapOf(
    "Physics" to Color(0xFF38BDF8),
    "Space" to Color(0xFFA78BFA),
    "Emerging Technology" to Color(0xFF22D3EE),
    "Engineering" to Color(0xFFFB923C),
    "Biology" to Color(0xFFF472B6),
    "Chemistry" to Color(0xFFF87171),
    "Math" to Color(0xFFFACC15),
)

fun categoryColor(category: String): Color =
    CATEGORY_COLORS[category] ?: Color(0xFF94A3B8)

fun categoryShortName(category: String): String = when (category) {
    "Emerging Technology" -> "Emerging Tech"
    else -> category
}
