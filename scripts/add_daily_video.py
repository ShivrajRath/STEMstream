#!/usr/bin/env python3
"""
STEMStream daily picks
======================

Finds what the tracked STEM channels have just published, asks Gemini to choose
one of those uploads and write its card, and prepends the accepted entry to
``src/data/data.json`` so the new video is the first card in its section (and in
the combined collection).

The id is never the model's invention. Candidates come from YouTube's
per-channel RSS feeds (``/feeds/videos.xml?channel_id=...``), which need no API
key, and every one of them is checked against YouTube's public oEmbed endpoint
before Gemini sees it. The title and channel are taken from YouTube itself, so a
card always names the real upload.

The finished card is then reviewed by a second request: it is shown the upload's
real title and channel next to the core question, description, concepts and
takeaway, and rejects the card when the prose describes some other subject or
the upload is not STEM education. A rejected card costs an attempt, never a day
of publishing.

One section is curated per day, rotating through ``VALID_CATEGORIES`` on the
date, so every section gets a new pick each week while a run spends a couple of
requests instead of seven.

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

The tracked channels live in ``scripts/stem_channels.json``. Only the Python
standard library is used.
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
import xml.etree.ElementTree as ET
from datetime import date
from typing import Any

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
if SCRIPT_DIR not in sys.path:
    sys.path.insert(0, SCRIPT_DIR)

# Reuse the catalogue's own schema rules instead of duplicating them.
import update_content as catalogue  # noqa: E402 - sys.path is prepared above

DATA_PATH = catalogue.DATA_PATH
STEM_CHANNELS_PATH = os.path.join(SCRIPT_DIR, "stem_channels.json")
USER_AGENT = "STEMStream-Curation/1.0 (+https://github.com/)"
GEMINI_ENDPOINT = (
    "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
)
FEED_ENDPOINT = "https://www.youtube.com/feeds/videos.xml?channel_id={channel_id}"
ATOM_NS = {
    "atom": "http://www.w3.org/2005/Atom",
    "yt": "http://www.youtube.com/xml/schemas/2015",
}
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
# Shared by the Gemini call and the YouTube lookup: "try again later", as
# opposed to "this upload is gone".
RETRYABLE_STATUS = (408, 429, 500, 502, 503, 504)
LOOKUP_ATTEMPTS = 3
# A channel feed carries its last 15 uploads, so a long window lets a quiet
# channel stay useful without ever offering ancient videos.
FEED_MAX_AGE_DAYS = 120
# How many verified candidates to offer the model at once. Each one costs an
# oEmbed request, so this is also the run's verification budget.
MAX_CANDIDATES = 8
CHANNEL_ID_RE = re.compile(r"^UC[A-Za-z0-9_-]{22}$")
ID_SUFFIX_RE = re.compile(r"^stem-(\d+)$")
BARE_ID_RE = re.compile(r"^[A-Za-z0-9_-]{11}$")

# YouTube's oEmbed endpoint only proves a video exists and allows embedding;
# it says nothing about the subject. Titles or channels that look like
# ambient/relaxation uploads or music releases are never a fit for STEMStream,
# no matter how calm they are. A music video once slipped through this way: its
# id resolved and allowed embedding, so only the wording can tell.
NON_STEM_TITLE_WORDS = (
    "fireplace", "ambience", "relaxation", "relaxing", "cozy", "cosy",
    "meditation", "remix", "lyrics",
)
NON_STEM_TITLE_PHRASES = (
    "fire place", "cozy fire", "cosy fire", "sleep music", "white noise",
    "nature sounds", "background music", "background noise",
    "background viewing", "ambient music", "ambient noise",
    "chillhop", "chill hop", "study beats",
    # Music releases: official music videos, lyric videos, audio uploads and the
    # auto-generated "Artist - Topic" channels. "vevo" and "- topic" are matched
    # as substrings so "NirvanaVEVO" and "Nirvana - Topic" are both caught.
    "official music video", "official video", "official audio", "lyric video",
    "vevo", " - topic",
)


def looks_non_stem(title: str, channel: str) -> str | None:
    """Return a reason when the title or channel looks like ambient or
    relaxation content rather than STEM education, else None."""
    haystack = " ".join((title or "", channel or "")).lower()
    for word in NON_STEM_TITLE_WORDS:
        if re.search(rf"\b{re.escape(word)}\b", haystack):
            return (
                "title/channel looks like non-STEM ambient or music content "
                f"({word!r})"
            )
    for phrase in NON_STEM_TITLE_PHRASES:
        if phrase in haystack:
            return (
                "title/channel looks like non-STEM ambient or music content "
                f"({phrase!r})"
            )
    return None


RECOMMENDATION_SCHEMA = {
    "type": "object",
    "properties": {
        "pick": {
            "type": "integer",
            "description": "The 1-based number of the chosen candidate.",
        },
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
        "pick",
        "coreQuestion",
        "difficulty",
        "duration",
        "concepts",
        "description",
        "takeaway",
    ],
}

# The finished card is checked once more, against the upload's real title and
# channel, before it can reach the catalogue.
REVIEW_SCHEMA = {
    "type": "object",
    "properties": {
        "approved": {
            "type": "boolean",
            "description": "True only when the card describes this exact upload.",
        },
        "reason": {
            "type": "string",
            "description": "One sentence: what passes, or what is wrong.",
        },
    },
    "required": ["approved", "reason"],
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
# Discovery: what the tracked channels have published lately
# ---------------------------------------------------------------------------

def _bare_channel_id(value: str) -> str:
    """Channel ids appear both as ``UC...`` and as the feed's bare suffix."""
    value = (value or "").strip()
    return value[2:] if value.startswith("UC") else value


