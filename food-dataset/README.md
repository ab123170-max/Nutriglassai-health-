# NutriGlass Nepali Food Detection Dataset

This directory defines the annotation contract and tooling for a future Nepali-food detector. **It contains no training photographs and no trained model.** Do not treat the class list as proof that the app can currently recognize these dishes.

## Initial classes

See `classes.txt`. The first 22 classes cover common foods and components. Keep labels visually distinguishable. For mixed meals, annotate visible components separately when their boundaries can be identified; use a whole-dish class only when the component cannot be reliably separated. Avoid drawing overlapping duplicate boxes for the same item.

## Dataset layout (YOLO detection format)

Place consented images and matching label files in a private dataset workspace, not necessarily in the public Git repository:

```
dataset/
  images/
    train/       # may initially contain all images
  labels/
    train/       # same filename as image, .txt extension
```

Each label line is:
`class_id x_center y_center width height`

All coordinates must be normalized to 0..1. Class IDs are zero-based and follow `classes.txt`.

## Collection guidance

- Get permission from the photographer and, where relevant, the food outlet.
- Photograph the same dish across different plates, lighting, distances, camera angles and portion sizes.
- Include real mixed Nepali meals, not only isolated studio food photos.
- Capture examples from multiple kitchens/outlets and days.
- Include hard negatives (empty plates, utensils, table patterns, packaging) as images with empty label files.
- Do not include faces, names, phone numbers, receipts, or other personal information.
- Do not use scraped images unless the license explicitly permits model training.
- Keep a source/session identifier outside the public repository. Split by source/session to reduce near-duplicate leakage between train and validation sets.

## Validate and split

Install Python dependencies:
`pip install -r food-dataset/requirements.txt`

Validate annotations:
`python food-dataset/tools/validate_dataset.py --root /path/to/dataset`

Split by source folder (images should be grouped under source/session subfolders):
`python food-dataset/tools/split_dataset.py --root /path/to/dataset --seed 42`

## Train

After you have enough labelled images per class and the validation report is clean:
`python food-dataset/train_yolo.py --data food-dataset/data.yaml --model yolov8n.pt --epochs 100 --imgsz 640`

Training is deliberately not triggered by Android CI. It requires a labelled dataset and a suitable training machine. Review per-class precision/recall and mAP; do not ship a model based only on training loss.

## Android integration status

Phase 4's Android adapter expects a TensorFlow Lite Task Library `ObjectDetector` model with compatible metadata and labels. Ultralytics exports are not automatically guaranteed to satisfy that contract. This phase trains/evaluates a dataset model only; a separate conversion/metadata validation and Android inference integration step is required before copying any model into `app/src/main/assets/food_detector.tflite`.
