#!/usr/bin/env python3
"""
STEMStream daily picks
======================

Asks Gemini for one fresh video recommendation and prepends the accepted entry
to ``src/data/data.json``, so the new video is the first card in its section
(and in the combined collection).

One section is curated per day, rotating through ``VALID_CATEGORIES`` on the
date, so every section gets a new pick each week while a run spends only one or
two requests instead of seven.

Every recommendation is verified against YouTube's public oEmbed endpoint
before it is written: the id must resolve, the upload must allow embedding
(STEMStream plays videos in an iframe), and it must not already be in the
catalogue. The title and channel are taken from YouTube rather than from the
model, so the card always names the real upload.

Usage
-----
    GEMINI_API_KEY=... python3 scripts/add_daily_video.py
    GEMINI_API_KEY=... python3 scripts/add_daily_video.py --dry-run
    GEMINI_API_KEY=... python3 scripts/add_daily_video.py --category Space

Environment
-----------
``GEMINI_API_KEY``  Required. Key from https://aistudio.google.com/apikey
``GEMINI_MODEL``    Optional local override for the hardcoded default model.
                    Unset, empty or whitespace-only values are ignored.

Only the Python standard library is used.
"""

from __future__ import annotations

import argparse
import http.client
import json
import os
import random
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import date
from typing import Any

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
if SCRIPT_DIR not in sys.path:
    sys.path.insert(0, SCRIPT_DIR)

# Reuse the catalogue's own schema rules instead of duplicating them.
import update_content as catalogue  # noqa: E402 - sys.path is prepared above

DATA_PATH = catalogue.DATA_PATH
USER_AGENT = "STEMStream-Curation/1.0 (+https://github.com/)"
GEMINI_ENDPOINT = (
    "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
)
# Hardcoded so the workflow needs no repository variable. Models are tried in
# order: an id that no longer exists, a model under load (503) or a model out of
# free-tier quota (429) hands the pick to the next one, so a busy tier cannot
# lose the day's video.
DEFAULT_MODEL = "gemini-3.8-flash"
FALLBACK_MODELS = (
    "gemini-3.6-flash",
    "gemini-3.5-flash-lite",
    "gemini-3.1-flash-lite",
)
DEFAULT_MODELS = (DEFAULT_MODEL, *FALLBACK_MODELS)
# A quota hint longer than this is not worth blocking the job for; the next
# model answers instead.
MAX_RETRY_WAIT = 65.0
RETRY_HINT_RE = re.compile(r"retry in (\d+(?:\.\d+)?)\s*s", re.IGNORECASE)
MAX_ATTEMPTS_PER_SECTION = 3
RETRYABLE_STATUS = (408, 429, 500, 502, 503, 504)
ID_SUFFIX_RE = re.compile(r"^stem-(\d+)$")
YOUTUBE_URL_ID_RE = re.compile(
    r"(?:v=|youtu\.be/|/embed/|/shorts/|/live/)([A-Za-z0-9_-]{11})"
)
BARE_ID_RE = re.compile(r"^[A-Za-z0-9_-]{11}$")

RECOMMENDATION_SCHEMA = {
    "type": "object",
    "properties": {
        "youtubeId": {
            "type": "string",
            "description": "The exact 11-character id from the video's watch URL.",
        },
        "title": {"type": "string"},
        "channel": {"type": "string"},
        "coreQuestion": {"type": "string"},
        "difficulty": {
            "type": "string",
            "enum": list(catalogue.VALID_DIFFICULTIES),
        },
        "duration": {"type": "string"},
        "concepts": {"type": "array", "items": {"type": "string"}},
        "description": {"type": "string"},
        "takeaway": {"type": "string"},
    },
    "required": [
        "youtubeId",
        "title",
        "channel",
        "coreQuestion",
        "difficulty",
        "duration",
        "concepts",
        "description",
        "takeaway",
    ],
}


class GeminiError(RuntimeError):
    """The Gemini API rejected the request or returned something unusable."""


class ModelUnavailable(GeminiError):
    """The requested model id does not exist for this key; try the next one."""


class ModelBusy(GeminiError):
    """The model is overloaded or out of quota; a lighter model may answer."""


class LookupFailed(RuntimeError):
    """A transient failure while checking a video with YouTube."""


