#!/usr/bin/env python3
"""Train a baseline Nepali-food detector; run manually after dataset validation."""
from __future__ import annotations
import argparse
from pathlib import Path
from ultralytics import YOLO

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--data", default=str(Path(__file__).with_name("data.yaml")))
    p.add_argument("--model", default="yolov8n.pt")
    p.add_argument("--epochs", type=int, default=100)
    p.add_argument("--imgsz", type=int, default=640)
    p.add_argument("--batch", type=int, default=16)
    p.add_argument("--device", default=None)
    p.add_argument("--project", default="runs/nepali-food")
    p.add_argument("--name", default="baseline")
    a = p.parse_args()
    model = YOLO(a.model)
    model.train(
        data=a.data, epochs=a.epochs, imgsz=a.imgsz, batch=a.batch,
        device=a.device, project=a.project, name=a.name,
        patience=20, cache=False, plots=True, seed=42,
        degrees=8.0, translate=.08, scale=.25, fliplr=.5,
        hsv_h=.015, hsv_s=.5, hsv_v=.35
    )
    best = Path(a.project) / a.name / "weights" / "best.pt"
    if best.exists():
        print(f"Best checkpoint: {best}")
        print("Next: evaluate per-class precision/recall and mAP on a held-out test set.")
        print("Do NOT copy this .pt file into Android assets; Phase 4 requires a validated TFLite ObjectDetector-compatible export.")

if __name__ == "__main__": main()
