#!/usr/bin/env python3
"""Stage the CC0 Indian-food classification dataset for review.

This is classification data, not object detection: no YOLO boxes are created.
"""
from pathlib import Path
import re
from datasets import load_dataset
from PIL import Image

ROOT = Path(__file__).resolve().parent / "raw" / "hf_indian_foods_cc0"
DATASET_ID = "bharat-raghunathan/indian-foods-dataset"
IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}


def safe_name(value):
    name = re.sub(r"[^a-zA-Z0-9._-]+", "_", str(value)).strip("._")
    return name[:100] or "unknown"


def main():
    ds = load_dataset(DATASET_ID)
    count = 0
    for split_name, split in ds.items():
        features = split.features
        image_key = next((k for k, v in features.items() if v.__class__.__name__ == "Image"), None)
        if image_key is None:
            image_key = next((k for k in ("image", "img", "picture", "file") if k in features), None)
        if image_key is None:
            raise RuntimeError(f"No image column found in split {split_name}; columns={list(features)}")

        label_key = next((k for k in ("label", "labels", "class", "category", "food", "target") if k in features), None)
        if label_key is None:
            raise RuntimeError(f"No label column found in split {split_name}; columns={list(features)}")
        label_feature = features[label_key]

        for index, row in enumerate(split):
            label = row[label_key]
            if hasattr(label, "item"):
                label = label.item()
            if hasattr(label_feature, "int2str"):
                label = label_feature.int2str(int(label))
            label_dir = safe_name(label)

            image = row[image_key]
            if isinstance(image, dict):
                if image.get("bytes"):
                    import io
                    image = Image.open(io.BytesIO(image["bytes"]))
                elif image.get("path"):
                    image = Image.open(image["path"])
                else:
                    raise RuntimeError(f"Image record has neither bytes nor path: split={split_name}, index={index}")
            elif not isinstance(image, Image.Image):
                image = Image.open(image)

            out_dir = ROOT / safe_name(split_name) / label_dir
            out_dir.mkdir(parents=True, exist_ok=True)
            out_path = out_dir / f"{index:06d}.jpg"
            image.convert("RGB").save(out_path, quality=92)
            count += 1

    if count == 0:
        raise RuntimeError("Dataset loaded but no images were staged.")
    print(f"Staged {count} labelled classification images under {ROOT}")
    print("Keep this source separate from YOLO detection data; review labels and license before training.")


if __name__ == "__main__":
    main()
