# NutriGlass AI: Vision Models & Dataset Architecture Documentation

## 1. Executive Summary
NutriGlass AI integrates a modular computer vision and nutritional estimation engine designed for Android 16 (API 36). It combines on-device neural heuristics and local food composition tables with optional cloud multimodal inference via Google Gemini 2.5 Flash.

---

## 2. Candidate Models & Licensing

| Task | Candidate Model | Architecture | Size | License | Source / Download Location |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Detection** | YOLOv8n-Food | CSP-DarkNet + PANet | ~6.5 MB | AGPL-3.0 / Commercial or Apache-2.0 (YOLOv8-TFLite export) | Ultralytics / HuggingFace `models/yolov8n_food` |
| **Detection** | EfficientDet-Lite0 | MobileNetV3 + BiFPN | ~4.4 MB | Apache-2.0 | TensorFlow Hub / MediaPipe Tasks |
| **Segmentation**| DeepLabV3-MobileNetV3 | Atrous Spatial Pyramid Pooling | ~5.8 MB | Apache-2.0 | TensorFlow Lite Model Zoo |
| **Classification**| MobileNetV3-Large-Food | Inverted Residuals + SE blocks | ~4.2 MB | Apache-2.0 | HuggingFace / Google AI Models |
| **Cloud Multimodal**| Gemini 2.5 Flash | Multimodal Transformer | Cloud API | Google Terms of Service | Google AI Studio (`gemini-2.5-flash`) |

---

## 3. Input Normalization & Formats

- **Detection / Classification Input:**
  - Shape: `[1, 224, 224, 3]`
  - Color Space: RGB (Standard Android Bitmap converted via `StandardImagePreprocessor`)
  - Aspect Ratio: Maintained via black letterboxing to prevent distorting elongated foods (such as Sel Roti, bananas, or cucumbers).
  - Pixel Value Range: Normalized to `[0.0, 1.0]` or standard ImageNet zero-mean `[-1.0, 1.0]`.
- **Coordinate System:**
  - Normalized coordinates: `[left, top, right, bottom]` where `0.0 <= coord <= 1.0`.
  - Mapped back to uncropped original pixel frame using `PreprocessedFrame.mapBoxToOriginal()`.

---

## 4. Supported Taxonomy & Nepali Cuisine Classes

NutriGlass AI natively supports 32 verified food classes spanning international staples and specialized Nepali dishes:

1. **Dal Bhat Set (दाल भात)**: Standard multi-dish Nepali thali
2. **Cooked Rice (भात)**: Steamed white rice / Chamal
3. **Yellow Dal (पहेंलो दाल)**: Cooked yellow lentil soup / Musuro dal
4. **Black Dal (कालो दाल)**: Traditional slow-cooked Kalo Maas ko Dal
5. **Nepali Chicken Curry (कुखुराको मासु)**: Spiced bone-in chicken curry
6. **Mixed Vegetable Curry (तरकारी)**: Aloo-Gobi-Mataar / seasonal mixed sabzi
7. **Steamed Momo (ममः)**: Discrete dumpling piece count (typically 8–10 pieces/plate)
8. **Chowmein (चाउमिन)**: Wok-tossed noodles with cabbage, carrots, onion
9. **Sel Roti (सेल रोटी)**: Traditional ring-shaped crispy rice flour bread
10. **Roti / Chapati (रोटी)**: Whole wheat flatbread
11. **Chatpate (चटपटे)**: Spiced puffed rice, chickpeas, peanuts, and tamarind
12. **Panipuri (पानीपुरी)**: Crispy semolina puris with spiced mint potato water
13. **Dhido (ढिँडो)**: Traditional boiled millet / buckwheat staple
14. **Gundruk (गुन्द्रुक भटमास)**: Fermented sun-dried leafy greens with roasted soybeans
15. **Aloo Tama Bodi (आलु तामा बोडी)**: Bamboo shoot, potato, and black-eyed cowpea soup
16. **Samosa (समोसा)**: Spiced potato-filled triangular pastry
17. **Pakoda (पकोडा)**: Besan (chickpea flour) vegetable fritters
18. **Thukpa (थुक्पा)**: Tibetan / Himalayan noodle broth with shredded vegetables
19. **Chicken Biryani (बिर्यानी)**: Spiced basmati rice with marinated chicken
20. **Boiled Egg (उसिनेको अण्डा)**: Whole egg (piece-counted, 50g standard)
21. **Fresh Apple / Fruits (स्याउ, केरा)**: Whole fruit piece counts
22. **Nepali Masala Chiya (दुध चिया)**: Spiced whole milk tea

---

## 5. Portion Estimation & Volume Calibration

NutriGlass AI strictly avoids presenting unverified 2D visual guesses as exact measured truth:

- **Piece-Count Foods**: Foods with discrete physical units (e.g., Momo, Eggs, Sel Roti, Samosa) are quantified by discrete counting with narrower uncertainty (±10%).
- **Plate Calibration**: Utilizes reference dinner plates (standard 26 cm dinner plate, 18 cm side plate, 14 cm kachaura/bowl) to compute surface area in cm².
- **Volume & Density**:
  $$\text{Volume} (\text{cm}^3) = \text{Area} (\text{cm}^2) \times \text{Depth} (\text{cm})$$
  $$\text{Mass} (\text{g}) = \text{Volume} (\text{cm}^3) \times \text{Food Density} (\text{g/cm}^3)$$
- **Transparent Uncertainty**: When depth cannot be verified by hardware sensors (ToF/LiDAR/ARCore), the app displays:
  > *"Visual 2D area estimate (~X g, range Y-Z g). Please confirm serving size before logging."*

---

## 6. Training & Evaluation Pipeline (Outside Mobile App)

1. **Dataset Collection & Annotation**:
   - High-resolution smartphone captures across varied lighting (direct sunlight, indoor tungsten, fluorescent, dim restaurant lighting).
   - Multi-angle perspectives: top-down (90°), natural dining angle (45°), and low angle (20°).
   - COCO annotation schema for bounding boxes and polygonal segmentation masks.
2. **Train / Validation / Test Splits**:
   - 70% Training / 15% Validation / 15% Held-out Test Set.
   - Stratified by dish category to ensure low-frequency traditional dishes (Dhido, Gundruk, Aloo Tama) are equally evaluated.
3. **Evaluation Metrics**:
   - Mean Average Precision (mAP@0.5 and mAP@0.5:0.95) for detection.
   - Mean Intersection over Union (mIoU) for plate/dish segmentation.
   - Top-1 and Top-5 accuracy for food classification.
4. **Export**:
   - Quantized FP16 or INT8 TensorFlow Lite (`.tflite`) artifacts optimized for Android Neural Networks API (NNAPI) and GPU delegate.
