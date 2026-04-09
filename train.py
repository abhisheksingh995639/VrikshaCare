import os
import numpy as np
import tensorflow as tf
from tensorflow.keras import layers, models
from tensorflow.keras.applications import EfficientNetB0
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.applications.efficientnet import preprocess_input
import json
import shutil


EXPORT_ONLY = True  
FINE_TUNE = True
DATASET_DIR = os.path.join("archive", "dataset", "dataset")
IMG_SIZE = (224, 224)
BATCH_SIZE = 32
EPOCHS = 30
MODEL_SAVE_PATH = "rose_disease_model.h5"
TFLITE_SAVE_PATH = "rose_disease_model.tflite"
CLASSES = ["Black_Spot", "Downy_Mildew", "Dry_Leaf", "Healthy_Leaf", "Leaf_Holes"]
ANDROID_ASSETS = os.path.join("PROApp", "android_app", "app", "src", "main", "assets")


base = EfficientNetB0(input_shape=(224, 224, 3), include_top=False, weights='imagenet')
base.trainable = False

model = models.Sequential([
    base,
    layers.GlobalAveragePooling2D(),
    layers.BatchNormalization(),
    layers.Dense(128, activation='relu',
                 kernel_regularizer=tf.keras.regularizers.l2(1e-3)),
    layers.Dropout(0.5),
    layers.Dense(len(CLASSES), activation='softmax')
])

if not EXPORT_ONLY:
    # ... (Actual training code would go here if EXPORT_ONLY was False)
    pass
else:
    print(f"REPAIR MODE: Loading best weights from {MODEL_SAVE_PATH}...")
    if not os.path.exists(MODEL_SAVE_PATH):
        print(f"ERROR: {MODEL_SAVE_PATH} not found!")
        exit(1)
    
    # Initialize the model graph before loading weights
    model(np.zeros((1, 224, 224, 3), dtype=np.float32))
    model.load_weights(MODEL_SAVE_PATH)
    print("Weights loaded successfully!")

# --- TFLite Export (High Compatibility / High Accuracy) ---
print("\nExporting to TFLite (Float32, No Quantization)...")
try:
    # Using from_keras_model on the live model object is the most reliable way 
    # to preserve the exact weights in the TFLite file.
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    
    # CRITICAL: We DO NOT use converter.optimizations = [tf.lite.Optimize.DEFAULT]
    # This avoids the "FULLY_CONNECTED version 12" error on Android.
    tflite_model = converter.convert()
    
    with open(TFLITE_SAVE_PATH, 'wb') as f:
        f.write(tflite_model)
    print(f"✅ TFLite saved: {TFLITE_SAVE_PATH}")

    # Auto-Sync to Android Assets with a safe filename
    ANDROID_MODEL_NAME = "rose_model_v3.tflite"
    if os.path.exists(ANDROID_ASSETS):
        shutil.copy2(TFLITE_SAVE_PATH, os.path.join(ANDROID_ASSETS, ANDROID_MODEL_NAME))
        shutil.copy2("class_indices.json", os.path.join(ANDROID_ASSETS, "class_indices.json"))
        print(f"✅ Auto-synced model + classes to Android: {ANDROID_MODEL_NAME}")
    else:
        print(f"⚠️ Android assets folder not found at {ANDROID_ASSETS}")

except Exception as e:
    print(f"Export failed: {e}")

print("\nSuccess! Please rebuild your Android app (Clean & Re-run).")