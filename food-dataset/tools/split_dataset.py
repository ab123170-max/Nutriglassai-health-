#!/usr/bin/env python3
"""Split YOLO dataset by top-level source/session folder to reduce data leakage."""
from __future__ import annotations
import argparse, random, shutil
from pathlib import Path

EXTS = {".jpg", ".jpeg", ".png", ".webp"}

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--root", required=True, type=Path)
    p.add_argument("--seed", type=int, default=42)
    p.add_argument("--train", type=float, default=.8)
    p.add_argument("--val", type=float, default=.1)
    p.add_argument("--copy", action="store_true", help="Copy instead of move")
    a = p.parse_args()
    if a.train <= 0 or a.val <= 0 or a.train + a.val >= 1:
        p.error("train and val must be >0 and sum to less than 1")
    src_images, src_labels = a.root/"images"/"train", a.root/"labels"/"train"
    if not src_images.exists(): p.error(f"Missing {src_images}")
    groups = {}
    for image in src_images.rglob("*"):
        if image.suffix.lower() not in EXTS: continue
        rel = image.relative_to(src_images)
        if len(rel.parts) < 2:
            p.error("Put images inside source/session subfolders before splitting; flat data cannot be safely grouped.")
        groups.setdefault(rel.parts[0], []).append(image)
    names = sorted(groups)
    if len(names) < 3: p.error("Need at least 3 distinct source/session folders for train/val/test.")
    random.Random(a.seed).shuffle(names)
    n_train = max(1, round(len(names)*a.train))
    n_val = max(1, round(len(names)*a.val))
    if n_train+n_val >= len(names): n_train = len(names)-2; n_val = 1
    split = {"train": names[:n_train], "val": names[n_train:n_train+n_val], "test": names[n_train+n_val:]}
    op = shutil.copy2 if a.copy else shutil.move
    for target, group_names in split.items():
        for group in group_names:
            for image in groups[group]:
                rel = image.relative_to(src_images)
                label = src_labels / rel.with_suffix(".txt")
                if not label.exists(): raise SystemExit(f"Missing label: {label}")
                out_img, out_lbl = a.root/"images"/target/rel, a.root/"labels"/target/rel.with_suffix(".txt")
                out_img.parent.mkdir(parents=True, exist_ok=True); out_lbl.parent.mkdir(parents=True, exist_ok=True)
                op(str(image), str(out_img)); op(str(label), str(out_lbl))
    for k,v in split.items(): print(f"{k}: {len(v)} source groups")
    print("Split complete. Review class balance in each split before training.")

if __name__ == "__main__": main()
