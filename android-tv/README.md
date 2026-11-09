# STEMStream for Android TV and Fire TV

A native, remote-first Kotlin/Compose app for the same curated STEM catalogue the
website uses. No analytics, no advertising SDK, no account to create. Playback is
the one place a WebView appears: it hosts YouTube's official embedded player so
watching stays inside the app.

The Android project lives in this folder and reads the catalogue from
`../src/data/data.json` — the repository copy stays the single source of truth.

## What the app does

- **Home** — hero pick, "fresh from the curation desk", deep dives, short watches,
  and one rail per discipline.
- **Browse** — full grid with category filters.
- **Search** — remote-friendly text field plus concept and category shortcuts.
- **Video detail** — description, concepts, takeaway, and a play action.
- **Playback** — plays inside the app. YouTube's official embedded player fills
  the screen. The bar and title strip leave the screen four seconds into
  playback, leaving the picture unobstructed; any remote press brings them back,
  a paused video keeps them up, and Back returns to the library. A hairline of
  progress stays along the very bottom edge while the bar is away, so position
  is still visible without a control over the picture. On the remote,
  centre is play/pause and left/right seek ±10 seconds whether the bar is up or
  not, so seeking never needs a trip through the buttons. Nothing is scraped,
  extracted or proxied, so the uploader keeps the view and the monetisation a
  browser would give them.
- D-pad navigation, focus-based scaling, back-button handling, offline browsing,
  and a 48 MB memory / 160 MB disk image cache sized for 1 GB Fire TV sticks.

## How content stays current

The catalogue is in two places by design:

| Copy | Where it comes from | When it changes |
| --- | --- | --- |
| Bundled snapshot | `catalog.json` asset inside the APK, written by the `syncCatalog` Gradle task from `src/data/data.json` | Every time you rebuild the app |
| Downloaded copy | `https://<site>/catalog.json`, published by `vite build` next to the website | Every curation commit, no new APK |

On launch the app shows the newest copy it already has, then checks the published
catalogue in the background — shortly after start and every five minutes while
the app is open. The check is a conditional GET, so an unchanged file answers
`304` and costs a few hundred bytes. A `200` is parsed and validated before it
replaces anything, so a truncated download or a captive portal can never empty
the library. It is written to app storage atomically and is used offline next
time.

**Practical effect:** the daily Gemini curation commit lands on the website, and
televisions show the new video within about five minutes without reinstalling
anything. Pressing **Check for updates** on the About screen forces an immediate
check. The About screen shows which copy is in use and when it was downloaded.

## Requirements

- JDK 17 or newer (JDK 21 is what this project builds with)
- Android SDK with platform 36 and build-tools 36
- `local.properties` pointing at that SDK:

```properties
sdk.dir=/path/to/android-sdk
```

- For installing: `adb` from the same SDK, on the same network as the TV

## Build

```bash
cd android-tv

# Unsigned-by-you debug build, installable on any device
./gradlew :app:assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk

# Release build, minified and shrunk
./gradlew :app:assembleRelease
# -> app/build/outputs/apk/release/app-release.apk

# Google Play upload format
./gradlew :app:bundleRelease
# -> app/build/outputs/bundle/release/app-release.aab
```

### Signing

`app/build.gradle.kts` reads `keystore.properties` from this folder and only
configures a release signing config when that file exists. A key was generated
for this project at `stemstream-release.jks`; **back it up** and keep it out of
version control (already in `.gitignore`). Losing it means you cannot ship an
update to anyone who installed the app — Android rejects an APK signed with a
different key than the installed one.

`keystore.properties` holds the alias and password:

```properties
storeFile=stemstream-release.jks
storePassword=…
keyAlias=stemstream
keyPassword=…
```

To replace it with your own key:

```bash
keytool -genkeypair -keystore stemstream-release.jks -alias stemstream \
  -keyalg RSA -keysize 4096 -validity 10950
```

Bump `versionCode` in `app/build.gradle.kts` for every release you publish.
`versionName` is what the About screen shows.

## Install

The release APK is `app/build/outputs/apk/release/app-release.apk`. It is a
universal APK, so the same file installs on 32-bit and 64-bit Fire TV sticks,
boxes, and Google TV devices.

### Fire TV / Fire TV Stick

1. On the TV: **Settings → My Fire TV → About →** click **Fire TV Stick** seven
   times to reveal **Developer Options**.
2. **Settings → My Fire TV → Developer Options →** enable **ADB Debugging**.
   Note the device IP under **Settings → My Fire TV → About → Network**.
3. From your computer:

```bash
adb connect 192.168.1.50:5555
adb install -r app/build/outputs/apk/release/app-release.apk
```

4. STEMStream appears in **Apps → Your Apps Library** and on the Fire TV home
   screen row. `adb disconnect` when done.

Without a computer, use the **Downloader** app on the Fire TV (enable
"Install unknown apps" for it): host `app-release.apk` anywhere the TV can reach
— a GitHub Release asset is the easiest — and enter its URL, or the short code
Downloader generates for that URL.

### Android TV / Google TV

```bash
# Enable Developer options: Settings → System → About → tap Build 7 times
# Then Settings → System → Developer options → USB debugging / Network debugging
adb connect 192.168.1.60:5555
adb install -r app/build/outputs/apk/release/app-release.apk
```

The app appears in the TV launcher's app row. It also installs on a phone or
tablet, where it works but is not optimised for touch.

### Updating a sideloaded install

Keep the same signing key and use the same `applicationId`, then:

```bash
adb install -r app-release.apk
```

