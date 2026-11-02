# STEMStream

A curated, single-page video library for the best science, space, engineering,
biology, computer science, and mathematics content on YouTube.

The catalogue lives **inside this repository** in
[`src/data/data.json`](src/data/data.json). There is no Google Sheet, no API key,
and no runtime backend. Vite inlines the JSON into a fully static build that can
be hosted on GitHub Pages.

## What it does

- **24 hand-picked videos**, four in each of six disciplines.
- **Content-first cards**: the idea, the question it answers, and the takeaway
  come before the channel name.
- **Search** across titles, concepts, descriptions, channels, and takeaways.
- **Sticky, scrollable category filters** with live counts.
- **Cinematic inline playback** via `youtube-nocookie.com`. Clicking the backdrop
  or pressing <kbd>Esc</kbd> unmounts the iframe and stops playback.
- **Responsive grid**: 1 column on phones, 2 on tablets, 3–4 on desktop.
- **YouTube-style dark theme**, accessible focus states, keyboard support, and
  reduced-motion-friendly transitions.
- **Thumbnail resilience**: `maxresdefault.jpg` first, then `hqdefault.jpg`, then
  a styled placeholder.

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
├── .github/workflows/deploy.yml   # Push + weekly schedule → build → Pages
├── public/
│   ├── .nojekyll
│   └── favicon.svg
├── scripts/
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
| `category` | yes | One of `Physics`, `Space`, `Computer Science`, `Engineering`, `Biology`, `Math`. |
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

## Privacy

Playback uses the privacy-enhanced `www.youtube-nocookie.com` embed domain.
YouTube may still set cookies or show creator-controlled ads inside the iframe.
All videos remain the property of their respective creators; STEMStream only
links to and embeds public videos.
