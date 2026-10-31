# Public food dataset sources for NutriGlass AI

This project can bootstrap its food dataset without asking the app owner to upload photos. The sources below are public; they are not all equivalent and must not be mixed blindly.

## 1. Nepali Food Image Dataset 13 (primary local-food source)
- Project: https://universe.roboflow.com/sajha-samadhan/nepali-food-image-dataset-13
- Reported size: 1,999 images; 14 classes.
- Classes include: bara, chatpate, dal_bhat, dhido, ghonghi, gundruk, juju_dhau, momo, phulaura, samosa, sel_roti, thukpa, yomari.
- License shown on the project page: CC BY 4.0.
- Use: local-food detection bootstrap. Preserve attribution and the source license. Verify the downloadable version's labels and license before training or redistribution.

## 2. Indian Foods image dataset (CC0)
- Dataset: https://huggingface.co/datasets/bharat-raghunathan/indian-foods-dataset
- Reported size: 4,770 images across train/test, 15 classes.
- Includes: biryani, dal, pakora, panipuri, samosa and other Indian dishes.
- Dataset card declares CC0-1.0.
- Use: classification support for overlapping dishes. It is not a substitute for Nepali-specific examples or multi-dish bounding boxes.

## 3. SOHL multi-dish Indian food detection dataset
- Dataset: https://huggingface.co/datasets/devmka/sohl-multidish-yolo-dataset
- Reported size: 377 annotated images, 16 classes, YOLO format.
- Use: research/evaluation candidate for multi-dish plates. Confirm the dataset's license and terms before commercial use; do not include it in commercial training until verified.

## Important limitations
- These datasets do not cover every NutriGlass class. Momo varieties, panipuri, chatpate, thukpa and dal-bhat need balanced examples from the appropriate sources.
- Image classification labels do not provide bounding boxes. Do not convert a whole-image class label into a fabricated detection box.
- Public web availability does not automatically grant commercial reuse. Keep source, license, attribution and dataset version in the manifest.
- Do not train or ship a model until dataset validation, class balance, duplicate removal and a held-out test set are complete.
- No model weights are included by this source catalog.
