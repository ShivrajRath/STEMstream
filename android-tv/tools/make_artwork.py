#!/usr/bin/env python3
"""Generate STEMStream TV launcher icons, TV banner and store artwork.

Everything is drawn locally with Pillow, so the artwork is reproducible without
a design tool. Run from anywhere:

    python3 android-tv/tools/make_artwork.py

Requires Pillow (`python3 -m pip install --user pillow`).
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
ARTWORK = ROOT / "artwork"

INK = (8, 9, 10, 255)
WHITE = (255, 255, 255, 255)
MUTED = (161, 161, 170, 255)
ACCENT = (255, 45, 85, 255)
ACCENT_WARM = (255, 122, 102, 255)
ACCENT_AMBER = (255, 154, 60, 255)

FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_REGULAR = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def font(path: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(path, size)


def radial_glow(size: int, color: tuple[int, int, int, int], strength: float = 0.55) -> Image.Image:
    """A soft radial glow on a transparent canvas."""
    gradient = Image.radial_gradient("L").resize((size, size), Image.Resampling.BICUBIC)
    alpha = ImageOps.invert(gradient)
    alpha = alpha.point(lambda value: int(value * strength))
    layer = Image.new("RGBA", (size, size), color)
    layer.putalpha(alpha)
    return layer


def atom_mark(size: int, tint: tuple[int, int, int, int] = WHITE, accent: tuple[int, int, int, int] = ACCENT) -> Image.Image:
    """Draw the three-orbit atom mark on a transparent square canvas."""
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    center = size / 2
    oval_w = size * 0.78
    oval_h = size * 0.28
    stroke = max(2, int(size * 0.022))
    box = (center - oval_w / 2, center - oval_h / 2, center + oval_w / 2, center + oval_h / 2)

    for angle in (0, 60, 120):
        layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(layer)
        draw.ellipse(box, outline=tint, width=stroke)
        rotated = layer.rotate(angle, resample=Image.Resampling.BICUBIC, center=(center, center))
        canvas = Image.alpha_composite(canvas, rotated)

    draw = ImageDraw.Draw(canvas)
    dot = size * 0.072
    draw.ellipse(
        (center - dot, center - dot, center + dot, center + dot),
        fill=accent,
    )
    # A small highlight on the nucleus gives the flat mark a little depth.
    highlight = dot * 0.35
    draw.ellipse(
        (center - dot * 0.3 - highlight, center - dot * 0.3 - highlight,
         center - dot * 0.3 + highlight, center - dot * 0.3 + highlight),
        fill=(255, 255, 255, 180),
    )
    return canvas


def icon(size: int, round_icon: bool = False) -> Image.Image:
    """A dark launcher icon with a warm accent glow and the atom mark."""
    scale = 4
    big = size * scale
    image = Image.new("RGBA", (big, big), INK)

    glow_size = int(big * 0.95)
    glow = radial_glow(glow_size, ACCENT, 0.34)
    image.alpha_composite(glow, (int(-big * 0.22), int(big * 0.24)))

    ring = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    ring_draw = ImageDraw.Draw(ring)
    inset = int(big * 0.17)
    ring_draw.ellipse(
        (inset, inset, big - inset, big - inset),
        outline=(255, 255, 255, 24),
        width=max(2, int(big * 0.004)),
    )
    image = Image.alpha_composite(image, ring)

    mark = atom_mark(int(big * 0.66))
    image.alpha_composite(mark, ((big - mark.width) // 2, (big - mark.height) // 2))

    if round_icon:
        mask = Image.new("L", (big, big), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, big - 1, big - 1), fill=255)
        image.putalpha(mask)

    return image.resize((size, size), Image.Resampling.LANCZOS)


def banner(width: int = 320, height: int = 180) -> Image.Image:
    """The 320x180 Android TV / Fire TV launcher banner."""
    scale = 4
    w, h = width * scale, height * scale
    image = Image.new("RGBA", (w, h), INK)

    glow = radial_glow(int(h * 1.8), ACCENT, 0.38)
    image.alpha_composite(glow, (int(-h * 0.55), int(-h * 0.38)))

    # Fine vertical hairline that echoes the website's editorial rules.
    draw = ImageDraw.Draw(image)
    draw.line((int(w * 0.028), int(h * 0.18), int(w * 0.028), int(h * 0.82)), fill=(255, 255, 255, 40), width=2)

    mark_size = int(h * 0.56)
    mark = atom_mark(mark_size)
    image.alpha_composite(mark, (int(w * 0.08), (h - mark.height) // 2))

    title_font = font(FONT_BOLD, int(h * 0.245))
    tagline_font = font(FONT_REGULAR, int(h * 0.092))

    title = "STEMStream"
    accent = "TV"
    title_x = int(w * 0.30)
    title_y = int(h * 0.30)
    draw.text((title_x, title_y), title, font=title_font, fill=WHITE)
    title_w = draw.textlength(title, font=title_font)
    draw.text((title_x + title_w + int(w * 0.012), title_y), accent, font=title_font, fill=ACCENT)
    draw.text(
        (title_x, int(h * 0.585)),
        "Feed your curiosity.",
        font=tagline_font,
        fill=(255, 255, 255, 168),
    )

    return image.resize((width, height), Image.Resampling.LANCZOS).convert("RGB")


def feature_graphic(width: int = 1024, height: int = 500) -> Image.Image:
    """Google Play feature graphic / Fire TV promo art."""
    scale = 2
    w, h = width * scale, height * scale
    image = Image.new("RGBA", (w, h), INK)

    glow = radial_glow(int(h * 1.7), ACCENT, 0.4)
    image.alpha_composite(glow, (int(-h * 0.5), int(-h * 0.25)))
    glow2 = radial_glow(int(h * 1.3), (167, 139, 250, 255), 0.22)
    image.alpha_composite(glow2, (int(w * 0.55), int(-h * 0.45)))

    draw = ImageDraw.Draw(image)
    draw.line((int(w * 0.06), int(h * 0.2), int(w * 0.06), int(h * 0.8)), fill=(255, 255, 255, 40), width=3)

    mark_size = int(h * 0.54)
    mark = atom_mark(mark_size)
    image.alpha_composite(mark, (int(w * 0.11), (h - mark.height) // 2))

    title_font = font(FONT_BOLD, int(h * 0.18))
    tagline_font = font(FONT_REGULAR, int(h * 0.062))
    meta_font = font(FONT_REGULAR, int(h * 0.042))

    title_x = int(w * 0.32)
    title_y = int(h * 0.29)
    title = "STEMStream"
    draw.text((title_x, title_y), title, font=title_font, fill=WHITE)
    title_w = draw.textlength(title, font=title_font)
    draw.text((title_x + title_w + int(w * 0.008), title_y), "TV", font=title_font, fill=ACCENT)
    draw.text(
        (title_x, int(h * 0.53)),
        "Feed your curiosity.",
        font=tagline_font,
        fill=(255, 255, 255, 175),
    )
    draw.text(
        (title_x, int(h * 0.64)),
        "749 hand-picked videos  ·  7 disciplines  ·  no ads",
        font=meta_font,
        fill=(255, 255, 255, 118),
    )

    return image.resize((width, height), Image.Resampling.LANCZOS).convert("RGB")


def save(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=True)
    print(f"wrote {path.relative_to(ROOT)}")


def main() -> None:
    densities = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192,
    }

    for density, size in densities.items():
        save(icon(size), RES / f"mipmap-{density}" / "ic_launcher.png")
        save(icon(size, round_icon=True), RES / f"mipmap-{density}" / "ic_launcher_round.png")

    save(banner(), RES / "drawable-nodpi" / "tv_banner.png")

    save(icon(512), ARTWORK / "play-store-icon-512.png")
    save(banner(320, 180), ARTWORK / "tv-banner-320x180.png")
    save(feature_graphic(), ARTWORK / "play-feature-graphic-1024x500.png")


if __name__ == "__main__":
    main()
