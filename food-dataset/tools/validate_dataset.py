#!/usr/bin/env python3
"""Validate YOLO-format image/label pairs for the NutriGlass food dataset."""
from __future__ import annotations
import argparse
from pathlib import Path
from PIL import Image

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--root", required=True, type=Path, help="Dataset root with images/ and labels/")
    p.add_argument("--classes", type=Path, default=Path(__file__).resolve().parents[1] / "classes.txt")
    args = p.parse_args()
    classes = [x.strip() for x in args.classes.read_text(encoding="utf-8").splitlines() if x.strip()]
    image_root, label_root = args.root / "images", args.root / "labels"
    images = [x for x in image_root.rglob("*") if x.suffix.lower() in {".jpg", ".jpeg", ".png", ".webp"}]
    errors, empty = [], 0
    if not images:
        errors.append(f"No images found under {image_root}")
    for image in images:
        rel = image.relative_to(image_root).with_suffix(".txt")
        label = label_root / rel
        if not label.exists():
            errors.append(f"Missing label file: {label}")
            continue
        try:
            with Image.open(image) as im:
                im.verify()
        except Exception as exc:
            errors.append(f"Unreadable image {image}: {exc}")
            continue
        lines = [x.strip() for x in label.read_text(encoding="utf-8").splitlines() if x.strip()]
        if not lines:
            empty += 1
        for line_no, line in enumerate(lines, 1):
            parts = line.split()
            if len(parts) != 5:
                errors.append(f"{label}:{line_no}: expected 5 values, got {len(parts)}")
                continue
            try:
                cls, cx, cy, w, h = map(float, parts)
            except ValueError:
                errors.append(f"{label}:{line_no}: non-numeric annotation")
                continue
            if not cls.is_integer() or not 0 <= int(cls) < len(classes):
                errors.append(f"{label}:{line_no}: class id {cls} outside 0..{len(classes)-1}")
            if not all(0.0 <= v <= 1.0 for v in (cx, cy, w, h)) or w <= 0 or h <= 0:
                errors.append(f"{label}:{line_no}: invalid normalized box {cx,cy,w,h}")
            if cx-w/2 < -1e-6 or cy-h/2 < -1e-6 or cx+w/2 > 1+1e-6 or cy+h/2 > 1+1e-6:
                errors.append(f"{label}:{line_no}: box extends outside image")
    print(f"Images: {len(images)} | empty-label negatives: {empty} | classes: {len(classes)}")
    if errors:
        print(f"Validation FAILED: {len(errors)} issue(s)")
        for e in errors[:200]: print(" -", e)
        raise SystemExit(1)
    print("Validation passed. This checks annotation structure, not label correctness.")

if __name__ == "__main__":
    main()