`-r` replaces the app and keeps its downloaded catalogue. A different key
requires uninstalling first, which is why the keystore above matters.

## Publish

### Google Play (TV)

1. Create the app in Play Console and upload
   `app/build/outputs/bundle/release/app-release.aab` to **Internal testing**
   first.
2. Under **Advanced settings → Form factors**, add **Android TV**. Play reads the
   manifest's `LEANBACK_LAUNCHER` intent filter and the `leanback` uses-feature,
   both of which are already declared.
3. TV store listing requirements:
   - 320×180 banner — ready at `res/drawable-nodpi/tv_banner.png`
   - 512×512 icon — `artwork/play-store-icon-512.png`
   - 1024×500 feature graphic — `artwork/play-feature-graphic-1024x500.png`
   - at least four 1920×1080 TV screenshots — **still to capture** on a real TV
     or Android TV emulator
4. Complete the content rating and data-safety forms. Declare: no data collected,
   no ads. Playback uses YouTube's official embedded player, so YouTube's own
   terms apply to viewing.
5. Roll out to production after internal testing passes on a TV device.

### Amazon Appstore (Fire TV)

1. Register at the Amazon Developer Console and create a **Fire TV** app.
2. Upload the same `app-release.apk`.
3. Amazon requires its own screenshots (1920×1080 recommended), the 512×512 icon,
   and a 1024×500 promotional image. The banner in `res/` is for the device
   launcher, not the store.
4. Amazon reviews the binary and the listing before it appears in the Fire TV
   store. Sideloading in the meantime is fully supported.

### Without a store

Host the signed APK on a GitHub Release and share the link, or keep using `adb`.
Content updates still arrive over the air, because they do not depend on the app
being rebuilt.

## Configuration

The catalogue endpoint is a build config field, defaulting to
`https://novicelab.org/STEMstream/catalog.json` — the canonical site origin (the
`github.io` address only redirects there, and a redirect is a wasted round trip
on a TV stick). For a fork, a custom domain, or local testing, override it at
build time:

```bash
./gradlew :app:assembleRelease \
  -PstemstreamCatalogUrl=https://example.com/stemstream/catalog.json
```

To test against the dev server on a physical TV, use the computer's LAN address
(GitHub Pages and the site must be HTTPS; a LAN HTTP URL also needs
`android:usesCleartextTraffic="true"` temporarily).

The endpoint must serve the catalogue as a JSON array, exactly like
`src/data/data.json`. The site's Vite build produces it automatically.

**The endpoint only exists once the updated site has been deployed.** Until the
Pages workflow publishes a build containing `catalog.json`, that URL returns
`404` and the app quietly keeps using the snapshot bundled in the APK — nothing
breaks, but nothing new arrives either. Push the site change (or run
**Deploy STEMStream** from the Actions tab) and the app starts picking up
curation commits from then on.

## Project layout

```text
android-tv/
├── app/src/main/java/com/stemstream/tv/
│   ├── MainActivity.kt              # edge-to-edge, immersive host
│   ├── StemStreamApplication.kt     # one bounded Coil image loader
│   ├── data/
│   │   ├── CatalogRepository.kt      # bundled + downloaded catalogue, ETag cache
│   │   ├── Video.kt                  # model, JSON parser, search
│   │   └── Categories.kt             # order and accent colours
│   ├── playback/YouTubePlayer.kt     # fallback handoff if embedding is blocked
│   ├── ui/player/VideoPlayerScreen.kt # in-app WebView player + remote controls
│   └── ui/                           # Compose TV screens and components
├── app/build.gradle.kts             # SDK levels, signing, catalog asset, R8
├── artwork/                          # store listing images
└── tools/make_artwork.py             # regenerates the listing images
```

## Troubleshooting

| Symptom | Cause and fix |
| --- | --- |
| "This video would not start in the app" | The embed was blocked or the creator disabled embedding. Use **Open in YouTube** on that screen. |
| Picture is black but the audio plays | History: the player iframe collapsed to zero height because Fire OS leaves the WebView unmeasured while the page loads. The page now gives the iframe explicit pixel dimensions and re-checks it every two seconds. `adb logcat -s StemPlayer` prints `frame=…x… vp=…` and the chosen `quality` if it ever recurs. |
| New videos do not appear | The TV is offline, the Pages deployment has not finished, or the endpoint is wrong. Check About: it reports the last download time. Confirm `/catalog.json` loads in a browser. |
| Build fails with "SDK location not found" | `local.properties` is missing or `sdk.dir` is wrong. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | The install was signed with a different key. Uninstall the old app or use the original keystore. |
| `adb: device offline` | Re-run `adb connect <ip>:5555`; make sure ADB debugging is still enabled. |

## Known gaps

- Playback runs YouTube's embedded player, so in-video ads are YouTube's and
  cannot be removed by this app. The app itself has no ads, trackers or account.
  YouTube Premium removes playback ads.
- A creator who disables embedding cannot be played in-app; that entry shows an
  "Open in YouTube" fallback instead.
- Store screenshots for Play and Amazon still need to be captured on a real
  device.
- Verified on a Fire TV Stick 4K (Fire OS 6 / Android 7.1): catalogue sync,
  navigation and playback all run. The player reports `frame=960x540 vp=960x540`,
  `quality=hd1080`, with about 87% of the picture area changing between frames
  four seconds apart. Not yet exercised on Google TV hardware.
- The player iframe is sized in pixels and re-fitted by a two-second watchdog,
  because this WebView never resolves percentage heights reliably.
