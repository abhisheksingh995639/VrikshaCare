import tkinter as tk
from tkinter import filedialog, ttk
import numpy as np
from PIL import Image, ImageTk
import json
import os

try:
    import tflite_runtime.interpreter as tflite
    Interpreter = tflite.Interpreter
except ImportError:
    import tensorflow as tf
    Interpreter = tf.lite.Interpreter
from tensorflow.keras.applications.efficientnet import preprocess_input

MODEL_PATH = "rose_model_v3.tflite"
CLASS_JSON = "class_indices.json"
IMG_SIZE = (224, 224)
CONFIDENCE_THRESHOLD = 0.70  

DISEASE_INFO = {
    "Black_Spot": {
        "color": "#e74c3c",
        "icon": "🔴",
        "cause": "Fungal infection (Diplocarpon rosae)",
        "treatment": "Remove infected leaves, apply fungicide (copper-based or neem oil). Improve air circulation.",
        "severity": "High"
    },
    "Downy_Mildew": {
        "color": "#3498db",
        "icon": "🔵",
        "cause": "Water mold (Peronospora sparsa) spreading in humid conditions",
        "treatment": "Improve airflow, avoid overhead watering, remove infected parts, apply systemic fungicides.",
        "severity": "High"
    },
    "Dry_Leaf": {
        "color": "#e67e22",
        "icon": "🟠",
        "cause": "Underwatering, heat stress or root issues",
        "treatment": "Water deeply and regularly. Check soil drainage. Add mulch to retain moisture.",
        "severity": "Medium"
    },
    "Healthy_Leaf": {
        "color": "#27ae60",
        "icon": "🟢",
        "cause": "No disease detected",
        "treatment": "Continue regular care: water, fertilize, prune as needed.",
        "severity": "None"
    },
    "Leaf_Holes": {
        "color": "#8e44ad",
        "icon": "🟣",
        "cause": "Insect damage (caterpillars, beetles) or shot-hole fungus",
        "treatment": "Inspect for insects and remove manually. Apply neem oil or insecticidal soap spray.",
        "severity": "Medium"
    }
}

