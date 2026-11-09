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

// Emerging Technology is emerald, not the cyan it used to be: cyan sat in the
// same blue family as Physics, so the two chips read as the same colour from a
// sofa. Every discipline now has a hue of its own.
private val CATEGORY_COLORS = mapOf(
    "Physics" to Color(0xFF38BDF8),
    "Space" to Color(0xFFA78BFA),
    "Emerging Technology" to Color(0xFF34D399),
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
