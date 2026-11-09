package com.stemstream.tv.ui.player

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.stemstream.tv.data.Video
import com.stemstream.tv.playback.YouTubePlayer
import com.stemstream.tv.ui.components.AppIcons
import com.stemstream.tv.ui.theme.Faint
import com.stemstream.tv.ui.theme.Ink
import com.stemstream.tv.ui.theme.Muted
import kotlinx.coroutines.delay

/**
 * Playback that never leaves STEMStream.
 *
 * The video is played by YouTube's own embedded player, so the uploader keeps
 * the view, the analytics and the monetisation they would get in a browser.
 * Nothing is extracted, proxied or ad-stripped: that would break YouTube's terms
 * and, in practice, break playback within weeks.
 *
 * Two details make this work inside a WebView:
 *
 * 1. YouTube requires the embed to identify the site that is embedding it and
 *    answers `error 153 / Video player configuration error` when the Referer is
 *    missing, which is exactly what a WebView sends when it loads
 *    `youtube.com/embed/...` directly. Loading the page with
 *    [WebView.loadDataWithBaseURL] and a real origin gives the iframe a Referer.
 * 2. A WebView's HTML controls are not remote-friendly, so the WebView is made
 *    non-focusable and the D-pad drives the Compose bar instead, calling the
 *    IFrame API for play/pause and seeking.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerScreen(
    video: Video,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    // Playing *or* buffering. A stick topping up its buffer reports long
    // stretches of buffering, and counting those as "stopped" kept the bar on
    // screen forever.
    var active by remember { mutableStateOf(true) }
    // True once the player reports a real duration, so the bar is never hidden
    // before a video has actually begun.
    var started by remember(video.id) { mutableStateOf(false) }
    var progressLabel by remember { mutableStateOf("Connecting…") }
    var positionSeconds by remember { mutableStateOf(0) }
    var durationSeconds by remember { mutableStateOf(0) }
    // The bar is for operating the video, not for watching it: it leaves the
    // screen a few seconds into playback and comes back on any remote press.
    var controlsVisible by remember(video.id) { mutableStateOf(true) }
    var interaction by remember(video.id) { mutableStateOf(0) }
    // Set when a press has only brought the bar back: the matching key-up must
    // be swallowed too, or it lands on the button it left focused and clicks it.
    var swallowKeyUp by remember { mutableStateOf(false) }
    val playFocusRequester = remember { FocusRequester() }

    BackHandler { onClose() }

    // A paused video must not let the TV's screen saver take over mid-watch.
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // The bar keeps focus while it is invisible, which is what lets a remote
    // press reach the key handler below instead of falling into an empty focus
    // owner and being dropped.
    LaunchedEffect(controlsVisible, video.id) {
        if (!controlsVisible) return@LaunchedEffect
        repeat(6) {
            withFrameNanos { }
            if (runCatching { playFocusRequester.requestFocus() }.isSuccess) {
                return@LaunchedEffect
            }
            delay(120L)
        }
    }

    // Keep the bar on screen while a video is paused or starting; it is the only
    // way to resume, so it must not disappear on its own.
    LaunchedEffect(controlsVisible, active, started, interaction) {
        if (controlsVisible && active && started) {
            delay(CONTROLS_LINGER_MS)
            controlsVisible = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.let { player ->
                player.stopLoading()
                player.loadUrl("about:blank")
                (player.parent as? ViewGroup)?.removeView(player)
                player.destroy()
            }
            webView = null
        }
    }

    fun runScript(script: String, onResult: (String) -> Unit = {}) {
        webView?.evaluateJavascript(script) { raw ->
            onResult(raw.orEmpty().trim('"'))
        }
    }

    fun togglePlayback() {
        runScript("toggle()") { state ->
            if (state == "playing" || state == "buffering" || state == "paused") {
                active = state == "playing" || state == "buffering"
                if (!active) controlsVisible = true
            }
        }
    }

    // The embedded player hides its own controls on a TV, so the bar reports
    // position and duration instead. Every second is cheap: one tiny JS call.
    LaunchedEffect(webView) {
        if (webView == null) return@LaunchedEffect
        while (true) {
            runScript(JS_PROGRESS) { raw ->
                val parts = raw.split('|')
                if (parts.size == 3) {
                    val state = parts[0]
                    val position = parts[1].toIntOrNull() ?: 0
                    val duration = parts[2].toIntOrNull() ?: 0
                    progressLabel = if (duration > 0) {
                        "${clock(position)} / ${clock(duration)}"
                    } else {
                        clock(position)
                    }
                    if (duration > 0) started = true
                    positionSeconds = position
                    durationSeconds = duration
                    active = state == "playing" || state == "buffering"
                    // Only a stopped video brings the bar back; a buffering one
                    // is still being watched.
                    if (state == "paused" || state == "state0" || state == "state5") {
                        controlsVisible = true
                    }
                }
            }
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Keys are caught before they reach a button: while the bar is
            // tucked away the first press only brings it back, so a control
            // nobody can see can never be operated by accident.
            .onPreviewKeyEvent { event ->
                // The remote is driven from here rather than through Compose
                // focus: a focused-button path stopped delivering these presses
                // once the bar had been hidden, and this way centre is always
                // play/pause and left/right always seek while a video is up.
                when {
                    // The release belonging to a press we already handled must
                    // not also reach a button and fire a second time.
                    swallowKeyUp -> {
                        if (event.type == KeyEventType.KeyUp) swallowKeyUp = false
                        true
                    }
                    event.key == Key.Back -> false
                    event.type == KeyEventType.KeyUp -> false
                    !controlsVisible -> {
                        swallowKeyUp = true
                        interaction++
                        controlsVisible = true
                        true
                    }
                    event.key == Key.DirectionCenter || event.key == Key.Enter -> {
                        swallowKeyUp = true
                        interaction++
                        togglePlayback()
                        true
                    }
                    event.key == Key.DirectionLeft -> {
                        swallowKeyUp = true
                        interaction++
                        runScript("seekBy(-10)")
                        true
                    }
                    event.key == Key.DirectionRight -> {
                        swallowKeyUp = true
                        interaction++
                        runScript("seekBy(10)")
                        true
                    }
                    else -> false
                }
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)

                    // The D-pad must never disappear into the page's own focus
                    // order; all remote input is handled by the bar below.
                    isFocusable = false
                    isFocusableInTouchMode = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        // Lets the embed start on its own, which is what a TV
                        // expects after pressing Play.
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        allowFileAccess = false
                        allowContentAccess = false
                        javaScriptCanOpenWindowsAutomatically = false
                        setSupportMultipleWindows(false)
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(
                            message: android.webkit.ConsoleMessage,
                        ): Boolean {
                            Log.i(PLAYER_LOG, "${message.message()} (${message.lineNumber()})")
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        /**
                         * The player lives in a frame, so frames may navigate
                         * freely; only the top document is pinned. That stops
                         * "Watch on YouTube" from escaping into a browser-like
                         * page with no remote support.
                         */
                        override fun shouldOverrideUrlLoading(
                            target: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            if (!request.isForMainFrame) return false
                            val host = request.url.host.orEmpty()
                            return !(host.endsWith("youtube.com") ||
                                host.endsWith("youtube-nocookie.com"))
                        }

                        override fun onPageFinished(target: WebView, url: String?) {
                            loading = false
                            // One line of evidence, so a black picture can be told
                            // apart from a player that never received any frames.
                            val probe = Runnable {
                                target.evaluateJavascript(JS_DIAG) { raw ->
                                    Log.i(PLAYER_LOG, "probe=$raw")
                                }
                            }
                            target.postDelayed(probe, 4_000L)
                            target.postDelayed(probe, 12_000L)
                        }

                        override fun onReceivedError(
                            target: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError,
                        ) {
                            if (request.isForMainFrame) {
                                failed = true
                                loading = false
                            }
                        }
                    }

                    // The base URL is what YouTube verifies as the embedding
                    // origin, and what the Referer header is derived from.
                    loadDataWithBaseURL(
                        PLAYER_ORIGIN,
                        playerHtml(video.youtubeId),
                        "text/html",
                        "utf-8",
                        null,
                    )
                    webView = this
                }
            },
            update = { player -> webView = player },
        )

        // Title strip: tells you what is playing without covering the video. It
        // fades out with the bar rather than being removed from the composition.
        Row(
            modifier = Modifier
                .alpha(if (controlsVisible) 1f else 0f)
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.72f),
                            1f to Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 40.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = video.channel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Faint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (loading && !failed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Starting the player…",
                    style = MaterialTheme.typography.titleMedium,
                    color = Muted,
                )
            }
        }

        if (failed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Ink.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(48.dp),
                ) {
                    Text(
                        text = "This video would not start in the app.",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                    )
                    Text(
                        text = "The network may be blocking it. The YouTube app can still play it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Faint,
                    )
                    Button(
                        onClick = { YouTubePlayer.open(context, video) },
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White,
                            contentColor = Ink,
                            focusedContainerColor = Color.White,
                            focusedContentColor = Ink,
                        ),
                    ) {
                        Text("Open in YouTube", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Remote control bar. This is the only focusable surface on screen, and
        // it fades away once playback is under way. It stays composed while
        // invisible so it keeps focus: a released focus owner would swallow the
        // remote press that is meant to bring the bar back.
        Row(
            modifier = Modifier
                .alpha(if (controlsVisible) 1f else 0f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.82f),
                        ),
                    ),
                )
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    interaction++
                    togglePlayback()
                },
                modifier = Modifier.focusRequester(playFocusRequester),
                colors = ButtonDefaults.colors(
                    containerColor = Color.White,
                    contentColor = Ink,
                    focusedContainerColor = Color.White,
                    focusedContentColor = Ink,
                ),
                scale = ButtonDefaults.scale(focusedScale = 1.06f),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Icon(
                    imageVector = if (active) AppIcons.Pause else AppIcons.Play,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (active) "Pause" else "Play",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            ControlButton("−10s") {
                interaction++
                runScript("seekBy(-10)")
            }
            ControlButton("+10s") {
                interaction++
                runScript("seekBy(10)")
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = progressLabel,
                style = MaterialTheme.typography.labelLarge,
                color = Muted,
            )

            ControlButton("YouTube app") {
                YouTubePlayer.open(context, video)
                onClose()
            }

            ControlButton("Close", onClick = onClose, icon = AppIcons.Close)
        }

        // A hairline of position while the bar is away: enough to see how far
        // through a video this is without putting a control over the picture.
        if (!controlsVisible && durationSeconds > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.10f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(
                            (positionSeconds.toFloat() / durationSeconds.toFloat())
                                .coerceIn(0f, 1f),
                        )
                        .background(Color.White.copy(alpha = 0.75f)),
                )
            }
        }
    }
}