# ---------------------------------------------------------------------------
# HTTP helpers
# ---------------------------------------------------------------------------

def _retry_delay(
    error: urllib.error.HTTPError | None, attempt: int, detail: str = ""
) -> float:
    # Gemini puts the useful hint in the message body ("Please retry in 46.7s")
    # rather than in a Retry-After header, and it is a hard quota window.
    hint = RETRY_HINT_RE.search(detail or "")
    if hint:
        return min(max(float(hint.group(1)), 1.0), MAX_RETRY_WAIT)
    if error is not None and error.headers is not None:
        retry_after = error.headers.get("Retry-After")
        if retry_after and retry_after.strip().isdigit():
            return min(float(retry_after), 60.0)
    return min(2.0 ** attempt + random.uniform(0.0, 1.0), 30.0)


def _error_detail(error: urllib.error.HTTPError) -> str:
    try:
        payload = json.loads(error.read().decode("utf-8"))
    except Exception:  # noqa: BLE001 - diagnostics only
        return error.reason if isinstance(error.reason, str) else str(error.code)

    message = ((payload.get("error") or {}).get("message") or "").strip()
    return message or str(error.code)


def _request_json(
    url: str,
    *,
    payload: dict[str, Any] | None = None,
    headers: dict[str, str] | None = None,
    timeout: int = 60,
) -> dict[str, Any]:
    data = json.dumps(payload).encode("utf-8") if payload is not None else None
    request = urllib.request.Request(
        url,
        data=data,
        method="POST" if data is not None else "GET",
        headers=headers or {},
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


# ---------------------------------------------------------------------------
# Gemini
# ---------------------------------------------------------------------------

def build_prompt(category: str, library: list[dict[str, Any]]) -> str:
    titles = "\n".join(f"- {entry.get('title', '')}" for entry in library)
    taken_ids = ", ".join(entry.get("youtubeId", "") for entry in library)

    return f"""You curate STEMStream, a hand-picked library of the best science,
space, engineering and mathematics videos on YouTube. Choose exactly ONE video
for the "{category}" section.

Rules
- The video must be a real, currently public YouTube upload that allows
  embedding. Return the exact 11-character id from its watch URL. Never invent
  or guess an id: a wrong id is worse than no answer.
- It must not already be in the library. Already curated titles:
{titles}
- Already curated ids: {taken_ids}
- Prefer long-form explainers (roughly 8 to 45 minutes) from credible science,
  engineering or education creators. Substance over hype, idea over channel.
- coreQuestion: the single question the video answers, phrased as a question.
- description: two or three sentences on what the video actually shows.
- takeaway: the one idea a viewer should leave with.
- concepts: three or four short technical terms the video uses.
- duration: an approximate length such as "18 min".
- difficulty: Foundational, Intermediate or Deep Dive.

Reply with JSON only."""


def generate(
    model: str,
    api_key: str,
    category: str,
    library: list[dict[str, Any]],
    rejected: list[str] | tuple[str, ...] = (),
) -> dict[str, Any]:
    prompt = build_prompt(category, library)
    if rejected:
        prompt += (
            "\n\nYour previous suggestions failed verification for these reasons, "
            "so pick something else: " + "; ".join(rejected)
        )

    payload = {
        "contents": [{"role": "user", "parts": [{"text": prompt}]}],
        "generationConfig": {
            "temperature": 0.9,
            "responseMimeType": "application/json",
            "responseSchema": RECOMMENDATION_SCHEMA,
        },
    }
    headers = {"Content-Type": "application/json", "x-goog-api-key": api_key}
    url = GEMINI_ENDPOINT.format(model=urllib.parse.quote(model, safe=""))

    response: dict[str, Any] | None = None
    for attempt in range(4):
        try:
            response = _request_json(url, payload=payload, headers=headers)
            break
        except urllib.error.HTTPError as error:
            detail = _error_detail(error)
            if error.code == 404:
                raise ModelUnavailable(f"{model} is not available: {detail}") from error
            if error.code in RETRYABLE_STATUS and attempt < 3:
                delay = _retry_delay(error, attempt, detail)
                if delay <= MAX_RETRY_WAIT:
                    print(
                        f"  - Gemini {error.code} for {category}, "
                        f"retrying in {delay:.0f}s"
                    )
                    time.sleep(delay)
                    continue
            if error.code in RETRYABLE_STATUS:
                # Busy or rate limited: a lighter model is more likely to answer
                # than yet another attempt against this one.
                raise ModelBusy(f"{model} responded {error.code}: {detail}") from error
            raise GeminiError(f"Gemini responded {error.code}: {detail}") from error
        except urllib.error.URLError as error:
            if attempt < 3:
                delay = _retry_delay(None, attempt)
                print(f"  - Gemini unreachable for {category}, retrying in {delay:.0f}s")
                time.sleep(delay)
                continue
            raise GeminiError(f"could not reach the Gemini API ({error.reason})") from error
        except (TimeoutError, http.client.HTTPException) as error:
            # A stalled or dropped response surfaces as a bare timeout rather
            # than URLError; one slow call must not end the whole run.
            if attempt < 3:
                delay = _retry_delay(None, attempt)
                print(f"  - Gemini stalled for {category} ({error}), retrying in {delay:.0f}s")
                time.sleep(delay)
                continue
            raise GeminiError(f"Gemini request stalled ({error})") from error

    if response is None:
        raise GeminiError("Gemini did not answer after four attempts")

    candidates = response.get("candidates") or []
    if not candidates:
        block = (response.get("promptFeedback") or {}).get("blockReason")
        raise GeminiError(f"Gemini returned no candidate (blockReason={block or 'unknown'})")

    parts = (candidates[0].get("content") or {}).get("parts") or []
    text = "".join(part.get("text", "") for part in parts).strip()
    if not text:
        raise GeminiError("Gemini returned an empty response")

    text = re.sub(r"^```(?:json)?\s*|\s*```$", "", text)
    try:
        recommendation = json.loads(text)
    except json.JSONDecodeError as error:
        raise GeminiError(f"Gemini returned malformed JSON ({error})") from error
    if not isinstance(recommendation, dict):
        raise GeminiError("Gemini returned something other than a JSON object")
    return recommendation


# ---------------------------------------------------------------------------
# YouTube verification
# ---------------------------------------------------------------------------

def clean_youtube_id(value: Any) -> str:
    raw = str(value or "").strip()
    match = YOUTUBE_URL_ID_RE.search(raw)
    if match:
        return match.group(1)
    if BARE_ID_RE.match(raw):
        return raw
    return ""


def lookup_video(youtube_id: str) -> dict[str, Any] | None:
    """Return YouTube's own metadata, or None when the upload is not playable."""
    watch_url = f"https://www.youtube.com/watch?v={youtube_id}"
    url = (
        "https://www.youtube.com/oembed?format=json&url="
        + urllib.parse.quote(watch_url, safe="")
    )
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError:
        # 401/403: embedding disabled. 404: gone, private or never existed.
        return None
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as error:
        raise LookupFailed(f"could not verify {youtube_id}: {error}") from error

    if not isinstance(payload, dict) or not payload.get("title"):
        return None
    return payload


# ---------------------------------------------------------------------------
# Curation
# ---------------------------------------------------------------------------

def _next_id_number(entries: list[dict[str, Any]]) -> int:
    highest = 0
    for entry in entries:
        match = ID_SUFFIX_RE.match(str(entry.get("id", "")))
        if match:
            highest = max(highest, int(match.group(1)))
    return max(highest + 1, len(entries) + 1)


def _pick_entry(
    model: str,
    api_key: str,
    category: str,
    library: list[dict[str, Any]],
    taken: set[str],
    number: int,
) -> dict[str, Any] | None:
    """Ask Gemini until one recommendation survives verification."""
    rejected: list[str] = []

    for attempt in range(1, MAX_ATTEMPTS_PER_SECTION + 1):
        candidate = generate(model, api_key, category, library, rejected)
        video_id = clean_youtube_id(candidate.get("youtubeId"))
        if not video_id:
            rejected.append("no valid 11-character YouTube id")
            print(f"  - {category}: no usable id (attempt {attempt})")
            continue

        if video_id in taken:
            rejected.append(f"{video_id} is already in the library")
            print(f"  - {category}: {video_id} is already curated")
            continue

        try:
            metadata = lookup_video(video_id)
        except LookupFailed as error:
            print(f"  - {error}")
            time.sleep(_retry_delay(None, attempt))
            continue

        if metadata is None:
            rejected.append(f"{video_id} is not a public, embeddable video")
            print(f"  - {category}: {video_id} is unavailable or not embeddable")
            continue

        try:
            entry = catalogue.normalise_entry(
                {
                    **candidate,
                    "youtubeId": video_id,
                    # YouTube is authoritative about what the upload is.
                    "title": metadata.get("title") or candidate.get("title"),
                    "channel": metadata.get("author_name") or candidate.get("channel"),
                    "category": category,
                },
                number,
            )
        except ValueError as error:
            rejected.append(f"rejected by the catalogue schema ({error})")
            print(f"  - {category}: {error}")
            continue

        print(
            f"  + {category}: {entry['title']} — {entry['channel']} ({video_id})"
        )
        return entry

    return None


def _sections_for(day: date) -> list[str]:
    """The single section that is due on the given day."""
    index = day.toordinal() % len(catalogue.VALID_CATEGORIES)
    return [catalogue.VALID_CATEGORIES[index]]


def add_daily_videos(
    *,
    api_key: str,
    models: list[str] | None = None,
    path: str = DATA_PATH,
    dry_run: bool = False,
    categories: list[str] | None = None,
    day: date | None = None,
) -> int:
    """Add one verified video for the section due that day. Returns the count."""
    with open(path, "r", encoding="utf-8") as handle:
        entries = json.load(handle)
    if not isinstance(entries, list) or not entries:
        raise GeminiError(f"catalogue must be a non-empty JSON array: {path}")

    remaining_models = list(models or DEFAULT_MODELS)
    taken = {str(entry.get("youtubeId", "")) for entry in entries}
    number = _next_id_number(entries)
    sections = list(categories or _sections_for(day or date.today()))

    added: list[dict[str, Any]] = []
    skipped: list[str] = []

    for category in sections:
        library = [entry for entry in entries if entry.get("category") == category]
        entry = None

        while remaining_models and entry is None:
            model = remaining_models[0]
            try:
                entry = _pick_entry(model, api_key, category, library, taken, number)
            except (ModelUnavailable, ModelBusy) as error:
                print(f"  - {error}; trying the next model", file=sys.stderr)
                remaining_models.pop(0)
                continue
            except GeminiError as error:
                # A bad key or a hard rejection affects every section, so this
                # is reported and the remaining sections are still attempted.
                print(f"  ! {category}: {error}", file=sys.stderr)
                break

            if entry is None:
                break

        if entry is None:
            skipped.append(category)
            continue

        taken.add(entry["youtubeId"])
        added.append(entry)
        number += 1

    if not added:
        raise GeminiError("no recommendation passed verification, catalogue unchanged")

    updated = added + entries
    if dry_run:
        print(f"\ndry run: would prepend {len(added)} video(s)")
    else:
        catalogue.write_catalogue(updated, path)
        print(f"\nprepended {len(added)} video(s); catalogue now holds {len(updated)}")

    if skipped:
        print(f"warning: no new video for {', '.join(skipped)}", file=sys.stderr)

    return len(added)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Add one Gemini-recommended video to today's STEMStream section."
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="ask Gemini and verify, but do not write data.json",
    )
    parser.add_argument(
        "--category",
        choices=list(catalogue.VALID_CATEGORIES),
        help="curate this section instead of the one the date selects",
    )
    args = parser.parse_args()

    api_key = os.environ.get("GEMINI_API_KEY", "").strip()
    if not api_key:
        print(
            "error: GEMINI_API_KEY is not set. Add it as a repository secret "
            "(Settings → Secrets and variables → Actions).",
            file=sys.stderr,
        )
        return 1

    models: list[str] = []
    preferred = os.environ.get("GEMINI_MODEL", "").strip()
    if preferred:
        models.append(preferred)
    for model in DEFAULT_MODELS:
        if model not in models:
            models.append(model)

    sections = [args.category] if args.category else _sections_for(date.today())

    print(f"STEMStream daily picks: {', '.join(sections)}")
    try:
        added = add_daily_videos(
            api_key=api_key,
            models=models,
            dry_run=args.dry_run,
            categories=sections,
        )
    except (GeminiError, OSError, json.JSONDecodeError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 1

    print(f"done: {added} new video(s) at the top of their section")
    return 0


if __name__ == "__main__":
    sys.exit(main())
