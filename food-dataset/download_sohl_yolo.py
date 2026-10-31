#!/usr/bin/env python3
"""Stage the MIT-licensed SOHL YOLO dataset without changing its labels.

Usage:
  pip install huggingface_hub
  python food-dataset/download_sohl_yolo.py

The original images/, labels/, and dataset.yaml are kept intact. This dataset
uses broad Indian-food classes and is NOT merged into the Nepali class list.
"""
from pathlib import Path
from huggingface_hub import snapshot_download

ROOT = Path(__file__).resolve().parent / "raw" / "sohl_multidish_yolo"
REPO_ID = "SohlHealth/sohl-multidish-yolo-dataset"


def main():
    path = snapshot_download(
        repo_id=REPO_ID,
        repo_type="dataset",
        local_dir=str(ROOT),
        allow_patterns=["images/**", "labels/**", "dataset.yaml", "README.md", "LICENSE*"],
    )
    print(f"Downloaded original YOLO dataset to: {path}")
    print("Keep its original class names and MIT license; review before merging.")


if __name__ == "__main__":
    main()