@Composable
private fun ControlButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.12f),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Ink,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.06f),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

/** The site identity YouTube sees in the Referer when the embed loads. */
private const val PLAYER_ORIGIN = "https://novicelab.org/"

private const val PLAYER_LOG = "StemPlayer"

/** How long the bar stays up while a video plays before it gets out of the way. */
private const val CONTROLS_LINGER_MS = 4_000L

/** Frame size, chosen quality and buffered fraction, for diagnosing a black picture. */
private const val JS_DIAG =
    "(function(){if(!player||!ready){return 'loading';}" +
        "var r=player.getIframe().getBoundingClientRect();" +
        "return 'frame='+Math.round(r.width)+'x'+Math.round(r.height)+" +
        "' vp='+window.innerWidth+'x'+window.innerHeight+" +
        "' quality='+player.getPlaybackQuality()+" +
        "' frac='+(player.getVideoLoadedFraction()||0).toFixed(2)+" +
        "' state='+player.getPlayerState();})()"

/** Returns `state|position|duration` in whole seconds. */
private const val JS_PROGRESS =
    "(function(){if(!player||!ready){return 'loading';}" +
        "var s=player.getPlayerState();" +
        "var l=s===1?'playing':s===2?'paused':s===3?'buffering':'state'+s;" +
        "return l+'|'+Math.floor(player.getCurrentTime()||0)+'|'+Math.floor(player.getDuration()||0);})()"

private fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

private fun playerHtml(youtubeId: String): String = PLAYER_HTML.replace("__VIDEO_ID__", youtubeId)

/**
 * The IFrame Player API rather than a bare embed URL: the API creates the frame
 * with `origin` and `widget_referrer`, and gives us stable commands instead of
 * reaching into another document's DOM, which the browser sandbox forbids.
 */
private val PLAYER_HTML = """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="referrer" content="strict-origin-when-cross-origin">
  <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no">
  <style>
    html, body { margin: 0; padding: 0; height: 100%; background: #05070d; overflow: hidden; }
    #player { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
  </style>
</head>
<body>
  <div id="player"></div>
  <script src="https://www.youtube.com/iframe_api"></script>
  <script>
    var player = null;
    var ready = false;
    var ORIGIN = 'https://novicelab.org';

    // The iframe is created at the size its container has at that moment, and a
    // WebView can still be unmeasured while the page loads. That produced a
    // 960x0 frame: audio playing behind a black rectangle. Wait for a real
    // viewport, then pin the frame to it whatever the container reports.
    function onYouTubeIframeAPIReady() {
      if (window.innerHeight < 1 || window.innerWidth < 1) {
        window.setTimeout(onYouTubeIframeAPIReady, 100);
        return;
      }
      player = new YT.Player('player', {
        width: window.innerWidth,
        height: window.innerHeight,
        videoId: '__VIDEO_ID__',
        playerVars: {
          autoplay: 1,
          controls: 1,
          rel: 0,
          fs: 0,
          playsinline: 1,
          iv_load_policy: 3,
          modestbranding: 1,
          origin: ORIGIN,
          widget_referrer: ORIGIN
        },
        events: {
          'onReady': function (event) {
            ready = true;
            fit();
            event.target.playVideo();
          }
        }
      });
    }

    function fit() {
      var W = window.innerWidth;
      var H = window.innerHeight;
      if (W < 1 || H < 1) { return; }
      var container = document.getElementById('player');
      if (container) {
        container.style.width = W + 'px';
        container.style.height = H + 'px';
      }
      if (!player || !player.getIframe) { return; }
      var frame = player.getIframe();
      if (!frame) { return; }
      // Explicit pixels, not percentages: a percentage height resolves to 0 in
      // a WebView that has not been measured, which is what produced a 960x0
      // player that played audio behind a black rectangle.
      frame.setAttribute('width', W);
      frame.setAttribute('height', H);
      frame.style.position = 'absolute';
      frame.style.top = '0';
      frame.style.left = '0';
      frame.style.width = W + 'px';
      frame.style.height = H + 'px';
      frame.style.border = '0';
    }

    // The player re-applies its own size once it finishes loading, so correct a
    // frame that has drifted away from the viewport.
    window.setInterval(function () {
      if (!player || !player.getIframe) { return; }
      var frame = player.getIframe();
      if (!frame) { return; }
      if (frame.offsetWidth !== window.innerWidth ||
          frame.offsetHeight !== window.innerHeight) {
        fit();
      }
    }, 2000);

    window.addEventListener('resize', fit);

    function toggle() {
      if (!player || !ready) { return 'loading'; }
      // 1 = playing, 3 = buffering. Both are running, and a buffering player
      // must pause instead of being told to play again.
      var state = player.getPlayerState();
      if (state === 1 || state === 3) {
        player.pauseVideo();
        return 'paused';
      }
      player.playVideo();
      return 'playing';
    }

    function seekBy(seconds) {
      if (!player || !ready) { return 'loading'; }
      var position = player.getCurrentTime() || 0;
      player.seekTo(Math.max(0, position + seconds), true);
      return 'ok';
    }

    function progress() {
      if (!player || !ready) { return 'loading'; }
      var state = player.getPlayerState();
      var label = state === 1 ? 'playing'
        : state === 2 ? 'paused'
        : state === 3 ? 'buffering'
        : 'state' + state;
      return label + '|' + Math.floor(player.getCurrentTime() || 0) +
        '|' + Math.floor(player.getDuration() || 0);
    }
  </script>
</body>
</html>
""".trimIndent()
