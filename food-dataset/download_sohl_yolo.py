#!/usr/bin/env python3
"""Stage the MIT-licensed SOHL YOLO dataset without changing its labels.

Usage: python food-dataset/download_sohl_yolo.py
The original directory structure and labels are preserved. This is broad Indian
food detection data, not a substitute for Nepali-specific training examples.
"""
from pathlib import Path
from huggingface_hub import snapshot_download

ROOT = Path(__file__).resolve().parent / "raw" / "sohl_multidish_yolo"
REPO_ID = "SohlHealth/sohl-multidish-yolo-dataset"


def main():
    patterns = [
        "*.yaml", "*.yml", "*.md", "*.txt", "*.jpg", "*.jpeg", "*.png",
        "**/*.yaml", "**/*.yml", "**/*.md", "**/*.txt",
        "**/*.jpg", "**/*.jpeg", "**/*.png", "**/*.JPG", "**/*.JPEG", "**/*.PNG",
    ]
    path = snapshot_download(
        repo_id=REPO_ID,
        repo_type="dataset",
        local_dir=str(ROOT),
        allow_patterns=patterns,
    )
    images = [p for p in Path(path).rglob("*") if p.is_file() and p.suffix.lower() in {".jpg", ".jpeg", ".png"}]
    labels = list(Path(path).rglob("*.txt"))
    print(f"Downloaded dataset to: {path}")
    print(f"Image files: {len(images)}; text label files: {len(labels)}")
    if not images:
        raise SystemExit("No images found. Check the dataset repository layout or access.")
    if not labels:
        raise SystemExit("No YOLO text labels found. Do not train until labels are located.")


if __name__ == "__main__":
    main()
