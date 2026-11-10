# STEMStream

A curated, single-page video library for the best science, space, emerging
technology, engineering, biology, chemistry, and mathematics content on YouTube.

The catalogue lives **inside this repository** in
[`src/data/data.json`](src/data/data.json). There is no Google Sheet, no API key,
and no runtime backend. Vite inlines the JSON into a fully static build that can
be hosted on GitHub Pages.

## What it does

- **874 hand-picked videos** across seven disciplines, drawn heavily from
  twenty of YouTube's strongest science, engineering and mathematics channels.
- **Emerging Technology**: AI and machine learning, quantum computing, nanotech
  and advanced materials, biotechnology, robotics, clean energy, space
  technology, and the chipmaking that makes it all possible.
- **Content-first cards**: the idea, the question it answers, and the takeaway
  come before the channel name.
- **Watched and My List**: a played video moves out of the main collection into
  its own Watched section, and the bookmark on any card (or in the player) saves
  a video to My List. Both are kept in the browser, and the filter row shows how
  many videos are in each.
- **Search** across titles, concepts, descriptions, channels, and takeaways.
- **Sticky, scrollable category filters** with live counts.
- **Cinematic inline playback** via `youtube-nocookie.com`. Clicking the backdrop
  or pressing <kbd>Esc</kbd> unmounts the iframe and stops playback.
- **Responsive grid**: 1 column on phones, 2 on tablets, 3–4 on desktop.
- **YouTube-style dark theme**, accessible focus states, keyboard support, and
  reduced-motion-friendly transitions.
- **Responsive thumbnails**: cards use `srcset` and `sizes` so phones, laptops
  and TVs download a 320–480 px still instead of a 1280×720 one, with
  `hqdefault.jpg` and a styled placeholder as fallbacks.
- **Light on slow hardware**: no full-page backdrop blur, decorative animations
  pause while a video is playing, and `prefers-reduced-motion` disables motion.

## Stack

| Layer | Choice |
| --- | --- |
| Framework | React 19 + Vite |
| Styling | Tailwind CSS 3 |
| Icons | Lucide React |
| Data | Local `src/data/data.json` |
| Automation | Python 3 standard library |
| CI/CD | GitHub Actions → GitHub Pages |

## Run locally

```bash
npm install
npm run dev
```

Production build and local preview:

```bash
npm run build
npm run preview
```

Validate the catalogue without changing it:

```bash
python3 scripts/update_content.py --check
```

Check every video and thumbnail online as well:

```bash
python3 scripts/update_content.py --check --check-links
```

## Project structure

```text
.
├── .github/workflows/
│   ├── deploy.yml                 # Push + weekly schedule → build → Pages
│   └── daily-video.yml            # Daily Gemini pick → commit → build → Pages
├── public/
│   ├── .nojekyll
│   └── favicon.svg
├── scripts/
│   ├── add_daily_video.py         # daily Gemini picks, verified against YouTube
│   └── update_content.py          # schema validation + normalisation + audit
├── src/
│   ├── components/
│   │   ├── CategoryFilter.jsx
│   │   ├── Header.jsx
│   │   ├── Logo.jsx
│   │   ├── ModalPlayer.jsx
│   │   ├── VideoCard.jsx
│   │   └── VideoGrid.jsx
│   ├── data/data.json             # the catalogue (source of truth)
│   ├── lib/categories.js          # labels, icons, accent colours
│   ├── lib/personal.js            # watched history + My List, in localStorage
│   ├── App.jsx
│   ├── index.css
│   └── main.jsx
├── index.html
├── tailwind.config.js
└── vite.config.js
```

## Catalogue schema

`src/data/data.json` is an array of objects:

```json
{
  "id": "stem-001",
  "youtubeId": "AF8d72mA41M",
  "title": "Why It Was Almost Impossible to Make the Blue LED",
  "coreQuestion": "Why did red and green LEDs exist for decades while blue remained out of reach?",
  "channel": "Veritasium",
  "category": "Engineering",
  "difficulty": "Intermediate",
  "duration": "34 min",
  "concepts": ["Semiconductors", "Gallium Nitride", "Band Gap"],
  "description": "A short editorial summary of the video.",
  "takeaway": "The one idea a viewer should leave with."
}
```

| Field | Required | Notes |
| --- | --- | --- |
| `id` | generated | Rewritten as `stem-001`, `stem-002`, … by the script. |
| `youtubeId` | yes | Exactly 11 characters. Duplicates are removed. |
| `title` | yes | Shown on the card and in the player. |
| `coreQuestion` | recommended | The curiosity hook shown under the title. |
| `channel` | yes | Displayed as a subtle credit, not the focus. |
| `category` | yes | One of `Physics`, `Space`, `Emerging Technology`, `Engineering`, `Biology`, `Chemistry`, `Math`. |
| `difficulty` | recommended | `Foundational`, `Intermediate`, or `Deep Dive`. |
| `duration` | recommended | Human-readable, e.g. `"22 min"`. |
| `concepts` | recommended | Array of strings; shown in the player. |
| `description` | yes | Two or three sentences for the player. |
| `takeaway` | recommended | Highlighted callout in the player. |

### Add or replace a video

