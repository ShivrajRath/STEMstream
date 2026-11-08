package com.stemstream.tv.playback

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.stemstream.tv.data.Video

/**
 * Plays a catalogue entry through the official YouTube app. There is no stream
 * extraction, no background download and no ad interception: the uploader's
 * video is played by YouTube itself.
 */
object YouTubePlayer {

    fun open(context: Context, video: Video): Boolean {
        val candidates = listOf(
            "vnd.youtube:${video.youtubeId}",
            "https://www.youtube.com/watch?v=${video.youtubeId}",
            "https://youtu.be/${video.youtubeId}",
        )

        for (url in candidates) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
                // Try the next form: app deep link -> web URL -> short URL.
            } catch (_: SecurityException) {
                // A restrictive Fire OS profile can reject an implicit intent.
            }
        }

        Toast.makeText(
            context,
            "No YouTube app or browser is available on this device.",
            Toast.LENGTH_LONG,
        ).show()
        return false
    }
}
