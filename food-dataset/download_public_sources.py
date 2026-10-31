#!/usr/bin/env python3
"""Download the CC0 Indian-food classification source into a local staging folder.

Usage:
  pip install -r food-dataset/requirements.txt
  python food-dataset/download_public_sources.py

This deliberately stages classification images only. It does NOT fabricate
YOLO bounding boxes or claim that a trained model is ready.
"""
from pathlib import Path
from datasets import load_dataset
from PIL import Image

ROOT = Path(__file__).resolve().parent / "raw" / "hf_indian_foods_cc0"
DATASET_ID = "bharat-raghunathan/indian-foods-dataset"


def main():
    ds = load_dataset(DATASET_ID)
    count = 0
    for split_name, split in ds.items():
        for index, row in enumerate(split):
            label = row.get("label", row.get("target"))
            names = split.features.get("label") or split.features.get("target")
            if hasattr(label, "item"):
                label = label.item()
            if hasattr(names, "int2str"):
                label_name = names.int2str(int(label))
            elif isinstance(label, str):
                label_name = label
            else:
                label_name = str(label)
            image = row["image"]
            if not isinstance(image, Image.Image):
                image = Image.open(image)
            out_dir = ROOT / split_name / label_name
            out_dir.mkdir(parents=True, exist_ok=True)
            image.convert("RGB").save(out_dir / f"{index:06d}.jpg", quality=95)
            count += 1
    print(f"Staged {count} images under {ROOT}")
    print("Review labels, duplicates, and license metadata before training.")


if __name__ == "__main__":
    main()