def _published_date(text: str) -> date | None:
    try:
        return date.fromisoformat((text or "").strip()[:10])
    except ValueError:
        return None


def load_channels(path: str = STEM_CHANNELS_PATH) -> list[dict[str, Any]]:
    """The tracked STEM channels, validated. See scripts/stem_channels.json."""
    with open(path, "r", encoding="utf-8") as handle:
        payload = json.load(handle)

    rows = payload.get("channels") if isinstance(payload, dict) else payload
    if not isinstance(rows, list) or not rows:
        raise GeminiError(f"no channels defined in {path}")

    channels: list[dict[str, Any]] = []
    for row in rows:
        if not isinstance(row, dict):
            continue
        channel_id = str(row.get("channelId", "")).strip()
        if not CHANNEL_ID_RE.match(channel_id):
            print(f"  - skipping channel with a bad id: {row!r}", file=sys.stderr)
            continue
        categories = [
            str(name)
            for name in row.get("categories") or []
            if str(name) in catalogue.VALID_CATEGORIES
        ]
        if not categories:
            print(f"  - skipping {channel_id}: no valid section", file=sys.stderr)
            continue
        channels.append(
            {
                "channelId": channel_id,
                "name": str(row.get("name") or channel_id).strip(),
                "categories": categories,
            }
        )

    if not channels:
        raise GeminiError(f"no usable channels in {path}")
    return channels


def fetch_feed(channel_id: str, timeout: int = 20) -> list[dict[str, Any]]:
    """Recent uploads for one channel, newest first. Empty when unavailable."""
    url = FEED_ENDPOINT.format(channel_id=urllib.parse.quote(channel_id, safe=""))
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            payload = response.read()
    except (urllib.error.URLError, TimeoutError, http.client.HTTPException) as error:
        print(f"  - feed for {channel_id} unavailable ({error})", file=sys.stderr)
        return []

    try:
        root = ET.fromstring(payload)
    except ET.ParseError as error:
        print(f"  - feed for {channel_id} is not valid XML ({error})", file=sys.stderr)
        return []

    served_id = root.findtext("yt:channelId", namespaces=ATOM_NS) or ""
    if served_id and _bare_channel_id(served_id) != _bare_channel_id(channel_id):
        # A wrong id in stem_channels.json would quietly curate someone else.
        print(
            f"  - feed {channel_id} describes {served_id}; skipped",
            file=sys.stderr,
        )
        return []

    channel_name = " ".join(
        (root.findtext("atom:title", namespaces=ATOM_NS) or "").split()
    )
    uploads: list[dict[str, Any]] = []
    for node in root.findall("atom:entry", ATOM_NS):
        video_id = (node.findtext("yt:videoId", namespaces=ATOM_NS) or "").strip()
        title = " ".join(
            (node.findtext("atom:title", namespaces=ATOM_NS) or "").split()
        )
        published = (node.findtext("atom:published", namespaces=ATOM_NS) or "").strip()
        if not BARE_ID_RE.match(video_id) or not title:
            continue
        uploads.append(
            {
                "youtubeId": video_id,
                "title": title,
                "channel": channel_name,
                "published": published,
            }
        )
    return uploads