1. Open `src/data/data.json` and add an entry. Keep the editorial order you want
   on the page.
2. Run `python3 scripts/update_content.py` to validate the schema and normalise
   the file.
3. Run `npm run dev` and check the card, search result, and player.
4. Commit and push to `main`. The workflow validates, builds, and deploys.

The script will not invent content or scrape channels. Curation is manual and
version-controlled so the collection stays intentional.

## Automation script

`scripts/update_content.py` uses only the Python standard library:

- validates required fields, YouTube IDs, categories, and difficulties;
- removes duplicate videos;
- rewrites deterministic IDs and formatting;
- optionally verifies public availability and thumbnail quality over the network.

Modes:

```bash
python3 scripts/update_content.py                 # validate + rewrite data.json
python3 scripts/update_content.py --check         # read-only (used in CI)
python3 scripts/update_content.py --check-links   # also audit videos online
python3 scripts/update_content.py --check-links --strict-links
```

`--check-links` reports problems but exits successfully by default so a deleted
video or a transient YouTube response cannot block a deployment. Add
`--strict-links` if you want the audit to fail the job.

## Daily automated picks

`.github/workflows/daily-video.yml` runs every day at 05:17 UTC. It asks Gemini
for one new video, verifies the recommendation against YouTube, prepends the
accepted entry to `src/data/data.json`, commits the change to `main`, and then
builds and deploys the site in the same run.

One section is curated per day: the section is picked by rotating through the
seven of them on the date, so each section gains a video every week while a run
costs one or two API requests instead of seven. Start the workflow by hand with
a section name to curate that section out of turn.

A prepended entry is the first record in the file, so it becomes the first card
both in the full collection and in its own category.

Nothing is written until a suggestion passes every check:

- the id must resolve through YouTube's public oEmbed endpoint;
- the upload must allow embedding, because the site plays videos in an iframe;
- the id must not already be in the catalogue.

The title and channel come from YouTube itself rather than from the model, so a
card always names the real upload. When a suggestion fails a check the script
asks Gemini again, up to three times; if every suggestion from that model fails,
the next model in the chain gets the same three attempts, carrying the rejection
reasons with it. Only when the whole chain comes up empty does the day write
nothing and fail the job, so a broken key or an exhausted quota is visible
instead of silently skipping. A YouTube error that says nothing about the upload
(`429`, `5xx`, a dropped connection) is retried before the id is discarded.

Configure the key in **Settings → Secrets and variables → Actions**:

| Name | Type | Required | Purpose |
| --- | --- | --- | --- |
| `GEMINI_API_KEY` | secret | yes | API key from [Google AI Studio](https://aistudio.google.com/apikey). |

The model is hardcoded in the script, so no repository variable is needed. The
chain is `gemini-3.8-flash` → `gemini-3.6-flash` → `gemini-3.5-flash-lite` →
`gemini-3.1-flash-lite`. A model that no longer exists, is overloaded (`503`) or
is out of free-tier quota (`429`), or whose suggestions all fail verification,
hands the pick to the next model, so a busy model does not cost the day's video.
To try a different model locally, set `GEMINI_MODEL` in your own shell; an empty
value is ignored.

Run it by hand from the **Actions** tab (**Add daily video → Run workflow**), or
locally:

```bash
GEMINI_API_KEY=... python3 scripts/add_daily_video.py --dry-run           # verify only
GEMINI_API_KEY=... python3 scripts/add_daily_video.py                     # write the file
GEMINI_API_KEY=... python3 scripts/add_daily_video.py --category Space    # today's pick, other section
```

## GitHub Pages deployment

The included workflow handles both manual pushes and a scheduled weekend run.

1. Push the repository to GitHub.
2. Open **Settings → Pages**.
3. Under **Build and deployment → Source**, choose **GitHub Actions**.
4. Push to `main`, or run the workflow manually from the **Actions** tab.

Deployments happen when:

- anything is pushed to `main`;
- the workflow is started manually (`workflow_dispatch`);
- the schedule fires every **Saturday at 06:00 UTC** (`0 6 * * 6`), which
  revalidates the catalogue, rebuilds the site, and republishes it.

`vite.config.js` uses `base: './'`, so the build works at a project URL such as
`https://<username>.github.io/<repo>/` and at a custom domain without hardcoding
the repository name.

### Permissions recap

The workflow requests:

- `contents: read` — check out the repository;
- `pages: write` — publish the artifact;
- `id-token: write` — let GitHub authenticate the deployment.

No personal access token or repository secret is required.

## Android TV and Fire TV app

A native Kotlin/Compose app for televisions lives in [`android-tv/`](android-tv/).
It reads the same `src/data/data.json` catalogue, ships a snapshot inside the APK
for offline browsing, and refreshes from `catalog.json` — which this Vite build
publishes next to the site — so a daily curation commit reaches televisions
without a new APK.

See [`android-tv/README.md`](android-tv/README.md) for building a signed APK and
installing it on Fire TV and Android TV.

## Privacy

Playback uses the privacy-enhanced `www.youtube-nocookie.com` embed domain.
YouTube may still set cookies or show creator-controlled ads inside the iframe.
All videos remain the property of their respective creators; STEMStream only
links to and embeds public videos.