class RoseDiseaseApp:
    def __init__(self, root):
        self.root = root
        self.root.title("VrikshaCare - Rose Leaf Disease Detector")
        self.root.geometry("780x680")
        self.root.configure(bg="#1a1a2e")
        self.root.resizable(True, True)

        self.interpreter = None
        self.idx2class = {}
        self.current_image = None

        self._load_model()
        self._build_ui()

    def _load_model(self):
        if not os.path.exists(MODEL_PATH):
            self.model_status = f"❌ Model not found: {MODEL_PATH}"
            return
        if not os.path.exists(CLASS_JSON):
            self.model_status = f"❌ class_indices.json not found"
            return
        try:
            self.interpreter = Interpreter(model_path=MODEL_PATH)
            self.interpreter.allocate_tensors()
            self.input_details = self.interpreter.get_input_details()
            self.output_details = self.interpreter.get_output_details()
            with open(CLASS_JSON) as f:
                class_indices = json.load(f)
            self.idx2class = {v: k for k, v in class_indices.items()}
            self.model_status = f"✅ Model loaded ({os.path.getsize(MODEL_PATH)/1024:.0f} KB)"
        except Exception as e:
            self.model_status = f"❌ Error: {e}"

    def _build_ui(self):
        # Header
        hdr = tk.Frame(self.root, bg="#16213e", pady=12)
        hdr.pack(fill="x")
        tk.Label(hdr, text="🌿 VrikshaCare", font=("Georgia", 22, "bold"),
                 bg="#16213e", fg="#e94560").pack()
        tk.Label(hdr, text="AI-powered plant health analysis", font=("Helvetica", 10),
                 bg="#16213e", fg="#a0a0b0").pack()

        # Status bar
        self.status_var = tk.StringVar(value=self.model_status)
        status_bar = tk.Label(self.root, textvariable=self.status_var, font=("Courier", 9),
                              bg="#0f3460", fg="#e0e0e0", anchor="w", padx=10, pady=4)
        status_bar.pack(fill="x")

        # Main content
        content = tk.Frame(self.root, bg="#1a1a2e")
        content.pack(fill="both", expand=True, padx=20, pady=10)

        # Left: image panel
        left = tk.Frame(content, bg="#16213e", bd=2, relief="groove", width=320)
        left.pack(side="left", fill="both", expand=True, padx=(0, 10))
        left.pack_propagate(False)

        tk.Label(left, text="Leaf Image", font=("Helvetica", 11, "bold"),
                 bg="#16213e", fg="#a0a0b0").pack(pady=(12, 0))

        self.img_label = tk.Label(left, bg="#0f3460", text="No image loaded\n\nClick 'Load Image' to begin",
                                  fg="#606080", font=("Helvetica", 10), width=30, height=14,
                                  relief="flat", bd=0)
        self.img_label.pack(padx=15, pady=10, fill="both", expand=True)

        btn_frame = tk.Frame(left, bg="#16213e")
        btn_frame.pack(pady=10)

        tk.Button(btn_frame, text="📂 Load Image", command=self.load_image,
                  font=("Helvetica", 11, "bold"), bg="#e94560", fg="white",
                  activebackground="#c0304a", bd=0, padx=20, pady=8, cursor="hand2").pack(side="left", padx=5)

        tk.Button(btn_frame, text="🔍 Analyze", command=self.analyze,
                  font=("Helvetica", 11, "bold"), bg="#0f3460", fg="white",
                  activebackground="#1a4a80", bd=0, padx=20, pady=8, cursor="hand2").pack(side="left", padx=5)

        # Right: results panel
        right = tk.Frame(content, bg="#16213e", bd=2, relief="groove", width=350)
        right.pack(side="right", fill="both", expand=True)
        right.pack_propagate(False)

        tk.Label(right, text="Analysis Results", font=("Helvetica", 11, "bold"),
                 bg="#16213e", fg="#a0a0b0").pack(pady=(12, 5))

        # Prediction label
        self.pred_var = tk.StringVar(value="—")
        self.pred_label = tk.Label(right, textvariable=self.pred_var,
                                   font=("Georgia", 18, "bold"), bg="#16213e", fg="#e94560")
        self.pred_label.pack(pady=5)

        # Confidence bars frame
        bars_frame = tk.Frame(right, bg="#16213e")
        bars_frame.pack(fill="x", padx=15, pady=5)
        tk.Label(bars_frame, text="Confidence Scores", font=("Helvetica", 9, "bold"),
                 bg="#16213e", fg="#808090").pack(anchor="w")

        self.bar_widgets = {}
        colors = ["#e74c3c", "#3498db", "#e67e22", "#27ae60", "#8e44ad"]
        for i, cls in enumerate(["Black_Spot", "Downy_Mildew", "Dry_Leaf", "Healthy_Leaf", "Leaf_Holes"]):
            row = tk.Frame(bars_frame, bg="#16213e")
            row.pack(fill="x", pady=2)
            tk.Label(row, text=f"{DISEASE_INFO[cls]['icon']} {cls}", width=14, anchor="w",
                     font=("Helvetica", 9), bg="#16213e", fg="#c0c0d0").pack(side="left")
            canvas = tk.Canvas(row, height=16, bg="#0a0a1a", highlightthickness=0)
            canvas.pack(side="left", fill="x", expand=True, padx=5)
            pct_lbl = tk.Label(row, text="0%", width=5, font=("Courier", 9), bg="#16213e", fg="#a0a0b0")
            pct_lbl.pack(side="left")
            self.bar_widgets[cls] = (canvas, pct_lbl, colors[i])

        # Info box
        info_outer = tk.Frame(right, bg="#0f3460", bd=1, relief="flat")
        info_outer.pack(fill="x", padx=15, pady=10)

        self.info_text = tk.Text(info_outer, font=("Helvetica", 9), bg="#0f3460", fg="#c0d0e0",
                                 wrap="word", height=9, bd=0, padx=8, pady=8, state="disabled",
                                 highlightthickness=0)
        self.info_text.pack(fill="both", expand=True)

        # Bottom bar
        tk.Label(self.root, text="Run train.py first to generate the model • For Android: copy .tflite to assets/",
                 font=("Helvetica", 8), bg="#0a0a1a", fg="#404060", pady=4).pack(fill="x", side="bottom")

    def load_image(self):
        path = filedialog.askopenfilename(filetypes=[("Images", "*.jpg *.jpeg *.png *.bmp *.webp")])
        if not path:
            return
        self.current_image = path
        img = Image.open(path).convert("RGB")
        img.thumbnail((290, 250))
        photo = ImageTk.PhotoImage(img)
        self.img_label.configure(image=photo, text="")
        self.img_label.image = photo
        self.status_var.set(f"✅ Loaded: {os.path.basename(path)}")
        self._reset_results()

    def _reset_results(self):
        self.pred_var.set("—")
        self.pred_label.configure(fg="#e94560")
        for cls, (canvas, lbl, color) in self.bar_widgets.items():
            canvas.delete("all")
            lbl.configure(text="0%")
        self._set_info("Click 'Analyze' to detect disease.")

    def analyze(self):
        if not self.current_image:
            self.status_var.set("⚠️ Please load an image first.")
            return
        if self.interpreter is None:
            self.status_var.set("❌ Model not loaded. Run train.py first.")
            return

        img = Image.open(self.current_image).convert("RGB").resize(IMG_SIZE)
        arr = np.array(img, dtype=np.float32)
        arr = np.expand_dims(arr, axis=0)
        
        # PREPROCESSING: EfficientNet expects this function!
        arr = preprocess_input(arr)

        self.interpreter.set_tensor(self.input_details[0]['index'], arr)
        self.interpreter.invoke()
        output = self.interpreter.get_tensor(self.output_details[0]['index'])[0]

        probs = {self.idx2class[i]: float(output[i]) for i in range(len(output))}
        top_class = max(probs, key=lambda k: probs[k])
        top_conf = probs[top_class]

        info = DISEASE_INFO.get(top_class, {})

        # Always update the confidence bars regardless of threshold
        for cls, (canvas, lbl, bar_color) in self.bar_widgets.items():
            canvas.update_idletasks()
            w = canvas.winfo_width()
            canvas.delete("all")
            fill_w = int(w * probs.get(cls, 0))
            if fill_w > 0:
                canvas.create_rectangle(0, 2, fill_w, 14, fill=bar_color, outline="")
            lbl.configure(text=f"{probs.get(cls,0)*100:.1f}%")

        # --- Confidence threshold ---
        if top_conf < CONFIDENCE_THRESHOLD:
            self.pred_var.set(f"⚠️ Uncertain  ({top_conf*100:.1f}%)")
            self.pred_label.configure(fg="#f39c12")
            self._set_info(
                f"⚠️ Low Confidence ({top_conf*100:.1f}%)\n"
                f"Not confident enough for a reliable diagnosis.\n\n"
                f"Tips for a better result:\n"
                f"• Use a clear, well-lit photo of the leaf\n"
                f"• Fill the frame with the leaf\n"
                f"• Avoid blurry or dark images"
            )
            self.status_var.set("⚠️ Low confidence — try a clearer photo")
            return

        color = info.get("color", "#e94560")
        self.pred_var.set(f"{info.get('icon','🌿')} {top_class}  ({top_conf*100:.1f}%)")
        self.pred_label.configure(fg=color)

        self._set_info(
            f"Diagnosis: {top_class}\n"
            f"Confidence: {top_conf*100:.1f}%\n"
            f"Severity: {info.get('severity','—')}\n\n"
            f"Cause:\n{info.get('cause','—')}\n\n"
            f"Treatment:\n{info.get('treatment','—')}"
        )
        self.status_var.set(f"✅ Analysis complete — Detected: {top_class}")

    def _set_info(self, text):
        self.info_text.configure(state="normal")
        self.info_text.delete("1.0", "end")
        self.info_text.insert("end", text)
        self.info_text.configure(state="disabled")


if __name__ == "__main__":
    root = tk.Tk()
    app = RoseDiseaseApp(root)
    root.mainloop()