def discover_candidates(
    *,
    categories: list[str],
    taken: set[str],
    channels: list[dict[str, Any]],
    today: date | None = None,
    limit: int = MAX_CANDIDATES,
) -> list[dict[str, Any]]:
    """Fresh, uncurated, embeddable uploads, the day's section first.

    Nothing here trusts a model: ids come from the feeds and every returned
    candidate has been confirmed by YouTube's oEmbed endpoint.
    """
    today = today or date.today()
    wanted = set(categories)
    pool: list[dict[str, Any]] = []
    seen: set[str] = set()

    for channel in channels:
        uploads = fetch_feed(channel["channelId"])
        if not uploads:
            continue
        channel_categories = channel.get("categories") or []
        category = next(
            (name for name in channel_categories if name in wanted),
            channel_categories[0] if channel_categories else "",
        )
        if not category:
            continue

        for upload in uploads:
            video_id = upload["youtubeId"]
            if video_id in taken or video_id in seen:
                continue
            published = _published_date(upload.get("published", ""))
            if published is None or (today - published).days > FEED_MAX_AGE_DAYS:
                continue
            if looks_non_stem(upload["title"], upload["channel"]):
                continue
            seen.add(video_id)
            pool.append(
                {
                    **upload,
                    "category": category,
                    "preferred": category in wanted,
                    "publishedDate": published,
                }
            )

    # The day's section first, then the freshest upload from any channel.
    pool.sort(key=lambda item: (item["preferred"], item["publishedDate"]), reverse=True)

    verified: list[dict[str, Any]] = []
    for item in pool:
        if len(verified) >= limit:
            break
        try:
            metadata = verify_video(item["youtubeId"])
        except LookupFailed as error:
            print(f"  - {error}", file=sys.stderr)
            continue
        if metadata is None:
            print(
                f"  - {item['youtubeId']} is unavailable or not embeddable; skipped",
                file=sys.stderr,
            )
            continue
        verified.append(item)
    return verified


# ---------------------------------------------------------------------------
# Gemini
# ---------------------------------------------------------------------------

def build_prompt(category: str, candidates: list[dict[str, Any]]) -> str:
    listing = "\n".join(
        f'{index}. "{item["title"]}" — {item["channel"]} '
        f'({item["category"]}, published {item["publishedDate"].isoformat()})'
        for index, item in enumerate(candidates, start=1)
    )

    return f"""You curate STEMStream, a hand-picked library of the best science,
space, engineering and mathematics videos on YouTube. Choose exactly ONE video
for the "{category}" section from the candidates below.

Candidates
{listing}

Rules
- Return the chosen candidate's number in "pick". Every candidate is a real,
  public, embeddable upload; never name a video that is not in this list, and
  never return an id or a title instead of the number.
- Prefer a long-form explainer (roughly 8 to 45 minutes) that rewards a curious
  adult, and prefer the freshest upload that genuinely fits "{category}".
  Substance over hype, idea over channel. Skip anything that is not STEM
  education, whatever its number.
- coreQuestion: the single question the video answers, phrased as a question.
- description: two or three sentences on what the video actually shows.
- takeaway: the one idea a viewer should leave with.
- concepts: three or four short technical terms the video uses.
- duration: an approximate length such as "18 min".
- difficulty: Foundational, Intermediate or Deep Dive.

Reply with JSON only."""


