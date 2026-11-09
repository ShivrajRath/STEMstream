package com.stemstream.tv.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

/** Browse/Browse-style view keys that are not disciplines. */
const val SAVED_VIEW_KEY = "My List"
const val WATCHED_VIEW_KEY = "Watched"

/**
 * The viewer's own state: what has been watched and what has been saved.
 *
 * Everything stays on the device in SharedPreferences, so it survives app
 * updates (including `adb install -r`) and reboots, and never leaves the box.
 * Both lists are stored newest first, which is the order they are shown in.
 *
 * The properties are Compose state, so a change made in one screen (say, play
 * from the hero) immediately updates the rails and grids elsewhere.
 */
class PersonalLibrary(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var watchedIds by mutableStateOf(read(KEY_WATCHED))
        private set

    var savedIds by mutableStateOf(read(KEY_SAVED))
        private set

    fun isWatched(id: String): Boolean = id in watchedIds

    fun isSaved(id: String): Boolean = id in savedIds

    /** Playing a video files it under Watched for good. */
    fun markWatched(id: String) {
        if (isWatched(id)) return
        watchedIds = listOf(id) + watchedIds
        write(KEY_WATCHED, watchedIds)
    }

    /** Puts a watched video back into the main collection. */
    fun toggleWatched(id: String) {
        watchedIds = if (isWatched(id)) {
            watchedIds - id
        } else {
            listOf(id) + watchedIds
        }
        write(KEY_WATCHED, watchedIds)
    }

    fun toggleSaved(id: String) {
        savedIds = if (isSaved(id)) {
            savedIds - id
        } else {
            listOf(id) + savedIds
        }
        write(KEY_SAVED, savedIds)
    }

    fun clearWatched() {
        watchedIds = emptyList()
        write(KEY_WATCHED, watchedIds)
    }

    private fun read(key: String): List<String> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val id = array.optString(index).trim()
                    if (id.isNotEmpty()) add(id)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun write(key: String, ids: List<String>) {
        val array = JSONArray()
        for (id in ids) array.put(id)
        prefs.edit().putString(key, array.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "personal_library"
        const val KEY_WATCHED = "watched_ids"
        const val KEY_SAVED = "saved_ids"
    }
}
