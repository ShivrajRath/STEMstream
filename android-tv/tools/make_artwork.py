#!/usr/bin/env python3
"""Generate STEMStream TV launcher icons, TV banner and store artwork.

Everything is drawn locally with Pillow, so the artwork is reproducible without
a design tool. Run from anywhere:

    python3 android-tv/tools/make_artwork.py

Requires Pillow (`python3 -m pip install --user pillow`).
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
ARTWORK = ROOT / "artwork"

INK = (8, 9, 10, 255)
WHITE = (255, 255, 255, 255)
ACCENT = (255, 45, 85, 255)
VIOLET = (167, 139, 250, 255)

FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_REGULAR = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def font(path: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(path, max(1, size))


def soft_glow(
    size: int,
    color: tuple[int, int, int, int],
    strength: float = 0.55,
) -> Image.Image:
    """A round, blurred glow on a transparent canvas.

    `Image.radial_gradient` is square and only reaches zero alpha at its
    corners, so compositing one leaves four straight seams where the square
    ends. Blurring a filled circle instead is round all the way out.
    """
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(size * 0.22))
    layer = Image.new("RGBA", (size, size), color)
    layer.putalpha(mask.point(lambda value: int(value * strength)))
    return layer


def atom_mark(
    size: int,
    tint: tuple[int, int, int, int] = WHITE,
    accent: tuple[int, int, int, int] = ACCENT,
) -> Image.Image:
    """The three-orbit atom mark on a transparent square canvas.

    The orbits are deliberately heavy: at a 48 px launcher size a hairline ring
    turns into grey mush, which is what made the old icon read as a blob.
    """
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    center = size / 2
    oval_w = size * 0.82
    oval_h = size * 0.32
    stroke = max(3, round(size * 0.038))
    box = (
        center - oval_w / 2,
        center - oval_h / 2,
        center + oval_w / 2,
        center + oval_h / 2,
    )

    for angle in (0, 60, 120):
        layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(layer)
        draw.ellipse(box, outline=tint, width=stroke)
        rotated = layer.rotate(angle, resample=Image.Resampling.BICUBIC, center=(center, center))
        canvas = Image.alpha_composite(canvas, rotated)

    draw = ImageDraw.Draw(canvas)
    # A dark disc behind the nucleus keeps it readable where the orbits cross.
    halo = size * 0.125
    draw.ellipse((center - halo, center - halo, center + halo, center + halo), fill=INK)

    dot = size * 0.095
    draw.ellipse((center - dot, center - dot, center + dot, center + dot), fill=accent)
    highlight = dot * 0.34
    draw.ellipse(
        (
            center - dot * 0.32 - highlight,
            center - dot * 0.32 - highlight,
            center - dot * 0.32 + highlight,
            center - dot * 0.32 + highlight,
        ),
        fill=(255, 255, 255, 190),
    )
    return canvas


def icon(size: int, round_icon: bool = False) -> Image.Image:
    """A dark rounded tile with a warm glow and a bold atom mark.

    The legacy launcher icon is masked to a rounded square so it reads as a
    deliberate tile on Fire TV rather than a raw square of bitmap.
    """
    scale = 4
    big = size * scale
    image = Image.new("RGBA", (big, big), INK)

    glow = soft_glow(int(big * 1.35), ACCENT, 0.46)
    image.alpha_composite(glow, (int(-big * 0.38), int(big * 0.02)))

    mark = atom_mark(int(big * 0.66))
    image.alpha_composite(mark, ((big - mark.width) // 2, (big - mark.height) // 2))

    mask = Image.new("L", (big, big), 0)
    if round_icon:
        ImageDraw.Draw(mask).ellipse((0, 0, big - 1, big - 1), fill=255)
    else:
        ImageDraw.Draw(mask).rounded_rectangle(
            (0, 0, big - 1, big - 1),
            radius=int(big * 0.22),
            fill=255,
        )
    image.putalpha(mask)
    return image.resize((size, size), Image.Resampling.LANCZOS)


def fitted_text(
    draw: ImageDraw.ImageDraw,
    text: str,
    path: str,
    size: int,
    limit: float,
    minimum: int = 10,
) -> tuple[ImageFont.FreeTypeFont, int]:
    """Shrink a font until `text` fits inside `limit` pixels."""
    while size > minimum:
        candidate = font(path, size)
        if draw.textlength(text, font=candidate) <= limit:
            return candidate, size
        size -= 1
    return font(path, minimum), minimum


def banner(width: int = 320, height: int = 180) -> Image.Image:
    """The 320x180 Android TV / Fire TV launcher banner."""
    scale = 4
    w, h = width * scale, height * scale
    image = Image.new("RGBA", (w, h), INK)

    glow = soft_glow(int(h * 2.7), ACCENT, 0.36)
    image.alpha_composite(glow, (int(-h * 0.80), int(-h * 0.85)))
    # A second, wider glow behind the title: one glow alone fell off in a
    # near-vertical line on the right, which read as a seam.
    image.alpha_composite(soft_glow(int(h * 2.4), ACCENT, 0.15), (int(w * 0.30), int(-h * 0.70)))

    draw = ImageDraw.Draw(image)
    # Fine vertical hairline that echoes the website's editorial rules.
    draw.line(
        (int(w * 0.030), int(h * 0.20), int(w * 0.030), int(h * 0.80)),
        fill=(255, 255, 255, 38),
        width=max(2, int(h * 0.003)),
    )

    mark_size = int(h * 0.52)
    mark = atom_mark(mark_size)
    image.alpha_composite(mark, (int(w * 0.075), (h - mark.height) // 2))

    title_x = int(w * 0.285)
    right_limit = w * 0.955 - title_x
    title_font, title_px = fitted_text(draw, "STEMStream", FONT_BOLD, int(h * 0.255), right_limit * 0.86)
    accent_font, _ = fitted_text(draw, "TV", FONT_BOLD, title_px, right_limit * 0.14)

    title = "STEMStream"
    title_w = draw.textlength(title, font=title_font)
    accent_w = draw.textlength("TV", font=accent_font)
    block_w = title_w + int(w * 0.012) + accent_w
    title_x = int((w * 0.285 + w * 0.955 - block_w) / 2)

    # Sit the title block on its optical centre rather than the canvas centre.
    title_y = int(h * 0.335)
    draw.text((title_x, title_y), title, font=title_font, fill=WHITE)
    draw.text((title_x + title_w + int(w * 0.012), title_y), "TV", font=accent_font, fill=ACCENT)

    tagline_font, _ = fitted_text(draw, "Feed your curiosity.", FONT_REGULAR, int(h * 0.098), right_limit)
    draw.text(
        (title_x, int(h * 0.585)),
        "Feed your curiosity.",
        font=tagline_font,
        fill=(255, 255, 255, 170),
    )

    return image.resize((width, height), Image.Resampling.LANCZOS).convert("RGB")


def feature_graphic(width: int = 1024, height: int = 500) -> Image.Image:
    """Google Play feature graphic / Fire TV promo art."""
    scale = 2
    w, h = width * scale, height * scale
    image = Image.new("RGBA", (w, h), INK)

    image.alpha_composite(soft_glow(int(h * 1.8), ACCENT, 0.44), (int(-h * 0.55), int(-h * 0.35)))
    image.alpha_composite(soft_glow(int(h * 1.4), VIOLET, 0.24), (int(w * 0.52), int(-h * 0.52)))

    draw = ImageDraw.Draw(image)
    draw.line(
        (int(w * 0.06), int(h * 0.2), int(w * 0.06), int(h * 0.8)),
        fill=(255, 255, 255, 38),
        width=3,
    )

    mark_size = int(h * 0.52)
    mark = atom_mark(mark_size)
    image.alpha_composite(mark, (int(w * 0.10), (h - mark.height) // 2))

    title_x = int(w * 0.31)
    right_limit = w * 0.94 - title_x
    title_font, title_px = fitted_text(draw, "STEMStream", FONT_BOLD, int(h * 0.175), right_limit * 0.85)
    accent_font, _ = fitted_text(draw, "TV", FONT_BOLD, title_px, right_limit * 0.15)

    title = "STEMStream"
    title_w = draw.textlength(title, font=title_font)
    title_y = int(h * 0.28)
    draw.text((title_x, title_y), title, font=title_font, fill=WHITE)
    draw.text((title_x + title_w + int(w * 0.008), title_y), "TV", font=accent_font, fill=ACCENT)

    tagline_font, _ = fitted_text(draw, "Feed your curiosity.", FONT_REGULAR, int(h * 0.060), right_limit)
    draw.text(
        (title_x, int(h * 0.515)),
        "Feed your curiosity.",
        font=tagline_font,
        fill=(255, 255, 255, 172),
    )

    meta = "749 hand-picked videos  ·  7 disciplines  ·  no ads"
    meta_font, _ = fitted_text(draw, meta, FONT_REGULAR, int(h * 0.040), right_limit)
    draw.text((title_x, int(h * 0.625)), meta, font=meta_font, fill=(255, 255, 255, 120))

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
