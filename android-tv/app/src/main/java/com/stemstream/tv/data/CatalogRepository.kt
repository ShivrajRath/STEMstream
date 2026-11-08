package com.stemstream.tv.data

import android.content.Context
import com.stemstream.tv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * One catalogue, two copies.
 *
 * The APK carries a snapshot so the very first frame after install is complete
 * and browsing works offline. The published site serves the same JSON next to
 * the web app, so when the daily curation workflow commits a new pick the TV
 * downloads it and shows it without an app update.
 *
 * Every network call is optional: a missing connection, a captive portal or a
 * 404 leaves the last good copy on screen.
 */
class CatalogRepository(context: Context) {

    private val appContext = context.applicationContext
    private val cacheFile = File(appContext.filesDir, CACHE_FILE_NAME)
    private val metadata = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Mutex()

    /** When the catalogue currently on screen was downloaded, or 0 if it is the bundled copy. */
    val updatedAtMillis: Long get() = metadata.getLong(KEY_UPDATED_AT, 0L)

    /**
     * The catalogue to show immediately: the last downloaded copy when there is
     * one, otherwise the bundled snapshot. Never touches the network.
     */
    suspend fun current(): Library = withContext(Dispatchers.IO) {
        val cached = cacheFile.takeIf { it.isFile }?.readText(Charsets.UTF_8)
        if (cached != null) {
            runCatching { parseLibrary(cached) }.getOrNull()?.let { return@withContext it }
        }
        loadBundled(appContext)
    }

    /**
     * Downloads the published catalogue.
     *
     * @param force ignore the minimum interval, used by the manual refresh button.
     * @return the new [Library], or null when nothing changed, the check ran too
     *   recently, or the device is offline.
     */
    suspend fun refresh(force: Boolean = false): Library? = lock.withLock {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (!force && now - metadata.getLong(KEY_CHECKED_AT, 0L) < MIN_CHECK_INTERVAL_MS) {
                return@withContext null
            }

            val connection = try {
                openConnection()
            } catch (_: Exception) {
                return@withContext null
            }

            try {
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_NOT_MODIFIED -> {
                        metadata.edit().putLong(KEY_CHECKED_AT, now).apply()
                        null
                    }

                    HttpURLConnection.HTTP_OK -> {
                        val body = connection.readBody()
                        // Throws before anything is written, so a truncated or
                        // malformed response can never replace a good catalogue.
                        val library = parseLibrary(body)
                        writeCache(body)
                        rememberValidation(connection, now)
                        library
                    }

                    else -> null
                }
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun openConnection(): HttpURLConnection {
        return (URL(BuildConfig.CATALOG_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Encoding", "gzip")
            setRequestProperty("User-Agent", "STEMStreamTV/${BuildConfig.VERSION_NAME}")

            // Conditional GET: GitHub Pages answers 304 for an unchanged file,
            // so a poll costs a handful of bytes instead of 400 KB.
            if (cacheFile.isFile) {
                metadata.getString(KEY_ETAG, null)?.let { setRequestProperty("If-None-Match", it) }
                metadata.getString(KEY_LAST_MODIFIED, null)
                    ?.let { setRequestProperty("If-Modified-Since", it) }
            }
        }
    }

    private fun HttpURLConnection.readBody(): String {
        val encoding = getHeaderField("Content-Encoding")
        val raw: InputStream = inputStream
        val stream: InputStream =
            if (encoding != null && encoding.contains("gzip", ignoreCase = true)) {
                GZIPInputStream(raw)
            } else {
                raw
            }
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    /** Writes to a sibling file and renames, so a crash mid-write cannot truncate the cache. */
    private fun writeCache(body: String) {
        val temp = File(appContext.filesDir, "$CACHE_FILE_NAME.tmp")
        temp.writeText(body, Charsets.UTF_8)
        if (temp.renameTo(cacheFile)) return
        cacheFile.delete()
        if (!temp.renameTo(cacheFile)) temp.delete()
    }

    private fun rememberValidation(connection: HttpURLConnection, now: Long) {
        // Note: the trailing apply() is SharedPreferences.Editor.apply(), the
        // commit call — not Kotlin's apply {} scope function, which would return
        // the editor without writing anything.
        val editor = metadata.edit()
            .putLong(KEY_CHECKED_AT, now)
            .putLong(KEY_UPDATED_AT, now)
        connection.getHeaderField("ETag")?.let { editor.putString(KEY_ETAG, it) }
        connection.getHeaderField("Last-Modified")?.let { editor.putString(KEY_LAST_MODIFIED, it) }
        editor.apply()
    }

    private companion object {
        const val CACHE_FILE_NAME = "catalog_cache.json"
        const val PREFS_NAME = "catalog_metadata"
        const val KEY_CHECKED_AT = "checked_at"
        const val KEY_UPDATED_AT = "updated_at"
        const val KEY_ETAG = "etag"
        const val KEY_LAST_MODIFIED = "last_modified"

        const val CONNECT_TIMEOUT_MS = 8_000
        const val READ_TIMEOUT_MS = 12_000

        /** A curation commit lands at most once a day; five minutes is already generous. */
        const val MIN_CHECK_INTERVAL_MS = 5 * 60 * 1000L
    }
}