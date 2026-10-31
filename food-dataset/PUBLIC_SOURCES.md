# Public food dataset sources for NutriGlass AI

This catalog records public sources that can reduce manual collection. They are separate datasets with different label formats and must not be blindly combined.

## 1. Nepali Food Image Dataset 13 — local-food detection
- Project: https://universe.roboflow.com/sajha-samadhan/nepali-food-image-dataset-13
- Roboflow page reports 1,999 images and 14 classes; its visible class list includes bara, chatpate, dal_bhat, dhido, ghonghi, gundruk, juju_dhau, momo, phulaura, samosa, sel_roti, thukpa and yomari.
- License shown: CC BY 4.0. Attribute the creator and preserve license terms.
- Important: the currently listed downloadable version reports only 147 images, with 47 train and 100 test and no validation split. Verify the export before relying on the larger overview count.

## 2. Indian Foods image dataset — classification only
- Dataset: https://huggingface.co/datasets/bharat-raghunathan/indian-foods-dataset
- Hugging Face currently reports CC0-1.0 and parquet image data.
- Use for classification of overlapping dishes (for example biryani, dal, pakora, samosa, panipuri where present), after inspecting its actual label list.
- This is not a detection dataset: do not invent object bounding boxes from whole-image labels.

## 3. SOHL Multi-Dish Indian Food Detection — multi-item detection
- Dataset: https://huggingface.co/datasets/SohlHealth/sohl-multidish-yolo-dataset
- Dataset card reports 377 images, 377 YOLO annotation files, 16 broad classes, and MIT license.
- Use as a separate general Indian-food detection source. Its broad categories (curry, rice, snack, etc.) must not be relabeled as specific Nepali foods without verified annotation.

## Important limitations and safeguards
- These sources do not provide complete balanced coverage for all NutriGlass classes or all Nepalese recipes.
- Keep source, version, license, attribution, and original labels in the dataset manifest.
- Remove duplicates and inspect class distribution; split by image/source group to avoid train/test leakage.
- Keep source datasets separate until label mappings are reviewed. Never convert a generic class into a specific dish label by guessing.
- No NutriGlass model weights are included or trained by this catalog. Training and on-device inference still require validation and export work.
