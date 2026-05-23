<div align="center">

# 🌿 VrikshaCare
**AI-Powered Rose Leaf Disease Detector**

[![TensorFlow](https://img.shields.io/badge/TensorFlow-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white)]()
[![Keras](https://img.shields.io/badge/Keras-D00000?style=for-the-badge&logo=Keras&logoColor=white)]()
[![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)]()
[![Tkinter](https://img.shields.io/badge/Tkinter-UI-blueviolet?style=for-the-badge)]()
[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)]()

<img width="1857" height="460" alt="image" src="https://github.com/user-attachments/assets/3f50828e-315c-4767-944b-9ffa35c6df74" />

VrikshaCare is an intelligent plant health analysis tool specifically designed for identifying various diseases in rose leaves. Utilizing the power of **EfficientNetB0**, the model detects diseases ranging from fungal infections to environmental stress, providing actionable treatment recommendations.

</div>

---

## ✨ Key Features
- **Accurate Detection:** Leverages an EfficientNetB0-based CNN to classify complex disease patterns on rose foliage.
- **Desktop Application:** Includes a premium, dark-themed Tkinter GUI (`test_app.py`) for quick and visually appealing desktop inference.
- **Mobile Ready:** Features a TFLite export pipeline (`train.py`) that safely synchronizes lightweight edge models straight into the companion Android application (`PROApp/android_app`).
- **Actionable Insights:** Beyond raw detection, it provides detailed symptom causes, severity levels, and treatment regimes.

## 🩺 Supported Diagnoses

                                | Diagnosis | Icon | Primary Cause | Severity |
                                |---|:---:|---|:---:|
| **Black Spot** | 🔴 | Fungal infection (*Diplocarpon rosae*) | High |
| **Downy Mildew** | 🔵 | Water mold spreading in humid environments | High |
| **Dry Leaf** | 🟠 | Underwatering, heat stress, or root issues | Medium |
| **Healthy Leaf** | 🟢 | Perfect health / Optimal growing conditions | None |
| **Leaf Holes** | 🟣 | Insect damage or shot-hole fungus | Medium |

## 🛠 Tech Stack
* **Deep Learning Pipeline:** TensorFlow 2.x, Keras (EfficientNet-B0 Base)
* **Computer Vision / Data Processing:** Pillow (PIL), NumPy, Scikit-learn
* **Desktop Interface:** Python Tkinter
* **Mobile Inference Engine:** TensorFlow Lite (`tflite_runtime`)

## 🚀 Setup & Installation

**1. Clone the repository and navigate to the project folder:**
```bash
# Assuming you have cloned the repo
cd PlantDisease
```

**2. Install dependencies:**
```bash
pip install -r requirements.txt
```

*(Note: If you only need to run inference via the Tkinter UI without full model training capabilities, you can optionally install `tflite-runtime` instead of the full `tensorflow` library.)*

## 💻 Usage

### Launching the Desktop Analyzer
Run the cross-platform Tkinter GUI to analyze individual leaf photos:
```bash
python test_app.py
```
> **Tip:** Ensure that `rose_model_v3.tflite` and `class_indices.json` are present in the root directory before launching.

### Training & Exporting the Model
To re-train the model or export updated weights to TFLite (for mobile usage):
```bash
python train.py
```
> **Note:** The export script is configured to automatically sync the converted `.tflite` model directly into the `PROApp/android_app/app/src/main/assets` directory to streamline mobile development.

## 📁 Core Repository Structure
```text
PlantDisease/
├── archive/                  # Image dataset directory
├── PROApp/                   # Source code and assets for the Android mobile application
├── train.py                  # Model building, training, and TFLite export logic
├── test_app.py               # Desktop GUI application for local inference
├── requirements.txt          # Python environment dependencies
├── class_indices.json        # Categorical class label mappings
├── rose_disease_model.h5     # Checkpointed Keras H5 model 
└── rose_model_v3.tflite      # Exported TFLite edge model 
```
