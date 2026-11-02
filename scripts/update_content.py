#!/usr/bin/env python3
"""
STEMStream catalogue maintenance
================================

The catalogue is curated and version-controlled in this repository at
``src/data/data.json``. There is no external spreadsheet and no third-party
service required to build the site.

This script keeps that file healthy and canonical:

* validates every record against the schema used by the React app;
* checks YouTube ids, categories, difficulties and required fields;
* removes duplicate videos;
* rewrites ids and formatting in a deterministic order;
* optionally checks every video and thumbnail over the network.

Usage
-----
    python3 scripts/update_content.py                # validate + normalise file
    python3 scripts/update_content.py --check        # validate only (CI mode)
    python3 scripts/update_content.py --check-links  # also verify videos online
    python3 scripts/update_content.py --check-links --strict-links

Only the Python standard library is used.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Any

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_PATH = os.path.join(REPO_ROOT, "src", "data", "data.json")

VALID_CATEGORIES = (
    "Physics",
    "Space",
    "Computer Science",
    "Engineering",
    "Biology",
    "Math",
    "Chemistry",
)
VALID_DIFFICULTIES = ("Foundational", "Intermediate", "Deep Dive")

YOUTUBE_ID_RE = re.compile(r"^[A-Za-z0-9_-]{11}$")
USER_AGENT = "STEMStream-Catalogue/1.0 (+https://github.com/)"

REQUIRED_FIELDS = ("youtubeId", "title", "channel", "category", "description")


# ---------------------------------------------------------------------------
# Validation and normalisation
# ---------------------------------------------------------------------------

def string_field(entry: dict[str, Any], key: str, default: str = "") -> str:
    value = entry.get(key, default)
    if value is None:
        return default
    if not isinstance(value, str):
        raise ValueError(f"'{key}' must be a string")
    return " ".join(value.split())


def normalise_entry(entry: dict[str, Any], number: int) -> dict[str, Any]:
    if not isinstance(entry, dict):
        raise ValueError("entry must be a JSON object")

    youtube_id = string_field(entry, "youtubeId")
    if not YOUTUBE_ID_RE.match(youtube_id):
        raise ValueError(
            f"'youtubeId' must be an 11-character YouTube id, got {youtube_id!r}"
        )

    for field in REQUIRED_FIELDS:
        if field == "youtubeId":
            continue
        if not string_field(entry, field):
            raise ValueError(f"missing required field '{field}'")

    category = string_field(entry, "category")
    if category not in VALID_CATEGORIES:
        raise ValueError(
            f"unknown category {category!r}; use one of {', '.join(VALID_CATEGORIES)}"
        )

    difficulty = string_field(entry, "difficulty", "Intermediate") or "Intermediate"
    if difficulty not in VALID_DIFFICULTIES:
        raise ValueError(
            f"unknown difficulty {difficulty!r}; use one of "
            f"{', '.join(VALID_DIFFICULTIES)}"
        )

    concepts = entry.get("concepts", [])
    if isinstance(concepts, str):
        concepts = [part.strip() for part in concepts.split(",")]
    if not isinstance(concepts, list) or not all(
        isinstance(item, str) for item in concepts
    ):
        raise ValueError("'concepts' must be a list of strings")
    concepts = [item.strip() for item in concepts if item.strip()]

    return {
        "id": f"stem-{number:03d}",
        "youtubeId": youtube_id,
        "title": string_field(entry, "title"),
        "coreQuestion": string_field(entry, "coreQuestion"),
        "channel": string_field(entry, "channel"),
        "category": category,
        "difficulty": difficulty,
        "duration": string_field(entry, "duration", "—"),
        "concepts": concepts,
        "description": string_field(entry, "description"),
        "takeaway": string_field(entry, "takeaway"),
    }


def load_catalogue(path: str = DATA_PATH) -> list[dict[str, Any]]:
    if not os.path.exists(path):
        raise FileNotFoundError(f"catalogue not found: {path}")

    with open(path, "r", encoding="utf-8") as handle:
        payload = json.load(handle)

    if not isinstance(payload, list) or not payload:
        raise ValueError("catalogue must be a non-empty JSON array")

    cleaned: list[dict[str, Any]] = []
    seen: dict[str, int] = {}
    errors: list[str] = []

    for index, entry in enumerate(payload):
        try:
            item = normalise_entry(entry, len(cleaned) + 1)
        except ValueError as exc:
            title = entry.get("title", "untitled") if isinstance(entry, dict) else "?"
            errors.append(f"entry {index + 1} ({title!r}): {exc}")
            continue

        youtube_id = item["youtubeId"]
        if youtube_id in seen:
            print(
                f"  - skipping duplicate video {youtube_id} "
                f"(first seen at entry {seen[youtube_id]})"
            )
            continue

        seen[youtube_id] = index + 1
        cleaned.append(item)

    if errors:
        raise ValueError("\n".join(errors))
    if not cleaned:
        raise ValueError("catalogue is empty after validation")

    return cleaned


def write_catalogue(entries: list[dict[str, Any]], path: str = DATA_PATH) -> None:
    directory = os.path.dirname(path)
    os.makedirs(directory, exist_ok=True)
    temporary = f"{path}.tmp"
    with open(temporary, "w", encoding="utf-8") as handle:
        json.dump(entries, handle, indent=2, ensure_ascii=False)
        handle.write("\n")
    os.replace(temporary, path)


# ---------------------------------------------------------------------------
# Optional online checks
# ---------------------------------------------------------------------------

def _request(url: str, method: str = "GET") -> urllib.request.Request:
    return urllib.request.Request(
        url,
        method=method,
        headers={"User-Agent": USER_AGENT, "Accept-Language": "en-US,en;q=0.9"},
    )


def check_video(entry: dict[str, Any]) -> tuple[str, list[str]]:
    youtube_id = entry["youtubeId"]
    problems: list[str] = []

    oembed = (
        "https://www.youtube.com/oembed?format=json&url="
        + urllib.parse.quote(f"https://www.youtube.com/watch?v={youtube_id}", safe="")
    )
    try:
        with urllib.request.urlopen(_request(oembed), timeout=10) as response:
            if response.status != 200:
                problems.append(f"oEmbed responded {response.status}")
    except urllib.error.HTTPError as exc:
        problems.append(f"video unavailable (oEmbed {exc.code})")
    except Exception as exc:  # noqa: BLE001 - network diagnostics only
        problems.append(f"oEmbed check failed ({exc})")

    # maxresdefault is not available for every upload; hqdefault is a safe fallback.
    thumb_status = "missing"
    for quality in ("maxresdefault", "sddefault", "hqdefault"):
        thumb = f"https://img.youtube.com/vi/{youtube_id}/{quality}.jpg"
        try:
            with urllib.request.urlopen(_request(thumb, "HEAD"), timeout=10) as response:
                if response.status == 200:
                    thumb_status = quality
                    break
        except Exception:  # noqa: BLE001 - try the next quality
            continue

    if thumb_status == "missing":
        problems.append("no thumbnail available")

    return thumb_status, problems


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main() -> int:
    parser = argparse.ArgumentParser(description="Validate and normalise the STEMStream catalogue.")
    parser.add_argument(
        "--check",
        action="store_true",
        help="validate without rewriting data.json (useful in CI)",
    )
    parser.add_argument(
        "--check-links",
        action="store_true",
        help="verify every video and thumbnail with YouTube (requires network)",
    )
    parser.add_argument(
        "--strict-links",
        action="store_true",
        help="exit non-zero when an online check finds a problem",
    )
    args = parser.parse_args()

    print(f"STEMStream catalogue: {os.path.relpath(DATA_PATH, REPO_ROOT)}")

    try:
        entries = load_catalogue()
    except (FileNotFoundError, ValueError, json.JSONDecodeError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

    if not args.check:
        write_catalogue(entries)
        print(f"normalised and wrote {len(entries)} videos")
    else:
        print(f"validated {len(entries)} videos (no files written)")

    counts: dict[str, int] = {}
    for entry in entries:
        counts[entry["category"]] = counts.get(entry["category"], 0) + 1
    summary = " · ".join(f"{name} {counts[name]}" for name in VALID_CATEGORIES if name in counts)
    print(f"categories: {summary}")

    if args.check_links:
        print("\nchecking videos and thumbnails...")
        failures = 0
        with ThreadPoolExecutor(max_workers=8) as pool:
            futures = {pool.submit(check_video, entry): entry for entry in entries}
            for future in as_completed(futures):
                entry = futures[future]
                thumbnail, problems = future.result()
                label = f"{entry['id']} {entry['youtubeId']}"
                if problems:
                    failures += 1
                    print(f"  ! {label}: {'; '.join(problems)}")
                else:
                    print(f"  ok {label}: thumbnail {thumbnail}")

        if failures:
            message = f"{failures} video(s) need attention"
            if args.strict_links:
                print(f"\nerror: {message}", file=sys.stderr)
                return 1
            print(f"\nwarning: {message} (use --strict-links to fail)")
        else:
            print("\nall videos and thumbnails look healthy")

    return 0


if __name__ == "__main__":
    sys.exit(main())
