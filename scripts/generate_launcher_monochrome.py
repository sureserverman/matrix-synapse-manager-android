#!/usr/bin/env python3
"""
Build ic_launcher_monochrome.png for adaptive themed icons (Android 13+).

Monochrome assets must be white (#FFFFFF) with transparency only. A narrow
transparent seam between the green "M" and gray gear keeps them readable when
themed. Interior shadows that classify as "gear" inside the M are merged back
into M so seams do not split the letter. The final glyph is scaled on the canvas
(--fg-scale). Optional --src-crop-inset trims the master before processing
(same semantics as generate_launcher_foregrounds.py; default 0 = full image).
"""

from __future__ import annotations

import argparse
from pathlib import Path


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


def _classify_m_vs_gear(r: int, g: int, b: int, a: int) -> str:
    """Return 'bg' | 'm' | 'gear' for one pixel."""
    if a < 8:
        return "bg"
    total = r + g + b
    if total < 48:
        return "bg"
    # Lime / Matrix green dominates the letter
    if g > 95 and g >= r + 22 and g >= b + 18 and (g - b) > (r - b) * 0.4 + 10:
        return "m"
    return "gear"


def _idx(w: int, x: int, y: int) -> int:
    return y * w + x


def _merge_gear_inside_m(w: int, h: int, cls: list[str], min_m_neighbors: int = 5) -> list[str]:
    """
    Reclassify gear -> m when almost all neighbors are M (letter interior /
    anti-aliased creases), so we do not punch seams *inside* the M.
    """
    cur = cls[:]
    for _ in range(8):
        nxt = cur[:]
        changed = False
        for y in range(h):
            for x in range(w):
                i = _idx(w, x, y)
                if cur[i] != "gear":
                    continue
                m_count = 0
                for dy in (-1, 0, 1):
                    for dx in (-1, 0, 1):
                        if dx == 0 and dy == 0:
                            continue
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < w and 0 <= ny < h and cur[_idx(w, nx, ny)] == "m":
                            m_count += 1
                if m_count >= min_m_neighbors:
                    nxt[i] = "m"
                    changed = True
        cur = nxt
        if not changed:
            break
    return cur


def _build_seam(w: int, h: int, cls: list[str]) -> list[bool]:
    """True where M touches gear (4-neighborhood)."""
    seam = [False] * (w * h)

    for y in range(h):
        for x in range(w):
            i = _idx(w, x, y)
            c = cls[i]
            if c == "bg":
                continue
            for dx, dy in ((0, 1), (0, -1), (1, 0), (-1, 0)):
                nx, ny = x + dx, y + dy
                if nx < 0 or nx >= w or ny < 0 or ny >= h:
                    continue
                n = cls[_idx(w, nx, ny)]
                if c == "m" and n == "gear":
                    seam[i] = True
                    break
                if c == "gear" and n == "m":
                    seam[i] = True
                    break
    return seam


def _dilate8(seam: list[bool], w: int, h: int, passes: int) -> list[bool]:
    cur = seam[:]
    for _ in range(passes):
        nxt = cur[:]
        for y in range(h):
            for x in range(w):
                i = y * w + x
                if not cur[i]:
                    continue
                for dy in (-1, 0, 1):
                    for dx in (-1, 0, 1):
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < w and 0 <= ny < h:
                            nxt[ny * w + nx] = True
        cur = nxt
    return cur


def _embed_scaled(inner, canvas: int, fg_scale: float):
    from PIL import Image

    if fg_scale >= 0.999:
        return inner
    sw = max(1, int(round(canvas * fg_scale)))
    sh = sw
    small = inner.resize((sw, sh), Image.Resampling.LANCZOS)
    out = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    off_x = (canvas - sw) // 2
    off_y = (canvas - sh) // 2
    out.paste(small, (off_x, off_y), small)
    return out


def generate(
    src: Path,
    out: Path,
    size: int = 512,
    *,
    fg_scale: float = 0.75,
    src_crop_inset: float = 0.0,
    seam_dilate_passes: int = 2,
) -> None:
    from PIL import Image

    img = Image.open(src).convert("RGBA")
    img = _apply_src_crop_inset(img, src_crop_inset)
    img = img.resize((size, size), Image.Resampling.LANCZOS)
    w, h = img.size
    px = list(img.getdata())

    cls: list[str] = []
    for (r, g, b, a) in px:
        cls.append(_classify_m_vs_gear(r, g, b, a))

    cls = _merge_gear_inside_m(w, h, cls, min_m_neighbors=5)

    seam = _build_seam(w, h, cls)
    seam_wide = _dilate8(seam, w, h, passes=seam_dilate_passes)

    out_px: list[tuple[int, int, int, int]] = []
    for i, (r, g, b, a) in enumerate(px):
        if cls[i] == "bg" or (r + g + b < 52 and a < 252):
            out_px.append((0, 0, 0, 0))
        elif seam_wide[i]:
            out_px.append((0, 0, 0, 0))
        else:
            l = 0.299 * r + 0.587 * g + 0.114 * b
            na = min(255, max(a, int(l * 0.92)))
            out_px.append((255, 255, 255, na))

    mono = Image.new("RGBA", (w, h))
    mono.putdata(out_px)
    mono = _embed_scaled(mono, w, fg_scale)
    out.parent.mkdir(parents=True, exist_ok=True)
    mono.save(out, format="PNG", optimize=True)


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    default_src = root / "app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png"
    icon2 = Path.home() / "Documents" / "researches" / "icon2.png"
    if icon2.exists():
        default_src = icon2
    default_out = root / "app/src/main/res/drawable-nodpi/ic_launcher_monochrome.png"

    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--src", type=Path, default=default_src, help="RGBA source (full-color icon)")
    ap.add_argument("--out", type=Path, default=default_out, help="Output PNG path")
    ap.add_argument("--size", type=int, default=512, help="Output square size (default 512)")
    ap.add_argument(
        "--fg-scale",
        type=float,
        default=0.75,
        help="Scale of glyph on output canvas (0–1]; default 0.75 — lower = more margin)",
    )
    ap.add_argument(
        "--src-crop-inset",
        type=float,
        default=0.0,
        metavar="F",
        help="Per-edge crop from master as fraction of source W/H before pipeline (default 0 = full image)",
    )
    ap.add_argument(
        "--seam-dilate-passes",
        type=int,
        default=2,
        help="8-neighborhood dilations for M/gear seam width (default 2)",
    )
    args = ap.parse_args()

    if not args.src.exists():
        raise SystemExit(f"Source not found: {args.src}")
    generate(
        args.src,
        args.out,
        size=args.size,
        fg_scale=args.fg_scale,
        src_crop_inset=args.src_crop_inset,
        seam_dilate_passes=args.seam_dilate_passes,
    )
    print(f"Wrote {args.out}")


if __name__ == "__main__":
    main()
