#!/usr/bin/env python3
"""
Regenerate ic_launcher_foreground*.png at all densities plus the Fastlane store
icon, with transparent margin around the artwork (same fg_scale as monochrome).

Adaptive icons draw the foreground on top of ic_launcher_background; padding
on the *output* square is controlled by --fg-scale (smaller value = more empty
margin on the launcher tile).

The *master* PNG is used as-is except for an optional symmetric crop:

  --src-crop-inset  Fraction of source width/height trimmed from EACH edge
                    before any scaling (default 0 = use the whole image).
                    Increase only if you intentionally want to shave bleed off
                    the source; decrease toward 0 to “crop off less” of the
                    original and keep more of the bitmap.
"""

from __future__ import annotations

import argparse
from pathlib import Path

MIPMAP_SIZES: dict[str, int] = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def _apply_src_crop_inset(src_rgba, inset_fraction: float):
    """Symmetric crop: remove inset_fraction * side from left/right and top/bottom."""
    if inset_fraction <= 0:
        return src_rgba
    w, h = src_rgba.size
    dx = int(round(w * inset_fraction))
    dy = int(round(h * inset_fraction))
    if dx * 2 >= w or dy * 2 >= h:
        return src_rgba
    return src_rgba.crop((dx, dy, w - dx, h - dy))


def _embed_on_square(src_rgba, canvas: int, fg_scale: float):
    from PIL import Image

    if fg_scale >= 0.999:
        return src_rgba.resize((canvas, canvas), Image.Resampling.LANCZOS)
    inner = max(1, int(round(canvas * fg_scale)))
    scaled = src_rgba.resize((inner, inner), Image.Resampling.LANCZOS)
    out = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    off = (canvas - inner) // 2
    out.paste(scaled, (off, off), scaled)
    return out


def generate(
    src: Path,
    res_root: Path,
    *,
    fg_scale: float = 0.75,
    src_crop_inset: float = 0.0,
    fastlane_icon: Path | None = None,
) -> None:
    from PIL import Image

    img = Image.open(src).convert("RGBA")
    img = _apply_src_crop_inset(img, src_crop_inset)

    for folder, px in MIPMAP_SIZES.items():
        out_dir = res_root / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        tile = _embed_on_square(img, px, fg_scale)
        for name in ("ic_launcher_foreground.png", "ic_launcher_foreground_round.png"):
            out_path = out_dir / name
            tile.save(out_path, format="PNG", optimize=True)
            print("wrote", out_path)

    if fastlane_icon is not None:
        store = _embed_on_square(img, 512, fg_scale)
        fastlane_icon.parent.mkdir(parents=True, exist_ok=True)
        store.save(fastlane_icon, format="PNG", optimize=True)
        print("wrote", fastlane_icon)


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    default_src = root / "app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png"
    icon2 = Path.home() / "Documents" / "researches" / "icon2.png"
    if icon2.exists():
        default_src = icon2

    res_root = root / "app/src/main/res"
    fastlane = root / "fastlane/metadata/android/en-US/images/icon.png"

    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--src", type=Path, default=default_src, help="Master RGBA (high-res preferred)")
    ap.add_argument("--res", type=Path, default=res_root, help="app/src/main/res")
    ap.add_argument(
        "--fg-scale",
        type=float,
        default=0.75,
        help="Fraction of each output square filled by artwork (default 0.75; lower = more margin)",
    )
    ap.add_argument(
        "--src-crop-inset",
        type=float,
        default=0.0,
        metavar="F",
        help="Per-edge crop from master as fraction of source W/H before scaling (default 0 = full image)",
    )
    ap.add_argument(
        "--no-fastlane",
        action="store_true",
        help="Skip fastlane/metadata/.../icon.png",
    )
    args = ap.parse_args()

    if not args.src.exists():
        raise SystemExit(f"Source not found: {args.src}")

    generate(
        args.src,
        args.res,
        fg_scale=args.fg_scale,
        src_crop_inset=args.src_crop_inset,
        fastlane_icon=None if args.no_fastlane else fastlane,
    )


if __name__ == "__main__":
    main()