def _ask_json(
    model: str,
    api_key: str,
    prompt: str,
    schema: dict[str, Any],
    temperature: float,
    label: str,
) -> dict[str, Any]:
    """One structured-JSON request to Gemini, with the shared retry policy."""
    payload = {
        "contents": [{"role": "user", "parts": [{"text": prompt}]}],
        "generationConfig": {
            "temperature": temperature,
            "responseMimeType": "application/json",
            "responseSchema": schema,
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
                        f"  - Gemini {error.code} for {label}, "
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
                print(f"  - Gemini unreachable for {label}, retrying in {delay:.0f}s")
                time.sleep(delay)
                continue
            raise GeminiError(f"could not reach the Gemini API ({error.reason})") from error
        except (TimeoutError, http.client.HTTPException) as error:
            # A stalled or dropped response surfaces as a bare timeout rather
            # than URLError; one slow call must not end the whole run.
            if attempt < 3:
                delay = _retry_delay(None, attempt)
                print(f"  - Gemini stalled for {label} ({error}), retrying in {delay:.0f}s")
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


def generate(
    model: str,
    api_key: str,
    category: str,
    candidates: list[dict[str, Any]],
    rejected: list[str] | tuple[str, ...] = (),
) -> dict[str, Any]:
    prompt = build_prompt(category, candidates)
    if rejected:
        # The chain retries a rejected section with the next model, so the list
        # can grow; keep the prompt short and free of repeats.
        recent = list(dict.fromkeys(rejected))[-6:]
        prompt += (
            "\n\nYour previous picks were not usable for these reasons, so choose "
            "a different candidate: " + "; ".join(recent)
        )
    return _ask_json(model, api_key, prompt, RECOMMENDATION_SCHEMA, 0.9, category)


def build_review_prompt(category: str, entry: dict[str, Any]) -> str:
    concepts = ", ".join(entry.get("concepts") or []) or "—"
    return f"""You are the last check before a video is published on STEMStream, a
hand-picked library of science, space, engineering and mathematics explainers.
Another request already chose this upload and wrote its card. Your only job is
to reject the card when it does not describe this upload, or when the upload is
not STEM education.

Video, as YouTube itself reports it
- Title: {entry['title']}
- Channel: {entry['channel']}
- Section: {category}

Card under review
- Core question: {entry.get('coreQuestion', '')}
- Description: {entry.get('description', '')}
- Takeaway: {entry.get('takeaway', '')}
- Concepts: {concepts}
- Difficulty: {entry.get('difficulty', '')}
- Duration: {entry.get('duration', '')}

Reject ("approved": false) when any of these is true
- The card describes a different subject, video or genre from the title and
  channel above, so a viewer who opened the video would find it is about
  something else.
- The card invents specifics — experiments, numbers, demonstrations, results —
  that this title and channel do not support.
- The upload is not STEM education: music, gaming, vlogs, entertainment,
  advertisements, ambient or relaxation content.
- The section fits the subject badly enough to mislead.

Approve ("approved": true) only when every part of the card would still read
correctly to someone watching the upload named above. A doubtful card is a
rejection: skipping one day is cheaper than publishing a mismatch.

"reason": one sentence naming what passes, or exactly what is wrong.
Reply with JSON only."""


def review_entry(
    model: str, api_key: str, category: str, entry: dict[str, Any]
) -> tuple[bool, str]:
    """Second opinion on a finished card: does it describe this upload?

    A separate call, with the video's real title and channel and nothing else,
    so a card written about some other subject is caught rather than published.
    """
    answer = _ask_json(
        model,
        api_key,
        build_review_prompt(category, entry),
        REVIEW_SCHEMA,
        0.0,
        f"review of {entry.get('id', entry.get('youtubeId', '?'))}",
    )
    reason = " ".join(str(answer.get("reason") or "").split())
    # Anything other than an explicit true is not a pass.
    return answer.get("approved") is True, reason or "no reason given"


# ---------------------------------------------------------------------------
# YouTube verification
# ---------------------------------------------------------------------------

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
    except urllib.error.HTTPError as error:
        # 401/403: embedding disabled. 404: gone, private or never existed.
        # 429 and 5xx are YouTube having a bad moment, not a verdict on the
        # upload, so they must not reject an id that is perfectly fine.
        if error.code in RETRYABLE_STATUS:
            raise LookupFailed(
                f"YouTube oEmbed responded {error.code} for {youtube_id}"
            ) from error
        return None
    except (
        urllib.error.URLError,
        TimeoutError,
        http.client.HTTPException,
        json.JSONDecodeError,
    ) as error:
        raise LookupFailed(f"could not verify {youtube_id}: {error}") from error

    if not isinstance(payload, dict) or not payload.get("title"):
        return None
    return payload


def verify_video(youtube_id: str, attempts: int = LOOKUP_ATTEMPTS) -> dict[str, Any] | None:
    """Return YouTube's metadata, retrying a transient lookup first.

    A dropped connection or a 5xx from YouTube says nothing about the video,
    so the same id is checked again before the suggestion is thrown away.
    """
    for attempt in range(1, attempts + 1):
        try:
            return lookup_video(youtube_id)
        except LookupFailed as error:
            print(f"  - {error}")
            if attempt >= attempts:
                raise
            time.sleep(_retry_delay(None, attempt))
    return None


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
    candidates: list[dict[str, Any]],
    taken: set[str],
    number: int,
    rejected: list[str] | None = None,
) -> dict[str, Any] | None:
    """Ask Gemini to choose a candidate and write its card.

    ``rejected`` is caller-owned so a model that comes up empty hands its
    reasons to the next one instead of letting it repeat the same mistake.
    """
    rejected = rejected if rejected is not None else []

    for attempt in range(1, MAX_ATTEMPTS_PER_SECTION + 1):
        recommendation = generate(model, api_key, category, candidates, rejected)

        pick = recommendation.get("pick")
        try:
            index = int(pick)
        except (TypeError, ValueError):
            index = 0
        if not 1 <= index <= len(candidates):
            rejected.append(
                f"pick {pick!r} is not one of the {len(candidates)} candidates"
            )
            print(f"  - {category}: pick {pick!r} is out of range (attempt {attempt})")
            continue

        chosen = candidates[index - 1]
        video_id = chosen["youtubeId"]
        if video_id in taken:
            rejected.append(f"{video_id} is already in the library")
            print(f"  - {category}: {video_id} is already curated")
            continue

        # The candidate was checked during discovery; this re-check catches an
        # upload that went private in between.
        try:
            metadata = verify_video(video_id)
        except LookupFailed as error:
            rejected.append(f"{video_id} could not be verified ({error})")
            print(f"  - {error}")
            continue

        if metadata is None:
            rejected.append(f"{video_id} is no longer public and embeddable")
            print(f"  - {category}: {video_id} is unavailable or not embeddable")
            continue

        # YouTube proves the upload is public and embeddable; it does not
        # prove it is STEM. Reject ambient/relaxation uploads before they
        # reach the catalogue.
        title = metadata.get("title") or chosen["title"]
        channel = metadata.get("author_name") or chosen["channel"]
        reason = looks_non_stem(title, channel)
        if reason:
            rejected.append(reason)
            print(f"  - {category}: {reason} ({video_id})")
            continue

        try:
            entry = catalogue.normalise_entry(
                {
                    **recommendation,
                    "youtubeId": video_id,
                    # YouTube is authoritative about what the upload is.
                    "title": title,
                    "channel": channel,
                    "category": chosen["category"],
                },
                number,
            )
        except ValueError as error:
            rejected.append(f"rejected by the catalogue schema ({error})")
            print(f"  - {category}: {error}")
            continue

        # A second, independent pass over the finished card. The request that
        # wrote it is the least likely to notice it describes another video,
        # which is how an off-topic upload can still carry a plausible card.
        try:
            approved, review_reason = review_entry(model, api_key, category, entry)
        except GeminiError as error:
            rejected.append(f"the review step could not run ({error})")
            print(f"  - {category}: review unavailable ({error})")
            continue

        if not approved:
            rejected.append(f"review rejected the card: {review_reason}")
            print(f"  - {category}: review rejected {video_id} ({review_reason})")
            continue

        print(
            f"  + {entry['category']}: {entry['title']} — {entry['channel']} ({video_id})"
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
    channels: list[dict[str, Any]] | None = None,
) -> int:
    """Add one verified video for the section due that day. Returns the count."""
    with open(path, "r", encoding="utf-8") as handle:
        entries = json.load(handle)
    if not isinstance(entries, list) or not entries:
        raise GeminiError(f"catalogue must be a non-empty JSON array: {path}")

    today = day or date.today()
    sections = list(categories or _sections_for(today))
    tracked = channels if channels is not None else load_channels()
    taken = {str(entry.get("youtubeId", "")) for entry in entries}

    candidates = discover_candidates(
        categories=sections, taken=taken, channels=tracked, today=today
    )
    if not candidates:
        raise GeminiError(
            "no fresh, uncurated, embeddable upload from the tracked channels; "
            f"catalogue unchanged (check {os.path.basename(STEM_CHANNELS_PATH)})"
        )
    print(
        f"discovered {len(candidates)} verified candidate(s) for "
        f"{', '.join(sections)}"
    )

    remaining_models = list(models or DEFAULT_MODELS)
    number = _next_id_number(entries)

    added: list[dict[str, Any]] = []
    skipped: list[str] = []

    for category in sections:
        entry = None
        rejected: list[str] = []

        while remaining_models and entry is None:
            model = remaining_models[0]
            try:
                entry = _pick_entry(
                    model, api_key, category, candidates, taken, number, rejected
                )
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
                # Every pick this model returned was unusable. A different
                # model may choose a better candidate, so the day is not lost
                # until the whole chain has been tried.
                print(
                    f"  - {model} returned no usable pick for {category}; "
                    "trying the next model",
                    file=sys.stderr,
                )
                remaining_models.pop(0)

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
        description=(
            "Add one video from the tracked channels to today's STEMStream section."
        )
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="discover, verify and ask Gemini, but do not write data.json",
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
