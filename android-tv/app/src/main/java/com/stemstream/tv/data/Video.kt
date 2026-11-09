package com.stemstream.tv.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * A single catalogue record. `searchText` is built once at load time so typing
 * in Search never has to concatenate strings for 750 videos on every keystroke.
 */
data class Video(
    val id: String,
    val youtubeId: String,
    val title: String,
    val coreQuestion: String,
    val channel: String,
    val category: String,
    val difficulty: String,
    val duration: String,
    val concepts: List<String>,
    val description: String,
    val takeaway: String,
    val searchText: String,
)

data class Library(
    val videos: List<Video>,
    val categories: List<String>,
    val byCategory: Map<String, List<Video>>,
    val byId: Map<String, Video>,
    val latest: List<Video>,
) {
    val count: Int get() = videos.size
}

/**
 * Videos for [ids] in the order given, skipping ids a later curation commit has
 * removed. Used by Watched and My List, which store ids newest first.
 */
fun Library.select(ids: List<String>): List<Video> = ids.mapNotNull { byId[it] }

/**
 * Turns catalogue JSON into a [Library]. Shared by the copy bundled in the APK
 * and the copy downloaded from the published site, so both are parsed by the
 * same rules.
 *
 * @throws org.json.JSONException when the payload is not a JSON array.
 * @throws IllegalStateException when no usable record is present, which stops a
 *   truncated download from replacing a good catalogue.
 */
fun parseLibrary(json: String): Library {
    val array = JSONArray(json)
    val videos = ArrayList<Video>(array.length())

    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        val youtubeId = item.optString("youtubeId").trim()

        // Keep the player safe: anything that is not an 11-character YouTube id
        // never reaches an Intent.
        if (youtubeId.length != 11) continue

        val title = item.optString("title").trim()
        if (title.isEmpty()) continue

        val conceptsJson = item.optJSONArray("concepts")
        val concepts = buildList {
            if (conceptsJson != null) {
                for (conceptIndex in 0 until conceptsJson.length()) {
                    val concept = conceptsJson.optString(conceptIndex).trim()
                    if (concept.isNotEmpty()) add(concept)
                }
            }
        }

        val coreQuestion = item.optString("coreQuestion").trim()
        val channel = item.optString("channel").trim()
        val category = item.optString("category").trim()
        val difficulty = item.optString("difficulty").trim()
        val duration = item.optString("duration").trim()
        val description = item.optString("description").trim()
        val takeaway = item.optString("takeaway").trim()

        val searchText = buildString(320) {
            append(title).append(' ')
            append(coreQuestion).append(' ')
            append(channel).append(' ')
            append(category).append(' ')
            append(difficulty).append(' ')
            append(description).append(' ')
            append(takeaway)
            for (concept in concepts) {
                append(' ').append(concept)
            }
        }.lowercase()

        videos.add(
            Video(
                id = item.optString("id").ifBlank { youtubeId },
                youtubeId = youtubeId,
                title = title,
                coreQuestion = coreQuestion,
                channel = channel,
                category = category,
                difficulty = difficulty,
                duration = duration,
                concepts = concepts,
                description = description,
                takeaway = takeaway,
                searchText = searchText,
            ),
        )
    }

    check(videos.isNotEmpty()) { "The catalogue contained no usable videos." }

    val categories = videos
        .map { it.category }
        .distinct()
        .filter { it.isNotEmpty() }
        .sortedWith(
            compareBy { category ->
                val position = CATEGORY_ORDER.indexOf(category)
                if (position < 0) Int.MAX_VALUE else position
            },
        )

    return Library(
        videos = videos,
        categories = categories,
        byCategory = videos.groupBy { it.category },
        byId = videos.associateBy { it.id },
        latest = videos.take(24),
    )
}

/** The catalogue snapshot baked into the APK by the `syncCatalog` Gradle task. */
suspend fun loadBundled(context: Context): Library = withContext(Dispatchers.IO) {
    val json = context.assets.open("catalog.json")
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }
    parseLibrary(json)
}

/**
 * All query tokens must appear somewhere in the haystack. Matching "black
 * hole" therefore finds records that contain both words in any field.
 */
fun searchVideos(videos: List<Video>, query: String): List<Video> {
    val tokens = query.trim().lowercase().split(' ').filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return emptyList()

    return videos.filter { video ->
        tokens.all { token -> video.searchText.contains(token) }
    }
}